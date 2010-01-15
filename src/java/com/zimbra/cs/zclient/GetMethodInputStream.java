/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009 Zimbra, Inc.
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
package com.zimbra.cs.zclient;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.httpclient.methods.GetMethod;

/**
 * Wraps a HTTPClient <tt>GetMethod</tt> and automatically releases resources
 * when the stream is closed.
 */
public class GetMethodInputStream extends InputStream {

    private GetMethod mGetMethod;
    private InputStream mIn;
    
    public GetMethodInputStream(GetMethod getMethod)
    throws IOException {
        mGetMethod = getMethod;
        mIn = getMethod.getResponseBodyAsStream();
    }
    
    @Override
    public int read() throws IOException {
        return mIn.read();
    }

    @Override
    public int available() throws IOException {
        return mIn.available();
    }

    @Override
    public void close() throws IOException {
        mIn.close();
        mGetMethod.releaseConnection();
    }

    @Override
    public synchronized void mark(int readlimit) {
        mIn.mark(readlimit);
    }

    @Override
    public boolean markSupported() {
        return mIn.markSupported();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return mIn.read(b, off, len);
    }

    @Override
    public int read(byte[] b) throws IOException {
        return mIn.read(b);
    }

    @Override
    public synchronized void reset() throws IOException {
        mIn.reset();
    }

    @Override
    public long skip(long n) throws IOException {
        return mIn.skip(n);
    }
}
