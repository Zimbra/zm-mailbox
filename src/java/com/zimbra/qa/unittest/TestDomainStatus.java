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

package com.zimbra.qa.unittest;

import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;



import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.SoapFaultException;
import com.zimbra.common.soap.Element.XMLElement;
import com.zimbra.common.util.CliUtil;
import com.zimbra.common.util.DateUtil;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.account.ldap.LdapProvisioning;
import com.zimbra.cs.account.soap.SoapProvisioning;
import com.zimbra.cs.lmtpserver.LmtpProtocolException;
import com.zimbra.cs.service.account.Auth;
import com.zimbra.cs.service.account.AutoCompleteGal;
import com.zimbra.cs.service.account.ChangePassword;
import com.zimbra.cs.service.account.CreateIdentity;
import com.zimbra.cs.service.account.CreateSignature;
import com.zimbra.cs.service.account.DeleteIdentity;
import com.zimbra.cs.service.account.DeleteSignature;
import com.zimbra.cs.service.account.GetAccountInfo;
import com.zimbra.cs.service.account.GetAllLocales;
import com.zimbra.cs.service.account.GetAvailableLocales;
import com.zimbra.cs.service.account.GetAvailableSkins;
import com.zimbra.cs.service.account.GetIdentities;
import com.zimbra.cs.service.account.GetInfo;
import com.zimbra.cs.service.account.GetPrefs;
import com.zimbra.cs.service.account.GetSignatures;
import com.zimbra.cs.service.account.ModifyIdentity;
import com.zimbra.cs.service.account.ModifyPrefs;
import com.zimbra.cs.service.account.ModifyProperties;
import com.zimbra.cs.service.account.ModifySignature;
import com.zimbra.cs.service.account.SearchCalendarResources;
import com.zimbra.cs.service.account.SearchGal;
import com.zimbra.cs.service.account.SyncGal;
import com.zimbra.cs.zclient.ZClientException;


import junit.framework.AssertionFailedError;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class TestDomainStatus extends TestCase {
    private Provisioning mProv;
    private SoapProvisioning mSoapProv;
    private SoapProvisioning mSoapProvAdmin;
    
    private String TEST_ID;
    private static String TEST_NAME = "test-domainstatus";
    
    private static String PASSWORD = "test123";
    private static String NAMEPREFIX_ACCOUNT     = "acct-";
    private String DOMAIN_NAME;
    
    private static final String NAME_PREFIX = TestLmtp.class.getSimpleName();
    private Account[] mAccts;
    
    private static HashMap<String, Map<String, String>> sStatusMap;
    
    static {
        sStatusMap = new HashMap<String, Map<String, String>>();
        
        Map<String, String> domainActive = new HashMap<String, String>();
        domainActive.put(Provisioning.ACCOUNT_STATUS_ACTIVE, Provisioning.ACCOUNT_STATUS_ACTIVE);
        domainActive.put(Provisioning.ACCOUNT_STATUS_LOCKOUT, Provisioning.ACCOUNT_STATUS_LOCKOUT);
        domainActive.put(Provisioning.ACCOUNT_STATUS_LOCKED, Provisioning.ACCOUNT_STATUS_LOCKED);
        domainActive.put(Provisioning.ACCOUNT_STATUS_MAINTENANCE, Provisioning.ACCOUNT_STATUS_MAINTENANCE);
        domainActive.put(Provisioning.ACCOUNT_STATUS_CLOSED, Provisioning.ACCOUNT_STATUS_CLOSED);
        
        Map<String, String> domainLocked = new HashMap<String, String>();
        domainLocked.put(Provisioning.ACCOUNT_STATUS_ACTIVE, Provisioning.ACCOUNT_STATUS_LOCKED);
        domainLocked.put(Provisioning.ACCOUNT_STATUS_LOCKOUT, Provisioning.ACCOUNT_STATUS_LOCKED);
        domainLocked.put(Provisioning.ACCOUNT_STATUS_LOCKED, Provisioning.ACCOUNT_STATUS_LOCKED);
        domainLocked.put(Provisioning.ACCOUNT_STATUS_MAINTENANCE, Provisioning.ACCOUNT_STATUS_MAINTENANCE);
        domainLocked.put(Provisioning.ACCOUNT_STATUS_CLOSED, Provisioning.ACCOUNT_STATUS_CLOSED);
        
        Map<String, String> domainMaintenance = new HashMap<String, String>();
        domainMaintenance.put(Provisioning.ACCOUNT_STATUS_ACTIVE, Provisioning.ACCOUNT_STATUS_MAINTENANCE);
        domainMaintenance.put(Provisioning.ACCOUNT_STATUS_LOCKOUT, Provisioning.ACCOUNT_STATUS_MAINTENANCE);
        domainMaintenance.put(Provisioning.ACCOUNT_STATUS_LOCKED, Provisioning.ACCOUNT_STATUS_MAINTENANCE);
        domainMaintenance.put(Provisioning.ACCOUNT_STATUS_MAINTENANCE, Provisioning.ACCOUNT_STATUS_MAINTENANCE);
        domainMaintenance.put(Provisioning.ACCOUNT_STATUS_CLOSED, Provisioning.ACCOUNT_STATUS_CLOSED);
        
        Map<String, String> domainSuspended = new HashMap<String, String>();
        domainSuspended.put(Provisioning.ACCOUNT_STATUS_ACTIVE, Provisioning.ACCOUNT_STATUS_MAINTENANCE);
        domainSuspended.put(Provisioning.ACCOUNT_STATUS_LOCKOUT, Provisioning.ACCOUNT_STATUS_MAINTENANCE);
        domainSuspended.put(Provisioning.ACCOUNT_STATUS_LOCKED, Provisioning.ACCOUNT_STATUS_MAINTENANCE);
        domainSuspended.put(Provisioning.ACCOUNT_STATUS_MAINTENANCE, Provisioning.ACCOUNT_STATUS_MAINTENANCE);
        domainSuspended.put(Provisioning.ACCOUNT_STATUS_CLOSED, Provisioning.ACCOUNT_STATUS_CLOSED);
        
        Map<String, String> domainClosed = new HashMap<String, String>();
        domainClosed.put(Provisioning.ACCOUNT_STATUS_ACTIVE, Provisioning.ACCOUNT_STATUS_CLOSED);
        domainClosed.put(Provisioning.ACCOUNT_STATUS_LOCKOUT, Provisioning.ACCOUNT_STATUS_CLOSED);
        domainClosed.put(Provisioning.ACCOUNT_STATUS_LOCKED, Provisioning.ACCOUNT_STATUS_CLOSED);
        domainClosed.put(Provisioning.ACCOUNT_STATUS_MAINTENANCE, Provisioning.ACCOUNT_STATUS_CLOSED);
        domainClosed.put(Provisioning.ACCOUNT_STATUS_CLOSED, Provisioning.ACCOUNT_STATUS_CLOSED);
        
        sStatusMap.put(Provisioning.DOMAIN_STATUS_ACTIVE, domainActive);
        sStatusMap.put(Provisioning.DOMAIN_STATUS_LOCKED, domainLocked);
        sStatusMap.put(Provisioning.DOMAIN_STATUS_MAINTENANCE, domainMaintenance);
        sStatusMap.put(Provisioning.DOMAIN_STATUS_SUSPENDED, domainSuspended);
        sStatusMap.put(Provisioning.DOMAIN_STATUS_CLOSED, domainClosed);
        
    }
    
    private static enum DomainStatus {
        // must match ldap value
        EMPTY, // pseudo value for testing, meaning no value in zimbraDomainStatus
        active,
        locked,
        maintenance,
        suspended,
        closed;
    }
    
    private static enum AccountStatus {
        // must match ldap value
        active,
        lockout,
        locked,
        maintenance,
        closed;
    }
    
    private static enum AccountType {
        ACCT_USER,
        ACCT_DOMAIN_ADMIN,
        ACCT_GLOBAL_ADMIN
    }
    
    public void setUp() throws Exception {

        TEST_ID = TestProvisioningUtil.genTestId();
        
        System.out.println("\nTest " + TEST_ID + " setting up...\n");
        
        mSoapProv = new SoapProvisioning();
        mSoapProv.soapSetURI(TestUtil.getSoapUrl());
        
        
        mSoapProvAdmin = new SoapProvisioning();
        mSoapProvAdmin.soapSetURI(TestUtil.getAdminSoapUrl());
        // mSoapProvAdmin.soapSetURI("https://phoebe.local:7071/service/admin/soap/");
        // mSoapProvAdmin.soapSetURI("https://localhost:7071/service/admin/soap/");
        mSoapProvAdmin.soapZimbraAdminAuthenticate();
        
        // mProv = Provisioning.getInstance();
        // assertTrue(mProv instanceof LdapProvisioning);
        mProv = mSoapProvAdmin;
        
        
        DOMAIN_NAME = TestProvisioningUtil.baseDomainName(TEST_NAME, TEST_ID);
        
        createDomain(DOMAIN_NAME);
        mAccts = new Account[AccountStatus.values().length];
        
        for (AccountStatus as : AccountStatus.values()) {
            Account acct = createAccount(ACCOUNT_NAME(as), AccountType.ACCT_USER);
            mAccts[as.ordinal()] = acct;
            mProv.modifyAccountStatus(acct, as.name());
            if (as == AccountStatus.lockout)
                lockoutAccount(acct, true);
        }
    }
    
    private String ACCOUNT_NAME(AccountStatus as) {
        String localPart = NAMEPREFIX_ACCOUNT + as.name();
        return ACCOUNT_NAME(localPart);
    }
    
    private String ACCOUNT_NAME(String localPart) {
        return localPart + "@" + DOMAIN_NAME; 
    }

    private void createDomain(String domainName) throws Exception {
        Map<String, Object> attrs = new HashMap<String, Object>();
        Domain domain = mProv.createDomain(domainName, attrs);
        assertNotNull(domain);
    }
    
    private Account createAccount(String accountEmail, AccountType at) throws Exception {
        Map<String, Object> acctAttrs = new HashMap<String, Object>();
        
        if (at == AccountType.ACCT_DOMAIN_ADMIN)
            acctAttrs.put(Provisioning.A_zimbraIsDomainAdminAccount, Provisioning.TRUE);
        else if (at == AccountType.ACCT_GLOBAL_ADMIN)
            acctAttrs.put(Provisioning.A_zimbraIsAdminAccount, Provisioning.TRUE);
            
        Account acct = mProv.createAccount(accountEmail, PASSWORD, acctAttrs);
        assertNotNull(acct);
        return acct;
    }
    
    private Domain getDomain() throws Exception {
        Domain domain = mProv.get(Provisioning.DomainBy.name, DOMAIN_NAME);
        assertNotNull(domain);
        return domain;
    }
    
    private Account getAccount(AccountStatus as) throws Exception {
        /*
        Account acct = mProv.get(Provisioning.AccountBy.name, ACCOUNT_NAME(as));
        assertNotNull(acct);
        return acct;
        */
        return mAccts[as.ordinal()];
    }
    
    private String expectedAccountStatus(String domainStatus, String acctStatus) throws Exception {
        return sStatusMap.get(domainStatus).get(acctStatus);
    }
    
    private void lockoutAccount(Account acct, boolean set) throws Exception {
        Map<String, Object> attrs = new HashMap<String, Object>();
        if (set) {
            attrs.put(Provisioning.A_zimbraPasswordLockoutMaxFailures, "1");
            attrs.put(Provisioning.A_zimbraPasswordLockoutEnabled, Provisioning.TRUE);
            attrs.put(Provisioning.A_zimbraPasswordLockoutLockedTime, DateUtil.toGeneralizedTime(new Date()));
            mProv.modifyAttrs(acct, attrs);
        } else {
            attrs.put(Provisioning.A_zimbraPasswordLockoutMaxFailures, "");
            attrs.put("-" + Provisioning.A_zimbraPasswordLockoutEnabled, Provisioning.TRUE);
            attrs.put(Provisioning.A_zimbraPasswordLockoutLockedTime, "");
            mProv.modifyAttrs(acct, attrs);
        }
    }
    
    private void authTest(Account acct, String status) {
        
        boolean ok = false;
        
        try {
            // mSoapProv.authAccount(acct, PASSWORD, TEST_NAME);
            mProv.authAccount(acct, PASSWORD, TEST_NAME);
            
            if (status.equals(Provisioning.ACCOUNT_STATUS_ACTIVE))
                ok = true;
            
        } catch (ServiceException e) {
            if (status.equals(Provisioning.ACCOUNT_STATUS_ACTIVE))
                fail();
            else if (status.equals(Provisioning.ACCOUNT_STATUS_LOCKED)) {
                if (e.getCode().equals(AccountServiceException.AUTH_FAILED))
                    ok = true;
            } else if (status.equals(Provisioning.ACCOUNT_STATUS_LOCKOUT)) {
                if (e.getCode().equals(AccountServiceException.AUTH_FAILED))
                    ok = true;
            } else if (status.equals(Provisioning.ACCOUNT_STATUS_MAINTENANCE)) {
                if (e.getCode().equals(AccountServiceException.MAINTENANCE_MODE))
                    ok = true;
            } else if (status.equals(Provisioning.ACCOUNT_STATUS_CLOSED)) {
                if (e.getCode().equals(AccountServiceException.AUTH_FAILED))
                    ok = true;
            }  
        } 
        assertTrue(ok);
    }
    
    private void mailTest(Account acct, String status) throws Exception {
        
        boolean ok = false;
        
        try {
            TestUtil.insertMessageLmtp(1, NAME_PREFIX + " 1", acct.getName(), "phoebeshao");
            
            if (status.equals(Provisioning.ACCOUNT_STATUS_ACTIVE) ||
                status.equals(Provisioning.ACCOUNT_STATUS_LOCKOUT) ||
                status.equals(Provisioning.ACCOUNT_STATUS_LOCKED))
                ok = true;
        } catch (LmtpProtocolException e) {
            if (status.equals(Provisioning.ACCOUNT_STATUS_ACTIVE) ||
                    status.equals(Provisioning.ACCOUNT_STATUS_LOCKOUT) ||
                    status.equals(Provisioning.ACCOUNT_STATUS_LOCKED)) {
                fail();
            } else if (status.equals(Provisioning.ACCOUNT_STATUS_MAINTENANCE) || 
                       status.equals(Provisioning.ACCOUNT_STATUS_CLOSED)) {
                if (e.getMessage().equals("503 5.5.1 No recipients"))
                    ok = true;
            }
        }
        assertTrue(ok);
    }
    
    private void statusTest(Account acct, String domainStatus, String acctStatus) throws Exception {
        String status = expectedAccountStatus(domainStatus, acctStatus);
        System.out.println(domainStatus + " " + acctStatus + " => " +  status);
        
        authTest(acct, status);
        mailTest(acct, status);
        
    }
    
    private void statusTest(String domainStatus) throws Exception {
        for (AccountStatus as : AccountStatus.values()) {
            Account acct = getAccount(as); 
            statusTest(acct, domainStatus, as.name());
        }
    }
    
    private void assertCodeEquals(ServiceException e, String expectedCode) {
        assertEquals(e.getCode(), expectedCode);
    }
    
    private void statusTest() throws Exception {
        
        Domain domain = getDomain();
               
        for (DomainStatus ds : DomainStatus.values()) {
            String domainStatus = ds.name();
            if (ds == DomainStatus.EMPTY) {
                mProv.modifyDomainStatus(domain, "");
                domainStatus = DomainStatus.active.name();
            } else    
                mProv.modifyDomainStatus(domain, domainStatus);
            
            statusTest(domainStatus);
        }
    }
    
    static class AccountCommands {
        SoapTester mTester;
        AccountType mAuthedAcctType;
        private Account mTargetAcct;
        boolean mSuspended;
        
        AccountCommands(SoapTester tester, AccountType authedAcctType, 
                        Account targetAcct, boolean suspended) {
            mTester = tester;
            mAuthedAcctType = authedAcctType;
            mTargetAcct = targetAcct;
            mSuspended = suspended;
        }
        
        private String identityName() {
            return "identity-of-" + mAuthedAcctType.name();
        }
        
        public void AUTH_REQUEST() throws Exception {
            mTester.authAccount(mTargetAcct, PASSWORD, "suspended-domain-test-" + mSuspended);
        }
        
        public void CHANGE_PASSWORD_REQUEST() throws Exception {
            mTester.changePassword(mTargetAcct, PASSWORD, PASSWORD);
        }
        
        public void GET_PREFS_REQUEST() throws Exception {
            XMLElement req = new XMLElement(AccountConstants.GET_PREFS_REQUEST);
            mTester.invoke(req);
        }
        
        public void MODIFY_PREFS_REQUEST() throws Exception {
            XMLElement req = new XMLElement(AccountConstants.MODIFY_PREFS_REQUEST);
            Element p = req.addElement(AccountConstants.E_PREF);
            p.addAttribute(AccountConstants.A_NAME, Provisioning.A_zimbraPrefSkin);
            p.setText("sand");
            mTester.invoke(req);
        }
        
        public void GET_INFO_REQUEST() throws Exception {
            XMLElement req = new XMLElement(AccountConstants.GET_INFO_REQUEST);
            mTester.invoke(req);
        }
        
        public void GET_ACCOUNT_INFO_REQUEST() throws Exception {
           XMLElement req = new XMLElement(AccountConstants.GET_ACCOUNT_INFO_REQUEST);
           Element a = req.addElement(AccountConstants.E_ACCOUNT);
           a.addAttribute(AccountConstants.A_BY, AccountConstants.A_NAME);
           a.setText(mTargetAcct.getName());
           mTester.invoke(req);
        }
        
        public void SEARCH_GAL_REQUEST() throws Exception {
            XMLElement req = new XMLElement(AccountConstants.SEARCH_GAL_REQUEST);
            Element n = req.addElement(AccountConstants.E_NAME);
            n.setText("foo");
            mTester.invoke(req);
        }
        
        public void AUTO_COMPLETE_GAL_REQUEST() throws Exception {
            XMLElement req = new XMLElement(AccountConstants.AUTO_COMPLETE_GAL_REQUEST);
            req.addAttribute(AccountConstants.A_LIMIT, "20");
            Element n = req.addElement(AccountConstants.E_NAME);
            n.setText("foo");
            mTester.invoke(req);
        }
        
        public void SYNC_GAL_REQUEST() throws Exception {
            XMLElement req = new XMLElement(AccountConstants.SYNC_GAL_REQUEST);
            mTester.invoke(req);
        }
        
        public void SEARCH_CALENDAR_RESOURCES_REQUEST() throws Exception {
            XMLElement req = new XMLElement(AccountConstants.SEARCH_CALENDAR_RESOURCES_REQUEST);
            Element sf = req.addElement(AccountConstants.E_ENTRY_SEARCH_FILTER);
            Element cond = sf.addElement(AccountConstants.E_ENTRY_SEARCH_FILTER_SINGLECOND);
            cond.addAttribute(AccountConstants.A_ENTRY_SEARCH_FILTER_ATTR, Provisioning.A_zimbraCalResType);
            cond.addAttribute(AccountConstants.A_ENTRY_SEARCH_FILTER_OP, "eq");
            cond.addAttribute(AccountConstants.A_ENTRY_SEARCH_FILTER_VALUE, "Equipment");
            mTester.invoke(req);
        }
        
        public void MODIFY_PROPERTIES_REQUEST() throws Exception {
            XMLElement req = new XMLElement(AccountConstants.MODIFY_PROPERTIES_REQUEST);
            mTester.invoke(req);
         }
        
        public void GET_ALL_LOCALES_REQUEST() throws Exception {
            XMLElement req = new XMLElement(AccountConstants.GET_ALL_LOCALES_REQUEST);
            mTester.invoke(req);
        }
        
        public void GET_AVAILABLE_LOCALES_REQUEST() throws Exception {
            XMLElement req = new XMLElement(AccountConstants.GET_AVAILABLE_LOCALES_REQUEST);
                mTester.invoke(req);
                
        }
        
        public void GET_AVAILABLE_SKINS_REQUEST() throws Exception {
            XMLElement req = new XMLElement(AccountConstants.GET_AVAILABLE_SKINS_REQUEST);
            mTester.invoke(req);
        }
        
        public void CREATE_IDENTITY_REQUEST() throws Exception {
            mTester.createIdentity(mTargetAcct, "identity-" + identityName(), new HashMap<String, Object>()) ;
        }
        
        public void MODIFY_IDENTITY_REQUEST() throws Exception {
            mTester.modifyIdentity(mTargetAcct, "identity-" + identityName(), new HashMap<String, Object>()) ;
        }
        
        public void DELETE_IDENTITY_REQUEST() throws Exception {
            mTester.deleteIdentity(mTargetAcct, "identity-" + identityName()) ;
        }
        
        public void GET_IDENTITIES_REQUEST() throws Exception {
            mTester.getAllIdentities(mTargetAcct) ;
        }
        
        /*
        private void GET_PREFS_REQUEST() {
            try {
                XMLElement req = new XMLElement(AccountConstants.GET_PREFS_REQUEST);
                mTester.invoke(req);
                if (mSuspended)
                    fail();
            } catch (ServiceException e) {
                if (mSuspended)
                    assertEquals(ServiceException.AUTH_EXPIRED, e.getCode());
                else
                    fail();
            }
        }
        
        private void MODIFY_PREFS_REQUEST() {
            try {
                XMLElement req = new XMLElement(AccountConstants.MODIFY_PREFS_REQUEST);
                Element p = req.addElement(AccountConstants.E_PREF);
                p.addAttribute(AccountConstants.A_NAME, Provisioning.A_zimbraPrefSkin);
                p.setText("sand");
                mTester.invoke(req);
                if (mSuspended)
                    fail();
            } catch (ServiceException e) {
                if (mSuspended)
                    assertEquals(ServiceException.AUTH_EXPIRED, e.getCode());
                else
                    fail();
            }
        }
        
        private void GET_INFO_REQUEST() {
            try {
                XMLElement req = new XMLElement(AccountConstants.GET_INFO_REQUEST);
                mTester.invoke(req);
                if (mSuspended)
                    fail();
            } catch (ServiceException e) {
                if (mSuspended)
                    assertEquals(ServiceException.AUTH_EXPIRED, e.getCode());
                else
                    fail();
            }
        }
        
        private void GET_ACCOUNT_INFO_REQUEST() {
            try {
                XMLElement req = new XMLElement(AccountConstants.GET_ACCOUNT_INFO_REQUEST);
                Element a = req.addElement(AccountConstants.E_ACCOUNT);
                a.addAttribute(AccountConstants.A_BY, AccountConstants.A_NAME);
                a.setText(mTargetAcct.getName());
                mTester.invoke(req);
                if (mSuspended)
                    fail();
            } catch (ServiceException e) {
                if (mSuspended)
                    assertEquals(ServiceException.AUTH_EXPIRED, e.getCode());
                else
                    fail();
            }
        }
        
        private void SEARCH_GAL_REQUEST() {
            try {
                XMLElement req = new XMLElement(AccountConstants.SEARCH_GAL_REQUEST);
                Element n = req.addElement(AccountConstants.E_NAME);
                n.setText("foo");
                mTester.invoke(req);
                if (mSuspended)
                    fail();
            } catch (ServiceException e) {
                if (mSuspended)
                    assertEquals(ServiceException.AUTH_EXPIRED, e.getCode());
                else
                    fail();
            }
        }
        
        private void AUTO_COMPLETE_GAL_REQUEST() {
            try {
                XMLElement req = new XMLElement(AccountConstants.AUTO_COMPLETE_GAL_REQUEST);
                req.addAttribute(AccountConstants.A_LIMIT, "20");
                Element n = req.addElement(AccountConstants.E_NAME);
                n.setText("foo");
                mTester.invoke(req);
                if (mSuspended)
                    fail();
            } catch (ServiceException e) {
                if (mSuspended)
                    assertEquals(ServiceException.AUTH_EXPIRED, e.getCode());
                else
                    fail();
            }
        }
        
        private void SYNC_GAL_REQUEST() {
            try {
                XMLElement req = new XMLElement(AccountConstants.SYNC_GAL_REQUEST);
                mTester.invoke(req);
                if (mSuspended)
                    fail();
            } catch (ServiceException e) {
                if (mSuspended)
                    assertEquals(ServiceException.AUTH_EXPIRED, e.getCode());
                else
                    fail();
            }
        }
        
        private void SEARCH_CALENDAR_RESOURCES_REQUEST() {
            try {
                XMLElement req = new XMLElement(AccountConstants.SEARCH_CALENDAR_RESOURCES_REQUEST);
                // req.addAttribute(AccountConstants.A_LIMIT, "20");
                Element sf = req.addElement(AccountConstants.E_ENTRY_SEARCH_FILTER);
                Element cond = sf.addElement(AccountConstants.E_ENTRY_SEARCH_FILTER_SINGLECOND);
                cond.addAttribute(AccountConstants.A_ENTRY_SEARCH_FILTER_ATTR, Provisioning.A_zimbraCalResType);
                cond.addAttribute(AccountConstants.A_ENTRY_SEARCH_FILTER_OP, "eq");
                cond.addAttribute(AccountConstants.A_ENTRY_SEARCH_FILTER_VALUE, "Equipment");
                mTester.invoke(req);
                if (mSuspended)
                    fail();
            } catch (ServiceException e) {
                if (mSuspended)
                    assertEquals(ServiceException.AUTH_EXPIRED, e.getCode());
                else
                    fail();
            }
        }
        
        private void MODIFY_PROPERTIES_REQUEST() {
            try {
                XMLElement req = new XMLElement(AccountConstants.MODIFY_PROPERTIES_REQUEST);
                mTester.invoke(req);
                if (mSuspended)
                    fail();
            } catch (ServiceException e) {
                if (mSuspended)
                    assertEquals(ServiceException.AUTH_EXPIRED, e.getCode());
                else
                    fail();
            }
        }
        
        private void GET_ALL_LOCALES_REQUEST() {
            try {
                XMLElement req = new XMLElement(AccountConstants.GET_ALL_LOCALES_REQUEST);
                mTester.invoke(req);
                if (mSuspended)
                    fail();
            } catch (ServiceException e) {
                if (mSuspended)
                    assertEquals(ServiceException.AUTH_EXPIRED, e.getCode());
                else
                    fail();
            }
        }
        
        private void GET_AVAILABLE_LOCALES_REQUEST() {
            try {
                XMLElement req = new XMLElement(AccountConstants.GET_AVAILABLE_LOCALES_REQUEST);
                mTester.invoke(req);
                if (mSuspended)
                    fail();
            } catch (ServiceException e) {
                if (mSuspended)
                    assertEquals(ServiceException.AUTH_EXPIRED, e.getCode());
                else
                    fail();
            }
        }
        
        private void GET_AVAILABLE_SKINS_REQUEST() {
            try {
                XMLElement req = new XMLElement(AccountConstants.GET_AVAILABLE_SKINS_REQUEST);
                mTester.invoke(req);
                if (mSuspended)
                    fail();
            } catch (ServiceException e) {
                if (mSuspended)
                    assertEquals(ServiceException.AUTH_EXPIRED, e.getCode());
                else
                    fail();
            }
        }
        
        private void CREATE_IDENTITY_REQUEST() {
            try {
                mTester.createIdentity(mTargetAcct, "identity-" + identityName(), new HashMap<String, Object>()) ;
                if (mSuspended)
                    fail();
            } catch (ServiceException e) {
                if (mSuspended)
                    assertEquals(ServiceException.AUTH_EXPIRED, e.getCode());
                else
                    fail();
            }
        }
        
        private void MODIFY_IDENTITY_REQUEST() {
            try {
                mTester.modifyIdentity(mTargetAcct, "identity-" + identityName(), new HashMap<String, Object>()) ;
                if (mSuspended)
                    fail();
            } catch (ServiceException e) {
                if (mSuspended)
                    assertEquals(ServiceException.AUTH_EXPIRED, e.getCode());
                else
                    fail();
            }
        }
        
        private void DELETE_IDENTITY_REQUEST() {
            try {
                mTester.deleteIdentity(mTargetAcct, "identity-" + identityName()) ;
                if (mSuspended)
                    fail();
            } catch (ServiceException e) {
                if (mSuspended)
                    assertEquals(ServiceException.AUTH_EXPIRED, e.getCode());
                else
                    fail();
            }
        }
        
        private void GET_IDENTITIES_REQUEST() {
            try {
                mTester.getAllIdentities(mTargetAcct) ;
                if (mSuspended)
                    fail();
            } catch (ServiceException e) {
                if (mSuspended)
                    assertEquals(ServiceException.AUTH_EXPIRED, e.getCode());
                else
                    fail();
            }
        }
        */
        
        private static enum Commands {
            CMD_AUTH_REQUEST("AUTH_REQUEST",                                           AccountServiceException.MAINTENANCE_MODE),
            CMD_CHANGE_PASSWORD_REQUEST("CHANGE_PASSWORD_REQUEST",                     AccountServiceException.MAINTENANCE_MODE),
            CMD_GET_PREFS_REQUEST("GET_PREFS_REQUEST",                                 ServiceException.AUTH_EXPIRED),
            CMD_MODIFY_PREFS_REQUEST("MODIFY_PREFS_REQUEST",                           ServiceException.AUTH_EXPIRED),
            CMD_GET_INFO_REQUEST("GET_INFO_REQUEST",                                   ServiceException.AUTH_EXPIRED),
            CMD_GET_ACCOUNT_INFO_REQUEST("GET_ACCOUNT_INFO_REQUEST",                   ServiceException.AUTH_EXPIRED),
            CMD_SEARCH_GAL_REQUEST("SEARCH_GAL_REQUEST",                               ServiceException.AUTH_EXPIRED),
            CMD_AUTO_COMPLETE_GAL_REQUEST("AUTO_COMPLETE_GAL_REQUEST",                 ServiceException.AUTH_EXPIRED),
            CMD_SYNC_GAL_REQUEST("SYNC_GAL_REQUEST",                                   ServiceException.AUTH_EXPIRED),
            CMD_SEARCH_CALENDAR_RESOURCES_REQUEST("SEARCH_CALENDAR_RESOURCES_REQUEST", ServiceException.AUTH_EXPIRED),
            CMD_MODIFY_PROPERTIES_REQUEST("MODIFY_PROPERTIES_REQUEST",                 ServiceException.AUTH_EXPIRED),
            CMD_GET_ALL_LOCALES_REQUEST("GET_ALL_LOCALES_REQUEST",                     ServiceException.AUTH_EXPIRED),
            CMD_GET_AVAILABLE_LOCALES_REQUEST("GET_AVAILABLE_LOCALES_REQUEST",         ServiceException.AUTH_EXPIRED),
            CMD_GET_AVAILABLE_SKINS_REQUEST("GET_AVAILABLE_SKINS_REQUEST",             ServiceException.AUTH_EXPIRED),
            CMD_CREATE_IDENTITY_REQUEST("CREATE_IDENTITY_REQUEST",                     ServiceException.AUTH_EXPIRED),
            CMD_MODIFY_IDENTITY_REQUEST("MODIFY_IDENTITY_REQUEST",                     ServiceException.AUTH_EXPIRED),
            CMD_DELETE_IDENTITY_REQUEST("DELETE_IDENTITY_REQUEST",                     ServiceException.AUTH_EXPIRED),
            CMD_GET_IDENTITIES_REQUEST("GET_IDENTITIES_REQUEST",                       ServiceException.AUTH_EXPIRED);
            
            private String mFuncName;
            private String mExpectedCode; // WhenDomainIsSuspended;
            
            Commands(String funcName, String expectedCode) {
                mFuncName = funcName;
                mExpectedCode = expectedCode;
            }
        }
        
        public static void run(SoapTester tester, AccountType authedAcctType, 
                               Account targetAcct, boolean suspended) throws Exception {
            AccountCommands cmds = new AccountCommands(tester, authedAcctType, targetAcct, suspended);
            
            /*
            cmds.AUTH_REQUEST();
            cmds.CHANGE_PASSWORD_REQUEST();
            cmds.GET_PREFS_REQUEST();
            cmds.MODIFY_PREFS_REQUEST();
            cmds.GET_INFO_REQUEST();
            cmds.GET_ACCOUNT_INFO_REQUEST();
            cmds.SEARCH_GAL_REQUEST();
            cmds.AUTO_COMPLETE_GAL_REQUEST();
            cmds.SYNC_GAL_REQUEST();
            cmds.SEARCH_CALENDAR_RESOURCES_REQUEST();
            cmds.MODIFY_PROPERTIES_REQUEST();
            cmds.GET_ALL_LOCALES_REQUEST();
            cmds.GET_AVAILABLE_LOCALES_REQUEST();
            cmds.GET_AVAILABLE_SKINS_REQUEST();
            cmds.CREATE_IDENTITY_REQUEST();
            cmds.MODIFY_IDENTITY_REQUEST();
            cmds.DELETE_IDENTITY_REQUEST();
            cmds.GET_IDENTITIES_REQUEST();
            */
           
            Class cls = cmds.getClass();
             for (Commands c : Commands.values()) {
                 Method method = cls.getMethod(c.mFuncName);
                 
                 try {
                     method.invoke(cmds);
                     if (cmds.mSuspended)
                         fail(c.name());
                 } catch (InvocationTargetException e) {
                     if (cmds.mSuspended) {
                         Throwable te = e.getTargetException();
                         if (te instanceof ServiceException) {
                             ServiceException se = (ServiceException)te;
                             assertEquals(c.name(), c.mExpectedCode, se.getCode());
                         } else
                             fail(c.name());
                     } else
                         fail(c.name());
                 }
                
             }
            
            /*
            // auth
            dispatcher.registerHandler(AccountConstants.AUTH_REQUEST, new Auth());
            dispatcher.registerHandler(AccountConstants.CHANGE_PASSWORD_REQUEST, new ChangePassword());
    
            // prefs
            dispatcher.registerHandler(AccountConstants.GET_PREFS_REQUEST, new GetPrefs());
            dispatcher.registerHandler(AccountConstants.MODIFY_PREFS_REQUEST, new ModifyPrefs());
    
            dispatcher.registerHandler(AccountConstants.GET_INFO_REQUEST, new GetInfo());
            dispatcher.registerHandler(AccountConstants.GET_ACCOUNT_INFO_REQUEST, new GetAccountInfo());
    
            dispatcher.registerHandler(AccountConstants.SEARCH_GAL_REQUEST, new SearchGal());
            dispatcher.registerHandler(AccountConstants.AUTO_COMPLETE_GAL_REQUEST, new AutoCompleteGal());
            dispatcher.registerHandler(AccountConstants.SYNC_GAL_REQUEST, new SyncGal());
            dispatcher.registerHandler(AccountConstants.SEARCH_CALENDAR_RESOURCES_REQUEST, new SearchCalendarResources());
    
            dispatcher.registerHandler(AccountConstants.MODIFY_PROPERTIES_REQUEST, new ModifyProperties());
    
            dispatcher.registerHandler(AccountConstants.GET_ALL_LOCALES_REQUEST, new GetAllLocales());
            dispatcher.registerHandler(AccountConstants.GET_AVAILABLE_LOCALES_REQUEST, new GetAvailableLocales());
            dispatcher.registerHandler(AccountConstants.GET_AVAILABLE_SKINS_REQUEST, new GetAvailableSkins());
    
            // identity
            dispatcher.registerHandler(AccountConstants.CREATE_IDENTITY_REQUEST, new CreateIdentity());
            dispatcher.registerHandler(AccountConstants.MODIFY_IDENTITY_REQUEST, new ModifyIdentity());
            dispatcher.registerHandler(AccountConstants.DELETE_IDENTITY_REQUEST, new DeleteIdentity());
            dispatcher.registerHandler(AccountConstants.GET_IDENTITIES_REQUEST, new GetIdentities());
            
            // signature
            dispatcher.registerHandler(AccountConstants.CREATE_SIGNATURE_REQUEST, new CreateSignature());
            dispatcher.registerHandler(AccountConstants.MODIFY_SIGNATURE_REQUEST, new ModifySignature());
            dispatcher.registerHandler(AccountConstants.DELETE_SIGNATURE_REQUEST, new DeleteSignature());
            dispatcher.registerHandler(AccountConstants.GET_SIGNATURES_REQUEST, new GetSignatures());
             
             */
        }
    }
    
    private static class SoapTester extends SoapProvisioning {
              
        private void soapAuthenticate(String name, String password) throws ServiceException {
            XMLElement req = new XMLElement(AccountConstants.AUTH_REQUEST);
            Element a = req.addElement(AccountConstants.E_ACCOUNT);
            a.addAttribute(AccountConstants.A_BY, "name");
            a.setText(name);
            req.addElement(AccountConstants.E_PASSWORD).setText(password);
            Element response = invoke(req);
            String authToken = response.getElement(AccountConstants.E_AUTH_TOKEN).getText();
            setAuthToken(authToken);
         }     
        
        static SoapTester newInstance(Account acct, AccountType at) throws Exception {
            
            SoapTester soapTester = new SoapTester();
            if (at == AccountType.ACCT_USER) {
                soapTester.soapSetURI(TestUtil.getSoapUrl());
                soapTester.authAccount(acct, PASSWORD, "SoapTester");
                soapTester.soapAuthenticate(acct.getName(), PASSWORD);
            } else {
                soapTester.soapSetURI(TestUtil.getAdminSoapUrl());
                soapTester.soapAdminAuthenticate(acct.getName(), PASSWORD);
            }
                
            return soapTester;
        }
    }

 
    private void commandsTest(SoapTester tester, AccountType authedAcctType, 
                              Account targetAcct, boolean suspended) throws Exception {
        AccountCommands.run(tester, authedAcctType, targetAcct, suspended);
        // AdminCommands.run(tester, authedAcctType, targetAcct, suspended);
        // MailCommands.run(tester, authedAcctType, targetAcct, suspended);
    }
    
    private void suspendedDomainTest() throws Exception {
        Domain domain = getDomain();
        mProv.modifyDomainStatus(domain, DomainStatus.active.name());
        
        // auth to user, domain admin and global admin before we suspend the domain
        Account user = createAccount(ACCOUNT_NAME("user"), AccountType.ACCT_USER);
        SoapTester testerUser = SoapTester.newInstance(user, AccountType.ACCT_USER);
        
        Account domainAdmin = createAccount(ACCOUNT_NAME("domain-admin"), AccountType.ACCT_DOMAIN_ADMIN);
        SoapTester testerDomainAdmin = SoapTester.newInstance(domainAdmin, AccountType.ACCT_DOMAIN_ADMIN);
        
        Account globalAdmin = createAccount(ACCOUNT_NAME("global-admin"), AccountType.ACCT_GLOBAL_ADMIN);
        SoapTester testerGlobalAdmin = SoapTester.newInstance(globalAdmin, AccountType.ACCT_GLOBAL_ADMIN);
        
        mProv.modifyDomainStatus(domain, DomainStatus.suspended.name());
        commandsTest(testerUser, AccountType.ACCT_USER, user, true);
        commandsTest(testerDomainAdmin, AccountType.ACCT_DOMAIN_ADMIN, user, true);
        commandsTest(testerGlobalAdmin, AccountType.ACCT_GLOBAL_ADMIN, user, true);
        
        mProv.modifyDomainStatus(domain, DomainStatus.active.name());
        commandsTest(testerUser, AccountType.ACCT_USER, user, false);
        commandsTest(testerDomainAdmin, AccountType.ACCT_DOMAIN_ADMIN, user, false);
        commandsTest(testerGlobalAdmin, AccountType.ACCT_GLOBAL_ADMIN, user, false);
    }
    
    private void execute() throws Exception {
        // statusTest();
        suspendedDomainTest();
    }

    public void testDomainStatus() throws Exception {
        try {
            System.out.println("\nTest " + TEST_ID + " starting\n");
            execute();
            System.out.println("\nTest " + TEST_ID + " done!");
        } catch (ServiceException e) {
            Throwable cause = e.getCause();
            System.out.println("ERROR: " + e.getCode() + " (" + e.getMessage() + ")" + 
                               (cause == null ? "" : " (cause: " + cause.getClass().getName() + " " + cause.getMessage() + ")"));
            e.printStackTrace(System.out);
            System.out.println("\nTest " + TEST_ID + " failed!");
        } catch (AssertionFailedError e) {
            System.out.println("\n===== assertion failed =====");
            System.out.println(e.getMessage());
            e.printStackTrace(System.out);
        }
    }
    
    public static void main(String[] args) throws Exception {
        // CliUtil.toolSetup("DEBUG");
        CliUtil.toolSetup();
        
        TestUtil.runTest(new TestSuite(TestDomainStatus.class));
        
        /*
        TestDomainStatus t = new TestDomainStatus();
        t.setUp();
        t.execute();
        */
    }
}
