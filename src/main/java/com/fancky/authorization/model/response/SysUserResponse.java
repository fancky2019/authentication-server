package com.fancky.authorization.model.response;


import com.fancky.authorization.model.entity.BaseEntity;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 登录成功返回的消息体
 */
@Data
@Builder
public class SysUserResponse extends BaseEntity  {

    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long userId;
    private String username;
    private String nickname;
    private String email;
    private String phone;
    private String avatar;
    private List<String> roles;
    private List<String> permissions;
    private List<String> permissionPathList;
    private Long expiresIn;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date loginTime;
}