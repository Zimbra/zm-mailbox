/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.qa.unittest.prov.ldap;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.*;
import com.zimbra.cs.account.ldap.ChangePasswordListener;
import com.zimbra.cs.account.ldap.LdapProv;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

public class TestChangePasswordListener extends LdapTest {
    private static LdapProvTestUtil provUtil;
    private static Domain domain;
    private static Sequencer passGen = new Sequencer();


    private static LdapProv prov;

    @BeforeClass
    public static void init() throws Exception {
        provUtil = new LdapProvTestUtil();
        prov = provUtil.getProv();
    }

    @AfterClass
    public static void cleanup() throws Exception {
        Cleanup.deleteAll(baseDomainName());

    }

    private String getZimbraDomainName(String testName) {
        return testName + "." + baseDomainName();
    }

    private Domain createZimbraDomain(String testName, Map<String, Object> zimbraDomainAttrs)
            throws Exception {
        return provUtil.createDomain(getZimbraDomainName(testName), zimbraDomainAttrs);
    }


    /*
     * Test for onException method when exception is thrown at preModify
     */

    @Test
    public void onExceptionAtPreModify() throws Exception {
        String testName = getTestName();

        // get Test listener class name and register the class as ChangePasswordListener
        String className = TestListenerExceptionThrownAtPreModify.class.getName();
        TestListenerExceptionThrownAtPreModify listener = new TestListenerExceptionThrownAtPreModify();
        TestListenerExceptionThrownAtPreModify.register(className, listener);

        // create domain for which the class name is set to zimbraPasswordChangeListener attribute
        Map<String, Object> zimbraDomainAttrs = new HashMap<String, Object>();
        zimbraDomainAttrs.put(Provisioning.A_zimbraPasswordChangeListener, className);
        domain = createZimbraDomain(getZimbraDomainName(testName), zimbraDomainAttrs);

        // create account with old password
        String acctName = testName + "@" + domain.getDomainName();
        String oldpassword = "old_password_" + passGen.next();
        Account acct = provUtil.createAccount(acctName,oldpassword);

        // change password with new password
        String newpassword = "new_password_" + passGen.next();
        try {
            prov.setPassword(acct,newpassword);
            fail();  // if there is no exception taken place in setPassword().
        } catch (Exception e){
            // make sure if caught exception is thrown at preModify()
            assertTrue(e.getMessage().equals(ServiceException.TEMPORARILY_UNAVAILABLE().getMessage()));
        }


        assertNotNull(listener);
        assertTrue(listener.isPreModifyCalled());
        assertFalse(listener.isPostModifyCalled());
        // onException should not be called
        assertFalse(listener.isOnExceptionCalled());

    }

    /*
     *  Mock listener class implementing ChangePasswordListener
     *                        and throwing Exception from preModify()
     */
    public class TestListenerExceptionThrownAtPreModify extends ChangePasswordListener {

        private Boolean preModifyCalled;
        private Boolean postModifyCalled;
        private Boolean onExceptionCalled;

        private Account acctParam;
        private String newPasswordParam;
        private ServiceException exceptionThrown;

        public TestListenerExceptionThrownAtPreModify() {
            preModifyCalled = false;
            postModifyCalled = false;
            onExceptionCalled = false;
            acctParam = null;
            newPasswordParam = null;
            exceptionThrown =  null;
        }

        @Override
        public void preModify(Account acct, String newPassword, Map context, Map<String, Object> attrsToModify) throws ServiceException{
            preModifyCalled = true;
            // keep the parameters to make sure if onException is called with the same ones
            acctParam = acct;
            newPasswordParam = newPassword;
            exceptionThrown = ServiceException.TEMPORARILY_UNAVAILABLE();
            throw exceptionThrown;

        }

        @Override
        public void postModify(Account acct, String newPassword, Map context){
            postModifyCalled = true;
        }

        public void onException(Account acct, String newPassword, Map context, ServiceException se){
            onExceptionCalled = true;
            fail();  // onException should not be called

        }


        public Boolean isPreModifyCalled() {
            return preModifyCalled;
        }

        public Boolean isPostModifyCalled() {
            return postModifyCalled;
        }

        public Boolean isOnExceptionCalled() {
            return onExceptionCalled;
        }
    }

    /*
     * Test for onException method when exception is thrown at modifyAttrs
     */

    @Test
    public void onExceptionAtModifyAttrs01() throws Exception {
        String testName = getTestName();

        // get Test listener class name and register the class as ChangePasswordListener
        String className = TestListenerExceptionThrownAtModifyAttrs.class.getName();
        TestListenerExceptionThrownAtModifyAttrs listener = new TestListenerExceptionThrownAtModifyAttrs();
        TestListenerExceptionThrownAtModifyAttrs.register(className, listener);

        // create domain for which the class name is set to zimbraPasswordChangeListener attribute
        Map<String, Object> zimbraDomainAttrs = new HashMap<String, Object>();
        zimbraDomainAttrs.put(Provisioning.A_zimbraPasswordChangeListener, className);
        domain = createZimbraDomain(getZimbraDomainName(testName), zimbraDomainAttrs);

        // create account with old password
        String acctName = testName + "@" + domain.getDomainName();
        String oldpassword = "old_password_" + passGen.next();
        Account acct = provUtil.createAccount(acctName,oldpassword);

        // register dummy validator which throws exception when modifyAttrs gets called in LdapProvisioning
        DummyValidator dm = new DummyValidator();
        dm.activate();
        prov.register(dm);

        // change password with new password
        String newpassword = "new_password_" + passGen.next();
        try {
            prov.setPassword(acct,newpassword);
            fail();  // if there is no exception taken place in setPassword().
        } catch (Exception e){
            // make sure if caught exception is thrown at modifyAttrs()
            assertTrue(e.getMessage().equals("forbidden: password modification is not permitted for this account"));
        }

        dm.inactivate();

        assertNotNull(listener);
        assertTrue(listener.isPreModifyCalled());
        assertFalse(listener.isPostModifyCalled());
        assertTrue(listener.isOnExceptionCalled());
    }

    /*
     * Test for onException method when exception is thrown at modifyAttrs
     * Listener is registered as Internal one
     */

    @Test
    public void onExceptionAtModifyAttrs02() throws Exception {
        String testName = getTestName();

        // get Test listener class name and register the class as ChangePasswordListener
        String className = TestListenerExceptionThrownAtModifyAttrs.class.getName();
        TestListenerExceptionThrownAtModifyAttrs listener = new TestListenerExceptionThrownAtModifyAttrs();
        TestListenerExceptionThrownAtModifyAttrs.registerInternal(ChangePasswordListener.InternalChangePasswordListenerId.CPL_SYNC, listener);

        // create domain for which the class name is set to zimbraPasswordChangeListener attribute
        Map<String, Object> zimbraDomainAttrs = new HashMap<String, Object>();
        domain = createZimbraDomain(getZimbraDomainName(testName),zimbraDomainAttrs);

        // create account with old password
        String acctName = testName + "@" + domain.getDomainName();
        String oldpassword = "old_password_" + passGen.next();
        Account acct = provUtil.createAccount(acctName,oldpassword);

        // register dummy validator which throws exception when modifyAttrs gets called in LdapProvisioning
        DummyValidator dm = new DummyValidator();
        dm.activate();
        prov.register(dm);

        // change password with new password
        String newpassword = "new_password_" + passGen.next();
        try {
            prov.setPassword(acct,newpassword);
            fail();  // if there is no exception taken place in setPassword().
        } catch (Exception e){
            // make sure if caught exception is thrown at modifyAttrs()
            assertTrue(e.getMessage().equals("forbidden: password modification is not permitted for this account"));
        }

        dm.inactivate();

        assertNotNull(listener);
        assertTrue(listener.isPreModifyCalled());
        assertFalse(listener.isPostModifyCalled());
        assertTrue(listener.isOnExceptionCalled());
    }

    /*
     * Dummy validator which throws ServiceException when modifyAttrs gets called in LdapProvisioning
     */
    public static class DummyValidator implements Provisioning.ProvisioningValidator {

        private boolean activated;

        @Override
        public void validate(Provisioning prov, String action, Object... args) throws ServiceException {
            if(MODIFY_ACCOUNT_CHECK_DOMAIN_COS_AND_FEATURE.equals(action) && activated){
                throw ServiceException.FORBIDDEN("password modification is not permitted for this account");
            }
        }

        @Override
        public void refresh() {

        }

        // activate just before change password
        public void activate() {
            activated = true;
        }

        // inactivate right after change password so that the exception is not be thrown
        // from this validator for the following test cases
        public void inactivate() {
            activated = false;
        }
    }

    /*
     * Mock listener class implementing ChangePasswordListener
     */
    public class TestListenerExceptionThrownAtModifyAttrs extends ChangePasswordListener {

        private Boolean preModifyCalled;
        private Boolean postModifyCalled;
        private Boolean onExceptionCalled;
        private Account acctParam;
        private String newPasswordParam;

        public TestListenerExceptionThrownAtModifyAttrs() {
            preModifyCalled = false;
            postModifyCalled = false;
            onExceptionCalled = false;
            acctParam = null;
            newPasswordParam = null;
        }

        @Override
        public void preModify(Account acct, String newPassword, Map context, Map<String, Object> attrsToModify) throws ServiceException{
            preModifyCalled = true;
            acctParam = acct;
            newPasswordParam = newPassword;
            context.put("NEW",newPassword);
        }

        @Override
        public void postModify(Account acct, String newPassword, Map context){
            postModifyCalled = true;
        }


        public void onException(Account acct, String newPassword, Map context, ServiceException se){
            onExceptionCalled = true;
            // make sure if ServiceException instance of parameter is same as the one thrown at DummyValidator.
            assertTrue(se instanceof ServiceException);
            assertTrue(se.getMessage().equals("forbidden: password modification is not permitted for this account"));
            // make sure if the parameters are the same objects as the ones for preModify
            assertEquals(acct, acctParam);
            assertEquals(newPassword, newPasswordParam);
            assertEquals(newPassword, context.get("NEW"));

        }

        public Boolean isPreModifyCalled() {
            return preModifyCalled;
        }

        public Boolean isPostModifyCalled() {
            return postModifyCalled;
        }

        public Boolean isOnExceptionCalled() {
            return onExceptionCalled;
        }

    }
}
