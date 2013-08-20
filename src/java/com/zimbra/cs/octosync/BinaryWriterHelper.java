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
