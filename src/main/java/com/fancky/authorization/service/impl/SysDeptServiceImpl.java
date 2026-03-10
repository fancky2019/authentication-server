package com.fancky.authorization.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fancky.authorization.mapper.SysDeptMapper;
import com.fancky.authorization.mapper.SysUserDeptMapper;
import com.fancky.authorization.model.dto.DeptDTO;
import com.fancky.authorization.model.entity.SysDept;
import com.fancky.authorization.model.entity.SysUserDept;
import com.fancky.authorization.model.response.PageVO;
import com.fancky.authorization.service.SysDeptService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SysDeptServiceImpl extends ServiceImpl<SysDeptMapper, SysDept> implements SysDeptService {

    @Autowired
    private  SysDeptMapper deptMapper;
    @Autowired
    private  SysUserDeptMapper userDeptMapper;

    @Override
    public List<SysDept> getDeptTree() {
        List<SysDept> depts = deptMapper.selectList(
                new LambdaQueryWrapper<SysDept>()
                        .orderByAsc(SysDept::getSort)
        );
        return buildTree(depts, 0L);
    }

    @Override
    public List<SysDept> getNormalDeptTree() {
        List<SysDept> depts = deptMapper.selectNormalDepts();
        return buildTree(depts, 0L);
    }

    @Override
    public PageVO<SysDept> getDeptPage(DeptDTO deptDTO) {
        Page<SysDept> page = new Page<>(deptDTO.getCurrent(), deptDTO.getSize());

        LambdaQueryWrapper<SysDept> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(deptDTO.getDeptName()), SysDept::getDeptName, deptDTO.getDeptName())
                .like(StringUtils.hasText(deptDTO.getDeptCode()), SysDept::getDeptCode, deptDTO.getDeptCode())
                .like(StringUtils.hasText(deptDTO.getLeader()), SysDept::getLeader, deptDTO.getLeader())
                .eq(deptDTO.getStatus() != null, SysDept::getStatus, deptDTO.getStatus())
                .orderByAsc(SysDept::getSort);

        Page<SysDept> deptPage = deptMapper.selectPage(page, wrapper);
        return PageVO.build(deptPage);
    }

    @Override
    public SysDept getDeptDetail(Long id) {
        SysDept dept = deptMapper.selectById(id);
        if (dept != null) {
            // 获取父部门名称
            if (dept.getParentId() != null && dept.getParentId() > 0) {
                SysDept parentDept = deptMapper.selectById(dept.getParentId());
                if (parentDept != null) {
                    dept.setParentName(parentDept.getDeptName());
                }
            }
        }
        return dept;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addDept(DeptDTO deptDTO) {
        // 检查部门编码是否存在
        if (checkDeptCodeExists(deptDTO.getDeptCode(), null)) {
            throw new RuntimeException("部门编码已存在");
        }

        // 检查父部门是否存在
        if (deptDTO.getParentId() != null && deptDTO.getParentId() > 0) {
            SysDept parentDept = deptMapper.selectById(deptDTO.getParentId());
            if (parentDept == null) {
                throw new RuntimeException("父部门不存在");
            }
            if (parentDept.getStatus() == 0) {
                throw new RuntimeException("父部门已禁用，无法添加子部门");
            }
        }

        SysDept dept = new SysDept();
        dept.setParentId(deptDTO.getParentId() != null ? deptDTO.getParentId() : 0L);
        dept.setDeptName(deptDTO.getDeptName());
        dept.setDeptCode(deptDTO.getDeptCode());
        dept.setLeader(deptDTO.getLeader());
        dept.setPhone(deptDTO.getPhone());
        dept.setEmail(deptDTO.getEmail());
        dept.setStatus(deptDTO.getStatus() != null ? deptDTO.getStatus() : 1);
        dept.setSort(deptDTO.getSort() != null ? deptDTO.getSort() : 0);
        dept.setRemark(deptDTO.getRemark());

        return deptMapper.insert(dept) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateDept(DeptDTO deptDTO) {
        SysDept dept = deptMapper.selectById(deptDTO.getId());
        if (dept == null) {
            throw new RuntimeException("部门不存在");
        }

        // 检查部门编码是否存在
        if (!dept.getDeptCode().equals(deptDTO.getDeptCode()) &&
                checkDeptCodeExists(deptDTO.getDeptCode(), deptDTO.getId())) {
            throw new RuntimeException("部门编码已存在");
        }

        // 不能将父节点设置为自己
        if (deptDTO.getParentId() != null && deptDTO.getParentId().equals(deptDTO.getId())) {
            throw new RuntimeException("不能将父部门设置为自己");
        }

        // 检查父部门是否存在
        if (deptDTO.getParentId() != null && deptDTO.getParentId() > 0) {
            SysDept parentDept = deptMapper.selectById(deptDTO.getParentId());
            if (parentDept == null) {
                throw new RuntimeException("父部门不存在");
            }
            // 检查是否将子部门设置为父部门
            if (isChildDept(deptDTO.getId(), deptDTO.getParentId())) {
                throw new RuntimeException("不能将子部门设置为父部门");
            }
        }

        dept.setParentId(deptDTO.getParentId());
        dept.setDeptName(deptDTO.getDeptName());
        dept.setDeptCode(deptDTO.getDeptCode());
        dept.setLeader(deptDTO.getLeader());
        dept.setPhone(deptDTO.getPhone());
        dept.setEmail(deptDTO.getEmail());
        dept.setStatus(deptDTO.getStatus());
        dept.setSort(deptDTO.getSort());
        dept.setRemark(deptDTO.getRemark());

        return deptMapper.updateById(dept) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteDept(Long id) {
        // 检查是否有子部门
        if (hasChildDept(id)) {
            throw new RuntimeException("存在子部门，无法删除");
        }

        // 检查部门下是否有用户
        if (hasUsers(id)) {
            throw new RuntimeException("部门下存在用户，无法删除");
        }

        // 删除部门用户关联
        userDeptMapper.delete(new LambdaQueryWrapper<SysUserDept>()
                .eq(SysUserDept::getDeptId, id));

        return deptMapper.deleteById(id) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteBatch(Long[] ids) {
        for (Long id : ids) {
            deleteDept(id);
        }
        return true;
    }

    @Override
    public boolean updateStatus(Long id, Integer status) {
        SysDept dept = new SysDept();
        dept.setId(id);
        dept.setStatus(status);
        return deptMapper.updateById(dept) > 0;
    }

    @Override
    public boolean checkDeptCodeExists(String deptCode, Long excludeId) {
        LambdaQueryWrapper<SysDept> wrapper = new LambdaQueryWrapper<SysDept>()
                .eq(SysDept::getDeptCode, deptCode);

        if (excludeId != null) {
            wrapper.ne(SysDept::getId, excludeId);
        }

        return deptMapper.selectCount(wrapper) > 0;
    }

    @Override
    public boolean hasChildDept(Long id) {
        return deptMapper.countChildDepts(id) > 0;
    }

    @Override
    public boolean hasUsers(Long id) {
        return deptMapper.countDeptUsers(id) > 0;
    }

    @Override
    public String getDeptFullName(Long id) {
        if (id == null || id <= 0) {
            return "";
        }

        List<String> names = new ArrayList<>();
        getDeptFullNameRecursive(id, names);

        return String.join(" / ", names);
    }

    /**
     * 递归获取部门完整名称
     */
    private void getDeptFullNameRecursive(Long id, List<String> names) {
        SysDept dept = deptMapper.selectById(id);
        if (dept != null) {
            names.add(0, dept.getDeptName());
            if (dept.getParentId() != null && dept.getParentId() > 0) {
                getDeptFullNameRecursive(dept.getParentId(), names);
            }
        }
    }

    /**
     * 检查是否将子部门设置为父部门
     */
    private boolean isChildDept(Long parentId, Long childId) {
        if (parentId == null || parentId <= 0) {
            return false;
        }

        SysDept child = deptMapper.selectById(childId);
        if (child == null) {
            return false;
        }

        return parentId.equals(child.getId()) || isChildDept(parentId, child.getParentId());
    }

    /**
     * 构建部门树
     */
    private List<SysDept> buildTree(List<SysDept> depts, Long parentId) {
        return depts.stream()
                .filter(dept -> dept.getParentId().equals(parentId))
                .peek(dept -> dept.setChildren(buildTree(depts, dept.getId())))
                .collect(Collectors.toList());
    }
}