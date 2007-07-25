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
import com.zimbra.cs.account.Signature;
import com.zimbra.cs.account.ldap.LdapProvisioning;
import com.zimbra.cs.account.soap.SoapProvisioning;
import com.zimbra.cs.lmtpserver.LmtpProtocolException;
import com.zimbra.cs.service.admin.*;

/*
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
*/

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
    
    static class SoapCommands {
        protected SoapTester mTester;
        protected AccountType mAuthedAcctType;
        protected Account mTargetAcct;
        protected boolean mSuspended;
        
        // signature APIs only takes id, not name, 
        // remember the signature ids, we need them for modify/delete signatures
        private Map<String, String> mSignatureNameIdMap;
        
        SoapCommands(SoapTester tester, AccountType authedAcctType, 
                     Account targetAcct, boolean suspended) {
            
            mTester = tester;
            mAuthedAcctType = authedAcctType;
            mTargetAcct = targetAcct;
            mSuspended = suspended;
            
            mSignatureNameIdMap = new HashMap<String, String>();
        }
        
        private String identityName() {
            return "identity-of-" + mAuthedAcctType.name();
        }
        
        private String signatureName() {
            return "signature-of-" + mAuthedAcctType.name();
        }
        
        private void setSignatureIdByName(String sigName, String sigId) {
            mSignatureNameIdMap.put(sigName, sigId);
        }
        
        private String getSignatureIdByName(String sigName) {
            return mSignatureNameIdMap.get(sigName);
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
            mTester.createIdentity(mTargetAcct, identityName(), new HashMap<String, Object>()) ;
        }
        
        public void MODIFY_IDENTITY_REQUEST() throws Exception {
            mTester.modifyIdentity(mTargetAcct, identityName(), new HashMap<String, Object>()) ;
        }
        
        public void DELETE_IDENTITY_REQUEST() throws Exception {
            mTester.deleteIdentity(mTargetAcct, identityName()) ;
        }
        
        public void GET_IDENTITIES_REQUEST() throws Exception {
            mTester.getAllIdentities(mTargetAcct) ;
        }

        public void CREATE_SIGNATURE_REQUEST() throws Exception {
            Signature sig = mTester.createSignature(mTargetAcct, signatureName(), new HashMap<String, Object>());
            setSignatureIdByName(sig.getName(), sig.getId());
        }
        
        public void MODIFY_SIGNATURE_REQUEST() throws Exception {
            String sigId = getSignatureIdByName(signatureName());
            mTester.modifySignature(mTargetAcct, sigId, new HashMap<String, Object>()) ;
        }
        
        public void DELETE_SIGNATURE_REQUEST() throws Exception {
            String sigId = getSignatureIdByName(signatureName());
            mTester.deleteSignature(mTargetAcct, sigId) ;
        }
        
        public void GET_SIGNATURES_REQUEST() throws Exception {
            mTester.getAllSignatures(mTargetAcct) ;
        }
        
        public void PING_REQUEST() throws Exception {
            XMLElement req = new XMLElement(AdminConstants.PING_REQUEST);
            mTester.invoke(req);
        }
        
        public void CHECK_HEALTH_REQUEST() throws Exception {
            mTester.healthCheck();
        }
        
        public void ADMIN_AUTH_REQUEST() throws Exception {
            XMLElement req = new XMLElement(AdminConstants.AUTH_REQUEST);
            req.addElement(AdminConstants.E_NAME).setText(mTargetAcct.getName());
            req.addElement(AdminConstants.E_PASSWORD).setText(PASSWORD);
            mTester.invoke(req);
        }
        
        private static enum Result {
            GOOD("G_O_O_D", "G_O_O_D"),
            BAD("B_A_D", "B_A_D"),
            
            AUTH_EXPIRED(ServiceException.AUTH_EXPIRED, "auth credentials have expired"),
            MAINTENANCE_MODE(AccountServiceException.MAINTENANCE_MODE, "account is in maintenance mode"),
            PERM_DENIED(ServiceException.PERM_DENIED, "permission denied: not an admin account");
            
            String mCode;
            String mMsg;
            
            private static class R {
                static Map<String, Result> sMap = new HashMap<String, Result>();
                static String getKey(String code, String msg) {
                    return "[" +code + "]" + "[" + msg + "]";
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
                    
                String key = R.getKey(code, msg);
                Result r = R.sMap.get(key);
                assertNotNull(key, r);  // unknown exception
                
                return r;
            }
        }

        private static enum Command {
            
            ACCOUNT_FIRST_COMMAND(),
            ACCOUNT_AUTH_REQUEST("AUTH_REQUEST",                                           Result.GOOD, Result.GOOD, Result.GOOD, Result.MAINTENANCE_MODE, Result.MAINTENANCE_MODE, Result.MAINTENANCE_MODE),
            ACCOUNT_CHANGE_PASSWORD_REQUEST("CHANGE_PASSWORD_REQUEST",                     Result.GOOD, Result.GOOD, Result.GOOD, Result.MAINTENANCE_MODE, Result.MAINTENANCE_MODE, Result.MAINTENANCE_MODE),
            ACCOUNT_GET_PREFS_REQUEST("GET_PREFS_REQUEST",                                 Result.GOOD, Result.GOOD, Result.GOOD, Result.AUTH_EXPIRED, Result.AUTH_EXPIRED, Result.AUTH_EXPIRED),
            ACCOUNT_MODIFY_PREFS_REQUEST("MODIFY_PREFS_REQUEST",                           Result.GOOD, Result.GOOD, Result.GOOD, Result.AUTH_EXPIRED, Result.AUTH_EXPIRED, Result.AUTH_EXPIRED),
            ACCOUNT_GET_INFO_REQUEST("GET_INFO_REQUEST",                                   Result.GOOD, Result.GOOD, Result.GOOD, Result.AUTH_EXPIRED, Result.AUTH_EXPIRED, Result.AUTH_EXPIRED),
            ACCOUNT_GET_ACCOUNT_INFO_REQUEST("GET_ACCOUNT_INFO_REQUEST",                   Result.GOOD, Result.GOOD, Result.GOOD, Result.AUTH_EXPIRED, Result.AUTH_EXPIRED, Result.AUTH_EXPIRED),
            ACCOUNT_SEARCH_GAL_REQUEST("SEARCH_GAL_REQUEST",                               Result.GOOD, Result.GOOD, Result.GOOD, Result.AUTH_EXPIRED, Result.AUTH_EXPIRED, Result.AUTH_EXPIRED),
            ACCOUNT_AUTO_COMPLETE_GAL_REQUEST("AUTO_COMPLETE_GAL_REQUEST",                 Result.GOOD, Result.GOOD, Result.GOOD, Result.AUTH_EXPIRED, Result.AUTH_EXPIRED, Result.AUTH_EXPIRED),
            ACCOUNT_SYNC_GAL_REQUEST("SYNC_GAL_REQUEST",                                   Result.GOOD, Result.GOOD, Result.GOOD, Result.AUTH_EXPIRED, Result.AUTH_EXPIRED, Result.AUTH_EXPIRED),
            ACCOUNT_SEARCH_CALENDAR_RESOURCES_REQUEST("SEARCH_CALENDAR_RESOURCES_REQUEST", Result.GOOD, Result.GOOD, Result.GOOD, Result.AUTH_EXPIRED, Result.AUTH_EXPIRED, Result.AUTH_EXPIRED),
            ACCOUNT_MODIFY_PROPERTIES_REQUEST("MODIFY_PROPERTIES_REQUEST",                 Result.GOOD, Result.GOOD, Result.GOOD, Result.AUTH_EXPIRED, Result.AUTH_EXPIRED, Result.AUTH_EXPIRED),
            ACCOUNT_GET_ALL_LOCALES_REQUEST("GET_ALL_LOCALES_REQUEST",                     Result.GOOD, Result.GOOD, Result.GOOD, Result.AUTH_EXPIRED, Result.AUTH_EXPIRED, Result.AUTH_EXPIRED),
            ACCOUNT_GET_AVAILABLE_LOCALES_REQUEST("GET_AVAILABLE_LOCALES_REQUEST",         Result.GOOD, Result.GOOD, Result.GOOD, Result.AUTH_EXPIRED, Result.AUTH_EXPIRED, Result.AUTH_EXPIRED),
            ACCOUNT_GET_AVAILABLE_SKINS_REQUEST("GET_AVAILABLE_SKINS_REQUEST",             Result.GOOD, Result.GOOD, Result.GOOD, Result.AUTH_EXPIRED, Result.AUTH_EXPIRED, Result.AUTH_EXPIRED),
            ACCOUNT_CREATE_IDENTITY_REQUEST("CREATE_IDENTITY_REQUEST",                     Result.GOOD, Result.GOOD, Result.GOOD, Result.AUTH_EXPIRED, Result.AUTH_EXPIRED, Result.AUTH_EXPIRED),
            ACCOUNT_MODIFY_IDENTITY_REQUEST("MODIFY_IDENTITY_REQUEST",                     Result.GOOD, Result.GOOD, Result.GOOD, Result.AUTH_EXPIRED, Result.AUTH_EXPIRED, Result.AUTH_EXPIRED),
            ACCOUNT_DELETE_IDENTITY_REQUEST("DELETE_IDENTITY_REQUEST",                     Result.GOOD, Result.GOOD, Result.GOOD, Result.AUTH_EXPIRED, Result.AUTH_EXPIRED, Result.AUTH_EXPIRED),
            ACCOUNT_GET_IDENTITIES_REQUEST("GET_IDENTITIES_REQUEST",                       Result.GOOD, Result.GOOD, Result.GOOD, Result.AUTH_EXPIRED, Result.AUTH_EXPIRED, Result.AUTH_EXPIRED),
            ACCOUNT_CREATE_SIGNATURE_REQUEST("CREATE_SIGNATURE_REQUEST",                   Result.GOOD, Result.GOOD, Result.GOOD, Result.AUTH_EXPIRED, Result.AUTH_EXPIRED, Result.AUTH_EXPIRED),
            ACCOUNT_MODIFY_SIGNATURE_REQUEST("MODIFY_SIGNATURE_REQUEST",                   Result.GOOD, Result.GOOD, Result.GOOD, Result.AUTH_EXPIRED, Result.AUTH_EXPIRED, Result.AUTH_EXPIRED),
            ACCOUNT_DELETE_SIGNATURE_REQUEST("DELETE_SIGNATURE_REQUEST",                   Result.GOOD, Result.GOOD, Result.GOOD, Result.AUTH_EXPIRED, Result.AUTH_EXPIRED, Result.AUTH_EXPIRED),
            ACCOUNT_GET_SIGNATURES_REQUEST("GET_SIGNATURES_REQUEST",                       Result.GOOD, Result.GOOD, Result.GOOD, Result.AUTH_EXPIRED, Result.AUTH_EXPIRED, Result.AUTH_EXPIRED),
            ACCOUNT_LAST_COMMAND(),
           
            
            ADMIN_FIRST_COMMAND(),
            ADMIN_PING_REQUEST("PING_REQUEST",                                             Result.GOOD, Result.GOOD, Result.GOOD, Result.GOOD, Result.GOOD, Result.GOOD),
            ADMIN_CHECK_HEALTH_REQUEST("CHECK_HEALTH_REQUEST",                             Result.GOOD, Result.GOOD, Result.GOOD, Result.GOOD, Result.GOOD, Result.GOOD),
            // TODO, for now the target account is always a user account, should also test when the target is a domain admon/global admin
            ADMIN_AUTH_REQUEST("ADMIN_AUTH_REQUEST",                                       Result.PERM_DENIED, Result.PERM_DENIED, Result.PERM_DENIED, Result.MAINTENANCE_MODE, Result.MAINTENANCE_MODE, Result.MAINTENANCE_MODE),
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
        dispatcher.registerHandler(AdminConstants.PING_REQUEST, new Ping());
        dispatcher.registerHandler(AdminConstants.CHECK_HEALTH_REQUEST, new CheckHealth());

        dispatcher.registerHandler(AdminConstants.AUTH_REQUEST, new Auth());
        dispatcher.registerHandler(AdminConstants.CREATE_ACCOUNT_REQUEST, new CreateAccount());
        dispatcher.registerHandler(AdminConstants.DELEGATE_AUTH_REQUEST, new DelegateAuth());
        dispatcher.registerHandler(AdminConstants.GET_ACCOUNT_REQUEST, new GetAccount());
        dispatcher.registerHandler(AdminConstants.GET_ACCOUNT_INFO_REQUEST, new GetAccountInfo());
        dispatcher.registerHandler(AdminConstants.GET_ALL_ACCOUNTS_REQUEST, new GetAllAccounts());
        dispatcher.registerHandler(AdminConstants.GET_ALL_ADMIN_ACCOUNTS_REQUEST, new GetAllAdminAccounts());
        dispatcher.registerHandler(AdminConstants.MODIFY_ACCOUNT_REQUEST, new ModifyAccount());
        dispatcher.registerHandler(AdminConstants.DELETE_ACCOUNT_REQUEST, new DeleteAccount());
        dispatcher.registerHandler(AdminConstants.SET_PASSWORD_REQUEST, new SetPassword());
        dispatcher.registerHandler(AdminConstants.CHECK_PASSWORD_STRENGTH_REQUEST, new CheckPasswordStrength());
        dispatcher.registerHandler(AdminConstants.ADD_ACCOUNT_ALIAS_REQUEST, new AddAccountAlias());
        dispatcher.registerHandler(AdminConstants.REMOVE_ACCOUNT_ALIAS_REQUEST, new RemoveAccountAlias());
        dispatcher.registerHandler(AdminConstants.SEARCH_ACCOUNTS_REQUEST, new SearchAccounts());
        dispatcher.registerHandler(AdminConstants.RENAME_ACCOUNT_REQUEST, new RenameAccount());

        dispatcher.registerHandler(AdminConstants.SEARCH_DIRECTORY_REQUEST, new SearchDirectory());
        dispatcher.registerHandler(AdminConstants.GET_ACCOUNT_MEMBERSHIP_REQUEST, new GetAccountMembership());

        dispatcher.registerHandler(AdminConstants.CREATE_DOMAIN_REQUEST, new CreateDomain());
        dispatcher.registerHandler(AdminConstants.GET_DOMAIN_REQUEST, new GetDomain());
        dispatcher.registerHandler(AdminConstants.GET_DOMAIN_INFO_REQUEST, new GetDomainInfo());
        dispatcher.registerHandler(AdminConstants.GET_ALL_DOMAINS_REQUEST, new GetAllDomains());
        dispatcher.registerHandler(AdminConstants.MODIFY_DOMAIN_REQUEST, new ModifyDomain());
        dispatcher.registerHandler(AdminConstants.DELETE_DOMAIN_REQUEST, new DeleteDomain());

        dispatcher.registerHandler(AdminConstants.CREATE_COS_REQUEST, new CreateCos());
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
        
        public static void run(SoapTester tester, AccountType authedAcctType, 
                               Account targetAcct, boolean suspended) throws Exception {
            SoapCommands cmds = new SoapCommands(tester, authedAcctType, targetAcct, suspended);
           
            Class cls = cmds.getClass();
            for (Command c : Command.values()) {
                
                if (c.mFuncName == null)
                    continue;
                
                /*
                if (authedAcctType == AccountType.ACCT_USER && c.name().startsWith("ADMIN"))
                    continue;
                */
                
                Method method = cls.getMethod(c.mFuncName);
                 
                Result expectedResult = c.expectedResult(cmds.mSuspended, cmds.mAuthedAcctType);
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
                
                assertEquals(c.name(), expectedResult, actualResult);
             }
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
                // soapTester.soapSetURI(TestUtil.getSoapUrl());
                soapTester.soapSetURI(TestUtil.getAdminSoapUrl());
                soapTester.authAccount(acct, PASSWORD, "SoapTester");
                soapTester.soapAuthenticate(acct.getName(), PASSWORD);
            } else {
                soapTester.soapSetURI(TestUtil.getAdminSoapUrl());
                soapTester.soapAdminAuthenticate(acct.getName(), PASSWORD);
            }
                
            return soapTester;
        }
        
        protected synchronized Element invokeOnTargetAccount(Element request, String targetId) throws ServiceException {
            return super.invokeOnTargetAccount(request, targetId);
        }
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
        SoapCommands.run(testerUser, AccountType.ACCT_USER, user, true);
        SoapCommands.run(testerDomainAdmin, AccountType.ACCT_DOMAIN_ADMIN, user, true);
        SoapCommands.run(testerGlobalAdmin, AccountType.ACCT_GLOBAL_ADMIN, user, true);
        
        mProv.modifyDomainStatus(domain, DomainStatus.active.name());
        SoapCommands.run(testerUser, AccountType.ACCT_USER, user, false);
        SoapCommands.run(testerDomainAdmin, AccountType.ACCT_DOMAIN_ADMIN, user, false);
        SoapCommands.run(testerGlobalAdmin, AccountType.ACCT_GLOBAL_ADMIN, user, false);
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
