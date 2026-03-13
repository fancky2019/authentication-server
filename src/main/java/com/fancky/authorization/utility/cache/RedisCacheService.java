package com.fancky.authorization.utility.cache;

import com.fancky.authorization.utility.RedisKey;
import com.fancky.authorization.utility.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

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


    public <T, K> BatchCacheQueryBuilder<T, K> batchBuilder() {
        return new BatchCacheQueryBuilder<>();
    }


    public class BatchCacheQueryBuilder<T, K> {

        private String cacheKey;
        private List<K> fields;
        private Function<List<K>, List<T>> dbFunction;
        private Function<T, K> idExtractor;
        private Class<T> resultType;

        private String nullCacheKey;
        private long nullCacheTimeout = RedisKey.TIME_OUT_ONE_MINUTE;
        private TimeUnit nullCacheTimeUnit = TimeUnit.SECONDS;

        private int dbBatchSize = 100;
        private int pipelineBatchSize = 200;
        private boolean skipNullCache = false;
        private boolean enableMetrics = false;

        private static final String EMPTY_VALUE = RedisKey.EMPTY_VALUE;

        // Builder methods
        public BatchCacheQueryBuilder<T, K> cache(String cacheKey, List<K> fields) {
            this.cacheKey = cacheKey;
            this.fields = fields;
            return this;
        }

        public BatchCacheQueryBuilder<T, K> db(Function<List<K>, List<T>> dbFunction,
                                               Function<T, K> idExtractor,
                                               Class<T> resultType) {
            this.dbFunction = dbFunction;
            this.idExtractor = idExtractor;
            this.resultType = resultType;
            return this;
        }

        public BatchCacheQueryBuilder<T, K> nullCache(String nullCacheKey) {
            this.nullCacheKey = nullCacheKey;
            return this;
        }

        public BatchCacheQueryBuilder<T, K> nullCacheTimeout(long timeout, TimeUnit unit) {
            this.nullCacheTimeout = timeout;
            this.nullCacheTimeUnit = unit;
            return this;
        }

        public BatchCacheQueryBuilder<T, K> withDbBatchSize(int dbBatchSize) {
            this.dbBatchSize = dbBatchSize;
            return this;
        }

        public BatchCacheQueryBuilder<T, K> withPipelineBatchSize(int pipelineBatchSize) {
            this.pipelineBatchSize = pipelineBatchSize;
            return this;
        }

        public BatchCacheQueryBuilder<T, K> skipNullCache(boolean skip) {
            this.skipNullCache = skip;
            return this;
        }

        public BatchCacheQueryBuilder<T, K> enableMetrics(boolean enable) {
            this.enableMetrics = enable;
            return this;
        }

        @SuppressWarnings("unchecked")
        public List<T> execute() {
            long startTime = enableMetrics ? System.currentTimeMillis() : 0;

            try {
                if (CollectionUtils.isEmpty(fields)) {
                    log.debug("Empty fields list, returning empty result");
                    return Collections.emptyList();
                }

                validateParams();

                // 去重 + 过滤null
                List<K> distinctFields = distinctFields(fields);
                boolean hasDuplicates = distinctFields.size() != fields.size();

                if (hasDuplicates) {
                    log.debug("Fields list contains duplicates, deduplicated from {} to {}",
                            fields.size(), distinctFields.size());
                }

                // 执行核心逻辑
                Map<K, T> resultMap = executeInternal(distinctFields);

                return buildOrderedResult(resultMap);

            } finally {
                if (enableMetrics) {
                    long cost = System.currentTimeMillis() - startTime;
                    log.debug("Batch query executed in {} ms, fields count: {}", cost, fields.size());
                }
            }
        }

        /**
         * 字段去重
         */
        private List<K> distinctFields(List<K> fields) {
            return fields.stream()
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());
        }

        /**
         * 内部执行逻辑
         */
        private Map<K, T> executeInternal(List<K> queryFields) {
            Map<K, T> resultMap = new HashMap<>(queryFields.size() * 2);

            // 1. 批量查询主缓存
            List<Object> cacheList = multiGetFromHash(queryFields);

            // 2. 批量查询空值缓存
            Set<K> nullCacheSet = shouldCheckNullCache() ?
                    getNullCacheSetBatch(queryFields) : Collections.emptySet();

            // 3. 分析缓存命中情况
            List<K> missIds = analyzeCacheHits(queryFields, cacheList, nullCacheSet, resultMap);

            // 4. 处理未命中的数据（带分布式锁和双检锁）
            if (!missIds.isEmpty()) {
                loadFromDbWithLock(missIds, resultMap);
            }

            return resultMap;
        }

        /**
         * 批量查询Hash缓存
         */
        private List<Object> multiGetFromHash(List<K> fields) {
            try {
                // 1. 构建主缓存Map
                List<Object> strKeyList = fields.stream()
                        .map(id -> String.valueOf(id))
                        .collect(Collectors.toList());

                return redisTemplate.opsForHash().multiGet(cacheKey, strKeyList);
            } catch (Exception e) {
                log.error("Failed to multiGet from hash cache: {}", cacheKey, e);
                return Collections.emptyList();
            }
        }

        /**
         * 判断是否需要检查空值缓存
         */
        private boolean shouldCheckNullCache() {
            return !skipNullCache && nullCacheKey != null;
        }

        /**
         * 批量查询空值缓存
         */
        private Set<K> getNullCacheSetBatch(List<K> ids) {
            if (CollectionUtils.isEmpty(ids)) {
                return Collections.emptySet();
            }

            Set<K> nullSet = new HashSet<>(ids.size());

            try {
                List<List<K>> batches = ListUtils.partition(ids, pipelineBatchSize);

                for (List<K> batch : batches) {
                    List<String> keys = batch.stream()
                            .map(this::buildNullCacheKey)
                            .collect(Collectors.toList());

                    List<Object> values = redisTemplate.opsForValue().multiGet(keys);
                    if (values != null) {
                        for (int i = 0; i < batch.size(); i++) {
                            if (values.get(i) != null) {
                                nullSet.add(batch.get(i));
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Failed to query null cache: {}", nullCacheKey, e);
            }

            return nullSet;
        }

        /**
         * 构建空值缓存key
         */
        private String buildNullCacheKey(K id) {
            return nullCacheKey + ":" + id;
        }

        /**
         * 分析缓存命中情况
         */
        private List<K> analyzeCacheHits(List<K> queryFields, List<Object> cacheList,
                                         Set<K> nullCacheSet, Map<K, T> resultMap) {
            List<K> missIds = new ArrayList<>();

            for (int i = 0; i < queryFields.size(); i++) {
                K id = queryFields.get(i);
                Object obj = i < cacheList.size() ? cacheList.get(i) : null;

                if (obj != null) {
                    resultMap.put(id, (T) obj);
                    continue;
                }

                if (nullCacheSet.contains(id)) {
                    resultMap.put(id, null);
                    continue;
                }

                missIds.add(id);
            }

            return missIds;
        }

        /**
         * 带分布式锁的数据库加载（实现双检锁）- 保持原锁key不变
         */
        private void loadFromDbWithLock(List<K> missIds, Map<K, T> resultMap) {
            // 保持原锁key不变：使用resultType的简单类名
            String lockKey = RedisKey.REDISSON_PREFIX + resultType.getSimpleName();
            RLock lock = redissonClient.getLock(lockKey);
            boolean lockSuccess = false;

            try {
                lockSuccess = lock.tryLock(
                        RedisKey.TIME_OUT_HALF_MINUTE,
                        RedisKey.TIME_OUT_ONE_MINUTE,
                        TimeUnit.SECONDS);

                if (!lockSuccess) {
                    log.warn("Failed to acquire lock for key: {}, will load from DB without cache", lockKey);
                    // 锁失败时降级：直接查询数据库，不写缓存
                    loadFromDbWithoutCache(missIds, resultMap);
                    return;
                }

                // === 双检锁：重新检查缓存 ===
                log.debug("Acquired lock, performing double check for {} ids", missIds.size());

                // 1. 重新检查主缓存
                List<Object> recheckCacheList = multiGetFromHash(missIds);

                // 2. 重新检查空值缓存
                Set<K> recheckNullSet = shouldCheckNullCache() ?
                        getNullCacheSetBatch(missIds) : Collections.emptySet();

                // 3. 重新分析未命中ID
                List<K> stillMissIds = new ArrayList<>();
                for (int i = 0; i < missIds.size(); i++) {
                    K id = missIds.get(i);
                    Object obj = i < recheckCacheList.size() ? recheckCacheList.get(i) : null;

                    if (obj != null) {
                        // 命中主缓存
                        resultMap.put(id, (T) obj);
                        log.debug("Double check hit main cache for id: {}", id);
                    } else if (recheckNullSet.contains(id)) {
                        // 命中空值缓存
                        resultMap.put(id, null);
                        log.debug("Double check hit null cache for id: {}", id);
                    } else {
                        // 仍然未命中
                        stillMissIds.add(id);
                    }
                }

                // 4. 如果还有未命中的，才查询数据库
                if (!stillMissIds.isEmpty()) {
                    log.debug("Double check still miss {} ids, loading from DB and caching", stillMissIds.size());
                    loadFromDbAndCache(stillMissIds, resultMap);
                } else {
                    log.debug("Double check hit all caches, skip DB query");
                }

            } catch (InterruptedException e) {
                log.error("Interrupted while acquiring lock for key: {}", lockKey, e);
                Thread.currentThread().interrupt();
                // 中断时降级
                loadFromDbWithoutCache(missIds, resultMap);
            } catch (Exception e) {
                log.error("Error in loadFromDbWithLock for key: {}", lockKey, e);
                // 异常时降级
                loadFromDbWithoutCache(missIds, resultMap);
            } finally {
                if (lockSuccess) {
                    redisUtil.releaseLock(lock, true);
                }
            }
        }

        /**
         * 查询数据库并写入缓存
         */
        private void loadFromDbAndCache(List<K> missIds, Map<K, T> resultMap) {
            List<List<K>> batches = ListUtils.partition(missIds, dbBatchSize);
            int totalBatches = batches.size();
            int currentBatch = 0;

            for (List<K> batch : batches) {
                currentBatch++;
                long startTime = enableMetrics ? System.currentTimeMillis() : 0;

                try {
                    // 查询数据库
                    List<T> dbList = dbFunction.apply(batch);

                    if (enableMetrics) {
                        long cost = System.currentTimeMillis() - startTime;
                        log.debug("DB batch {}/{} executed in {} ms, batch size: {}, found: {}",
                                currentBatch, totalBatches, cost, batch.size(),
                                dbList != null ? dbList.size() : 0);
                    }

                    // 构建foundMap
                    Map<K, T> foundMap = buildFoundMap(dbList);

                    // 更新resultMap
                    for (Map.Entry<K, T> entry : foundMap.entrySet()) {
                        resultMap.put(entry.getKey(), entry.getValue());
                    }

                    // 批量写入缓存
                    writeToCache(batch, foundMap);

                } catch (Exception e) {
                    log.error("Failed to load from DB for batch {}/{}: {}",
                            currentBatch, totalBatches, batch, e);
                    // 继续处理下一批，当前批的数据在结果中为null
                }
            }
        }

        /**
         * 降级方法：只查询数据库，不写缓存
         */
        private void loadFromDbWithoutCache(List<K> missIds, Map<K, T> resultMap) {
            log.warn("Fallback to load from DB without cache for {} ids", missIds.size());

            List<List<K>> batches = ListUtils.partition(missIds, dbBatchSize);

            for (List<K> batch : batches) {
                try {
                    List<T> dbList = dbFunction.apply(batch);
                    Map<K, T> foundMap = buildFoundMap(dbList);

                    for (Map.Entry<K, T> entry : foundMap.entrySet()) {
                        resultMap.put(entry.getKey(), entry.getValue());
                    }
                } catch (Exception e) {
                    log.error("Failed to load from DB for batch: {}", batch, e);
                }
            }
        }

        /**
         * 构建foundMap
         */
        private Map<K, T> buildFoundMap(List<T> dbList) {
            if (CollectionUtils.isEmpty(dbList)) {
                return Collections.emptyMap();
            }

            Map<K, T> foundMap = new HashMap<>(dbList.size());
            for (T obj : dbList) {
                if (obj != null) {
                    K id = idExtractor.apply(obj);
                    if (id != null) {
                        foundMap.put(id, obj);
                    }
                }
            }
            return foundMap;
        }


        /**
         *
         */
        private void writeToCache(List<K> batch, Map<K, T> foundMap) {
            if (batch.isEmpty()) {
                return;
            }

            try {
                // 1. 构建主缓存Map
                Map<String, T> mainCacheMap = batch.stream()
                        .filter(id -> foundMap.get(id) != null)
                        .collect(Collectors.toMap(
                                id -> String.valueOf(id),
                                foundMap::get
                        ));

                // 2. 写入主缓存
                if (!mainCacheMap.isEmpty()) {
                    redisTemplate.opsForHash().putAll(cacheKey, mainCacheMap);
                }

                // 3. 写入空值缓存
                if (nullCacheKey != null) {
                    List<K> nullEntries = batch.stream()
                            .filter(id -> foundMap.get(id) == null)
                            .collect(Collectors.toList());

                    if (!nullEntries.isEmpty()) {
                        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
//                            byte[] emptyBytes = redisTemplate.getStringSerializer().serialize(EMPTY_VALUE);
                            long timeout = nullCacheTimeUnit.toSeconds(nullCacheTimeout);

                            for (K id : nullEntries) {
                                String nullKey = buildNullCacheKey(id);
                                byte[] keyBytes = redisTemplate.getStringSerializer().serialize(nullKey);
                                byte[] emptyBytes = redisTemplate.getStringSerializer().serialize(String.valueOf(id));
                                connection.setEx(keyBytes, timeout, emptyBytes);
                            }
                            return null;
                        });
                    }
                }

            } catch (Exception e) {
                log.error("Failed to write to cache", e);
            }
        }

        /**
         * 构建有序结果
         */
        private List<T> buildOrderedResult(Map<K, T> resultMap) {
            List<T> result = new ArrayList<>(fields.size());

            for (K id : fields) {
                T value = resultMap.get(id);
                result.add(value);
            }

            return result;
        }

        /**
         * 参数校验
         */
        private void validateParams() {
            Assert.notNull(redisTemplate, "RedisTemplate must not be null");
            Assert.hasText(cacheKey, "Cache key must not be empty");
            Assert.notEmpty(fields, "Fields must not be empty");
            Assert.notNull(dbFunction, "DB function must not be null");
            Assert.notNull(idExtractor, "ID extractor must not be null");
            Assert.notNull(resultType, "Result type must not be null");

            if (dbBatchSize <= 0) {
                throw new IllegalArgumentException("DB batch size must be positive");
            }
            if (pipelineBatchSize <= 0) {
                throw new IllegalArgumentException("Pipeline batch size must be positive");
            }
        }
    }
}