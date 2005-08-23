/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: ZPL 1.1
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
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

import java.io.IOException;

import javax.activation.DataSource;
import javax.mail.internet.MimeMessage;

import org.apache.lucene.document.Document;

import com.zimbra.cs.convert.AttachmentInfo;
import com.zimbra.cs.convert.ConversionException;
import com.zimbra.cs.mime.MimeHandler;
import com.zimbra.cs.mime.MimeHandlerException;
import com.zimbra.cs.util.JMSession;


/**
 * @author schemers
 *
 *  class that creates a Lucene document from a Java Mail Message
 */
public class MessageRFC822Handler extends MimeHandler {

    private MimeMessage mMessage;
    
    public void init(DataSource source) throws MimeHandlerException {
        super.init(source);
        try {
            mMessage = new MimeMessage(JMSession.getSession(), source.getInputStream());
        } catch (Exception e) {
            throw new MimeHandlerException(e);
        }
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.mime.MimeHandler#populate(org.apache.lucene.document.Document)
     */
    public void addFields(Document doc) throws MimeHandlerException {
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.mime.MimeHandler#getContent()
     */
    protected String getContentImpl() throws MimeHandlerException {
        try {
            return mMessage.getSubject();
        } catch (Exception e) {
            throw new MimeHandlerException(e);
        }
    }
    
    /**
     * No need to convert rfc822 messages ever.
     */
    public boolean doConversion() {
        return false;
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.mime.MimeHandler#convert(com.zimbra.cs.convert.AttachmentInfo, java.lang.String)
     */
    public String convert(AttachmentInfo doc, String baseURL) throws IOException, ConversionException {
        throw new IllegalStateException("no need to convert message/rfc822 content");
    }

}
