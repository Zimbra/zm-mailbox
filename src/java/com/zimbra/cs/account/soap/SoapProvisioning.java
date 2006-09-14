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

package com.zimbra.cs.account.soap;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.CalendarResource;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.EntrySearchFilter;
import com.zimbra.cs.account.GalContact;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.WellKnownTimeZone;
import com.zimbra.cs.account.Zimlet;
import com.zimbra.cs.account.NamedEntry.Visitor;
import com.zimbra.cs.httpclient.URLUtil;
import com.zimbra.cs.localconfig.LC;
import com.zimbra.cs.mailbox.calendar.ICalTimeZone;
import com.zimbra.cs.mime.MimeTypeInfo;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.account.AccountService;
import com.zimbra.cs.service.admin.AdminService;
import com.zimbra.cs.util.StringUtil;
import com.zimbra.cs.zclient.ZClientException;
import com.zimbra.soap.Element;
import com.zimbra.soap.SoapFaultException;
import com.zimbra.soap.SoapHttpTransport;
import com.zimbra.soap.Element.XMLElement;
import com.zimbra.soap.SoapTransport.DebugListener;

public class SoapProvisioning extends Provisioning {

    private SoapHttpTransport mTransport;
    private String mAuthToken;
    private long mAuthTokenLifetime;
    private long mAuthTokenExpiration;
    private DebugListener mDebugListener;
    
    public SoapProvisioning() {
        
    }

    /**
     * @param uri URI of server we want to talk to
     */
    public void soapSetURI(String uri) {
        if (mTransport != null) mTransport.shutdown();
        mTransport = new SoapHttpTransport(uri);
        if (mAuthToken != null)
            mTransport.setAuthToken(mAuthToken);
        if (mDebugListener != null)
            mTransport.setDebugListener(mDebugListener);
    }
    
    public void soapSetTransportDebugListener(DebugListener listener) {
        mDebugListener = listener;
        if (mTransport != null)
            mTransport.setDebugListener(mDebugListener);
    }

    public String soapGetURI() {
        return mTransport.getURI();
    }
    
    public String getAuthToken() {
        return mAuthToken;
    }
    
    public void setAuthToken(String authToken) {
        mAuthToken = authToken;
        if (mTransport != null)
            mTransport.setAuthToken(authToken);
    }

    /**
     * used to authenticate via admin AuthRequest. can only be called after setting the URI with setURI.
     * 
     * @param name
     * @param password
     * @throws ServiceException
     * @throws IOException 
     */
    public void soapAdminAuthenticate(String name, String password) throws ServiceException {
       if (mTransport == null) throw ZClientException.CLIENT_ERROR("must call setURI before calling adminAuthenticate", null);
       XMLElement req = new XMLElement(AdminService.AUTH_REQUEST);
       req.addElement(AdminService.E_NAME).setText(name);
       req.addElement(AdminService.E_PASSWORD).setText(password);
       Element response = invoke(req);
       mAuthToken = response.getElement(AdminService.E_AUTH_TOKEN).getText();
       mAuthTokenLifetime = response.getAttributeLong(AdminService.E_LIFETIME);
       mAuthTokenExpiration = System.currentTimeMillis() + mAuthTokenLifetime;
       mTransport.setAuthToken(mAuthToken);
    }

    /**
     * auth as zimbra admin (over SOAP) using password from localconfig. Can only be called after
     * setting the URI with setUI. 
     * 
     * @throws ServiceException
     * @throws IOException
     */
    public void soapZimbraAdminAuthenticate() throws ServiceException {
        soapAdminAuthenticate(LC.zimbra_ldap_user.value(), LC.zimbra_ldap_password.value());
    }

    private String serverName() {
        try {
            return new URI(mTransport.getURI()).getHost();
        } catch (URISyntaxException e) {
            return mTransport.getURI();
        }
    }
    
    synchronized Element invoke(Element request) throws ServiceException {
        try {
            return mTransport.invoke(request);
        } catch (SoapFaultException e) {
            throw e; // for now, later, try to map to more specific exception
        } catch (IOException e) {
            throw ZClientException.IO_ERROR("invoke "+e.getMessage()+", server: "+serverName(), e);
        }
    }

    synchronized Element invoke(Element request, String serverName) throws ServiceException {
        String oldUri = soapGetURI();
        String newUri = URLUtil.getAdminURL(serverName);
        boolean diff = !oldUri.equals(newUri);        
        try {
            if (diff) soapSetURI(newUri);
            return mTransport.invoke(request);
        } catch (SoapFaultException e) {
            throw e; // for now, later, try to map to more specific exception
        } catch (IOException e) {
            throw ZClientException.IO_ERROR("invoke "+e.getMessage()+", server: "+serverName, e);
        } finally {
            if (diff) soapSetURI(oldUri);
        }
    }

    static Map<String, Object> getAttrs(Element e) throws ServiceException {
        Map<String, Object> result = new HashMap<String,Object>();
        for (Element a : e.listElements(AdminService.E_A)) {
            StringUtil.addToMultiMap(result, a.getAttribute(AdminService.A_N), a.getText());
        }
        return result;
    }

    static void addAttrElements(Element req, Map<String, ? extends Object> attrs) throws ServiceException {
        if (attrs == null) return;
        
        for (Entry entry : attrs.entrySet()) {
            String key = (String) entry.getKey();
            Object value = entry.getValue();
            if (value instanceof String) {
                Element  a = req.addElement(AdminService.E_A);
                a.addAttribute(AdminService.A_N, key);
                a.setText((String)value);
            } else if (value instanceof String[]) {
                String[] values = (String[]) value;
                for (String v: values) {
                    Element  a = req.addElement(AdminService.E_A);
                    a.addAttribute(AdminService.A_N, key);
                    a.setText((String)v);                    
                }
            } else {
                throw ZClientException.CLIENT_ERROR("invalid attr type: "+key+" "+value.getClass().getName(), null);
            }
        }        
    }

    @Override
    public void addAlias(Account acct, String alias) throws ServiceException {
        XMLElement req = new XMLElement(AdminService.ADD_ACCOUNT_ALIAS_REQUEST);
        req.addElement(AdminService.E_ID).setText(acct.getId());
        req.addElement(AdminService.E_ALIAS).setText(alias);
        invoke(req);
        reload(acct);
    }

    @Override
    public void addAlias(DistributionList dl, String alias)
            throws ServiceException {
        XMLElement req = new XMLElement(AdminService.ADD_DISTRIBUTION_LIST_ALIAS_REQUEST);
        req.addElement(AdminService.E_ID).setText(dl.getId());
        req.addElement(AdminService.E_ALIAS).setText(alias);
        invoke(req); 
        reload(dl);
    }

    @Override
    public void authAccount(Account acct, String password, String proto)
            throws ServiceException {
        XMLElement req = new XMLElement(AccountService.AUTH_REQUEST);
        Element a = req.addElement(AccountService.E_ACCOUNT);
        a.addAttribute(AccountService.A_BY, "name");
        a.setText(acct.getName());
        req.addElement(AccountService.E_PASSWORD).setText(password);        
        invoke(req);
    }

    @Override
    public void changePassword(Account acct, String currentPassword,
            String newPassword) throws ServiceException {
        XMLElement req = new XMLElement(AccountService.CHANGE_PASSWORD_REQUEST);
        Element a = req.addElement(AccountService.E_ACCOUNT);
        a.addAttribute(AccountService.A_BY, "name");
        a.setText(acct.getName());
        req.addElement(AccountService.E_OLD_PASSWORD).setText(currentPassword);
        req.addElement(AccountService.E_PASSWORD).setText(newPassword);        
        invoke(req);
    }

    @Override
    public Account createAccount(String emailAddress, String password, Map<String, Object> attrs) 
        throws ServiceException 
    {
        XMLElement req = new XMLElement(AdminService.CREATE_ACCOUNT_REQUEST);
        req.addElement(AdminService.E_NAME).setText(emailAddress);
        req.addElement(AdminService.E_PASSWORD).setText(password);
        addAttrElements(req, attrs);
        return new SoapAccount(invoke(req).getElement(AdminService.E_ACCOUNT));
    }

    @Override
    public CalendarResource createCalendarResource(String emailAddress, String password,
            Map<String, Object> attrs) throws ServiceException {
        XMLElement req = new XMLElement(AdminService.CREATE_CALENDAR_RESOURCE_REQUEST);
        req.addElement(AdminService.E_NAME).setText(emailAddress);
        req.addElement(AdminService.E_PASSWORD).setText(password);
        addAttrElements(req, attrs);
        return new SoapCalendarResource(invoke(req).getElement(AdminService.E_CALENDAR_RESOURCE));
    }

    @Override
    public Cos createCos(String name, Map<String, Object> attrs)
            throws ServiceException {
        XMLElement req = new XMLElement(AdminService.CREATE_COS_REQUEST);
        req.addElement(AdminService.E_NAME).setText(name);
        addAttrElements(req, attrs);
        return new SoapCos(invoke(req).getElement(AdminService.E_COS));
    }

    @Override
    public DistributionList createDistributionList(String listAddress,
            Map<String, Object> listAttrs) throws ServiceException {
        XMLElement req = new XMLElement(AdminService.CREATE_DISTRIBUTION_LIST_REQUEST);
        req.addElement(AdminService.E_NAME).setText(listAddress);
        addAttrElements(req, listAttrs);
        return new SoapDistributionList(invoke(req).getElement(AdminService.E_DL));
    }

    @Override
    public Domain createDomain(String name, Map<String, Object> attrs)
            throws ServiceException {
        XMLElement req = new XMLElement(AdminService.CREATE_DOMAIN_REQUEST);
        req.addElement(AdminService.E_NAME).setText(name);
        addAttrElements(req, attrs);
        return new SoapDomain(invoke(req).getElement(AdminService.E_DOMAIN));
    }

    @Override
    public Server createServer(String name, Map<String, Object> attrs)
            throws ServiceException {
        XMLElement req = new XMLElement(AdminService.CREATE_SERVER_REQUEST);
        req.addElement(AdminService.E_NAME).setText(name);
        addAttrElements(req, attrs);
        return new SoapServer(invoke(req).getElement(AdminService.E_SERVER));
    }

    /**
     * unsuported
     */
    @Override
    public Zimlet createZimlet(String name, Map<String, Object> attrs)
            throws ServiceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteAccount(String zimbraId) throws ServiceException {
        XMLElement req = new XMLElement(AdminService.DELETE_ACCOUNT_REQUEST);
        req.addElement(AdminService.E_ID).setText(zimbraId);
        invoke(req);
    }

    @Override
    public void deleteCalendarResource(String zimbraId) throws ServiceException {
        XMLElement req = new XMLElement(AdminService.DELETE_CALENDAR_RESOURCE_REQUEST);
        req.addElement(AdminService.E_ID).setText(zimbraId);
        invoke(req);        
    }

    @Override
    public void deleteCos(String zimbraId) throws ServiceException {
        XMLElement req = new XMLElement(AdminService.DELETE_COS_REQUEST);
        req.addElement(AdminService.E_ID).setText(zimbraId);
        invoke(req);                
    }

    @Override
    public void deleteDistributionList(String zimbraId) throws ServiceException {
        XMLElement req = new XMLElement(AdminService.DELETE_DISTRIBUTION_LIST_REQUEST);
        req.addElement(AdminService.E_ID).setText(zimbraId);
        invoke(req);
    }

    @Override
    public void deleteDomain(String zimbraId) throws ServiceException {
        XMLElement req = new XMLElement(AdminService.DELETE_DOMAIN_REQUEST);
        req.addElement(AdminService.E_ID).setText(zimbraId);
        invoke(req);                        
    }

    @Override
    public void deleteServer(String zimbraId) throws ServiceException {
        XMLElement req = new XMLElement(AdminService.DELETE_SERVER_REQUEST);
        req.addElement(AdminService.E_ID).setText(zimbraId);
        invoke(req);
    }

    /**
     * unsuported
     */
    @Override
    public void deleteZimlet(String name) throws ServiceException {
        throw new UnsupportedOperationException();
    }
    
    public static class DelegateAuthResponse {
        private String mAuthToken;
        private long mExpires;
        private long mLifetime;

        DelegateAuthResponse(Element e) throws ServiceException {
            mAuthToken = e.getElement(AccountService.E_AUTH_TOKEN).getText();
            mLifetime = e.getAttributeLong(AccountService.E_LIFETIME);
            mExpires = System.currentTimeMillis() + mLifetime;
            Element re = e.getOptionalElement(AccountService.E_REFERRAL); 
        }

        public String getAuthToken() {
            return mAuthToken;
        }
        
        public long getExpires() {
            return mExpires;
        }
        
        public long getLifetime() {
            return mLifetime;
        }
    }
    
    public DelegateAuthResponse delegateAuth(AccountBy keyType, String key, int durationSeconds) throws ServiceException {
        XMLElement req = new XMLElement(AdminService.DELEGATE_AUTH_REQUEST);
        req.addAttribute(AdminService.A_DURATION, durationSeconds);
        Element acct = req.addElement(AdminService.E_ACCOUNT);
        acct.addAttribute(AccountService.A_BY, keyType.name());
        acct.setText(key);
        return new DelegateAuthResponse(invoke(req));
    }

    public SoapAccountInfo getAccountInfo(AccountBy keyType, String key) throws ServiceException {
        XMLElement req = new XMLElement(AdminService.GET_ACCOUNT_INFO_REQUEST);
        Element a = req.addElement(AdminService.E_ACCOUNT);
        a.setText(key);
        a.addAttribute(AdminService.A_BY, keyType.name());
        return new SoapAccountInfo(invoke(req));
    }

    @Override
    public Account get(AccountBy keyType, String key) throws ServiceException {
        XMLElement req = new XMLElement(AdminService.GET_ACCOUNT_REQUEST);
        Element a = req.addElement(AdminService.E_ACCOUNT);
        a.setText(key);
        a.addAttribute(AdminService.A_BY, keyType.name());
        return new SoapAccount(invoke(req).getElement(AdminService.E_ACCOUNT));
    }

    @Override
    public List<Account> getAllAdminAccounts() throws ServiceException {
        ArrayList<Account> result = new ArrayList<Account>();
        XMLElement req = new XMLElement(AdminService.GET_ALL_ADMIN_ACCOUNTS_REQUEST);
        Element resp = invoke(req);
        for (Element a: resp.listElements(AdminService.E_ACCOUNT)) {
            result.add(new SoapAccount(a));
        }
        return result;
    }

    @Override
    public List<Cos> getAllCos() throws ServiceException {
        ArrayList<Cos> result = new ArrayList<Cos>();
        XMLElement req = new XMLElement(AdminService.GET_ALL_COS_REQUEST);
        Element resp = invoke(req);
        for (Element a: resp.listElements(AdminService.E_COS)) {
            result.add(new SoapCos(a));
        }
        return result;        
    }

    @Override
    public List<Domain> getAllDomains() throws ServiceException {
        ArrayList<Domain> result = new ArrayList<Domain>();
        XMLElement req = new XMLElement(AdminService.GET_ALL_DOMAINS_REQUEST);
        Element resp = invoke(req);
        for (Element a: resp.listElements(AdminService.E_DOMAIN)) {
            result.add(new SoapDomain(a));
        }
        return result;        
    }

    @Override
    public List<Server> getAllServers() throws ServiceException {
        ArrayList<Server> result = new ArrayList<Server>();
        XMLElement req = new XMLElement(AdminService.GET_ALL_SERVERS_REQUEST);
        Element resp = invoke(req);
        for (Element a: resp.listElements(AdminService.E_SERVER)) {
            result.add(new SoapServer(a));
        }
        return result;        
    }

    public static class QuotaUsage {
        public String mName;
        public String mId;
        long mUsed;
        long mLimit;
        
        public String getName() { return mName; }
        public String getId() { return mId; }
        public long getUsed() { return mUsed; } 
        public long getLimit() { return mLimit; }

        QuotaUsage(Element e) throws ServiceException {
            mName = e.getAttribute(AdminService.A_NAME);
            mId = e.getAttribute(AdminService.A_ID);
            mUsed = e.getAttributeLong(AdminService.A_QUOTA_USED);
            mLimit = e.getAttributeLong(AdminService.A_QUOTA_LIMIT);
        }
    }

    public List<QuotaUsage> getQuotaUsage(String server) throws ServiceException {
            ArrayList<QuotaUsage> result = new ArrayList<QuotaUsage>();
        XMLElement req = new XMLElement(AdminService.GET_QUOTA_USAGE_REQUEST);
        Element resp = invoke(req, server);
        for (Element a: resp.listElements(AdminService.E_ACCOUNT)) {
            result.add(new QuotaUsage(a));
        }
        return result;        
    }

    public static class MailboxInfo {
        private long mUsed;
        private String mMboxId;
        
        public long getUsed() { return mUsed; }
        public String getMailboxId() { return mMboxId; }
        
        public MailboxInfo(String id, long used) {
            mMboxId = id;
            mUsed = used;
        }
    }

    public MailboxInfo getMailbox(Account acct) throws ServiceException {
        XMLElement req = new XMLElement(AdminService.GET_MAILBOX_REQUEST);
        Element mboxReq = req.addElement(AdminService.E_MAILBOX);
        mboxReq.addAttribute(AdminService.A_ID, acct.getId());
        Server server = getServer(acct);
        String serviceHost = server.getAttr(A_zimbraServiceHostname);
        Element mbox = invoke(req, serviceHost).getElement(AdminService.E_MAILBOX);
        return new MailboxInfo(
                mbox.getAttribute(AdminService.A_MAILBOXID), 
                mbox.getAttributeLong(AdminService.A_SIZE));
    }

    @Override
    public List<Server> getAllServers(String service) throws ServiceException {
        ArrayList<Server> result = new ArrayList<Server>();
        XMLElement req = new XMLElement(AdminService.GET_ALL_SERVERS_REQUEST);
        req.addAttribute(AdminService.A_SERVICE, service);
        Element resp = invoke(req);
        for (Element a: resp.listElements(AdminService.E_SERVER)) {
            result.add(new SoapServer(a));
        }
        return result;        
    }

    /**
     * unsuported
     */
    @Override
    public List<WellKnownTimeZone> getAllTimeZones() throws ServiceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public CalendarResource get(CalendarResourceBy keyType, String key) throws ServiceException {
        XMLElement req = new XMLElement(AdminService.GET_CALENDAR_RESOURCE_REQUEST);
        Element a = req.addElement(AdminService.E_CALENDAR_RESOURCE);
        a.setText(key);
        a.addAttribute(AdminService.A_BY, keyType.name());
        return new SoapCalendarResource(invoke(req).getElement(AdminService.E_CALENDAR_RESOURCE));
    }

    @Override
    public Config getConfig() throws ServiceException {
        XMLElement req = new XMLElement(AdminService.GET_ALL_CONFIG_REQUEST);
        return new SoapConfig(invoke(req));
    }

    @Override
    public Cos get(CosBy keyType, String key) throws ServiceException {
        XMLElement req = new XMLElement(AdminService.GET_COS_REQUEST);
        Element a = req.addElement(AdminService.E_COS);
        a.setText(key);
        a.addAttribute(AdminService.A_BY, keyType.name());
        return new SoapCos(invoke(req).getElement(AdminService.E_COS));
    }

    @Override
    public DistributionList get(DistributionListBy keyType, String key) throws ServiceException {
        XMLElement req = new XMLElement(AdminService.GET_DISTRIBUTION_LIST_REQUEST);
        Element a = req.addElement(AdminService.E_DL);
        a.setText(key);
        a.addAttribute(AdminService.A_BY, keyType.name());
        return new SoapDistributionList(invoke(req).getElement(AdminService.E_DL));
    }

    @Override
    public Domain get(DomainBy keyType, String key) throws ServiceException {
        XMLElement req = new XMLElement(AdminService.GET_DOMAIN_REQUEST);
        Element a = req.addElement(AdminService.E_DOMAIN);
        a.setText(key);
        a.addAttribute(AdminService.A_BY, keyType.name());
        return new SoapDomain(invoke(req).getElement(AdminService.E_DOMAIN));
    }

    @Override
    public Server getLocalServer() throws ServiceException {
        String hostname = LC.zimbra_server_hostname.value();
        if (hostname == null) 
            throw ServiceException.FAILURE("zimbra_server_hostname not specified in localconfig.xml", null);
        Server local = get(ServerBy.name, hostname);
        if (local == null) 
            throw ServiceException.FAILURE("Could not find an LDAP entry for server '" + hostname + "'", null);
        return local;
    }

    /**
     * unsuported
     */
    @Override
    public MimeTypeInfo getMimeType(String name) throws ServiceException {
        throw new UnsupportedOperationException();
    }

    /**
     * unsuported
     */
    @Override
    public MimeTypeInfo getMimeTypeByExtension(String ext)
            throws ServiceException {
        throw new UnsupportedOperationException();
    }

    /**
     * unsuported
     */
    @Override
    public List<Zimlet> getObjectTypes() throws ServiceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Server get(ServerBy keyType, String key) throws ServiceException {
        XMLElement req = new XMLElement(AdminService.GET_SERVER_REQUEST);
        Element a = req.addElement(AdminService.E_SERVER);
        a.setText(key);
        a.addAttribute(AdminService.A_BY, keyType.name());
        return new SoapServer(invoke(req).getElement(AdminService.E_SERVER));
    }

    /**
     * unsuported
     */
    @Override
    public WellKnownTimeZone getTimeZoneById(String tzId)
            throws ServiceException {
        throw new UnsupportedOperationException();
    }

    /**
     * unsuported
     */
    @Override
    public Zimlet getZimlet(String name) throws ServiceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean healthCheck() throws ServiceException {
        XMLElement req = new XMLElement(AdminService.CHECK_HEALTH_REQUEST);
        Element response = invoke(req);
        return response.getAttributeBool(AdminService.A_HEALTHY);
    }

    /**
     * unsuported
     */
    @Override
    public List<Zimlet> listAllZimlets() throws ServiceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void modifyAccountStatus(Account acct, String newStatus)
            throws ServiceException {
        HashMap<String, String> attrs = new HashMap<String,String>();
        attrs.put(Provisioning.A_zimbraAccountStatus, newStatus);
        modifyAttrs(acct, attrs);
    }

    @Override
    public void preAuthAccount(Account acct, String accountName,
            String accountBy, long timestamp, long expires, String preAuth)
            throws ServiceException {
        XMLElement req = new XMLElement(AccountService.AUTH_REQUEST);
        Element a = req.addElement(AccountService.E_ACCOUNT);
        a.addAttribute(AccountService.A_BY, "name");
        a.setText(accountName);
        Element p = req.addElement(AccountService.E_PREAUTH);
        p.addAttribute(AccountService.A_TIMESTAMP, timestamp);
        p.addAttribute(AccountService.A_BY, accountBy);
        p.addAttribute(AccountService.A_EXPIRES, expires);
        p.setText(preAuth);
        invoke(req);
    }

    @Override
    public void removeAlias(Account acct, String alias) throws ServiceException {
        XMLElement req = new XMLElement(AdminService.REMOVE_ACCOUNT_ALIAS_REQUEST);
        req.addElement(AdminService.E_ID).setText(acct.getId());
        req.addElement(AdminService.E_ALIAS).setText(alias);
        invoke(req);
        reload(acct);
    }

    @Override
    public void removeAlias(DistributionList dl, String alias)
            throws ServiceException {
        XMLElement req = new XMLElement(AdminService.REMOVE_DISTRIBUTION_LIST_ALIAS_REQUEST);
        req.addElement(AdminService.E_ID).setText(dl.getId());
        req.addElement(AdminService.E_ALIAS).setText(alias);
        invoke(req);
        reload(dl);
    }

    @Override
    public void renameAccount(String zimbraId, String newName)
            throws ServiceException {
        XMLElement req = new XMLElement(AdminService.RENAME_ACCOUNT_REQUEST);
        req.addElement(AdminService.E_ID).setText(zimbraId);
        req.addElement(AdminService.E_NEW_NAME).setText(newName);
        invoke(req);
    }

    @Override
    public void renameCalendarResource(String zimbraId, String newName)
            throws ServiceException {
        XMLElement req = new XMLElement(AdminService.RENAME_CALENDAR_RESOURCE_REQUEST);
        req.addElement(AdminService.E_ID).setText(zimbraId);
        req.addElement(AdminService.E_NEW_NAME).setText(newName);
        invoke(req);
    }

    @Override
    public void renameCos(String zimbraId, String newName)
            throws ServiceException {
        XMLElement req = new XMLElement(AdminService.RENAME_COS_REQUEST);
        req.addElement(AdminService.E_ID).setText(zimbraId);
        req.addElement(AdminService.E_NEW_NAME).setText(newName);
        invoke(req);        
    }

    @Override
    public void renameDistributionList(String zimbraId, String newName)
            throws ServiceException {
        XMLElement req = new XMLElement(AdminService.RENAME_DISTRIBUTION_LIST_REQUEST);
        req.addElement(AdminService.E_ID).setText(zimbraId);
        req.addElement(AdminService.E_NEW_NAME).setText(newName);
        invoke(req);
    }

    @Override
    public List<NamedEntry> searchAccounts(String query, String[] returnAttrs,
            String sortAttr, boolean sortAscending, int flags)
            throws ServiceException {
        return searchAccounts((Domain) null, query, returnAttrs, sortAttr, sortAscending, flags);
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<NamedEntry> searchCalendarResources(EntrySearchFilter filter,
            String[] returnAttrs, String sortAttr, boolean sortAscending)
            throws ServiceException {
        return searchCalendarResources((Domain)null, filter, returnAttrs, sortAttr, sortAscending);
    }

    @Override
    public void setCOS(Account acct, Cos cos) throws ServiceException {
        HashMap<String, String> attrs = new HashMap<String, String>();
        attrs.put(Provisioning.A_zimbraCOSId, cos.getId());
        modifyAttrs(acct, attrs);
    }

    @Override
    public void setPassword(Account acct, String newPassword)
            throws ServiceException {
        XMLElement req = new XMLElement(AdminService.SET_PASSWORD_REQUEST);
        req.addElement(AdminService.E_ID).setText(acct.getId());
        req.addElement(AdminService.E_NEW_PASSWORD).setText(newPassword);
        invoke(req);
    }

    @Override
    public void modifyAttrs(com.zimbra.cs.account.Entry e,
                            Map<String, ? extends Object> attrs,
                            boolean checkImmutable)
    throws ServiceException {
        SoapEntry se = (SoapEntry) e;
        se.modifyAttrs(this, attrs, checkImmutable);
    }

    @Override
    public void modifyAttrs(com.zimbra.cs.account.Entry e,
                            Map<String, ? extends Object> attrs,
                            boolean checkImmutable,
                            boolean allowCallback)
    throws ServiceException {
        // allowCallback is ignored over SOAP interface
        modifyAttrs(e, attrs, checkImmutable, true);
    }

    @Override
    public void reload(com.zimbra.cs.account.Entry e) throws ServiceException {
        SoapEntry se = (SoapEntry) e;
        se.reload(this);
    }

    private static final String DATA_DL_SET = "DL_SET";
    
    @SuppressWarnings("unchecked")
    @Override
    public Set<String> getDistributionLists(Account acct) throws ServiceException {
        Set<String> dls = (Set<String>) acct.getCachedData(DATA_DL_SET);
        if (dls != null) return dls;
     
        dls = new HashSet<String>();
       
       List<DistributionList> lists = getDistributionLists(acct, false, null);
        
        for (DistributionList dl : lists) {
            dls.add(dl.getId());
        }
        dls = Collections.unmodifiableSet(dls);
        acct.setCachedData(DATA_DL_SET, dls);
        return dls;
    }

    @Override
    public List<DistributionList> getDistributionLists(Account acct, boolean directOnly, Map<String, String> via) throws ServiceException {
        ArrayList<DistributionList> result = new ArrayList<DistributionList>();
        XMLElement req = new XMLElement(AdminService.GET_ACCOUNT_MEMBERSHIP_REQUEST);
        Element acctEl = req.addElement(AdminService.E_ACCOUNT);
        acctEl.addAttribute(AdminService.A_BY, AccountBy.id.name());
        acctEl.setText(acct.getId());
        Element resp = invoke(req);
        for (Element a: resp.listElements(AdminService.E_DL)) {
            String viaList = a.getAttribute(AdminService.A_VIA, null);
            if (directOnly && viaList != null) continue;
            DistributionList dl = new SoapDistributionList(a);
            if (via != null && viaList != null) {
                via.put(dl.getName(), viaList);
            }
            result.add(dl);
        }
        return result;
    }

    @Override
    public boolean inDistributionList(Account acct, String zimbraId) throws ServiceException {
        return getDistributionLists(acct).contains(zimbraId);  
    }

    /**
     * unsuported
     */
    @Override
    public ICalTimeZone getTimeZone(Account acct) throws ServiceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<DistributionList> getDistributionLists(DistributionList list, boolean directOnly, Map<String, String> via) throws ServiceException {
        ArrayList<DistributionList> result = new ArrayList<DistributionList>();
        XMLElement req = new XMLElement(AdminService.GET_DISTRIBUTION_LIST_MEMBERSHIP_REQUEST);
        Element acctEl = req.addElement(AdminService.E_DL);
        acctEl.addAttribute(AdminService.A_BY, DistributionListBy.id.name());
        acctEl.setText(list.getId());
        Element resp = invoke(req);
        for (Element a: resp.listElements(AdminService.E_DL)) {
            String viaList = a.getAttribute(AdminService.A_VIA, null);
            if (directOnly && viaList != null) continue;
            DistributionList dl = new SoapDistributionList(a);
            if (via != null && viaList != null) {
                via.put(dl.getName(), viaList);
            }
            result.add(dl);
        }
        return result;
    }

    @Override
    public List getAllAccounts(Domain d) throws ServiceException {
        ArrayList<Account> result = new ArrayList<Account>();
        XMLElement req = new XMLElement(AdminService.GET_ALL_ACCOUNTS_REQUEST);
        Element domainEl = req.addElement(AdminService.E_DOMAIN);
        domainEl.addAttribute(AdminService.A_BY, AccountBy.id.name());
        domainEl.setText(d.getId());
        Element resp = invoke(req);
        for (Element a: resp.listElements(AdminService.E_ACCOUNT)) {
            result.add(new SoapAccount(a));
        }
        return result;
    }

    @Override
    public void getAllAccounts(Domain d, Visitor visitor) throws ServiceException {
        XMLElement req = new XMLElement(AdminService.GET_ALL_ACCOUNTS_REQUEST);
        Element domainEl = req.addElement(AdminService.E_DOMAIN);
        domainEl.addAttribute(AdminService.A_BY, DomainBy.id.name());
        domainEl.setText(d.getId());
        Element resp = invoke(req);
        for (Element a: resp.listElements(AdminService.E_ACCOUNT)) {
            visitor.visit(new SoapAccount(a));
        }
    }

    @Override
    public List getAllCalendarResources(Domain d) throws ServiceException {
        ArrayList<CalendarResource> result = new ArrayList<CalendarResource>();
        XMLElement req = new XMLElement(AdminService.GET_ALL_CALENDAR_RESOURCES_REQUEST);
        Element domainEl = req.addElement(AdminService.E_DOMAIN);
        domainEl.addAttribute(AdminService.A_BY, CalendarResourceBy.id.name());
        domainEl.setText(d.getId());
        Element resp = invoke(req);
        for (Element a: resp.listElements(AdminService.E_CALENDAR_RESOURCE)) {
            result.add(new SoapCalendarResource(a));
        }
        return result;
    }

    @Override
    public void getAllCalendarResources(Domain d, Visitor visitor) throws ServiceException {
        XMLElement req = new XMLElement(AdminService.GET_ALL_CALENDAR_RESOURCES_REQUEST);
        Element domainEl = req.addElement(AdminService.E_DOMAIN);
        domainEl.addAttribute(AdminService.A_BY, CalendarResourceBy.id.name());
        domainEl.setText(d.getId());
        Element resp = invoke(req);
        for (Element a: resp.listElements(AdminService.E_CALENDAR_RESOURCE)) {
            
            visitor.visit(new SoapCalendarResource(a));
        }
    }

    @Override
    public List getAllDistributionLists(Domain d) throws ServiceException {
        ArrayList<DistributionList> result = new ArrayList<DistributionList>();
        XMLElement req = new XMLElement(AdminService.GET_ALL_DISTRIBUTION_LISTS_REQUEST);
        Element domainEl = req.addElement(AdminService.E_DOMAIN);
        domainEl.addAttribute(AdminService.A_BY, DomainBy.id.name());
        domainEl.setText(d.getId());
        Element resp = invoke(req);
        for (Element a: resp.listElements(AdminService.E_DL)) {
            result.add(new SoapDistributionList(a));
        }
        return result;
    }

    @Override
    public SearchGalResult autoCompleteGal(Domain d, String query, GAL_SEARCH_TYPE type, int limit) throws ServiceException {
        String typeStr = null;

        if (type == GAL_SEARCH_TYPE.ALL) typeStr = "all";
        else if (type == GAL_SEARCH_TYPE.USER_ACCOUNT) typeStr = "account";
        else if (type == GAL_SEARCH_TYPE.CALENDAR_RESOURCE) typeStr = "resource";
        else typeStr = "all";
        
        XMLElement req = new XMLElement(AdminService.AUTO_COMPLETE_GAL_REQUEST);
        req.addElement(AdminService.E_NAME).setText(query);
        req.addAttribute(AdminService.A_DOMAIN, d.getName());
        req.addAttribute(AdminService.A_TYPE, typeStr);
        req.addAttribute(AdminService.A_LIMIT, limit);

        Element resp = invoke(req);

        SearchGalResult result = new SearchGalResult();
        result.matches = new ArrayList<GalContact>();
        result.hadMore = resp.getAttributeBool(AdminService.A_MORE);
        for (Element e: resp.listElements(AdminService.E_CN)) {
            result.matches.add(new GalContact(AdminService.A_ID, getAttrs(e)));
        }
        return result;
    }

    @Override
    public List<NamedEntry> searchAccounts(Domain d, String query, String[] returnAttrs, String sortAttr, boolean sortAscending, int flags) throws ServiceException {
        List<NamedEntry> result = new ArrayList<NamedEntry>();
        XMLElement req = new XMLElement(AdminService.SEARCH_ACCOUNTS_REQUEST);
        req.addElement(AdminService.E_QUERY).setText(query);
        if (d != null) req.addAttribute(AdminService.A_DOMAIN, d.getName());
        if (sortAttr != null) req.addAttribute(AdminService.A_SORT_BY, sortAttr);
        if (flags != 0) req.addAttribute(AdminService.A_TYPES, Provisioning.searchAccountMaskToString(flags));
        req.addAttribute(AdminService.A_SORT_ASCENDING, sortAscending ? "1" : "0");
        if (returnAttrs != null) {
            req.addAttribute(AdminService.A_ATTRS, StringUtil.join(",", returnAttrs));
        }
        // TODO: handle ApplyCos, limit, offset?
        Element resp = invoke(req);
        for (Element e: resp.listElements(AdminService.E_DL))
            result.add(new SoapDistributionList(e));

        for (Element e: resp.listElements(AdminService.E_ALIAS))
            result.add(new SoapAlias(e));
        
        for (Element e: resp.listElements(AdminService.E_ACCOUNT))
            result.add(new SoapAccount(e));
        
        return result;
    }

    @Override
    public List searchCalendarResources(Domain d, EntrySearchFilter filter, String[] returnAttrs, String sortAttr, boolean sortAscending) throws ServiceException {
        // TODO
        throw new UnsupportedOperationException();        
/*
        List<NamedEntry> result = new ArrayList<NamedEntry>();
        XMLElement req = new XMLElement(AdminService.SEARCH_CALENDAR_RESOURCES_REQUEST);
        req.addElement(MailSer).setText(query);
        if (d != null) req.addAttribute(AdminService.A_DOMAIN, d.getName());
        if (sortAttr != null) req.addAttribute(AdminService.A_SORT_BY, sortAttr);
        if (flags != 0) req.addAttribute(AdminService.A_TYPES, Provisioning.searchAccountMaskToString(flags));
        req.addAttribute(AdminService.A_SORT_ASCENDING, sortAscending ? "1" : "0");
        if (returnAttrs != null) {
            req.addAttribute(AdminService.A_ATTRS, StringUtil.join(",", returnAttrs));
        }
        // TODO: handle ApplyCos, limit, offset?
        Element resp = invoke(req);
        for (Element e: resp.listElements(AdminService.E_CALENDAR_RESOURCE))
            result.add(new SoapCalendarResource(e));
        
        return result;
*/        
    }

    @Override
    public SearchGalResult searchGal(Domain d, String query, GAL_SEARCH_TYPE type, String token) throws ServiceException {
        String typeStr = null;

        if (type == GAL_SEARCH_TYPE.ALL) typeStr = "all";
        else if (type == GAL_SEARCH_TYPE.USER_ACCOUNT) typeStr = "account";
        else if (type == GAL_SEARCH_TYPE.CALENDAR_RESOURCE) typeStr = "resource";
        else typeStr = "all";
        
        XMLElement req = new XMLElement(AdminService.SEARCH_GAL_REQUEST);
        req.addElement(AdminService.E_NAME).setText(query);
        req.addAttribute(AdminService.A_DOMAIN, d.getName());
        req.addAttribute(AdminService.A_TYPE, typeStr);
        if (token != null) req.addAttribute(AdminService.A_TOKEN, token);

        Element resp = invoke(req);

        SearchGalResult result = new SearchGalResult();
        result.matches = new ArrayList<GalContact>();
        result.hadMore = resp.getAttributeBool(AdminService.A_MORE);
        for (Element e: resp.listElements(AdminService.E_CN)) {
            result.matches.add(new GalContact(AdminService.A_ID, getAttrs(e)));
        }
        return result;
    }

    @Override
    public void addMembers(DistributionList list, String[] members) throws ServiceException {
        XMLElement req = new XMLElement(AdminService.ADD_DISTRIBUTION_LIST_MEMBER_REQUEST);
        req.addElement(AdminService.E_ID).setText(list.getId());
        for (String m : members) {
            req.addElement(AdminService.E_DLM).setText(m);
        }
        invoke(req);
        reload(list);        
    }

    @Override
    public void removeMembers(DistributionList list, String[] members) throws ServiceException {
        XMLElement req = new XMLElement(AdminService.REMOVE_DISTRIBUTION_LIST_MEMBER_REQUEST);
        req.addElement(AdminService.E_ID).setText(list.getId());
        for (String m : members) {
            req.addElement(AdminService.E_DLM).setText(m);
        }
        invoke(req);
        reload(list);
    }
}
