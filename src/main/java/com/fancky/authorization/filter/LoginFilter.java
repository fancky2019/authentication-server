package com.fancky.authorization.filter;


import com.fancky.authorization.model.request.LoginRequest;
import com.fancky.authorization.model.response.LoginResponse;
import com.fancky.authorization.model.response.Result;
import com.fancky.authorization.model.entity.User;
import com.fancky.authorization.service.JwtService;
import com.fancky.authorization.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;

/**
 * 专门用于处理用户名密码登录的认证过滤器。
 */
@Slf4j
public class LoginFilter extends UsernamePasswordAuthenticationFilter {
//    @Autowired
//    private  AuthenticationManager authenticationManager;
//    @Autowired
//    private  JwtService jwtService;
//    @Autowired
//    private  ObjectMapper objectMapper;
//    @Autowired
//    private  UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final ObjectMapper objectMapper;
    private final UserService userService;

    public LoginFilter(AuthenticationManager authenticationManager,
                       JwtService jwtService,
                       ObjectMapper objectMapper,
                       UserService userService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.objectMapper = objectMapper;
        this.userService = userService;
        setAuthenticationManager(authenticationManager);
        // 设置登录请求的URL
        this.setRequiresAuthenticationRequestMatcher(
                new AntPathRequestMatcher("/api/auth/login", HttpMethod.POST.name())
        );
        // 设置用户名和密码的参数名（可选，默认就是username和password）
        this.setUsernameParameter("username");
        this.setPasswordParameter("password");
    }

//    public LoginFilter(AuthenticationManager authenticationManager) {
//        this.setRequiresAuthenticationRequestMatcher(
//                new AntPathRequestMatcher("/api/auth/login", HttpMethod.POST.name())
//        );
//    }

    /**
     * 登录密码校验执行此方法
     * @param request
     * @param response
     * @return
     * @throws AuthenticationException
     */
    @Override
    public Authentication attemptAuthentication(HttpServletRequest request,
                                                HttpServletResponse response)
            throws AuthenticationException {

        if (!MediaType.APPLICATION_JSON_VALUE.equals(request.getContentType())) {
            throw new BadCredentialsException("请求格式必须是JSON");
        }

        try {
            LoginRequest loginRequest = objectMapper.readValue(
                    request.getInputStream(), LoginRequest.class
            );

            log.info("用户尝试登录: {}", loginRequest.getUsername());

            UsernamePasswordAuthenticationToken authRequest =
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    );

            setDetails(request, authRequest);

            return authenticationManager.authenticate(authRequest);

        } catch (IOException e) {
            log.error("解析登录请求失败: {}", e.getMessage());
            throw new BadCredentialsException("解析登录请求失败");
        }
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request,
                                            HttpServletResponse response,
                                            FilterChain chain,
                                            Authentication authResult)
            throws IOException {

        User user = (User) authResult.getPrincipal();

        userService.updateLastLoginTime(user.getUsername());

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        LoginResponse loginResponse = LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType(jwtService.getTokenPrefix())
                .userId(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .phone(user.getPhone())
                .avatar(user.getAvatar())
                .roles(user.getRoles())
                .permissions(user.getPermissions())
                .expiresIn(jwtService.getTokenExpiresIn(accessToken))
                .loginTime(new Date())
                .build();

        log.info("用户登录成功: {}, 角色: {}", user.getUsername(), user.getRoles());

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setStatus(HttpServletResponse.SC_OK);

        PrintWriter writer = response.getWriter();
        writer.write(objectMapper.writeValueAsString(Result.success(loginResponse)));
        writer.flush();
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request,
                                              HttpServletResponse response,
                                              AuthenticationException failed)
            throws IOException {

        log.warn("用户登录失败: {}", failed.getMessage());

        String message;
        int status = HttpServletResponse.SC_UNAUTHORIZED;

        if (failed instanceof BadCredentialsException) {
            message = "用户名或密码错误";
        } else if (failed instanceof DisabledException) {
            message = "账号已被禁用";
            status = HttpServletResponse.SC_FORBIDDEN;
        } else {
            message = "登录失败: " + failed.getMessage();
        }

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setStatus(status);

        PrintWriter writer = response.getWriter();
        writer.write(objectMapper.writeValueAsString(Result.error(status, message)));
        writer.flush();
    }
}