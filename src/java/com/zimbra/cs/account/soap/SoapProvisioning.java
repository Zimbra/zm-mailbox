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
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.account.soap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
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
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.WellKnownTimeZone;
import com.zimbra.cs.account.Zimlet;
import com.zimbra.cs.account.NamedEntry.Visitor;
import com.zimbra.cs.localconfig.LC;
import com.zimbra.cs.mailbox.calendar.ICalTimeZone;
import com.zimbra.cs.mime.MimeTypeInfo;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.account.AccountService;
import com.zimbra.cs.service.admin.AdminService;
import com.zimbra.cs.util.StringUtil;
import com.zimbra.cs.util.Zimbra;
import com.zimbra.soap.Element;
import com.zimbra.soap.SoapFaultException;
import com.zimbra.soap.SoapHttpTransport;
import com.zimbra.soap.Element.XMLElement;

public class SoapProvisioning extends Provisioning {

    private SoapHttpTransport mTransport;
    private String mAuthToken;
    private long mAuthTokenLifetime;
    private long mAuthTokenExpiration;
    
    public SoapProvisioning() {
        
    }

    /**
     * @param uri URI of server we want to talk to
     */
    public void soapSetURI(String uri) {
        if (mTransport != null) mTransport.shutdown();
        mTransport = new SoapHttpTransport(uri);
    }
    
    public String soapGetURI() {
        return mTransport.getURI();
    }

    /**
     * used to authenticate via admin AuthRequest. can only be called after setting the URI with setURI.
     * 
     * @param name
     * @param password
     * @throws ServiceException
     * @throws IOException 
     */
    public void soapAdminAuthenticate(String name, String password) throws ServiceException, IOException {
       if (mTransport == null) throw SoapFaultException.CLIENT_ERROR("must call setURI before calling adminAuthenticate", null);
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
     * auth as zimbra admin from localconfig
     * 
     * @throws ServiceException
     * @throws IOException
     */
    public void soapZimbraAdminAuthenticate() throws ServiceException, IOException {
        soapAdminAuthenticate(LC.zimbra_ldap_user.value(), LC.zimbra_ldap_password.value());
    }
    
    Element invoke(Element request) throws ServiceException {
        try {
            return mTransport.invoke(request);
        } catch (SoapFaultException e) {
            throw e; // for now, later, try to map to more specific exception
        } catch (IOException e) {
            throw SoapFaultException.IO_ERROR("invoke "+e.getMessage(), e);
        }        
    }
    
    static Map<String, Object> getAttrs(Element e) throws ServiceException {
        Map<String, Object> result = new HashMap<String,Object>();
        for (Element a : e.listElements(AdminService.E_A)) {
            StringUtil.addToMultiMap(result, a.getAttribute(AdminService.A_N), a.getText());
        }
        return result;
    }

    static void addAttrElements(Element req, Map<String, ? extends Object> attrs) throws SoapFaultException {
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
                throw SoapFaultException.CLIENT_ERROR("invalid attr type: "+key+" "+value.getClass().getName(), null);
            }
        }        
    }

    @Override
    public void addAlias(Account acct, String alias) throws ServiceException {
        XMLElement req = new XMLElement(AdminService.ADD_ACCOUNT_ALIAS_REQUEST);
        req.addElement(AdminService.E_ID).setText(acct.getId());
        req.addElement(AdminService.E_ALIAS).setText(alias);
        invoke(req);        
    }

    @Override
    public void addAlias(DistributionList dl, String alias)
            throws ServiceException {
        XMLElement req = new XMLElement(AdminService.ADD_DISTRIBUTION_LIST_ALIAS_REQUEST);
        req.addElement(AdminService.E_ID).setText(dl.getId());
        req.addElement(AdminService.E_ALIAS).setText(alias);
        invoke(req);                
    }

    @Override
    public void authAccount(Account acct, String password)
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
    public CalendarResource createCalendarResource(String emailAddress,
            Map<String, Object> attrs) throws ServiceException {
        XMLElement req = new XMLElement(AdminService.CREATE_CALENDAR_RESOURCE_REQUEST);
        req.addElement(AdminService.E_NAME).setText(emailAddress);
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
        // TODO Auto-generated method stub
        return null;
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

    @Override
    public Zimlet createZimlet(String name, Map<String, Object> attrs)
            throws ServiceException {
        // TODO Auto-generated method stub
        return null;
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

    @Override
    public void deleteZimlet(String name) throws ServiceException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    private Account getAccountBy(String by, String value) throws ServiceException {
        XMLElement req = new XMLElement(AdminService.GET_ACCOUNT_REQUEST);
        Element a = req.addElement(AdminService.E_ACCOUNT);
        a.setText(value);
        a.addAttribute(AdminService.A_BY, by);
        return new SoapAccount(invoke(req).getElement(AdminService.E_ACCOUNT));
    }

    @Override
    public Account get(AccountBy keyType, String key) throws ServiceException {
        switch(keyType) {
        case ADMIN_NAME:
            return getAccountBy(AdminService.BY_ADMIN_NAME, key);            
        case ID:
            return getAccountBy(AdminService.BY_ID, key);
        case FOREIGN_PRINCIPAL:
            return getAccountBy(AdminService.BY_FOREIGN_PRINCIPAL, key);
        case NAME:
            return getAccountBy(AdminService.BY_NAME, key);            
        default:
                return null;
        }
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

    @Override
    public List<WellKnownTimeZone> getAllTimeZones() throws ServiceException {
        // TODO Auto-generated method stub
        return null;
    }

    private CalendarResource getCalendarResourceBy(String by, String value) throws ServiceException {
        XMLElement req = new XMLElement(AdminService.GET_CALENDAR_RESOURCE_REQUEST);
        Element a = req.addElement(AdminService.E_CALENDAR_RESOURCE);
        a.setText(value);
        a.addAttribute(AdminService.A_BY, by);
        return new SoapCalendarResource(invoke(req).getElement(AdminService.E_CALENDAR_RESOURCE));
    }

    @Override
    public CalendarResource get(CalendarResourceBy keyType, String key) throws ServiceException {
        switch(keyType) {
            case ID:
                return getCalendarResourceBy(AdminService.BY_ID, key);
            case FOREIGN_PRINCIPAL:
                return getCalendarResourceBy(AdminService.BY_FOREIGN_PRINCIPAL, key);
            case NAME: 
                return getCalendarResourceBy(AdminService.BY_NAME, key);
            default:
                    return null;
        }
    }

    @Override
    public Config getConfig() throws ServiceException {
        XMLElement req = new XMLElement(AdminService.GET_ALL_CONFIG_REQUEST);
        return new SoapConfig(invoke(req));
    }

    private Cos getCosBy(String by, String value) throws ServiceException {
        XMLElement req = new XMLElement(AdminService.GET_COS_REQUEST);
        Element a = req.addElement(AdminService.E_COS);
        a.setText(value);
        a.addAttribute(AdminService.A_BY, by);
        return new SoapCos(invoke(req).getElement(AdminService.E_COS));
    }

    @Override
    public Cos get(CosBy keyType, String key) throws ServiceException {
        switch(keyType) {
        case ID:
            return getCosBy(AdminService.BY_ID, key);
        case NAME:
            return getCosBy(AdminService.BY_NAME, key);
        default:
                return null;
        }
    }

    @Override
    public DistributionList get(DistributionListBy keyType, String key) throws ServiceException {
        // TODO Auto-generated method stub        
        /*
        switch(keyType) {
            case ID: 
                return getDistributionListById(key);
            case NAME: 
                return getDistributionListByName(key);
            default:
                    return null;
        }
        */
        return null;        
    }

    private Domain getDomainBy(String by, String value) throws ServiceException {
        XMLElement req = new XMLElement(AdminService.GET_DOMAIN_REQUEST);
        Element a = req.addElement(AdminService.E_DOMAIN);
        a.setText(value);
        a.addAttribute(AdminService.A_BY, by);
        return new SoapDomain(invoke(req).getElement(AdminService.E_DOMAIN));
    }

    @Override
    public Domain get(DomainBy keyType, String key) throws ServiceException {
        switch(keyType) {
        case ID:
            return getDomainBy(AdminService.BY_ID, key);
        case NAME:
            return getDomainBy(AdminService.BY_NAME, key);
        case VIRTUAL_HOST_NAME:
            return getDomainBy(AdminService.BY_VIRTUAL_HOST_NAME, key);
        default:
                return null;
        }
    }

    @Override
    public Server getLocalServer() throws ServiceException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public MimeTypeInfo getMimeType(String name) throws ServiceException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public MimeTypeInfo getMimeTypeByExtension(String ext)
            throws ServiceException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<Zimlet> getObjectTypes() throws ServiceException {
        // TODO Auto-generated method stub
        return null;
    }

    private Server getServerBy(String by, String value) throws ServiceException {
        XMLElement req = new XMLElement(AdminService.GET_SERVER_REQUEST);
        Element a = req.addElement(AdminService.E_SERVER);
        a.setText(value);
        a.addAttribute(AdminService.A_BY, by);
        return new SoapServer(invoke(req).getElement(AdminService.E_SERVER));
    }

    @Override
    public Server get(ServerBy keyType, String key) throws ServiceException {
        switch(keyType) {
        case ID:
            return getServerBy(AdminService.BY_ID, key);            
        case NAME:
            return getServerBy(AdminService.BY_NAME, key);
        default:
                return null;
        }
    }

    @Override
    public WellKnownTimeZone getTimeZoneById(String tzId)
            throws ServiceException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Zimlet getZimlet(String name) throws ServiceException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean healthCheck() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public List<Zimlet> listAllZimlets() throws ServiceException {
        // TODO Auto-generated method stub
        return null;
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
        // TODO Auto-generated method stub

    }

    @Override
    public void removeAlias(Account acct, String alias) throws ServiceException {
        XMLElement req = new XMLElement(AdminService.REMOVE_ACCOUNT_ALIAS_REQUEST);
        req.addElement(AdminService.E_ID).setText(acct.getId());
        req.addElement(AdminService.E_ALIAS).setText(alias);
        invoke(req);        
    }

    @Override
    public void removeAlias(DistributionList dl, String alias)
            throws ServiceException {
        XMLElement req = new XMLElement(AdminService.REMOVE_DISTRIBUTION_LIST_ALIAS_REQUEST);
        req.addElement(AdminService.E_ID).setText(dl.getId());
        req.addElement(AdminService.E_ALIAS).setText(alias);
        invoke(req);        
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
        // TODO Auto-generated method stub
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
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<NamedEntry> searchCalendarResources(EntrySearchFilter filter,
            String[] returnAttrs, String sortAttr, boolean sortAscending)
            throws ServiceException {
        // TODO Auto-generated method stub
        return null;
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
    public void modifyAttrs(com.zimbra.cs.account.Entry e, Map<String, ? extends Object> attrs, boolean checkImmutable) throws ServiceException {
        SoapEntry se = (SoapEntry) e;
        se.modifyAttrs(this, attrs, checkImmutable);
    }

    @Override
    public void reload(com.zimbra.cs.account.Entry e) throws ServiceException {
        SoapEntry se = (SoapEntry) e;
        se.reload(this);
    }
    
    public static void main(String args[]) throws ServiceException, IOException {
        try {
            Zimbra.toolSetup();
            SoapProvisioning p = new SoapProvisioning();
            p.soapSetURI("https://localhost:7071/service/admin/soap");
            p.soapZimbraAdminAuthenticate();
            /*
            HashMap<String,Object> attrs = new HashMap<String,Object>();
            attrs.put(Provisioning.A_displayName, "DISPLAY THIS");
            Account acct = p.createAccount("userkewl8@macintel.local", "test123", attrs);
            System.out.println(acct);
            attrs = new HashMap<String,Object>();
            attrs.put(Provisioning.A_displayName, "DISPLAY THAT");
            p.modifyAttrs(acct, attrs);
            */
            /*
            Account acct2 = p.getAccountByName("userkewl8@macintel.local");
            System.out.println(acct2);
            p.changePassword(acct2, "test123", "test1235");
            */
            Account acct3 = p.get(AccountBy.ADMIN_NAME, "zimbra");
            System.out.println(acct3);
            System.out.println("----");
            for (Account at: p.getAllAdminAccounts()) System.out.println(at.getName());
            System.out.println("----");            
        } catch (SoapFaultException e) {
            System.out.println(e.getCode());
        }
    }

    @Override
    public Set<String> getDistributionLists(Account acct) throws ServiceException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<DistributionList> getDistributionLists(Account acct, boolean directOnly, Map<String, String> via) throws ServiceException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean inDistributionList(Account acct, String zimbraId) throws ServiceException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public ICalTimeZone getTimeZone(Account acct) throws ServiceException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<DistributionList> getDistributionLists(DistributionList list, boolean directOnly, Map<String, String> via) throws ServiceException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List getAllAccounts(Domain d) throws ServiceException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void getAllAccounts(Domain d, Visitor visitor) throws ServiceException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public List getAllCalendarResources(Domain d) throws ServiceException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void getAllCalendarResources(Domain d, Visitor visitor) throws ServiceException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public List getAllDistributionLists(Domain d) throws ServiceException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public SearchGalResult autoCompleteGal(Domain d, String query, GAL_SEARCH_TYPE type, int limit) throws ServiceException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List searchAccounts(Domain d, String query, String[] returnAttrs, String sortAttr, boolean sortAscending, int flags) throws ServiceException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List searchCalendarResources(Domain d, EntrySearchFilter filter, String[] returnAttrs, String sortAttr, boolean sortAscending) throws ServiceException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public SearchGalResult searchGal(Domain d, String query, GAL_SEARCH_TYPE type, String token) throws ServiceException {
        // TODO Auto-generated method stub
        return null;
    }
}
