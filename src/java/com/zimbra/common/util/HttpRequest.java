/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007 Zimbra, Inc.
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
 * HttpRequeset.java
 */

package com.zimbra.common.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.EOFException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A light-weight class for parsing HTTP headers
 */
public class HttpRequest {
    
    public class Constants {

        /** charset for http headers */
        public static final String HTTP_HEADER_CHARSET = "US-ASCII";

        /** HTTP VERSION string (HTTP/1.1) */
        public static final String HTTP_VERSION = "HTTP/1.1";

    }

    private static final String NL = "\r\n";

    private HttpRequestLine mHttpRequestLine;
    private HashMap mRequestHeaders;
    
    private boolean mReadBody = false;
    private byte[] mBody;
    private ByteArrayOutputStream mLineBuff;
    private InputStream mInputStream;
    
    /**
     * Construct a new HttpRequest object from an input stream
     */
    public HttpRequest(InputStream input) 
        throws java.io.IOException, java.io.EOFException
    {
        mRequestHeaders = new LinkedHashMap();
        mLineBuff = new ByteArrayOutputStream(128);
        mInputStream = input;

        // readRequestLine
        readHttpRequestLine(input);
        // check version and method code?
        readHttpHeaders(input);
    }

    /**
     * return the HTTP request-line
     */
    public HttpRequestLine getRequestLine()
    {
        return mHttpRequestLine;
    }

    /**
     * return a map of the headers. All header names are lowercased.
     */
    public Map getHeaders()
    {
        return mRequestHeaders;
    }

    /**
     * return the content of the requeset
     */
    public byte[] getContent() throws IOException
    {
        if (!mReadBody)
            readBody();
        
        return mBody;
    }
    
    public void readBody() throws IOException
    {
        mReadBody = true;
        if ("chunked".equals(mRequestHeaders.get("transfer-encoding"))) {
            mBody = readChunkedContent(mInputStream);
            readHttpHeaders(mInputStream);
        } else {

            String cl = (String) mRequestHeaders.get("content-length");

            if (cl == null) {
                mBody = readUntilEOF(mInputStream);
            } else {
                try {
                    int contentLength = Integer.parseInt(cl);
                    mBody = readFully(mInputStream, contentLength);
                } catch (NumberFormatException nfe) {
                    throw new IOException("can't parse Content-Length: "+cl);
                }
            }
        }
        
    }

    /**
     * read the specified number of bytes from the input stream. 
     * Throws EOFException if EOF is reached before
     * all the content is read.
     */
    private byte[] readFully(InputStream input, int len)
        throws IOException
    {
        byte[] bc = new byte[len];
        int n = 0;
        while (n < len) {
            int count = input.read(bc, n, len-n);
            if (count < 0)
                throw new EOFException("can't read content");
            //return null;
            n += count;
        }
        return bc;
    }

    /**
     * read until EOF is reached
     */
    private byte[] readUntilEOF(InputStream input)
        throws IOException
    {
        final int SIZE = 2048;
        ByteArrayOutputStream baos = new ByteArrayOutputStream(SIZE);
        byte[] buffer = new byte[SIZE];

        int n = 0;
        while ((n = input.read(buffer, 0, SIZE)) > 0) 
            baos.write(buffer, 0, n);
        return baos.toByteArray();
    }

    /**
     * return the next chunk size
     */
    private int readChunkSize(InputStream input)
        throws IOException
    {
        String line = readHttpLine(input);
        int sep = line.indexOf(';');
        line = (sep > 0) ? 
            line.substring(0, sep).trim() : line.trim();

        try {
            return Integer.parseInt(line, 16);
        } catch (NumberFormatException nfe) {
            throw new IOException("can't parse chunk size: "+line);
        }
    }

    /**
     * read chunkled content body. Returns null if EOF is read before
     * all the content is read.
     */
    private byte[] readChunkedContent(InputStream input)
        throws IOException
    {
        ByteArrayOutputStream baos = null;
        int chunkSize;

        while ((chunkSize = readChunkSize(input)) > 0) {
            if (baos == null)
                baos = new ByteArrayOutputStream(chunkSize);
            baos.write(readFully(input, chunkSize));
            // read trailing CRLF
            int CR = input.read();
            int LF = input.read();
            if ((CR != '\r') || (LF != '\n'))
                throw new IOException("can't parse CRLF after chunk");
        }
        return baos.toByteArray();
    }

    /**
     * go through and parse all http headers into requesetHeaders
     */
    private void readHttpHeaders(InputStream input)
        throws java.io.IOException
    {
        String name = null;
        StringBuffer value = new StringBuffer();

        String line;
        while ((line = readHttpLine(input)) != null) {
            if (line.length() == 0) {
                break;
            }
            // unfold values
            if ((line.charAt(0) == ' ') || (line.charAt(0) == '\t')) {
                value.append(' ').append(line.trim());
            } else {
                if (name != null) {
                    mRequestHeaders.put(name, value.toString());
                    value.setLength(0);
                }
                int i = line.indexOf(":");
                if (i < 0) {
                    throw new IOException("unable to parse harder: "+line);
                }
                name = line.substring(0, i).toLowerCase();
                value.append(line.substring(i+1).trim());
            }
        }
        if (name != null) {
            mRequestHeaders.put(name, value.toString());
        }
    }

    /**
     * read and parse the request line
     */
    private void readHttpRequestLine(InputStream input)
        throws java.io.IOException
    {
        String request = readHttpLine(input);
        if (request == null) 
            throw new java.io.EOFException("readHttpRequestLine");
        mHttpRequestLine = new HttpRequestLine(request);
    }

    /**
     * Read a line of data from input
     */
    private String readHttpLine(InputStream input)
        throws IOException
    {
        int ch = -1, prev_ch = -1;
        mLineBuff.reset();
        while ((ch = input.read()) >= 0) {
            if (ch == '\n') {
                break;
            }
            if (prev_ch == '\r')
                mLineBuff.write(prev_ch);
            if (ch != '\r')
                mLineBuff.write(ch);
            prev_ch = ch;
        }
        if (ch == -1) 
            return null;
        else {
            return mLineBuff.toString(Constants.HTTP_HEADER_CHARSET);
        }
    }

    public String toString() 
    {
        StringBuffer sb = new StringBuffer(1024);
        sb.append(mHttpRequestLine);
        sb.append(NL);
        for (java.util.Iterator it=mRequestHeaders.keySet().iterator(); 
             it.hasNext();) {
            String name = (String) it.next();
            String value = (String)mRequestHeaders.get(name);
            sb.append(name).append(": ").append(value).append(NL);
        }
        sb.append(NL);
        if (mReadBody) {
            sb.append(new String(mBody));
        } else {
            sb.append("BODY NOT READ");
        }
        return sb.toString();
    }
}
