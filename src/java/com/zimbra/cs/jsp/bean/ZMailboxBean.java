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

import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.zclient.ZFolder;
import com.zimbra.cs.zclient.ZMailbox;

public class ZMailboxBean {

    private ZMailbox mMbox;
    
    public ZMailboxBean(ZMailbox mbox) {
        mMbox = mbox;
    }
    
    public String getName() throws ServiceException { return mMbox.getName(); }
    
    public long getSize() { return mMbox.getSize(); }
       
    private ZFolderBean getFolderBeanById(String id) {
        ZFolder folder = mMbox.getFolderById(id);
        return folder == null ? null : new ZFolderBean(folder);
    }
    
    public ZFolderBean getInbox() { return getFolderBeanById(ZFolder.ID_INBOX); }
    
    public ZFolderBean getTrash() { return getFolderBeanById(ZFolder.ID_TRASH); }
    
    public ZFolderBean getSpam() { return getFolderBeanById(ZFolder.ID_SPAM); }
    
    public ZFolderBean getSent() { return getFolderBeanById(ZFolder.ID_SENT); }
    
    public ZFolderBean getDrafts() { return getFolderBeanById(ZFolder.ID_DRAFTS); }
    
    public ZFolderBean getCalendar() { return getFolderBeanById(ZFolder.ID_CALENDAR); }
    
    public ZFolderBean getContacts() { return getFolderBeanById(ZFolder.ID_CONTACTS); }    
    
    public ZFolderBean getAutoContacts() { return getFolderBeanById(ZFolder.ID_AUTO_CONTACTS); }
    
}
