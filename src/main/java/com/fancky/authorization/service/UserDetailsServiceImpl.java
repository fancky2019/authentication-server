package com.fancky.authorization.service;


import com.fancky.authorization.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private  UserService userService;

    /**
     * 登录时候会调用此方法
     * @param username
     * @return
     * @throws UsernameNotFoundException
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("正在加载用户: {}", username);

        User user = userService.getUserWithRolesAndPermissions(username);

        if (user == null) {
            log.warn("用户不存在: {}", username);
            throw new UsernameNotFoundException("用户不存在: " + username);
        }

        if (!user.isEnabled()) {
            log.warn("用户已被禁用: {}", username);
            throw new UsernameNotFoundException("用户已被禁用: " + username);
        }

        List<String> roles=new ArrayList<>();
        roles.add("roles1");
        roles.add("roles2");
        roles.add("roles3");
        List<String> permissions=new ArrayList<>();
        permissions.add("permissions1");
        permissions.add("permissions2");
        permissions.add("permissions3");
        permissions.add("getCurrentUser");
        user.setRoles(roles);
        user.setPermissions(permissions);
        log.debug("用户加载成功: {}, 角色: {}, 权限: {}",
                username, user.getRoles(), user.getPermissions());

        return user;
    }
}