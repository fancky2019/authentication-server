package com.fancky.authorization.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fancky.authorization.mapper.SysRolePermissionMapper;
import com.fancky.authorization.model.dto.PermissionAssignDTO;
import com.fancky.authorization.model.entity.SysPermission;
import com.fancky.authorization.model.entity.SysRolePermission;
import com.fancky.authorization.service.SysPermissionService;
import com.fancky.authorization.service.SysRolePermissionService;
import com.fancky.authorization.utility.RedisKey;
import com.fancky.authorization.utility.RedisUtil;
import com.fancky.authorization.utility.TransactionCallbackManager;
import com.fancky.authorization.utility.cache.RedisCacheService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.bouncycastle.asn1.cmc.CMCStatus.success;

@Slf4j
@Service
public class SysRolePermissionServiceImpl extends ServiceImpl<SysRolePermissionMapper, SysRolePermission>
        implements SysRolePermissionService {

    @Autowired
    private SysPermissionService sysPermissionService;

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
    public List<SysRolePermission> getByRoleIds(List<Long> roleIdList) {
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
    public List<SysRolePermission> getRolePermissions() {
        Map<String, SysRolePermission> rolePermissionMap = this.redisUtil.getHash(RedisKey.ROLE_PERMISSION_KEY, SysRolePermission.class);
        if (MapUtils.isEmpty(rolePermissionMap)) {
            initRolePermission();
        }
        return new ArrayList<>(rolePermissionMap.values());
    }

    @Override
    public List<SysRolePermission> getByPermissionIds(List<Long> permissionIdList) {
        if (CollectionUtils.isEmpty(permissionIdList)) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<SysRolePermission> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.in(SysRolePermission::getPermissionId, permissionIdList);
        return this.list(lambdaQueryWrapper);
    }

    @Override
    public boolean removeByPermissionIds(List<Long> permissionIdList) throws Exception {
        if (CollectionUtils.isEmpty(permissionIdList)) {
            return false;
        }
        List<SysRolePermission> sysRolePermissionList = getByPermissionIds(permissionIdList);
        List<Long> sysRolePermissionIdList = sysRolePermissionList.stream().map(p -> p.getId()).collect(Collectors.toList());
        return deleteBatch(sysRolePermissionIdList);
    }

    @Override
    public boolean deleteBatch(List<Long> sysRolePermissionIdList) throws Exception {
        if (CollectionUtils.isEmpty(sysRolePermissionIdList)) {
            return false;
        }


        removeCache(sysRolePermissionIdList, null);
        boolean success = this.removeByIds(sysRolePermissionIdList);

        if (success) {
            // 3. 注册事务回调 - 方式1：链式调用
            callbackManager.register()
//                .releaseLock(lock, lockSuccessfully)
                    //此处优化成 删除 角色key
                    .deleteCache(RedisKey.ROLE_PERMISSION_ROLE_KEY)
                    .onCommit(() -> {
                        // 事务提交后，可以发送MQ消息通知其他服务
                        // log.info("Permission added, sending notification...");
                        // sendPermissionChangeNotification();
                        removeCache(sysRolePermissionIdList, null);
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
    public void initRolePermission() {
        log.info("start init RolePermission");

        long startTime = System.currentTimeMillis();

        // 1. 清理缓存（使用pipe删除多个key）
        redisTemplate.delete(Arrays.asList(
                RedisKey.ROLE_PERMISSION_KEY,
                RedisKey.ROLE_PERMISSION_ROLE_KEY
        ));
        log.info("RolePermission Cache cleared");

        // 2. 查询所有角色权限关系
        List<SysRolePermission> list = this.list();

        if (CollectionUtils.isEmpty(list)) {
            log.warn("No role permission data found");
            return;
        }

        // 3. 存储原始数据（按ID索引）
        Map<String, SysRolePermission> idMap = list.stream()
                .collect(Collectors.toMap(
                        p -> p.getId().toString(),
                        Function.identity(),
                        (v1, v2) -> v1  // 如果有重复，保留第一个
                ));

        redisTemplate.opsForHash().putAll(RedisKey.ROLE_PERMISSION_KEY, idMap);
        log.info("RolePermission data cached, size: {}", idMap.size());

        // 4. 使用Stream分组，构建角色-权限映射
        Map<String, List<SysRolePermission>> rolePermissionMap = list.stream()
                .collect(Collectors.groupingBy(
                        rp -> rp.getRoleId().toString(),
                        HashMap::new,
                        Collectors.toList() // Collectors.toCollection(HashSet::new)
                ));


        // 6. 批量存入Redis
        if (!rolePermissionMap.isEmpty()) {
            redisTemplate.opsForHash().putAll(RedisKey.ROLE_PERMISSION_ROLE_KEY, rolePermissionMap);
            log.info("Role-Permission mapping cached, role count: {}", rolePermissionMap.size());
        }


        //取值
//        Object obj = redisTemplate.opsForHash().get(RedisKey.ROLE_PERMISSION_ROLE_KEY, key);
//        if (obj instanceof Set) {
//            Set<Long> data = (Set<Long>) obj;
//        }

        long cost = System.currentTimeMillis() - startTime;
        log.info("init RolePermission complete, total records: {}, cost: {}ms",
                list.size(), cost);
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
    public void assignPermissions(PermissionAssignDTO assignDTO) throws Exception {
        Long roleId = assignDTO.getRoleId();
        List<Long> selectedPermissionIds = assignDTO.getPermissionIds();

        log.info("开始为角色[{}]分配权限，选中权限数量: {}", roleId, selectedPermissionIds == null ? 0 : selectedPermissionIds.size());

        // 1. 参数校验
        if (roleId == null) {
            throw new Exception("role is null");
        }

        if (CollectionUtils.isEmpty(selectedPermissionIds)) {
            throw new Exception("permission is null");
        }
//        // 2. 获取所有权限数据（用于计算完整路径）
//        List<SysPermission> allPermissions = sysPermissionService.list();
//        if (CollectionUtils.isEmpty(allPermissions)) {
//            log.warn("权限表为空");
//            return;
//        }
//
//        // 3. 构建权限映射
//        Map<Long, SysPermission> permissionMap = allPermissions.stream()
//                .collect(Collectors.toMap(SysPermission::getId, p -> p));

        Map<String, SysPermission> permissionMap = this.redisUtil.getHash(RedisKey.PERMISSION_KEY, SysPermission.class);
        // 4. 计算完整的权限集合（包含所有父级）
        Set<Long> completePermissionIds = calculateCompletePermissionSet(
                selectedPermissionIds, permissionMap);

        log.info("计算后的完整权限数量: {}", completePermissionIds.size());

        // 5. 获取角色当前已有的权限
//        Map<String, SysRolePermission> rolePermissionMap = this.redisUtil.getHash(RedisKey.ROLE_PERMISSION_KEY, SysRolePermission.class);
//        List<SysRolePermission> rolePermissionList =
//                rolePermissionMap.values().stream().filter(p -> p.getRoleId().equals(roleId)).collect(Collectors.toList());
        List<SysRolePermission> rolePermissionList = this.getByRoleIds(Arrays.asList(roleId));
//        List<SysRolePermission> rolePermissionList = (List<SysRolePermission>) this.redisTemplate.opsForHash().get(RedisKey.ROLE_PERMISSION_ROLE_KEY, roleId.toString());
        List<Long> existingPermissionIds = rolePermissionList.stream().map(p -> p.getPermissionId()).distinct().collect(Collectors.toList());
        Set<Long> existingSet = new HashSet<>(existingPermissionIds);

        // 6. 计算需要新增和删除的权限
        Set<Long> toAdd = completePermissionIds.stream()
                .filter(id -> !existingSet.contains(id))
                .collect(Collectors.toSet());

        Set<Long> toRemove = existingSet.stream()
                .filter(id -> !completePermissionIds.contains(id))
                .collect(Collectors.toSet());

        log.info("需要新增权限: {}, 需要移除权限: {}", toAdd.size(), toRemove.size());

        // 7. 批量新增
        if (!toAdd.isEmpty()) {
            List<SysRolePermission> addList = buildRolePermissions(roleId, toAdd, permissionMap);
            this.saveBatch(addList);
        }

        // 8. 批量删除
        if (!toRemove.isEmpty()) {
            List<Long> toRemoveRolePermissionId = rolePermissionList.stream().filter(p -> p.getRoleId().equals(roleId) &&
                    toRemove.contains(p.getPermissionId())
            ).map(p -> p.getId()).collect(Collectors.toList());

//            rolePermissionMapper.batchDeleteByRoleIdAndPermissionIds(roleId,
//                    new ArrayList<>(toRemove));
            boolean deletedSuccess = this.removeByIds(toRemoveRolePermissionId);
            int n = 0;
        }
        // 3. 注册事务回调 - 方式1：链式调用
        callbackManager.register()
//                .releaseLock(lock, lockSuccessfully)
                .deleteCache(RedisKey.ROLE_PERMISSION_KEY, RedisKey.ROLE_PERMISSION_ROLE_KEY)
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
        log.info("角色[{}]权限分配完成", roleId);
    }

    /**
     * 计算完整的权限集合（包含所有父级）
     */
    private Set<Long> calculateCompletePermissionSet(List<Long> selectedPermissionIds,
                                                     Map<String, SysPermission> permissionMap) {
        Set<Long> completeSet = new HashSet<>();

        if (CollectionUtils.isEmpty(selectedPermissionIds)) {
            return completeSet;
        }

        // 对每个选中的权限，递归添加其所有父级
        for (Long permissionId : selectedPermissionIds) {
            addPermissionAndParents(permissionId, permissionMap, completeSet);
        }

        return completeSet;
    }


    /**
     * 递归添加权限及其所有父级
     */
    private void addPermissionAndParents(Long permissionId,
                                         Map<String, SysPermission> permissionMap,
                                         Set<Long> collector) {
        if (permissionId == null || permissionId == 0) {
            return;
        }

        // 添加当前权限
        collector.add(permissionId);

        // 递归添加父级
        SysPermission permission = permissionMap.get(permissionId);
        if (permission != null && permission.getParentId() != null && permission.getParentId() != 0) {
            addPermissionAndParents(permission.getParentId(), permissionMap, collector);
        }
    }

    /**
     * 构建角色权限关联对象列表
     */
    private List<SysRolePermission> buildRolePermissions(Long roleId,
                                                         Set<Long> permissionIds,
                                                         Map<String, SysPermission> permissionMap) {
        List<SysRolePermission> rolePermissions = new ArrayList<>();

        for (Long permissionId : permissionIds) {
            SysPermission permission = permissionMap.get(permissionId.toString());
            if (permission == null) {
                log.warn("权限[{}]不存在，跳过", permissionId);
                continue;
            }

            SysRolePermission rp = new SysRolePermission();
            rp.setRoleId(roleId);
            rp.setPermissionId(permissionId);
            rp.setPermissionType(permission.getPermissionType());

            // 计算权限路径和层级
//            String path = calculatePermissionPath(permissionId, permissionMap);
//            rp.setPermissionPath(path);
//            rp.setPermissionLevel(path.split("-").length);

            rolePermissions.add(rp);
        }

        return rolePermissions;
    }

    /**
     * 计算权限路径（格式：父ID-子ID）
     */
    private String calculatePermissionPath(Long permissionId,
                                           Map<String, SysPermission> permissionMap) {
        List<String> pathSegments = new ArrayList<>();
        Long currentId = permissionId;

        while (currentId != null && currentId != 0) {
            pathSegments.add(String.valueOf(currentId));
            SysPermission permission = permissionMap.get(currentId.toString());
            currentId = permission != null ? permission.getParentId() : null;
        }

        Collections.reverse(pathSegments);
        return String.join("-", pathSegments);
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
    public boolean removeRolePermissions(Long roleId) throws Exception {
//        return remove(new LambdaQueryWrapper<SysRolePermission>()
//                .eq(SysRolePermission::getRoleId, roleId));

        List<SysRolePermission> sysRolePermissionList = this.getByRoleIds(Arrays.asList(roleId));
        List<Long> sysRolePermissionIdList = sysRolePermissionList.stream().map(p -> p.getId()).collect(Collectors.toList());
        return deleteBatch(sysRolePermissionIdList);

//        if (CollectionUtils.isNotEmpty(sysRolePermissionIdList)) {
//            removeCache(sysRolePermissionIdList, roleId);
//            boolean success = this.removeByIds(sysRolePermissionIdList);
//
//            if (success) {
//                // 3. 注册事务回调 - 方式1：链式调用
//                callbackManager.register()
////                .releaseLock(lock, lockSuccessfully)
//                        //此处优化成 删除 角色key
////                        .deleteCache(RedisKey.ROLE_PERMISSION_KEY, RedisKey.ROLE_PERMISSION_ROLE_KEY)
//                        .onCommit(() -> {
//                            // 事务提交后，可以发送MQ消息通知其他服务
//                            // log.info("Permission added, sending notification...");
//                            // sendPermissionChangeNotification();
//                            removeCache(sysRolePermissionIdList, roleId);
//                        })
//                        .onRollback(() -> {
//                            // 事务回滚后，可以做些补偿操作
//                            // log.warn("Permission addition rolled back");
//                        })
//                        .execute();
//            }
//            return success;
//        }
//        return true;

    }


    private void removeCache(List<Long> sysRolePermissionIdList, Long roleId) {
        String[] idStrArray = sysRolePermissionIdList.stream()
                .map(p -> p.toString())
                .toArray(String[]::new);
        if (roleId != null && roleId > 0) {
            this.redisTemplate.opsForHash().delete(RedisKey.ROLE_PERMISSION_ROLE_KEY, roleId.toString());
        }

        this.redisTemplate.opsForHash().delete(RedisKey.ROLE_PERMISSION_KEY, idStrArray);

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
