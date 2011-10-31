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