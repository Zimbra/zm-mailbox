/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.zclient.soap;

import java.io.IOException;

import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.account.AccountService;
import com.zimbra.cs.zclient.ZAccount;
import com.zimbra.cs.zclient.ZClientException;
import com.zimbra.cs.zclient.ZMailbox;
import com.zimbra.soap.Element;
import com.zimbra.soap.SoapFaultException;
import com.zimbra.soap.SoapHttpTransport;
import com.zimbra.soap.SoapTransport;
import com.zimbra.soap.Element.XMLElement;

public class ZSoapAccount extends ZAccount {

    private String mAuthToken;
    private long mAuthTokenExpiration;
    private SoapHttpTransport mTransport;
    private SoapTransport.DebugListener mListener;

    public ZSoapAccount(String key, AccountBy by, String password, String uri, SoapTransport.DebugListener listener) throws ServiceException {
        mListener = listener;        
        setSoapURI(uri);
        AuthResult ar = auth(key, by, password);
        mAuthToken = ar.getAuthToken();
        mAuthTokenExpiration = ar.getExpires();
        mTransport.setAuthToken(mAuthToken);
    }

    public ZSoapAccount(String authToken, String uri, SoapTransport.DebugListener listener) throws ServiceException {
        mListener = listener;        
        setSoapURI(uri);
        mAuthToken = authToken;
        mTransport.setAuthToken(mAuthToken);
    }

    /**
     * @param uri URI of server we want to talk to
     */
    void setSoapURI(String uri) {
        if (mTransport != null) mTransport.shutdown();
        mTransport = new SoapHttpTransport(uri);
        mTransport.setUserAgent("zclient", "1.0");
        mTransport.setMaxNoitfySeq(0);
        if (mAuthToken != null)
            mTransport.setAuthToken(mAuthToken);
        if (mListener != null)
            mTransport.setDebugListener(mListener);
    }    

    private static class AuthResult {
        private String mAuthToken;
        private long mExpires;
        private long mLifetime;
        private String mRefer;

        AuthResult(Element e) throws ServiceException {
            mAuthToken = e.getElement(AccountService.E_AUTH_TOKEN).getText();
            mLifetime = e.getAttributeLong(AccountService.E_LIFETIME);
            mExpires = System.currentTimeMillis() + mLifetime;
            Element re = e.getOptionalElement(AccountService.E_REFERRAL); 
            if (re != null) mRefer = re.getText();
        }

        public String getAuthToken() {
            return mAuthToken;
        }
        
        public long getExpires() {
            return mExpires;
        }
        
        public long getLifetime() {
            return mLifetime;
        }
        
        public String getRefer() {
            return mRefer;
        }
    }

    private AuthResult auth(String key, AccountBy by, String password) throws ServiceException {
        if (mTransport == null) throw ZClientException.CLIENT_ERROR("must call setURI before calling asuthenticate", null);
        XMLElement req = new XMLElement(AccountService.AUTH_REQUEST);
        Element account = req.addElement(AccountService.E_ACCOUNT);
        account.addAttribute(AccountService.A_BY, by.name());
        account.setText(key);
        req.addElement(AccountService.E_PASSWORD).setText(password);
        return new AuthResult(invoke(req));
    }

    synchronized Element invoke(Element request) throws ServiceException {
        try {
            return mTransport.invoke(request, false, true, true, null);
        } catch (SoapFaultException e) {
            throw e; // for now, later, try to map to more specific exception
        } catch (IOException e) {
            throw ZClientException.IO_ERROR("invoke "+e.getMessage(), e);
        }
    }

    @Override
    public ZMailbox getMailbox() throws ServiceException {
        return ZSoapMailbox.getMailbox(mAuthToken, mTransport.getURI(), mListener);
    }
  
}

