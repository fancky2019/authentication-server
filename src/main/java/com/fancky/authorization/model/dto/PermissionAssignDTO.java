package com.fancky.authorization.model.dto;


import lombok.Data;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 权限分配请求DTO
 */
@Data
public class PermissionAssignDTO {

    @NotNull(message = "角色ID不能为空")
    private Long roleId;

    private List<Long> permissionIds;

    /**
     * 操作类型：add-添加，remove-移除，update-更新
     */
    private String operationType = "update";
}