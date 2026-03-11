package com.fancky.authorization.utility;


import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class RedisUtil {
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private RedissonClient redissonClient;

    public Object getKeyWithLock(String key) {
        log.info("getKeyWithLock {}", key);

        ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();
        String operationLockKey = key + RedisKey.KEY_LOCK_SUFFIX;
        //并发访问，加锁控制，此方法内没有事务操作。可以用try finally 释放资源 否则用 MqSendUtil releaseLock 方法
        RLock lock = redissonClient.getLock(operationLockKey);
        boolean lockSuccessfully = false;
        try {
            long waitTime = 10;
            lockSuccessfully = lock.tryLock(waitTime, TimeUnit.SECONDS);
            if (lockSuccessfully) {
                Object val = valueOperations.get(key);
                return val;
            } else {
                //如果controller是void 返回类型，此处返回 MessageResult<Void>  也不会返回给前段
                //超过waitTime ，扔未获得锁
                log.info("getKeyWithLock: {} 获取锁失败", key);
                return null;
            }
        } catch (Exception e) {
            // throw  e;
            log.error("", e);
            return null;
        } finally {
            releaseLock(lock, lockSuccessfully);
        }
    }

    public void setKeyWithLock(String key, Object keyVal) {
        log.info("setKeyWithLock {}", key);

        ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();
        String operationLockKey = key + RedisKey.KEY_LOCK_SUFFIX;
        //并发访问，加锁控制，此方法内没有事务操作。可以用try finally 释放资源 否则用 MqSendUtil releaseLock 方法
        RLock lock = redissonClient.getLock(operationLockKey);
        boolean lockSuccessfully = false;
        try {
            long waitTime = 10;
            lockSuccessfully = lock.tryLock(waitTime, TimeUnit.SECONDS);
            if (lockSuccessfully) {
                valueOperations.set(key, keyVal);
            } else {
                //如果controller是void 返回类型，此处返回 MessageResult<Void>  也不会返回给前段
                //超过waitTime ，扔未获得锁
                log.info("getKeyWithLock: {} 获取锁失败", key);

            }
        } catch (Exception e) {
            // throw  e;
            log.error("", e);

        } finally {
            releaseLock(lock, lockSuccessfully);
        }
    }

    public <HK, HV> Map<HK, HV> getHashEntries(String key, Collection<HK> hashKeys) {
        List<HV> values = redisTemplate.<HK, HV>opsForHash().multiGet(key, hashKeys);
        Map<HK, HV> result = new HashMap<>();

        Iterator<HK> keyIter = hashKeys.iterator();
        Iterator<HV> valueIter = values.iterator();

        while (keyIter.hasNext() && valueIter.hasNext()) {
            result.put(keyIter.next(), valueIter.next());
        }

        return result;
    }

    public void releaseLockAfterTransaction(RLock lock, boolean lockSuccessfully) throws Exception {
        //处理事务回调发送信息到mq
        //boolean actualTransactionActive = TransactionSynchronizationManager.isActualTransactionActive();
        // 判断当前是否存在事务,如果没有开启事务是会报错的
        boolean isActualTransactionActive = TransactionSynchronizationManager.isActualTransactionActive();
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            // 无事务，非事务方法内会立即释放锁，在某些事务传播不支持事务的方法内会有并发问题。强制在事务内
//            releaseLock(lock, lockSuccessfully);
            throw new Exception("not in Transactional method");
//            return;
        }

        //事务回调：事务同步，此处待处理， 所有事务提交了才会执行 事务回调
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
            @Override
            public void afterCompletion(int status) {
                //先执行事务afterCommit，然后执行afterCompletion
                //afterCommit,afterCompletion
                //afterCompletion 事务完成
                // 调用父类的事务提交方法,空方法
                //   super.afterCompletion(status);

                //事务完成有可能是 回滚
//                int STATUS_COMMITTED = 0;
//                int STATUS_ROLLED_BACK = 1;
//                int STATUS_UNKNOWN = 2;
                releaseLock(lock, lockSuccessfully);


            }


        });

    }

    /**
     * Redisson 的分布式锁在 Redis 里是用 Hash 结构
     *redis key :redisson:updateTruckOrder:2
     *
     * hash field :clientId:threadId    Redisson 客户端唯一ID（这个 JVM 实例的身份:当前 JVM 里的线程 ID
     * hash field :47f74905-0865-48fe-973d-edd7db765977:217
     * hash value:重入次数 = 1
     * hash value:1
     * @param lock
     * @param lockSuccessfully
     */

    public void releaseLock(RLock lock, boolean lockSuccessfully) {
        if (lockSuccessfully && lock.isLocked() && lock.isHeldByCurrentThread()) {
            String lockName = lock.getName();
            try {
                log.info("start release lock, key: {}", lockName);
                lock.unlock();
                log.info("release lock success, key: {}", lockName);
            } catch (Exception e) {
                log.error("release fail " +lockName, e);
                //watchdog 30s ttl  ,停止续期 自动删
//                try {
//                    // 不校验现成持有，直接删除key
//                    lock.forceUnlock();
//                } catch (Exception ex) {
//                    log.error("forceUnlock fail", ex);
//                }
            }
        }


    }
}
