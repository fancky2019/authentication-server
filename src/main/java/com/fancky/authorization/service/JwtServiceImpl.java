package com.fancky.authorization.service;


import com.fancky.authorization.entity.User;
import io.jsonwebtoken.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class JwtServiceImpl implements JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token-expiration}")
    private Long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration}")
    private Long refreshTokenExpiration;

    @Value("${jwt.token-prefix}")
    private String tokenPrefix;

    @Value("${jwt.header}")
    private String header;
    @Autowired
    private  StringRedisTemplate redisTemplate;
    @Autowired
    private  UserDetailsServiceImpl userDetailsService;

    private static final String BLACKLIST_PREFIX = "jwt:blacklist:";
    private static final String REFRESH_PREFIX = "jwt:refresh:";
    private static final String TOKEN_USER_PREFIX = "jwt:user:";



//    private final StringRedisTemplate redisTemplate;
//    private final UserDetailsServiceImpl userDetailsService;
//
//    private static final String BLACKLIST_PREFIX = "jwt:blacklist:";
//    private static final String REFRESH_PREFIX = "jwt:refresh:";
//    private static final String TOKEN_USER_PREFIX = "jwt:user:";
//
//    public JwtServiceImpl(StringRedisTemplate redisTemplate,
//                          UserDetailsServiceImpl userDetailsService) {
//        this.redisTemplate = redisTemplate;
//        this.userDetailsService = userDetailsService;
//    }

    @PostConstruct
    public void init() {
        log.info("JWT服务初始化完成，令牌有效期: {}ms, 刷新令牌有效期: {}ms",
                accessTokenExpiration, refreshTokenExpiration);
    }

    @Override
    public String generateAccessToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("id", user.getId());
        claims.put("username", user.getUsername());
        claims.put("nickname", user.getNickname());
        claims.put("roles", user.getRoles());
        claims.put("permissions", user.getPermissions());
        claims.put("type", "access");

        String token = Jwts.builder()
                .setClaims(claims)
                .setSubject(user.getUsername())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + accessTokenExpiration))
                .signWith(SignatureAlgorithm.HS512, secret)
                .compact();

        String userTokenKey = TOKEN_USER_PREFIX + user.getUsername();
        redisTemplate.opsForValue().set(userTokenKey, token,
                accessTokenExpiration, TimeUnit.MILLISECONDS);

        log.debug("生成访问令牌: {}", token);
        return token;
    }

    @Override
    public String generateRefreshToken(User user) {
        String token = Jwts.builder()
                .setSubject(user.getUsername())
                .setId(UUID.randomUUID().toString())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + refreshTokenExpiration))
                .signWith(SignatureAlgorithm.HS512, secret)
                .compact();

        String refreshKey = REFRESH_PREFIX + user.getUsername();
        redisTemplate.opsForValue().set(refreshKey, token,
                refreshTokenExpiration, TimeUnit.MILLISECONDS);

        log.debug("生成刷新令牌: {}", token);
        return token;
    }

    @Override
    public String getUsernameFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(secret)
                    .parseClaimsJws(token)
                    .getBody();
            return claims.getSubject();
        } catch (ExpiredJwtException e) {
            log.warn("令牌已过期: {}", e.getMessage());
            return e.getClaims().getSubject();
        } catch (JwtException e) {
            log.error("解析令牌失败: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public Boolean validateToken(String token, UserDetails userDetails) {
        if (!StringUtils.hasText(token)) {
            log.warn("令牌为空");
            return false;
        }

        if (isTokenBlacklisted(token)) {
            log.warn("令牌已在黑名单中");
            return false;
        }

        try {
            String username = getUsernameFromToken(token);

            if (username == null || !username.equals(userDetails.getUsername())) {
                log.warn("令牌用户名不匹配: {}", username);
                return false;
            }

            if (isTokenExpired(token)) {
                log.warn("令牌已过期");
                return false;
            }

            Claims claims = Jwts.parser()
                    .setSigningKey(secret)
                    .parseClaimsJws(token)
                    .getBody();
            String type = (String) claims.get("type");
            if (type != null && !"access".equals(type)) {
                log.warn("令牌类型错误: {}", type);
                return false;
            }

            log.debug("令牌验证通过: {}", username);
            return true;

        } catch (ExpiredJwtException e) {
            log.warn("令牌已过期: {}", e.getMessage());
            return false;
        } catch (JwtException e) {
            log.error("令牌验证失败: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(header);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(tokenPrefix + " ")) {
            return bearerToken.substring(tokenPrefix.length() + 1);
        }
        return null;
    }

    @Override
    public String refreshAccessToken(String refreshToken) {
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(secret)
                    .parseClaimsJws(refreshToken)
                    .getBody();

            String username = claims.getSubject();
            String tokenId = claims.getId();

            String refreshKey = REFRESH_PREFIX + username;
            String storedToken = redisTemplate.opsForValue().get(refreshKey);

            if (storedToken == null || !storedToken.equals(refreshToken)) {
                log.warn("刷新令牌无效或已过期: {}", username);
                throw new RuntimeException("无效的刷新令牌");
            }

            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            return generateAccessToken((User) userDetails);

        } catch (ExpiredJwtException e) {
            log.warn("刷新令牌已过期");
            throw new RuntimeException("刷新令牌已过期，请重新登录");
        } catch (JwtException e) {
            log.error("刷新令牌验证失败: {}", e.getMessage());
            throw new RuntimeException("无效的刷新令牌");
        }
    }

    @Override
    public void invalidateToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(secret)
                    .parseClaimsJws(token)
                    .getBody();

            String username = claims.getSubject();
            Date expiration = claims.getExpiration();

            long ttl = expiration.getTime() - System.currentTimeMillis();
            if (ttl > 0) {
                String blacklistKey = BLACKLIST_PREFIX + token;
                redisTemplate.opsForValue().set(blacklistKey, "1", ttl, TimeUnit.MILLISECONDS);
                log.debug("令牌加入黑名单: {}", token);
            }

            String refreshKey = REFRESH_PREFIX + username;
            redisTemplate.delete(refreshKey);

            String userTokenKey = TOKEN_USER_PREFIX + username;
            redisTemplate.delete(userTokenKey);

            log.info("用户已登出: {}", username);

        } catch (Exception e) {
            log.error("使令牌失效失败: {}", e.getMessage());
        }
    }

    @Override
    public Long getTokenExpiresIn(String token) {
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(secret)
                    .parseClaimsJws(token)
                    .getBody();
            Date expiration = claims.getExpiration();
            long diff = expiration.getTime() - System.currentTimeMillis();
            return diff > 0 ? diff / 1000 : 0L;
        } catch (Exception e) {
            return 0L;
        }
    }

    @Override
    public Map<String, Object> parseToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(secret)
                    .parseClaimsJws(token)
                    .getBody();
            return new HashMap<>(claims);
        } catch (Exception e) {
            log.error("解析令牌失败: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    @Override
    public String getTokenPrefix() {
        return tokenPrefix;
    }

    private Boolean isTokenExpired(String token) {
        try {
            Date expiration = Jwts.parser()
                    .setSigningKey(secret)
                    .parseClaimsJws(token)
                    .getBody()
                    .getExpiration();
            return expiration.before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        }
    }

    private Boolean isTokenBlacklisted(String token) {
        String blacklistKey = BLACKLIST_PREFIX + token;
        return Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey));
    }
}