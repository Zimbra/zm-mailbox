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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Helper class for creating patches for testing.
 *
 * @author grzes
 *
 */
class PatchBuilder
{
    public ByteArrayOutputStream buffer;
    BinaryWriterHelper helper;

    PatchBuilder() throws IOException
    {
        buffer = new ByteArrayOutputStream();
        helper = new BinaryWriterHelper(buffer);

        buffer.write("OPATCH".getBytes());
        helper.writeShort((short) 0);
        helper.writeLong(-1);
    }

    public byte[] toByteArray()
    {
        return buffer.toByteArray();
    }

    public void addData(byte[] data) throws IOException
    {
        buffer.write('D');
        helper.writeLong((long)data.length);
        buffer.write(data);
    }

    public void addData(String data) throws IOException
    {
        addData(data.getBytes());
    }

    public void addRef(PatchRef patchRef) throws IOException
    {
        buffer.write('R');
        helper.writeInt(patchRef.fileId);
        helper.writeInt(patchRef.fileVersion);
        helper.writeLong(patchRef.offset);
        helper.writeInt(patchRef.length);
        buffer.write(patchRef.hashKey);
    }

    public byte[] calcSHA256(String s) throws NoSuchAlgorithmException
    {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(s.getBytes());
        return md.digest();
    }

}