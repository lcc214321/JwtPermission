package com.yilers.jwtp.util;

import com.yilers.jwtp.provider.Token;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.security.Key;
import java.util.Date;

/**
 * Token工具类
 * <p>
 * Created by wangfan on 2018-1-21 下午4:30:59
 */
public class TokenUtil {
    /**
     * 默认token过期时长,单位秒
     */
    public static final long DEFAULT_EXPIRE = 60 * 60;
    /**
     * 默认refresh_token过期时长,单位秒
     */
    public static final long DEFAULT_EXPIRE_REFRESH_TOKEN = 60 * 60 * 24 * 30 * 3;

    /**
     * 生成token
     *
     * @param subject 载体
     * @return Token
     */
    public static Token buildToken(String subject) {
        return buildToken(subject, DEFAULT_EXPIRE);
    }

    /**
     * 生成token
     *
     * @param subject 载体
     * @param expire  token过期时间，单位秒
     * @return Token
     */
    public static Token buildToken(String subject, long expire) {
        return buildToken(subject, expire, DEFAULT_EXPIRE_REFRESH_TOKEN);
    }

    /**
     * 生成token
     *
     * @param subject  载体
     * @param expire   token过期时间，单位秒
     * @param rtExpire refresh_token过期时间，单位秒
     * @return
     */
    public static Token buildToken(String subject, long expire, long rtExpire) {
        return buildToken(subject, expire, rtExpire, getKey());
    }

    /**
     * 生成token
     *
     * @param subject  载体
     * @param expire   token过期时间，单位秒
     * @param rtExpire refresh_token过期时间，单位秒
     * @param key      密钥
     * @return Token
     */
    public static Token buildToken(String subject, Long expire, Long rtExpire, Key key) {
        return buildToken(subject, expire, rtExpire, key, true);
    }

    /**
     * 生成token
     *
     * @param subject  载体
     * @param expire   token过期时间，单位秒
     * @param rtExpire refresh_token过期时间，单位秒
     * @param key      密钥
     * @param needRt   是否生成refresh_token
     * @return Token
     */
    public static Token buildToken(String subject, Long expire, Long rtExpire, Key key, boolean needRt) {
        Date expireDate = new Date(System.currentTimeMillis() + 1000 * expire);
        // 生成access_token
        String access_token = Jwts.builder().setSubject(subject).signWith(key).setExpiration(expireDate).compact();
        // 构建Token对象
        Token token = new Token();
        token.setUserId(subject);
        token.setAccessToken(access_token);
        token.setExpireTime(expireDate);
        // 生成refresh_token
        if (needRt) {
            Date refreshExpireDate = new Date(System.currentTimeMillis() + 1000 * rtExpire);
            String refresh_token = Jwts.builder().setSubject(subject).signWith(key).setExpiration(refreshExpireDate).compact();
            token.setRefreshToken(refresh_token);
            token.setRefreshTokenExpireTime(refreshExpireDate);
        }
        return token;
    }

    /**
     * 解析token
     *
     * @param token  token
     * @param hexKey 16进制密钥
     * @return 载体
     */
    public static String parseToken(String token, String hexKey) {
        Jws<Claims> claimsJws = Jwts.parserBuilder().setSigningKey(parseHexKey(hexKey)).build().parseClaimsJws(token);
        return claimsJws.getBody().getSubject();
    }

    /**
     * 生成密钥Key
     *
     * @return Key
     */
    public static Key getKey() {
        return Keys.secretKeyFor(SignatureAlgorithm.HS256);
    }

    /**
     * 生成16进制的key
     *
     * @return hexKey
     */
    public static String getHexKey() {
        return getHexKey(getKey());
    }

    /**
     * 生成16进制的key
     *
     * @param key 密钥Key
     * @return hexKey
     */
    public static String getHexKey(Key key) {
        return Hex.encodeToString(key.getEncoded());
    }

    /**
     * 把16进制的key解析成Key
     *
     * @param hexKey 16进制key
     * @return Key
     */
    public static Key parseHexKey(String hexKey) {
        if (hexKey == null || hexKey.trim().isEmpty()) {
            return null;
        }
        SecretKey key = Keys.hmacShaKeyFor(Hex.decode(hexKey));
        return key;
    }

}
