/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
/*
 * HttpRequestLine.java
 */

package com.zimbra.common.util;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

/**
 * Class to parse an HttpRequestLine.
 */

public class HttpRequestLine {
   
    private String requestLine;
    private String method;
    private String httpVersion;

    private String uri;
    private Map<String, String> mUriParams = null;
    

    public HttpRequestLine(String requestLine) 
        throws IOException
    {
        this.requestLine = requestLine;

        int sp1 = requestLine.indexOf(" ");
        if (sp1 < 0) 
            throw new IOException("unable to parse method in request-line");
        method = requestLine.substring(0, sp1).toUpperCase();

        int sp2 = requestLine.indexOf(" ", sp1+1);
        if (sp2 < 0)
            throw new IOException("unable to parse URI in request-line");

        uri = requestLine.substring(sp1+1, sp2);

        if (sp2+1 < requestLine.length()) {
            httpVersion = requestLine.substring(sp2+1).trim().toUpperCase();
        } else {
            throw new IOException("unable to parse version in request-line");
        }
    }

    public String getRequestLine() {
        return requestLine;
    }

    public String getHttpVersion() {
        return httpVersion;
    }

    public String getURI() {
        return uri;
    }
    
    public String getBaseURI() {
        int idx = uri.indexOf('?');
        String baseURI = idx >= 0 ? uri.substring(0, idx) : uri;
        try {
            return URLDecoder.decode(baseURI, "utf-8");
        } catch (UnsupportedEncodingException uee) {
            return baseURI;
        }
    }
    
    public Map<String, String> getUriParams() {
        if (mUriParams != null)
            return mUriParams;
        
        int idx = uri.indexOf('?');
        if (idx >= 0 && idx < uri.length()) {
            mUriParams = HttpUtil.getURIParams(uri.substring(idx+1));
        } else {
            mUriParams = new HashMap<String,String>();
        }
        return mUriParams;
    }

    public String getMethod() {
        return method;
    }

    public String toString() {
        return requestLine;
    }
}

