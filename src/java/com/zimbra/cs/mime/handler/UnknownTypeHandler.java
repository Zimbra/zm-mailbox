/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011 Zimbra, Inc.
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

import org.apache.lucene.document.Document;

import com.zimbra.cs.convert.AttachmentInfo;
import com.zimbra.cs.mime.MimeHandler;

/**
 * {@link MimeHandler} for no conversion.
 *
 * @since Apr 1, 2004
 * @author schemers
 */
public class UnknownTypeHandler extends MimeHandler {

    private String contentType;

    @Override
    protected boolean runsExternally() {
        return false;
    }

    @Override
    public void addFields(Document doc) {
        // do nothing
    }

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
        return contentType;
    }

    @Override
    public String convert(AttachmentInfo doc, String baseURL) {
        throw new IllegalStateException("conversion not allowed for content of unknown type");
    }

    @Override
    public boolean doConversion() {
        return false;
    }

    @Override
    public void setContentType(String value) {
        contentType = value;
    }
}
