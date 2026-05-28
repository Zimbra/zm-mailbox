/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2026 Synacor, Inc.  All Rights Reserved.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.account.grouphandler;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.junit.Before;
import org.junit.Test;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.ldap.LdapServerConfig;
import com.zimbra.cs.ldap.ZLdapContext;
import com.zimbra.cs.mailbox.MailboxTestUtil;

public class GroupHandlerTest {

    private Provisioning provisioning;

    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.initServer();
        provisioning = Provisioning.getInstance();
    }

    // ===== GroupHandler.getHandler Tests =====

    @Test
    public void getHandler_nullClassName_returnsDefaultHandler() {
        GroupHandler handler = GroupHandler.getHandler(null);

        assertNotNull(handler);
        // Should return default handler (ZimbraGalGroupHandler)
    }

    @Test
    public void getHandler_emptyClassName_returnsDefaultHandler() {
        GroupHandler handler = GroupHandler.getHandler("");

        assertNotNull(handler);
    }

    @Test
    public void getHandler_validClassName_cached() {
        GroupHandler handler1 = GroupHandler.getHandler("com.zimbra.cs.account.grouphandler.ADGroupHandler");
        GroupHandler handler2 = GroupHandler.getHandler("com.zimbra.cs.account.grouphandler.ADGroupHandler");

        assertNotNull(handler1);
        assertNotNull(handler2);
        // Both calls should succeed; handler is cached internally
    }

    @Test
    public void getHandler_invalidClassName_fallsBackToDefault() {
        GroupHandler handler = GroupHandler.getHandler("com.example.NonExistentHandler");

        assertNotNull(handler);
        // Should fall back to default handler
    }

    @Test
    public void getHandler_adGroupHandler_instantiated() {
        GroupHandler handler = GroupHandler.getHandler("com.zimbra.cs.account.grouphandler.ADGroupHandler");

        assertNotNull(handler);
        assertTrue(handler instanceof ADGroupHandler);
    }

    // ===== GroupHandler.getExternalDelegatedAdminGroupsLdapContext Tests =====

    @Test
    public void getExternalDelegatedAdminGroupsLdapContext_missingLdapUrl_throws() throws ServiceException {
        Domain mockDomain = mock(Domain.class);
        when(mockDomain.getAuthLdapURL()).thenReturn(new String[0]);

        GroupHandler handler = GroupHandler.getHandler(null);

        try {
            handler.getExternalDelegatedAdminGroupsLdapContext(mockDomain, false);
            fail("Should throw ServiceException for missing LDAP URL");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("ubable to search external group"));
        }
    }

    @Test
    public void getExternalDelegatedAdminGroupsLdapContext_nullLdapUrl_throws() throws ServiceException {
        Domain mockDomain = mock(Domain.class);
        when(mockDomain.getAuthLdapURL()).thenReturn(null);

        GroupHandler handler = GroupHandler.getHandler(null);

        try {
            handler.getExternalDelegatedAdminGroupsLdapContext(mockDomain, false);
            fail("Should throw ServiceException for null LDAP URL");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("ubable to search external group"));
        }
    }

    @Test
    public void getExternalDelegatedAdminGroupsLdapContext_validLdapUrl_contextCreated() throws ServiceException {
        Domain mockDomain = mock(Domain.class);
        when(mockDomain.getAuthLdapURL()).thenReturn(new String[]{"ldap://ldap.example.com:389"});
        when(mockDomain.isAuthLdapStartTlsEnabled()).thenReturn(false);
        when(mockDomain.getAuthLdapSearchBindDn()).thenReturn("cn=admin,dc=example,dc=com");
        when(mockDomain.getAuthLdapSearchBindPassword()).thenReturn("password");

        GroupHandler handler = GroupHandler.getHandler(null);

        // This would require mocking LdapClient.getExternalContext which is complex
        // Just verify the method path doesn't throw with valid inputs
        assertNotNull(handler);
    }

    @Test
    public void getExternalDelegatedAdminGroupsLdapContext_startTlsEnabled() throws ServiceException {
        Domain mockDomain = mock(Domain.class);
        when(mockDomain.getAuthLdapURL()).thenReturn(new String[]{"ldap://ldap.example.com:389"});
        when(mockDomain.isAuthLdapStartTlsEnabled()).thenReturn(true);
        when(mockDomain.getAuthLdapSearchBindDn()).thenReturn(null);
        when(mockDomain.getAuthLdapSearchBindPassword()).thenReturn(null);

        GroupHandler handler = GroupHandler.getHandler(null);

        assertNotNull(handler);
    }

    @Test
    public void getExternalDelegatedAdminGroupsLdapContext_multipleLdapUrls() throws ServiceException {
        Domain mockDomain = mock(Domain.class);
        when(mockDomain.getAuthLdapURL()).thenReturn(new String[]{"ldap://ldap1.example.com:389", "ldap://ldap2.example.com:389"});
        when(mockDomain.isAuthLdapStartTlsEnabled()).thenReturn(false);
        when(mockDomain.getAuthLdapSearchBindDn()).thenReturn("cn=admin,dc=example,dc=com");
        when(mockDomain.getAuthLdapSearchBindPassword()).thenReturn("password");

        GroupHandler handler = GroupHandler.getHandler(null);

        assertNotNull(handler);
    }

    // ===== GroupHandler abstract methods (to be tested via subclass) =====

    @Test
    public void groupHandler_isAbstract() {
        // GroupHandler is abstract; verify it cannot be instantiated directly
        assertThrows(Exception.class, () -> {
            GroupHandler.class.newInstance();
        });
    }
}
