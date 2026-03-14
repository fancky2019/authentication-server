package com.fancky.authorization.utility;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
public class TransactionCallbackManager {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private RedissonClient redissonClient;


    /**
     * 注册事务回调 - 链式调用
     */
    public TransactionCallbackBuilder register() {
        return new TransactionCallbackBuilder();
    }

    /**
     * 事务回调构建器
     */
    public class TransactionCallbackBuilder {
        private List<Runnable> commitCallbacks = new ArrayList<>();
        private List<Runnable> rollbackCallbacks = new ArrayList<>();
        private List<Runnable> completionCallbacks = new ArrayList<>();

        /**
         * 添加事务提交后执行的回调
         */
        public TransactionCallbackBuilder onCommit(Runnable callback) {
            this.commitCallbacks.add(callback);
            return this;
        }

        /**
         * 添加事务回滚后执行的回调
         */
        public TransactionCallbackBuilder onRollback(Runnable callback) {
            this.rollbackCallbacks.add(callback);
            return this;
        }

        /**
         * 添加事务完成后执行的回调（无论提交还是回滚）
         */
        public TransactionCallbackBuilder onCompletion(Runnable callback) {
            this.completionCallbacks.add(callback);
            return this;
        }

        /**
         * 添加释放锁的回调
         */
        public TransactionCallbackBuilder releaseLock(RLock lock, boolean lockSuccessfully) {
            this.completionCallbacks.add(() -> {
                if (lockSuccessfully && lock != null && lock.isHeldByCurrentThread()) {
                    try {
                        lock.unlock();
                        log.info("Lock released in transaction callback");
                    } catch (Exception e) {
                        log.error("Failed to release lock", e);
                    }
                }
            });
            return this;
        }

        /**
         * 添加删除缓存key的回调
         */
        public TransactionCallbackBuilder deleteCache(String... keys) {
            this.completionCallbacks.add(() -> {
                try {
                    redisTemplate.delete(Arrays.asList(keys));
                    log.info("Cache deleted in transaction callback: {}", Arrays.toString(keys));
                } catch (Exception e) {
                    log.error("Failed to delete cache", e);
                }
            });
            return this;
        }

        /**
         * 执行注册
         */
        public void execute() throws Exception {
            if (!TransactionSynchronizationManager.isSynchronizationActive()) {
                throw new Exception("not in Transactional method");
            }

            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronizationAdapter() {

                        @Override
                        public void afterCommit() {
                            commitCallbacks.forEach(Runnable::run);
                        }

                        @Override
                        public void afterCompletion(int status) {
                            if (status == STATUS_ROLLED_BACK) {
                                rollbackCallbacks.forEach(Runnable::run);
                            }
                            completionCallbacks.forEach(Runnable::run);

                            log.info("All transaction callbacks executed, status: {}",
                                    status == STATUS_COMMITTED ? "COMMITTED" :
                                            status == STATUS_ROLLED_BACK ? "ROLLED_BACK" : "UNKNOWN");
                        }
                    }
            );
        }
    }
}
