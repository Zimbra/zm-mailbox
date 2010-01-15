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

package com.zimbra.cs.mime;

import com.zimbra.common.mime.MimeConstants;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.activation.DataSource;

public class ParsedMessageDataSource implements DataSource {
    
    private ParsedMessage mParsedMessage;
    
    public ParsedMessageDataSource(ParsedMessage pm) {
        if (pm == null) {
            throw new NullPointerException();
        }
        mParsedMessage = pm;
    }

    public String getContentType() {
        return MimeConstants.CT_MESSAGE_RFC822;
    }

    public InputStream getInputStream() throws IOException {
        return mParsedMessage.getRawInputStream();
    }

    public String getName() {
        return mParsedMessage.getSubject();
    }

    public OutputStream getOutputStream() throws IOException {
        throw new IOException("not supported");
    }
}
