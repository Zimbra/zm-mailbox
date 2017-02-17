/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2015, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.dav.resource;

import java.util.Set;

import org.dom4j.Element;

import com.google.common.collect.Sets;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.dav.DavContext;
import com.zimbra.cs.dav.DavElements;
import com.zimbra.cs.dav.property.ResourceProperty;
import com.zimbra.cs.mailbox.ACL;
import com.zimbra.cs.mailbox.ACL.Grant;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;

public class ProxyGroupMemberSet extends ResourceProperty {

    private final Account account;
    private final boolean readOnly;

    public ProxyGroupMemberSet(Account account, boolean readOnly) {
        super(DavElements.E_GROUP_MEMBER_SET);
        this.account = account;
        this.readOnly = readOnly;
        setProtected(false);
    }

    @Override
    public Element toElement(DavContext ctxt, Element parent, boolean nameOnly) {
        Element group = super.toElement(ctxt, parent, nameOnly);
        if (nameOnly) {
            return group;
        }
        for (Account user : getUsersWithProxyAccessToCalendar(ctxt, account, readOnly)) {
            group.addElement(DavElements.E_HREF).setText(UrlNamespace.getPrincipalUrl(account, user));
        }
        return group;
    }

    /**
     * @param readOnly - if true, return accounts which have "read" right but not "write" access.<br />
     * if false, return accounts that have at least "write" access.
     */
    public static Set<Account> getUsersWithProxyAccessToCalendar(DavContext ctxt, Account acct, boolean readOnly) {
        Set<Account> accts = Sets.newHashSet();
        try {
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(acct);
            Folder f = mbox.getFolderById(ctxt.getOperationContext(), Mailbox.ID_FOLDER_CALENDAR);
            ACL acl = f.getEffectiveACL();
            if (acl == null) {
                return accts;
            }
            for (Grant g : acl.getGrants()) {
                if (g.getGranteeType() != ACL.GRANTEE_USER) {
                    continue;
                }
                boolean match = readOnly ?
                    (g.getGrantedRights() & ACL.RIGHT_READ) != 0 && (g.getGrantedRights() & ACL.RIGHT_WRITE) == 0 :
                    (g.getGrantedRights() & ACL.RIGHT_WRITE) != 0;
                if (match) {
                    Account user = Provisioning.getInstance().get(AccountBy.id, g.getGranteeId());
                    if (user != null) {
                        accts.add(user);
                    }
                }
            }
        } catch (ServiceException se) {
            ZimbraLog.dav.warn("can't get mailbox", se);
        }
        return accts;
    }
}
