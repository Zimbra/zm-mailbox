/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2004, 2005, 2006, 2007 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
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

import javax.activation.DataSource;
import javax.mail.internet.MimeMessage;

import org.apache.lucene.document.Document;

import com.zimbra.common.util.ByteUtil;
import com.zimbra.cs.convert.AttachmentInfo;
import com.zimbra.cs.mime.MimeHandler;
import com.zimbra.cs.mime.MimeHandlerException;
import com.zimbra.cs.util.JMSession;

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

    @Override protected String getContentImpl() throws MimeHandlerException {
        DataSource source = getDataSource();
        InputStream is = null;
        try {
            // FIXME: should just read headers, not entire message
            MimeMessage mm = new MimeMessage(JMSession.getSession(), is = source.getInputStream());
            String subject = mm.getSubject();
            return (subject != null ? subject : "");
        } catch (Exception e) {
            throw new MimeHandlerException(e);
        } finally {
            ByteUtil.closeStream(is);
        }
    }

    @Override public boolean doConversion() {
        return false;
    }

    @Override public String convert(AttachmentInfo doc, String baseURL) {
        throw new IllegalStateException("no need to convert message/rfc822 content");
    }
}
