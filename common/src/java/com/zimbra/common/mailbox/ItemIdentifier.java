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

import com.zimbra.common.service.ServiceException;

public class ItemIdentifier {
    public static final char ACCOUNT_DELIMITER = ':';
    public static final char PART_DELIMITER    = '-';

    public final String accountId;
    public final int    id;
    public final int    subPartId;

    public ItemIdentifier(String acctId, int id) {
        this (acctId, id, -1);
    }

    public ItemIdentifier(String acctId, int id, int subId) {
        this.accountId = acctId;
        this.id = id;
        this.subPartId = subId;
    }

    public ItemIdentifier(String encoded, String defaultAccountId) throws ServiceException {
        if (encoded == null || encoded.equals("")) {
            throw ServiceException.INVALID_REQUEST("empty/missing item ID", null);
        }

        // strip off the account id, if present
        int delimiter = encoded.indexOf(ACCOUNT_DELIMITER);
        if (delimiter == 0 || delimiter == encoded.length() - 1) {
            throw ServiceException.INVALID_REQUEST("malformed item ID: " + encoded, null);
        }
        if (delimiter != -1) {
            accountId = encoded.substring(0, delimiter);
        } else if (defaultAccountId != null) {
            accountId = defaultAccountId;
        } else {
            accountId = null;
        }
        encoded = encoded.substring(delimiter + 1);

        // break out the appointment sub-id, if present
        delimiter = encoded.indexOf(PART_DELIMITER);
        if (delimiter == encoded.length() - 1) {
            throw ServiceException.INVALID_REQUEST("malformed item ID: " + encoded, null);
        }
        try {
            if (delimiter > 0) {
                subPartId = Integer.parseInt(encoded.substring(delimiter + 1));
                if (subPartId < 0) {
                    throw ServiceException.INVALID_REQUEST("malformed item ID: " + encoded, null);
                }
                encoded = encoded.substring(0, delimiter);
            } else {
                subPartId = -1;
            }
            id = Integer.parseInt(encoded);
        } catch (NumberFormatException nfe) {
            throw ServiceException.INVALID_REQUEST("malformed item ID: " + encoded, nfe);
        }
    }

    public static ItemIdentifier fromOwnerAndRemoteId(String ownerId, String remoteId) {
        return new ItemIdentifier(ownerId, Integer.parseInt(remoteId));
    }

    public static ItemIdentifier fromOwnerAndFolder(String ownerId, FolderStore folderStore) {
        return fromOwnerAndRemoteId(ownerId, folderStore.getFolderIdAsString());
    }
}