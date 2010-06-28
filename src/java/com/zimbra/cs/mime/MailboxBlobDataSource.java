/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
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
