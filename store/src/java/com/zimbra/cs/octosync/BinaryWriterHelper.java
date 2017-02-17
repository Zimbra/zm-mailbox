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
