/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2024 Synacor, Inc.
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
package com.zimbra.cs.account.callback;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;

@RunWith(JUnitParamsRunner.class)
public class CallbackUtilTest {

    private static Object[] validTestData() {
        return new Object[] {
                new Object[] {Provisioning.A_zimbraTwoFactorCodeLength, 7, createAttrsMap(Provisioning.A_zimbraTwoFactorCodeLength, 6, 8)},
                new Object[] {Provisioning.A_zimbraTwoFactorCodeLength, 5, createAttrsMap(Provisioning.A_zimbraTwoFactorCodeLength, 6, 8)},
                new Object[] {Provisioning.A_zimbraTwoFactorCodeLength, 9, createAttrsMap(Provisioning.A_zimbraTwoFactorCodeLength, 6, 8)},
                new Object[] {Provisioning.A_zimbraTwoFactorCodeLength, 10, createAttrsMap(Provisioning.A_zimbraTwoFactorCodeLength, 6, 8)},
                new Object[] {Provisioning.A_zimbraTwoFactorAuthEmailCodeLength, 7, createAttrsMap(Provisioning.A_zimbraTwoFactorAuthEmailCodeLength, 6, 8)},
                new Object[] {Provisioning.A_zimbraTwoFactorAuthEmailCodeLength, 5, createAttrsMap(Provisioning.A_zimbraTwoFactorAuthEmailCodeLength, 6, 8)},
                new Object[] {Provisioning.A_zimbraTwoFactorAuthEmailCodeLength, 9, createAttrsMap(Provisioning.A_zimbraTwoFactorAuthEmailCodeLength, 6, 8)},
                new Object[] {Provisioning.A_zimbraTwoFactorAuthEmailCodeLength, 10, createAttrsMap(Provisioning.A_zimbraTwoFactorAuthEmailCodeLength, 6, 8)},
                new Object[] {Provisioning.A_zimbraTwoFactorScratchCodeLength, 7, createAttrsMap(Provisioning.A_zimbraTwoFactorScratchCodeLength, 6, 8)},
                new Object[] {Provisioning.A_zimbraTwoFactorScratchCodeLength, 5, createAttrsMap(Provisioning.A_zimbraTwoFactorScratchCodeLength, 6, 8)},
                new Object[] {Provisioning.A_zimbraTwoFactorScratchCodeLength, 9, createAttrsMap(Provisioning.A_zimbraTwoFactorScratchCodeLength, 6, 8)},
                new Object[] {Provisioning.A_zimbraTwoFactorScratchCodeLength, 10, createAttrsMap(Provisioning.A_zimbraTwoFactorScratchCodeLength, 6, 8)}
        };
    }

    private static Object[] invalidTestData() {
        return new Object[] {
                new Object[] {Provisioning.A_zimbraTwoFactorCodeLength, 6, createAttrsMap(Provisioning.A_zimbraTwoFactorCodeLength, 6, 8)},
                new Object[] {Provisioning.A_zimbraTwoFactorCodeLength, 8, createAttrsMap(Provisioning.A_zimbraTwoFactorCodeLength, 6, 8)},
                new Object[] {Provisioning.A_zimbraTwoFactorAuthEmailCodeLength, 6, createAttrsMap(Provisioning.A_zimbraTwoFactorAuthEmailCodeLength, 6, 8)},
                new Object[] {Provisioning.A_zimbraTwoFactorAuthEmailCodeLength, 8, createAttrsMap(Provisioning.A_zimbraTwoFactorAuthEmailCodeLength, 6, 8)},
                new Object[] {Provisioning.A_zimbraTwoFactorScratchCodeLength, 6, createAttrsMap(Provisioning.A_zimbraTwoFactorScratchCodeLength, 6, 8)},
                new Object[] {Provisioning.A_zimbraTwoFactorScratchCodeLength, 8, createAttrsMap(Provisioning.A_zimbraTwoFactorScratchCodeLength, 6, 8)}
        };
    }

    private static Map<String, Integer> createAttrsMap(String attrName, int value1, int value2) {
        Map<String, Integer> attrs = new HashMap<>();
        switch (attrName) {
            case Provisioning.A_zimbraTwoFactorCodeLength:
                attrs.put(Provisioning.A_zimbraTwoFactorScratchCodeLength, value1);
                attrs.put(Provisioning.A_zimbraTwoFactorAuthEmailCodeLength, value2);
                break;
            case Provisioning.A_zimbraTwoFactorAuthEmailCodeLength:
                attrs.put(Provisioning.A_zimbraTwoFactorCodeLength, value1);
                attrs.put(Provisioning.A_zimbraTwoFactorScratchCodeLength, value2);
                break;
            case Provisioning.A_zimbraTwoFactorScratchCodeLength:
                attrs.put(Provisioning.A_zimbraTwoFactorCodeLength, value1);
                attrs.put(Provisioning.A_zimbraTwoFactorAuthEmailCodeLength, value2);
                break;
        }
        return attrs;
    }

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initProvisioning();
    }

    @Test
    @Parameters(method = "validTestData")
    public void validateAttributeValueTest(String attrName, int attrValue, Map<String, Integer> attrs) throws ServiceException {
        CallbackUtil.validateTwoFactorAuthAttributeValue(attrName, attrValue, attrs, 10);
    }

    @Test(expected = ServiceException.class)
    @Parameters(method = "invalidTestData")
    public void validateAttributeValueThrowsExceptionTest(String attrName, int attrValue, Map<String, Integer> attrs) throws ServiceException {
        CallbackUtil.validateTwoFactorAuthAttributeValue(attrName, attrValue, attrs, 10);
    }
}
