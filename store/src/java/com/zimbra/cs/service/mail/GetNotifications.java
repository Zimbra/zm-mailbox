/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2020 Synacor, Inc.
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
package com.zimbra.cs.service.mail;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.MailboxOperation;
import com.zimbra.cs.mailbox.event.ItemEventLog;
import com.zimbra.cs.mailbox.event.MailboxEvent;
import com.zimbra.cs.mailbox.event.NewNotifications;
import com.zimbra.cs.service.util.ItemIdFormatter;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.mail.message.GetNotificationsRequest;
import com.zimbra.soap.mail.message.GetNotificationsResponse;
import com.zimbra.soap.mail.type.IdEmailName;
import com.zimbra.soap.type.NamedElement;

/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2020 Synacor, Inc.
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
public class GetNotifications extends MailDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context)
            throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);

        GetNotificationsRequest req = JaxbUtil.elementToJaxb(request);

        Account account = getRequestedAccount(zsc);
        Account authAccount = getAuthenticatedAccount(zsc);
        NewNotifications notif = new NewNotifications(authAccount, account);
        ItemEventLog log = notif.getLog();

        GetNotificationsResponse resp = new GetNotificationsResponse();
        resp.setlastSeen(notif.getLastSeen() * 1000);  // return millis in order to be consistent with the rest of ts in activities
        for (MailboxOperation op : log.getLoggedOps()) {
            resp.addOperation(new NamedElement(op.name()));
        }
        ItemIdFormatter fmt = new ItemIdFormatter(zsc);
        Provisioning prov = Provisioning.getInstance();
        for (String user : log.getLoggedUsers()) {
            IdEmailName userInfo = IdEmailName.fromId(user);
            resp.addUser(userInfo);
            Account a = prov.getAccountById(user);
            if (a != null) {
                userInfo.setEmail(a.getName());
                userInfo.setName(a.getDisplayName());
            }
        }
        writeEntries(resp, log, fmt);
        if (req.isMarkSeen()) {
            notif.markSeen();
        }

        return zsc.jaxbToElement(resp);
    }

    private void writeEntries(GetNotificationsResponse resp, ItemEventLog log, ItemIdFormatter fmt)
    throws ServiceException {
        Provisioning prov = Provisioning.getInstance();
        for (MailboxEvent ev : log.getEvents(0, 100)) {
            Account acct = null;
            try {
                acct = prov.getAccountById(ev.getAccountId());
            } catch (ServiceException e) {
                
            }
            resp.addActivity(ToXML.toActivityInfo(acct, ev.getOperation(),
                    ev.getTimestamp(), fmt.formatItemId(ev.getItemId()), ev.getVersion(),
                    ev.getUserAgent(), ev.getArgs()));
        }
    }
}
