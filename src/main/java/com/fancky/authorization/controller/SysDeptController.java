package com.fancky.authorization.controller;


import com.fancky.authorization.model.dto.DeptDTO;
import com.fancky.authorization.model.entity.SysDept;
import com.fancky.authorization.model.response.PageVO;
import com.fancky.authorization.model.response.Result;
import com.fancky.authorization.service.SysDeptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/dept")
public class SysDeptController {

    @Autowired
    private SysDeptService deptService;

    /**
     * 获取部门树
     */
    @GetMapping("/tree")
    public Result<List<SysDept>> getTree() {
        List<SysDept> tree = deptService.getDeptTree();
        return Result.success(tree);
    }

    /**
     * 获取正常状态的部门树
     */
    @GetMapping("/normal-tree")
    public Result<List<SysDept>> getNormalTree() {
        List<SysDept> tree = deptService.getNormalDeptTree();
        return Result.success(tree);
    }

    /**
     * 分页查询部门列表
     */
    @GetMapping("/page")
    public Result<PageVO<SysDept>> page(DeptDTO deptDTO) {
        PageVO<SysDept> page = deptService.getDeptPage(deptDTO);
        return Result.success(page);
    }

    /**
     * 查询所有部门列表
     */
    @GetMapping("/list")
    public Result<List<SysDept>> list() {
        List<SysDept> list = deptService.list();
        return Result.success(list);
    }

    /**
     * 查询部门详情
     */
    @GetMapping("/{id}")
    public Result<SysDept> getById(@PathVariable Long id) {
        SysDept dept = deptService.getDeptDetail(id);
        return Result.success(dept);
    }

    /**
     * 新增部门
     */
    @PostMapping
    public Result<String> add(@Valid @RequestBody DeptDTO deptDTO) {
        deptService.addDept(deptDTO);
        return Result.success("新增部门成功");
    }

    /**
     * 修改部门
     */
    @PutMapping
    public Result<String> update(@Valid @RequestBody DeptDTO deptDTO) {
        deptService.updateDept(deptDTO);
        return Result.success("修改部门成功");
    }

    /**
     * 删除部门
     */
    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        deptService.deleteDept(id);
        return Result.success("删除部门成功");
    }

    /**
     * 批量删除部门
     */
    @DeleteMapping("/batch")
    public Result<String> deleteBatch(@RequestBody Long[] ids) {
        deptService.deleteBatch(ids);
        return Result.success("批量删除成功");
    }

    /**
     * 修改部门状态
     */
    @PutMapping("/status")
    public Result<String> updateStatus(@RequestParam Long id, @RequestParam Integer status) {
        deptService.updateStatus(id, status);
        return Result.success("修改状态成功");
    }

    /**
     * 检查部门编码是否存在
     */
    @GetMapping("/check-code")
    public Result<Boolean> checkDeptCode(@RequestParam String deptCode,
                                         @RequestParam(required = false) Long id) {
        boolean exists = deptService.checkDeptCodeExists(deptCode, id);
        return Result.success(exists);
    }

    /**
     * 获取部门完整名称
     */
    @GetMapping("/full-name/{id}")
    public Result<String> getDeptFullName(@PathVariable Long id) {
        String fullName = deptService.getDeptFullName(id);
        return Result.success(fullName);
    }
}
