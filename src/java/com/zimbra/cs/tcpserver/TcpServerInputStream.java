/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on 2004. 10. 26.
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.zimbra.cs.tcpserver;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author jhahm
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class TcpServerInputStream extends BufferedInputStream {

	StringBuffer mBuffer;
	protected static final int CR = 13;
	protected static final int LF = 10;

	/**
	 * @param in
	 */
	public TcpServerInputStream(InputStream in) {
		super(in);
		mBuffer = new StringBuffer(128);
	}

	/**
	 * @param in
	 * @param size
	 */
	public TcpServerInputStream(InputStream in, int size) {
		super(in, size);
		mBuffer = new StringBuffer(128);
	}

	/**
	 * Reads a line from the stream.  A line is terminated with either
	 * CRLF or bare LF.  (This is different from the behavior of
	 * BufferedReader.readLine() which considers a bare CR as line
	 * terminator.)
	 * @return A String containing the contents of the line, not
	 *         including any line-termination characters, or null if
	 *         the end of the stream has been reached
	 * @throws IOException
	 */
	public String readLine() throws IOException {
		boolean gotCR = false;
		mBuffer.delete(0, mBuffer.length());
		while (true) {
			int ch = read();
			if (ch == -1) {
				return null;
			}
			if (ch == CR) {
				gotCR = true;
				continue;
			} else {
				gotCR = false;
			}
			if (ch == LF) {
				return mBuffer.toString();
			}
			mBuffer.append((char)ch);
		}
	}
}
