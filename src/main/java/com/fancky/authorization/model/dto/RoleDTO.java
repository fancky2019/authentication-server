package com.fancky.authorization.model.dto;


import lombok.Data;
import javax.validation.constraints.NotBlank;

@Data
public class RoleDTO {

    private Long id;

    @NotBlank(message = "角色编码不能为空")
    private String roleCode;

    @NotBlank(message = "角色名称不能为空")
    private String roleName;

    private Integer roleSort;

    private Integer dataScope;

    private Integer status;

    private String remark;

    private Long[] permissionIds;

    // 分页参数
    private Long current = 1L;
    private Long size = 10L;
}