package com.fancky.authorization.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.fancky.authorization.mapper.SysRoleMapper;
import com.fancky.authorization.mapper.SysRolePermissionMapper;
import com.fancky.authorization.model.dto.RoleDTO;
import com.fancky.authorization.model.entity.SysPermission;
import com.fancky.authorization.model.entity.SysRole;
import com.fancky.authorization.model.entity.SysRolePermission;
import com.fancky.authorization.model.entity.SysUserRole;
import com.fancky.authorization.model.response.PageVO;
import com.fancky.authorization.service.SysRolePermissionService;
import com.fancky.authorization.service.SysRoleService;
import com.fancky.authorization.utility.RedisKey;
import com.fancky.authorization.utility.RedisUtil;
import com.fancky.authorization.utility.TransactionCallbackManager;
import com.fancky.authorization.utility.cache.RedisCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.apache.commons.lang3.StringUtils;


import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements SysRoleService {

    @Autowired
    private SysRoleMapper roleMapper;
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

    @Autowired
    private SysRolePermissionService sysRolePermissionService;


    @Override
    public void initRole() {
        log.info("start init Role");
        redisTemplate.delete(RedisKey.ROLE_KEY);
        log.info("delete Role complete");
        List<SysRole> list = this.list();
        Map<String, SysRole> map = list.stream().collect(Collectors.toMap(p -> p.getId().toString(), p -> p));
        redisTemplate.opsForHash().putAll(RedisKey.ROLE_KEY, map);
        log.info("init Role complete");
    }

    @Override
    public PageVO<SysRole> getRolePage(RoleDTO roleDTO) {
        Page<SysRole> page = new Page<>(roleDTO.getCurrent(), roleDTO.getSize());

        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.isNotEmpty(roleDTO.getRoleCode()), SysRole::getRoleCode, roleDTO.getRoleCode())
                .like(StringUtils.isNotEmpty(roleDTO.getRoleName()), SysRole::getRoleName, roleDTO.getRoleName())
                .eq(roleDTO.getStatus() != null, SysRole::getStatus, roleDTO.getStatus())
                .orderByAsc(SysRole::getRoleSort);

        Page<SysRole> rolePage = roleMapper.selectPage(page, wrapper);
        return PageVO.build(rolePage);
    }

    @Override
    public SysRole getById(Long id) {
        List<SysRole> roleList = getRoleByIds(Arrays.asList(id));
        if (CollectionUtils.isEmpty(roleList)) {
            return null;
        }
        return roleList.get(0);
    }

    @Override
    public SysRole getRoleByCode(String code) throws Exception {
        LambdaQueryWrapper<SysRole> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(SysRole::getRoleCode, code);
        List<SysRole> sysRoles = this.list(lambdaQueryWrapper);
        if (CollectionUtils.isEmpty(sysRoles)) {
            throw new Exception("Can't get role info by code: " + code);
        }
        if (sysRoles.size() > 1) {
            throw new Exception("Get multiple role info by code: " + code);
        }
        return sysRoles.get(0);
    }

//    @Override
//    public List<SysRole> getRoleByIds(List<Long> idList) throws Exception {
//
//
//
//        LambdaQueryWrapper<SysRole> lambdaQueryWrapper = new LambdaQueryWrapper<>();
//        lambdaQueryWrapper.in(SysRole::getId, idList);
//        List<SysRole> sysRoles = this.list(lambdaQueryWrapper);
//        String idStr = org.apache.commons.lang3.StringUtils.join(idList, ",");
//        if (CollectionUtils.isEmpty(sysRoles)) {
//            throw new Exception("Can't get role info by code: " + idStr);
//        }
//
//        List<Long> dbIds = sysRoles.stream()
//                .map(SysRole::getId)
//                .collect(Collectors.toList());
//
//        List<Long> notExistIds = new ArrayList<>(idList);
//        notExistIds.removeAll(dbIds);
//
//        if (!notExistIds.isEmpty()) {
//            throw new Exception("Role id not exist: " + idStr);
//        }
//        return sysRoles;
//    }

    @Override
    public List<SysRole> getRoleByIds(List<Long> idList) {
        if (CollectionUtils.isEmpty(idList)) {
            return Collections.emptyList();
        }

        try {
            // 1. 批量查询
            List<SysRole> roles = redisCacheService
                    .<SysRole, Long>batchBuilder()
                    .cache(RedisKey.ROLE_KEY, idList)
                    .db(
                            missIds -> {
                                // 分批查询数据库
                                LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
                                wrapper.in(SysRole::getId, missIds);
                                return this.list(wrapper);
                            },
                            SysRole::getId,
                            SysRole.class  // 添加resultType
                    )
                    .nullCache(RedisKey.ROLE_NULL_KEY)  // 建议添加空值缓存
//                    .nullCacheTimeout(30, TimeUnit.SECONDS)
//                    .withDbBatchSize(100)  // 数据库分批大小
//                    .enableMetrics(true)    // 启用性能监控
                    .execute();

            // 2. 检查结果（只检查传入的ID是否都有返回）
            Set<Long> foundIds = roles.stream()
                    .filter(Objects::nonNull)
                    .map(SysRole::getId)
                    .collect(Collectors.toSet());

            List<Long> notFoundIds = idList.stream()
                    .filter(id -> !foundIds.contains(id))
                    .collect(Collectors.toList());

            if (!notFoundIds.isEmpty()) {
                log.warn("Some role ids not found: {}", StringUtils.join(notFoundIds, ","));
                // 根据业务需求决定是否抛异常
                // throw new Exception("Can't get role info for ids: " + notFoundIds);
            }

            return roles;

        } catch (Exception e) {
            log.error("Failed to get roles by ids: {}", StringUtils.join(idList, ","), e);
            throw new RuntimeException("Failed to get roles by ids", e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addRole(RoleDTO roleDTO) throws Exception {
        // 检查角色编码是否存在
        Long count = roleMapper.selectCount(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleCode, roleDTO.getRoleCode()));
        if (count > 0) {
            throw new RuntimeException("角色编码已存在");
        }

        // 创建角色
        SysRole role = new SysRole();
        role.setRoleCode(roleDTO.getRoleCode());
        role.setRoleName(roleDTO.getRoleName());
        role.setRoleSort(roleDTO.getRoleSort());
        role.setDataScope(roleDTO.getDataScope() != null ? roleDTO.getDataScope() : 1);
        role.setStatus(roleDTO.getStatus() != null ? roleDTO.getStatus() : 1);
        role.setRemark(roleDTO.getRemark());

        int insert = roleMapper.insert(role);

        // 分配权限
        if (roleDTO.getPermissionIds() != null && roleDTO.getPermissionIds().length > 0) {
            assignPermissions(role.getId(), roleDTO.getPermissionIds());
        }

        boolean success = insert > 0;
//        if (success) {
//            callbackManager.register()
////                .releaseLock(lock, lockSuccessfully)
//                    .deleteCache(RedisKey.ROLE_PERMISSION_KEY, RedisKey.ROLE_PERMISSION_ROLE_KEY)
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
//        }
        return success;

    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateRole(RoleDTO roleDTO) throws Exception {
        SysRole role = roleMapper.selectById(roleDTO.getId());
        if (role == null) {
            throw new RuntimeException("角色不存在");
        }

        // 检查角色编码是否重复
        if (!role.getRoleCode().equals(roleDTO.getRoleCode())) {
            Long count = roleMapper.selectCount(new LambdaQueryWrapper<SysRole>()
                    .eq(SysRole::getRoleCode, roleDTO.getRoleCode()));
            if (count > 0) {
                throw new RuntimeException("角色编码已存在");
            }
        }
        this.redisTemplate.opsForHash().delete(RedisKey.ROLE_KEY, roleDTO.getId().toString());
        // 更新角色信息
        role.setRoleCode(roleDTO.getRoleCode());
        role.setRoleName(roleDTO.getRoleName());
        role.setRoleSort(roleDTO.getRoleSort());
        role.setDataScope(roleDTO.getDataScope());
        role.setStatus(roleDTO.getStatus());
        role.setRemark(roleDTO.getRemark());

        int update = roleMapper.updateById(role);

//        // 更新权限
//        if (roleDTO.getPermissionIds() != null) {
//            // 删除原有权限
//            rolePermissionMapper.delete(new LambdaQueryWrapper<SysRolePermission>()
//                    .eq(SysRolePermission::getRoleId, role.getId()));
//
//            // 分配新权限
//            if (roleDTO.getPermissionIds().length > 0) {
//                assignPermissions(role.getId(), roleDTO.getPermissionIds());
//            }
//        }

        boolean success = update > 0;
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
                        this.redisTemplate.opsForHash().delete(RedisKey.ROLE_KEY, roleDTO.getId().toString());
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
    public boolean deleteRole(Long id) throws Exception {
        if (id == null || id <= 0) {
            return false;
        }
//        // 删除角色权限关联
//        rolePermissionMapper.delete(new LambdaQueryWrapper<SysRolePermission>()
//                .eq(SysRolePermission::getRoleId, id));
//
//        // 删除角色
//        return roleMapper.deleteById(id) > 0;

//        List<SysRolePermission> sysRolePermissionList = this.sysRolePermissionService.getPermissionsByRoleIds(Arrays.asList(id));
//        List<Long> sysRolePermissionIdList = sysRolePermissionList.stream().map(p -> p.getId()).collect(Collectors.toList());
//        if (CollectionUtils.isNotEmpty(sysRolePermissionIdList)) {
//            this.sysRolePermissionService.removeByIds(sysRolePermissionIdList);
//        }
        this.sysRolePermissionService.removeRolePermissions(id);

        this.redisTemplate.opsForHash().delete(RedisKey.ROLE_KEY, id.toString());

        boolean success = this.removeById(id);
        if (success) {
            // 3. 注册事务回调 - 方式1：链式调用
            callbackManager.register()
//                .releaseLock(lock, lockSuccessfully)
                    //此处优化成 删除 角色key
//                    .deleteCache(RedisKey.ROLE_PERMISSION_KEY, RedisKey.ROLE_PERMISSION_ROLE_KEY)
                    .onCommit(() -> {
                        // 事务提交后，可以发送MQ消息通知其他服务
                        // log.info("Permission added, sending notification...");
                        // sendPermissionChangeNotification();
                        this.redisTemplate.opsForHash().delete(RedisKey.ROLE_KEY, id.toString());
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
    public boolean deleteBatch(Long[] ids) throws Exception {
        for (Long id : ids) {
            deleteRole(id);
        }
        return true;
    }

    @Override
    public boolean updateStatus(Long id, Integer status) {
        SysRole role = new SysRole();
        role.setId(id);
        role.setStatus(status);
        return roleMapper.updateById(role) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean assignPermissions(Long roleId, Long[] permissionIds) {
        // 删除原有权限
        rolePermissionMapper.delete(new LambdaQueryWrapper<SysRolePermission>()
                .eq(SysRolePermission::getRoleId, roleId));

        // 分配新权限
        List<SysRolePermission> rolePermissions = Arrays.stream(permissionIds)
                .map(permissionId -> {
                    SysRolePermission rp = new SysRolePermission();
                    rp.setRoleId(roleId);
                    rp.setPermissionId(permissionId);
                    return rp;
                })
                .collect(Collectors.toList());

        return rolePermissionMapper.insertBatch(rolePermissions) > 0;
    }
}