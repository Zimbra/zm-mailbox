/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007 Zimbra, Inc.
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
package com.zimbra.cs.mime.handler;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.zimbra.cs.mime.Mime;

public class TextEnrichedHandler extends TextHtmlHandler {

    @Override protected Reader getReader(InputStream is, String ctype) throws IOException {
        String content = Mime.decodeText(is, ctype, null);
        return new StringReader(convertToHTML(content));
    }

    private static Map<String,String> sConversions = new HashMap<String,String>();
        static {
            sConversions.put("bold", "<b>");       sConversions.put("/bold", "</b>");
            sConversions.put("italic", "<i>");     sConversions.put("/italic", "</i>");
            sConversions.put("underline", "<u>");  sConversions.put("/underline", "</u>");
            sConversions.put("fixed", "<tt>");     sConversions.put("/fixed", "</tt>");
            sConversions.put("excerpt", "<q>");    sConversions.put("/excerpt", "</q>");

            sConversions.put("smaller", "<font size=-1>");          sConversions.put("/smaller", "</font>");
            sConversions.put("bigger", "<font size=+1>");           sConversions.put("/bigger", "</font>");
            sConversions.put("center", "<div align=center>");       sConversions.put("/center", "</div>");
            sConversions.put("flushleft", "<div align=left>");      sConversions.put("/flushleft", "</div>");
            sConversions.put("flushright", "<div align=right>");    sConversions.put("/flushright", "</div>");
            sConversions.put("flushboth", "<div align=justify>");   sConversions.put("/flushboth", "</div>");

            sConversions.put("/color", "</font>");
            sConversions.put("/fontfamily", "</font>");
        }

    public static String convertToHTML(String content) {
        if (content == null)
            return null;

        StringBuilder sb = new StringBuilder(content.length());
        int c, last = '\0', nofill = 0;
        for (int pos = 0, length = content.length(); pos < length; pos++) {
            switch (c = content.charAt(pos)) {
                case '\r':  continue;  // *completely* ignore CR
                case '\n':  sb.append(nofill > 0 || last == c ? "<br/>\n" : " ");  break;
                case ' ':   if (last == c || (last != 0xA0 && pos != length - 1 && content.charAt(pos + 1) == ' ')) {
                                sb.append("&nbsp;");  c = 0xA0;  break;
                            }
                            // fall through if the last char wasn't also a space...
                default:    sb.append((char) c);  break;
                case '&':   sb.append("&amp;");  break;
                case '>':   sb.append("&gt;");  break;
                case '<':
                    int gt = content.indexOf('>', pos);
                    if (gt == -1 || gt - pos > 61) {
                        sb.append("&lt;");  break;
                    } else if (content.charAt(pos + 1) == '<') {
                        sb.append("&lt;");  pos++;  break;
                    }
                    String format = content.substring(pos + 1, gt).toLowerCase();
                    String tag = sConversions.get(format);
                    if (format.equals("nofill")) {
                        nofill++;
                    } else if (format.equals("/nofill")) {
                        nofill = Math.max(--nofill, 0);
                    } else if (tag != null) {
                        sb.append(tag);
                    } else if (format.equals("param")) {
                        int end = content.indexOf("</param>", gt);
                        if (end != -1)
                            gt = end + 7;
                    } else if (format.equals("color")) {
                        sb.append("<font color=#").append(readColorParam(content, gt + 1)).append(">");
                    } else if (format.equals("fontfamily")) {
                        String family = readParam(content, gt + 1);
                        sb.append("<font face=\"").append(family == null ? "Times" : encodeParam(family)).append("\">");
                    }
                    pos = gt;  break;
            }
            last = c;
        }
        return sb.toString();
    }

    private static String readParam(String content, int pos) {
        int end = content.indexOf("</param>", pos);
        if (content.startsWith("<param>", pos) && end != -1)
            return content.substring(pos + 7, end);
        return null;
    }

    private static String encodeParam(String param) {
        StringBuilder sb = new StringBuilder(param.length());
        for (int i = 0, length = param.length(); i < length; i++) {
            int c = param.charAt(i);
            switch (c) {
                case '\r':
                case '\n':  break;
                case '<':   sb.append("&lt;");  break;
                case '>':   sb.append("&gt;");  break;
                case '&':   sb.append("&amp;");  break;
                case '"':   sb.append("&quot;");  break;
                default:    sb.append((char) c);  break;
            }
        }
        return sb.toString();
    }

    private static Map<String,String> sColors = new HashMap<String,String>();
        static {
            sColors.put("red", "FF0000");    sColors.put("blue", "0000FF");
            sColors.put("green", "008000");  sColors.put("yellow", "FFFF00");
            sColors.put("cyan", "00FFFF");   sColors.put("magenta", "FF00FF");
            sColors.put("black", "000000");  sColors.put("white", "FFFFFF");
        }

    private static Pattern RGB_COLOR_PATTERN = Pattern.compile("(\\p{XDigit}{2})\\p{XDigit}{2},(\\p{XDigit}{2})\\p{XDigit}{2},(\\p{XDigit}{2})\\p{XDigit}{2}");

    private static String readColorParam(String content, int pos) {
        String color = readParam(content, pos);
        if (color != null) {
            String triplet = sColors.get(color);
            if (triplet != null)
                return triplet;
            Matcher matcher = RGB_COLOR_PATTERN.matcher(color);
            if (matcher.matches())
                return (matcher.group(1) + matcher.group(2) + matcher.group(3)).toUpperCase();
        }
        // default to black
        return "000000";
    }

    public static void main(String[] args) {
        String test = "<bold>Now</bold> is the time for <italic>all</italic>\ngood men\n<smaller>(and <<women>)</smaller> to\n" +
                "<ignoreme>come</ignoreme>\n\nto the aid of their\n\n\n<color><param>red</param>beloved</color>\ncountry.\n\n" +
                "By the way,\nI think that <paraindent><param>left</param><<smaller>\n</paraindent>should REALLY be called\n\n" +
                "<paraindent><param>left</param><<tinier></paraindent>\nand that I am always right.\n\n<center>-- the end</center>\n";
        System.out.println(convertToHTML(test));
    }
}
