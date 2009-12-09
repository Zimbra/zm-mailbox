package com.zimbra.cs.account.accesscontrol.callback;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.accesscontrol.CheckRightCallback;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.ACL;
import com.zimbra.cs.mailbox.acl.FolderACL;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.util.Zimbra;

public class InviteCallback extends CheckRightCallback {
    
    //
    // allow the invite right if the authed user has admin folder right on the default calendar folder
    // (calendar folder in which the appointment is to be updated when the owner of the calendar receives 
    // an invite)
    //
    protected Boolean doCheckRight(Account authedAcct, Entry target, boolean asAdmin) throws ServiceException {
        // Don't do anything unless running inside the server
        if (!Zimbra.started())
            return null;
        
        if (!(target instanceof Account))
            return null;
        
        Account targetAcct = (Account)target;
        
        OperationContext octxt = new OperationContext(authedAcct, asAdmin);
        
        int folderId;
        boolean alternateDefaultFolderEnabled = targetAcct.getBooleanAttr(Provisioning.A_zimbraCalendarAlternateDefaultFolderEnabled, false);
        String defaultCalendar = null;
        if (alternateDefaultFolderEnabled)
            defaultCalendar = targetAcct.getAttr(Provisioning.A_zimbraPrefCalendarDefaultFolder);
        
        if (defaultCalendar == null)
            folderId = Mailbox.ID_FOLDER_CALENDAR;
        else {
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(targetAcct, false);
            if (mbox == null) {
                ZimbraLog.acl.warn("no mailbox for target account " + targetAcct.getName() +
                        ", checkRight callback for right [" + mRight.getName() +"] skipped");
                return null;
            }
            Folder folder = mbox.getFolderByPath(octxt, defaultCalendar);
            folderId = folder.getId();
        }
        
        FolderACL folderACL = new FolderACL(octxt, targetAcct, folderId, Boolean.FALSE);
        boolean hasAdminRight = folderACL.canAccess(ACL.RIGHT_ADMIN);
        
        if (hasAdminRight)
            return Boolean.TRUE;
        else
            return null;
    }
}
