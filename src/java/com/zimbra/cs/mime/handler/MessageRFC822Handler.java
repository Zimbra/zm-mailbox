/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Apr 1, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.zimbra.cs.mime.handler;

import java.io.InputStream;

import javax.mail.internet.InternetHeaders;

import org.apache.lucene.document.Document;

import com.zimbra.common.util.ByteUtil;
import com.zimbra.cs.convert.AttachmentInfo;
import com.zimbra.cs.mime.MimeHandler;
import com.zimbra.cs.mime.MimeHandlerException;
import com.zimbra.cs.mime.MimeHandlerManager;

/**
 * @author schemers
 *
 *  class that creates a Lucene document from a JavaMail Message
 */
public class MessageRFC822Handler extends MimeHandler {

    @Override protected boolean runsExternally() {
        return false;
    }

    @Override public void addFields(Document doc) {
    }

    /**
     * Returns the subject of the attached message.
     */
    @Override protected String getContentImpl() throws MimeHandlerException {
        InputStream is = null;
        String content = null;
        try {
            is = getDataSource().getInputStream();
            if (is == null) {
                return null;
            }
            InternetHeaders headers = new InternetHeaders(is);
            String[] subject = headers.getHeader("Subject");
            if (subject == null || subject.length == 0 || subject[0] == null) {
                return null;
            }
            int maxLength = MimeHandlerManager.getIndexedTextLimit();
            if (subject[0].length() > maxLength) {
                content = subject[0].substring(0, maxLength);
            } else {
                content = subject[0];
            }
        } catch (Exception e) {
            throw new MimeHandlerException(e);
        } finally {
            ByteUtil.closeStream(is);
        }
        
        return content;
    }

    @Override public boolean doConversion() {
        return false;
    }

    @Override public String convert(AttachmentInfo doc, String baseURL) {
        throw new IllegalStateException("no need to convert message/rfc822 content");
    }
}
