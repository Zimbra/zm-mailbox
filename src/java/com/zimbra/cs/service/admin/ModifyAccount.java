/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
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
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.common.account.Key;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.account.Key.CacheEntryBy;
import com.zimbra.cs.account.Provisioning.CacheEntry;
import com.zimbra.cs.account.Provisioning.CacheEntryType;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.cs.account.soap.SoapProvisioning;
import com.zimbra.cs.httpclient.URLUtil;
import com.zimbra.cs.session.AdminSession;
import com.zimbra.cs.session.Session;
import com.zimbra.common.soap.Element;
import com.zimbra.soap.ZimbraSoapContext;

import java.util.List;
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

        checkAccountRight(zsc, account, attrs);

        // check to see if quota is being changed
        long curQuota = account.getLongAttr(Provisioning.A_zimbraMailQuota, 0);
        
        /*
        // Note: isDomainAdminOnly *always* returns false for pure ACL based AccessManager 
        // checkQuota is called only for domain based access manager, remove when we
        // can totally deprecate domain based access manager 
        if (isDomainAdminOnly(zsc))
            checkQuota(zsc, account, attrs);
        */
        
        /*
         * for bug 42896, the above is no longer true.
         * 
         * For quota, we have to support the per admin limitation zimbraDomainAdminMaxMailQuota, 
         * until we come up with a framework to support constraints on a per admin basis.
         * 
         * for now, always call checkQuota, which will check zimbraDomainAdminMaxMailQuota.
         * 
         * If the access manager, and if we have come here, it has already passed the constraint
         * checking, in the checkAccountRight call.   If it had violated any constraint, it would 
         * have errored out.  i.e. for zimbraMailQuota, both zimbraConstraint and zimbraDomainAdminMaxMailQuota 
         * are enforced.
         */
        checkQuota(zsc, account, attrs);
        
        // check to see if cos is being changed, need right on new cos 
        checkCos(zsc, account, attrs);

        // pass in true to checkImmutable
        prov.modifyAttrs(account, attrs, true);
        
        // get account again, in the case when zimbraCOSId or zimbraForeignPrincipal
        // is changed, the cache object(he one we are holding on to) would'd been 
        // flushed out from cache.  Get the account again to get the fresh one.
        account = prov.get(AccountBy.id, id, zsc.getAuthToken());

        ZimbraLog.security.info(ZimbraLog.encodeAttrs(
                new String[] {"cmd", "ModifyAccount","name", account.getName()}, attrs));
        
        checkNewServer(zsc, context, account);
        
        long newQuota = account.getLongAttr(Provisioning.A_zimbraMailQuota, 0);
        if (newQuota != curQuota) {
            // clear the quota cache
            AdminSession session = (AdminSession) getSession(zsc, Session.Type.ADMIN);
            if (session != null)
                GetQuotaUsage.clearCachedQuotaUsage(session);
        }
            

        Element response = zsc.createElement(AdminConstants.MODIFY_ACCOUNT_RESPONSE);
        ToXML.encodeAccount(response, account);
	    return response;
	}
	
    static String getStringAttrNewValue(String attrName, Map<String, Object> attrs) throws ServiceException {
	    Object object = attrs.get(attrName);
        if (object == null) object = attrs.get("+" + attrName);
        if (object == null) object = attrs.get("-" + attrName);
        if (object == null) return null;

        if (!(object instanceof String))
            throw ServiceException.PERM_DENIED("can not modify " +  attrName + "(single valued attribute)");

        String attrNewValue = (String)object;
        return attrNewValue;
	}

    private void checkQuota(ZimbraSoapContext zsc, Account account, Map<String, Object> attrs) throws ServiceException {
        String quotaAttr = getStringAttrNewValue(Provisioning.A_zimbraMailQuota, attrs);
        if (quotaAttr == null)
            return;  // not changing it

        long quota;

        if (quotaAttr.equals("")) {
            // they are unsetting it, so check the COS
            quota = Provisioning.getInstance().getCOS(account).getIntAttr(Provisioning.A_zimbraMailQuota, 0);
        } else {
            try {
                quota = Long.parseLong(quotaAttr);
            } catch (NumberFormatException e) {
                throw AccountServiceException.INVALID_ATTR_VALUE("can not modify mail quota (invalid format): "+quotaAttr, e);
            }
        }
        
        if (!canModifyMailQuota(zsc,  account, quota))
            throw ServiceException.PERM_DENIED("can not modify mail quota");
    }
    
    private void checkCos(ZimbraSoapContext zsc, Account account, Map<String, Object> attrs) throws ServiceException {
        String newCosId = getStringAttrNewValue(Provisioning.A_zimbraCOSId, attrs);
        if (newCosId == null)
            return;  // not changing it
        
        Provisioning prov = Provisioning.getInstance();
        if (newCosId.equals("")) {
            // they are unsetting it, so check the domain
            newCosId = prov.getDomain(account).getAttr(Provisioning.A_zimbraDomainDefaultCOSId);
            if (newCosId == null)
                return;  // no domain cos, use the default COS, which is available to all
        } 

        Cos cos = prov.get(Key.CosBy.id, newCosId);
        if (cos == null) {
            throw AccountServiceException.NO_SUCH_COS(newCosId);
        }
        
        // call checkRight instead of checkCosRight, because:
        // 1. no domain based access manager backward compatibility issue
        // 2. we only want to check right if we are using pure ACL based access manager. 
        checkRight(zsc, cos, Admin.R_assignCos);
    }
    
    /*
     * if the account's home server is changed as a result of this command and the 
     * new server is no longer this server, need to send a flush cache command to the 
     * new server so we don't get into the following:
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
            if (!Provisioning.onLocalServer(acct)) {
                newServer = Provisioning.getInstance().getServer(acct);
                
                // in the case when zimbraMailHost is being removed, newServer will be null
                if (newServer != null) {
                    /*
                    Element request = zsc.createRequestElement(AdminConstants.FLUSH_CACHE_REQUEST);
                    Element eCache = request.addElement(AdminConstants.E_CACHE).addAttribute(AdminConstants.A_TYPE, Provisioning.CacheEntryType.account.name());
                    eCache.addElement(AdminConstants.E_ENTRY).addAttribute(AdminConstants.A_BY, Key.CacheEntryBy.id.name()).addText(acct.getId());
    
                    Element response = proxyRequest(request, context, newServer);
                    */
                    SoapProvisioning soapProv = new SoapProvisioning();
                    String adminUrl = URLUtil.getAdminURL(newServer, AdminConstants.ADMIN_SERVICE_URI, true);
                    soapProv.soapSetURI(adminUrl);
                    soapProv.soapZimbraAdminAuthenticate();
                    soapProv.flushCache(CacheEntryType.account, 
                            new CacheEntry[]{new CacheEntry(CacheEntryBy.id, acct.getId())});
                }
            }
        } catch (ServiceException e) {
            // ignore any error and continue
            ZimbraLog.mailbox.warn("cannot flush account cache on server " + (newServer==null?"":newServer.getName()) + " for " + acct.getName(), e);
        }
    }
    
    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        relatedRights.add(Admin.R_assignCos);
        
        notes.add(String.format(AdminRightCheckPoint.Notes.MODIFY_ENTRY, 
                Admin.R_modifyAccount.getName(), "account") + "\n");
        
        notes.add("Notes on " + Provisioning.A_zimbraCOSId + ": " +
                "If setting " + Provisioning.A_zimbraCOSId + ", needs the " + Admin.R_assignCos.getName() + 
                " right on the cos." + 
                "If removing " + Provisioning.A_zimbraCOSId + ", needs the " + Admin.R_assignCos.getName() + 
                " right on the domain default cos. (in domain attribute " + Provisioning.A_zimbraDomainDefaultCOSId +").");
    }
}
