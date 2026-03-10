package com.fancky.authorization.service;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.fancky.authorization.mapper.SysUserMapper;
import com.fancky.authorization.mapper.SysUserRoleMapper;
import com.fancky.authorization.model.dto.UserDTO;
import com.fancky.authorization.model.entity.SysUser;
import com.fancky.authorization.model.entity.SysUserRole;
import com.fancky.authorization.model.request.RegisterRequest;
import com.fancky.authorization.model.response.PageVO;
import com.fancky.authorization.model.response.UserInfoVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service

public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {


    @Autowired
    private  SysUserMapper sysUserMapper;
    @Autowired
    private  SysUserRoleMapper userRoleMapper;
    @Autowired
    private  PasswordEncoder passwordEncoder;

    @Override
    public SysUser getUserByUsername(String username) {
        return sysUserMapper.selectUserByUsername(username);
    }

    @Override
    public PageVO<SysUser> getUserPage(UserDTO userDTO) {
        Page<SysUser> page = new Page<>(userDTO.getCurrent(), userDTO.getSize());

        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(userDTO.getUsername()), SysUser::getUsername, userDTO.getUsername())
                .like(StringUtils.hasText(userDTO.getNickname()), SysUser::getNickname, userDTO.getNickname())
                .like(StringUtils.hasText(userDTO.getPhone()), SysUser::getPhone, userDTO.getPhone())
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
        user.setAccountNonExpired(1);
        user.setAccountNonLocked(1);
        user.setCredentialsNonExpired(1);

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

        if (StringUtils.hasText(userDTO.getPassword())) {
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
        user.setEnabled(enabled==1);
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
    public SysUser getUserWithRolesAndPermissions(String username) {
        log.debug("查询用户信息: {}", username);
        return sysUserMapper.selectUserWithRolesAndPermissions(username);
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
        if (StringUtils.hasText(registerRequest.getEmail())) {
            wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SysUser::getEmail, registerRequest.getEmail());
            if (this.count(wrapper) > 0) {
                throw new RuntimeException("邮箱已被注册");
            }
        }

        // 检查手机号
        if (StringUtils.hasText(registerRequest.getPhone())) {
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
    public boolean changePassword(String username, String oldPassword, String newPassword) {
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

        if (StringUtils.hasText(keyword)) {
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
    public UserInfoVO convertToVO(SysUser user) {
        if (user == null) {
            return null;
        }

        UserInfoVO vo = new UserInfoVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setEmail(user.getEmail());
        vo.setPhone(user.getPhone());
        vo.setAvatar(user.getAvatar());
        vo.setGender(user.getGender());
        vo.setRoles(user.getRoles());
        vo.setPermissions(user.getPermissions());
        vo.setLastLoginTime(user.getLastLoginTime());
        vo.setCreateTime(user.getCreateTime());

        return vo;
    }
}