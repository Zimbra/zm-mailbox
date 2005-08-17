/*
 * Created on 2004. 10. 26.
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.liquidsys.coco.tcpserver;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * @author jhahm
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class TcpServerOutputStream extends BufferedOutputStream {

	protected static final byte[] CRLF = { (byte) 13, (byte) 10 };

	/**
	 * @param out
	 */
	public TcpServerOutputStream(OutputStream out) {
		super(out);
	}

	/**
	 * @param out
	 * @param size
	 */
	public TcpServerOutputStream(OutputStream out, int size) {
		super(out, size);
	}

	public void writeLine() throws IOException {
		write(CRLF, 0, CRLF.length);
	}

	public void writeLine(String str) throws IOException {
		byte[] data = str.getBytes();
		write(data, 0, data.length);
		writeLine();
	}
}
