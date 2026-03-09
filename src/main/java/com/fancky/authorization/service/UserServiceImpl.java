package com.fancky.authorization.service;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.fancky.authorization.model.request.RegisterRequest;
import com.fancky.authorization.model.entity.User;
import com.fancky.authorization.model.response.UserInfoVO;
import com.fancky.authorization.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    private  UserMapper userMapper;
    @Autowired
    private  PasswordEncoder passwordEncoder;

    @Override
    public User getUserWithRolesAndPermissions(String username) {
        log.debug("查询用户信息: {}", username);
        return userMapper.selectUserWithRolesAndPermissions(username);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean register(RegisterRequest registerRequest) {
        log.info("用户注册: {}", registerRequest.getUsername());

        // 检查用户名
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, registerRequest.getUsername());
        if (this.count(wrapper) > 0) {
            throw new RuntimeException("用户名已存在");
        }

        // 检查邮箱
        if (StringUtils.hasText(registerRequest.getEmail())) {
            wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(User::getEmail, registerRequest.getEmail());
            if (this.count(wrapper) > 0) {
                throw new RuntimeException("邮箱已被注册");
            }
        }

        // 检查手机号
        if (StringUtils.hasText(registerRequest.getPhone())) {
            wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(User::getPhone, registerRequest.getPhone());
            if (this.count(wrapper) > 0) {
                throw new RuntimeException("手机号已被注册");
            }
        }

        User user = new User();
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
        userMapper.updateLastLoginTime(username, LocalDateTime.now());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean changePassword(String username, String oldPassword, String newPassword) {
        User user = this.getUserWithRolesAndPermissions(username);
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
        int rows = userMapper.updatePassword(username, encodedNewPassword);

        return rows > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean resetPassword(String username, String newPassword) {
        String encodedPassword = passwordEncoder.encode(newPassword);
        int rows = userMapper.updatePassword(username, encodedPassword);
        return rows > 0;
    }

    @Override
    public IPage<User> getUserPage(Page<User> page, String keyword) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w
                    .like(User::getUsername, keyword)
                    .or()
                    .like(User::getNickname, keyword)
                    .or()
                    .like(User::getEmail, keyword)
                    .or()
                    .like(User::getPhone, keyword)
            );
        }

        wrapper.orderByDesc(User::getCreateTime);

        return this.page(page, wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchUpdateStatus(List<Long> ids, Integer enabled) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        int rows = userMapper.batchUpdateEnabled(ids, enabled);
        return rows > 0;
    }

    @Override
    public UserInfoVO convertToVO(User user) {
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