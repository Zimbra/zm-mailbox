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
 */
package com.zimbra.cs.mime.handler;

import java.io.Reader;

import javax.activation.DataSource;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;

import com.zimbra.cs.convert.AttachmentInfo;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.mime.MimeHandler;
import com.zimbra.cs.mime.MimeHandlerException;

/**
 * @author schemers
 *
 *  class that creates a Lucene document from a Java Mail Message
 */
public class TextHtmlHandler extends MimeHandler {

    String mContent;
    private org.xml.sax.XMLReader mParser;
    private ContentExtractor mHandler;
    private Reader mReader;

    private class ContentExtractor extends org.xml.sax.helpers.DefaultHandler {
        private StringBuffer sb = new StringBuffer();
        private String title = null;
        boolean inTitle = false;
        boolean inCharacters = false;
        int skipping = 0;

        public void startDocument() { sb.setLength(0); }
        public void startElement(String uri, String localName, String qName, org.xml.sax.Attributes attributes) {
            String element = localName.toUpperCase();
            if ("TITLE".equals(element))
                inTitle = true;
            else if ("STYLE".equals(element) || "SCRIPT".equals(element))
                skipping++;
            else if ("IMG".equals(element) && attributes != null) {
                String altText = attributes.getValue("alt");
                if (altText != null && !altText.equals("")) {
                    if (sb.length() > 0)
                        sb.append(' ');
                    sb.append('[').append(altText).append(']');
                }
            }
            inCharacters = false;
        }
        public void characters(char[] ch, int offset, int length) {
            if (skipping > 0 || length == 0) {
                return;
            } else if (inTitle) {
                String content = new String(ch, offset, length);
                if (length > 0) {
                    if (title == null)
                        title = content;
                    else
                        title = title + (inCharacters ? "" : " ") + content;
                }
            } else {
                int original = offset;
                // trim leading spaces (don't bother with trailers; the output isn't for public viewing)
                while (length > 0) {
                    char c = ch[offset];
                    if (c > ' ' && c != 0xA0)
                        break;
                    offset++;  length--;
                }
                // and if there's anything left, append it
                if (length > 0) {
                    if (sb.length() > 0 && (!inCharacters || original != offset))
                        sb.append(' ');
                    sb.append(ch, offset, length);
                }
            }
            inCharacters = (length > 0);
        }
        public void endElement(String uri, String localName, String qName) {
            String element = localName.toUpperCase();
            if ("TITLE".equals(element))
                inTitle = false;
            else if ("STYLE".equals(element) || "SCRIPT".equals(element))
                skipping--;
            inCharacters = false;
        }

        public String toString()  { return sb.toString(); }
        public String getTitle()  { return title == null ? "" : title; }
        public String getSummary() {
            if (title != null && mContent.startsWith(title))
                return title;
            if (mContent.length() < 200)
                return mContent;
            int wsp = Math.max(mContent.lastIndexOf(' ', 200), mContent.lastIndexOf('\n', 200));
            return (wsp == -1 ? mContent.substring(0, 200) : mContent.substring(0, wsp - 1)).trim();
        }
    }

    public void init(DataSource source) throws MimeHandlerException {
        super.init(source);
        try {
            mContent = null;
            mReader = Mime.decodeText(source.getInputStream(), source.getContentType());
            mParser = new org.cyberneko.html.parsers.SAXParser();
            mHandler = new ContentExtractor();
            mParser.setContentHandler(mHandler);
        } catch (Exception e) {
            throw new MimeHandlerException(e);
        }
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.mime.MimeHandler#populate(org.apache.lucene.document.Document)
     */
    public void addFields(Document doc) throws MimeHandlerException {
        // make sure we've parsed the document
        getContentImpl();
        // Add the summary as an UnIndexed field, so that it is stored and returned
        // with hit documents for display.
        doc.add(Field.UnIndexed("summary", mHandler.getSummary()));
        // Add the title as a separate Text field, so that it can be searched
        // separately.
        doc.add(Field.Text("title", mHandler.getTitle()));
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.mime.MimeHandler#getContent()
     */
    protected String getContentImpl() throws MimeHandlerException {
        if (mContent == null) {
            try {
                mParser.parse(new org.xml.sax.InputSource(mReader));
                mContent = mHandler.toString();
            } catch (Exception e) {
                throw new MimeHandlerException(e);
            }
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
    public String convert(AttachmentInfo doc, String baseURL) {
        throw new UnsupportedOperationException();
    }
}
