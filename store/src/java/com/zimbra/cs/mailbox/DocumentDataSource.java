/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
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
