/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
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
package com.zimbra.cs.jsp.tag;

import java.io.IOException;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspContext;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.PageContext;

import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.jsp.ZJspSession;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.zclient.ZGetInfoResult;
import com.zimbra.cs.zclient.ZMailbox;

public class LoginTag extends ZimbraSimpleTag {
    
    private String mUsername;
    private String mPassword;
    private boolean mRememberMe;
    private String mUrl = ZJspSession.SOAP_URL;
    
    public void setUsername(String username) { this.mUsername = username; }
    
    public void setPassword(String password) { this.mPassword = password; }
    
    public void setRememberme(boolean rememberMe) { this.mRememberMe = rememberMe; }
    
    public void setUrl(String url) { this.mUrl = url; }

    public void doTag() throws JspException, IOException {
        JspContext jctxt = getJspContext();
        try {
            ZMailbox mbox = ZMailbox.getMailbox(mUsername, AccountBy.name, mPassword, mUrl, null);
            PageContext pageContext = (PageContext) jctxt;
            HttpServletResponse response = (HttpServletResponse) pageContext.getResponse();
            Cookie authTokenCookie = new Cookie(ZJspSession.COOKIE_NAME, mbox.getAuthToken());
            if (mRememberMe) {
                ZGetInfoResult info = mbox.getAccountInfo(false);
                long timeLeft = info.getExpiration() - System.currentTimeMillis();
                if (timeLeft > 0) authTokenCookie.setMaxAge((int) (timeLeft/1000));
            } else {
                authTokenCookie.setMaxAge(-1);
            }
            response.addCookie(authTokenCookie);
            setMailbox(mbox.getAuthToken(), mbox);
        } catch (ServiceException e) {
            throw new JspTagException(e.getMessage(), e);
        }
    }
}
