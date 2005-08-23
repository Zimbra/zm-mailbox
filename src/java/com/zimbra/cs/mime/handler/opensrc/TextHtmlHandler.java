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
 * The Initial Developer of the Original Code is Zimbra, Inc.  Portions
 * created by Zimbra are Copyright (C) 2005 Zimbra, Inc.  All Rights
 * Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Apr 1, 2004
 *
 */
package com.zimbra.cs.mime.handler.opensrc;

import java.io.IOException;
import java.io.Reader;

import javax.activation.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.demo.html.HTMLParser;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;

import com.zimbra.cs.convert.AttachmentInfo;
import com.zimbra.cs.convert.ConversionException;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.mime.MimeHandler;
import com.zimbra.cs.mime.MimeHandlerException;

/**
 * @author schemers
 *
 *  class that creates a Lucene document from a Java Mail Message
 */
public class TextHtmlHandler extends MimeHandler {
    
    private static Log mLog = LogFactory.getLog(TextHtmlHandler.class);

    private static final int MAX_DECODE_BUFFER = 2048;
    
    private String mContent;
    private HTMLParser mParser;
        
    private void copy(Reader reader, StringBuffer buffer)
    	throws IOException 
    {
        char [] cbuff = new char[MAX_DECODE_BUFFER];
        int num;
        while ( (num = reader.read(cbuff, 0, cbuff.length)) != -1) {
	         buffer.append(cbuff, 0, num);
        }
    }

    public void init(DataSource source) throws MimeHandlerException {
        super.init(source);
        try {
            StringBuffer buffer = new StringBuffer();
            Reader reader = Mime.decodeText(source.getInputStream(), source.getContentType());
            mParser = new HTMLParser(reader);
        } catch (Exception e) {
            throw new MimeHandlerException(e);
        }
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.mime.MimeHandler#populate(org.apache.lucene.document.Document)
     */
    public void addFields(Document doc) throws MimeHandlerException {
        try {
            // Add the summary as an UnIndexed field, so that it is stored and returned
            // with hit documents for display.
            doc.add(Field.UnIndexed("summary", mParser.getSummary()));
            // Add the title as a separate Text field, so that it can be searched
            // separately.
            doc.add(Field.Text("title", mParser.getTitle()));
        } catch (IOException e) {
            throw new MimeHandlerException(e);
        } catch (InterruptedException e) {
            throw new MimeHandlerException(e);
        }
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.mime.MimeHandler#getContent()
     */
    protected String getContentImpl() throws MimeHandlerException {
        if (mContent == null) {
            StringBuffer sb = new StringBuffer();
            try {
                copy(mParser.getReader(), sb);
            } catch (IOException e) {
                throw new MimeHandlerException(e);
            }
            mContent = sb.toString();            
        }
        return mContent;
    }
    
    /**
     * No need to convert text/html document ever.
     */
    public boolean doConversion() {
        return false;
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.mime.MimeHandler#convert(com.zimbra.cs.convert.AttachmentInfo, java.lang.String)
     */
    public String convert(AttachmentInfo doc, String baseURL) throws IOException, ConversionException {
        throw new UnsupportedOperationException();
    }
            
}
