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
