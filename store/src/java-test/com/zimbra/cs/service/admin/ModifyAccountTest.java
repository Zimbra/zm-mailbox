package com.zimbra.cs.service.admin;


import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

public class ModifyAccountTest {

    private Map<String, Object> attrs = new HashMap<>();
    private ModifyAccount modifyAccount = new ModifyAccount();

    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.initServer();
        Provisioning prov = Provisioning.getInstance();
}
    @Test
    public void testValidateMailAttachmentMaxSize_WithinLimit() {
        attrs.put(Provisioning.A_zimbraMailAttachmentMaxSize,"10000");
        try {
            modifyAccount.validateMailAttachmentMaxSize(attrs);
        } catch (ServiceException e) {
            fail("No exception should be thrown");
        }
    }

    @Test
    public void testValidateMailAttachmentMaxSize_ExceedingLimit() {
        attrs.put(Provisioning.A_zimbraMailAttachmentMaxSize,"100000000");
        try {
            modifyAccount.validateMailAttachmentMaxSize(attrs);
            fail("Exception should be thrown");
        } catch (ServiceException e) {
            assertEquals("account.INVALID_ATTR_VALUE", e.getCode());
            assertTrue(e.getMessage().contains("larger than max allowed"));
        }
    }

    @Test
    public void testValidateMailAttachmentMaxSize_InvalidLong(){
        attrs.put(Provisioning.A_zimbraMailAttachmentMaxSize,"19999abc");
        try {
            modifyAccount.validateMailAttachmentMaxSize(attrs);
            fail("Exception should be thrown");
        } catch (ServiceException e) {
            assertEquals("account.INVALID_ATTR_VALUE", e.getCode());
            assertTrue(e.getMessage().contains("must be a valid long"));
        }
    }
}