package com.fancky.authorization.model.dto;

import lombok.Data;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@Data
public class DeptDTO {

    private Long id;

    private Long parentId;

    @NotBlank(message = "部门名称不能为空")
    private String deptName;

    @NotBlank(message = "部门编码不能为空")
    private String deptCode;

    private String leader;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Email(message = "邮箱格式不正确")
    private String email;

    private Integer status;

    private Integer sort;

    private String remark;

    // 分页参数
    private Long current = 1L;
    private Long size = 10L;
}