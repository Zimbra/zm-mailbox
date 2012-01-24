/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012 Zimbra, Inc.
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
package com.zimbra.qa.unittest.prov.soap;

import java.util.Map;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

import com.google.common.collect.Maps;
import com.zimbra.common.account.Key.GranteeBy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.SoapTransport;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.CalendarResource;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.GranteeType;
import com.zimbra.cs.account.accesscontrol.Right;
import com.zimbra.cs.account.accesscontrol.TargetType;
import com.zimbra.cs.account.accesscontrol.Rights.User;
import com.zimbra.soap.admin.message.SetPasswordRequest;
import com.zimbra.soap.admin.message.SetPasswordResponse;
import com.zimbra.soap.type.TargetBy;

public class TestSetPassword extends SoapTest {
    private static SoapProvTestUtil provUtil;
    private static Provisioning prov;
    private static Domain domain;
    
    private static final int MIN_PASSSWORD_LEN = 5;
    private static final Sequencer goodPassword = new Sequencer();
    private static final Sequencer badPassword = new Sequencer();
        
    @BeforeClass
    public static void init() throws Exception {
        provUtil = new SoapProvTestUtil();
        prov = provUtil.getProv();
        domain = provUtil.createDomain(baseDomainName());
    }
        
    @AfterClass
    public static void cleanup() throws Exception {
        Cleanup.deleteAll(baseDomainName());
    }
    
    private static void setPasswordPolicy(Account acct) throws Exception {
        Map<String, Object> attrs = Maps.newHashMap();
        attrs.put(Provisioning.A_zimbraPasswordMinLength, "" + MIN_PASSSWORD_LEN);
        prov.modifyAttrs(acct, attrs);
    }
    
    /*
     * generate a password that is at least MIN_PASSSWORD_LEN chars
     */
    private String genGoodPassword() {
        String password = "good-" + goodPassword.next();
        assertTrue(password.length() >= MIN_PASSSWORD_LEN);
        return password;
    }
    
    /*
     * generate a password that is shorter than 5 chars
     */
    private String genBadPassword() {
        String password = "b-" + badPassword.next();
        assertTrue(password.length() < MIN_PASSSWORD_LEN);
        return password;
    }
    
    private String setPassword(Account authedAcct, Account targetAcct, boolean againstPolicy) 
    throws Exception {
        String newPassword = againstPolicy ? genBadPassword() : genGoodPassword();
        
        SoapTransport transport = authAdmin(authedAcct.getName());
        SetPasswordRequest req = new SetPasswordRequest(targetAcct.getId(), newPassword);
        SetPasswordResponse resp = invokeJaxb(transport, req);
        
        return newPassword;
    }
    
    private Account createAcctAndSetPasswordpolicy() throws Exception {
        Account acct = provUtil.createAccount(genAcctNameLocalPart("user"), domain);
        setPasswordPolicy(acct);
        return acct;
    }
    
    private CalendarResource createCRAndSetPasswordpolicy() throws Exception {
        CalendarResource cr = provUtil.createCalendarResource(genCalendarResourceNameLocalPart(), domain);
        setPasswordPolicy(cr);
        return cr;
    }
    
    private void verifyOK(Account adminAcct, Account userAccount, boolean againstPolicy) 
    throws Exception {
        String newPassword = setPassword(adminAcct, userAccount, againstPolicy);
        authUser(userAccount.getName(), newPassword); // make sure new password works
    }
    
    private void verifyPermDenied(Account adminAcct, Account userAccount, boolean againstPolicy) 
    throws Exception {
        String errorCode = null;
        try {
            setPassword(adminAcct, userAccount, againstPolicy);
        } catch (ServiceException e) {
            errorCode = e.getCode();
        }
        assertEquals(ServiceException.PERM_DENIED, errorCode);
    }
    
    private void verifyInvalidPassword(Account adminAcct, Account userAccount, boolean againstPolicy) 
    throws Exception {
        String errorCode = null;
        try {
            setPassword(adminAcct, userAccount, againstPolicy);
        } catch (ServiceException e) {
            errorCode = e.getCode();
        }
        assertEquals(AccountServiceException.INVALID_PASSWORD, errorCode);
    }
    
    @Test
    public void globalAdmin() throws Exception {
        Account acct = createAcctAndSetPasswordpolicy();
        CalendarResource cr = createCRAndSetPasswordpolicy();
        Account adminAcct = provUtil.createGlobalAdmin(genAcctNameLocalPart("admin"), domain);
        
        verifyOK(adminAcct, acct, true);
        verifyOK(adminAcct, acct, false);
        verifyOK(adminAcct, cr, true);
        verifyOK(adminAcct, cr, false);
        
        provUtil.deleteAccount(acct);
        provUtil.deleteAccount(cr);
        provUtil.deleteAccount(adminAcct);
    }
    
    @Test
    public void delegatedAdminWithNoRight() throws Exception {
        Account acct = createAcctAndSetPasswordpolicy();
        CalendarResource cr = createCRAndSetPasswordpolicy();
        Account adminAcct = provUtil.createDelegatedAdmin(genAcctNameLocalPart("admin"), domain);
       
        verifyPermDenied(adminAcct, acct, true);
        verifyPermDenied(adminAcct, acct, false);
        verifyPermDenied(adminAcct, cr, true);
        verifyPermDenied(adminAcct, cr, false);
        
        provUtil.deleteAccount(acct);
        provUtil.deleteAccount(cr);
        provUtil.deleteAccount(adminAcct);
    }
    
    @Test
    public void delegatedAdminWithSetPasswordRight() throws Exception {
        Account acct = createAcctAndSetPasswordpolicy();
        CalendarResource cr = createCRAndSetPasswordpolicy();
        Account adminAcct = provUtil.createDelegatedAdmin(genAcctNameLocalPart("admin"), domain);
        
        prov.grantRight(TargetType.account.getCode(), TargetBy.name, acct.getName(), 
                GranteeType.GT_USER.getCode(), GranteeBy.name, adminAcct.getName(), null, 
                Right.RT_setAccountPassword, null);
        prov.grantRight(TargetType.calresource.getCode(), TargetBy.name, cr.getName(), 
                GranteeType.GT_USER.getCode(), GranteeBy.name, adminAcct.getName(), null, 
                Right.RT_setCalendarResourcePassword, null);
        
        verifyOK(adminAcct, acct, true);
        verifyOK(adminAcct, acct, false);
        verifyOK(adminAcct, cr, true);
        verifyOK(adminAcct, cr, false);
        
        provUtil.deleteAccount(acct);
        provUtil.deleteAccount(cr);
        provUtil.deleteAccount(adminAcct);
    }
    
    @Test
    public void delegatedAdminWithChangePasswordRight() throws Exception {
        Account acct = createAcctAndSetPasswordpolicy();
        CalendarResource cr = createCRAndSetPasswordpolicy();
        Account adminAcct = provUtil.createDelegatedAdmin(genAcctNameLocalPart("admin"), domain);
        
        prov.grantRight(TargetType.account.getCode(), TargetBy.name, acct.getName(), 
                GranteeType.GT_USER.getCode(), GranteeBy.name, adminAcct.getName(), null, 
                Right.RT_changeAccountPassword, null);
        prov.grantRight(TargetType.calresource.getCode(), TargetBy.name, cr.getName(), 
                GranteeType.GT_USER.getCode(), GranteeBy.name, adminAcct.getName(), null, 
                Right.RT_changeCalendarResourcePassword, null);
        
        verifyInvalidPassword(adminAcct, acct, true);
        verifyOK(adminAcct, acct, false);
        verifyInvalidPassword(adminAcct, cr, true);
        verifyOK(adminAcct, cr, false);
        
        provUtil.deleteAccount(acct);
        provUtil.deleteAccount(cr);
        provUtil.deleteAccount(adminAcct);
    }
}
