package com.zimbra.cs.service.account;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.accesscontrol.generated.RightConsts;
import org.junit.Assert;
import org.junit.Test;


public class GrantRightsTest {

    @Test
    public void testLoginAsRightIsDenied() {
        try {
            GrantRights.checkIfRightIsLoginAsRight(RightConsts.RT_loginAs);
        } catch (ServiceException e) {
            Assert.assertEquals(e.getCode(), ServiceException.PERM_DENIED);
        } catch (Exception e) {
            Assert.fail();
        }
    }

    @Test
    public void testSOBORightIsAllowed() {
        try {
            GrantRights.checkIfRightIsLoginAsRight(RightConsts.RT_sendOnBehalfOf);
        } catch (Exception e) {
            Assert.fail();
        }
    }

    @Test
    public void testSendAsRightIsAllowed() {
        try {
            GrantRights.checkIfRightIsLoginAsRight(RightConsts.RT_sendAs);
        } catch (Exception e) {
            Assert.fail();
        }
    }
}
