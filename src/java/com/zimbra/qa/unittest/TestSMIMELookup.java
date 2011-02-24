package com.zimbra.qa.unittest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.CliUtil;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.account.Provisioning.DomainBy;
import com.zimbra.cs.account.ZAttrProvisioning.GalMode;
import com.zimbra.cs.account.ldap.LdapSMIMEConfig;
import com.zimbra.cs.account.ldap.LdapUtil;
import com.zimbra.qa.unittest.TestLdapBinary.Content;


/*
TODO: Add this class to {@link ZimbraSuite} once it supports JUnit 4 annotations.
*/
public class TestSMIMELookup {
    
    private static final String DOMAIN = "domain.smimelookup";
    private static final String USER = "user";
    private static final String CONTACT = "contact";
    private static final String SMIME_ATTR = Provisioning.A_zimbraPrefMailSMIMECertificate;
    private static final int NUM_BYTES_IN_CERT = 100;
    private static final Content CERT_1 = Content.generateContent(NUM_BYTES_IN_CERT);
    private static final Content CERT_2 = Content.generateContent(NUM_BYTES_IN_CERT);
    
    private static Domain domain;
    private static Account account;
    private static Provisioning prov;
    
    private static abstract class SMIMEConfig {
        Map<String, Object> attrs = new HashMap<String, Object>();
        
        String getName() {
            return this.getClass().getSimpleName();
        }
        
        Map<String, Object> getConfig() {
            return attrs;
        }
        
        protected void assertEqualsPrefMailSMIMECertificate(List<String> certs) {
            Assert.assertEquals(2, certs.size());
            assertContainsPrefMailSMIMECertificate(certs);
        }
        
        protected void assertContainsPrefMailSMIMECertificate(List<String> certs) {
            boolean foundCert1 = false;
            boolean foundCert2 = false;
            
            for (String cert : certs) {
                if (CERT_1.equals(cert)) {
                    foundCert1 = true;
                }
                
                if (CERT_2.equals(cert)) {
                    foundCert2 = true;
                }
            }
            
            Assert.assertTrue(foundCert1);
            Assert.assertTrue(foundCert2);
        }
        
        // asserts certs contains all certs that should be found by the config, no more no less
        abstract void assertEquals(List<String> certs) throws Exception;
        
        // asserts certs contains all certs that should be found by the config, there could be more 
        // values in certs (that are returned from other configs) than those that should be found by this config
        abstract void assertContains(List<String> certs) throws Exception;
    }
    
    private static class EmailMatch extends SMIMEConfig {
        private EmailMatch() {
            attrs.put(Provisioning.A_zimbraSMIMELdapURL, "ldap://localhost:389");
            attrs.put(Provisioning.A_zimbraSMIMELdapBindDn, "cn=config");
            attrs.put(Provisioning.A_zimbraSMIMELdapBindPassword, "zimbra");
            attrs.put(Provisioning.A_zimbraSMIMELdapSearchBase, "");
            attrs.put(Provisioning.A_zimbraSMIMELdapFilter, "(mail=%n)");
            attrs.put(Provisioning.A_zimbraSMIMELdapAttribute, Provisioning.A_zimbraPrefMailSMIMECertificate);
        }
        
        @Override
        void assertEquals(List<String> certs) throws Exception {
            assertEqualsPrefMailSMIMECertificate(certs);
        }
        
        @Override
        void assertContains(List<String> certs) throws Exception {
            assertContainsPrefMailSMIMECertificate(certs);
        }
    }
    
    private static class LocalpartMatch extends SMIMEConfig {
        private LocalpartMatch() {
            attrs.put(Provisioning.A_zimbraSMIMELdapURL, "ldap://localhost:389");
            attrs.put(Provisioning.A_zimbraSMIMELdapBindDn, "cn=config");
            attrs.put(Provisioning.A_zimbraSMIMELdapBindPassword, "zimbra");
            attrs.put(Provisioning.A_zimbraSMIMELdapSearchBase, "");
            attrs.put(Provisioning.A_zimbraSMIMELdapFilter, "(uid=%u)");
            attrs.put(Provisioning.A_zimbraSMIMELdapAttribute, Provisioning.A_zimbraPrefMailSMIMECertificate);
        }
        
        @Override
        void assertEquals(List<String> certs) throws Exception {
            assertEqualsPrefMailSMIMECertificate(certs);
        }
        
        @Override
        void assertContains(List<String> certs) throws Exception {
            assertContainsPrefMailSMIMECertificate(certs);
        }
    }
    
    private static class OmitSearchBase extends SMIMEConfig {
        private OmitSearchBase() {
            attrs.put(Provisioning.A_zimbraSMIMELdapURL, "ldap://localhost:389");
            attrs.put(Provisioning.A_zimbraSMIMELdapBindDn, "cn=config");
            attrs.put(Provisioning.A_zimbraSMIMELdapBindPassword, "zimbra");
            // attrs.put(Provisioning.A_zimbraSMIMELdapSearchBase, "");  // should default to ""
            attrs.put(Provisioning.A_zimbraSMIMELdapFilter, "(uid=%u)");
            attrs.put(Provisioning.A_zimbraSMIMELdapAttribute, Provisioning.A_zimbraPrefMailSMIMECertificate);
        }
        
        @Override
        void assertEquals(List<String> certs) throws Exception {
            assertEqualsPrefMailSMIMECertificate(certs);
        }
        
        @Override
        void assertContains(List<String> certs) throws Exception {
            assertContainsPrefMailSMIMECertificate(certs);
        }
    }
    
    private static class AnonBind extends SMIMEConfig {
        private AnonBind() {
            attrs.put(Provisioning.A_zimbraSMIMELdapURL, "ldap://localhost:389");
            attrs.put(Provisioning.A_zimbraSMIMELdapSearchBase, "");
            attrs.put(Provisioning.A_zimbraSMIMELdapFilter, "(uid=%u)");  
            attrs.put(Provisioning.A_zimbraSMIMELdapAttribute, Provisioning.A_mail); // // Zimbra LDAP allow mail to be read by anon bind
        }
        
        @Override
        void assertEquals(List<String> certs) throws Exception {
            Assert.assertEquals(1, certs.size());
            assertContains(certs);
        }
        
        @Override
        void assertContains(List<String> certs) throws Exception {
            String expected = ByteUtil.encodeLDAPBase64(TestUtil.getAddress(CONTACT, DOMAIN).getBytes());
            Assert.assertEquals(expected, certs.get(0));
        }
    }
    
    private static class MultipleAttrs extends SMIMEConfig {
        private MultipleAttrs() {
            attrs.put(Provisioning.A_zimbraSMIMELdapURL, "ldap://localhost:389");
            attrs.put(Provisioning.A_zimbraSMIMELdapBindDn, "cn=config");
            attrs.put(Provisioning.A_zimbraSMIMELdapBindPassword, "zimbra");
            attrs.put(Provisioning.A_zimbraSMIMELdapSearchBase, "");
            attrs.put(Provisioning.A_zimbraSMIMELdapFilter, "(mail=%n)");
            attrs.put(Provisioning.A_zimbraSMIMELdapAttribute, 
                    Provisioning.A_cn + "," + Provisioning.A_uid + "," + Provisioning.A_mail);
        }
        
        @Override
        void assertEquals(List<String> certs) throws Exception {
            Assert.assertEquals(3, certs.size());
            assertContains(certs);
        }
        
        @Override
        void assertContains(List<String> certs) throws Exception {
            boolean foundCn = false;
            boolean foundUid = false;
            boolean foundMail = false;
            
            String expectedCn = ByteUtil.encodeLDAPBase64(CONTACT.getBytes());
            String expectedUid = ByteUtil.encodeLDAPBase64(CONTACT.getBytes());
            String expectedMail = ByteUtil.encodeLDAPBase64(TestUtil.getAddress(CONTACT, DOMAIN).getBytes());
            
            for (String cert : certs) {
                if (expectedCn.equals(cert)) {
                    foundCn = true;
                }
                if (expectedUid.equals(cert)) {
                    foundUid = true;
                }
                if (expectedMail.equals(cert)) {
                    foundMail = true;
                }
            }
            
            Assert.assertTrue(foundCn);
            Assert.assertTrue(foundUid);
            Assert.assertTrue(foundMail);
        }
    }

    
    @Before
    public void removeAllSMIMEConfigs() throws Exception {
        Map<String, Map<String, Object>> smimeConfigs;

        smimeConfigs = prov.getDomainSMIMEConfig(domain, null);
        for (String configName : smimeConfigs.keySet()) {
            prov.removeDomainSMIMEConfig(domain, configName);
        }
        
        smimeConfigs = prov.getConfigSMIMEConfig(null);
        for (String configName : smimeConfigs.keySet()) {
            prov.removeConfigSMIMEConfig(configName);
        }
    }
    
    @Test
    public void testEmailMatch() throws Exception {
        EmailMatch smimeConfig = new EmailMatch();
        prov.modifyDomainSMIMEConfig(domain, smimeConfig.getName(), smimeConfig.getConfig());
        
        List<String> certs = LdapSMIMEConfig.lookupPublicKeys(account, TestUtil.getAddress(CONTACT, DOMAIN));
        smimeConfig.assertEquals(certs);
    }
    
    @Test
    public void testLocalpartMatch() throws Exception {
        LocalpartMatch smimeConfig = new LocalpartMatch();
        prov.modifyDomainSMIMEConfig(domain, smimeConfig.getName(), smimeConfig.getConfig());
        
        List<String> certs = LdapSMIMEConfig.lookupPublicKeys(account, CONTACT);
        smimeConfig.assertEquals(certs);
    }
    
    @Test
    public void testOmitSearchBase() throws Exception {
        OmitSearchBase smimeConfig = new OmitSearchBase();
        prov.modifyDomainSMIMEConfig(domain, smimeConfig.getName(), smimeConfig.getConfig());
        
        List<String> certs = LdapSMIMEConfig.lookupPublicKeys(account, CONTACT);
        smimeConfig.assertEquals(certs);
    }
    
    @Test
    public void testAnonBind() throws Exception {
        AnonBind smimeConfig = new AnonBind();
        prov.modifyDomainSMIMEConfig(domain, smimeConfig.getName(), smimeConfig.getConfig());
        
        List<String> certs = LdapSMIMEConfig.lookupPublicKeys(account, CONTACT);
        smimeConfig.assertEquals(certs);
    }
    
    @Test
    public void testFallbackToGlobalConfig() throws Exception {
        EmailMatch smimeConfig = new EmailMatch();
        prov.modifyConfigSMIMEConfig(smimeConfig.getName(), smimeConfig.getConfig());
        
        List<String> certs = LdapSMIMEConfig.lookupPublicKeys(account, TestUtil.getAddress(CONTACT, DOMAIN));
        smimeConfig.assertEquals(certs);
    }
    
    @Test
    public void testMultipleAttrs() throws Exception {
        MultipleAttrs smimeConfig = new MultipleAttrs();
        prov.modifyDomainSMIMEConfig(domain, smimeConfig.getName(), smimeConfig.getConfig());
        
        List<String> certs = LdapSMIMEConfig.lookupPublicKeys(account, TestUtil.getAddress(CONTACT, DOMAIN));
        smimeConfig.assertEquals(certs);
    }
    
    @Test
    public void testMultipleSMIMEConfigs() throws Exception {
        EmailMatch smimeConfig1 = new EmailMatch();
        prov.modifyDomainSMIMEConfig(domain, smimeConfig1.getName(), smimeConfig1.getConfig());
        
        MultipleAttrs smimeConfig2 = new MultipleAttrs();
        prov.modifyDomainSMIMEConfig(domain, smimeConfig2.getName(), smimeConfig2.getConfig());
        
        List<String> certs = LdapSMIMEConfig.lookupPublicKeys(account, TestUtil.getAddress(CONTACT, DOMAIN));
        smimeConfig1.assertContains(certs);
        smimeConfig2.assertContains(certs);
    }
    
    @Test
    public void testNoSMIMEConfigs() throws Exception {
        boolean caughtException = false;
        
        try {
            List<String> certs = LdapSMIMEConfig.lookupPublicKeys(account, TestUtil.getAddress(CONTACT, DOMAIN));
        } catch (AccountServiceException e) {
            if (AccountServiceException.NO_SMIME_CONFIG.equals(e.getCode())) {
                caughtException = true;
            }
        }
        
        Assert.assertTrue(caughtException);
    }
    
    @BeforeClass
    public static void init() throws Exception {
        CliUtil.toolSetup();
        
        prov = Provisioning.getInstance();

        // create the domain
        domain = prov.get(DomainBy.name, DOMAIN);
        if (domain == null) {
            ZimbraLog.test.info("Creating domain " + DOMAIN);
            domain = prov.createDomain(DOMAIN, new HashMap<String, Object>());
        }
        
        // create the test user
        String userAddr = TestUtil.getAddress(USER, DOMAIN);
        account = prov.get(AccountBy.name, userAddr);
        if (account == null) {
            account = prov.createAccount(userAddr, "test123", null);
        }
        
        // create the contact user
        String contactAddr = TestUtil.getAddress(CONTACT, DOMAIN);
        Account contact = prov.get(AccountBy.name, contactAddr);
        if (contact == null) {
            Map<String, Object> attrs = new HashMap<String, Object>();
            StringUtil.addToMultiMap(attrs, SMIME_ATTR, CERT_1.getString());
            StringUtil.addToMultiMap(attrs, SMIME_ATTR, CERT_2.getString());
            contact = prov.createAccount(contactAddr, "test123", attrs);
        }
    }
    
    @AfterClass 
    public static void cleanup() throws Exception {
        String userAddr = TestUtil.getAddress(USER, DOMAIN);
        account = prov.get(AccountBy.name, userAddr);
        if (account != null) {
            prov.deleteAccount(account.getId());
        }
        
        String contactAddr = TestUtil.getAddress(CONTACT, DOMAIN);
        Account contact = prov.get(AccountBy.name, contactAddr);
        if (contact != null) {
            prov.deleteAccount(contact.getId());
        }
        
        Domain domain = prov.get(DomainBy.name, DOMAIN);
        if (domain != null) {
            ZimbraLog.test.info("Deleting domain " + DOMAIN);
            prov.deleteDomain(domain.getId());
        }
    }
}
