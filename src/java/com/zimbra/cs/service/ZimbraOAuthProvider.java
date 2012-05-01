package com.zimbra.cs.service;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.google.common.base.Strings;
import com.zimbra.common.auth.ZAuthToken;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.StringUtil;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.AuthTokenException;
import com.zimbra.cs.account.ZimbraAuthToken;

/**
 * @author pshao
 */
public class ZimbraOAuthProvider extends ZimbraAuthProvider {
    
    public static final String ZIMBRA_OAUTH_PROVIDER = "oauth";
    
    public static final String OAUTH_ACCESS_TOKEN = "access_token";

    protected ZimbraOAuthProvider() {
        super(ZIMBRA_OAUTH_PROVIDER);
    }

    @Override
    protected AuthToken authToken(HttpServletRequest req, boolean isAdminReq)
            throws AuthProviderException, AuthTokenException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected AuthToken authToken(Element soapCtxt, Map engineCtxt)
            throws AuthProviderException, AuthTokenException {
        // TODO Auto-generated method stub
        return null;
    }
    
    @Override
    protected AuthToken authToken(Element authTokenElem, Account acct) 
    throws AuthProviderException, AuthTokenException {
        ZAuthToken zAuthToken;
        try {
            zAuthToken = new ZAuthToken(authTokenElem, false);
        } catch (ServiceException e) {
            throw AuthProviderException.FAILURE(e.getMessage());
        }
        
        if (ZIMBRA_OAUTH_PROVIDER.equals(zAuthToken.getType())) {
            Map<String, String> attrs = zAuthToken.getAttrs();
            
            // TODO: no validation of access_token in IronMaiden D4!!!
            String accessToken = attrs.get(OAUTH_ACCESS_TOKEN);
            if (Strings.isNullOrEmpty(accessToken)) {
                throw new AuthTokenException("no oauth access token");
            }
            
            return authToken(acct);
        } else {
            throw AuthProviderException.NO_AUTH_DATA();
        }
    }

}
