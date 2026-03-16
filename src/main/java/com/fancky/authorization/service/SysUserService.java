package com.fancky.authorization.service;


import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.fancky.authorization.model.dto.CheckPermissionDto;
import com.fancky.authorization.model.dto.UserDTO;
import com.fancky.authorization.model.entity.SysUser;
import com.fancky.authorization.model.entity.SysUser;
import com.fancky.authorization.model.request.RefreshTokenRequest;
import com.fancky.authorization.model.request.RegisterRequest;
import com.fancky.authorization.model.response.PageVO;
import com.fancky.authorization.model.response.SysUserResponse;
import org.springframework.web.bind.annotation.RequestBody;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;


public interface SysUserService extends IService<SysUser> {

    /**
     * 根据用户名查询用户
     */
    SysUser getUserByUsername(String username) throws Exception;

    Map<String, String> refreshToken(RefreshTokenRequest request) throws Exception;

    SysUser getUserById(Long id) throws Exception;

    /**
     * 分页查询用户列表
     */
    PageVO<SysUser> getUserPage(UserDTO userDTO);

    /**
     * 新增用户
     */
    boolean addUser(UserDTO userDTO);

    /**
     * 更新用户
     */
    boolean updateUser(UserDTO userDTO);

    /**
     * 删除用户
     */
    boolean deleteUser(Long id);

    /**
     * 批量删除用户
     */
    boolean deleteBatch(Long[] ids);

    /**
     * 更新用户状态
     */
    boolean updateStatus(Long id, Integer enabled);

    /**
     * 重置密码
     */
    boolean resetPassword(Long id, String password);

    /**
     * 分配角色
     */
    boolean assignRoles(Long userId, Long[] roleIds);


    SysUser getUserWithRolesAndPermissions(String username) throws Exception;

    boolean register(RegisterRequest registerRequest);

    void updateLastLoginTime(String username);

    boolean changePassword(String username, String oldPassword, String newPassword) throws Exception;

    boolean resetPassword(String username, String newPassword);

    IPage<SysUser> getUserPage(Page<SysUser> page, String keyword);

    boolean batchUpdateStatus(List<Long> ids, Integer enabled);


    CheckPermissionDto checkPermission(CheckPermissionDto dto, HttpServletRequest request) throws Exception;
}
