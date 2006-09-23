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
package com.zimbra.cs.jsp.bean;

import javax.servlet.jsp.JspTagException;

import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.service.ServiceException;

public class ZExceptionBean {

    private Exception mException;
    
    public ZExceptionBean(Exception e) {
        if (e instanceof JspTagException) {
            mException = (Exception) ((JspTagException) e).getRootCause();
        } else if (e instanceof ServiceException) {
            
        } else  if (e.getCause() instanceof ServiceException) {
            mException = (ServiceException) e.getCause();
        } else {
            mException = e;            
        }
    }

    public boolean getIsServiceException() {
        return mException instanceof ServiceException;
    }
    
    public String getCode() {
        return getIsServiceException() ? ((ServiceException)mException).getCode() : null;
    }
    
    public String getDisplayMessage() {
        String code = getCode();
        if (code == null) return "ERROR: "+mException.getMessage();
        
        if (code.equals(AccountServiceException.AUTH_FAILED)) {
            return "The username or password is incorrect. Verify that CAPS LOCK is not on, and then retype the current username and password";
        } else {
            return mException.getMessage();
        }
    }
}
