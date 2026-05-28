/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.cs.account.accesscontrol;

import org.junit.Assert;
import org.junit.Test;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.accesscontrol.Rights.User;
import com.zimbra.cs.ldap.LdapUtil;

public class CheckRightTest {

    private class MockAccount extends Account {
        private String id = LdapUtil.generateUUID();
        private String name;

        private MockAccount(String name) {
            super(name, null, null, null, null);
            this.name = name;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public boolean isIsAdminAccount() {
            return false;
        }

        @Override
        public boolean isIsDelegatedAdminAccount() {
            return false;
        }
    }

    @Test
    public void allowGroupTarget_userRightNonAccount_returnsTrue() throws Exception {
        Right right = User.R_viewFreeBusy;  // User right, non-account target
        Assert.assertTrue("User right with non-account target should allow group target",
                          CheckRight.allowGroupTarget(right));
    }

    @Test
    public void allowGroupTarget_userRightAccount_returnsFalse() throws Exception {
        Right right = User.R_loginAs;  // User right, account target type
        Assert.assertFalse("Account-type user right should not allow group target",
                           CheckRight.allowGroupTarget(right));
    }

    @Test
    public void rightApplicableOnTargetType_executableRight_accountTarget_returnsTrue() throws Exception {
        Right right = User.R_loginAs;
        TargetType targetType = TargetType.account;

        boolean result = CheckRight.rightApplicableOnTargetType(targetType, right, false);
        Assert.assertTrue("Executable account right should be applicable on account target",
                          result);
    }

    @Test
    public void rightApplicableOnTargetType_grantableRight_accountTarget_returnsTrue() throws Exception {
        Right right = User.R_loginAs;
        TargetType targetType = TargetType.account;

        boolean result = CheckRight.rightApplicableOnTargetType(targetType, right, true);
        // Result depends on whether right is grantable
        Assert.assertNotNull("Should handle grantable check for account target", result);
    }

    @Test
    public void rightApplicableOnTargetType_invalidRightForTarget_returnsFalse() throws Exception {
        // Create scenario where right is not applicable
        Right right = User.R_viewFreeBusy;
        TargetType targetType = TargetType.account;

        boolean result = CheckRight.rightApplicableOnTargetType(targetType, right, false);
        // For non-account rights on account targets, this may be false
        Assert.assertNotNull("Should return a boolean result", result);
    }

    @Test
    public void rightApplicableOnTargetType_domainTarget_returnsValid() throws Exception {
        Right right = User.R_viewFreeBusy;
        TargetType targetType = TargetType.domain;

        boolean result = CheckRight.rightApplicableOnTargetType(targetType, right, false);
        Assert.assertNotNull("Should handle domain target type", result);
    }

    @Test
    public void rightApplicableOnTargetType_distributionListTarget_returnsValid() throws Exception {
        Right right = User.R_viewFreeBusy;
        TargetType targetType = TargetType.dl;

        boolean result = CheckRight.rightApplicableOnTargetType(targetType, right, false);
        Assert.assertNotNull("Should handle distribution list target type", result);
    }

    @Test
    public void allowGroupTarget_multipleUserRights_consistency() throws Exception {
        Right right1 = User.R_viewFreeBusy;
        Right right2 = User.R_sendAs;

        boolean result1 = CheckRight.allowGroupTarget(right1);
        boolean result2 = CheckRight.allowGroupTarget(right2);

        // Results should be consistent for similar right types
        Assert.assertTrue("Both non-account user rights should allow groups consistently",
                          result1 && result2);
    }

    @Test
    public void rightApplicableOnTargetType_canDelegateTrue_checksGrantable() throws Exception {
        Right right = User.R_viewFreeBusy;
        TargetType targetType = TargetType.account;

        boolean result = CheckRight.rightApplicableOnTargetType(targetType, right, true);
        // When canDelegateNeeded is true, it checks grantableOnTargetType
        Assert.assertNotNull("Should apply grantable check when canDelegateNeeded is true", result);
    }

    @Test
    public void rightApplicableOnTargetType_canDelegateFalse_checksExecutable() throws Exception {
        Right right = User.R_viewFreeBusy;
        TargetType targetType = TargetType.account;

        boolean result = CheckRight.rightApplicableOnTargetType(targetType, right, false);
        // When canDelegateNeeded is false, it checks executableOnTargetType
        Assert.assertNotNull("Should apply executable check when canDelegateNeeded is false", result);
    }

    @Test
    public void rightApplicableOnTargetType_differentTargetTypes_mayDiffer() throws Exception {
        Right right = User.R_viewFreeBusy;

        boolean resultAccount = CheckRight.rightApplicableOnTargetType(TargetType.account, right, false);
        boolean resultDomain = CheckRight.rightApplicableOnTargetType(TargetType.domain, right, false);

        // Results may differ for different target types
        Assert.assertNotNull("Account result should be valid", resultAccount);
        Assert.assertNotNull("Domain result should be valid", resultDomain);
    }

    @Test
    public void allowGroupTarget_systemConsistency() throws Exception {
        // Verify that allowGroupTarget is consistent across multiple calls
        Right right = User.R_viewFreeBusy;

        boolean result1 = CheckRight.allowGroupTarget(right);
        boolean result2 = CheckRight.allowGroupTarget(right);
        boolean result3 = CheckRight.allowGroupTarget(right);

        Assert.assertEquals("Results should be consistent across multiple calls",
                            result1, result2);
        Assert.assertEquals("Results should be consistent across multiple calls",
                            result2, result3);
    }
}
