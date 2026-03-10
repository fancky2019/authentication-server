package com.fancky.authorization.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.fancky.authorization.model.dto.DeptDTO;
import com.fancky.authorization.model.entity.SysDept;
import com.fancky.authorization.model.response.PageVO;


import java.util.List;

public interface SysDeptService extends IService<SysDept> {

    /**
     * 获取部门树
     */
    List<SysDept> getDeptTree();

    /**
     * 获取部门树（只包含正常状态的部门）
     */
    List<SysDept> getNormalDeptTree();

    /**
     * 分页查询部门列表
     */
    PageVO<SysDept> getDeptPage(DeptDTO deptDTO);

    /**
     * 查询部门详情
     */
    SysDept getDeptDetail(Long id);

    /**
     * 新增部门
     */
    boolean addDept(DeptDTO deptDTO);

    /**
     * 更新部门
     */
    boolean updateDept(DeptDTO deptDTO);

    /**
     * 删除部门
     */
    boolean deleteDept(Long id);

    /**
     * 批量删除部门
     */
    boolean deleteBatch(Long[] ids);

    /**
     * 更新部门状态
     */
    boolean updateStatus(Long id, Integer status);

    /**
     * 检查部门编码是否存在
     */
    boolean checkDeptCodeExists(String deptCode, Long excludeId);

    /**
     * 检查是否有子部门
     */
    boolean hasChildDept(Long id);

    /**
     * 检查部门下是否有用户
     */
    boolean hasUsers(Long id);

    /**
     * 获取部门的完整层级名称
     */
    String getDeptFullName(Long id);
}
