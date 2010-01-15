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
package com.zimbra.cs.service.formatter;

import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.service.UserServletException;
import com.zimbra.cs.service.UserServlet.Context;

public class TgzFormatter extends TarFormatter {
    @Override public String[] getDefaultMimeTypes() {
        return new String[] { "application/x-compressed-tar" };
    }

    @Override public String getType() { return "tgz"; }
    
    protected ArchiveInputStream getInputStream(Context context,
        String charset) throws IOException, ServiceException, UserServletException {
        return new TarArchiveInputStream(new GZIPInputStream(
            context.getRequestInputStream(-1)), charset);
    }

    protected ArchiveOutputStream getOutputStream(Context context, String
        charset) throws IOException {
        return new TarArchiveOutputStream(new GZIPOutputStream(
            context.resp.getOutputStream()), charset);
    }
}
