/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite, Network Edition.
 * Copyright (C) 2026 Synacor, Inc.  All Rights Reserved.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.account.auth.ropc;

public enum Outcome {
    SUCCESS,

    INVALID,

    POLICY_DENIED,

    MFA_TIMEOUT,

    ERROR
}
