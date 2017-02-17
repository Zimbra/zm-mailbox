/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.redolog;

import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;

import com.zimbra.common.util.ByteUtil;

/**
 * This class is equivalent to java.io.DataOutputStream except that writeUTF()
 * method doesn't have 64KB limit thanks to using a different serialization
 * format. (thus incompatible with DataOutputStream)  This class is not derived
 * from DataOutputStream and does not implement DataOutput interface, to prevent
 * using either of those in redo log operation classes.
 * 
 * @author jhahm
 */
public class RedoLogOutput {
    private DataOutput mOUT;

    public RedoLogOutput(OutputStream os) {
        mOUT = new DataOutputStream(os);
    }

    public RedoLogOutput(RandomAccessFile raf) {
        mOUT = raf;
    }

    public void write(byte[] b) throws IOException { mOUT.write(b); }
    public void writeBoolean(boolean v) throws IOException { mOUT.writeBoolean(v); }
    public void writeByte(byte v) throws IOException { mOUT.writeByte(v); }
    public void writeShort(short v) throws IOException { mOUT.writeShort(v); }
    public void writeInt(int v) throws IOException { mOUT.writeInt(v); }
    public void writeLong(long v) throws IOException { mOUT.writeLong(v); }
    public void writeDouble(double v) throws IOException { mOUT.writeDouble(v); }

    public void writeUTF(String v) throws IOException {
        ByteUtil.writeUTF8(mOUT, v);
    }

    public void writeUTFArray(String[] v) throws IOException {
        if (v == null) {
            writeInt(-1);
        } else {
            writeInt(v.length);
            for (String s : v) {
                writeUTF(s);
            }
        }
    }

    // methods of DataOutput that shouldn't be used in redo logging
    // not implemented on purpose

    //public void write(byte[] b, int off, int len) throws IOException { mOUT.write(b, off, len); }
    //public void write(int b) throws IOException { mOUT.write(b); }
    //public void writeBytes(String v) throws IOException { mOUT.writeBytes(v); }
    //public void writeChar(int v) throws IOException { mOUT.writeChar(v); }
    //public void writeChars(String v) throws IOException { mOUT.writeChars(v); }
    //public void writeFloat(float v) throws IOException { mOUT.writeFloat(v); }
}
