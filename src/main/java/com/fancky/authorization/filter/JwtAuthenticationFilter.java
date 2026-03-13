package com.fancky.authorization.filter;


import com.fancky.authorization.model.entity.*;
import com.fancky.authorization.model.response.Result;
import com.fancky.authorization.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.org.apache.bcel.internal.generic.RETURN;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.jce.exception.ExtException;
import org.bouncycastle.jce.exception.ExtIOException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * UsernamePasswordAuthenticationFilter	 处理登录认证 	/login
 * OncePerRequestFilter	                 通用过滤器基类	JWT / 日志 / CORS
 */
@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    @Autowired
    private JwtService jwtService;
    @Autowired
    private UserDetailsServiceImpl userDetailsService;
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SysUserRoleService sysUserRoleService;

    @Autowired
    private SysRoleService sysRoleService;

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private SysRolePermissionService sysRolePermissionService;
    @Autowired
    private SysPermissionService sysPermissionService;

//    private final JwtService jwtService;
//    private final UserDetailsServiceImpl userDetailsService;
//    private final ObjectMapper objectMapper;
//
//    public JwtAuthenticationFilter(JwtService jwtService,
//                                   UserDetailsServiceImpl userDetailsService,
//                                   ObjectMapper objectMapper) {
//        this.jwtService = jwtService;
//        this.userDetailsService = userDetailsService;
//        this.objectMapper = objectMapper;
//    }

    /**
     * 请求后台资源的接口经过此过滤器请求头携带 token
     * Authorization  Bearer eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ0ZXN0dXNlciIsInBlcm1pc3Npb25zIjpbInBlcm1pc3Npb25zMSIsInBlcm1pc3Npb25zMiIsInBlcm1pc3Npb25zMyIsImdldEN1cnJlbnRVc2VyIl0sInJvbGVzIjpbInJvbGVzMSIsInJvbGVzMiIsInJvbGVzMyJdLCJuaWNrbmFtZSI6Iua1i-ivleeUqOaItyIsImlkIjoxLCJ0eXBlIjoiYWNjZXNzIiwiZXhwIjoxNzcyNzc3NTAzLCJpYXQiOjE3NzI3NzU3MDMsInVzZXJuYW1lIjoidGVzdHVzZXIifQ.6xEWMDafRda-ON_wJqXEyeiDKGWkcWLLpJeejUhG0oP3lfq_dTZxEHNbIdeO29gTyBshfmaZ3eplUnwIpX_2Vw
     * @param request
     * @param response
     * @param filterChain
     * @throws ServletException
     * @throws IOException
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        try {
            String token = jwtService.getTokenFromRequest(request);

            // 情况1：没有token - 继续执行（可能访问公开接口）
            if (token == null) {
                log.debug("请求中没有token，继续执行");
                filterChain.doFilter(request, response);
                return;
            }

            // 情况2：有token，解析用户名
            String username = jwtService.getUsernameFromToken(token);
            if (username == null) {
                log.warn("无法从token中解析用户名");
                sendUnauthorizedResponse(response, "无效的令牌");
                return;
            }

            // 情况3：已经认证过，直接继续
            if (SecurityContextHolder.getContext().getAuthentication() != null) {
                filterChain.doFilter(request, response);
                return;
            }

            // 情况4：加载用户并验证token
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            if (jwtService.validateToken(token, userDetails)) {
                // 验证成功，设置认证信息
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );

                authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );


                //设置用户信息
                SecurityContextHolder.getContext().setAuthentication(authToken);
                //获取当前用户
                Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
                if (principal instanceof UserDetails) {
                    UserDetails userDetails1 = (UserDetails) principal;
                } else {
                    principal.toString();
                }

                log.debug("用户认证成功: {}, 权限: {}", username, userDetails.getAuthorities());
                //路由权限认证
//                // 检查特定接口的权限
//                if (!checkEndpointPermission(userDetails, requestURI, method)) {
//                    log.warn("用户 {} 没有权限访问: {}", username, requestURI);
//                    sendForbiddenResponse(response, "无权限访问");
//                    return;
//                }
                String userName=userDetails.getUsername();
//                userName="12";
                SysUser sysUser = sysUserService.getUserByUsername(userName);
                if (sysUser == null) {
                    throw new Exception("用户不存在");
                }
                List<SysUserRole> userRoles = sysUserRoleService.getUserRoles(sysUser.getId());
                if (CollectionUtils.isEmpty(userRoles)) {
                    sendForbiddenResponse(response, "用户没有角色信息");
                    return;
                }
                String superAdmin = "ROLE_SUPER_ADMIN";


                List<Long> roleIdList = userRoles.stream().map(p -> p.getRoleId()).distinct().collect(Collectors.toList());

//                List<Long> idList1= Arrays.asList(99L);
                List<SysRole> sysRoleList = this.sysRoleService.getRoleByIds(roleIdList);
                List<String> sysRoleCodeList = sysRoleList.stream().map(p -> p.getRoleCode()).distinct().collect(Collectors.toList());
                if (sysRoleCodeList.contains(superAdmin)) {

                } else {
                    //  /authentication-server/api/auth/getCurrentUser
                    String requestURI = request.getRequestURI();
                    // /api/auth/getCurrentUser
                    String servletPath = request.getServletPath();
                    List<SysRolePermission> sysRolePermissionList = this.sysRolePermissionService.getPermissionsByRoleIds(roleIdList);
                    if (CollectionUtils.isEmpty(sysRolePermissionList)) {
                        sendForbiddenResponse(response, "用户没有角色权限信息");
                        return;
                    }
                    List<Long> sysPermissionIdList = sysRolePermissionList.stream().map(p -> p.getPermissionId()).distinct().collect(Collectors.toList());
                    List<SysPermission> permissionList = sysPermissionService.getPermissions(sysPermissionIdList);
                    if (CollectionUtils.isEmpty(permissionList)) {
                        sendForbiddenResponse(response, "用户没有权限信息");
                        return;
                    }
                    List<String> pathList = permissionList.stream().filter(p -> StringUtils.isNotEmpty(p.getPath())).map(p -> p.getPath()).distinct().collect(Collectors.toList());
                    if (!pathList.contains(servletPath)) {
                        sendForbiddenResponse(response, "用户没有该权限");
                        return;
                    }
                }
                //验证通过
                filterChain.doFilter(request, response);
            } else {
                // 验证失败，直接返回401
                log.warn("令牌验证失败: {}", username);
                sendUnauthorizedResponse(response, "令牌无效或已过期");
            }

        } catch (UsernameNotFoundException e) {
            log.error("用户不存在: {}", e.getMessage());
            sendUnauthorizedResponse(response, "用户不存在");
        } catch (ExpiredJwtException e) {
            log.error("令牌已过期: {}", e.getMessage());
            sendUnauthorizedResponse(response, "令牌已过期");
        } catch (JwtException e) {
            log.error("JWT解析错误: {}", e.getMessage());
            sendUnauthorizedResponse(response, "令牌格式错误");
        } catch (Exception e) {
            log.error("JWT认证过程中发生错误: ", e);
            sendUnauthorizedResponse(response, "认证失败");
        }
    }

    /**
     * 发送401未授权响应
     */
    private void sendUnauthorizedResponse(HttpServletResponse response, String message)
            throws IOException {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        PrintWriter writer = response.getWriter();
        writer.write(objectMapper.writeValueAsString(
                Result.error(401, message)
        ));
        writer.flush();
    }

    /**
     * 发送401未授权响应
     */
    private void sendForbiddenResponse(HttpServletResponse response, String message)
            throws IOException {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);

        PrintWriter writer = response.getWriter();
        writer.write(objectMapper.writeValueAsString(
                Result.error(403, message)
        ));
        writer.flush();
    }

    /**
     * 白名单
     * @param request
     * @return
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // 登录注册刷新忘记密码等接口直接跳过
        return path.equals("/api/auth/login") ||
                path.equals("/api/auth/register") ||
                path.equals("/api/auth/refresh") ||
                path.equals("/api/auth/forgot-password") ||
                path.equals("/api/auth/reset-password") ||
                path.equals("/api/test/public");
    }
}
