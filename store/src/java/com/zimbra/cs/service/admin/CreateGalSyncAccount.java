/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.cs.account.accesscontrol.TargetType;
import com.zimbra.cs.ldap.LdapConstants;
import com.zimbra.cs.mailbox.ACL;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.admin.message.CreateGalSyncAccountRequest;
import com.zimbra.soap.admin.type.DataSourceType;
import com.zimbra.soap.admin.type.GalMode;
import com.zimbra.soap.type.AccountBy;
import com.zimbra.soap.type.AccountSelector;

public class CreateGalSyncAccount extends AdminDocumentHandler {

    @Override
    public boolean domainAuthSufficient(Map<String, Object> context) {
        return true;
    }

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {

        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();

        CreateGalSyncAccountRequest cgaRequest = zsc.elementToJaxb(request);

        String name = cgaRequest.getName();
        String domainStr = cgaRequest.getDomain();
        GalMode type = cgaRequest.getType();

        AccountSelector acctSelector = cgaRequest.getAccount();
        AccountBy acctBy = acctSelector.getBy();
        String acctValue = acctSelector.getKey();

        String password = cgaRequest.getPassword();
        String folder = cgaRequest.getFolder();

        String mailHost = cgaRequest.getMailHost();

        Domain domain = prov.getDomainByName(domainStr);

        if (domain == null) {
            throw AccountServiceException.NO_SUCH_DOMAIN(domainStr);
        }

        Account account = null;
        try {
            account = prov.get(acctBy.toKeyAccountBy(), acctValue, zsc.getAuthToken());
        } catch (ServiceException se) {
            ZimbraLog.gal.warn("error checking GalSyncAccount", se);
        }

        // create the system account if not already exists.
        if (account == null) {
            if (acctBy != AccountBy.name) {
                throw AccountServiceException.NO_SUCH_ACCOUNT(acctValue);
            }
            // there should be one gal sync account per domain per mailhost
            for (String acctId : domain.getGalAccountId()) {
                Account acct = prov.getAccountById(acctId);
                if ((acct != null) && (acct.getMailHost().equals(mailHost))) {
                    throw AccountServiceException.ACCOUNT_EXISTS(acct.getName());
                }
            }
            // XXX revisit
            checkDomainRightByEmail(zsc, acctValue, Admin.R_createAccount);
            Map<String,Object> accountAttrs = new HashMap<String,Object>();
            StringUtil.addToMultiMap(accountAttrs, Provisioning.A_zimbraIsSystemResource, LdapConstants.LDAP_TRUE);
            StringUtil.addToMultiMap(accountAttrs, Provisioning.A_zimbraIsSystemAccount, LdapConstants.LDAP_TRUE);
            StringUtil.addToMultiMap(accountAttrs, Provisioning.A_zimbraHideInGal, LdapConstants.LDAP_TRUE);
            StringUtil.addToMultiMap(accountAttrs, Provisioning.A_zimbraContactMaxNumEntries, "0");
            StringUtil.addToMultiMap(accountAttrs, Provisioning.A_zimbraMailHost, mailHost);
            checkSetAttrsOnCreate(zsc, TargetType.account, acctValue, accountAttrs);
            account = prov.createAccount(acctValue, password, accountAttrs);
        }

        if (!Provisioning.onLocalServer(account)) {
            String acctId = account.getId();
            String affinityIp = Provisioning.affinityServerForZimbraId(acctId);
            ZimbraSoapContext pxyCtxt = new ZimbraSoapContext(zsc);         
            return proxyRequest(request, context, affinityIp, pxyCtxt);
        }
        addDataSource(request, zsc, account, domain, folder, name, type);

        Element response = zsc.createElement(AdminConstants.CREATE_GAL_SYNC_ACCOUNT_RESPONSE);
        ToXML.encodeAccount(response, account, false, emptySet, null);
        return response;
    }

    static void addDataSource(Element request, ZimbraSoapContext zsc, Account account,
            Domain domain, String folder, String name, GalMode type)  throws ServiceException {
        String acctName = account.getName();
        String acctId = account.getId();
        HashSet<String> galAcctIds = new HashSet<String>();
        galAcctIds.addAll(Arrays.asList(domain.getGalAccountId()));
        if (!galAcctIds.contains(acctId)) {
            galAcctIds.add(acctId);
            domain.setGalAccountId(galAcctIds.toArray(new String[0]));
        }

        // create folder if not already exists.
        if (folder == null) {
            folder = "/Contacts";
        } else if (folder.length() > 0 && folder.charAt(0) != '/') {
            folder = "/" + folder;
        }
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
        Folder contactFolder = null;
        try {
            contactFolder = mbox.getFolderByPath(null, folder);
        } catch (MailServiceException.NoSuchItemException e) {
            contactFolder = mbox.createFolder(null, folder, new Folder.FolderOptions().setDefaultView(MailItem.Type.CONTACT));
        }

        int folderId = contactFolder.getId();

        // check if there is another datasource already that maps to the same contact folder.
        for (DataSource ds : account.getAllDataSources()) {
            if (ds.getFolderId() == folderId) {
                throw MailServiceException.ALREADY_EXISTS("data source " + ds.getName() + " already contains folder " + folder);
            }
        }


        mbox.grantAccess(null, folderId, domain.getId(), ACL.GRANTEE_DOMAIN, ACL.stringToRights("r"), null);

        // create datasource
        Map<String,Object> attrs = AdminService.getAttrs(request, true);
        try {
            attrs.put(Provisioning.A_zimbraGalType, type.name());
            attrs.put(Provisioning.A_zimbraDataSourceFolderId, "" + folderId);
            if (!attrs.containsKey(Provisioning.A_zimbraDataSourceEnabled)) {
                attrs.put(Provisioning.A_zimbraDataSourceEnabled, LdapConstants.LDAP_TRUE);
            }
            if (!attrs.containsKey(Provisioning.A_zimbraGalStatus)) {
                attrs.put(Provisioning.A_zimbraGalStatus, "enabled");
            }
            Provisioning.getInstance().createDataSource(account, DataSourceType.gal, name, attrs);
        } catch (ServiceException e) {
            ZimbraLog.gal.error("error creating datasource for GalSyncAccount", e);
            throw e;
        }

        ZimbraLog.security.info(ZimbraLog.encodeAttrs(new String[] {"cmd", "CreateGalSyncAccount", "name", acctName} ));
    }

    private static final Set<String> emptySet = Collections.emptySet();

    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        // XXX revisit
        relatedRights.add(Admin.R_createAccount);
        notes.add(String.format(AdminRightCheckPoint.Notes.MODIFY_ENTRY,
                Admin.R_modifyAccount.getName(), "account"));
    }
}
