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
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.jsp.tag;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import javax.servlet.jsp.JspContext;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.JspFragment;

import com.zimbra.cs.jsp.bean.ZFolderBean;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.zclient.ZFolder;
import com.zimbra.cs.zclient.ZMailbox;

public class ForEachFolderTag extends ZimbraSimpleTag {
    
    private String mVar;
    
    public void setVar(String var) { this.mVar = var; }
    
    public void doTag() throws JspException, IOException {
        JspFragment body = getJspBody();
        if (body == null) return;
        
        try {
            ZMailbox mbox = getMailbox();
            JspContext jctxt = getJspContext();
            handleFolder(mbox, mbox.getUserRoot(), body, jctxt, true);
        } catch (ServiceException e) {
            getJspContext().getOut().write(e.toString());
        }
    }    
    
    private void handleFolder(ZMailbox mbox, ZFolder folder, JspFragment body, JspContext jctxt, boolean skip)
    throws ServiceException, JspException, IOException {
        if (!skip) {
            jctxt.setAttribute(mVar, new ZFolderBean(folder));
            body.invoke(null);
        }
        List<ZFolder> subfolders = folder.getSubFolders();
        for (Iterator<ZFolder> it = subfolders.iterator(); it.hasNext(); ) {
                ZFolder subfolder = it.next();
                if (subfolder != null)
                    handleFolder(mbox, subfolder, body, jctxt, false);
        }
    }
}