package com.fancky.authorization.service;


import com.fancky.authorization.model.entity.SysUser;
import com.fancky.authorization.model.entity.SysUserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private SysUserService sysUserService;

    /**
     * 登录时候会调用此方法
     * @param username
     * @return
     * @throws UsernameNotFoundException
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("正在加载用户: {}", username);

        SysUser user = null;
        try {
            user = sysUserService.getUserWithRolesAndPermissions(username);
        } catch (Exception e) {
            throw new UsernameNotFoundException("用户不存在: " + username);
        }

        if (user == null) {
            log.warn("用户不存在: {}", username);
            throw new UsernameNotFoundException("用户不存在: " + username);
        }

        if (!user.isEnabled()) {
            log.warn("用户已被禁用: {}", username);
            throw new UsernameNotFoundException("用户已被禁用: " + username);
        }

        log.debug("用户加载成功: {}, 角色: {}, 权限: {}",
                username, user.getRoles(), user.getPermissions());

        return user;
    }
}