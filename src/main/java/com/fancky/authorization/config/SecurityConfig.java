package com.fancky.authorization.config;


import com.fancky.authorization.filter.JwtAuthenticationFilter;
import com.fancky.authorization.filter.LoginFilter;
import com.fancky.authorization.service.JwtService;
import com.fancky.authorization.service.SysUserService;
import com.fancky.authorization.service.UserDetailsServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 配置类 (兼容 Spring Boot 2.7.x)
 * 使用 WebSecurityConfigurerAdapter 的方式
 */
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(
        prePostEnabled = true,
        securedEnabled = true,
        jsr250Enabled = true
)
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SysUserService userService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        /*
                //每次加密都会生成随机盐
                $2a$10$UozQnhVaPy.P2vIyG4PJW.kX4yKiIV35wh5pk.i2fooGFh7ZIyfyi
                 部分       长度
                算法 $2a$	4
                cost $10$	3
                salt	    22
                hash	    31
         */
        return new BCryptPasswordEncoder(10);
    }

    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userDetailsService)
                .passwordEncoder(passwordEncoder());
    }

    @Bean
    public LoginFilter loginFilter() throws Exception {
        LoginFilter loginFilter = new LoginFilter(
                authenticationManagerBean(),
                jwtService,
                objectMapper,
                userService
        );
        loginFilter.setFilterProcessesUrl("/api/auth/login");
        return loginFilter;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                // 启用CORS
                .cors().and()
                // 禁用CSRF（因为使用JWT，不需要CSRF）
                .csrf().disable()
                // 使用无状态session
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()

                // 配置请求授权
                .authorizeRequests()
                // 允许访问的公共接口
                .antMatchers("/api/auth/login").permitAll()
                .antMatchers("/api/auth/register").permitAll()
                .antMatchers("/api/auth/refresh").permitAll()
                .antMatchers("/api/auth/forgot-password").permitAll()
                .antMatchers("/api/auth/reset-password").permitAll()
                .antMatchers("/api/test/public").permitAll()
                .antMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
//                .antMatchers("/api/auth/getCurrentUser").permitAll()  // 临时放行
                // 其他所有请求都需要认证
                .anyRequest().authenticated()
                .and()

                // 添加过滤器
                .addFilterAt(loginFilter(), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
    }
}