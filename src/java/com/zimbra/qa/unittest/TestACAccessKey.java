package com.zimbra.qa.unittest;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpState;

import com.zimbra.common.auth.ZAuthToken;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.AuthTokenException;

public class TestACAccessKey {

    /*
     * for testing key grantees
     */
    public static class KeyAuthToken extends AuthToken {

        private String mName;
        private String mAccessKey;
        
        KeyAuthToken(String name, String accessKey) {
            mName = name;
            mAccessKey = accessKey;
        }
        
        @Override
        public void encode(HttpClient client, HttpMethod method,
                boolean isAdminReq, String cookieDomain) throws ServiceException {
            // TODO Auto-generated method stub

        }

        @Override
        public void encode(HttpState state, boolean isAdminReq, String cookieDomain)
                throws ServiceException {
            // TODO Auto-generated method stub

        }

        @Override
        public void encode(HttpServletResponse resp, boolean isAdminReq, boolean secureCookie)
                throws ServiceException {
            // TODO Auto-generated method stub

        }

        @Override
        public void encodeAuthResp(Element parent, boolean isAdmin)
                throws ServiceException {
            // TODO Auto-generated method stub

        }

        @Override
        public String getAccountId() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String getAdminAccountId() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String getCrumb() throws AuthTokenException {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String getDigest() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String getEncoded() throws AuthTokenException {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public long getExpires() {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public String getExternalUserEmail() {
            // TODO Auto-generated method stub
            return mName;
        }

        @Override
        public boolean isAdmin() {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean isDomainAdmin() {
            // TODO Auto-generated method stub
            return false;
        }
        
        @Override
        public boolean isDelegatedAdmin() {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean isExpired() {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean isZimbraUser() {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public String toString() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public ZAuthToken toZAuthToken() throws ServiceException {
            // TODO Auto-generated method stub
            return null;
        }
        
        public String getAccessKey() {
            return mAccessKey;
        }
    }
    
    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub

    }

}
