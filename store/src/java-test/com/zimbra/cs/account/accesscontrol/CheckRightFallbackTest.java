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
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Entry;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link CheckRightFallback}.
 *
 * CheckRightFallback is abstract, so tests use anonymous inner-class subclasses
 * to exercise the concrete behaviour.
 */
public class CheckRightFallbackTest {

    // ---------------------------------------------------------------
    // Helper — concrete subclass that returns a fixed value
    // ---------------------------------------------------------------

    private static CheckRightFallback returning(final Boolean value) {
        return new CheckRightFallback() {
            @Override
            protected Boolean doCheckRight(Account grantee, Entry target, boolean asAdmin)
                    throws ServiceException {
                return value;
            }
        };
    }

    private static CheckRightFallback throwing() {
        return new CheckRightFallback() {
            @Override
            protected Boolean doCheckRight(Account grantee, Entry target, boolean asAdmin)
                    throws ServiceException {
                throw ServiceException.FAILURE("simulated failure", null);
            }
        };
    }

    private static Right mockRight(String name) {
        Right r = Mockito.mock(Right.class);
        Mockito.when(r.getName()).thenReturn(name);
        return r;
    }

    // ---------------------------------------------------------------
    // setRight / mRight field
    // ---------------------------------------------------------------

    @Test
    public void testSetRight_doesNotThrow() {
        CheckRightFallback fb = returning(Boolean.TRUE);
        fb.setRight(mockRight("testRight")); // package-private method, accessible from same package
    }

    // ---------------------------------------------------------------
    // checkRight — happy path
    // ---------------------------------------------------------------

    @Test
    public void testCheckRight_returnsTrue_whenDoCheckRightReturnsTrue() {
        CheckRightFallback fb = returning(Boolean.TRUE);
        fb.setRight(mockRight("r1"));
        assertEquals(Boolean.TRUE, fb.checkRight(null, null, false));
    }

    @Test
    public void testCheckRight_returnsFalse_whenDoCheckRightReturnsFalse() {
        CheckRightFallback fb = returning(Boolean.FALSE);
        fb.setRight(mockRight("r1"));
        assertEquals(Boolean.FALSE, fb.checkRight(null, null, true));
    }

    @Test
    public void testCheckRight_returnsNull_whenDoCheckRightReturnsNull() {
        CheckRightFallback fb = returning(null);
        fb.setRight(mockRight("r1"));
        assertNull(fb.checkRight(null, null, false));
    }

    // ---------------------------------------------------------------
    // checkRight — ServiceException caught → null returned
    // ---------------------------------------------------------------

    @Test
    public void testCheckRight_returnsNull_whenDoCheckRightThrowsServiceException() {
        CheckRightFallback fb = throwing();
        // Must set mRight so the catch block can call mRight.getName() without NPE
        fb.setRight(mockRight("throwingRight"));

        Boolean result = fb.checkRight(null, null, false);
        assertNull(result);
    }

    // ---------------------------------------------------------------
    // checkRight — asAdmin parameter is forwarded correctly
    // ---------------------------------------------------------------

    @Test
    public void testCheckRight_forwardsAsAdminTrue() {
        final boolean[] captured = {false};
        CheckRightFallback fb = new CheckRightFallback() {
            @Override
            protected Boolean doCheckRight(Account grantee, Entry target, boolean asAdmin)
                    throws ServiceException {
                captured[0] = asAdmin;
                return Boolean.TRUE;
            }
        };
        fb.setRight(mockRight("r1"));
        fb.checkRight(null, null, true);
        assertTrue(captured[0]);
    }

    @Test
    public void testCheckRight_forwardsAsAdminFalse() {
        final boolean[] captured = {true};
        CheckRightFallback fb = new CheckRightFallback() {
            @Override
            protected Boolean doCheckRight(Account grantee, Entry target, boolean asAdmin)
                    throws ServiceException {
                captured[0] = asAdmin;
                return Boolean.FALSE;
            }
        };
        fb.setRight(mockRight("r1"));
        fb.checkRight(null, null, false);
        assertFalse(captured[0]);
    }
}
