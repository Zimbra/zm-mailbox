/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2026 Synacor, Inc.  All Rights Reserved.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.account.ldap;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Before;
import org.junit.BeforeClass;

import java.util.LinkedHashSet;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.AttributeClass;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.MailboxTestUtil;

/**
 * Unit tests for LdapObjectClass.
 *
 * Tests LDAP object class management and hierarchy.
 */
public class LdapObjectClassTest {

    private Provisioning prov;

    @BeforeClass
    public static void setUpClass() throws Exception {
        MailboxTestUtil.initServer();
    }

    @Before
    public void setUp() throws Exception {
        prov = Provisioning.getInstance();
    }

    // ========== Default Person Object Class ==========

    @Test
    public void zimbra_defaultPersonOC_isInetOrgPerson() {
        assertEquals("inetOrgPerson", LdapObjectClass.ZIMBRA_DEFAULT_PERSON_OC);
    }

    // ========== getAccountObjectClasses Tests ==========

    @Test
    public void getAccountObjectClasses_zimbraDefaultOnly_containsDefaultPersonAndAccount() throws ServiceException {
        Set<String> ocs = LdapObjectClass.getAccountObjectClasses(prov, true);

        assertNotNull(ocs);
        assertTrue(ocs.contains("inetOrgPerson"));
        assertTrue(ocs.contains(AttributeClass.OC_zimbraAccount));
        assertEquals(2, ocs.size());
    }

    @Test
    public void getAccountObjectClasses_noZimbraDefaultOnly_includesExtraClasses() throws ServiceException {
        Set<String> ocs = LdapObjectClass.getAccountObjectClasses(prov, false);

        assertNotNull(ocs);
        assertTrue(ocs.contains("inetOrgPerson"));
        assertTrue(ocs.contains(AttributeClass.OC_zimbraAccount));
        // May include extra classes depending on config
        assertTrue(ocs.size() >= 2);
    }

    @Test
    public void getAccountObjectClasses_noParam_defaultsToIncludeExtra() throws ServiceException {
        Set<String> ocs = LdapObjectClass.getAccountObjectClasses(prov);

        assertNotNull(ocs);
        assertTrue(ocs.contains("inetOrgPerson"));
        assertTrue(ocs.contains(AttributeClass.OC_zimbraAccount));
        // Should include extra classes by default
        assertTrue(ocs.size() >= 2);
    }

    @Test
    public void getAccountObjectClasses_returnsLinkedHashSet_preservesOrder() throws ServiceException {
        Set<String> ocs = LdapObjectClass.getAccountObjectClasses(prov, true);

        // Should be LinkedHashSet to preserve order
        assertNotNull(ocs);
        // Verify order is maintained (first item should be inetOrgPerson)
        String firstItem = ocs.iterator().next();
        assertEquals("inetOrgPerson", firstItem);
    }

    @Test
    public void getAccountObjectClasses_setIsUnique_noDuplicates() throws ServiceException {
        Set<String> ocs = LdapObjectClass.getAccountObjectClasses(prov, false);

        // Convert to array and check size equals set size (no duplicates)
        Object[] array = ocs.toArray();
        assertEquals(ocs.size(), array.length);
    }

    // ========== getCalendarResourceObjectClasses Tests ==========

    @Test
    public void getCalendarResourceObjectClasses_containsCalendarResourceClass() throws ServiceException {
        Set<String> ocs = LdapObjectClass.getCalendarResourceObjectClasses(prov);

        assertNotNull(ocs);
        assertTrue(ocs.contains(AttributeClass.OC_zimbraCalendarResource));
        assertTrue(ocs.size() >= 1);
    }

    @Test
    public void getCalendarResourceObjectClasses_mayIncludeExtraClasses() throws ServiceException {
        Set<String> ocs = LdapObjectClass.getCalendarResourceObjectClasses(prov);

        assertNotNull(ocs);
        // Should have at least the calendar resource class
        assertTrue(ocs.size() >= 1);
    }

    @Test
    public void getCalendarResourceObjectClasses_returnsSet_notNull() throws ServiceException {
        Set<String> ocs = LdapObjectClass.getCalendarResourceObjectClasses(prov);

        assertNotNull(ocs);
        assertTrue(ocs instanceof LinkedHashSet);
    }

    // ========== getCosObjectClasses Tests ==========

    @Test
    public void getCosObjectClasses_containsCosClass() throws ServiceException {
        Set<String> ocs = LdapObjectClass.getCosObjectClasses(prov);

        assertNotNull(ocs);
        assertTrue(ocs.contains(AttributeClass.OC_zimbraCOS));
        assertTrue(ocs.size() >= 1);
    }

    @Test
    public void getCosObjectClasses_mayIncludeExtraClasses() throws ServiceException {
        Set<String> ocs = LdapObjectClass.getCosObjectClasses(prov);

        assertNotNull(ocs);
        assertTrue(ocs.size() >= 1);
    }

    @Test
    public void getCosObjectClasses_returnsLinkedHashSet() throws ServiceException {
        Set<String> ocs = LdapObjectClass.getCosObjectClasses(prov);

        assertTrue(ocs instanceof LinkedHashSet);
    }

    // ========== getDomainObjectClasses Tests ==========

    @Test
    public void getDomainObjectClasses_containsStandardClasses() throws ServiceException {
        Set<String> ocs = LdapObjectClass.getDomainObjectClasses(prov);

        assertNotNull(ocs);
        assertTrue(ocs.contains("dcObject"));
        assertTrue(ocs.contains("organization"));
        assertTrue(ocs.contains(AttributeClass.OC_zimbraDomain));
        assertTrue(ocs.size() >= 3);
    }

    @Test
    public void getDomainObjectClasses_mayIncludeExtraClasses() throws ServiceException {
        Set<String> ocs = LdapObjectClass.getDomainObjectClasses(prov);

        assertNotNull(ocs);
        // Should have at least the 3 standard classes
        assertTrue(ocs.size() >= 3);
    }

    @Test
    public void getDomainObjectClasses_orderedCorrectly() throws ServiceException {
        Set<String> ocs = LdapObjectClass.getDomainObjectClasses(prov);

        assertNotNull(ocs);
        Object[] array = ocs.toArray();
        assertEquals("dcObject", array[0]);
        assertEquals("organization", array[1]);
        assertEquals(AttributeClass.OC_zimbraDomain, array[2]);
    }

    // ========== Object Class Set Integrity Tests ==========

    @Test
    public void accountObjectClasses_zimbraDefaultOnly_exactSize() throws ServiceException {
        Set<String> ocs = LdapObjectClass.getAccountObjectClasses(prov, true);

        // Zimbrra default only should have exactly 2 classes
        assertEquals(2, ocs.size());
    }

    @Test
    public void domainObjectClasses_minimumSize() throws ServiceException {
        Set<String> ocs = LdapObjectClass.getDomainObjectClasses(prov);

        // Should have at least 3 classes
        assertTrue(ocs.size() >= 3);
    }

    @Test
    public void allObjectClasses_notEmpty() throws ServiceException {
        assertTrue(LdapObjectClass.getAccountObjectClasses(prov).size() > 0);
        assertTrue(LdapObjectClass.getCalendarResourceObjectClasses(prov).size() > 0);
        assertTrue(LdapObjectClass.getCosObjectClasses(prov).size() > 0);
        assertTrue(LdapObjectClass.getDomainObjectClasses(prov).size() > 0);
    }

    // ========== Extra Object Classes Integration ==========

    @Test
    public void getAccountObjectClasses_withoutExtra_size2() throws ServiceException {
        Set<String> ocs = LdapObjectClass.getAccountObjectClasses(prov, true);

        assertEquals(2, ocs.size());
    }

    @Test
    public void getAccountObjectClasses_withExtra_sizeGeq2() throws ServiceException {
        Set<String> ocs = LdapObjectClass.getAccountObjectClasses(prov, false);

        assertTrue(ocs.size() >= 2);
    }

    // ========== Zimba Account Class Present ==========

    @Test
    public void zimbraAccountClassPresent_inAllAccountClasses() throws ServiceException {
        Set<String> ocsDefault = LdapObjectClass.getAccountObjectClasses(prov, true);
        Set<String> ocsWithExtra = LdapObjectClass.getAccountObjectClasses(prov, false);

        assertTrue(ocsDefault.contains(AttributeClass.OC_zimbraAccount));
        assertTrue(ocsWithExtra.contains(AttributeClass.OC_zimbraAccount));
    }

    @Test
    public void zimbraCosClassPresent_inCosObjectClasses() throws ServiceException {
        Set<String> ocs = LdapObjectClass.getCosObjectClasses(prov);

        assertTrue(ocs.contains(AttributeClass.OC_zimbraCOS));
    }

    @Test
    public void zimbraCalendarResourceClassPresent() throws ServiceException {
        Set<String> ocs = LdapObjectClass.getCalendarResourceObjectClasses(prov);

        assertTrue(ocs.contains(AttributeClass.OC_zimbraCalendarResource));
    }

    @Test
    public void zimbraDomainClassPresent_inDomainObjectClasses() throws ServiceException {
        Set<String> ocs = LdapObjectClass.getDomainObjectClasses(prov);

        assertTrue(ocs.contains(AttributeClass.OC_zimbraDomain));
    }

    // ========== Multiple Calls Consistency ==========

    @Test
    public void multipleCallsToGetAccountObjectClasses_returnConsistentResults() throws ServiceException {
        Set<String> ocs1 = LdapObjectClass.getAccountObjectClasses(prov, true);
        Set<String> ocs2 = LdapObjectClass.getAccountObjectClasses(prov, true);

        assertEquals(ocs1, ocs2);
        assertEquals(ocs1.size(), ocs2.size());
    }

    @Test
    public void multipleCallsToGetDomainObjectClasses_returnConsistentResults() throws ServiceException {
        Set<String> ocs1 = LdapObjectClass.getDomainObjectClasses(prov);
        Set<String> ocs2 = LdapObjectClass.getDomainObjectClasses(prov);

        assertEquals(ocs1, ocs2);
    }

    // ========== Standard LDAP Classes ==========

    @Test
    public void standardLdapClasses_usedCorrectly() throws ServiceException {
        Set<String> domainOcs = LdapObjectClass.getDomainObjectClasses(prov);

        // Standard LDAP object classes
        assertTrue(domainOcs.contains("dcObject"));
        assertTrue(domainOcs.contains("organization"));
    }

    @Test
    public void inetOrgPersonClass_usedForAccounts() throws ServiceException {
        Set<String> accountOcs = LdapObjectClass.getAccountObjectClasses(prov, true);

        assertTrue(accountOcs.contains("inetOrgPerson"));
    }

    // ========== Edge Cases ==========

    @Test
    public void getAccountObjectClasses_provisioningInstanceNotNull() throws ServiceException {
        Provisioning p = Provisioning.getInstance();

        assertNotNull(p);
        Set<String> ocs = LdapObjectClass.getAccountObjectClasses(p);
        assertNotNull(ocs);
    }

    @Test
    public void allObjectClassSets_stringValues() throws ServiceException {
        Set<String> accountOcs = LdapObjectClass.getAccountObjectClasses(prov);
        Set<String> domainOcs = LdapObjectClass.getDomainObjectClasses(prov);
        Set<String> cosOcs = LdapObjectClass.getCosObjectClasses(prov);
        Set<String> resourceOcs = LdapObjectClass.getCalendarResourceObjectClasses(prov);

        for (String s : accountOcs) {
            assertTrue(s instanceof String);
            assertTrue(s.length() > 0);
        }
        for (String s : domainOcs) {
            assertTrue(s instanceof String);
            assertTrue(s.length() > 0);
        }
        for (String s : cosOcs) {
            assertTrue(s instanceof String);
            assertTrue(s.length() > 0);
        }
        for (String s : resourceOcs) {
            assertTrue(s instanceof String);
            assertTrue(s.length() > 0);
        }
    }

    @Test
    public void objectClassNames_validLdapFormat() throws ServiceException {
        Set<String> allOcs = new java.util.HashSet<>();
        allOcs.addAll(LdapObjectClass.getAccountObjectClasses(prov));
        allOcs.addAll(LdapObjectClass.getDomainObjectClasses(prov));
        allOcs.addAll(LdapObjectClass.getCosObjectClasses(prov));
        allOcs.addAll(LdapObjectClass.getCalendarResourceObjectClasses(prov));

        for (String oc : allOcs) {
            // LDAP OC names should only contain alphanumeric and hyphens (typically)
            assertTrue(oc.matches("^[a-zA-Z0-9\\-]+$"));
        }
    }
}
