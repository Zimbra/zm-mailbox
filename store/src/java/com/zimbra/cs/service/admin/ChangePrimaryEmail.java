/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2018 Synacor, Inc.
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
package com.zimbra.cs.service.admin;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.zimbra.common.account.Key;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.localconfig.DebugConfig;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.CacheEntry;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.admin.message.ChangePrimaryEmailRequest;
import com.zimbra.soap.admin.type.CacheEntryType;
import com.zimbra.soap.type.AccountSelector;

public class ChangePrimaryEmail extends AdminDocumentHandler {

    private static final String[] TARGET_ACCOUNT_PATH = new String[] { AdminConstants.E_ACCOUNT };

    @Override
    protected String[] getProxiedAccountElementPath() {
        return TARGET_ACCOUNT_PATH;
    }

    /**
     * must be careful and only allow renames from/to domains a domain admin can
     * see
     */
    @Override
    public boolean domainAuthSufficient(Map<String, Object> context) {
        return true;
    }

    /**
     * @return true - which means accept responsibility for measures to prevent
     *         account harvesting by delegate admins
     */
    @Override
    public boolean defendsAgainstDelegateAdminAccountHarvesting() {
        return true;
    }

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {

        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();

        ChangePrimaryEmailRequest req = JaxbUtil.elementToJaxb(request);
        String newName = req.getNewName();
        if (StringUtils.isEmpty(newName)) {
            throw ServiceException.INVALID_REQUEST(String.format("missing <%s>", AdminConstants.E_NEW_NAME), null);
        }
        AccountSelector acctSel = req.getAccount();
        if (acctSel == null) {
            throw ServiceException.INVALID_REQUEST(String.format("missing <%s>", AdminConstants.E_ACCOUNT), null);
        }

        String accountSelectorKey = acctSel.getKey();
        AccountBy by = acctSel.getBy().toKeyAccountBy();
        Account account = prov.get(by, accountSelectorKey, zsc.getAuthToken());

        defendAgainstAccountHarvesting(account, by, accountSelectorKey, zsc, Admin.R_renameAccount);
        defendAgainstAccountOrCalendarResourceHarvesting(account, by, accountSelectorKey, zsc, Admin.R_addAccountAlias,
                Admin.R_addCalendarResourceAlias);

        String oldName = account.getName();
        String accountId = account.getId();

        checkAccountRight(zsc, account, Admin.R_renameAccount);
        checkDomainRightByEmail(zsc, newName, Admin.R_createAccount);
        checkDomainRightByEmail(zsc, oldName, Admin.R_createAlias);

        account.setOldMailAddress(oldName);

        try {
            ZimbraLog.security.debug("old mail address set to %s for account %s", oldName, accountId);
            Mailbox mbox = Provisioning.onLocalServer(account) ? MailboxManager.getInstance().getMailboxByAccount(account) : null;
            if (mbox == null) {
                throw ServiceException.FAILURE("unable to get mailbox for rename: " + accountId, null);
            }
            prov.renameAccount(accountId, newName);
            mbox.renameMailbox(oldName, newName);
            ZimbraLog.security.info(ZimbraLog
                    .encodeAttrs(new String[] { "cmd", "ChangePrimaryEmail account renamed", "name", oldName, "newName", newName }));

            // get account again after rename
            account = prov.get(AccountBy.id, accountId, true, zsc.getAuthToken());
            if (account == null) {
                throw ServiceException.FAILURE("unable to get account after rename: " + accountId, null);
            }

            int sleepTime = DebugConfig.sleepTimeForTestingChangePrimaryEmail;
            try {
                ZimbraLog.security.debug("ChangePrimaryEmail thread sleeping for %d milliseconds", sleepTime);
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                ZimbraLog.security.debug("ChangePrimaryEmail sleep was interrupted", e);
            }

            prov.addAlias(account, oldName);
            ZimbraLog.security.info(ZimbraLog.encodeAttrs(new String[] { "cmd", "ChangePrimaryEmail added alias",
                    "name", account.getName(), "alias", oldName }));

            Date renameTime = new Date();
            account.addPrimaryEmailChangeHistory(String.format("%s|%d", oldName, renameTime.getTime()));
        } finally {
            if (account != null) {
                prov.flushCache(CacheEntryType.account, new CacheEntry[]{new CacheEntry(Key.CacheEntryBy.id, account.getId())});
                account.unsetOldMailAddress();
            }
        }

        Element response = zsc.createElement(AdminConstants.CHANGE_PRIMARY_EMAIL_RESPONSE);
        ToXML.encodeAccount(response, account);
        return response;
    }

    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        relatedRights.add(Admin.R_renameAccount);
        relatedRights.add(Admin.R_createAccount);
        relatedRights.add(Admin.R_createAlias);
        relatedRights.add(Admin.R_addCalendarResourceAlias);
        relatedRights.add(Admin.R_addAccountAlias);
    }
}
