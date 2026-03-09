/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2024 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.account.accesscontrol;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Entry;
import org.junit.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link ACLUtil}.
 *
 * All tests operate without LDAP by using a minimal in-memory Entry subclass.
 * Entry has no abstract methods; the protected constructor accepts plain maps.
 */
public class ACLUtilTest {

    // ---------------------------------------------------------------
    // Minimal concrete Entry backed by plain HashMaps (no LDAP)
    // ---------------------------------------------------------------

    /**
     * Concrete Entry subclass that has no attrs so getACL returns null.
     * Entry.getEntryType() returns EntryType.ENTRY by default;
     * Entry.getLabel() returns the id by default - neither is abstract.
     */
    private static class SimpleEntry extends Entry {
        SimpleEntry() {
            super(new HashMap<String, Object>(), new HashMap<String, Object>(), null);
        }
    }

    // ---------------------------------------------------------------
    // getAllACEs – entry with no ACE attributes → null
    // ---------------------------------------------------------------

    @Test
    public void testGetAllACEs_noAceAttributes_returnsNull() throws ServiceException {
        SimpleEntry entry = new SimpleEntry();
        List<ZimbraACE> result = ACLUtil.getAllACEs(entry);
        assertNull(result);
    }

    // ---------------------------------------------------------------
    // getAllowedNotDelegableACEs – entry with no ACE attributes → null
    // ---------------------------------------------------------------

    @Test
    public void testGetAllowedNotDelegableACEs_noAceAttributes_returnsNull() throws ServiceException {
        SimpleEntry entry = new SimpleEntry();
        Set<ZimbraACE> result = ACLUtil.getAllowedNotDelegableACEs(entry);
        assertNull(result);
    }

    // ---------------------------------------------------------------
    // getAllowedDelegableACEs – entry with no ACE attributes → null
    // ---------------------------------------------------------------

    @Test
    public void testGetAllowedDelegableACEs_noAceAttributes_returnsNull() throws ServiceException {
        SimpleEntry entry = new SimpleEntry();
        Set<ZimbraACE> result = ACLUtil.getAllowedDelegableACEs(entry);
        assertNull(result);
    }

    // ---------------------------------------------------------------
    // getDeniedACEs – entry with no ACE attributes → null
    // ---------------------------------------------------------------

    @Test
    public void testGetDeniedACEs_noAceAttributes_returnsNull() throws ServiceException {
        SimpleEntry entry = new SimpleEntry();
        Set<ZimbraACE> result = ACLUtil.getDeniedACEs(entry);
        assertNull(result);
    }

    // ---------------------------------------------------------------
    // getACEs (filtered) – entry with no ACE attributes → null
    // ---------------------------------------------------------------

    @Test
    public void testGetACEs_noAceAttributes_returnsNull() throws ServiceException {
        SimpleEntry entry = new SimpleEntry();
        Set<Right> rights = new HashSet<Right>();
        rights.add(new UserRight("r"));
        List<ZimbraACE> result = ACLUtil.getACEs(entry, rights);
        assertNull(result);
    }

    // ---------------------------------------------------------------
    // getACL cache behaviour – pre-cached ACL is returned as-is
    // ---------------------------------------------------------------

    @Test
    public void testGetACL_cachedAcl_returnsCachedObject() throws ServiceException {
        SimpleEntry entry = new SimpleEntry();
        // ZimbraACL(String[] aces, TargetType, String) builds an empty ACL when aces=[].
        ZimbraACL expected = new ZimbraACL(new String[0], TargetType.account, "test-entry");
        entry.setCachedData("ENTRY.ACL_CACHE", expected);

        ZimbraACL result = ACLUtil.getACL(entry);
        assertSame(expected, result);
    }
}
