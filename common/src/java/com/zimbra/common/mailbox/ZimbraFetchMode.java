/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2016 Synacor, Inc.
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
package com.zimbra.common.mailbox;

import java.util.Arrays;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.zclient.ZClientException;

public enum ZimbraFetchMode {
        /* Everything. */
        NORMAL,
        /* Only IMAP data. */
        IMAP,
        /* Only the metadata modification sequence number. */
        MODSEQ,
        /* Only the ID of the item's parent (-1 if no parent). */
        PARENT,
        /* Only ID. */
        IDS;

    public static ZimbraFetchMode fromString(String s)
    throws ServiceException {
        try {
            return ZimbraFetchMode.valueOf(s);
        } catch (IllegalArgumentException e) {
            throw ZClientException.CLIENT_ERROR(String.format("unknown 'fetchMode':'%s' - valid values: ", s,
                   Arrays.asList(ZimbraFetchMode.values())), null);
        }
    }
}
