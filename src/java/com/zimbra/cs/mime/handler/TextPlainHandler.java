/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2009, 2010, 2011 Zimbra, Inc.
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
package com.zimbra.cs.mime.handler;

import java.io.IOException;
import java.io.Reader;

import javax.activation.DataSource;

import org.apache.lucene.document.Document;

import com.zimbra.common.util.ByteUtil;
import com.zimbra.cs.convert.AttachmentInfo;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.mime.MimeHandler;
import com.zimbra.cs.mime.MimeHandlerException;
import com.zimbra.cs.mime.MimeHandlerManager;

/**
 * {@link MimeHandler} that creates a Lucene document from a {@code text/plain} part.
 *
 * @since Apr 1, 2004
 * @author schemers
 */
public class TextPlainHandler extends MimeHandler {

    private String content;

    @Override
    protected boolean runsExternally() {
        return false;
    }

    @Override
    public void addFields(Document doc) {
        // we add no type-specific fields to the doc
    }

    @Override
    protected String getContentImpl() throws MimeHandlerException {
        if (content == null) {
            DataSource source = getDataSource();
            if (source != null) {
                String ctype = source.getContentType();
                try {
                    Reader reader = Mime.getTextReader(source.getInputStream(), ctype, getDefaultCharset());
                    content = ByteUtil.getContent(reader, MimeHandlerManager.getIndexedTextLimit(), true);
                } catch (IOException e) {
                    throw new MimeHandlerException(e);
                }
            }
        }
        if (content == null) {
            content = "";
        }
        return content;
    }

    /** No need to convert plain text document ever. */
    @Override
    public boolean doConversion() {
        return false;
    }

    @Override
    public String convert(AttachmentInfo doc, String baseURL) {
        throw new UnsupportedOperationException();
    }

}
