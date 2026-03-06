package com.fancky.authorization.service;


import com.fancky.authorization.entity.User;
import org.springframework.security.core.userdetails.UserDetails;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

public interface JwtService {

    String generateAccessToken(User user);

    String generateRefreshToken(User user);

    String getUsernameFromToken(String token);

    Boolean validateToken(String token, UserDetails userDetails);

    String getTokenFromRequest(HttpServletRequest request);

    String refreshAccessToken(String refreshToken);

    void invalidateToken(String token);

    Long getTokenExpiresIn(String token);

    Map<String, Object> parseToken(String token);

    String getTokenPrefix();
}