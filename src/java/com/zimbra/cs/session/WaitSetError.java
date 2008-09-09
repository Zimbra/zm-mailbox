/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.session;

/**
 * Simple struct used to communicate error codes for individual accounts during a wait 
 */
public class WaitSetError {
    public static enum Type {
        ALREADY_IN_SET_DURING_ADD,
        ERROR_LOADING_MAILBOX,
        MAINTENANCE_MODE,
        NO_SUCH_ACCOUNT,
        WRONG_HOST_FOR_ACCOUNT,
        NOT_IN_SET_DURING_REMOVE,
        NOT_IN_SET_DURING_UPDATE,
        MAILBOX_DELETED,
        ;
    }

    public WaitSetError(String accountId, WaitSetError.Type error) {
        this.accountId = accountId;
        this.error = error;
    }
    public final String accountId;

    public final WaitSetError.Type error;
}