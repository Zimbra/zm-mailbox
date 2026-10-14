package com.zimbra.cs.service.account;

import java.util.Map;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Account;

/**
 * Provider SPI for Native OAuth 2.0 persistent session tokens.
 * Implemented by closed-source/NE extensions (such as zm-oauth-ext).
 */
public interface NativeOAuthProvider {

    /**
     * Issues persistent refresh credentials (refreshToken and refreshTokenSalt)
     * and appends them to the AuthResponse element on a permitted password login.
     */
    void issueRefreshToken(Account acct, Element request, Element response, Map<String, Object> context)
            throws ServiceException;

    /**
     * Handles incoming refresh token exchange requests (<AuthRequest> with <refreshToken>).
     * Validates cryptographic tokens, enforces rotation/DB policies, and returns the response.
     */
    Element handleRefresh(Account acct, Element request, Map<String, Object> context)
            throws ServiceException;
}