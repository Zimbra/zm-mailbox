/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
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
import javax.mail.MessagingException;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mime.Mime;

public class MessageDataSource implements DataSource {
    
    private Message mMessage;
    private String mContentType;

    public MessageDataSource(Message message)
    throws MessagingException, ServiceException {
        if (message == null) {
            throw new NullPointerException("message cannot be null");
        }
        mMessage = message;
        mContentType = message.getMimeMessage().getContentType();
    }
    
    public String getContentType() {
        return mContentType;
    }

    public InputStream getInputStream() throws IOException {
        try {
            return Mime.getInputStream(mMessage.getMimeMessage());
        } catch (Exception e) {
            ZimbraLog.mailbox.error("Unable to get stream to message " + mMessage.getId(), e);
            throw new IOException(e.toString());
        }
    }

    public String getName() {
        return mMessage.getSubject();
    }

    public OutputStream getOutputStream() {
        throw new UnsupportedOperationException();
    }
}
