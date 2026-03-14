package com.fancky.authorization.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fancky.authorization.mapper.SysRolePermissionMapper;
import com.fancky.authorization.model.dto.RolePermissionDto;
import com.fancky.authorization.model.entity.SysRole;
import com.fancky.authorization.model.entity.SysRolePermission;
import com.fancky.authorization.service.SysRolePermissionService;
import com.fancky.authorization.utility.RedisKey;
import com.fancky.authorization.utility.RedisUtil;
import com.fancky.authorization.utility.TransactionCallbackManager;
import com.fancky.authorization.utility.cache.RedisCacheService;
import lombok.RequiredArgsConstructor;
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
public class SysRolePermissionServiceImpl extends ServiceImpl<SysRolePermissionMapper, SysRolePermission>
        implements SysRolePermissionService {

    @Autowired
    private SysRolePermissionMapper rolePermissionMapper;

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
    public List<Long> getPermissionIdsByRoleId(Long roleId) {
        return rolePermissionMapper.selectPermissionIdsByRoleId(roleId);
    }

    @Override
    public List<SysRolePermission> getPermissionsByRoleIds(List<Long> roleIdList) {
        if (CollectionUtils.isEmpty(roleIdList)) {
            return Collections.emptyList();
        }
//        LambdaQueryWrapper<SysRolePermission> lambdaQueryWrapper = new LambdaQueryWrapper<>();
//        lambdaQueryWrapper.in(SysRolePermission::getRoleId, roleIdList);
//        return this.list(lambdaQueryWrapper);


        // 此处保存的是set  ,通用保存的是hashmap.


        try {
            // 1. 批量查询
            List<SysRolePermission> dataList = redisCacheService.<SysRolePermission, Long>listBatchBuilder()
                    .cache(RedisKey.ROLE_PERMISSION_ROLE_KEY, roleIdList)
                    .db(
                            missIds -> {
                                // 分批查询数据库
                                LambdaQueryWrapper<SysRolePermission> wrapper = new LambdaQueryWrapper<>();
                                wrapper.in(SysRolePermission::getRoleId, missIds);
                                return this.list(wrapper);
                            },
                            SysRolePermission::getRoleId,
                            SysRolePermission.class
                    )
                    .nullCache(RedisKey.ROLE_PERMISSION_ROLE_NULL_KEY)
//                    .nullCacheTimeout(30, TimeUnit.MINUTES)
//                    .withDbBatchSize(100)
//                    .enableMetrics(true)
                    .returnEmptyList(true)  // 没有权限时返回空List而不是null
                    .execute();

            // 2. 检查结果（只检查传入的ID是否都有返回）
            Set<Long> foundIds = dataList.stream()
                    .filter(Objects::nonNull)
                    .map(SysRolePermission::getRoleId)
                    .collect(Collectors.toSet());

            List<Long> notFoundIds = roleIdList.stream()
                    .filter(id -> !foundIds.contains(id))
                    .collect(Collectors.toList());

            if (!notFoundIds.isEmpty()) {
                log.warn("Some role ids not found: {}", StringUtils.join(notFoundIds, ","));
                // 根据业务需求决定是否抛异常
                // throw new Exception("Can't get role info for ids: " + notFoundIds);
            }

            return dataList;

        } catch (Exception e) {
            log.error("Failed to get roles by ids: {}", StringUtils.join(roleIdList, ","), e);
            throw new RuntimeException("Failed to get roles by ids", e);
        }
    }

    @Override
    public List<Long> getRoleIdsByPermissionId(Long permissionId) {
        return rolePermissionMapper.selectRoleIdsByPermissionId(permissionId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean assignPermissionsToRole(Long roleId, List<Long> permissionIds) {
        // 删除角色原有的权限关联
        rolePermissionMapper.deleteByRoleId(roleId);

        if (permissionIds == null || permissionIds.isEmpty()) {
            return true;
        }

        // 创建新的权限关联
        List<SysRolePermission> rolePermissions = permissionIds.stream()
                .map(permissionId -> {
                    SysRolePermission rolePermission = new SysRolePermission();
                    rolePermission.setRoleId(roleId);
                    rolePermission.setPermissionId(permissionId);
                    return rolePermission;
                })
                .collect(Collectors.toList());

        return rolePermissionMapper.insertBatch(rolePermissions) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addRolePermission(RolePermissionDto dto) throws Exception {
        // 删除权限原有的角色关联
//        rolePermissionMapper.deleteByPermissionId(permissionId);

        Long permissionId = dto.getPermissionId();
        List<Long> roleIds = dto.getRoleIds();
        if (CollectionUtils.isEmpty(roleIds)) {
            return false;
        }
        redisTemplate.delete(Arrays.asList(
                RedisKey.ROLE_PERMISSION_KEY,
                RedisKey.ROLE_PERMISSION_ROLE_KEY
        ));
        // 创建新的角色关联
        List<SysRolePermission> rolePermissions = roleIds.stream()
                .map(roleId -> {
                    SysRolePermission rolePermission = new SysRolePermission();
                    rolePermission.setRoleId(roleId);
                    rolePermission.setPermissionId(permissionId);
                    return rolePermission;
                })
                .collect(Collectors.toList());

        boolean success = this.saveBatch(rolePermissions);
        // 3. 注册事务回调 - 方式1：链式调用
        callbackManager.register()
//                .releaseLock(lock, lockSuccessfully)
                .deleteCache(RedisKey.PERMISSION_KEY)
                .onCommit(() -> {
                    // 事务提交后，可以发送MQ消息通知其他服务
                    // log.info("Permission added, sending notification...");
                    // sendPermissionChangeNotification();
                })
                .onRollback(() -> {
                    // 事务回滚后，可以做些补偿操作
                    // log.warn("Permission addition rolled back");
                })
                .execute();
        return success;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeRolePermission(Long roleId, Long permissionId) {
        return remove(new LambdaQueryWrapper<SysRolePermission>()
                .eq(SysRolePermission::getRoleId, roleId)
                .eq(SysRolePermission::getPermissionId, permissionId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeRolePermissions(Long roleId) {
        return remove(new LambdaQueryWrapper<SysRolePermission>()
                .eq(SysRolePermission::getRoleId, roleId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removePermissionRoles(Long permissionId) {
        return remove(new LambdaQueryWrapper<SysRolePermission>()
                .eq(SysRolePermission::getPermissionId, permissionId));
    }

    @Override
    public boolean hasPermission(Long roleId, Long permissionId) {
        return count(new LambdaQueryWrapper<SysRolePermission>()
                .eq(SysRolePermission::getRoleId, roleId)
                .eq(SysRolePermission::getPermissionId, permissionId)) > 0;
    }

    @Override
    public List<SysRolePermission> getRolePermissions(Long roleId) {
        return list(new LambdaQueryWrapper<SysRolePermission>()
                .eq(SysRolePermission::getRoleId, roleId));
    }
}
