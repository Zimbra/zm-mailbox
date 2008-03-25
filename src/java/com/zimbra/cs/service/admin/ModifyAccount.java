/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Jun 17, 2004
 */
package com.zimbra.cs.service.admin;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.AttributeManager;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.service.account.ToXML;
import com.zimbra.common.soap.Element;
import com.zimbra.soap.ZimbraSoapContext;

import java.util.Map;

/**
 * @author schemers
 */
public class ModifyAccount extends AdminDocumentHandler {

    private static final String[] TARGET_ACCOUNT_PATH = new String[] { AdminConstants.E_ID };
    protected String[] getProxiedAccountPath()  { return TARGET_ACCOUNT_PATH; }

    /**
     * must be careful and only allow modifies to accounts/attrs domain admin has access to
     */
    public boolean domainAuthSufficient(Map context) {
        return true;
    }

	public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
	    Provisioning prov = Provisioning.getInstance();

	    String id = request.getAttribute(AdminConstants.E_ID);
	    Map<String, Object> attrs = AdminService.getAttrs(request);

	    Account account = prov.get(AccountBy.id, id, zsc.getAuthToken());
        if (account == null)
            throw AccountServiceException.NO_SUCH_ACCOUNT(id);

        if (!canAccessAccount(zsc, account))
            throw ServiceException.PERM_DENIED("can not access account");

        if (isDomainAdminOnly(zsc)) {
            for (String attrName : attrs.keySet()) {
                if (attrName.charAt(0) == '+' || attrName.charAt(0) == '-')
                    attrName = attrName.substring(1);

                if (!AttributeManager.getInstance().isDomainAdminModifiable(attrName))
                    throw ServiceException.PERM_DENIED("can not modify attr: "+attrName);
            }
        }

        // check to see if quota is being changed
        checkQuota(zsc, account, attrs);


        // pass in true to checkImmutable
        prov.modifyAttrs(account, attrs, true);

        ZimbraLog.security.info(ZimbraLog.encodeAttrs(
                new String[] {"cmd", "ModifyAccount","name", account.getName()}, attrs));
        
        checkNewServer(zsc, context, account);

        Element response = zsc.createElement(AdminConstants.MODIFY_ACCOUNT_RESPONSE);
        ToXML.encodeAccountOld(response, account);
	    return response;
	}

    private void checkQuota(ZimbraSoapContext zsc, Account account, Map<String, Object> attrs) throws ServiceException {
        Object object = attrs.get(Provisioning.A_zimbraMailQuota);
        if (object == null) object = attrs.get("+" + Provisioning.A_zimbraMailQuota);
        if (object == null) object = attrs.get("-" + Provisioning.A_zimbraMailQuota);
        if (object == null) return;

        if (!(object instanceof String))
            throw ServiceException.PERM_DENIED("can not modify mail quota (single valued attribute)");

        String quotaAttr = (String) object;

        long quota;

        if (quotaAttr.equals("")) {
            // they are unsetting it, so check the COS
            quota = Provisioning.getInstance().getCOS(account).getIntAttr(Provisioning.A_zimbraMailQuota, 0);
        } else {
            try {
                quota = Long.parseLong(quotaAttr);
            } catch (NumberFormatException e) {
                throw AccountServiceException.INVALID_ATTR_VALUE("can not modify mail quota (invalid format): "+object, e);
            }
        }
        
        if (!canModifyMailQuota(zsc,  account, quota))
            throw ServiceException.PERM_DENIED("can not modify mail quota");
    }
    
    /*
     * if the account's home server is changed as a result of this command and the new server is no longer
     * this server, need to send a flush cache command to the new server so we don't get into the following:
     * 
     * account is on server A (this server)
     * 
     * on server B: 
     *     zmprov ma {account} zimbraMailHost B
     *     (the ma is proxied to server A;
     *      and on server B, the account still appears to be on A)
     *               
     *     zmprov ma {account} {any attr} {value}
     *     ERROR: service.TOO_MANY_HOPS
     *     Until the account is expired from cache on server B.          
     */
    private void checkNewServer(ZimbraSoapContext zsc, Map<String, Object> context, Account acct) {
        Server newServer = null;
        try {
            if (!Provisioning.getInstance().onLocalServer(acct)) {
                newServer = Provisioning.getInstance().getServer(acct);
                Element request = zsc.createRequestElement(AdminConstants.FLUSH_CACHE_REQUEST);
                Element eCache = request.addElement(AdminConstants.E_CACHE).addAttribute(AdminConstants.A_TYPE, Provisioning.CacheEntryType.account.name());
                eCache.addElement(AdminConstants.E_ENTRY).addAttribute(AdminConstants.A_BY, Provisioning.CacheEntryBy.id.name()).addText(acct.getId());

                Element response = proxyRequest(request, context, newServer, zsc);
            }
        } catch (ServiceException e) {
            // ignore any error and continue
            ZimbraLog.mailbox.warn("cannot flush account cache on server " + (newServer==null?"":newServer.getName()) + " for " + acct.getName(), e);
        }
    }

}
