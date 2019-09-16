/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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

/*
 * Created on Apr 2, 2005
 */
package com.zimbra.cs.service.admin;

import java.util.List;
import java.util.Map;

import com.zimbra.client.ZMailbox;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.common.account.Key;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.auth.ZAuthToken;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.cs.account.soap.SoapProvisioning;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.service.AuthProvider;
import com.zimbra.cs.util.AccountUtil;
import com.zimbra.common.soap.Element;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.admin.message.PurgeMessagesRequest;
import com.zimbra.soap.admin.message.PurgeMessagesResponse;
import com.zimbra.soap.admin.type.MailboxWithMailboxId;
import com.zimbra.soap.admin.type.PurgeMessagesStatus;

/**
 * @author dkarp
 */
public class PurgeMessages extends AdminDocumentHandler {

    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);

        Element mreq = request.getOptionalElement(AdminConstants.E_MAILBOX);
        String[] accounts;
        if (mreq != null) {
            accounts = new String[] { mreq.getAttribute(AdminConstants.A_ACCOUNTID) };

            // accounts are specified, check right or each account
            Provisioning prov = Provisioning.getInstance();
            for (String acctId : accounts) {
                Account acct = prov.get(AccountBy.id, acctId);
                if (acct == null) {
                    throw AccountServiceException.NO_SUCH_ACCOUNT(acctId);
                }
                checkAccountRight(zsc, acct, Admin.R_purgeMessages);
            }

        } else {
            // all accounts on the system, has to be a system admin
            checkRight(zsc, context, null, AdminRight.PR_SYSTEM_ADMIN_ONLY);

            accounts = MailboxManager.getInstance().getAccountIds();
        }

        PurgeMessagesResponse purgeResponse = new PurgeMessagesResponse();
        for (int i = 0; i < accounts.length; i++) {
            Account account = Provisioning.getInstance().getAccountById(accounts[i]);
            if (account == null) {
                continue;
            }
            PurgeMessagesStatus mboxResp;
            if (Provisioning.onLocalServer(account)) { // local
                Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account, false);
                if (mbox == null) {
                    continue;
                }
                boolean purgedAll = mbox.purgeMessages(null);
                if (account.isFeatureSearchHistoryEnabled()) {
                    mbox.purgeSearchHistory(null);
                }
                mboxResp = new PurgeMessagesStatus(mbox.getId(), account.getId(), Long.valueOf(mbox.getSize()), purgedAll);
            } else { // remote
                Server server = account.getServer();
                if (server == null) {
                    continue;
                }
                SoapProvisioning soapProvisioning = SoapProvisioning.getAdminInstance();
                mboxResp = soapProvisioning.purgeMessages(account);
                if (mboxResp == null) {
                    continue;
                }
                mboxResp.setAccountId(account.getId());
            }
            purgeResponse.addMailbox(mboxResp);
        }
        return zsc.jaxbToElement(purgeResponse);
    }

    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        relatedRights.add(Admin.R_purgeMessages);
        notes.add("If account ids are specified, needs effective " +
                Admin.R_purgeMessages.getName() + " right for each account.  " +
                "If account ids are not specified, the authed account has to be a system admin.");
    }
}
