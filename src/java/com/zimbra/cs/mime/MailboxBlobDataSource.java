/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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

/*
 * Created on Apr 29, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.zimbra.cs.mime;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.activation.DataSource;

import com.zimbra.cs.store.MailboxBlob;
import com.zimbra.cs.store.StoreManager;

// TODO: Consolidate this class with com.zimbra.cs.store.MailboxBlobDataSource.
// Not doing it now to minimize impact on 6.0.x. 
public class MailboxBlobDataSource implements DataSource {

    private MailboxBlob mBlob;
    private String mContentType;

    public MailboxBlobDataSource(MailboxBlob blob) {
        mBlob = blob;
    }

    public MailboxBlobDataSource(MailboxBlob blob, String ct) {
        this(blob);
        mContentType = ct;
    }

    public String getContentType() {
        if (mContentType != null)
            return mContentType;
        return "message/rfc822";
    }

    public InputStream getInputStream() throws IOException {
        return StoreManager.getInstance().getContent(mBlob);
    }

    public String getName() {
        // TODO should we just return null?
        return mBlob.toString();
    }

    public OutputStream getOutputStream() {
        throw new UnsupportedOperationException();
    }
}
