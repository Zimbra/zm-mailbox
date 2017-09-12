/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite, Network Edition.
 * Copyright (C) 2013, 2014 Zimbra, Inc.  All Rights Reserved.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.filter;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ldap.LdapProvisioning;
import com.zimbra.cs.filter.jsieve.ActionFlag;
import com.zimbra.cs.mailbox.DeliveryContext;
import com.zimbra.cs.mailbox.DeliveryOptions;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mime.ParsedMessage;

/**
 * @author zimbra
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ Provisioning.class })
@PowerMockIgnore({"javax.xml.parsers.*", "org.apache.log4j.*", "org.xml.sax.*", "org.w3c.dom.*"})
public class IncomingMessageHandlerTest {

    @Mock
    private Provisioning prov = PowerMockito.mock(LdapProvisioning.class);

    @Before
    public void setUp() {
        PowerMockito.mockStatic(Provisioning.class);
        PowerMockito.when(Provisioning.getInstance()).thenReturn(prov);
    }


    @Test
    public void testImplicitKeepWhenForwardFeatureEnabledAndReadFlagSet() {
        try {
            OperationContext octxt = EasyMock.createMock(OperationContext.class);
            DeliveryContext dctxt = EasyMock.createMock(DeliveryContext.class);
            String recipientsAddress = "user1@email.com";
            int size = 100;
            int defaultFolderId = 2;
            boolean noICal = false;
            Mailbox mailbox = new MockMailbox();
            IncomingMessageHandler messageHandler = new IncomingMessageHandler(octxt, dctxt,
                mailbox, recipientsAddress, null, size, defaultFolderId, noICal);
            Account acct = mailbox.getAccount();
            acct.setPrefMailForwardingAddress("forwar@zimbra.com");
            acct.setFeatureMarkMailForwardedAsRead(true);
            acct.setFeatureMailForwardingEnabled(true);
            List<ActionFlag> actions = new ArrayList<ActionFlag>();
            String[] tags = { "yellow" };

            Message msg = messageHandler.implicitKeep(actions, tags);
            assertEquals(0, msg.getFlagBitmask());
        } catch (ServiceException e) {
            Assert.fail("No Exception should be thrown.");
        }

    }

    @Test
    public void testImplicitKeepWhenForwardFeatureEnabledButReadFlagNotSet() {
        try {
            OperationContext octxt = EasyMock.createMock(OperationContext.class);
            DeliveryContext dctxt = EasyMock.createMock(DeliveryContext.class);
            String recipientsAddress = "user1@email.com";
            int size = 100;
            int defaultFolderId = 2;
            boolean noICal = false;
            Mailbox mailbox = new MockMailbox();

            IncomingMessageHandler messageHandler = new IncomingMessageHandler(octxt, dctxt,
                mailbox, recipientsAddress, null, size, defaultFolderId, noICal);

            Account acct = mailbox.getAccount();
            acct.setPrefMailForwardingAddress("forwar@zimbra.com");
            acct.setFeatureMarkMailForwardedAsRead(false);
            acct.setFeatureMailForwardingEnabled(true);
            List<ActionFlag> actions = new ArrayList<ActionFlag>();
            String[] tags = { "yellow" };

            Message msg = messageHandler.implicitKeep(actions, tags);
            assertEquals(Flag.BITMASK_UNREAD, msg.getFlagBitmask());
        } catch (ServiceException e) {
            Assert.fail("No Exception should be thrown.");
        }

    }

    @Test
    public void testImplicitKeepWhenForwardFeatureDisabled() {
        try {
            OperationContext octxt = EasyMock.createMock(OperationContext.class);
            DeliveryContext dctxt = EasyMock.createMock(DeliveryContext.class);
            String recipientsAddress = "user1@email.com";
            int size = 100;
            int defaultFolderId = 2;
            boolean noICal = false;
            Mailbox mailbox = new MockMailbox();

            IncomingMessageHandler messageHandler = new IncomingMessageHandler(octxt, dctxt,
                mailbox, recipientsAddress, null, size, defaultFolderId, noICal);

            Account acct = mailbox.getAccount();
            acct.setFeatureMarkMailForwardedAsRead(false);
            acct.setFeatureMailForwardingEnabled(false);
            List<ActionFlag> actions = new ArrayList<ActionFlag>();
            String[] tags = { "yellow" };

            Message msg = messageHandler.implicitKeep(actions, tags);
            assertEquals(Flag.BITMASK_UNREAD, msg.getFlagBitmask());
        } catch (ServiceException e) {
            Assert.fail("No Exception should be thrown.");
        }

    }
    
    public Account getAccount() throws ServiceException {

        HashMap<String, Object> attrs;
        attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraId, UUID.randomUUID().toString());
        Account acctLocal = new MockAccount("test@zimbra.com", null, attrs, attrs, prov);

        return acctLocal;
    }

    public class MockAccount extends Account {

        private boolean zimbraFeatureMarkMailForwardedAsRead;
        private String prefMailForwardingAddress;
        private boolean zimbraFeatureMailForwardingEnabled;

        /**
         * @param name
         * @param id
         * @param attrs
         * @param defaults
         * @param prov
         */
        public MockAccount(String name, String id, Map<String, Object> attrs,
                           Map<String, Object> defaults, Provisioning prov) {
            super(name, id, attrs, defaults, prov);
        }

        public boolean isFeatureAntispamEnabled() {
            return false;
        }

        public String getPrefMailForwardingAddress() {
            return this.prefMailForwardingAddress;
        }

        public boolean isFeatureMailForwardingEnabled() {
            return this.zimbraFeatureMailForwardingEnabled;
        }

        public boolean isFeatureMarkMailForwardedAsRead() {
            return zimbraFeatureMarkMailForwardedAsRead;
        }

        @Override
        public void setFeatureMarkMailForwardedAsRead(boolean zimbraFeatureMarkMailForwardedAsRead)
            throws ServiceException {
            this.zimbraFeatureMarkMailForwardedAsRead = zimbraFeatureMarkMailForwardedAsRead;
        }

        @Override
        public void setPrefMailForwardingAddress(String zimbraPrefMailForwardingAddress)
            throws ServiceException {
            this.prefMailForwardingAddress = zimbraPrefMailForwardingAddress;
        }

        @Override
        public void setFeatureMailForwardingEnabled(boolean zimbraFeatureMailForwardingEnabled)
            throws ServiceException {
            this.zimbraFeatureMailForwardingEnabled = zimbraFeatureMailForwardingEnabled;
        }
    }

    public class MockMailbox extends Mailbox {

        private Account account;
        /**
         * @param data
         */
        protected MockMailbox() {
            
            HashMap<String, Object> attrs;
            attrs = new HashMap<String, Object>();
            attrs.put(Provisioning.A_zimbraId, UUID.randomUUID().toString());
            account = new MockAccount("test@zimbra.com", null, attrs, attrs, prov);
        }

        @Override
        public Account getAccount() throws ServiceException {
            return account;
        }

        @Override
        public String getAccountId() {
            return "test";
        }

        @Override
        public Message addMessage(OperationContext octxt, ParsedMessage pm, DeliveryOptions dopt,
            DeliveryContext dctxt) throws IOException, ServiceException {

            Message msg = PowerMockito.mock(Message.class);
            PowerMockito.when(msg.getFlagBitmask()).thenReturn(dopt.getFlags());
            return msg;
        }
    }

}
