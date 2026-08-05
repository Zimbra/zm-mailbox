/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2026 Synacor, Inc.
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

package com.zimbra.cs.service.admin;

import com.google.common.collect.Maps;
import com.zimbra.common.account.Key;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.RightManager;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.service.mail.ServiceTestUtil;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.admin.message.CreateAccountRequest;
import com.zimbra.soap.admin.message.DeleteAccountRequest;
import com.zimbra.soap.admin.message.GetAccountRequest;
import com.zimbra.soap.type.AccountSelector;
import java.util.Map;
import java.util.UUID;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

/**
 * Functional tests for the admin {@link CreateAccount}, {@link GetAccount} and
 * {@link DeleteAccount} SOAP handlers, driving the real {@code handle()} flow against the
 * in-memory MockProvisioning harness and asserting on persisted provisioning state.
 */
public class CreateAccountAdminTest {

    private static final String DOMAIN = "zimbra.com";

    private static final String ADMIN = "admin@zimbra.com";

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        Provisioning prov = Provisioning.getInstance();
        prov.createDomain(DOMAIN, Maps.<String, Object>newHashMap());

        Map<String, Object> adminAttrs = Maps.newHashMap();
        adminAttrs.put(Provisioning.A_zimbraId, UUID.randomUUID().toString());
        adminAttrs.put(Provisioning.A_zimbraIsAdminAccount, true);
        prov.createAccount(ADMIN, "secret", adminAttrs);

        // Required: without this, admin-right checks see null rights and handle() fails authz.
        RightManager.getInstance().getAllAdminRights();
    }

    @Before
    public void setUp() throws Exception {
        // clearData() resets the DB/index but NOT the provisioning account maps, so the admin
        // and domain created in @BeforeClass survive across methods.
        MailboxTestUtil.clearData();
    }

    private Account admin() throws ServiceException {
        Account admin = Provisioning.getInstance().get(Key.AccountBy.name, ADMIN);
        admin.setIsAdminAccount(true);
        return admin;
    }

    private void createAccount(String name, String password) throws Exception {
        CreateAccountRequest request = new CreateAccountRequest(name, password);
        Element req = JaxbUtil.jaxbToElement(request);
        CreateAccount handler = new CreateAccount();
        handler.setResponseQName(AdminConstants.CREATE_ACCOUNT_RESPONSE);
        handler.handle(req, ServiceTestUtil.getRequestContext(admin()));
    }

    // ---- CreateAccount ----------------------------------------------------------------------

    @Test
    public void handleCreateAccountPersistsAndIsRetrievable() throws Exception {
        // Act
        createAccount("alice@zimbra.com", "secret");

        // Assert — persisted in provisioning with the requested name and a generated id
        Account created = Provisioning.getInstance().get(Key.AccountBy.name, "alice@zimbra.com");
        assertNotNull("account must be persisted after CreateAccount", created);
        assertEquals("alice@zimbra.com", created.getName());
        assertNotNull("created account must have an id", created.getId());
    }

    @Test
    public void handleCreateAccountReturnsAccountInResponse() throws Exception {
        // Act
        CreateAccountRequest request = new CreateAccountRequest("bob@zimbra.com", "secret");
        Element req = JaxbUtil.jaxbToElement(request);
        CreateAccount handler = new CreateAccount();
        handler.setResponseQName(AdminConstants.CREATE_ACCOUNT_RESPONSE);
        Element response = handler.handle(req, ServiceTestUtil.getRequestContext(admin()));

        // Assert — the response carries the created <account> with the right name
        Element acctEl = response.getElement(AdminConstants.E_ACCOUNT);
        assertEquals("bob@zimbra.com", acctEl.getAttribute(AdminConstants.A_NAME));
    }

    @Test
    public void handleCreateAccountDuplicateNameReplacesExisting() throws Exception {
        // Arrange — MockProvisioning's create contract overwrites on duplicate name (casebook §B.3)
        createAccount("dup@zimbra.com", "secret");
        Account first = Provisioning.getInstance().get(Key.AccountBy.name, "dup@zimbra.com");
        assertNotNull(first);

        // Act — recreate the same name
        createAccount("dup@zimbra.com", "secret");

        // Assert — still exactly one resolvable account under that name
        Account second = Provisioning.getInstance().get(Key.AccountBy.name, "dup@zimbra.com");
        assertNotNull("duplicate create should overwrite, not vanish", second);
        assertEquals("dup@zimbra.com", second.getName());
    }

    // ---- GetAccount -------------------------------------------------------------------------

    @Test
    public void handleGetAccountAfterCreateReturnsAccount() throws Exception {
        // Arrange
        createAccount("carol@zimbra.com", "secret");

        // Act
        GetAccountRequest request = new GetAccountRequest(AccountSelector.fromName("carol@zimbra.com"));
        Element req = JaxbUtil.jaxbToElement(request);
        GetAccount handler = new GetAccount();
        handler.setResponseQName(AdminConstants.GET_ACCOUNT_RESPONSE);
        Element response = handler.handle(req, ServiceTestUtil.getRequestContext(admin()));

        // Assert
        Element acctEl = response.getElement(AdminConstants.E_ACCOUNT);
        assertEquals("carol@zimbra.com", acctEl.getAttribute(AdminConstants.A_NAME));
    }

    @Test
    public void handleGetAccountNonexistentThrows() throws Exception {
        // Act / Assert — admin GetAccount on a missing account is a clean ServiceException
        GetAccountRequest request = new GetAccountRequest(AccountSelector.fromName("ghost@zimbra.com"));
        Element req = JaxbUtil.jaxbToElement(request);
        GetAccount handler = new GetAccount();
        handler.setResponseQName(AdminConstants.GET_ACCOUNT_RESPONSE);
        try {
            handler.handle(req, ServiceTestUtil.getRequestContext(admin()));
            fail("expected ServiceException for nonexistent account");
        } catch (ServiceException e) {
            assertNotNull(e.getCode());
        }
    }

    // ---- DeleteAccount ----------------------------------------------------------------------

    @Test
    public void handleDeleteAccountRemovesAccount() throws Exception {
        // Arrange
        createAccount("dave@zimbra.com", "secret");
        Account dave = Provisioning.getInstance().get(Key.AccountBy.name, "dave@zimbra.com");
        assertNotNull(dave);

        // Act — delete by id
        DeleteAccountRequest request = new DeleteAccountRequest(dave.getId());
        Element req = JaxbUtil.jaxbToElement(request);
        DeleteAccount handler = new DeleteAccount();
        handler.setResponseQName(AdminConstants.DELETE_ACCOUNT_RESPONSE);
        handler.handle(req, ServiceTestUtil.getRequestContext(admin()));

        // Assert — gone from provisioning
        assertNull("account must be removed after DeleteAccount",
                Provisioning.getInstance().get(Key.AccountBy.name, "dave@zimbra.com"));
    }

    @Test
    public void handleCreateThenDeleteFullLifecycle() throws Exception {
        // Create
        createAccount("erin@zimbra.com", "secret");
        Account erin = Provisioning.getInstance().get(Key.AccountBy.name, "erin@zimbra.com");
        assertNotNull("created", erin);
        String id = erin.getId();

        // Delete
        DeleteAccountRequest del = new DeleteAccountRequest(id);
        DeleteAccount delHandler = new DeleteAccount();
        delHandler.setResponseQName(AdminConstants.DELETE_ACCOUNT_RESPONSE);
        delHandler.handle(JaxbUtil.jaxbToElement(del), ServiceTestUtil.getRequestContext(admin()));

        // Verify gone
        assertNull("deleted", Provisioning.getInstance().get(Key.AccountBy.id, id));
    }
}
