package com.fancky.authorization.utility.cache;

import com.fancky.authorization.utility.RedisKey;
import com.fancky.authorization.utility.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@Slf4j
@Component
public class RedisCacheService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private RedisUtil redisUtil;

    /**
     * 缓存查询构建器
     */
    public <T> CacheQueryBuilder<T> builder() {
        return new CacheQueryBuilder<>();
    }

    /**
     * 缓存查询构建器类
     */
    public class CacheQueryBuilder<T> {
        // 核心参数
        private String primaryCacheKey;        // 主缓存Key（必填）
        private String field;                   // Hash field（必填）
        private String nullCacheKey;            // 空值缓存Key（必填）
        private Supplier<T> dbSupplier;         // 数据库查询逻辑（必填）
        private Class<T> resultType;             // 返回值类型（必填）

        // 可选参数
        private String secondaryCacheKey;        // 二级缓存Key（如code缓存）
        private String secondaryField;            // 二级缓存field
        private Function<T, String> secondaryKeyExtractor; // 二级缓存key提取器
        private Function<T, String> secondaryFieldExtractor; // 二级缓存field提取器
        private Consumer<T> additionalCacheLogic; // 额外的缓存逻辑

        // 配置标志
        private boolean enableSecondaryCache = false;
        private long nullCacheTimeout = RedisKey.TIME_OUT_ONE_MINUTE;
        private TimeUnit nullCacheTimeUnit = TimeUnit.SECONDS;

        public CacheQueryBuilder<T> primary(String cacheKey, String field) {
            this.primaryCacheKey = cacheKey;
            this.field = field;
            return this;
        }

        public CacheQueryBuilder<T> nullCache(String nullCacheKey) {
            this.nullCacheKey = nullCacheKey;
            return this;
        }

        public CacheQueryBuilder<T> db(Supplier<T> dbSupplier, Class<T> resultType) {
            this.dbSupplier = dbSupplier;
            this.resultType = resultType;
            return this;
        }

        /**
         * 配置二级缓存（如username索引）
         */
        public CacheQueryBuilder<T> secondaryCache(
                String secondaryCacheKey,
                Function<T, String> fieldExtractor) {
            this.enableSecondaryCache = true;
            this.secondaryCacheKey = secondaryCacheKey;
            this.secondaryFieldExtractor = fieldExtractor;
            return this;
        }

        /**
         * 配置二级缓存（如果field和主缓存不同）
         */
        public CacheQueryBuilder<T> secondaryCache(
                String secondaryCacheKey,
                Function<T, String> fieldExtractor,
                Function<T, String> keyExtractor) {
            this.enableSecondaryCache = true;
            this.secondaryCacheKey = secondaryCacheKey;
            this.secondaryFieldExtractor = fieldExtractor;
            this.secondaryKeyExtractor = keyExtractor;
            return this;
        }

        /**
         * 添加自定义缓存逻辑
         */
        public CacheQueryBuilder<T> withAdditionalCache(Consumer<T> cacheLogic) {
            this.additionalCacheLogic = cacheLogic;
            return this;
        }

        /**
         * 设置空缓存超时时间
         */
        public CacheQueryBuilder<T> nullCacheTimeout(long timeout, TimeUnit unit) {
            this.nullCacheTimeout = timeout;
            this.nullCacheTimeUnit = unit;
            return this;
        }

        /**
         * 执行查询
         */
        @SuppressWarnings("unchecked")
        public T execute() throws Exception {
            // 参数校验
            validateParams();

            // 1. 查主缓存
            Object cacheObj = redisTemplate.opsForHash().get(primaryCacheKey, field);
            if (cacheObj != null) {
                return (T) cacheObj;
            }

            // 2. 查空缓存
            if (Boolean.TRUE.equals(redisTemplate.hasKey(nullCacheKey))) {
                log.debug("Null cache hit for key: {}", nullCacheKey);
                return null;
            }

            // 3. 分布式锁

            String lockKey = RedisKey.REDISSON_PREFIX + resultType.getSimpleName();
            RLock lock = redissonClient.getLock(lockKey);
            boolean lockSuccess = false;

            try {
                lockSuccess = lock.tryLock(
                        RedisKey.TIME_OUT_HALF_MINUTE,
                        RedisKey.TIME_OUT_ONE_MINUTE,
                        TimeUnit.SECONDS);

                if (!lockSuccess) {
                    log.warn("Failed to acquire lock for field: {}", field);
                    return null;
                }

                // 4. Double Check
                cacheObj = redisTemplate.opsForHash().get(primaryCacheKey, field);
                if (cacheObj != null) {
                    return (T) cacheObj;
                }
                if (Boolean.TRUE.equals(redisTemplate.hasKey(nullCacheKey))) {
                    return null;
                }

                // 5. 查询数据库
                T result = dbSupplier.get();
                if (result == null) {
                    log.info("Data not exist for field: {}", field);
                    // 缓存空值
                    redisTemplate.opsForValue().set(
                            nullCacheKey,
                            RedisKey.EMPTY_VALUE,
                            nullCacheTimeout,
                            nullCacheTimeUnit
                    );
                    return null;
                }

                // 6. 批量写入缓存
                cacheResult(result);

                return result;

            } catch (Exception e) {
                log.error("Error getting data for field: {}", field, e);
                throw e;
            } finally {
                redisUtil.releaseLock(lock, lockSuccess);
            }
        }

        /**
         * 缓存结果
         */
        private void cacheResult(T result) {
            redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                RedisSerializer keySerializer = redisTemplate.getKeySerializer();
                RedisSerializer hashKeySerializer = redisTemplate.getHashKeySerializer();
                RedisSerializer hashValueSerializer = redisTemplate.getHashValueSerializer();

                List<Object> results = new ArrayList<>();

                // 缓存主键
                byte[] primaryKeyBytes = keySerializer.serialize(primaryCacheKey);
                byte[] fieldBytes = hashKeySerializer.serialize(field);
                byte[] valueBytes = hashValueSerializer.serialize(result);
                connection.hSet(primaryKeyBytes, fieldBytes, valueBytes);

                // 缓存二级索引（如username）
                if (enableSecondaryCache && secondaryFieldExtractor != null) {
                    String secondaryField = secondaryFieldExtractor.apply(result);
                    byte[] secondaryKeyBytes = keySerializer.serialize(secondaryCacheKey);
                    byte[] secondaryFieldBytes = hashKeySerializer.serialize(secondaryField);
                    connection.hSet(secondaryKeyBytes, secondaryFieldBytes, valueBytes);
                }
                return null;
            });

            // 自定义缓存逻辑
            if (additionalCacheLogic != null) {
                additionalCacheLogic.accept(result);
            }
        }

        private void validateParams() {
            Assert.notNull(primaryCacheKey, "Primary cache key must not be null");
            Assert.notNull(field, "Field must not be null");
            Assert.notNull(nullCacheKey, "Null cache key must not be null");
            Assert.notNull(dbSupplier, "DB supplier must not be null");
            Assert.notNull(resultType, "Result type must not be null");
        }
    }
}