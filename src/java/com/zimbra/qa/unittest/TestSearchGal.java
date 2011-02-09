package com.zimbra.qa.unittest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.common.mailbox.ContactConstants;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.SoapHttpTransport;
import com.zimbra.common.soap.SoapTransport;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.CalendarResource;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.GalContact;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.EntrySearchFilter.Operator;
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
    
    private static final String DOMAIN_LDAP = "ldap.searchgaltest";
    private static final String DOMAIN_GSA = "gsa.searchgaltest";
    private static final String AUTHED_USER = "user1";
    
    private static final String KEY_FOR_SEARCH_BY_NAME = "account";
    private static final String ACCOUNT_PREFIX = "account";
    private static final String DEPARTMENT_PREFIX = "engineering";
    
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
    
    // find all
    private void searchWithDot(boolean ldap, String domainName) throws Exception {
        SoapHttpTransport transport = new SoapHttpTransport(TestUtil.getSoapUrl());
        TestSearchGal.authUser(transport, TestUtil.getAddress(AUTHED_USER, domainName));
        
        Element request = Element.create(transport.getRequestProtocol(), AccountConstants.SEARCH_GAL_REQUEST);
        request.addElement(AccountConstants.E_NAME).setText(".");
        
        Element response = transport.invoke(request);
        
        boolean paginationSupported = response.getAttributeBool(AccountConstants.A_PAGINATION_SUPPORTED);
        
        List<GalContact> result = new ArrayList<GalContact>();
        for (Element e: response.listElements(AdminConstants.E_CN)) {
            result.add(new GalContact(AdminConstants.A_ID, SoapProvisioning.getAttrs(e)));
        }
  
        if (ldap) {
            // pagination is not supported
            Assert.assertFalse(paginationSupported);
        } else {
            // pagination is supported
            Assert.assertTrue(paginationSupported);
        }
        
        // should find all account, plus the authed user
        Assert.assertEquals(NUM_ACCOUNTS + 1, result.size());
        
    }
    
    private void searchWithOffsetLimit(boolean ldap, String domainName) throws Exception {
        SoapHttpTransport transport = new SoapHttpTransport(TestUtil.getSoapUrl());
        TestSearchGal.authUser(transport, TestUtil.getAddress(AUTHED_USER, domainName));
        
        Element request = Element.create(transport.getRequestProtocol(), AccountConstants.SEARCH_GAL_REQUEST);
        request.addElement(AccountConstants.E_NAME).setText(".");
        
        int offset = 5;
        int limit = 3;
        request.addAttribute(MailConstants.A_QUERY_OFFSET, offset);
        request.addAttribute(MailConstants.A_QUERY_LIMIT, limit);
        request.addAttribute(MailConstants.A_SORTBY, "nameAsc");
        
        Element response = transport.invoke(request);
        
        boolean paginationSupported = response.getAttributeBool(AccountConstants.A_PAGINATION_SUPPORTED);
        
        List<GalContact> result = new ArrayList<GalContact>();
        for (Element e: response.listElements(AdminConstants.E_CN)) {
            result.add(new GalContact(AdminConstants.A_ID, SoapProvisioning.getAttrs(e)));
        }
  
        if (ldap) {
            // pagination is not supported
            Assert.assertFalse(paginationSupported);
            
            // limit is ignored, ldap search is limited by zimbraGalMaxResults
            // should find all account, plus the authed user
            Assert.assertEquals(NUM_ACCOUNTS + 1, result.size());
        } else {
            // pagination is supported
            Assert.assertTrue(paginationSupported);
            
            Assert.assertEquals(limit, result.size());
            for (int i = 0; i < limit; i++) {
                Assert.assertEquals(getEmail(offset + i, domainName), result.get(i).getSingleAttr(ContactConstants.A_email));
            }
        }
        
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
    
    private void searchByFilter(boolean ldap, String domainName) throws Exception {
        SoapHttpTransport transport = new SoapHttpTransport(TestUtil.getSoapUrl());
        TestSearchGal.authUser(transport, TestUtil.getAddress(AUTHED_USER, domainName));
        
        Element request = Element.create(transport.getRequestProtocol(), AccountConstants.SEARCH_GAL_REQUEST);
        request.addElement(AccountConstants.E_NAME).setText(".");

        
        Element eSearchFilter = request.addElement(AccountConstants.E_ENTRY_SEARCH_FILTER);
        Element eConds = eSearchFilter.addElement(AccountConstants.E_ENTRY_SEARCH_FILTER_MULTICOND);
        
        int acctToMatch = 8;
        Element eCondResType = eConds.addElement(AccountConstants.E_ENTRY_SEARCH_FILTER_SINGLECOND);
        eCondResType.addAttribute(AccountConstants.A_ENTRY_SEARCH_FILTER_ATTR, ContactConstants.A_email);
        eCondResType.addAttribute(AccountConstants.A_ENTRY_SEARCH_FILTER_OP, Operator.has.name());
        eCondResType.addAttribute(AccountConstants.A_ENTRY_SEARCH_FILTER_VALUE, acctToMatch);
        
        String matchDepartment = getDepartment(acctToMatch, domainName);
        Element eCondResSite = eConds.addElement(AccountConstants.E_ENTRY_SEARCH_FILTER_SINGLECOND);
        eCondResSite.addAttribute(AccountConstants.A_ENTRY_SEARCH_FILTER_ATTR, ContactConstants.A_department);
        eCondResSite.addAttribute(AccountConstants.A_ENTRY_SEARCH_FILTER_OP, Operator.eq.name());
        eCondResSite.addAttribute(AccountConstants.A_ENTRY_SEARCH_FILTER_VALUE, matchDepartment);
        
        Element response = transport.invoke(request);

        List<GalContact> result = new ArrayList<GalContact>();
        for (Element e: response.listElements(AdminConstants.E_CN)) {
            result.add(new GalContact(AdminConstants.A_ID, SoapProvisioning.getAttrs(e)));
        }
  
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(getEmail(acctToMatch, domainName), result.get(0).getSingleAttr(ContactConstants.A_email));
    }

    
    private static String getEmail(int index, String domainName) {
        return TestUtil.getAddress(ACCOUNT_PREFIX + "-" + index, domainName);
    }
    
    private static String getDepartment(int index, String domainName) {
        return TestUtil.getAddress(DEPARTMENT_PREFIX + "-" + index, domainName);
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
            String acctName = getEmail(i, domainName);
            if (prov.get(AccountBy.name, acctName) == null) {
                Map<String, Object> attrs = new HashMap<String, Object>();
                attrs.put(Provisioning.A_ou, getDepartment(i, domainName));
                prov.createAccount(acctName, "test123", attrs);
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
            String acctName = getEmail(i, domainName);
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
        
        Provisioning prov = Provisioning.getInstance();
        Config config = prov.getConfig();
        Map<String, Object> attrs = new HashMap<String, Object>();
        config.removeGalLdapFilterDef("department_eq:(ou=%s)", attrs);
        config.removeGalLdapFilterDef("email_has:(mail=*%s*)", attrs);
        prov.modifyAttrs(config, attrs);
        
    }
    
    @Test
    public void testGSASerarhWithDot() throws Exception {
        TestSearchGal.enableGalSyncAccount(DOMAIN_GSA);
        searchWithDot(false, DOMAIN_GSA);
    }
    
    @Test
    public void testGSASerarhWithOffsetlimit() throws Exception {
        TestSearchGal.enableGalSyncAccount(DOMAIN_GSA);
        searchWithOffsetLimit(false, DOMAIN_GSA);
    }
    
    @Test
    public void testGSASerarhByName() throws Exception {
        TestSearchGal.enableGalSyncAccount(DOMAIN_GSA);
        searchByName(false, DOMAIN_GSA);
    }
    
    @Test
    public void testGalSyncAccountSerarhByFilter() throws Exception {
        TestSearchGal.enableGalSyncAccount(DOMAIN_GSA);
        searchByFilter(false, DOMAIN_GSA);
    }

    @Test
    public void testLdapSerarhWithDot() throws Exception {
        searchWithDot(true, DOMAIN_LDAP);
    }
    
    @Test
    public void testLdapSerarhWithOffsetlimit() throws Exception {
        searchWithOffsetLimit(true, DOMAIN_LDAP);
    }
    
    @Test
    public void testLdapSerarhByName() throws Exception {
        searchByName(true, DOMAIN_LDAP);
    }
    
    @Test
    public void testLdapSerarhByFilter() throws Exception {
        /*
         *   <globalConfigValue>department_eq:(ou=%s)</globalConfigValue>
         *   <globalConfigValue>email_has:(mail=*%s*)</globalConfigValue>
         */
        Provisioning prov = Provisioning.getInstance();
        Config config = prov.getConfig();
        Map<String, Object> attrs = new HashMap<String, Object>();
        config.addGalLdapFilterDef("department_eq:(ou=%s)", attrs);
        config.addGalLdapFilterDef("email_has:(mail=*%s*)", attrs);
        prov.modifyAttrs(config, attrs);
        
        searchByFilter(true, DOMAIN_LDAP);
    }

}
