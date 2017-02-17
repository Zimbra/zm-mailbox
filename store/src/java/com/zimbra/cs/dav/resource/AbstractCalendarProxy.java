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

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.dom4j.Element;
import org.dom4j.QName;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.zimbra.client.ZMailbox;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.dav.DavContext;
import com.zimbra.cs.dav.DavElements;
import com.zimbra.cs.dav.DavException;
import com.zimbra.cs.dav.property.Acl;
import com.zimbra.cs.mailbox.ACL;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.soap.mail.message.SendShareNotificationRequest;
import com.zimbra.soap.mail.type.EmailAddrInfo;

public abstract class AbstractCalendarProxy extends Principal {
    protected AbstractCalendarProxy(Account acct, String url, QName resourceType, boolean readOnly)
    throws ServiceException {
        super(acct, url);
        initProps(resourceType, readOnly);
    }
    protected AbstractCalendarProxy(String user, String url, QName resourceType, boolean readOnly)
    throws ServiceException {
        super(user, url);
        initProps(resourceType, readOnly);
    }

    private void initProps(QName resourceType, boolean readOnly) {
        addResourceType(resourceType);
        addProperty(Acl.getPrincipalUrl(this));
        addProperty(new ProxyGroupMemberSet(getAccount(), readOnly));
    }

    /**
     * Only support setting of which accounts are in the group-member-set - defined as those which
     * have the appropriate perms on the default Calendar folder
     * Need to grant access to new members of the set (with sending of invites) and revoke access to deleted
     * members as appropriate.
     *
     * Note that un-checking a box next to a name in the Yosemite Calendar UI would result in 2 PROPPATCHes,
     * one to add an entry to the read group and one to remove that entry from the write group (or vice versa)
     *
     * Note that there is a mismatch between the Apple model and ours in that in the Zimbra model we send out a
     * share notification email, and the recipient actively accepts the share - something the Apple model
     * doesn't require.
     */
    @Override
    public void patchProperties(DavContext ctxt, Collection<Element> set, Collection<QName> remove)
    throws DavException, IOException {
        boolean readOnlyProxy = !(this instanceof CalendarProxyWrite);
        // Zimbra supports more fine grained permissions than this.  ROLE_VIEW is a good match but
        // which role is appropriate for read-write proxy is probably either ROLE_MANAGER or ROLE_ADMIN
        // Flipped the coin and chosen ROLE_ADMIN here.
        short role = readOnlyProxy ? ACL.ROLE_VIEW : ACL.ROLE_ADMIN;
        Set<Account> origGroupMembers =
                ProxyGroupMemberSet.getUsersWithProxyAccessToCalendar(ctxt, getAccount(), readOnlyProxy);
        Set<Account> invitees = Sets.newHashSet();
        Set<Account> newGroupMembers = Sets.newHashSet();
        boolean settingNewGroupMembers = false;
        for (Element setElem : set) {
            if (setElem.getQName().equals(DavElements.E_GROUP_MEMBER_SET)) {
                settingNewGroupMembers = true;
                Iterator hrefs = setElem.elementIterator(DavElements.E_HREF);
                while (hrefs.hasNext()) {
                    Account target = hrefToAccount(ctxt, (Element) hrefs.next());
                    if (target != null) {
                        newGroupMembers.add(target);
                        if ((!origGroupMembers.contains(target))) {
                            invitees.add(target);
                        }
                    }
                }
            }
        }
        if (!settingNewGroupMembers) {
            return;
        }
        Set<Account> revokees = Sets.newHashSet();
        for (Account origMember : origGroupMembers) {
            if (!newGroupMembers.contains(origMember)) {
                revokees.add(origMember);
            }
        }
        changeGroupMembership(ctxt, invitees, revokees, role);
    }

    /**
     * Reflect changes in membership, i.e. invite new members and revoke access to people no longer in the group.
     * Note that we are only dealing with the default Calendar - the Apple model doesn't talk about things at
     * the calendar level - so for our purposes we shall assume it only applies to that calendar.
     */
    private void changeGroupMembership(DavContext ctxt, Set<Account> invitees, Set<Account> revokees, short role) {
        try {
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(getAccount());
            ZMailbox zmbox = ctxt.getZMailbox(getAccount());

            List<EmailAddrInfo> emailAddresses = Lists.newArrayList();
            for (Account target : invitees) {
                mbox.grantAccess(ctxt.getOperationContext(), Mailbox.ID_FOLDER_CALENDAR,
                        target.getId(), ACL.GRANTEE_USER, role, null);
                emailAddresses.add(new EmailAddrInfo(target.getName()));
            }
            SendShareNotificationRequest ssnreq;
            if (!emailAddresses.isEmpty()) {
                ssnreq = SendShareNotificationRequest.create(Mailbox.ID_FOLDER_CALENDAR,
                        null /* SendShareNotificationRequest.Action */, null /* notes */, emailAddresses);
                zmbox.invokeJaxb(ssnreq);
            }

            emailAddresses = Lists.newArrayList();
            for (Account target : revokees) {
                mbox.revokeAccess(ctxt.getOperationContext(), Mailbox.ID_FOLDER_CALENDAR, target.getId());
                emailAddresses.add(new EmailAddrInfo(target.getName()));
            }
            if (!emailAddresses.isEmpty()) {
                ssnreq = SendShareNotificationRequest.create(Mailbox.ID_FOLDER_CALENDAR,
                        SendShareNotificationRequest.Action.revoke, null /* notes */, emailAddresses);
                zmbox.invokeJaxb(ssnreq);
            }
        } catch (ServiceException se) {
            ZimbraLog.dav.warn("can't modify acl on %s for %s", getAccount().getName(), ctxt.getPath());
        }
    }

    private Account hrefToAccount(DavContext ctxt, Element href) {
        String principalPath = href.getText();
        DavResource principal = null;
        try {
            principal = UrlNamespace.getPrincipalAtUrl(ctxt, principalPath);
        } catch (DavException e) {
            ZimbraLog.dav.warn("can't find principal at %s", principalPath);
            return null;
        }
        if (!(principal instanceof User)) {
            ZimbraLog.dav.warn("not a user principal path %s", principalPath);
            return null;
        }
        return ((User)principal).getAccount();
    }

    @Override
    public boolean isCollection() {
        return true;
    }
}
