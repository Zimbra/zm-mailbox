/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2007, 2009, 2010 Zimbra, Inc.
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

/*
 * Created on Feb 14, 2005
 *
 */
package com.zimbra.cs.convert;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Encapsulates text extracted from a document, its character encoding, MIME type(s),
 * and associated attributes.
 */ 
public class DocumentText {
    public static DocumentText EMPTY;
    static {
        try {
            EMPTY = new DocumentText("".getBytes("UTF-8"), "UTF-8", Collections.EMPTY_MAP, "text/plain");
        } catch (UnsupportedEncodingException e) {
            // unless underlying java platform does not have UTF-8
        }
    }
    
    private Map mAttrs;
    private String mContentTypes;
    private byte[] mContentBytes;
    private String mCharEncoding;
    
    /**
     * Text extraction result from the engine.
     * 
     * @param bytes the bytes of the extracted text. Note the bytes are NOT copied so the caller should 
     *  not modify its copy after constructing a DocumentText with it
     * @param charEncoding the char encoding of the bytes
     * @param attrs the meta information attributes about the document
     * @param contentTypes the mime types derived from the content
     * @throws UnsupportedEncodingException
     */
    public DocumentText(byte[] bytes, String charEncoding, Map attrs, String contentTypes) throws UnsupportedEncodingException {
        mAttrs = attrs;
        mContentTypes = contentTypes;
        if (charEncoding != null && !Charset.isSupported(charEncoding))
            throw new UnsupportedEncodingException("unsupported char encoding: " + charEncoding);
        mContentBytes = bytes;
        mCharEncoding = charEncoding;
    }
    
    /**
     * @return Returns the attrs.
     */
    public Map getAttrs() {
        return mAttrs;
    }
    /**
     * @return Returns the content.
     */
    public String getContent() {
        if (mCharEncoding == null)
            return new String(mContentBytes);
        try {
            return new String(mContentBytes, mCharEncoding);
        } catch (UnsupportedEncodingException e) {
            // won't happen as we have already tested the encoding in the constructor
            return "";
        }
    }
    
    public byte[] getContentBytes() {
        return mContentBytes;
    }
    
    public String getCharEncoding() {
        return mCharEncoding;
    }
    
    /**
     * @return comma-separated MIME types
     */
    public String getContentTypes() {
        return mContentTypes;
    }
    
    /**
     * @return a <tt>List</tt> of MIME types or an empty list
     */
    public List<String> getContentTypeList() {
        if (mContentTypes == null || "".equals(mContentTypes)) {
            return Collections.emptyList();
        }
        List<String> a = new ArrayList<String>();
        StringTokenizer st = new StringTokenizer(mContentTypes, ",");
        while (st.hasMoreTokens()) {
            a.add(st.nextToken().trim());
        }
        return a;
    }
}
