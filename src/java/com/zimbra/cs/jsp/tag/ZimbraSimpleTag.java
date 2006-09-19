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

import javax.servlet.jsp.JspContext;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.SimpleTagSupport;

import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.zclient.ZMailbox;

public abstract class ZimbraSimpleTag extends SimpleTagSupport {

    private static final String ATTR_ACCOUNT = "ZimbraSimpleTag.account";
    private static final String ATTR_ZMAILBOX = "ZimbraSimpleTag.zmailbox";
    private static final String ATTR_OPERATION_CONTEXT = "ZimbraSimpleTag.operationcontext";    
    
    public ZMailbox getMailbox() throws JspException { 
        try {
            JspContext jc = getJspContext();
            ZMailbox mbox = (ZMailbox) jc.getAttribute(ATTR_ZMAILBOX, PageContext.SESSION_SCOPE);
            if (mbox == null) {
                mbox = ZMailbox.getMailbox("user1", AccountBy.name, "test123", "http://localhost:7070/service/soap/", null);
                jc.setAttribute(ATTR_ZMAILBOX, mbox, PageContext.SESSION_SCOPE);
            }
            return mbox;
        } catch (ServiceException e) {
            throw new JspException("getMailbox", e);
        }
    }
}