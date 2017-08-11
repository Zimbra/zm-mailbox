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

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.zimbra.common.service.ServiceException;

public class ItemIdentifier implements Serializable {
    private static final long serialVersionUID = 7996731473950544030L;
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

    public static ItemIdentifier fromEncodedAndDefaultAcctId(String encoded, String defaultAccountId)
    throws ServiceException {
        return new ItemIdentifier(encoded, defaultAccountId);
    }

    /** If remoteId already contains ownership information, ownerId is ignored */
    public static ItemIdentifier fromOwnerAndRemoteId(String ownerId, String remoteId) throws ServiceException {
        if (remoteId.indexOf(ACCOUNT_DELIMITER) != -1) {
            return new ItemIdentifier(remoteId, ownerId);
        }
        try {
            return new ItemIdentifier(ownerId, Integer.parseInt(remoteId));
        } catch (NumberFormatException nfe) {
            throw ServiceException.INVALID_REQUEST(String.format(
                    "malformed item ID - can't be based on : ownerId=%s remoteId=%s",ownerId, remoteId), nfe);
        }
    }

    public static ItemIdentifier fromAccountIdAndItemId(String accountId, int id) {
        return new ItemIdentifier(accountId, id);
    }

    /** If folderStore already contains ownership information, ownerId is ignored */
    public static ItemIdentifier fromOwnerAndFolder(String ownerId, FolderStore folderStore) throws ServiceException {
        return fromOwnerAndRemoteId(ownerId, folderStore.getFolderIdAsString());
    }

    @Override
    public String toString() {
        return toString((String) null);
    }

    public boolean sameAndFullyDefined(ItemIdentifier other) {
        if ((this.accountId == null) || (other.accountId == null)) {
            return false;
        }
        return (this.id == other.id) && (this.subPartId == other.subPartId) && (this.accountId.equals(other.accountId));
    }

    public String toString(String authAccountId) {
        StringBuilder sb = new StringBuilder();
        if (!Strings.isNullOrEmpty(accountId) && !accountId.equals(authAccountId)) {
            sb.append(accountId).append(ACCOUNT_DELIMITER);
        }
        sb.append(this.id);
        if (this.subPartId >= 0) {
            sb.append(ItemIdentifier.PART_DELIMITER).append(this.subPartId);
        }
        return sb.toString();
    }

    public static String asSimplestString(ItemIdentifier ident, String authAccountId) {
        return ident.toString(authAccountId);
    }

    public static String asSimplestString(String encoded, String authAccountId) throws ServiceException {
        return asSimplestString(fromEncodedAndDefaultAcctId(encoded, authAccountId), authAccountId);
    }

    public static List<ItemIdentifier> fromAccountIdAndItemIds(String accountId, Collection<Integer> ids) {
        if (null == ids) {
            return Collections.emptyList();
        }
        List<ItemIdentifier>iidlist = Lists.newArrayListWithExpectedSize(ids.size());
        for (Integer idnum : ids) {
            iidlist.add(ItemIdentifier.fromAccountIdAndItemId(accountId, idnum));
        }
        return iidlist;
    }

    public static List<Integer> toIds(Collection<ItemIdentifier> ids) {
        if (null == ids) {
            return Collections.emptyList();
        }
        List<Integer>idnums = Lists.newArrayListWithExpectedSize(ids.size());
        for (ItemIdentifier id : ids) {
            idnums.add(id.id);
        }
        return idnums;
    }
}