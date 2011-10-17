package com.zimbra.cs.octosync;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
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

    PatchBuilder() throws IOException
    {
        buffer = new ByteArrayOutputStream();
        buffer.write("OPATCH".getBytes());
        writeShort((short) 0);
        writeLong(-1);
    }

    public byte[] toByteArray()
    {
        return buffer.toByteArray();
    }

    public void addData(byte[] data) throws IOException
    {
        buffer.write('D');
        writeLong((long)data.length);
        buffer.write(data);
    }

    public void addData(String data) throws IOException
    {
        addData(data.getBytes());
    }

    public void addRef(PatchRef patchRef) throws IOException
    {
        buffer.write('R');
        writeInt(patchRef.fileId);
        writeInt(patchRef.fileVersion);
        writeLong(patchRef.offset);
        writeInt(patchRef.length);
        buffer.write(patchRef.hashKey);
    }

    public byte[] calcSHA256(String s) throws NoSuchAlgorithmException
    {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(s.getBytes());
        return md.digest();
    }

    private void writeShort(short value) throws IOException
    {
        ByteBuffer bb = getByteBuffer(2);
        bb.putShort(value);
        buffer.write(bb.array());
    }

    private void writeInt(int value) throws IOException
    {
        ByteBuffer bb = getByteBuffer(4);
        bb.putInt(value);
        buffer.write(bb.array());
    }

    private void writeLong(long value) throws IOException
    {
        ByteBuffer bb = getByteBuffer(8);
        bb.putLong(value);
        buffer.write(bb.array());
    }

    private ByteBuffer getByteBuffer(int capacity)
    {
        return ByteBuffer.allocate(capacity).order(java.nio.ByteOrder.LITTLE_ENDIAN);
    }

}