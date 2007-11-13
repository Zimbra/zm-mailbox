/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007 Zimbra, Inc.
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

package com.zimbra.qa.unittest;

import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;



import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
import com.zimbra.cs.account.AttributeType;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.account.Provisioning.DomainBy;
import com.zimbra.cs.account.Signature;
import com.zimbra.cs.account.ldap.LdapProvisioning;
import com.zimbra.cs.account.soap.SoapProvisioning;
import com.zimbra.cs.account.soap.SoapProvisioning.DelegateAuthResponse;
import com.zimbra.cs.lmtpserver.LmtpProtocolException;


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
            TestUtil.addMessageLmtp(1, NAME_PREFIX + " 1", acct.getName(), "phoebeshao");
            
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
                Map attrs = new HashMap<String, String>();
                attrs.put(Provisioning.A_zimbraDomainStatus, "");
                mProv.modifyAttrs(domain, attrs);
                domainStatus = DomainStatus.active.name();
            } else {
                Map attrs = new HashMap<String, String>();
                attrs.put(Provisioning.A_zimbraDomainStatus, domainStatus);
                mProv.modifyAttrs(domain, attrs);
            }
            
            statusTest(domainStatus);
        }
    }
    
    static class SoapTestContext {
        private SoapClient mSoapClient;
        private AccountType mAuthedAcctType;
        private Account mTargetAcct;
        private boolean mSuspended;
        private String mDomainName;
        private String mDomainId;
        private String[] mTesters;
        private String mTestId;
        
        SoapTestContext(SoapClient soapClient, AccountType authedAcctType, 
                        Account targetAcct, boolean suspended, 
                        String domainName, String domainId, // we don't want to pass a Domain object
                        String[] testers, String testId) {
            mSoapClient = soapClient;
            mAuthedAcctType = authedAcctType;
            mTargetAcct = targetAcct;
            mSuspended = suspended;
            mDomainName = domainName;
            mDomainId = domainId;
            mTesters = testers;
            mTestId = testId;
        }
    }
    
    static class SoapCommands {
         
        private SoapTestContext mCtx;
        
        public static enum TesterObjs {
            TESTER_ID_ACCOUNT_FOR_DELETE_ACCOUNT_REQUEST
        }
        
        // signature APIs only takes id, not name, 
        // remember the signature ids, we need them for modify/delete signatures
        private Map<String, String> mSignatureNameIdMap;

        SoapCommands(SoapTestContext ctx) {
            mCtx = ctx;
            
            mSignatureNameIdMap = new HashMap<String, String>();
        }
        
        private String testCtxId() {
            return mCtx.mAuthedAcctType.name() + "-" + (mCtx.mSuspended?"suspended":"active");
        }
        
        private String identityName() {
            return "identity-of-" + testCtxId();
        }
        
        private String signatureName() {
            return "signature-of-" + testCtxId();
        }
        
        private String accountName() {
            return "acct-created-by-" + testCtxId() + "@" + mCtx.mDomainName;
        }
        
        private String aliasName() {
            return "alias-created-by-" + testCtxId() + "@" + mCtx.mDomainName;
        }
        
        private String domainName() {
            return "domain-created-by-" + testCtxId() + "." + mCtx.mDomainName;
        }
        
        private String cosName() {
            String cosName = "cos-created-by-" + testCtxId() + "-" + mCtx.mTestId;
            return cosName.replace("_", "-");
        }
        
        /*
         * ================
         * account commands
         * ================
         */
        public void AUTH_REQUEST() throws Exception {
            mCtx.mSoapClient.authAccount(mCtx.mTargetAcct, PASSWORD, "suspended-domain-test-" + mCtx.mSuspended);
        }
        
        public void CHANGE_PASSWORD_REQUEST() throws Exception {
            mCtx.mSoapClient.changePassword(mCtx.mTargetAcct, PASSWORD, PASSWORD);
        }
        
        public void GET_PREFS_REQUEST() throws Exception {
            XMLElement req = new XMLElement(AccountConstants.GET_PREFS_REQUEST);
            mCtx.mSoapClient.invoke(req);
        }
        
        public void MODIFY_PREFS_REQUEST() throws Exception {
            XMLElement req = new XMLElement(AccountConstants.MODIFY_PREFS_REQUEST);
            Element p = req.addElement(AccountConstants.E_PREF);
            p.addAttribute(AccountConstants.A_NAME, Provisioning.A_zimbraPrefSkin);
            p.setText("sand");
            mCtx.mSoapClient.invoke(req);
        }
        
        public void GET_INFO_REQUEST() throws Exception {
            XMLElement req = new XMLElement(AccountConstants.GET_INFO_REQUEST);
            mCtx.mSoapClient.invoke(req);
        }
        
        public void GET_ACCOUNT_INFO_REQUEST() throws Exception {
           XMLElement req = new XMLElement(AccountConstants.GET_ACCOUNT_INFO_REQUEST);
           Element a = req.addElement(AccountConstants.E_ACCOUNT);
           a.addAttribute(AccountConstants.A_BY, AccountConstants.A_NAME);
           a.setText(mCtx.mTargetAcct.getName());
           mCtx.mSoapClient.invokeOnTargetAccount(req, mCtx.mTargetAcct.getId());
        }
        
        public void SEARCH_GAL_REQUEST() throws Exception {
            XMLElement req = new XMLElement(AccountConstants.SEARCH_GAL_REQUEST);
            Element n = req.addElement(AccountConstants.E_NAME);
            n.setText("foo");
            mCtx.mSoapClient.invoke(req);
        }
        
        public void AUTO_COMPLETE_GAL_REQUEST() throws Exception {
            XMLElement req = new XMLElement(AccountConstants.AUTO_COMPLETE_GAL_REQUEST);
            req.addAttribute(AccountConstants.A_LIMIT, "20");
            Element n = req.addElement(AccountConstants.E_NAME);
            n.setText("foo");
            mCtx.mSoapClient.invoke(req);
        }
        
        public void SYNC_GAL_REQUEST() throws Exception {
            XMLElement req = new XMLElement(AccountConstants.SYNC_GAL_REQUEST);
            mCtx.mSoapClient.invoke(req);
        }
        
        public void SEARCH_CALENDAR_RESOURCES_REQUEST() throws Exception {
            XMLElement req = new XMLElement(AccountConstants.SEARCH_CALENDAR_RESOURCES_REQUEST);
            Element sf = req.addElement(AccountConstants.E_ENTRY_SEARCH_FILTER);
            Element cond = sf.addElement(AccountConstants.E_ENTRY_SEARCH_FILTER_SINGLECOND);
            cond.addAttribute(AccountConstants.A_ENTRY_SEARCH_FILTER_ATTR, Provisioning.A_zimbraCalResType);
            cond.addAttribute(AccountConstants.A_ENTRY_SEARCH_FILTER_OP, "eq");
            cond.addAttribute(AccountConstants.A_ENTRY_SEARCH_FILTER_VALUE, "Equipment");
            mCtx.mSoapClient.invoke(req);
        }
        
        public void MODIFY_PROPERTIES_REQUEST() throws Exception {
            XMLElement req = new XMLElement(AccountConstants.MODIFY_PROPERTIES_REQUEST);
            mCtx.mSoapClient.invoke(req);
         }
        
        public void GET_ALL_LOCALES_REQUEST() throws Exception {
            XMLElement req = new XMLElement(AccountConstants.GET_ALL_LOCALES_REQUEST);
            mCtx.mSoapClient.invoke(req);
        }
        
        public void GET_AVAILABLE_LOCALES_REQUEST() throws Exception {
            XMLElement req = new XMLElement(AccountConstants.GET_AVAILABLE_LOCALES_REQUEST);
            mCtx.mSoapClient.invoke(req);
        }
        
        public void GET_AVAILABLE_SKINS_REQUEST() throws Exception {
            XMLElement req = new XMLElement(AccountConstants.GET_AVAILABLE_SKINS_REQUEST);
            mCtx.mSoapClient.invoke(req);
        }
        
        public void CREATE_IDENTITY_REQUEST() throws Exception {
            mCtx.mSoapClient.createIdentity(mCtx.mTargetAcct, identityName(), new HashMap<String, Object>()) ;
        }
        
        public void MODIFY_IDENTITY_REQUEST() throws Exception {
            mCtx.mSoapClient.modifyIdentity(mCtx.mTargetAcct, identityName(), new HashMap<String, Object>()) ;
        }
        
        public void DELETE_IDENTITY_REQUEST() throws Exception {
            mCtx.mSoapClient.deleteIdentity(mCtx.mTargetAcct, identityName()) ;
        }
        
        public void GET_IDENTITIES_REQUEST() throws Exception {
            mCtx.mSoapClient.getAllIdentities(mCtx.mTargetAcct) ;
        }

        public void CREATE_SIGNATURE_REQUEST() throws Exception {
            Signature sig = mCtx.mSoapClient.createSignature(mCtx.mTargetAcct, signatureName(), new HashMap<String, Object>());
            mSignatureNameIdMap.put(sig.getName(), sig.getId());
        }
        
        public void MODIFY_SIGNATURE_REQUEST() throws Exception {
            String sigId = mSignatureNameIdMap.get(signatureName());
            mCtx.mSoapClient.modifySignature(mCtx.mTargetAcct, sigId, new HashMap<String, Object>()) ;
        }
        
        public void DELETE_SIGNATURE_REQUEST() throws Exception {
            String sigId = mSignatureNameIdMap.get(signatureName());
            mCtx.mSoapClient.deleteSignature(mCtx.mTargetAcct, sigId) ;
        }
        
        public void GET_SIGNATURES_REQUEST() throws Exception {
            mCtx.mSoapClient.getAllSignatures(mCtx.mTargetAcct) ;
        }
        
        /*
         * ==============
         * admin commands
         * ==============
         */
        public void PING_REQUEST() throws Exception {
            XMLElement req = new XMLElement(AdminConstants.PING_REQUEST);
            mCtx.mSoapClient.invoke(req);
        }
        
        public void CHECK_HEALTH_REQUEST() throws Exception {
            mCtx.mSoapClient.healthCheck();
        }
        
        public void ADMIN_AUTH_REQUEST() throws Exception {
            XMLElement req = new XMLElement(AdminConstants.AUTH_REQUEST);
            req.addElement(AdminConstants.E_NAME).setText(mCtx.mTargetAcct.getName());
            req.addElement(AdminConstants.E_PASSWORD).setText(PASSWORD);
            mCtx.mSoapClient.invoke(req);
        }
        
        public void CREATE_ACCOUNT_REQUEST() throws Exception {
            Account acct = mCtx.mSoapClient.createAccount(accountName(), PASSWORD, null);
        }
        
        public void DELEGATE_AUTH_REQUEST() throws Exception {
            DelegateAuthResponse r = mCtx.mSoapClient.delegateAuth(AccountBy.name, mCtx.mTargetAcct.getName(), 0);
        }
        
        public void GET_ACCOUNT_REQUEST() throws Exception {
            XMLElement req = new XMLElement(AdminConstants.GET_ACCOUNT_REQUEST);
            Element a = req.addElement(AccountConstants.E_ACCOUNT);
            a.addAttribute(AccountConstants.A_BY, AccountConstants.A_NAME);
            a.setText(mCtx.mTargetAcct.getName());
            mCtx.mSoapClient.invokeOnTargetAccount(req, mCtx.mTargetAcct.getId());
        }
        
        public void ADMIN_GET_ACCOUNT_INFO_REQUEST() throws Exception {
            mCtx.mSoapClient.getAccountInfo(AccountBy.name, mCtx.mTargetAcct.getName());
        }
        
        public void GET_ALL_ACCOUNTS_REQUEST() throws Exception {
            // the one in SOapProvisioning requires a Domain obj, so we write our own
            XMLElement req = new XMLElement(AdminConstants.GET_ALL_ACCOUNTS_REQUEST);
            Element eDomain = req.addElement(AdminConstants.E_DOMAIN);
            eDomain.addAttribute(AdminConstants.A_BY, DomainBy.name.name());
            eDomain.setText(mCtx.mDomainName);
            mCtx.mSoapClient.invoke(req);
        }
        
        public void KNOWNBUG_GET_ALL_ACCOUNTS_REQUEST() throws Exception {
            // if invoked by global admin without a specific domain, accounts in the suspended domain will also be returned, they should not
            XMLElement req = new XMLElement(AdminConstants.GET_ALL_ACCOUNTS_REQUEST);
            mCtx.mSoapClient.invoke(req);
        }
        
        public void GET_ALL_ADMIN_ACCOUNTS_REQUEST() throws Exception {
            mCtx.mSoapClient.getAllAdminAccounts();
        }
        
        public void MODIFY_ACCOUNT_REQUEST() throws Exception {
            XMLElement req = new XMLElement(AdminConstants.MODIFY_ACCOUNT_REQUEST);
            Element eId = req.addElement(AccountConstants.E_ID);
            eId.setText(mCtx.mTargetAcct.getId());
            Element eA = req.addElement(AccountConstants.E_A);
            eA.addAttribute(AccountConstants.A_N, Provisioning.A_zimbraFeatureCalendarEnabled);
            mCtx.mSoapClient.invoke(req);
        }
        
        public void DELETE_ACCOUNT_REQUEST() throws Exception {
            mCtx.mSoapClient.deleteAccount(mCtx.mTesters[TesterObjs.TESTER_ID_ACCOUNT_FOR_DELETE_ACCOUNT_REQUEST.ordinal()]);
        }
        
        public void SET_PASSWORD_REQUEST() throws Exception {
            mCtx.mSoapClient.setPassword(mCtx.mTargetAcct, PASSWORD);
        }
        
        public void CHECK_PASSWORD_STRENGTH_REQUEST() throws Exception {
            mCtx.mSoapClient.checkPasswordStrength(mCtx.mTargetAcct, PASSWORD);
        }
        
        public void ADD_ACCOUNT_ALIAS_REQUEST() throws Exception {
            mCtx.mSoapClient.addAlias(mCtx.mTargetAcct, aliasName());
        }
          
        public void REMOVE_ACCOUNT_ALIAS_REQUEST() throws Exception {
            mCtx.mSoapClient.removeAlias(mCtx.mTargetAcct, aliasName());
        }
        
        /* ... TODO */
        
        public void CREATE_DOMAIN_REQUEST() throws Exception {
            mCtx.mSoapClient.createDomain(domainName(), null);
        }
        
        public void GET_DOMAIN_REQUEST() throws Exception {
            mCtx.mSoapClient.get(DomainBy.name, mCtx.mDomainName) ;
        }
        
        public void GET_DOMAIN_INFO_REQUEST() throws Exception {
            // KNOWNBU?? what is this API for and why doesn't it require any auth?
            mCtx.mSoapClient.getDomainInfo(DomainBy.name, mCtx.mDomainName);
        }
       
        public void GET_ALL_DOMAINS_REQUEST() throws Exception {
            mCtx.mSoapClient.getAllDomains();
        }
        
        public void MODIFY_DOMAIN_REQUEST() throws Exception {
            XMLElement req = new XMLElement(AdminConstants.MODIFY_DOMAIN_REQUEST);
            Element eId = req.addElement(AdminConstants.E_ID);
            eId.setText(mCtx.mDomainId);
            Element eA = req.addElement(AccountConstants.E_A);
            eA.addAttribute(AccountConstants.A_N, Provisioning.A_description).addText("this is a domain");
            mCtx.mSoapClient.invoke(req);
        }
        
        /*
        public void MODIFY_DOMAIN_STATUS_REQUEST() throws Exception {
            String curStatus = (mCtx.mSuspended) ? Provisioning.DOMAIN_STATUS_SUSPENDED : Provisioning.DOMAIN_STATUS_ACTIVE;
            
            {   // change to a new status 
                XMLElement req = new XMLElement(AdminConstants.MODIFY_DOMAIN_STATUS_REQUEST);
                Element eId = req.addElement(AccountConstants.E_ID);
                eId.setText(mCtx.mDomainId);
                Element eStatus = req.addElement(AdminConstants.E_STATUS);
                eStatus.setText(Provisioning.DOMAIN_STATUS_LOCKED);
                mCtx.mSoapClient.invoke(req);
            }
            
            {
                // set it back so our test can continue
                XMLElement req = new XMLElement(AdminConstants.MODIFY_DOMAIN_STATUS_REQUEST);
                Element eId = req.addElement(AccountConstants.E_ID);
                eId.setText(mCtx.mDomainId);
                Element eStatus = req.addElement(AdminConstants.E_STATUS);
                eStatus.setText(curStatus);
                mCtx.mSoapClient.invoke(req);
            }
        }
        */
        
        public void DELETE_DOMAIN_REQUEST() throws Exception {
            mCtx.mSoapClient.deleteDomain(mCtx.mDomainId);
        }
        
        
        /* ... TODO */
        public void CREATE_COS_REQUEST() throws Exception {
            mCtx.mSoapClient.createCos(cosName(), null);
        }
        
        
        private static enum Result {
            GOOD("G_O_O_D", "G_O_O_D"),
            BAD("B_A_D", "B_A_D"),
            
            KNOWN_BUG("KNOWN_BUG", "KNOWN_BUG"),
            ACCOUNT_INACTIVE(AccountServiceException.ACCOUNT_INACTIVE, "account is not active"),
            AUTH_EXPIRED(ServiceException.AUTH_EXPIRED, "auth credentials have expired"),
            DOMAIN_NOT_EMPTY(AccountServiceException.DOMAIN_NOT_EMPTY, "domain not empty"),
            MAINTENANCE_MODE(AccountServiceException.MAINTENANCE_MODE, "account is in maintenance mode"),
            PERM_DENIED_1(ServiceException.PERM_DENIED, "permission denied: not an admin account"),
            PERM_DENIED_2(ServiceException.PERM_DENIED, "permission denied: need admin token"),
            PERM_DENIED_3(ServiceException.PERM_DENIED, "permission denied: domain is suspended");
            
            String mCode;
            String mMsg;
            
            private static class R {
                static Map<String, Result> sMap = new HashMap<String, Result>();
                static String getKey(String code, String msg) {
                    return "[" +code + "]" + " - " + msg;
                }
            }

            Result(String code, String msg) {
                mCode = code;
                mMsg = msg;
                
                R.sMap.put(R.getKey(mCode, mMsg), this);
            }
            
            static Result toResult(ServiceException e) {
                String code = e.getCode();
                String msg = e.getMessage();
                
                assertNotNull(code);
                assertNotNull(msg);
                    
                /*
                String key = R.getKey(code, msg);
                Result r = R.sMap.get(key);
                assertNotNull(key, r);  // unknown exception
                */
                
                String key = R.getKey(code, msg);
                for (String k : R.sMap.keySet()) {
                    if (key.startsWith(k))
                        return R.sMap.get(k);
                }
                fail("unknow exception " + key);
                return null; // to shut the compiler error
            }
        }

        private static enum Command {
            ACCOUNT_FIRST_COMMAND(),
            ACCOUNT_AUTH_REQUEST("AUTH_REQUEST",                                           Result.GOOD,             Result.GOOD,             Result.GOOD,             Result.MAINTENANCE_MODE, Result.MAINTENANCE_MODE, Result.MAINTENANCE_MODE),
            ACCOUNT_CHANGE_PASSWORD_REQUEST("CHANGE_PASSWORD_REQUEST",                     Result.GOOD,             Result.GOOD,             Result.GOOD,             Result.MAINTENANCE_MODE, Result.MAINTENANCE_MODE, Result.MAINTENANCE_MODE),
            ACCOUNT_GET_PREFS_REQUEST("GET_PREFS_REQUEST",                                 Result.GOOD,             Result.GOOD,             Result.GOOD,             Result.AUTH_EXPIRED,     Result.AUTH_EXPIRED,     Result.GOOD),
            ACCOUNT_MODIFY_PREFS_REQUEST("MODIFY_PREFS_REQUEST",                           Result.GOOD,             Result.GOOD,             Result.GOOD,             Result.AUTH_EXPIRED,     Result.AUTH_EXPIRED,     Result.GOOD),
            ACCOUNT_GET_INFO_REQUEST("GET_INFO_REQUEST",                                   Result.GOOD,             Result.GOOD,             Result.GOOD,             Result.AUTH_EXPIRED,     Result.AUTH_EXPIRED,     Result.GOOD),
            ACCOUNT_GET_ACCOUNT_INFO_REQUEST("GET_ACCOUNT_INFO_REQUEST",                   Result.GOOD,             Result.GOOD,             Result.GOOD,             Result.AUTH_EXPIRED,     Result.AUTH_EXPIRED,     Result.ACCOUNT_INACTIVE),
            ACCOUNT_SEARCH_GAL_REQUEST("SEARCH_GAL_REQUEST",                               Result.GOOD,             Result.GOOD,             Result.GOOD,             Result.AUTH_EXPIRED,     Result.AUTH_EXPIRED,     Result.GOOD),
            ACCOUNT_AUTO_COMPLETE_GAL_REQUEST("AUTO_COMPLETE_GAL_REQUEST",                 Result.GOOD,             Result.GOOD,             Result.GOOD,             Result.AUTH_EXPIRED,     Result.AUTH_EXPIRED,     Result.GOOD),
            ACCOUNT_SYNC_GAL_REQUEST("SYNC_GAL_REQUEST",                                   Result.GOOD,             Result.GOOD,             Result.GOOD,             Result.AUTH_EXPIRED,     Result.AUTH_EXPIRED,     Result.GOOD),
            ACCOUNT_SEARCH_CALENDAR_RESOURCES_REQUEST("SEARCH_CALENDAR_RESOURCES_REQUEST", Result.GOOD,             Result.GOOD,             Result.GOOD,             Result.AUTH_EXPIRED,     Result.AUTH_EXPIRED,     Result.GOOD),
            ACCOUNT_MODIFY_PROPERTIES_REQUEST("MODIFY_PROPERTIES_REQUEST",                 Result.GOOD,             Result.GOOD,             Result.GOOD,             Result.AUTH_EXPIRED,     Result.AUTH_EXPIRED,     Result.GOOD),
            ACCOUNT_GET_ALL_LOCALES_REQUEST("GET_ALL_LOCALES_REQUEST",                     Result.GOOD,             Result.GOOD,             Result.GOOD,             Result.AUTH_EXPIRED,     Result.AUTH_EXPIRED,     Result.GOOD),
            ACCOUNT_GET_AVAILABLE_LOCALES_REQUEST("GET_AVAILABLE_LOCALES_REQUEST",         Result.GOOD,             Result.GOOD,             Result.GOOD,             Result.AUTH_EXPIRED,     Result.AUTH_EXPIRED,     Result.GOOD),
            ACCOUNT_GET_AVAILABLE_SKINS_REQUEST("GET_AVAILABLE_SKINS_REQUEST",             Result.GOOD,             Result.GOOD,             Result.GOOD,             Result.AUTH_EXPIRED,     Result.AUTH_EXPIRED,     Result.GOOD),
            ACCOUNT_CREATE_IDENTITY_REQUEST("CREATE_IDENTITY_REQUEST",                     Result.GOOD,             Result.GOOD,             Result.GOOD,             Result.AUTH_EXPIRED,     Result.AUTH_EXPIRED,     Result.ACCOUNT_INACTIVE),
            ACCOUNT_MODIFY_IDENTITY_REQUEST("MODIFY_IDENTITY_REQUEST",                     Result.GOOD,             Result.GOOD,             Result.GOOD,             Result.AUTH_EXPIRED,     Result.AUTH_EXPIRED,     Result.ACCOUNT_INACTIVE),
            ACCOUNT_DELETE_IDENTITY_REQUEST("DELETE_IDENTITY_REQUEST",                     Result.GOOD,             Result.GOOD,             Result.GOOD,             Result.AUTH_EXPIRED,     Result.AUTH_EXPIRED,     Result.ACCOUNT_INACTIVE),
            ACCOUNT_GET_IDENTITIES_REQUEST("GET_IDENTITIES_REQUEST",                       Result.GOOD,             Result.GOOD,             Result.GOOD,             Result.AUTH_EXPIRED,     Result.AUTH_EXPIRED,     Result.ACCOUNT_INACTIVE),
            ACCOUNT_CREATE_SIGNATURE_REQUEST("CREATE_SIGNATURE_REQUEST",                   Result.GOOD,             Result.GOOD,             Result.GOOD,             Result.AUTH_EXPIRED,     Result.AUTH_EXPIRED,     Result.ACCOUNT_INACTIVE),
            ACCOUNT_MODIFY_SIGNATURE_REQUEST("MODIFY_SIGNATURE_REQUEST",                   Result.GOOD,             Result.GOOD,             Result.GOOD,             Result.AUTH_EXPIRED,     Result.AUTH_EXPIRED,     Result.ACCOUNT_INACTIVE),
            ACCOUNT_DELETE_SIGNATURE_REQUEST("DELETE_SIGNATURE_REQUEST",                   Result.GOOD,             Result.GOOD,             Result.GOOD,             Result.AUTH_EXPIRED,     Result.AUTH_EXPIRED,     Result.ACCOUNT_INACTIVE),
            ACCOUNT_GET_SIGNATURES_REQUEST("GET_SIGNATURES_REQUEST",                       Result.GOOD,             Result.GOOD,             Result.GOOD,             Result.AUTH_EXPIRED,     Result.AUTH_EXPIRED,     Result.ACCOUNT_INACTIVE),
            ACCOUNT_LAST_COMMAND(),
          
            
            ADMIN_FIRST_COMMAND(),
            ADMIN_PING_REQUEST("PING_REQUEST",                                             Result.GOOD,             Result.GOOD,             Result.GOOD,             Result.GOOD,             Result.GOOD,             Result.GOOD),
            ADMIN_CHECK_HEALTH_REQUEST("CHECK_HEALTH_REQUEST",                             Result.GOOD,             Result.GOOD,             Result.GOOD,             Result.GOOD,             Result.GOOD,             Result.GOOD),
            // TODO, for now the target account is always a user account, should also test when the target is a domain admon/global admin
            ADMIN_AUTH_REQUEST("ADMIN_AUTH_REQUEST",                                       Result.PERM_DENIED_1,    Result.PERM_DENIED_1,    Result.PERM_DENIED_1,    Result.MAINTENANCE_MODE, Result.MAINTENANCE_MODE, Result.MAINTENANCE_MODE),
            ADMIN_CREATE_ACCOUNT_REQUEST("CREATE_ACCOUNT_REQUEST",                         Result.PERM_DENIED_2,    Result.GOOD,             Result.GOOD,             Result.PERM_DENIED_2,    Result.AUTH_EXPIRED,     Result.PERM_DENIED_3),
            ADMIN_DELEGATE_AUTH_REQUEST("DELEGATE_AUTH_REQUEST",                           Result.PERM_DENIED_2,    Result.GOOD,             Result.GOOD,             Result.PERM_DENIED_2,    Result.AUTH_EXPIRED,     Result.PERM_DENIED_3),
            ADMIN_GET_ACCOUNT_REQUEST("GET_ACCOUNT_REQUEST",                               Result.PERM_DENIED_2,    Result.GOOD,             Result.GOOD,             Result.PERM_DENIED_2,    Result.AUTH_EXPIRED,     Result.PERM_DENIED_3),
            ADMIN_GET_ACCOUNT_INFO_REQUEST("ADMIN_GET_ACCOUNT_INFO_REQUEST",               Result.PERM_DENIED_2,    Result.GOOD,             Result.GOOD,             Result.PERM_DENIED_2,    Result.AUTH_EXPIRED,     Result.PERM_DENIED_3),
            ADMIN_GET_ALL_ACCOUNTS_REQUEST("GET_ALL_ACCOUNTS_REQUEST",                     Result.PERM_DENIED_2,    Result.GOOD,             Result.GOOD,             Result.PERM_DENIED_2,    Result.AUTH_EXPIRED,     Result.PERM_DENIED_3),
            ADMIN_KNOWNBUG_GET_ALL_ACCOUNTS_REQUEST("KNOWNBUG_GET_ALL_ACCOUNTS_REQUEST",   Result.PERM_DENIED_2,    Result.GOOD,             Result.GOOD,             Result.PERM_DENIED_2,    Result.AUTH_EXPIRED,     Result.KNOWN_BUG),
            ADMIN_GET_ALL_ADMIN_ACCOUNTS_REQUEST("GET_ALL_ADMIN_ACCOUNTS_REQUEST",         Result.PERM_DENIED_2,    Result.PERM_DENIED_2,    Result.GOOD,             Result.PERM_DENIED_2,    Result.PERM_DENIED_2,    Result.GOOD),
            ADMIN_MODIFY_ACCOUNT_REQUEST("MODIFY_ACCOUNT_REQUEST",                         Result.PERM_DENIED_2,    Result.GOOD,             Result.GOOD,             Result.PERM_DENIED_2,    Result.AUTH_EXPIRED,     Result.PERM_DENIED_3),
            ADMIN_DELETE_ACCOUNT_REQUEST("DELETE_ACCOUNT_REQUEST",                         Result.PERM_DENIED_2,    Result.GOOD,             Result.GOOD,             Result.PERM_DENIED_2,    Result.AUTH_EXPIRED,     Result.PERM_DENIED_3),
            ADMIN_SET_PASSWORD_REQUEST("SET_PASSWORD_REQUEST",                             Result.PERM_DENIED_2,    Result.GOOD,             Result.GOOD,             Result.PERM_DENIED_2,    Result.AUTH_EXPIRED,     Result.PERM_DENIED_3),
            ADMIN_CHECK_PASSWORD_STRENGTH_REQUEST("CHECK_PASSWORD_STRENGTH_REQUEST",       Result.PERM_DENIED_2,    Result.GOOD,             Result.GOOD,             Result.PERM_DENIED_2,    Result.AUTH_EXPIRED,     Result.PERM_DENIED_3),
            ADMIN_ADD_ACCOUNT_ALIAS_REQUEST("ADD_ACCOUNT_ALIAS_REQUEST",                   Result.PERM_DENIED_2,    Result.GOOD,             Result.GOOD,             Result.PERM_DENIED_2,    Result.AUTH_EXPIRED,     Result.PERM_DENIED_3),
            ADMIN_REMOVE_ACCOUNT_ALIAS_REQUEST("REMOVE_ACCOUNT_ALIAS_REQUEST",             Result.PERM_DENIED_2,    Result.GOOD,             Result.GOOD,             Result.PERM_DENIED_2,    Result.AUTH_EXPIRED,     Result.PERM_DENIED_3), 
            /* TODO skip for now, complete when have time
            dispatcher.registerHandler(AdminConstants.SEARCH_ACCOUNTS_REQUEST, new SearchAccounts());
            dispatcher.registerHandler(AdminConstants.RENAME_ACCOUNT_REQUEST, new RenameAccount());

            dispatcher.registerHandler(AdminConstants.SEARCH_DIRECTORY_REQUEST, new SearchDirectory());
            dispatcher.registerHandler(AdminConstants.GET_ACCOUNT_MEMBERSHIP_REQUEST, new GetAccountMembership());
            */
            ADMIN_CREATE_DOMAIN_REQUEST("CREATE_DOMAIN_REQUEST",                           Result.PERM_DENIED_2,    Result.PERM_DENIED_2,    Result.GOOD,             Result.PERM_DENIED_2,    Result.PERM_DENIED_2,    Result.GOOD),
            ADMIN_GET_DOMAIN_REQUEST("GET_DOMAIN_REQUEST",                                 Result.PERM_DENIED_2,    Result.GOOD,             Result.GOOD,             Result.PERM_DENIED_2,    Result.AUTH_EXPIRED,     Result.GOOD),
            // ADMIN_GET_DOMAIN_INFO_REQUEST("GET_DOMAIN_INFO_REQUEST",                       Result.PERM_DENIED_2,    Result.GOOD,    Result.GOOD,             Result.PERM_DENIED_2,    Result.AUTH_EXPIRED,     Result.PERM_DENIED_3),
            ADMIN_GET_DOMAIN_INFO_REQUEST("GET_DOMAIN_INFO_REQUEST",                       Result.GOOD,             Result.GOOD,             Result.GOOD,             Result.GOOD,             Result.GOOD,             Result.GOOD),
            ADMIN_GET_ALL_DOMAINS_REQUEST("GET_ALL_DOMAINS_REQUEST",                       Result.PERM_DENIED_2,    Result.PERM_DENIED_2,    Result.GOOD,             Result.PERM_DENIED_2,    Result.PERM_DENIED_2,    Result.GOOD),
            ADMIN_MODIFY_DOMAIN_REQUEST("MODIFY_DOMAIN_REQUEST",                           Result.PERM_DENIED_2,    Result.PERM_DENIED_2,    Result.GOOD,             Result.PERM_DENIED_2,    Result.PERM_DENIED_2,    Result.GOOD),
//             ADMIN_MODIFY_DOMAIN_STATUS_REQUEST("MODIFY_DOMAIN_STATUS_REQUEST",             Result.PERM_DENIED_2,    Result.PERM_DENIED_2,    Result.GOOD,             Result.PERM_DENIED_2,    Result.PERM_DENIED_2,    Result.GOOD),
            ADMIN_DELETE_DOMAIN_REQUEST("DELETE_DOMAIN_REQUEST",                           Result.PERM_DENIED_2,    Result.PERM_DENIED_2,    Result.DOMAIN_NOT_EMPTY, Result.PERM_DENIED_2,    Result.PERM_DENIED_2,    Result.DOMAIN_NOT_EMPTY),  // global admin should be able to delete domain if it is suspended??? TODO
            ADMIN_CREATE_COS_REQUEST("CREATE_COS_REQUEST",                                 Result.PERM_DENIED_2,    Result.PERM_DENIED_2,    Result.GOOD,             Result.PERM_DENIED_2,    Result.PERM_DENIED_2,    Result.GOOD),
            ADMIN_LAST_COMMAND();
            
            private String mFuncName;
            private Result[][] mExpectedResult;
            
            // for markers
            Command() {}

            
            Command(String funcName, 
                    Result activeUser,    Result activeDomainAdmin,    Result activeGlobalAdmin,
                    Result suspendedUser, Result suspendedDomainAdmin, Result suspendedGlobalAdmin) {
                mFuncName = funcName;
                mExpectedResult = new Result[2][AccountType.values().length];
                
                mExpectedResult[0][AccountType.ACCT_USER.ordinal()] = activeUser;
                mExpectedResult[0][AccountType.ACCT_DOMAIN_ADMIN.ordinal()] = activeDomainAdmin;
                mExpectedResult[0][AccountType.ACCT_GLOBAL_ADMIN.ordinal()] = activeGlobalAdmin;
                mExpectedResult[1][AccountType.ACCT_USER.ordinal()] = suspendedUser;
                mExpectedResult[1][AccountType.ACCT_DOMAIN_ADMIN.ordinal()] = suspendedDomainAdmin;
                mExpectedResult[1][AccountType.ACCT_GLOBAL_ADMIN.ordinal()] = suspendedGlobalAdmin;
            }
            
            Result expectedResult(boolean suspended, AccountType acctType) {
                int idx = (suspended)? 1 : 0;
                return mExpectedResult[idx][acctType.ordinal()];
            }
              
        }
        
        /*
        dispatcher.registerHandler(AdminConstants.GET_COS_REQUEST, new GetCos());
        dispatcher.registerHandler(AdminConstants.GET_ALL_COS_REQUEST, new GetAllCos());
        dispatcher.registerHandler(AdminConstants.MODIFY_COS_REQUEST, new ModifyCos());
        dispatcher.registerHandler(AdminConstants.DELETE_COS_REQUEST, new DeleteCos());
        dispatcher.registerHandler(AdminConstants.RENAME_COS_REQUEST, new RenameCos());

        dispatcher.registerHandler(AdminConstants.CREATE_SERVER_REQUEST, new CreateServer());
        dispatcher.registerHandler(AdminConstants.GET_SERVER_REQUEST, new GetServer());
        dispatcher.registerHandler(AdminConstants.GET_ALL_SERVERS_REQUEST, new GetAllServers());
        dispatcher.registerHandler(AdminConstants.MODIFY_SERVER_REQUEST, new ModifyServer());
        dispatcher.registerHandler(AdminConstants.DELETE_SERVER_REQUEST, new DeleteServer());

        dispatcher.registerHandler(AdminConstants.GET_CONFIG_REQUEST, new GetConfig());
        dispatcher.registerHandler(AdminConstants.GET_ALL_CONFIG_REQUEST, new GetAllConfig());
        dispatcher.registerHandler(AdminConstants.MODIFY_CONFIG_REQUEST, new ModifyConfig());

        dispatcher.registerHandler(AdminConstants.GET_SERVICE_STATUS_REQUEST, new GetServiceStatus());

        dispatcher.registerHandler(AdminConstants.PURGE_MESSAGES_REQUEST, new PurgeMessages());
        dispatcher.registerHandler(AdminConstants.DELETE_MAILBOX_REQUEST, new DeleteMailbox());
        dispatcher.registerHandler(AdminConstants.GET_MAILBOX_REQUEST, new GetMailbox());

        dispatcher.registerHandler(AdminConstants.MAINTAIN_TABLES_REQUEST, new MaintainTables());

        dispatcher.registerHandler(AdminConstants.RUN_UNIT_TESTS_REQUEST, new RunUnitTests());

        dispatcher.registerHandler(AdminConstants.CHECK_AUTH_CONFIG_REQUEST, new CheckAuthConfig());
        dispatcher.registerHandler(AdminConstants.CHECK_GAL_CONFIG_REQUEST, new CheckGalConfig());
        dispatcher.registerHandler(AdminConstants.CHECK_HOSTNAME_RESOLVE_REQUEST, new CheckHostnameResolve());

        dispatcher.registerHandler(AdminConstants.CREATE_VOLUME_REQUEST, new CreateVolume());
        dispatcher.registerHandler(AdminConstants.GET_VOLUME_REQUEST, new GetVolume());
        dispatcher.registerHandler(AdminConstants.GET_ALL_VOLUMES_REQUEST, new GetAllVolumes());
        dispatcher.registerHandler(AdminConstants.MODIFY_VOLUME_REQUEST, new ModifyVolume());
        dispatcher.registerHandler(AdminConstants.DELETE_VOLUME_REQUEST, new DeleteVolume());
        dispatcher.registerHandler(AdminConstants.GET_CURRENT_VOLUMES_REQUEST, new GetCurrentVolumes());
        dispatcher.registerHandler(AdminConstants.SET_CURRENT_VOLUME_REQUEST, new SetCurrentVolume());

        dispatcher.registerHandler(AdminConstants.CREATE_DISTRIBUTION_LIST_REQUEST, new CreateDistributionList());
        dispatcher.registerHandler(AdminConstants.GET_DISTRIBUTION_LIST_REQUEST, new GetDistributionList());
        dispatcher.registerHandler(AdminConstants.GET_ALL_DISTRIBUTION_LISTS_REQUEST, new GetAllDistributionLists());
        dispatcher.registerHandler(AdminConstants.ADD_DISTRIBUTION_LIST_MEMBER_REQUEST, new AddDistributionListMember());
        dispatcher.registerHandler(AdminConstants.REMOVE_DISTRIBUTION_LIST_MEMBER_REQUEST, new RemoveDistributionListMember());
        dispatcher.registerHandler(AdminConstants.MODIFY_DISTRIBUTION_LIST_REQUEST, new ModifyDistributionList());
        dispatcher.registerHandler(AdminConstants.DELETE_DISTRIBUTION_LIST_REQUEST, new DeleteDistributionList());
        dispatcher.registerHandler(AdminConstants.ADD_DISTRIBUTION_LIST_ALIAS_REQUEST, new AddDistributionListAlias());
        dispatcher.registerHandler(AdminConstants.REMOVE_DISTRIBUTION_LIST_ALIAS_REQUEST, new RemoveDistributionListAlias());
        dispatcher.registerHandler(AdminConstants.RENAME_DISTRIBUTION_LIST_REQUEST, new RenameDistributionList());
        dispatcher.registerHandler(AdminConstants.GET_DISTRIBUTION_LIST_MEMBERSHIP_REQUEST, new GetDistributionListMembership());

        dispatcher.registerHandler(AdminConstants.GET_VERSION_INFO_REQUEST, new GetVersionInfo());
        dispatcher.registerHandler(AdminConstants.GET_LICENSE_INFO_REQUEST, new GetLicenseInfo());

        dispatcher.registerHandler(AdminConstants.REINDEX_REQUEST, new ReIndex());

        // zimlet
        dispatcher.registerHandler(AdminConstants.GET_ZIMLET_REQUEST, new GetZimlet());
        dispatcher.registerHandler(AdminConstants.CREATE_ZIMLET_REQUEST, new CreateZimlet());
        dispatcher.registerHandler(AdminConstants.DELETE_ZIMLET_REQUEST, new DeleteZimlet());
        dispatcher.registerHandler(AdminConstants.GET_ADMIN_EXTENSION_ZIMLETS_REQUEST, new GetAdminExtensionZimlets());
        dispatcher.registerHandler(AdminConstants.GET_ZIMLET_STATUS_REQUEST, new GetZimletStatus());
        dispatcher.registerHandler(AdminConstants.GET_ALL_ZIMLETS_REQUEST, new GetAllZimlets());
        dispatcher.registerHandler(AdminConstants.DEPLOY_ZIMLET_REQUEST, new DeployZimlet());
        dispatcher.registerHandler(AdminConstants.UNDEPLOY_ZIMLET_REQUEST, new UndeployZimlet());
        dispatcher.registerHandler(AdminConstants.CONFIGURE_ZIMLET_REQUEST, new ConfigureZimlet());
        dispatcher.registerHandler(AdminConstants.MODIFY_ZIMLET_REQUEST, new ModifyZimlet());
        dispatcher.registerHandler(AdminConstants.DUMP_SESSIONS_REQUEST, new DumpSessions());
        dispatcher.registerHandler(AdminConstants.GET_SESSIONS_REQUEST, new GetSessions());

        // calendar resources
        dispatcher.registerHandler(AdminConstants.CREATE_CALENDAR_RESOURCE_REQUEST,   new CreateCalendarResource());
        dispatcher.registerHandler(AdminConstants.DELETE_CALENDAR_RESOURCE_REQUEST,   new DeleteCalendarResource());
        dispatcher.registerHandler(AdminConstants.MODIFY_CALENDAR_RESOURCE_REQUEST,   new ModifyCalendarResource());
        dispatcher.registerHandler(AdminConstants.RENAME_CALENDAR_RESOURCE_REQUEST,   new RenameCalendarResource());
        dispatcher.registerHandler(AdminConstants.GET_CALENDAR_RESOURCE_REQUEST,      new GetCalendarResource());
        dispatcher.registerHandler(AdminConstants.GET_ALL_CALENDAR_RESOURCES_REQUEST, new GetAllCalendarResources());
        dispatcher.registerHandler(AdminConstants.SEARCH_CALENDAR_RESOURCES_REQUEST,  new SearchCalendarResources());

        // QUOTA
        dispatcher.registerHandler(AdminConstants.GET_QUOTA_USAGE_REQUEST, new GetQuotaUsage());

        // Mail queue management
        dispatcher.registerHandler(AdminConstants.GET_MAIL_QUEUE_INFO_REQUEST, new GetMailQueueInfo());
        dispatcher.registerHandler(AdminConstants.GET_MAIL_QUEUE_REQUEST, new GetMailQueue());
        dispatcher.registerHandler(AdminConstants.MAIL_QUEUE_ACTION_REQUEST, new MailQueueAction());
        dispatcher.registerHandler(AdminConstants.MAIL_QUEUE_FLUSH_REQUEST, new MailQueueFlush());

        dispatcher.registerHandler(AdminConstants.INIT_NOTEBOOK_REQUEST, new InitNotebook());

        dispatcher.registerHandler(AdminConstants.AUTO_COMPLETE_GAL_REQUEST, new AutoCompleteGal());
        dispatcher.registerHandler(AdminConstants.SEARCH_GAL_REQUEST, new SearchGal());

        // throttling
        dispatcher.registerHandler(AdminConstants.SET_THROTTLE_REQUEST, new SetThrottle());

        // data source
        dispatcher.registerHandler(AdminConstants.GET_DATA_SOURCES_REQUEST, new GetDataSources());
        dispatcher.registerHandler(AdminConstants.CREATE_DATA_SOURCE_REQUEST, new CreateDataSource());
        dispatcher.registerHandler(AdminConstants.MODIFY_DATA_SOURCE_REQUEST, new ModifyDataSource());
        dispatcher.registerHandler(AdminConstants.DELETE_DATA_SOURCE_REQUEST, new DeleteDataSource());

        // calendar time zone fixup
        dispatcher.registerHandler(AdminConstants.FIX_CALENDAR_TIME_ZONE_REQUEST, new FixCalendarTimeZone());
        
        // admin saved searches
        dispatcher.registerHandler(AdminConstants.GET_ADMIN_SAVED_SEARCHES_REQUEST, new GetAdminSavedSearches());
        dispatcher.registerHandler(AdminConstants.MODIFY_ADMIN_SAVED_SEARCHES_REQUEST, new ModifyAdminSavedSearches());
        
        dispatcher.registerHandler(AdminConstants.ADD_ACCOUNT_LOGGER_REQUEST, new AddAccountLogger());
        dispatcher.registerHandler(AdminConstants.REMOVE_ACCOUNT_LOGGER_REQUEST, new RemoveAccountLogger());
        dispatcher.registerHandler(AdminConstants.GET_ACCOUNT_LOGGERS_REQUEST, new GetAccountLoggers());
        dispatcher.registerHandler(AdminConstants.GET_ALL_ACCOUNT_LOGGERS_REQUEST, new GetAllAccountLoggers());
        
        dispatcher.registerHandler(AdminConstants.CHECK_DIRECTORY_REQUEST, new CheckDirectory());
        
        dispatcher.registerHandler(AdminConstants.FLUSH_CACHE_REQUEST, new FlushCache());
         */
        
        public static void run(SoapTestContext ctx) throws Exception {
            SoapCommands cmds = new SoapCommands(ctx);
           
            Class cls = cmds.getClass();
            for (Command c : Command.values()) {
                
                System.out.println((ctx.mSuspended?"suspended ":"active ") + ctx.mAuthedAcctType.name() + ": " + c.name());
                
                if (c.mFuncName == null)
                    continue;
                
                /*
                if (authedAcctType == AccountType.ACCT_USER && c.name().startsWith("ADMIN"))
                    continue;
                */
                
                Method method = cls.getMethod(c.mFuncName);
                 
                Result expectedResult = c.expectedResult(ctx.mSuspended, ctx.mAuthedAcctType);
                Result actualResult = null;
                
                try {
                    method.invoke(cmds);
                    actualResult = Result.GOOD;
                } catch (InvocationTargetException e) {
                    actualResult = Result.BAD;
                    Throwable te = e.getTargetException();
                    if (te instanceof ServiceException) {
                        ServiceException se = (ServiceException)te;
                        actualResult = Result.toResult(se);
                    } else {
                        te.printStackTrace();
                    }
                   
                }
                
                if (expectedResult != Result.KNOWN_BUG)
                    assertEquals(c.name(), expectedResult, actualResult);
             }
        }
    }
    
    private static class SoapClient extends SoapProvisioning {
              
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
        
        static SoapClient newInstance(Account acct, AccountType at) throws Exception {
            
            SoapClient soapClient = new SoapClient();
            if (at == AccountType.ACCT_USER) {
                // soapClient.soapSetURI(TestUtil.getSoapUrl());
                soapClient.soapSetURI(TestUtil.getAdminSoapUrl());
                // soapClient.authAccount(acct, PASSWORD, "DomainStatusTest");
                soapClient.soapAuthenticate(acct.getName(), PASSWORD);
            } else {
                soapClient.soapSetURI(TestUtil.getAdminSoapUrl());
                soapClient.soapAdminAuthenticate(acct.getName(), PASSWORD);
            }
                
            return soapClient;
        }
        
        protected synchronized Element invokeOnTargetAccount(Element request, String targetId) throws ServiceException {
            return super.invokeOnTargetAccount(request, targetId);
        }
    }
    
    String[] createTesters(DomainStatus ds, AccountType at) throws Exception {
        String[] testers = new String[SoapCommands.TesterObjs.values().length];
        
        String objSuffix = "tester-" + ds.name() + "-" + at.name();
        
        String name = ACCOUNT_NAME("acct-" + objSuffix);
        testers[SoapCommands.TesterObjs.TESTER_ID_ACCOUNT_FOR_DELETE_ACCOUNT_REQUEST.ordinal()] = createAccount(name, AccountType.ACCT_USER).getId();
        
        return testers;
    }
    
    private void suspendedDomainTest() throws Exception {
        Domain domain = getDomain();
        String domainId = domain.getId();
        
        Map attrs = new HashMap<String, String>();
        attrs.put(Provisioning.A_zimbraDomainStatus, DomainStatus.active.name());
        mProv.modifyAttrs(domain, attrs);
                
        // auth to user, domain admin and global admin before we suspend the domain
        Account user = createAccount(ACCOUNT_NAME("user"), AccountType.ACCT_USER);
        String[] testersActiveUser = createTesters(DomainStatus.active, AccountType.ACCT_USER);
        String[] testersSuspendedUser = createTesters(DomainStatus.suspended, AccountType.ACCT_USER);
        SoapClient clientUser = SoapClient.newInstance(user, AccountType.ACCT_USER);
        
        Account domainAdmin = createAccount(ACCOUNT_NAME("domain-admin"), AccountType.ACCT_DOMAIN_ADMIN);
        String[] testersActiveDomainAdmin = createTesters(DomainStatus.active, AccountType.ACCT_DOMAIN_ADMIN);
        String[] testersSuspendedDomainAdmin = createTesters(DomainStatus.suspended, AccountType.ACCT_DOMAIN_ADMIN);
        SoapClient clientDomainAdmin = SoapClient.newInstance(domainAdmin, AccountType.ACCT_DOMAIN_ADMIN);
        
        Account globalAdmin = createAccount(ACCOUNT_NAME("global-admin"), AccountType.ACCT_GLOBAL_ADMIN);
        String[] testersActiveGlobalAdmin = createTesters(DomainStatus.active, AccountType.ACCT_GLOBAL_ADMIN);
        String[] testersSuspendedGlobalAdmin = createTesters(DomainStatus.suspended, AccountType.ACCT_GLOBAL_ADMIN);
        SoapClient clientGlobalAdmin = SoapClient.newInstance(globalAdmin, AccountType.ACCT_GLOBAL_ADMIN);
        
        attrs.clear();
        attrs.put(Provisioning.A_zimbraDomainStatus, DomainStatus.suspended.name());
        mProv.modifyAttrs(domain, attrs);
        SoapCommands.run(new SoapTestContext(clientUser, AccountType.ACCT_USER, user, true, DOMAIN_NAME, domainId, testersSuspendedUser, TEST_ID));
        SoapCommands.run(new SoapTestContext(clientDomainAdmin, AccountType.ACCT_DOMAIN_ADMIN, user, true, DOMAIN_NAME, domainId, testersSuspendedDomainAdmin, TEST_ID));
        SoapCommands.run(new SoapTestContext(clientGlobalAdmin, AccountType.ACCT_GLOBAL_ADMIN, user, true, DOMAIN_NAME, domainId, testersSuspendedGlobalAdmin, TEST_ID));
        
        attrs.clear();
        attrs.put(Provisioning.A_zimbraDomainStatus, DomainStatus.active.name());
        mProv.modifyAttrs(domain, attrs);
        SoapCommands.run(new SoapTestContext(clientUser, AccountType.ACCT_USER, user, false, DOMAIN_NAME, domainId, testersActiveUser, TEST_ID));
        SoapCommands.run(new SoapTestContext(clientDomainAdmin, AccountType.ACCT_DOMAIN_ADMIN, user, false, DOMAIN_NAME, domainId, testersActiveDomainAdmin, TEST_ID));
        SoapCommands.run(new SoapTestContext(clientGlobalAdmin, AccountType.ACCT_GLOBAL_ADMIN, user, false, DOMAIN_NAME, domainId, testersActiveGlobalAdmin, TEST_ID));
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

// TODO
// - ModifyDomain with zimbraDomainStatus
// - DeleteDomain while is suspended status