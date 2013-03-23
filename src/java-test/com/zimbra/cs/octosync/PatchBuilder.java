/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 VMware, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
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