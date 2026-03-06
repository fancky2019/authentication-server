package com.fancky.authorization.service;


import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.fancky.authorization.entity.RegisterRequest;
import com.fancky.authorization.entity.User;
import com.fancky.authorization.entity.UserInfoVO;


import java.util.List;

public interface UserService extends IService<User> {

    User getUserWithRolesAndPermissions(String username);

    boolean register(RegisterRequest registerRequest);

    void updateLastLoginTime(String username);

    boolean changePassword(String username, String oldPassword, String newPassword);

    boolean resetPassword(String username, String newPassword);

    IPage<User> getUserPage(Page<User> page, String keyword);

    boolean batchUpdateStatus(List<Long> ids, Integer enabled);

    UserInfoVO convertToVO(User user);
}