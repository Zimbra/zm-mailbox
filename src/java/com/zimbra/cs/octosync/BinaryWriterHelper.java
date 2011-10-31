package com.zimbra.cs.octosync;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

// TODO: Auto-generated Javadoc
/**
 * Utility class to write binary integers to an OutputStream.
 *
 * Wrapper around ByteBuffer.
 *
 * @author grzes
 *
 */
public class BinaryWriterHelper
{
    private ByteBuffer buffer;
    private OutputStream out;

    /**
     * Constructor.
     *
     * @param out OutputStream to write to.
     */
    BinaryWriterHelper(OutputStream out)
    {
        this.out = out;
        buffer = ByteBuffer.allocate(Long.SIZE >> 3).order(java.nio.ByteOrder.LITTLE_ENDIAN);
    }

    /**
     * Write short.
     *
     * @param value the value
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public void writeShort(short value) throws IOException
    {
        buffer.rewind();
        buffer.putShort(value);
        out.write(buffer.array(), 0, Short.SIZE >> 3);
    }

    /**
     * Write int.
     *
     * @param value the value
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public void writeInt(int value) throws IOException
    {
        buffer.rewind();
        buffer.putInt(value);
        out.write(buffer.array(), 0, Integer.SIZE >> 3);
    }

    /**
     * Write long.
     *
     * @param value the value
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public void writeLong(long value) throws IOException
    {
        buffer.rewind();
        buffer.putLong(value);
        out.write(buffer.array(), 0, Long.SIZE >> 3);
    }
}
