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

package com.zimbra.cs.store;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.activation.DataSource;

import com.zimbra.common.mime.MimeConstants;

// TODO: Consolidate this class with com.zimbra.cs.mime.MailboxBlobDataSource.
// Not doing it now to minimize impact on 6.0.x. 
public class MailboxBlobDataSource implements DataSource {
    
    private MailboxBlob mBlob;
    
    public MailboxBlobDataSource(MailboxBlob blob) {
        if (blob == null) {
            throw new NullPointerException();
        }
        mBlob = blob;
    }

    public String getContentType() {
        return MimeConstants.CT_APPLICATION_OCTET_STREAM;
    }

    public InputStream getInputStream() throws IOException {
        return StoreManager.getReaderSMInstance(mBlob.getLocator()).getContent(mBlob);
    }

    public String getName() {
        return null;
    }

    public OutputStream getOutputStream() throws IOException {
        throw new IOException("not supported");
    }
}
