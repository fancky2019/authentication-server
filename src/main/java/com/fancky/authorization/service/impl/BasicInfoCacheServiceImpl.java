package com.fancky.authorization.service.impl;

import com.fancky.authorization.model.entity.*;
import com.fancky.authorization.service.*;
import com.fancky.authorization.utility.RedisKey;
import com.fancky.authorization.utility.RedisUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.core.*;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class BasicInfoCacheServiceImpl implements BasicInfoCacheService {
    //    @Autowired
//    @Lazy  // 防止循环依赖
//    private BasicInfoCacheService selfProxy;
    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private SysRoleService sysRoleService;

    @Autowired
    private SysUserRoleService sysUserRoleService;

    @Autowired
    private SysPermissionService sysPermissionService;

    @Autowired
    private SysRolePermissionService sysRolePermissionService;


    @Autowired
    private RedisTemplate redisTemplate;
//    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    @Qualifier("threadPoolExecutor")
    private Executor threadPoolExecutor;




    public static final int EMPTY_VALUE_EXPIRE_TIME = 5;
    //
//    // 缓存前缀
//    private static final String ID_PREFIX = MATERIAL_PREFIX + "id:";
//    private static final String CODE_PREFIX = MATERIAL_PREFIX + "code:";
    //__NULL__
    public static final String EMPTY_VALUE = "-1@.EmptyValue";

    //    @Override
    public void getBasicInfoCache() {
//        Map<String, Location> locationMap = redisTemplate.opsForHash().entries(BasicInfoCacheServiceImpl.LOCATION_PREFIX);
//        Location location = locationMap.get("509955478157011");
//        int m = 0;
    }

    @Override
    public void initUser() {
        log.info("start init User");
        redisTemplate.delete(RedisKey.USER_KEY);
        log.info("delete User complete");
        List<SysUser> list = this.sysUserService.list();

        Map<String, SysUser> map = list.stream().collect(Collectors.toMap(p -> p.getId().toString(), p -> p));
        //redis key  都是string
        HashOperations<String, String, SysUser> hashOps = redisTemplate.opsForHash();
        hashOps.putAll(RedisKey.USER_KEY, map);

        Map<String, SysUser> codeKeyMap = list.stream().collect(Collectors.toMap(p -> p.getUsername(), p -> p));
        //redis key  都是string
        hashOps.putAll(RedisKey.USER_CODE_KEY, codeKeyMap);

        log.info("init location complete");
    }


    @Override
    public void initRole() {
        log.info("start init Role");
        redisTemplate.delete(RedisKey.ROLE_KEY);
        log.info("delete Role complete");
        List<SysRole> list = this.sysRoleService.list();
        Map<String, SysRole> map = list.stream().collect(Collectors.toMap(p -> p.getId().toString(), p -> p));
        redisTemplate.opsForHash().putAll(RedisKey.ROLE_KEY, map);
        log.info("init Role complete");
    }

    @Override
    public void initUserRole() {
        log.info("start init UserRole");
        redisTemplate.delete(RedisKey.USER_ROLE_KEY);
        log.info("delete UserRole complete");
        List<SysUserRole> list = this.sysUserRoleService.list();
        Map<String, SysUserRole> map = list.stream().collect(Collectors.toMap(p -> p.getId().toString(), p -> p));
        redisTemplate.opsForHash().putAll(RedisKey.USER_ROLE_KEY, map);
        log.info("init UserRole complete");
    }


    @Override
    public void initUserPermission() {
        log.info("start init Permission");
        redisTemplate.delete(RedisKey.PERMISSION_KEY);
        log.info("delete Permission complete");
        List<SysPermission> list = this.sysPermissionService.list();
        Map<String, SysPermission> map = list.stream().collect(Collectors.toMap(p -> p.getId().toString(), p -> p));
        redisTemplate.opsForHash().putAll(RedisKey.PERMISSION_KEY, map);
        log.info("init Permission complete");
    }

    @Override
    public void initUserRolePermission() {
        log.info("start init RolePermission");
        redisTemplate.delete(RedisKey.ROLE_PERMISSION_KEY);
        log.info("delete RolePermission complete");
        List<SysRolePermission> list = this.sysRolePermissionService.list();
        Map<String, SysRolePermission> map = list.stream().collect(Collectors.toMap(p -> p.getId().toString(), p -> p));
        redisTemplate.opsForHash().putAll(RedisKey.ROLE_PERMISSION_KEY, map);
        log.info("init RolePermission complete");
    }


//    @Override
//    public Material loadFromDbMaterial(Long materialId) throws InterruptedException {
////        Material material = (Material) redisTemplate.opsForHash().get(BasicInfoCacheServiceImpl.materialPrefix, inventoryItemDetail.getMaterialId().toString());
//
////boolean locked = redisTemplate.opsForValue().setIfAbsent("lock:material:123", "1", 10, TimeUnit.SECONDS);
//
//
//        HashOperations<String, String, Material> hashOps = redisTemplate.opsForHash();
//        String key = MATERIAL_PREFIX;
//        Material material = (Material) hashOps.get(key, materialId.toString());
//        if (material == null) {
//
//            String lockKey = MATERIAL_PREFIX + "redisson";
//            //获取分布式锁，此处单体应用可用 synchronized，分布式就用redisson 锁
//            RLock lock = redissonClient.getLock(lockKey);
//            boolean lockSuccessfully = false;
//            try {
//
//                lockSuccessfully = lock.tryLock(30, 60, TimeUnit.SECONDS);
//                if (!lockSuccessfully) {
//                    log.info("Thread - {} 获得锁 {}失败！锁被占用！", Thread.currentThread().getId(), lockKey);
//
//                    //获取不到锁，抛异常处理 服务器繁忙，稍后重试
////                    throw new Exception("服务器繁忙，稍后重试");
//                    return null;
//                }
//                material = this.materialService.getById(materialId);
//                //穿透：设置个空值,待优化
//                if (material != null) {
//                    hashOps.put(key, materialId.toString(), material);
//                } else {
////                    穿透：设置个空值,待优化
//
//                    //混合key方案 :
//                    // 数据：用 Hash ,
//                    // 空值：用 String + TTL
//
//
////            Hash类型：只能对整个key设置过期时间（EXPIRE），不能对内部的field单独设置过期
////            String类型：可以单独设置每个key的过期时间
//                    //nullKey string 类型
////                    String nullKey = "material:null:id:" + id;
////                    // 1. 先判断是否命中过空缓存
////                    if (Boolean.TRUE.equals(redisTemplate.hasKey(nullKey))) {
////                        return null;
////                    }
////
////// 2. 查 hash
////                    Material m = (Material) redisTemplate.opsForHash()
////                            .get("material:id", id);
////                    if (m != null) {
////                        return m;
////                    }
////
////// 3. 查 DB
////                    Material db = materialService.getById(id);
////                    if (db == null) {
////                        // 缓存空值（有 TTL）
////                        redisTemplate.opsForValue().set(nullKey, "1", 60, TimeUnit.SECONDS);
////                        return null;
////                    }
//
//
//                }
//            } catch (Exception e) {
//                throw e;
//            } finally {
//                //解锁，如果业务执行完成，就不会继续续期，即使没有手动释放锁，在30秒过后，也会释放锁
//                //unlock 删除key
//                //如果锁因超时（leaseTime）会抛异常

    ////                lock.unlock();
//                redisUtil.releaseLock(lock, lockSuccessfully);
//            }
//        }
//        return material;
//    }
//
//
    @Override
    public void initBasicInfoCache() {
        /*
         * Hash/String 的 put/set 操作会覆盖
         *
         * List/Set 的 push/add 操作不会覆盖，而是追加
         */
        log.info("start initBasicInfoCache");
        BasicInfoCacheService basicInfoCacheService = applicationContext.getBean(BasicInfoCacheService.class);

        CompletableFuture<Void> initUserFuture = CompletableFuture.runAsync(basicInfoCacheService::initUser, threadPoolExecutor);
        CompletableFuture<Void> initRoleFuture = CompletableFuture.runAsync(basicInfoCacheService::initRole, threadPoolExecutor);
        CompletableFuture<Void> initUserRoleFuture = CompletableFuture.runAsync(basicInfoCacheService::initUserRole, threadPoolExecutor);
        CompletableFuture<Void> initUserPermissionFuture = CompletableFuture.runAsync(basicInfoCacheService::initUserPermission, threadPoolExecutor);
        CompletableFuture<Void> initUserRolePermissionFuture = CompletableFuture.runAsync(basicInfoCacheService::initUserRolePermission, threadPoolExecutor);

        // 等待所有任务完成
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                initUserFuture,
                initRoleFuture,
                initUserRoleFuture,
                initUserPermissionFuture,
                initUserRolePermissionFuture
        );

        // 阻塞等待所有任务完成
        allFutures.join();

        log.info("initBasicInfoCache complete");
    }

    //region redis

    /**
     * * 雪崩：随机过期时间
     * * 击穿：分布式锁（表名），没有取到锁，sleep(50)+重试 .获取不到锁，抛异常处理 服务器繁忙，稍后重试
     * * 穿透：分布式锁（表名）+设置一段时间的null值，没有取到锁，sleep(50)+重试
     *
     * @param id
     * @return
     * @throws Exception
     */
    public String loadFromDb(@PathVariable int id) throws Exception {
//        ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();
//        String key = ConfigConst.DEMO_PRODUCT_PREFIX + id;
//        String val = valueOperations.get(key);
//        if (StringUtils.isEmpty(val)) {
//
//            String lockKey = DEMO_PRODUCT_PREFIX + "redisson";
//            //获取分布式锁，此处单体应用可用 synchronized，分布式就用redisson 锁
//            RLock lock = redissonClient.getLock(lockKey);
//            try {
//
//                boolean lockSuccessfully = lock.tryLock(30, 60, TimeUnit.SECONDS);
//                if (!lockSuccessfully) {
//                    log.info("Thread - {} 获得锁 {}失败！锁被占用！", Thread.currentThread().getId(), lockKey);
//
//                    //获取不到锁，抛异常处理 服务器繁忙，稍后重试
////                    throw new Exception("服务器繁忙，稍后重试");
//                    return null;
//                }
//                BigInteger idB = BigInteger.valueOf(id);
//                ProductTest productTest = this.getById(idB);
//                //穿透：设置个空值
//                if (productTest == null) {
//                    valueOperations.set(key, EMPTY_VALUE);
//                    redisTemplate.expire(key, 60, TimeUnit.SECONDS);
//                } else {
//                    String json = objectMapper.writeValueAsString(productTest);
//                    //要设置个过期时间
//                    valueOperations.set(key, json);
//                    //[100,2000)
//                    long expireTime = ThreadLocalRandom.current().nextInt(3600, 24 * 3600);
//                    redisTemplate.expire(key, expireTime, TimeUnit.SECONDS);
//                }
//            } catch (Exception e) {
//                throw e;
//            } finally {
//                //解锁，如果业务执行完成，就不会继续续期，即使没有手动释放锁，在30秒过后，也会释放锁
//                //unlock 删除key
//                //如果锁因超时（leaseTime）会抛异常
//                lock.unlock();
//            }
//
//
//        } else {
//            if (EMPTY_VALUE.equals(val)) {
//                return null;
//            }
//        }
//
//
//        return val;
        return "";
    }

    @Override
    public boolean getSbpEnable() {
        ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();
        Object val = valueOperations.get(RedisKey.SBP_ENABLE);
        return val != null && val.equals(1);
    }

    @Override
    public Object getStringKey(String key) {
        ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();
        Object val = valueOperations.get(key);
        return val;
    }


    @Override
    public void setSbpEnable() {
        ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();
        valueOperations.set(RedisKey.SBP_ENABLE, 1, 3600, TimeUnit.SECONDS);
    }

    @Override
    public void setKeyVal(String keyVal, Object val) {
        ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();
        valueOperations.set(keyVal, val);
    }

    @Override
    public void setKeyValExpire(String keyVal, Object val, long timeout, TimeUnit unit) {
        ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();
        valueOperations.set(keyVal, val, timeout, unit);
    }
}
