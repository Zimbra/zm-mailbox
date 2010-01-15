/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2010 Zimbra, Inc.
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
 * Created on Apr 1, 2004
 *
 */
package com.zimbra.cs.mime.handler;

import org.apache.lucene.document.Document;

import com.zimbra.cs.convert.AttachmentInfo;
import com.zimbra.cs.mime.MimeHandler;

/**
 * @author schemers
 *
 *  class that creates a Lucene document from a Java Mail Message
 */
public class UnknownTypeHandler extends MimeHandler {

    private String mContentType;

    @Override
    protected boolean runsExternally() {
        return false;
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.mime.MimeHandler#populate(org.apache.lucene.document.Document)
     */
    @Override
    public void addFields(Document doc) {
        // do nothing
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.mime.MimeHandler#getContent()
     */
    @Override
    protected String getContentImpl() {
        return "";
    }
    
    @Override
    public boolean isIndexingEnabled() {
        return true;
    }
    
    @Override
    public String getContentType() {
        return mContentType;
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.mime.MimeHandler#convert(com.zimbra.cs.convert.AttachmentInfo, java.lang.String)
     */
    @Override
    public String convert(AttachmentInfo doc, String baseURL) {
        throw new IllegalStateException("conversion not allowed for content of unknown type");
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.mime.MimeHandler#doConversion()
     */
    @Override
    public boolean doConversion() {
        return false;
    }
    
    /**
     * @see com.zimbra.cs.mime.MimeHandler#setContentType(String)
     */
    @Override
    protected void setContentType(String ct) {
        mContentType = ct;
    }
}
