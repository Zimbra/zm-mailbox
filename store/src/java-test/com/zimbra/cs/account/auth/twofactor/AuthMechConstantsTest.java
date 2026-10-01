/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2026 Synacor, Inc.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.account.auth.twofactor;

import java.lang.reflect.Constructor;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class AuthMechConstantsTest {

    @Test
    public void constantsExposeExpectedValues() {
        assertEquals("idp-ropc", AuthMechConstants.IDP_ROPC);
        assertEquals("fallback:", AuthMechConstants.FALLBACK_PREFIX);
    }

    @Test
    public void utilityClassConstructorIsAccessibleViaReflection() throws Exception {
        Constructor<AuthMechConstants> ctor = AuthMechConstants.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        assertNotNull(ctor.newInstance());
    }
}


