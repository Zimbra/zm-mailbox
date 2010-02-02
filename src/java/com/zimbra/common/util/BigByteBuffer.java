/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.common.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


/**
 * This class is not thread safe.  User must call destroy() in a finally clause when done with this buffer.
 * 
 * User must also call doneWriting() to end writing before read the sequence back.
 * 
 * @author jjzhuang
 *
 */
public class BigByteBuffer extends OutputStream {
	
	private static final int DEFAULT_MAX_MEM_BUF_SIZE = 1024 * 1024;
	private static final int STREAM_CHUNK_SIZE = 8 * 1024;

	final private int maxMemBufSize;
	private ByteArrayOutputStream bao;
	private int bufSize;
	
	private File bufFile;
	private OutputStream bufFileOut;
	
	private boolean isWritingDone;
	private boolean isDestroyed;
	
	public BigByteBuffer() {
		this(DEFAULT_MAX_MEM_BUF_SIZE);
	}
	
	public BigByteBuffer(int maxMemBufSize) {
		this.maxMemBufSize = maxMemBufSize;
		bao = new ByteArrayOutputStream();
	}
	
	public void doneWriting() throws IOException {
		if (!isWritingDone) {
			if (bufFileOut != null) {
				bufFileOut.close();
				bufFileOut = null;
			}
			isWritingDone = true;
		}
	}
	
	public void destroy() throws IOException {
		try {
			doneWriting();
		} finally {
			isDestroyed = true;
			if (bufFile != null) {
				bufFile.delete();
				bufFile = null;
			}
		}
	}
	
	public int length() {
		return bufSize;
	}

	@Override
	public void write(int b) throws IOException {
		if (isWritingDone)
			throw new IllegalStateException("no more writing");
		
		if (bufSize < maxMemBufSize) {
			bao.write(b);
		} else {
			try {
				if (bufFileOut == null) {
					bufFile = File.createTempFile("BigByteBuffer", null);
					bufFileOut = new BufferedOutputStream(new FileOutputStream(bufFile), STREAM_CHUNK_SIZE);
				}
				bufFileOut.write(b);
			} catch (IOException x) {
				destroy();
			}
		}
		++bufSize;
	}

	@Override
	public void close() throws IOException {
		doneWriting();
		super.close();
	}
	
	@Override
	public void flush() throws IOException {
		if (isWritingDone)
			throw new IllegalStateException("no more writing");
		
		if (bufFileOut != null)
			bufFileOut.flush();
		super.flush();
	}

	public InputStream getInputStream() throws IOException {
		return new InputStream() {
			byte[] memBuf = bao.toByteArray();
			private int offset;
			private InputStream bufFileIn;
			
			private boolean isClosed;

			@Override
			public int read() throws IOException {
				if (!isWritingDone)
					throw new IllegalStateException("must finish writing before reading");
				else if (isDestroyed)
					throw new IllegalStateException("already destroyed");
				else if (isClosed)
					throw new IllegalStateException("already closed");
				else if (offset >= bufSize)
					return -1;
				
				int b = 0;
				if (offset < memBuf.length)
					b = (int)memBuf[offset] & 0xFF;
				else {
					try {
						if (bufFileIn == null) {
							assert bufFile != null;
							bufFileIn = new BufferedInputStream(new FileInputStream(bufFile), STREAM_CHUNK_SIZE);
						}
						b = bufFileIn.read();
					} catch (IOException x) {
						close();
					}
				}
				++offset;
				return b;
			}
			
			@Override
			public int available() throws IOException {
				return bufSize - offset;
			}
			
			@Override
			public void close() throws IOException {
				isClosed = true;
				if (bufFileIn != null) {
					bufFileIn.close();
					bufFileIn = null;
				}
				super.close();
				destroy();
			}
		};
	}
	
	public static void main(String[] args) throws IOException {
		test (new BigByteBuffer(4));
		test (new BigByteBuffer(8));
		test (new BigByteBuffer(16));
		testBig();
	}
	
	private static void test(BigByteBuffer bbb) throws IOException {
		bbb.write(new byte[] {1, 2}, 0, 2);
		assert bbb.length() == 2;
		bbb.write(new byte[] {3, 4, 5, 6, 7, 8}, 0, 6);
		assert bbb.length() == 8;
		try {
			bbb.getInputStream().read();
			assert false;
		} catch (IllegalStateException x) {
			System.out.println("pass write");
		}
		
		bbb.doneWriting();
		InputStream in = bbb.getInputStream();
		assert in.read() == 1;
		assert in.read() == 2;
		assert in.read() == 3;
		assert in.read() == 4;
		assert in.read() == 5;
		assert in.read() == 6;
		assert in.read() == 7;
		assert in.read() == 8;
		assert in.read() == -1;
		in.close();
		try {
			in.read();
			assert false;
		} catch (IllegalStateException x) {
			System.out.println("pass read");
		}
		
		bbb.destroy();
	}
	
	private static void testBig() throws IOException {
		BigByteBuffer bbb = new BigByteBuffer();
		for (int i = 0; i < DEFAULT_MAX_MEM_BUF_SIZE * 2; ++i)
			bbb.write((byte)(i & 0xFF));
		bbb.doneWriting();
		assert bbb.length() == DEFAULT_MAX_MEM_BUF_SIZE * 2;
		InputStream in = bbb.getInputStream();
		for (int i = 0; i < DEFAULT_MAX_MEM_BUF_SIZE * 2; ++i)
			assert (byte)in.read() == (byte)(i & 0xFF);
		assert in.read() == -1;
		bbb.destroy();
		System.out.println("big pass");
	}
}
