/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2026 Synacor, Inc.  All Rights Reserved.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.account;

import com.zimbra.cs.mailbox.MailboxTestUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link GlobalGrant}.
 *
 * Tests verify global grant creation, attribute access, and entry type verification.
 */
public class GlobalGrantTest {

    private Provisioning prov;

    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.initProvisioning();
        prov = Provisioning.getInstance();
    }

    @After
    public void tearDown() throws Exception {
        MailboxTestUtil.clearData();
    }

    @Test
    public void createGlobalGrant_withEmptyAttributes_succeeds() {
        Map<String, Object> attrs = new HashMap<>();
        GlobalGrant grant = new GlobalGrant(attrs, prov);

        assertNotNull(grant);
        assertNotNull(prov);
    }

    @Test
    public void getEntryType_forGlobalGrant_returnsGlobalGrantType() {
        Map<String, Object> attrs = new HashMap<>();
        GlobalGrant grant = new GlobalGrant(attrs, prov);

        assertEquals(Entry.EntryType.GLOBALGRANT, grant.getEntryType());
    }

    @Test
    public void getLabel_forGlobalGrant_returnsGlobalAclTarget() {
        Map<String, Object> attrs = new HashMap<>();
        GlobalGrant grant = new GlobalGrant(attrs, prov);

        assertEquals("globalacltarget", grant.getLabel());
    }

    @Test
    public void createGlobalGrant_withAttributes_attributesAccessible() {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("description", "Test Global Grant");
        attrs.put("zimbraId", "grant-id-123");

        GlobalGrant grant = new GlobalGrant(attrs, prov);

        assertNotNull(grant);
        assertEquals("Test Global Grant", grant.getAttr("description"));
    }

    @Test
    public void globalGrant_inheritsFromEntry_methodsAvailable() {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("zimbraId", "test-grant-id");

        GlobalGrant grant = new GlobalGrant(attrs, prov);

        // Inherited from Entry
        assertNotNull(grant.getProvisioning());
        assertEquals(prov, grant.getProvisioning());
    }

    @Test
    public void globalGrant_multipleInstances_independent() {
        Map<String, Object> attrs1 = new HashMap<>();
        attrs1.put("description", "Grant 1");

        Map<String, Object> attrs2 = new HashMap<>();
        attrs2.put("description", "Grant 2");

        GlobalGrant grant1 = new GlobalGrant(attrs1, prov);
        GlobalGrant grant2 = new GlobalGrant(attrs2, prov);

        assertEquals("Grant 1", grant1.getAttr("description"));
        assertEquals("Grant 2", grant2.getAttr("description"));
    }

    @Test
    public void globalGrant_withNullProvisioning_creationSucceeds() {
        Map<String, Object> attrs = new HashMap<>();
        // GlobalGrant can be created with null provisioning in some contexts
        GlobalGrant grant = new GlobalGrant(attrs, null);

        assertNotNull(grant);
        assertEquals(Entry.EntryType.GLOBALGRANT, grant.getEntryType());
    }

    @Test
    public void getAttr_withNonExistentAttribute_returnsNull() {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("description", "Test");

        GlobalGrant grant = new GlobalGrant(attrs, prov);

        assertNull(grant.getAttr("nonExistentAttribute"));
    }

    @Test
    public void globalGrant_hasDefaultEntryMethods() {
        Map<String, Object> attrs = new HashMap<>();
        GlobalGrant grant = new GlobalGrant(attrs, prov);

        // Entry base methods should be available
        assertNotNull(grant.getEntryType());
        assertNotNull(grant.getLabel());
    }

    @Test
    public void createGlobalGrant_resetDataCalled_consistentState() {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("description", "Test Grant");

        GlobalGrant grant = new GlobalGrant(attrs, prov);

        // After creation and resetData() call in constructor, should be consistent
        assertEquals(Entry.EntryType.GLOBALGRANT, grant.getEntryType());
        assertEquals("Test Grant", grant.getAttr("description"));
    }
}
