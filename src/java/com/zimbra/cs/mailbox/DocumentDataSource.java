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
package com.zimbra.cs.mailbox;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.activation.DataSource;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;


public class DocumentDataSource implements DataSource {

    private Document mDocument;
    
    public DocumentDataSource(Document document) {
        if (document == null) {
            throw new NullPointerException("document cannot be null");
        }
        mDocument = document;
    }
    
    public String getContentType() {
        return mDocument.getContentType();
    }

    public InputStream getInputStream() throws IOException {
        try {
            return mDocument.getContentStream();
        } catch (ServiceException e) {
            ZimbraLog.mailbox.error("Unable to get document content", e);
            throw new IOException(e.toString());
        }
    }

    public String getName() {
        return mDocument.getName();
    }

    public OutputStream getOutputStream() {
        throw new UnsupportedOperationException();
    }

}
