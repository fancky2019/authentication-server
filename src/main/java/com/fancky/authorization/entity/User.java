package com.fancky.authorization.entity;


import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Data
@TableName("sys_user")
public class User implements UserDetails {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    @JsonIgnore
    private String password;

    private String nickname;

    private String email;

    private String phone;

    private String avatar;

    private Integer gender; // 0-未知 1-男 2-女

    @Getter(AccessLevel.NONE)
    @Setter
    @TableField(fill = FieldFill.INSERT)
    private Boolean enabled = true; // 1-启用 0-禁用

    @Getter(AccessLevel.NONE)
    @Setter
    @TableField("account_non_expired")
    private Integer accountNonExpired = 1;

    @Getter(AccessLevel.NONE)
    @Setter
    @TableField("account_non_locked")
    private Integer accountNonLocked = 1;

    @Getter(AccessLevel.NONE)
    @Setter
    @TableField("credentials_non_expired")
    private Integer credentialsNonExpired = 1;

    //不映射到表
    @TableField(exist = false)
    private List<String> roles = new ArrayList<>();

    @TableField(exist = false)
    private List<String> permissions = new ArrayList<>();

    private LocalDateTime lastLoginTime;

    @TableField(value = "create_time",fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time",fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted = 0;

    @Version
    @TableField(fill = FieldFill.INSERT)
    private Integer version = 1;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> authorities = new ArrayList<>();
        if (roles != null) {
            roles.forEach(role ->
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + role)));
        }
        if (permissions != null) {
            permissions.forEach(permission ->
                    authorities.add(new SimpleGrantedAuthority(permission)));
        }
        return authorities;
    }

    @Override
    public boolean isAccountNonExpired() {
        return accountNonExpired == 1;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked == 1;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return credentialsNonExpired == 1;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}