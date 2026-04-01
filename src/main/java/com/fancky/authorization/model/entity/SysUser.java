package com.fancky.authorization.model.entity;


import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
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
    //    @TableField("role_code")
    private String username;
//    @TableLogic
//    private Integer deleted;
    //    @JsonIgnore
    private String password;

    private String nickname;

    private String email;

    private String phone;

    private String avatar;

    private Integer gender;

    @TableField(exist = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastLoginTime;

    @TableField(exist = false)
    private List<SysDept> depts;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime passwordLastChangeTime;
    private Integer  passwordExpireDays=30;


    //不映射到表
    @TableField(exist = false)
    private List<String> roles = new ArrayList<>();

    @TableField(exist = false)
    private List<String> permissions = new ArrayList<>();

    @TableField(exist = false)
    private List<String> permissionPathList = new ArrayList<>();

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
    private Boolean accountNonExpired = true;

    @Getter(AccessLevel.NONE)
    @Setter
    @TableField("account_non_locked")
    private Boolean accountNonLocked = true;

    @Getter(AccessLevel.NONE)
    @Setter
    @TableField("credentials_non_expired")
    private Boolean credentialsNonExpired = true;

    @Override
    public boolean isAccountNonExpired() {
        return accountNonExpired;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return credentialsNonExpired;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public void clearSensitiveInformation() {
        this.password = null;
        this.roles = null;
        this.permissions = null;
        this.permissionPathList = null;
    }
}