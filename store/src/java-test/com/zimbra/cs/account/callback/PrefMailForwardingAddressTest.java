package com.zimbra.cs.account.callback;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import java.util.HashMap;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class PrefMailForwardingAddressTest {

    private Account account;

    private PrefMailForwardingAddress callback;

    private CallbackContext context;

    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.initServer();
        callback = new PrefMailForwardingAddress();

        Provisioning prov = Provisioning.getInstance();
        account = prov.createAccount(
                "user@test.com",
                "secret",
                new HashMap<String, Object>()
        );
    }

    @After
    public void tearDown() throws Exception {
        MailboxTestUtil.clearData();
    }

    @Test(expected = ServiceException.class)
    public void testSelfForwardingShouldThrowException() throws Exception {
        context = new CallbackContext(CallbackContext.Op.MODIFY);
        Map<String, Object> attrsToModify = new HashMap<>();
        attrsToModify.put(Provisioning.A_zimbraPrefMailForwardingAddress, "user@test.com");

        callback.preModify(context, Provisioning.A_zimbraPrefMailForwardingAddress,
                "user@test.com", attrsToModify, account);
    }

    @Test(expected = ServiceException.class)
    public void testSelfForwardingCaseInsensitiveShouldThrowException() throws Exception {
        context = new CallbackContext(CallbackContext.Op.MODIFY);
        Map<String, Object> attrsToModify = new HashMap<>();
        attrsToModify.put(Provisioning.A_zimbraPrefMailForwardingAddress, "USER@TEST.COM");

        callback.preModify(context, Provisioning.A_zimbraPrefMailForwardingAddress,
                "USER@TEST.COM", attrsToModify, account);
    }

    @Test
    public void testValidForwardingShouldNotThrowException() throws Exception {
        context = new CallbackContext(CallbackContext.Op.MODIFY);
        Map<String, Object> attrsToModify = new HashMap<>();
        attrsToModify.put(Provisioning.A_zimbraPrefMailForwardingAddress, "other@test.com");

        callback.preModify(context, Provisioning.A_zimbraPrefMailForwardingAddress,
                "other@test.com", attrsToModify, account);
    }

}

