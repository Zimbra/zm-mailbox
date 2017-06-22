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

/**
 * For path e.g. "/foo/bar/baz/gub" where a mailbox has a Folder at "/foo/bar" but NOT one at "/foo/bar/baz"
 * this class can encapsulate this information where:
 *     parentFolderStore is the folder at path "/foo/bar"
 *     unmatchedPart = "baz/gub".
 */
public class ExistingParentFolderStoreAndUnmatchedPart {
    public FolderStore parentFolderStore;
    public String unmatchedPart;

    public ExistingParentFolderStoreAndUnmatchedPart(FolderStore fstore, String unmatched) {
        this.parentFolderStore = fstore;
        this.unmatchedPart = unmatched;
    }
}
