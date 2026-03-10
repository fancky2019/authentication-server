package com.fancky.authorization.model.dto;


import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class PermissionDTO {

    private Long id;

    private Long parentId;

    @NotBlank(message = "权限名称不能为空")
    private String permissionName;

    @NotNull(message = "权限类型不能为空")
    private Integer permissionType;

    private String permissionCode;

    private String permissionValue;

    private String path;

    private String component;

    private String icon;

    private Integer sort;

    private Integer visible;

    private Integer status;

    private String remark;

    // 分页参数
    private Long current = 1L;
    private Long size = 10L;
}
