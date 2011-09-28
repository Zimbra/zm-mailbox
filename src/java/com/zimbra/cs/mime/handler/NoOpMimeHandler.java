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

import java.io.IOException;

import javax.activation.DataSource;

import org.apache.lucene.document.Document;

import com.zimbra.cs.convert.AttachmentInfo;
import com.zimbra.cs.convert.ConversionException;
import com.zimbra.cs.mime.MimeHandler;
import com.zimbra.cs.mime.MimeHandlerException;
/**
 * A mime handler that does nothing. Unlike the unknown type handler
 * this won't throw any exceptions for calling the wrong method.
 * 
 * This handler is returned when indexing is turned off
 * @author jpowers
 *
 */
public class NoOpMimeHandler extends MimeHandler {

    @Override
    protected void addFields(Document doc) throws MimeHandlerException {
    }

    @Override
    public boolean isIndexingEnabled() {
        return false;
    }

    @Override
    public String convert(AttachmentInfo doc, String urlPart)
            throws IOException, ConversionException {
        return "";
    }

    @Override
    public boolean doConversion() {
        return false;
    }

    @Override
    public String getContentType() {
        // TODO Auto-generated method stub
        return super.getContentType();
    }

    @Override
    public String getDescription() {
        // TODO Auto-generated method stub
        return super.getDescription();
    }

    @Override
    public String getPartName() {
        // TODO Auto-generated method stub
        return super.getPartName();
    }

    @Override
    protected String getContentImpl() throws MimeHandlerException {
        return "";
    }

    @Override
    protected boolean runsExternally() {
        return false;
    }

}
