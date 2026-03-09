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
import com.zimbra.cs.account.accesscontrol.RightCommand.AllEffectiveRights;
import org.junit.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Unit tests for the {@link AdminConsoleCapable} interface.
 *
 * {@code AdminConsoleCapable} is a package-private interface.  These tests
 * verify that:
 * <ul>
 *   <li>An anonymous implementation can be constructed without errors.</li>
 *   <li>The three declared methods are callable on a concrete implementation.</li>
 *   <li>{@code targetTypesForGrantSearch()} returns a non-null, non-empty set
 *       (contract the framework relies upon).</li>
 * </ul>
 */
public class AdminConsoleCapableTest {

    // ---------------------------------------------------------------
    // Minimal anonymous implementation (no LDAP required)
    // ---------------------------------------------------------------

    /**
     * Returns a no-op implementation of {@link AdminConsoleCapable} that
     * returns a singleton set containing {@link TargetType#account}.
     */
    private static AdminConsoleCapable minimalImpl() {
        return new AdminConsoleCapable() {

            @Override
            public void getAllEffectiveRights(RightBearer rightBearer,
                    boolean expandSetAttrs, boolean expandGetAttrs,
                    AllEffectiveRights result) throws ServiceException {
                // no-op
            }

            @Override
            public void getEffectiveRights(RightBearer rightBearer, Entry target,
                    boolean expandSetAttrs, boolean expandGetAttrs,
                    RightCommand.EffectiveRights result) throws ServiceException {
                // no-op
            }

            @Override
            public Set<TargetType> targetTypesForGrantSearch() {
                return EnumSet.of(TargetType.account);
            }
        };
    }

    // ---------------------------------------------------------------
    // Interface method accessibility
    // ---------------------------------------------------------------

    @Test
    public void testTargetTypesForGrantSearch_returnsNonNull() {
        AdminConsoleCapable impl = minimalImpl();
        Set<TargetType> types = impl.targetTypesForGrantSearch();
        assertNotNull(types);
    }

    @Test
    public void testTargetTypesForGrantSearch_containsAccountType() {
        AdminConsoleCapable impl = minimalImpl();
        Set<TargetType> types = impl.targetTypesForGrantSearch();
        assertTrue(types.contains(TargetType.account));
    }

    @Test
    public void testTargetTypesForGrantSearch_notEmpty() {
        AdminConsoleCapable impl = minimalImpl();
        assertFalse(impl.targetTypesForGrantSearch().isEmpty());
    }

    @Test
    public void testGetAllEffectiveRights_doesNotThrow() throws ServiceException {
        AdminConsoleCapable impl = minimalImpl();
        // no-op impl must not throw – verify via normal invocation
        impl.getAllEffectiveRights(null, false, false, null);
    }

    @Test
    public void testGetEffectiveRights_doesNotThrow() throws ServiceException {
        AdminConsoleCapable impl = minimalImpl();
        impl.getEffectiveRights(null, null, false, false, null);
    }

    // ---------------------------------------------------------------
    // Multiple implementations with different target type sets
    // ---------------------------------------------------------------

    @Test
    public void testTargetTypesForGrantSearch_domainImpl_containsDomain() {
        AdminConsoleCapable domainImpl = new AdminConsoleCapable() {
            @Override
            public void getAllEffectiveRights(RightBearer rb,
                    boolean es, boolean eg, AllEffectiveRights r) {}
            @Override
            public void getEffectiveRights(RightBearer rb, Entry t,
                    boolean es, boolean eg, RightCommand.EffectiveRights r) {}
            @Override
            public Set<TargetType> targetTypesForGrantSearch() {
                return EnumSet.of(TargetType.domain, TargetType.global);
            }
        };

        assertTrue(domainImpl.targetTypesForGrantSearch().contains(TargetType.domain));
        assertTrue(domainImpl.targetTypesForGrantSearch().contains(TargetType.global));
        assertFalse(domainImpl.targetTypesForGrantSearch().contains(TargetType.account));
    }
}
