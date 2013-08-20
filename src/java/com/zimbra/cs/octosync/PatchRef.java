/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2013 Zimbra Software, LLC.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.4 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
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
