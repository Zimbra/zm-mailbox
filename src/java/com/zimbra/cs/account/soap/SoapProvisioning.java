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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import com.zimbra.cs.localconfig.LC;
import com.zimbra.cs.mime.MimeTypeInfo;
import com.zimbra.cs.service.ServiceException;
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
    
    private Element invoke(Element request) throws ServiceException {
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

    static void addAttrElements(Element req, Map<String, Object> attrs) throws SoapFaultException {
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
        // TODO Auto-generated method stub
    }

    @Override
    public void addAlias(DistributionList dl, String alias)
            throws ServiceException {
        // TODO Auto-generated method stub

    }

    @Override
    public void authAccount(Account acct, String password)
            throws ServiceException {
        // TODO Auto-generated method stub

    }

    @Override
    public void changePassword(Account acct, String currentPassword,
            String newPassword) throws ServiceException {
        // TODO Auto-generated method stub

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
    public Account createAdminAccount(String name, String password,
            Map<String, Object> attrs) throws ServiceException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public CalendarResource createCalendarResource(String emailAddress,
            Map<String, Object> attrs) throws ServiceException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Cos createCos(String name, Map<String, Object> attrs)
            throws ServiceException {
        // TODO Auto-generated method stub
        return null;
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
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Server createServer(String name, Map<String, Object> attrs)
            throws ServiceException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Zimlet createZimlet(String name, Map<String, Object> attrs)
            throws ServiceException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void deleteAccount(String zimbraId) throws ServiceException {
        // TODO Auto-generated method stub

    }

    @Override
    public void deleteCalendarResource(String zimbraId) throws ServiceException {
        // TODO Auto-generated method stub

    }

    @Override
    public void deleteCos(String zimbraId) throws ServiceException {
        // TODO Auto-generated method stub

    }

    @Override
    public void deleteDistributionList(String zimbraId) throws ServiceException {
        // TODO Auto-generated method stub

    }

    @Override
    public void deleteDomain(String zimbraId) throws ServiceException {
        // TODO Auto-generated method stub

    }

    @Override
    public void deleteServer(String zimbraId) throws ServiceException {
        // TODO Auto-generated method stub

    }

    @Override
    public void deleteZimlet(String name) throws ServiceException {
        // TODO Auto-generated method stub

    }

    @Override
    public Account getAccountByForeignPrincipal(String principal)
            throws ServiceException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Account getAccountById(String zimbraId) throws ServiceException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Account getAccountByName(String emailAddress)
            throws ServiceException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Account getAdminAccountByName(String name) throws ServiceException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<Account> getAllAdminAccounts() throws ServiceException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<Cos> getAllCos() throws ServiceException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<Domain> getAllDomains() throws ServiceException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<Server> getAllServers() throws ServiceException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<Server> getAllServers(String service) throws ServiceException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<WellKnownTimeZone> getAllTimeZones() throws ServiceException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public CalendarResource getCalendarResourceByForeignPrincipal(
            String foreignPrincipal) throws ServiceException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public CalendarResource getCalendarResourceById(String zimbraId)
            throws ServiceException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public CalendarResource getCalendarResourceByName(String emailAddress)
            throws ServiceException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Config getConfig() throws ServiceException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Cos getCosById(String zimbraId) throws ServiceException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Cos getCosByName(String name) throws ServiceException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public DistributionList getDistributionListById(String zimbraId)
            throws ServiceException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public DistributionList getDistributionListByName(String name)
            throws ServiceException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Domain getDomainById(String zimbraId) throws ServiceException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Domain getDomainByName(String name) throws ServiceException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Domain getDomainByVirtualHostname(String virtualHostname)
            throws ServiceException {
        // TODO Auto-generated method stub
        return null;
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

    @Override
    public Server getServerById(String zimbraId) throws ServiceException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Server getServerById(String zimbraId, boolean reload)
            throws ServiceException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Server getServerByName(String name) throws ServiceException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Server getServerByName(String name, boolean reload)
            throws ServiceException {
        // TODO Auto-generated method stub
        return null;
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
        // TODO Auto-generated method stub

    }

    @Override
    public void preAuthAccount(Account acct, String accountName,
            String accountBy, long timestamp, long expires, String preAuth)
            throws ServiceException {
        // TODO Auto-generated method stub

    }

    @Override
    public void removeAlias(Account acct, String alias) throws ServiceException {
        // TODO Auto-generated method stub

    }

    @Override
    public void removeAlias(DistributionList dl, String alias)
            throws ServiceException {
        // TODO Auto-generated method stub

    }

    @Override
    public void renameAccount(String zimbraId, String newName)
            throws ServiceException {
        // TODO Auto-generated method stub

    }

    @Override
    public void renameCalendarResource(String zimbraId, String newName)
            throws ServiceException {
        // TODO Auto-generated method stub

    }

    @Override
    public void renameCos(String zimbraId, String newName)
            throws ServiceException {
        // TODO Auto-generated method stub

    }

    @Override
    public void renameDistributionList(String zimbraId, String newName)
            throws ServiceException {
        // TODO Auto-generated method stub

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
        // TODO Auto-generated method stub

    }

    @Override
    public void setPassword(Account acct, String newPassword)
            throws ServiceException {
        // TODO Auto-generated method stub

    }
    
    public static void main(String args[]) throws ServiceException, IOException {
        try {
            Zimbra.toolSetup();
            SoapProvisioning p = new SoapProvisioning();
            p.soapSetURI("https://localhost:7071/service/admin/soap");
            p.soapZimbraAdminAuthenticate();
            HashMap<String,Object> attrs = new HashMap<String,Object>();
            attrs.put(Provisioning.A_displayName, "DISPLAY THIS");
            Account acct = p.createAccount("userkewl6@macintel.local", "test123", attrs);
            System.out.println(acct);
        } catch (SoapFaultException e) {
            System.out.println(e.getCode());
        }
    }
}
