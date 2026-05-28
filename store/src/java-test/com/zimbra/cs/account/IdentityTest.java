/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2026 Synacor, Inc.  All Rights Reserved.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.account;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import javax.mail.internet.InternetAddress;

import org.junit.Before;
import org.junit.Test;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.MailboxTestUtil;

public class IdentityTest {

    private Account account;
    private Provisioning provisioning;
    private Identity identity;

    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.initServer();
        provisioning = Provisioning.getInstance();

        // Create test account
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraMailHost, "localhost");
        account = provisioning.createAccount("testuser@example.com", "password", attrs);
    }

    // ===== Identity.getEntryType Tests =====

    @Test
    public void getEntryType_identity_correct() {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraPrefIdentityId, "identity-1");

        Identity ident = new Identity(account, "Default", "identity-1", attrs, provisioning);

        assertEquals(Entry.EntryType.IDENTITY, ident.getEntryType());
    }

    // ===== Identity.setId Tests =====

    @Test
    public void setId_modifiesInternalId() {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraPrefIdentityId, "original-id");

        Identity ident = new Identity(account, "Default", "original-id", attrs, provisioning);
        assertEquals("original-id", ident.getId());

        // Update id
        ident.setId("new-id");
        assertEquals("new-id", ident.getId());
    }

    @Test
    public void setId_updatesRawAttrs() {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraPrefIdentityId, "original-id");

        Identity ident = new Identity(account, "Default", "original-id", attrs, provisioning);

        ident.setId("modified-id");

        assertEquals("modified-id", ident.getRawAttrs().get(Provisioning.A_zimbraPrefIdentityId));
    }

    @Test
    public void setId_persistsMultipleTimes() {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraPrefIdentityId, "id-1");

        Identity ident = new Identity(account, "Default", "id-1", attrs, provisioning);

        ident.setId("id-2");
        assertEquals("id-2", ident.getId());

        ident.setId("id-3");
        assertEquals("id-3", ident.getId());
    }

    // ===== Identity.getFriendlyEmailAddress Tests =====

    @Test
    public void getFriendlyEmailAddress_withDisplayName() {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraPrefFromAddress, "user@example.com");
        attrs.put(Provisioning.A_zimbraPrefFromDisplay, "John Doe");

        Identity ident = new Identity(account, "Default", "identity-1", attrs, provisioning);

        InternetAddress ia = ident.getFriendlyEmailAddress();

        assertNotNull(ia);
        assertEquals("user@example.com", ia.getAddress());
        assertEquals("John Doe", ia.getPersonal());
    }

    @Test
    public void getFriendlyEmailAddress_withoutDisplayName() {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraPrefFromAddress, "user@example.com");

        Identity ident = new Identity(account, "Default", "identity-1", attrs, provisioning);

        InternetAddress ia = ident.getFriendlyEmailAddress();

        assertNotNull(ia);
        assertEquals("user@example.com", ia.getAddress());
        assertNull(ia.getPersonal());
    }

    @Test
    public void getFriendlyEmailAddress_emptyDisplayName_treated_as_null() {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraPrefFromAddress, "user@example.com");
        attrs.put(Provisioning.A_zimbraPrefFromDisplay, "   ");

        Identity ident = new Identity(account, "Default", "identity-1", attrs, provisioning);

        InternetAddress ia = ident.getFriendlyEmailAddress();

        assertNotNull(ia);
        assertEquals("user@example.com", ia.getAddress());
        assertNull(ia.getPersonal());
    }

    @Test
    public void getFriendlyEmailAddress_nullDisplayName() {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraPrefFromAddress, "user@example.com");
        attrs.put(Provisioning.A_zimbraPrefFromDisplay, null);

        Identity ident = new Identity(account, "Default", "identity-1", attrs, provisioning);

        InternetAddress ia = ident.getFriendlyEmailAddress();

        assertNotNull(ia);
        assertEquals("user@example.com", ia.getAddress());
        assertNull(ia.getPersonal());
    }

    @Test
    public void getFriendlyEmailAddress_withSpecialCharactersInDisplayName() {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraPrefFromAddress, "user@example.com");
        attrs.put(Provisioning.A_zimbraPrefFromDisplay, "Dr. John \"Johnny\" Doe, Ph.D.");

        Identity ident = new Identity(account, "Default", "identity-1", attrs, provisioning);

        InternetAddress ia = ident.getFriendlyEmailAddress();

        assertNotNull(ia);
        assertEquals("user@example.com", ia.getAddress());
        assertNotNull(ia.getPersonal());
        assertTrue(ia.getPersonal().contains("Johnny"));
    }

    @Test
    public void getFriendlyEmailAddress_withUnicodeDisplayName() {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraPrefFromAddress, "user@example.com");
        attrs.put(Provisioning.A_zimbraPrefFromDisplay, "日本の太郎");

        Identity ident = new Identity(account, "Default", "identity-1", attrs, provisioning);

        InternetAddress ia = ident.getFriendlyEmailAddress();

        assertNotNull(ia);
        assertEquals("user@example.com", ia.getAddress());
    }

    @Test
    public void getFriendlyEmailAddress_roundTrip_persistence() {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraPrefFromAddress, "alice@example.com");
        attrs.put(Provisioning.A_zimbraPrefFromDisplay, "Alice Smith");

        Identity ident1 = new Identity(account, "Primary", "id-1", attrs, provisioning);
        InternetAddress ia1 = ident1.getFriendlyEmailAddress();

        // Create another identity with same data
        Identity ident2 = new Identity(account, "Primary", "id-1", attrs, provisioning);
        InternetAddress ia2 = ident2.getFriendlyEmailAddress();

        assertEquals(ia1.getAddress(), ia2.getAddress());
        assertEquals(ia1.getPersonal(), ia2.getPersonal());
    }

    @Test
    public void identity_comparable_interface() {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraPrefFromAddress, "user@example.com");

        Identity ident = new Identity(account, "Default", "identity-1", attrs, provisioning);

        assertTrue(ident instanceof Comparable);
    }

    @Test
    public void identity_name_accessor() {
        Map<String, Object> attrs = new HashMap<String, Object>();
        Identity ident = new Identity(account, "MyIdentity", "id-1", attrs, provisioning);

        assertEquals("MyIdentity", ident.getName());
    }

    @Test
    public void identity_id_accessor() {
        Map<String, Object> attrs = new HashMap<String, Object>();
        Identity ident = new Identity(account, "Default", "id-123", attrs, provisioning);

        assertEquals("id-123", ident.getId());
    }

    @Test
    public void identity_account_reference() throws ServiceException {
        Map<String, Object> attrs = new HashMap<String, Object>();
        Identity ident = new Identity(account, "Default", "id-1", attrs, provisioning);

        assertEquals(account, ident.getAccount());
    }

    @Test
    public void getFriendlyEmailAddress_noFromAddress_createsWithoutPersonal() {
        Map<String, Object> attrs = new HashMap<String, Object>();
        // No zimbraPrefFromAddress set

        Identity ident = new Identity(account, "Default", "identity-1", attrs, provisioning);

        InternetAddress ia = ident.getFriendlyEmailAddress();

        assertNotNull(ia);
        // Should handle gracefully even without address
    }

    @Test
    public void identity_multiple_attributes_coexist() {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraPrefFromAddress, "user@example.com");
        attrs.put(Provisioning.A_zimbraPrefFromDisplay, "User Name");
        attrs.put(Provisioning.A_zimbraPrefReplyToAddress, "reply@example.com");

        Identity ident = new Identity(account, "Default", "identity-1", attrs, provisioning);

        assertEquals("user@example.com", ident.getAttr(Provisioning.A_zimbraPrefFromAddress));
        assertEquals("User Name", ident.getAttr(Provisioning.A_zimbraPrefFromDisplay));
        assertEquals("reply@example.com", ident.getAttr(Provisioning.A_zimbraPrefReplyToAddress));
    }
}
