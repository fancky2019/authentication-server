package com.fancky.authorization.model.entity;


import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_dept")
public class SysDept extends BaseEntity {

    /**
     * 父部门ID
     */
    private Long parentId;

    /**
     * 部门名称
     */
    private String deptName;

    /**
     * 部门编码
     */
    private String deptCode;

    /**
     * 负责人
     */
    private String leader;

    /**
     * 联系电话
     */
    private String phone;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 状态(1-正常 0-禁用)
     */
    private Integer status;

    /**
     * 显示顺序
     */
    private Integer sort;

    /**
     * 逻辑删除字段
     */
    private String deleteBy;

    private LocalDateTime deleteTime;

    /**
     * 子部门列表（用于树形结构）
     */
    @TableField(exist = false)
    private List<SysDept> children;

    /**
     * 部门下的用户列表
     */
    @TableField(exist = false)
    private List<SysUser> users;

    @TableField(exist = false)
    private String parentName;

}