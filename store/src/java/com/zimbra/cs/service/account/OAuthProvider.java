package com.zimbra.cs.service.account;

import java.util.Map;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.AuthToken.Usage;

public interface OAuthProvider {

    /**
     * Generic token issuance for Native OAuth flows.
     *
     * @param acct      Target Zimbra account
     * @param usage     Usage.AUTH (access token) or Usage.REFRESH_TOKEN (refresh token)
     * @param request   Incoming SOAP AuthRequest element
     * @param response  SOAP response element (can be null when minting pre-response access token)
     * @param context   SOAP context map
     * @return Generated AuthToken instance
     */
    AuthToken issueToken(Account acct, Usage usage, Element request, Element response, Map<String, Object> context)
            throws ServiceException;

    /**
     * Handles refresh token exchange flow
     */
    Element handleRefresh(Account acct, Element request, Map<String, Object> context)
            throws ServiceException;
}