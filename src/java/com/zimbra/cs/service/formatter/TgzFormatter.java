/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.service.formatter;

import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.service.UserServletContext;
import com.zimbra.cs.service.UserServletException;
import com.zimbra.cs.service.formatter.FormatterFactory.FormatType;

public class TgzFormatter extends TarFormatter {
    @Override public String[] getDefaultMimeTypes() {
        return new String[] { "application/x-compressed-tar" };
    }

    @Override 
    public FormatType getType() { 
        return FormatType.TGZ;
    }
    
    protected ArchiveInputStream getInputStream(UserServletContext context,
        String charset) throws IOException, ServiceException, UserServletException {
        return new TarArchiveInputStream(new GZIPInputStream(
            context.getRequestInputStream(-1)), charset);
    }

    protected ArchiveOutputStream getOutputStream(UserServletContext context, String
        charset) throws IOException {
        return new TarArchiveOutputStream(new GZIPOutputStream(
            context.resp.getOutputStream()), charset);
    }
}
