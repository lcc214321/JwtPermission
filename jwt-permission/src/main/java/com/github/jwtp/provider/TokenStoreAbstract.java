package com.github.jwtp.provider;

import com.github.jwtp.exception.ErrorTokenException;
import com.github.jwtp.exception.ExpiredTokenException;
import com.github.jwtp.util.Hex;
import com.github.jwtp.util.SecureUtil;
import com.github.jwtp.util.TokenUtil;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * 操作token的接口
 * Created by wangfan on 2018-12-28 上午 9:21.
 */
public abstract class TokenStoreAbstract implements TokenStore {
    protected final Log logger = LogFactory.getLog(this.getClass());

    /**
     * 单个用户最大的token数量
     */
    private Integer maxToken = -1;

    /**
     * 查询用户角色的sql
     */
    private String findRolesSql;

    /**
     * 查询用户权限的sql
     */
    private String findPermissionsSql;

    /**
     * 生成token用的Key
     */
    public String mTokenKey;

    @Override
    public Token createNewToken(String userId) {
        return createNewToken(userId, null, null);
    }

    @Override
    public Token createNewToken(String userId, long expire) {
        return createNewToken(userId, null, null, expire);
    }

    @Override
    public Token createNewToken(String userId, long expire, long rtExpire) {
        return createNewToken(userId, null, null, expire, rtExpire);
    }

    @Override
    public Token createNewToken(String userId, String[] permissions, String[] roles) {
        return createNewToken(userId, permissions, roles, TokenUtil.DEFAULT_EXPIRE);
    }

    @Override
    public Token createNewToken(String userId, String[] permissions, String[] roles, long expire) {
        return createNewToken(userId, permissions, roles, expire, TokenUtil.DEFAULT_EXPIRE_REFRESH_TOKEN);
    }

    @Override
    public Token createNewToken(String userId, String[] permissions, String[] roles, long expire, long rtExpire) {
        String tokenKey = getTokenKey();
        logger.debug("TOKEN_KEY: " + tokenKey);
        Token token = TokenUtil.buildToken(userId, expire, rtExpire, TokenUtil.parseHexKey(tokenKey));
        token.setRoles(roles);
        token.setPermissions(permissions);
        if (storeToken(token) > 0) {
            return token;
        }
        return null;
    }

    @Override
    public Token refreshToken(String refresh_token) {
        return refreshToken(refresh_token, TokenUtil.DEFAULT_EXPIRE);
    }

    @Override
    public Token refreshToken(String refresh_token, long expire) {
        return refreshToken(refresh_token, null, null, expire);
    }

    @Override
    public Token refreshToken(String refresh_token, String[] permissions, String[] roles, long expire) {
        String tokenKey = getTokenKey();
        logger.debug("TOKEN_KEY: " + tokenKey);
        String userId;
        try {
            userId = TokenUtil.parseToken(refresh_token, tokenKey);
        } catch (ExpiredJwtException e) {
            throw new ExpiredTokenException();
        } catch (Exception e) {
            throw new ErrorTokenException();
        }
        if (userId != null) {
            // 检查token是否存在系统中
            Token refreshToken = findRefreshToken(userId, refresh_token);
            if (refreshToken == null) {
                throw new ErrorTokenException();
            }
            // 生成新的token
            Token token = TokenUtil.buildToken(userId, expire, null, TokenUtil.parseHexKey(tokenKey), false);
            token.setRoles(roles);
            token.setPermissions(permissions);
            token.setRefreshToken(refresh_token);
            token.setRefreshTokenExpireTime(refreshToken.getRefreshTokenExpireTime());
            if (storeToken(token) > 0) {
                return token;
            }
        }
        return null;
    }

    @Override
    public void setMaxToken(Integer maxToken) {
        this.maxToken = maxToken;
    }

    @Override
    public void setFindRolesSql(String findRolesSql) {
        this.findRolesSql = findRolesSql;
    }

    @Override
    public void setFindPermissionsSql(String findPermissionsSql) {
        this.findPermissionsSql = findPermissionsSql;
    }

    @Override
    public Integer getMaxToken() {
        return maxToken;
    }

    @Override
    public String getFindRolesSql() {
        return findRolesSql;
    }

    @Override
    public String getFindPermissionsSql() {
        return findPermissionsSql;
    }

    @Override
    public void setMTokenKey(String secretKey) {
        String s = SecureUtil.md5(secretKey);
        SecretKey secretKeySpec = new SecretKeySpec(s.getBytes(), SignatureAlgorithm.HS256.getJcaName());
        byte[] encoded = secretKeySpec.getEncoded();
        String hexStr = Hex.encodeToString(encoded);
        this.mTokenKey = hexStr;
    }

    @Override
    public String getMTokenKey() {
        return mTokenKey;
    }

}