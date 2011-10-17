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
