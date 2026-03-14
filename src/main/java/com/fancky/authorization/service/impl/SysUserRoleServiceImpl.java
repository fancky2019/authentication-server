package com.fancky.authorization.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fancky.authorization.mapper.SysUserRoleMapper;
import com.fancky.authorization.model.entity.SysRolePermission;
import com.fancky.authorization.model.entity.SysUserRole;
import com.fancky.authorization.service.SysUserRoleService;
import com.fancky.authorization.utility.RedisKey;
import com.fancky.authorization.utility.RedisUtil;
import com.fancky.authorization.utility.cache.RedisCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SysUserRoleServiceImpl extends ServiceImpl<SysUserRoleMapper, SysUserRole> implements SysUserRoleService {

    @Autowired
    private SysUserRoleMapper userRoleMapper;

    @Autowired
    private RedisCacheService redisCacheService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private RedisUtil redisUtil;

    @Override
    public List<Long> getRoleIdsByUserId(Long userId) {
        return userRoleMapper.selectRoleIdsByUserId(userId);
    }

    @Override
    public List<Long> getUserIdsByRoleId(Long roleId) {
        return userRoleMapper.selectUserIdsByRoleId(roleId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean assignRolesToUser(Long userId, List<Long> roleIds) {
        // 删除用户原有的角色关联
        userRoleMapper.deleteByUserId(userId);

        if (roleIds == null || roleIds.isEmpty()) {
            return true;
        }

        // 创建新的角色关联
        List<SysUserRole> userRoles = roleIds.stream()
                .map(roleId -> {
                    SysUserRole userRole = new SysUserRole();
                    userRole.setUserId(userId);
                    userRole.setRoleId(roleId);
                    return userRole;
                })
                .collect(Collectors.toList());

        return userRoleMapper.insertBatch(userRoles) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean assignUsersToRole(Long roleId, List<Long> userIds) {
        // 删除角色原有的用户关联
        userRoleMapper.deleteByRoleId(roleId);

        if (userIds == null || userIds.isEmpty()) {
            return true;
        }

        // 创建新的用户关联
        List<SysUserRole> userRoles = userIds.stream()
                .map(userId -> {
                    SysUserRole userRole = new SysUserRole();
                    userRole.setUserId(userId);
                    userRole.setRoleId(roleId);
                    return userRole;
                })
                .collect(Collectors.toList());

        return userRoleMapper.insertBatch(userRoles) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeUserRole(Long userId, Long roleId) {
        return remove(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getUserId, userId)
                .eq(SysUserRole::getRoleId, roleId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeUserRoles(Long userId) {
        return remove(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getUserId, userId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeRoleUsers(Long roleId) {
        return remove(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getRoleId, roleId));
    }

    @Override
    public boolean hasRole(Long userId, Long roleId) {
        return count(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getUserId, userId)
                .eq(SysUserRole::getRoleId, roleId)) > 0;
    }

    @Override
    public List<SysUserRole> getUserRoles(Long userId) {
//        return list(new LambdaQueryWrapper<SysUserRole>()
//                .eq(SysUserRole::getUserId, userId));
//        redissonClient.

        List<Long> userIdList = Arrays.asList(userId);
        try {
            // 1. 批量查询
            List<SysUserRole> dataList = redisCacheService.<SysUserRole, Long>listBatchBuilder()
                    .cache(RedisKey.USER_ROLE_USER_KEY, userIdList)
                    .db(
                            missIds -> {
                                // 分批查询数据库
                                LambdaQueryWrapper<SysUserRole> wrapper = new LambdaQueryWrapper<>();
                                wrapper.in(SysUserRole::getUserId, missIds);
                                return this.list(wrapper);
                            }, // 返回 List<SysRolePermission>
                            SysUserRole::getUserId,                    // 提取roleId
                            SysUserRole.class
                    )
                    .nullCache(RedisKey.USER_ROLE_USER_NULL_KEY)
//                    .nullCacheTimeout(30, TimeUnit.MINUTES)
//                    .withDbBatchSize(100)
//                    .enableMetrics(true)
                    .returnEmptyList(true)  // 没有权限时返回空List而不是null
                    .execute();

            // 2. 检查结果（只检查传入的ID是否都有返回）
            Set<Long> foundIds = dataList.stream()
                    .filter(Objects::nonNull)
                    .map(SysUserRole::getUserId)
                    .collect(Collectors.toSet());

            List<Long> notFoundIds = userIdList.stream()
                    .filter(id -> !foundIds.contains(id))
                    .collect(Collectors.toList());

            if (!notFoundIds.isEmpty()) {
                log.warn("Some UserRole ids not found: {}", StringUtils.join(notFoundIds, ","));
                // 根据业务需求决定是否抛异常
                // throw new Exception("Can't get role info for ids: " + notFoundIds);
            }

            return dataList;

        } catch (Exception e) {
            log.error("Failed to get UserRole by ids: {}", StringUtils.join(userIdList, ","), e);
            throw new RuntimeException("Failed to get UserRole by ids", e);
        }
    }
}