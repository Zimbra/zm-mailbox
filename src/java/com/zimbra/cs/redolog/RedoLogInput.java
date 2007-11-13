/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.redolog;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

import com.zimbra.common.util.ByteUtil;

/**
 * This class is equivalent to java.io.DataInputStream except that readUTF()
 * method doesn't have 64KB limit thanks to using a different serialization
 * format. (thus incompatible with DataInputStream)  This class is not derived
 * from DataInputStream and does not implement DataInput interface, to prevent
 * using either of those in redo log operation classes.
 * 
 * @author jhahm
 */
public class RedoLogInput {
	private DataInput mIN;
    private String mPath;

	public RedoLogInput(InputStream is) {
		mIN = new DataInputStream(is);
	}

	public RedoLogInput(RandomAccessFile raf, String path) {
        mPath = path;
		mIN = raf;
	}
    
    /**
     * Returns the path to the redo log file, or <tt>null</tt> if this object
     * reads from an <tt>InputStream</tt>. 
     */
    public String getPath() {
        return mPath;
    }
    
    /**
     * Returns the current offset in this file, or <tt>-1</tt> if this object
     * reads from an <tt>InputStream</tt>.
     */
    public long getFilePointer()
    throws IOException {
        if (mIN instanceof RandomAccessFile) {
            RandomAccessFile file = (RandomAccessFile) mIN;
            return file.getFilePointer();
        }
        return -1;
    }
    
	public int skipBytes(int n) throws IOException { return mIN.skipBytes(n); }
	public void readFully(byte[] b) throws IOException { mIN.readFully(b); }
	public void readFully(byte[] b, int off, int len) throws IOException { mIN.readFully(b, off, len); }
	public boolean readBoolean() throws IOException { return mIN.readBoolean(); }
	public byte readByte() throws IOException { return mIN.readByte(); }
	public int readUnsignedByte() throws IOException { return mIN.readUnsignedByte(); }
	public short readShort() throws IOException { return mIN.readShort(); }
	public int readUnsignedShort() throws IOException { return mIN.readUnsignedShort(); }
	public int readInt() throws IOException { return mIN.readInt(); }
	public long readLong() throws IOException { return mIN.readLong(); }
	public double readDouble() throws IOException { return mIN.readDouble(); }

	public String readUTF() throws IOException {
		return ByteUtil.readUTF8(mIN);
	}

	// methods of DataInput that shouldn't be used in redo logging
	// not implemented on purpose

	//public String readLine() throws IOException { return mIN.readLine(); }
	//public char readChar(int v) throws IOException { return mIN.readChar(); }
	//public float readFloat() throws IOException { return mIN.readFloat(); }
}
