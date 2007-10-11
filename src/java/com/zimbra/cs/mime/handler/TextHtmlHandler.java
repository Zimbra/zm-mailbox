/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007 Zimbra, Inc.
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
 */
package com.zimbra.cs.mime.handler;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import javax.activation.DataSource;

import org.apache.lucene.document.Document;

import com.zimbra.common.util.ByteUtil;
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

    private class ContentExtractor extends org.xml.sax.helpers.DefaultHandler {
        private StringBuffer sb = new StringBuffer();
        private String title = null;
        boolean inTitle = false;
        boolean inCharacters = false;
        int skipping = 0;

        @Override public void startDocument() { sb.setLength(0); }
        @Override public void startElement(String uri, String localName, String qName, org.xml.sax.Attributes attributes) {
            String element = localName.toUpperCase();
            if ("TITLE".equals(element)) {
                inTitle = true;
            } else if ("STYLE".equals(element) || "SCRIPT".equals(element)) {
                skipping++;
            } else if ("IMG".equals(element) && attributes != null) {
                String altText = attributes.getValue("alt");
                if (altText != null && !altText.equals("")) {
                    if (sb.length() > 0)
                        sb.append(' ');
                    sb.append('[').append(altText).append(']');
                }
            }
            inCharacters = false;
        }
        @Override public void characters(char[] ch, int offset, int length) {
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
        @Override public void endElement(String uri, String localName, String qName) {
            String element = localName.toUpperCase();
            if ("TITLE".equals(element))
                inTitle = false;
            else if ("STYLE".equals(element) || "SCRIPT".equals(element))
                skipping--;
            inCharacters = false;
        }

        @Override public String toString()  { return sb.toString(); }
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

    @Override protected boolean runsExternally() {
        return false;
    }

    @Override public void addFields(Document doc) throws MimeHandlerException {
        // make sure we've parsed the document
        getContentImpl();
    }

    @Override protected String getContentImpl() throws MimeHandlerException {
        if (mContent == null) {
            DataSource source = getDataSource();
            InputStream is = null;
            try {
                org.xml.sax.XMLReader parser = new org.cyberneko.html.parsers.SAXParser();
                ContentExtractor handler = new ContentExtractor();
                parser.setContentHandler(handler);
                parser.setFeature("http://cyberneko.org/html/features/balance-tags", false); 

                Reader reader = getReader(is = source.getInputStream(), source.getContentType());
                parser.parse(new org.xml.sax.InputSource(reader));
                mContent = handler.toString();
            } catch (Exception e) {
                throw new MimeHandlerException(e);
            } finally {
                ByteUtil.closeStream(is);
            }
        }
        if (mContent == null)
            mContent = "";
        
        return mContent;
    }

    @SuppressWarnings("unused")
    protected Reader getReader(InputStream is, String ctype) throws IOException {
        return Mime.getTextReader(is, ctype, null);
    }
    
    /**
     * No need to convert text/html document ever.
     */
    @Override public boolean doConversion() {
        return false;
    }

    @Override public String convert(AttachmentInfo doc, String baseURL) {
        throw new UnsupportedOperationException();
    }
}
