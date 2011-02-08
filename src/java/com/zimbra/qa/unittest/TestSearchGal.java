package com.zimbra.qa.unittest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.SoapHttpTransport;
import com.zimbra.common.soap.SoapTransport;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.CalendarResource;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.GalContact;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.account.Provisioning.CalendarResourceBy;
import com.zimbra.cs.account.Provisioning.DomainBy;
import com.zimbra.cs.account.Provisioning.SearchGalResult;
import com.zimbra.cs.account.ZAttrProvisioning.CalResType;
import com.zimbra.cs.account.soap.SoapProvisioning;

/*
TODO: Add this class to {@link ZimbraSuite} once it supports JUnit 4 annotations.
*/

public class TestSearchGal {
    
    private static final String GAL_SYNC_ACCOUNT_NAME = "galsync";
    
    private static final String DOMAIN_LDAP = "ldap.galtest";
    private static final String DOMAIN_GSA = "gsa.galtest";
    private static final String AUTHED_USER = "user1";
    
    private static final String KEY_FOR_SEARCH_BY_NAME = "account";
    private static final String ACCOUNT_PREFIX = "account";
    
    private static final int NUM_ACCOUNTS = 10; 
    
    
    /*
    static void authAdmin(SoapTransport transport, String acctName) throws Exception {
        
        Element request = Element.create(transport.getRequestProtocol(), AdminConstants.AUTH_REQUEST);
        request.addElement(AccountConstants.E_ACCOUNT).addAttribute(AccountConstants.A_BY, AccountBy.name.name()).setText(acctName);
        request.addElement(AccountConstants.E_PASSWORD).setText("test123");
        
        Element response = transport.invoke(request);
        String authToken = response.getElement(AccountConstants.E_AUTH_TOKEN).getText();
        transport.setAuthToken(authToken);
    }
    */
    
    static void authUser(SoapTransport transport, String acctName) throws Exception {
        
        Element request = Element.create(transport.getRequestProtocol(), AccountConstants.AUTH_REQUEST);
        request.addElement(AccountConstants.E_ACCOUNT).addAttribute(AccountConstants.A_BY, AccountBy.name.name()).setText(acctName);
        request.addElement(AccountConstants.E_PASSWORD).setText("test123");
        
        Element response = transport.invoke(request);
        String authToken = response.getElement(AccountConstants.E_AUTH_TOKEN).getText();
        transport.setAuthToken(authToken);
    }
    
    static void disableGalSyncAccount(String domainName) throws Exception {
        Provisioning prov = Provisioning.getInstance();
        
        Domain domain = prov.get(DomainBy.name, domainName);
        
        String[] galSyncAcctIds = domain.getGalAccountId();
        for (String galSyncAcctId : galSyncAcctIds) {
            prov.deleteAccount(galSyncAcctId);
        }
        
        domain.unsetGalAccountId();
    }
  
    static void enableGalSyncAccount(String domainName) throws Exception {
        Provisioning prov = Provisioning.getInstance();
        
        Domain domain = prov.get(DomainBy.name, domainName);
        String[] galSyncAcctIds = domain.getGalAccountId();
        if (galSyncAcctIds.length > 0) {
            // already enabled
            return;
        } else {
            createAndSyncGalSyncAccount(TestUtil.getAddress(GAL_SYNC_ACCOUNT_NAME, domainName), domainName);
        }

    }
    
    static void createAndSyncGalSyncAccount(String galSyncAcctName, String domainName) throws Exception {
        String dataSourceName = "zimbra";
        
        SoapTransport transport = TestUtil.getAdminSoapTransport();
        
        //
        // create gal sync account
        //
        Element eCreateReq = Element.create(transport.getRequestProtocol(), AdminConstants.CREATE_GAL_SYNC_ACCOUNT_REQUEST);
        
        eCreateReq.addAttribute(AdminConstants.E_NAME, dataSourceName);
        eCreateReq.addAttribute(AdminConstants.E_DOMAIN, domainName);
        eCreateReq.addAttribute(AdminConstants.A_TYPE, "zimbra");
        eCreateReq.addAttribute(AdminConstants.E_FOLDER, "zimbra-gal-contacts");
        
        Element eAccount = eCreateReq.addElement(AdminConstants.E_ACCOUNT);
        eAccount.addAttribute(AdminConstants.A_BY, AccountBy.name.name());
        eAccount.setText(galSyncAcctName);
        
        Element response = transport.invoke(eCreateReq);
        
        eAccount = response.getElement(AdminConstants.E_ACCOUNT);
        String name = eAccount.getAttribute(AccountConstants.A_NAME);
        String id = eAccount.getAttribute(AccountConstants.A_ID);
        Assert.assertEquals(galSyncAcctName, name);
        
        //
        // sync gal sync account
        //
        Element eSyncReq = Element.create(transport.getRequestProtocol(), AdminConstants.SYNC_GAL_ACCOUNT_REQUEST);
        
        eAccount = eSyncReq.addElement(AdminConstants.E_ACCOUNT);
        eAccount.addAttribute(AccountConstants.A_ID, id);
        
        Element eDataSource = eAccount.addElement(AdminConstants.E_DATASOURCE);
        eDataSource.addAttribute(AdminConstants.A_RESET, "TRUE");
        eDataSource.addAttribute(AdminConstants.A_BY, AccountBy.name.name());
        eDataSource.setText(dataSourceName);
        
        transport.invoke(eSyncReq);
        
        //
        // index the gal sync account (otherwise the first search will fail)
        //
        Element eReIndex = Element.create(transport.getRequestProtocol(), AdminConstants.REINDEX_REQUEST);
        eReIndex.addAttribute(AdminConstants.A_ACTION, "start");
        Element eMbox = eReIndex.addElement(AdminConstants.E_MAILBOX);
        eMbox.addAttribute(AdminConstants.A_ID, id);
        transport.invoke(eReIndex);
        
        // wait for the reindex to finish
        Thread.sleep(2000);
    }
    
    private void searchByName(boolean ldap, String domainName) throws Exception {
        SoapHttpTransport transport = new SoapHttpTransport(TestUtil.getSoapUrl());
        TestSearchGal.authUser(transport, TestUtil.getAddress(AUTHED_USER, domainName));
        
        Element request = Element.create(transport.getRequestProtocol(), AccountConstants.SEARCH_GAL_REQUEST);
        request.addElement(AccountConstants.E_NAME).setText(KEY_FOR_SEARCH_BY_NAME);
        
        Element response = transport.invoke(request);
        
        List<GalContact> result = new ArrayList<GalContact>();
        for (Element e: response.listElements(AdminConstants.E_CN)) {
            result.add(new GalContact(AdminConstants.A_ID, SoapProvisioning.getAttrs(e)));
        }
  
        Assert.assertEquals(NUM_ACCOUNTS, result.size());
        
    }
    
    private static void createDomainObjects(String domainName) throws Exception {
        Provisioning prov = Provisioning.getInstance();
        
        if (prov.get(DomainBy.name, domainName) == null) {
            ZimbraLog.test.info("Creating domain " + domainName);
            prov.createDomain(domainName, new HashMap<String, Object>());
        }
        
        if (prov.get(AccountBy.name, TestUtil.getAddress(AUTHED_USER, domainName)) == null) {
            prov.createAccount(TestUtil.getAddress(AUTHED_USER, domainName), "test123", null);
        }
        
        for (int i = 0; i < NUM_ACCOUNTS; i++) {
            String acctName = TestUtil.getAddress(ACCOUNT_PREFIX + "-" + i, domainName);
            if (prov.get(AccountBy.name, acctName) == null) {
                prov.createAccount(acctName, "test123", null);
            }
        }
    }
    
    private static void deleteDomainObjects(String domainName) throws Exception {
        Provisioning prov = Provisioning.getInstance();
        
        TestSearchGal.disableGalSyncAccount(domainName);
        
        Account acct = prov.get(AccountBy.name, TestUtil.getAddress(AUTHED_USER, domainName));
        if (acct != null) {
            prov.deleteAccount(acct.getId());
        }
        
        for (int i = 0; i < NUM_ACCOUNTS; i++) {
            String acctName = TestUtil.getAddress(ACCOUNT_PREFIX + "-" + i, domainName);
            acct = prov.get(AccountBy.name, acctName);
            if (acct != null) {
                prov.deleteAccount(acct.getId());
            }
        }
        
        Domain domain = prov.get(DomainBy.name, domainName);
        if (domain != null) {
            ZimbraLog.test.info("Deleting domain " + domainName);
            prov.deleteDomain(domain.getId());
        }
    }
    
    @BeforeClass
    public static void init() throws Exception {
        TestUtil.cliSetup();
        
        createDomainObjects(DOMAIN_LDAP);
        createDomainObjects(DOMAIN_GSA);
    }
    
    @AfterClass 
    public static void cleanup() throws Exception {
        deleteDomainObjects(DOMAIN_LDAP);
        deleteDomainObjects(DOMAIN_GSA);
    }
    
    /*
    @Test
    public void testGalSyncAccountSerarhByName() throws Exception {
        TestSearchGal.enableGalSyncAccount(DOMAIN_GSA);
        searchByName(false, DOMAIN_GSA);
    }
    */
    
    /*
    @Test
    public void testGalSyncAccountSerarhByFilter() throws Exception {
        TestSearchGal.enableGalSyncAccount(DOMAIN_GSA);
        searchByFilter(false, DOMAIN_GSA);
    }
    */
    
    @Test
    public void testLdapSerarhByName() throws Exception {
        searchByName(true, DOMAIN_LDAP);
    }
    
    /*
    @Test
    public void testLdapSerarhByFilter() throws Exception {
        searchByFilter(true, DOMAIN_LDAP);
    }
    */
}
