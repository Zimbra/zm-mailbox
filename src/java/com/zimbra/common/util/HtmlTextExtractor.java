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
package com.zimbra.common.util;

import java.io.IOException;
import java.io.Reader;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class HtmlTextExtractor extends org.xml.sax.helpers.DefaultHandler {
    private StringBuilder sb = new StringBuilder(1024);
    private String title = null;
    boolean inTitle = false;
    boolean inCharacters = false;
    int skipping = 0;
    int maxLength;

    public HtmlTextExtractor(int maxLength) {
        this.maxLength = maxLength;
    }
    
    @Override public void startDocument() { sb.setLength(0); }
    @Override public void startElement(String uri, String localName, String qName, org.xml.sax.Attributes attributes) {
        if (sb.length() >= maxLength) {
            return;
        }
        
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
        if (skipping > 0 || length == 0 || sb.length() >= maxLength) {
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
                if (sb.length() + length > maxLength) {
                    sb.append(ch, offset, maxLength - sb.length());
                } else {
                    sb.append(ch, offset, length);
                }
            }
        }
        inCharacters = (length > 0);
    }
    @Override public void endElement(String uri, String localName, String qName) {
        if (sb.length() > maxLength) {
            return;
        }
        
        String element = localName.toUpperCase();
        if ("TITLE".equals(element))
            inTitle = false;
        else if ("STYLE".equals(element) || "SCRIPT".equals(element))
            skipping--;
        inCharacters = false;
    }

    @Override public String toString()  { return sb.toString(); }
    public String getTitle()  { return title == null ? "" : title; }
    
    /**
     * Extracts text from the HTML returned by the given <tt>Reader</tt>. 
     * @param htmlReader <tt>Reader</tt> that returns the HTML text
     * @param sizeLimit maximum number of characters to extract
     * @return the extracted text
     */
    public static String extract(Reader htmlReader, int sizeLimit)
    throws IOException, SAXException {
        org.xml.sax.XMLReader parser = new org.cyberneko.html.parsers.SAXParser();
        HtmlTextExtractor handler = new HtmlTextExtractor(sizeLimit);
        parser.setContentHandler(handler);
        parser.setFeature("http://cyberneko.org/html/features/balance-tags", false); 
        parser.parse(new InputSource(htmlReader));
        return handler.toString();
    }
}
