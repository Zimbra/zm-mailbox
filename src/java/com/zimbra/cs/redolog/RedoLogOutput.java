/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.redolog;

import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;

import com.zimbra.cs.util.ByteUtil;

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

	// methods of DataOutput that shouldn't be used in redo logging
	// not implemented on purpose

	//public void write(byte[] b, int off, int len) throws IOException { mOUT.write(b, off, len); }
	//public void write(int b) throws IOException { mOUT.write(b); }
	//public void writeBytes(String v) throws IOException { mOUT.writeBytes(v); }
	//public void writeChar(int v) throws IOException { mOUT.writeChar(v); }
	//public void writeChars(String v) throws IOException { mOUT.writeChars(v); }
	//public void writeFloat(float v) throws IOException { mOUT.writeFloat(v); }
}
