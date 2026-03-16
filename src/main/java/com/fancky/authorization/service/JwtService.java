package com.fancky.authorization.service;


import com.fancky.authorization.model.entity.SysUser;
import org.springframework.security.core.userdetails.UserDetails;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

public interface JwtService {

    String generateAccessToken(SysUser user);

    String generateRefreshToken(SysUser user);

    String getUsernameFromToken(String token);

    Boolean validateToken(String token, UserDetails userDetails);

    String getUsername(HttpServletRequest request);

    String getTokenFromRequest(HttpServletRequest request);

    String refreshAccessToken(String refreshToken);

    void invalidateToken(String token);

    Long getTokenExpiresIn(String token);

    Map<String, Object> parseToken(String token);

    String getTokenPrefix();
}