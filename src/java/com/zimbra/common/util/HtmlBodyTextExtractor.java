/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite, Network Edition.
 * Copyright (C) 2010, 2011, 2012, 2013 Zimbra Software, LLC.  All Rights Reserved.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.common.util;

import java.io.IOException;
import java.io.Reader;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class HtmlBodyTextExtractor extends org.xml.sax.helpers.DefaultHandler {
    private StringBuilder sb = new StringBuilder(1024);
    private boolean inCharacters = false;
    private boolean inBody = false;
    private int maxLength;

    public HtmlBodyTextExtractor(int maxLength) {
        this.maxLength = maxLength;
    }

    @Override
    public void startDocument() {
        sb.setLength(0);
    }

    @Override
    public void startElement(String uri, String localName, String qName, org.xml.sax.Attributes attributes) {
        if (sb.length() >= maxLength)
            return;

        String element = localName.toLowerCase();
        if ("body".equals(element))
            inBody = true;

        if (inBody && !"style".equals(element) && !"script".equals(element) && attributes != null) {
            String href = attributes.getValue("href");
            if (href != null && !href.equals("")) {
                if (sb.length() > 0)
                    sb.append(' ');
                sb.append('<').append(href).append('>');
                href = null;
            }
            if ("img".equals(element)) {
                String src = attributes.getValue("src");
                if (src != null && !src.equals("")) {
                    if (sb.length() > 0)
                        sb.append(' ');
                    sb.append('[').append(src).append(']');
                    src = null;
                }

                String alt = attributes.getValue("alt");
                if (alt != null && !alt.equals("")) {
                    if (sb.length() > 0)
                        sb.append(' ');
                    sb.append('[').append(alt).append(']');
                    alt = null;
                }
            }
        }
        inCharacters = false;
    }

    @Override
    public void characters(char[] ch, int offset, int length) {
        if (length == 0 || sb.length() >= maxLength)
            return;

        if (inBody) {
            int original = offset;
            // trim leading spaces (don't bother with trailers; the output isn't for public viewing)
            while (length > 0) {
                char c = ch[offset];
                if (c > ' ' && c != 0xA0)
                    break;
                offset++;
                length--;
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
                // bug 77254, replace 0x00 with space
                for (int i = 0; i < sb.length(); i++) {
                    if (sb.charAt(i) == 0x00) {
                        sb.setCharAt(i, ' ');
                    }
                }
            }
        }
        inCharacters = (length > 0);
    }

    @Override
    public void endElement(String uri, String localName, String qName) {
        if (sb.length() > maxLength)
            return;

        String element = localName.toLowerCase();
        if ("body".equals(element))
            inBody = false;

        inCharacters = false;

        if ("br".equals(element)) {
            if (sb.length() < maxLength)
                sb.append("\r\n");
        }
    }

    public String toString() {
        return sb.toString();
    }

    /**
     * Extracts text from the HTML returned by the given <tt>Reader</tt>.
     * 
     * @param htmlReader <tt>Reader</tt> that returns the HTML text
     * @param sizeLimit maximum number of characters to extract excluding hrefs
     * @return the extracted text
     */
    public static String extract(Reader htmlReader, int sizeLimit) throws IOException, SAXException {
        org.xml.sax.XMLReader parser = new org.cyberneko.html.parsers.SAXParser();
        HtmlBodyTextExtractor handler = new HtmlBodyTextExtractor(sizeLimit);
        parser.setContentHandler(handler);
        parser.parse(new InputSource(htmlReader));
        return handler.toString();
    }

}
