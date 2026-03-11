package com.fancky.authorization.model.entity;


import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUser extends BaseEntity implements UserDetails {
    @TableField("role_code")
    private String username;

    @JsonIgnore
    private String password;

    private String nickname;

    private String email;

    private String phone;

    private String avatar;

    private Integer gender;

//    private Integer enabled;
//
//    private Integer accountNonExpired;
//
//    private Integer accountNonLocked;
//
//    private Integer credentialsNonExpired;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastLoginTime;

    private String deleteBy;

    private LocalDateTime deleteTime;


    @TableField(exist = false)
    private List<SysDept> depts;

    //不映射到表
    @TableField(exist = false)
    private List<String> roles = new ArrayList<>();

    @TableField(exist = false)
    private List<String> permissions = new ArrayList<>();
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