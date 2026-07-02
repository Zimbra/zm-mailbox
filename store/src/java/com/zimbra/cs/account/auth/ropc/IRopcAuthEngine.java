/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite, Network Edition.
 * Copyright (C) 2026 Synacor, Inc.  All Rights Reserved.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.account.auth.ropc;

public final class IRopcAuthEngine {

    private IRopcAuthEngine() {
    }

    public static Outcome authenticate(String accountName, String basicPassword, String deviceId) {
        return Outcome.SUCCESS;
    }
}
