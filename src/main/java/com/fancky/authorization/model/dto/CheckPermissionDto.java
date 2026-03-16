package com.fancky.authorization.model.dto;

import com.fancky.authorization.model.entity.SysUser;
import com.fancky.authorization.model.response.SysUserResponse;
import lombok.Data;

@Data
public class CheckPermissionDto {
    private Boolean hasPermission=false;
    private String path;
    private SysUser user;
}
