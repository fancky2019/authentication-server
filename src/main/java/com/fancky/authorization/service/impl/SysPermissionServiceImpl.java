package com.fancky.authorization.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fancky.authorization.mapper.SysPermissionMapper;
import com.fancky.authorization.model.dto.PermissionDTO;
import com.fancky.authorization.model.entity.SysPermission;
import com.fancky.authorization.service.SysPermissionService;
import com.fancky.authorization.service.SysRolePermissionService;
import com.fancky.authorization.utility.RedisKey;
import com.fancky.authorization.utility.RedisUtil;
import com.fancky.authorization.utility.TransactionCallbackManager;
import com.fancky.authorization.utility.cache.RedisCacheService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SysPermissionServiceImpl extends ServiceImpl<SysPermissionMapper, SysPermission>
        implements SysPermissionService {

    @Autowired
    private SysPermissionMapper permissionMapper;

    @Autowired
    private SysRolePermissionService sysRolePermissionService;
    @Autowired
    private RedisCacheService redisCacheService;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private TransactionCallbackManager callbackManager;

    @Override
    public void initPermission() {
        log.info("start init Permission");
        redisTemplate.delete(RedisKey.PERMISSION_KEY);
        log.info("delete Permission complete");
        List<SysPermission> list = this.list();
        Map<String, SysPermission> map = list.stream().collect(Collectors.toMap(p -> p.getId().toString(), p -> p));
        redisTemplate.opsForHash().putAll(RedisKey.PERMISSION_KEY, map);
        log.info("init Permission complete");
    }


    @Override
    public List<SysPermission> getPermissionTree() {
        List<SysPermission> permissions = permissionMapper.selectList(
                new LambdaQueryWrapper<SysPermission>()
                        .orderByAsc(SysPermission::getSort)
        );

        return buildTree(permissions, 0L);
    }

    @Override
    public List<SysPermission> getUserPermissions(Long userId) {
        return permissionMapper.selectUserPermissions(userId);
    }

    @Override
    public SysPermission getPermissionById(Long id) {
        List<SysPermission> roleList = getPermissionByIds(Arrays.asList(id));
        if (CollectionUtils.isEmpty(roleList)) {
            return null;
        }
        return roleList.get(0);
    }

    @Override
    public List<SysPermission> getPermissionByIds(List<Long> idList) {
        if (CollectionUtils.isEmpty(idList)) {
            return Collections.emptyList();
        }
//        LambdaQueryWrapper<SysPermission> lambdaQueryWrapper = new LambdaQueryWrapper<>();
//        lambdaQueryWrapper.in(SysPermission::getId, idList);
//        return this.list(lambdaQueryWrapper);


        try {
            // 1. 批量查询
            List<SysPermission> list = redisCacheService
                    .<SysPermission, Long>batchBuilder()
                    .cache(RedisKey.PERMISSION_KEY, idList)
                    .db(
                            missIds -> {
                                // 分批查询数据库
                                LambdaQueryWrapper<SysPermission> lambdaQueryWrapper = new LambdaQueryWrapper<>();
                                lambdaQueryWrapper.in(SysPermission::getId, idList);
                                return this.list(lambdaQueryWrapper);
                            },
                            SysPermission::getId,
                            SysPermission.class  // 添加resultType
                    )
                    .nullCache(RedisKey.PERMISSION_NULL_KEY)  // 建议添加空值缓存
//                    .nullCacheTimeout(30, TimeUnit.SECONDS)
//                    .withDbBatchSize(100)  // 数据库分批大小
//                    .enableMetrics(true)    // 启用性能监控
                    .execute();

            // 2. 检查结果（只检查传入的ID是否都有返回）
            Set<Long> foundIds = list.stream()
                    .filter(Objects::nonNull)
                    .map(SysPermission::getId)
                    .collect(Collectors.toSet());

            List<Long> notFoundIds = idList.stream()
                    .filter(id -> !foundIds.contains(id))
                    .collect(Collectors.toList());

            if (!notFoundIds.isEmpty()) {
                log.warn("Some role ids not found: {}", StringUtils.join(notFoundIds, ","));
                // 根据业务需求决定是否抛异常
                // throw new Exception("Can't get role info for ids: " + notFoundIds);
            }

            return list;

        } catch (Exception e) {
            log.error("Failed to get SysPermission by ids: {}", StringUtils.join(idList, ","), e);
            throw new RuntimeException("Failed to get roles by ids", e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addPermission(PermissionDTO permissionDTO) throws Exception {

        //此处不加redisson 锁，数据库使用permissionCode 唯一索引兜底
        // 检查权限标识是否重复
        if (StringUtils.isNotEmpty(permissionDTO.getPermissionCode())) {
            Map<String, SysPermission> permissionMap = this.redisUtil.getHash(RedisKey.PERMISSION_KEY, SysPermission.class);
            boolean existPermissionCode = false;
            for (SysPermission permission : permissionMap.values()) {

                if (permissionDTO.getPermissionCode().equals(permission.getPermissionCode())) {
                    existPermissionCode = true;
                    break;
                }
            }
            if (existPermissionCode) {
                throw new RuntimeException("权限标识已存在");
            }
        }

        redisTemplate.delete(RedisKey.PERMISSION_KEY);
        SysPermission permission = new SysPermission();
        permission.setParentId(permissionDTO.getParentId() != null ? permissionDTO.getParentId() : 0L);
        permission.setPermissionName(permissionDTO.getPermissionName());
        permission.setPermissionType(permissionDTO.getPermissionType());
        permission.setPermissionCode(permissionDTO.getPermissionCode());
        permission.setPermissionValue(permissionDTO.getPermissionValue());
        permission.setPath(permissionDTO.getPath());
        permission.setComponent(permissionDTO.getComponent());
        permission.setIcon(permissionDTO.getIcon());
        permission.setSort(permissionDTO.getSort() != null ? permissionDTO.getSort() : 0);
        permission.setVisible(permissionDTO.getVisible() != null ? permissionDTO.getVisible() : 1);
        permission.setStatus(permissionDTO.getStatus() != null ? permissionDTO.getStatus() : 1);
        permission.setRemark(permissionDTO.getRemark());

//        int affectRow = permissionMapper.insert(permission);
        boolean success = this.save(permission);
        if (success) {
//            // 3. 注册事务回调 - 方式1：链式调用
//            callbackManager.register()
////                .releaseLock(lock, lockSuccessfully)
//                    .deleteCache(RedisKey.PERMISSION_KEY)
//                    .onCommit(() -> {
//                        // 事务提交后，可以发送MQ消息通知其他服务
//                        // log.info("Permission added, sending notification...");
//                        // sendPermissionChangeNotification();
//                    })
//                    .onRollback(() -> {
//                        // 事务回滚后，可以做些补偿操作
//                        // log.warn("Permission addition rolled back");
//                    })
//                    .execute();
        }
        return success;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updatePermission(PermissionDTO permissionDTO) throws Exception {
        SysPermission permission = permissionMapper.selectById(permissionDTO.getId());
        if (permission == null) {
            throw new RuntimeException("权限不存在");
        }

        // 检查权限标识是否重复
        if (permissionDTO.getPermissionCode() != null &&
                !permissionDTO.getPermissionCode().equals(permission.getPermissionCode())) {
            Long count = permissionMapper.selectCount(new LambdaQueryWrapper<SysPermission>()
                    .eq(SysPermission::getPermissionCode, permissionDTO.getPermissionCode()));
            if (count > 0) {
                throw new RuntimeException("权限标识已存在");
            }
        }

        // 不能将父节点设置为自己的子节点
        if (permissionDTO.getParentId() != null && permissionDTO.getParentId().equals(permissionDTO.getId())) {
            throw new RuntimeException("不能将父节点设置为自己");
        }

        permission.setParentId(permissionDTO.getParentId());
        permission.setPermissionName(permissionDTO.getPermissionName());
        permission.setPermissionType(permissionDTO.getPermissionType());
        permission.setPermissionCode(permissionDTO.getPermissionCode());
        permission.setPermissionValue(permissionDTO.getPermissionValue());
        permission.setPath(permissionDTO.getPath());
        permission.setComponent(permissionDTO.getComponent());
        permission.setIcon(permissionDTO.getIcon());
        permission.setSort(permissionDTO.getSort());
        permission.setVisible(permissionDTO.getVisible());
        permission.setStatus(permissionDTO.getStatus());
        permission.setRemark(permissionDTO.getRemark());
        this.redisTemplate.opsForHash().delete(RedisKey.PERMISSION_KEY, permissionDTO.getId().toString());
        boolean success =  permissionMapper.updateById(permission) > 0;

        if (success) {
            // 3. 注册事务回调 - 方式1：链式调用
            callbackManager.register()
//                .releaseLock(lock, lockSuccessfully)
//                    .deleteCache(RedisKey.USER_ROLE_KEY, RedisKey.USER_ROLE_USER_KEY,
//                            RedisKey.ROLE_PERMISSION_KEY, RedisKey.ROLE_PERMISSION_ROLE_KEY)
                    .onCommit(() -> {
                        // 事务提交后，可以发送MQ消息通知其他服务
                        // log.info("Permission added, sending notification...");
                        // sendPermissionChangeNotification();
                        this.redisTemplate.opsForHash().delete(RedisKey.PERMISSION_KEY, permissionDTO.getId().toString());
                    })
                    .onRollback(() -> {
                        // 事务回滚后，可以做些补偿操作
                        // log.warn("Permission addition rolled back");
                    })
                    .execute();
        }
        return success;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deletePermission(Long id) throws Exception {
        // 检查是否有子节点
        if (hasChildren(id)) {
            throw new RuntimeException("存在子权限，无法删除");
        }

//        // 检查是否被角色使用
//        Long count = permissionMapper.countRolePermission(id);
//        if (count > 0) {
//            throw new RuntimeException("该权限已被角色使用，无法删除");
//        }

      boolean removeRolePermissionSuccess=  this.sysRolePermissionService.removeByPermissionIds(Arrays.asList(id));
        if(!removeRolePermissionSuccess)
        {
            log.info("removeByPermissionIds fail");
            return false;

        }
        this.redisTemplate.opsForHash().delete(RedisKey.PERMISSION_KEY,id.toString());
        boolean success=  this.removeById(id);
        if (success) {
            // 3. 注册事务回调 - 方式1：链式调用
            callbackManager.register()
//                .releaseLock(lock, lockSuccessfully)
                    //此处优化成 删除 角色key
//                        .deleteCache(RedisKey.ROLE_PERMISSION_KEY, RedisKey.ROLE_PERMISSION_ROLE_KEY)
                    .onCommit(() -> {
                        // 事务提交后，可以发送MQ消息通知其他服务
                        // log.info("Permission added, sending notification...");

                        this.redisTemplate.opsForHash().delete(RedisKey.PERMISSION_KEY,id.toString());
                    })
                    .onRollback(() -> {
                        // 事务回滚后，可以做些补偿操作
                        // log.warn("Permission addition rolled back");
                    })
                    .execute();
        }
        return success;
    }




    @Override
    public boolean updateStatus(Long id, Integer status) {
        SysPermission permission = new SysPermission();
        permission.setId(id);
        permission.setStatus(status);
        return permissionMapper.updateById(permission) > 0;
    }

    @Override
    public boolean hasChildren(Long id) {
        Long count = permissionMapper.selectCount(
                new LambdaQueryWrapper<SysPermission>()
                        .eq(SysPermission::getParentId, id)
        );
        return count > 0;
    }

    private List<SysPermission> buildTree(List<SysPermission> permissions, Long parentId) {
        return permissions.stream()
                .filter(p -> p.getParentId().equals(parentId))
                .peek(p -> p.setChildren(buildTree(permissions, p.getId())))
                .collect(Collectors.toList());
    }
}
