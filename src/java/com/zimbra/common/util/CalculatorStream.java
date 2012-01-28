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

import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Calculates the size and digest of the wrapped <tt>InputStream</tt>.
 * Does not support mark/reset, since we need to read the data sequentially
 * when calculating the digest.
 */
public class CalculatorStream extends InputStream {

    private long mSize = 0;
    private InputStream mIn;
    private MessageDigest mDigestCalculator;
    
    public CalculatorStream(InputStream in) {
        try {
            mDigestCalculator = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Unable to initialize " + CalculatorStream.class.getSimpleName(), e);
        }
        
        mIn  = new DigestInputStream(in, mDigestCalculator);
    }

    /**
     * Returns the SHA-256 digest of the bytes read, encoded as base64.
     */
    public String getDigest() {
        return ByteUtil.encodeFSSafeBase64(mDigestCalculator.digest());
    }
    
    /**
     * Returns the number of bytes read.
     */
    public long getSize() {
        return mSize;
    }
    
    @Override
    public int read() throws IOException {
        int result = mIn.read();
        if (result >= 0) {
            mSize++;
        }
        return result;
    }

    @Override
    public boolean markSupported() {
        // Not supported because we need to read sequentially to calculate the digest.
        return false;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int numRead = mIn.read(b, off, len);
        if (numRead > 0) {
            mSize += numRead;
        }
        return numRead;
    }

    @Override
    public int read(byte[] b) throws IOException {
        int numRead = mIn.read(b);
        if (numRead > 0) {
            mSize += numRead;
        }
        return numRead;
    }

    @Override
    public int available() throws IOException {
        return mIn.available();
    }

    @Override
    public void close() throws IOException {
        mIn.close();
    }

    @Override
    public long skip(long n) throws IOException {
        return mIn.skip(n);
    }
}
