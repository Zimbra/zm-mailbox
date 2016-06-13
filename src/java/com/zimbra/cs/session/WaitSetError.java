/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
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