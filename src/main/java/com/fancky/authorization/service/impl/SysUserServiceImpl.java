package com.fancky.authorization.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fancky.authorization.mapper.SysUserMapper;
import com.fancky.authorization.mapper.SysUserRoleMapper;
import com.fancky.authorization.model.dto.CheckPermissionDto;
import com.fancky.authorization.model.dto.UserDTO;
import com.fancky.authorization.model.entity.*;
import com.fancky.authorization.model.request.RefreshTokenRequest;
import com.fancky.authorization.model.request.RegisterRequest;
import com.fancky.authorization.model.response.PageVO;
import com.fancky.authorization.service.*;
import com.fancky.authorization.utility.RedisKey;
import com.fancky.authorization.utility.RedisUtil;
import com.fancky.authorization.utility.cache.RedisCacheService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {


    @Autowired
    private SysUserMapper sysUserMapper;
    @Autowired
    private SysUserRoleMapper userRoleMapper;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private RedisCacheService redisCacheService;

    @Autowired
    private SysUserRoleService sysUserRoleService;

    @Autowired
    private SysRoleService sysRoleService;

    @Autowired
    private SysRolePermissionService sysRolePermissionService;

    @Autowired
    private SysPermissionService sysPermissionService;

    @Autowired
    private JwtService jwtService;


    @Override
    public SysUser getUserByUsername(String username) throws Exception {
        if (StringUtils.isEmpty(username)) {
            return null;
        }
        username = username.trim();
        String code = username;
        return redisCacheService.<SysUser>builder()
                .primary(RedisKey.USER_CODE_KEY, code)
                .nullCache(RedisKey.USER_CODE_NULL_PREFIX + code)
                .db(() -> sysUserMapper.selectUserByUsername(code), SysUser.class)
                .secondaryCache(                                      // 二级缓存：id -> user
                        RedisKey.USER_KEY,
                        user -> String.valueOf(user.getId())
                )
//                .withAdditionalCache(user -> {                        // 额外的缓存逻辑
//                    log.info("User {} cached successfully", code);
//                    // 可以在这里添加其他缓存操作，比如记录缓存时间等
//                })
//                .nullCacheTimeout(5, TimeUnit.MINUTES)               // 空值缓存5分钟
                .execute();
    }

    @Override
    public Map<String, String> refreshToken(RefreshTokenRequest request) throws Exception {

        String refreshToken = request.getRefreshToken();
        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            throw new Exception("刷新令牌不能为空");
        }
        String newAccessToken = jwtService.refreshAccessToken(refreshToken);
        Map<String, String> result = new HashMap<>();
        result.put("accessToken", newAccessToken);
        result.put("tokenType", jwtService.getTokenPrefix());
        result.put("expiresIn", String.valueOf(jwtService.getTokenExpiresIn(newAccessToken)));
        return result;

    }

    @Override
    public SysUser getUserById(Long id) throws Exception {
        if (id == null || id <= 0) {
            return null;
        }
        String idStr = id.toString();
        return redisCacheService.<SysUser>builder()
                .primary(RedisKey.USER_KEY, idStr)
                .nullCache(RedisKey.USER_NULL_PREFIX + idStr)
                .db(() -> this.getById(id), SysUser.class)
                .secondaryCache(RedisKey.USER_CODE_KEY, SysUser::getUsername)
                .nullCacheTimeout(1, TimeUnit.MINUTES)
                .execute();
    }


    @Override
    public PageVO<SysUser> getUserPage(UserDTO userDTO) {
        Page<SysUser> page = new Page<>(userDTO.getCurrent(), userDTO.getSize());

        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.isNotEmpty(userDTO.getUsername()), SysUser::getUsername, userDTO.getUsername())
                .like(StringUtils.isNotEmpty(userDTO.getNickname()), SysUser::getNickname, userDTO.getNickname())
                .like(StringUtils.isNotEmpty(userDTO.getPhone()), SysUser::getPhone, userDTO.getPhone())
                .eq(userDTO.getGender() != null, SysUser::getGender, userDTO.getGender())
                .eq(userDTO.getEnabled() != null, SysUser::isEnabled, userDTO.getEnabled())
                .orderByDesc(SysUser::getCreateTime);

        Page<SysUser> userPage = sysUserMapper.selectPage(page, wrapper);
        return PageVO.build(userPage);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addUser(UserDTO userDTO) {
        // 检查用户名是否存在
        Long count = sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, userDTO.getUsername()));
        if (count > 0) {
            throw new RuntimeException("用户名已存在");
        }

        // 创建用户
        SysUser user = new SysUser();
        user.setUsername(userDTO.getUsername());
        user.setPassword(passwordEncoder.encode(userDTO.getPassword()));
        user.setNickname(userDTO.getNickname());
        user.setEmail(userDTO.getEmail());
        user.setPhone(userDTO.getPhone());
        user.setGender(userDTO.getGender());
        user.setEnabled(true);
        user.setAccountNonExpired(true);
        user.setAccountNonLocked(true);
        user.setCredentialsNonExpired(true);

        int insert = sysUserMapper.insert(user);

        // 分配角色
        if (userDTO.getRoleIds() != null && userDTO.getRoleIds().length > 0) {
            assignRoles(user.getId(), userDTO.getRoleIds());
        }

        return insert > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateUser(UserDTO userDTO) {
        SysUser user = sysUserMapper.selectById(userDTO.getId());
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 更新用户信息
        user.setNickname(userDTO.getNickname());
        user.setEmail(userDTO.getEmail());
        user.setPhone(userDTO.getPhone());
        user.setGender(userDTO.getGender());

        if (StringUtils.isNotEmpty(userDTO.getPassword())) {
            user.setPassword(passwordEncoder.encode(userDTO.getPassword()));
        }

        int update = sysUserMapper.updateById(user);

        // 更新角色
        if (userDTO.getRoleIds() != null) {
            // 删除原有角色
            userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>()
                    .eq(SysUserRole::getUserId, user.getId()));

            // 分配新角色
            if (userDTO.getRoleIds().length > 0) {
                assignRoles(user.getId(), userDTO.getRoleIds());
            }
        }

        return update > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteUser(Long id) {
        // 删除用户角色关联
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getUserId, id));

        // 删除用户
        return sysUserMapper.deleteById(id) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteBatch(Long[] ids) {
        for (Long id : ids) {
            deleteUser(id);
        }
        return true;
    }

    @Override
    public boolean updateStatus(Long id, Integer enabled) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setEnabled(enabled == 1);
        return sysUserMapper.updateById(user) > 0;
    }

    @Override
    public boolean resetPassword(Long id, String password) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setPassword(passwordEncoder.encode(password));
        return sysUserMapper.updateById(user) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean assignRoles(Long userId, Long[] roleIds) {
        // 删除原有角色
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getUserId, userId));

        // 分配新角色
        List<SysUserRole> userRoles = Arrays.stream(roleIds)
                .map(roleId -> {
                    SysUserRole ur = new SysUserRole();
                    ur.setUserId(userId);
                    ur.setRoleId(roleId);
                    return ur;
                })
                .collect(Collectors.toList());

        return userRoleMapper.insertBatch(userRoles) > 0;
    }


    @Override
    public SysUser getUserWithRolesAndPermissions(String username) throws Exception {
//        log.debug("查询用户信息: {}", username);
//        return sysUserMapper.selectUserWithRolesAndPermissions(username);
        SysUser sysUser = getUserByUsername(username);
        if (sysUser == null) {
            String msg = MessageFormat.format("User {0} doesn't exist", username);
            log.info(msg);
            throw new Exception(msg);
        }
        List<SysUserRole> sysUserRoleList = this.sysUserRoleService.getUserRoles(sysUser.getId());

        List<SysUserRole> userRoles = sysUserRoleService.getUserRoles(sysUser.getId());
        if (CollectionUtils.isEmpty(userRoles)) {
            log.info("username {} doesn't set role information", username);
            return sysUser;
        }

        List<Long> roleIdList = userRoles.stream().filter(p -> p.getRoleId() != null).map(p -> p.getRoleId()).distinct().collect(Collectors.toList());
        if (CollectionUtils.isEmpty(roleIdList)) {
            log.info("username {} doesn't set role information", username);
            return sysUser;
        }
        List<SysRole> roleList = sysRoleService.getRoleByIds(roleIdList);
        if (CollectionUtils.isEmpty(roleList)) {
            String roleIdListStr = StringUtils.join(roleIdList, ",");
            log.info("Can not get role information by id {}", roleIdListStr);
            return sysUser;
        }
        List<String> roleCodeList = roleList.stream().map(p -> p.getRoleCode()).distinct().collect(Collectors.toList());
        sysUser.setRoles(roleCodeList);

        List<SysRolePermission> sysRolePermissionList = this.sysRolePermissionService.getPermissionsByRoleIds(roleIdList);
        if (CollectionUtils.isEmpty(sysRolePermissionList)) {
            String roleIdListStr = StringUtils.join(roleIdList, ",");
            log.info("Role {} don't set permission information", roleIdListStr);
            return sysUser;
        }
        List<Long> sysPermissionIdList = sysRolePermissionList.stream().map(p -> p.getPermissionId()).distinct().collect(Collectors.toList());
        List<SysPermission> permissionList = sysPermissionService.getPermissions(sysPermissionIdList);
        if (CollectionUtils.isEmpty(permissionList)) {
            String sysPermissionIdListStr = StringUtils.join(sysPermissionIdList, ",");
            log.info("Can not get permission information by id {}", sysPermissionIdListStr);
            return sysUser;
        }
        List<String> permissionCodeList = permissionList.stream().map(p -> p.getPermissionCode()).distinct().collect(Collectors.toList());
        sysUser.setPermissions(permissionCodeList);

        List<String> permissionPathList = permissionList.stream().filter(p -> p.getPath() != null).map(p -> p.getPath()).distinct().collect(Collectors.toList());
        sysUser.setPermissionPathList(permissionPathList);
        return sysUser;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean register(RegisterRequest registerRequest) {
        log.info("用户注册: {}", registerRequest.getUsername());

        // 检查用户名
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, registerRequest.getUsername());
        if (this.count(wrapper) > 0) {
            throw new RuntimeException("用户名已存在");
        }

        // 检查邮箱
        if (StringUtils.isNotEmpty(registerRequest.getEmail())) {
            wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SysUser::getEmail, registerRequest.getEmail());
            if (this.count(wrapper) > 0) {
                throw new RuntimeException("邮箱已被注册");
            }
        }

        // 检查手机号
        if (StringUtils.isNotEmpty(registerRequest.getPhone())) {
            wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SysUser::getPhone, registerRequest.getPhone());
            if (this.count(wrapper) > 0) {
                throw new RuntimeException("手机号已被注册");
            }
        }

        SysUser user = new SysUser();
        user.setUsername(registerRequest.getUsername());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setNickname(registerRequest.getNickname());
        user.setEmail(registerRequest.getEmail());
        user.setPhone(registerRequest.getPhone());
        user.setGender(registerRequest.getGender());

        boolean result = this.save(user);

        if (result) {
            log.info("用户注册成功: {}", user.getUsername());
        }

        return result;
    }

    @Override
    public void updateLastLoginTime(String username) {
        sysUserMapper.updateLastLoginTime(username, LocalDateTime.now());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean changePassword(String username, String oldPassword, String newPassword) throws Exception {
        SysUser user = this.getUserWithRolesAndPermissions(username);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new RuntimeException("原密码错误");
        }

        if (!newPassword.equals(newPassword)) {
            throw new RuntimeException("新密码与确认密码不一致");
        }

        String encodedNewPassword = passwordEncoder.encode(newPassword);
        int rows = sysUserMapper.updatePassword(username, encodedNewPassword);

        return rows > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean resetPassword(String username, String newPassword) {
        String encodedPassword = passwordEncoder.encode(newPassword);
        int rows = sysUserMapper.updatePassword(username, encodedPassword);
        return rows > 0;
    }

    @Override
    public IPage<SysUser> getUserPage(Page<SysUser> page, String keyword) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.isNotEmpty(keyword)) {
            wrapper.and(w -> w
                    .like(SysUser::getUsername, keyword)
                    .or()
                    .like(SysUser::getNickname, keyword)
                    .or()
                    .like(SysUser::getEmail, keyword)
                    .or()
                    .like(SysUser::getPhone, keyword)
            );
        }

        wrapper.orderByDesc(SysUser::getCreateTime);

        return this.page(page, wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchUpdateStatus(List<Long> ids, Integer enabled) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        int rows = sysUserMapper.batchUpdateEnabled(ids, enabled);
        return rows > 0;
    }

    @Override
    public CheckPermissionDto checkPermission(CheckPermissionDto dto, HttpServletRequest request) throws Exception {
        boolean hasPermission = false;
        String username = this.jwtService.getUsername(request);

        if (dto == null) {
            dto = new CheckPermissionDto();
            dto.setHasPermission(false);
            return dto;
        }
        String superAdmin = "ROLE_SUPER_ADMIN";
        SysUser sysUser = this.getUserWithRolesAndPermissions(username);
        if (CollectionUtils.isNotEmpty(sysUser.getPermissionPathList()) &&
                StringUtils.isNoneEmpty(dto.getPath())) {

            if (sysUser.getRoles().contains(superAdmin)) {
                hasPermission = true;
            } else {
                hasPermission = sysUser.getPermissionPathList().contains(dto.getPath());
            }
            if (hasPermission) {
                sysUser.clearSensitiveInformation();
                dto.setUser(sysUser);
            }
        }
        return dto;
    }

}