/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.octosync;

import org.apache.commons.codec.binary.Hex;

/**
 * Represents patch reference record, i.e the record in the patch that references other file's data.
 */
public class PatchRef
{
    /** Id of the file. */
    public int fileId;

    /** Version number of the file. */
    public int fileVersion;

    /** Offset in the file. */
    public long offset;

    /** Number of bytes. */
    public int length;

    /** Hashkey (for verification). */
    public byte[] hashKey;

    public String toString()
    {
        return "fileId: " + fileId + ", fileVersion: " + fileVersion + ", offset: " + offset +
            ", length: " + length + ", hash: " + new String(Hex.encodeHex(hashKey));
    }
}
