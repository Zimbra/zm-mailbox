/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.mime.handler;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import javax.activation.DataSource;

import org.apache.lucene.document.Document;

import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.HtmlTextExtractor;
import com.zimbra.cs.convert.AttachmentInfo;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.mime.MimeHandler;
import com.zimbra.cs.mime.MimeHandlerException;
import com.zimbra.cs.mime.MimeHandlerManager;

/**
 * @author schemers
 *
 *  class that creates a Lucene document from a Java Mail Message
 */
public class TextHtmlHandler extends MimeHandler {

    String mContent;

    @Override protected boolean runsExternally() {
        return false;
    }

    @Override public void addFields(Document doc) throws MimeHandlerException {
        // make sure we've parsed the document
        getContentImpl();
    }

    @Override protected String getContentImpl() throws MimeHandlerException {
        if (mContent == null) {
            DataSource source = getDataSource();
            InputStream is = null;
            try {
                Reader reader = getReader(is = source.getInputStream(), source.getContentType());
                mContent = HtmlTextExtractor.extract(reader, MimeHandlerManager.getIndexedTextLimit());
            } catch (Exception e) {
                throw new MimeHandlerException(e);
            } finally {
                ByteUtil.closeStream(is);
            }
        }
        if (mContent == null)
            mContent = "";
        
        return mContent;
    }

    @SuppressWarnings("unused")
    protected Reader getReader(InputStream is, String ctype) throws IOException {
        return Mime.getTextReader(is, ctype, null);
    }
    
    /**
     * No need to convert text/html document ever.
     */
    @Override public boolean doConversion() {
        return false;
    }

    @Override public String convert(AttachmentInfo doc, String baseURL) {
        throw new UnsupportedOperationException();
    }
}
