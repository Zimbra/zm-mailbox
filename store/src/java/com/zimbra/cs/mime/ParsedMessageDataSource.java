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
