/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2009 Zimbra, Inc.
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

/*
 * Created on Feb 4, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.zimbra.cs.html;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;

import org.apache.xerces.xni.parser.XMLDocumentFilter;
import org.apache.xerces.xni.parser.XMLInputSource;
import org.apache.xerces.xni.parser.XMLParserConfiguration;
import org.cyberneko.html.HTMLConfiguration;

import com.zimbra.common.util.ByteUtil;

public class HtmlDefang
{
    public static String defang(String html, boolean neuterImages) throws IOException {
        return defang(new StringReader(html), neuterImages);
    }
    
    public static String defang(InputStream html, boolean neuterImages)
    throws IOException {
        StringWriter writer = new StringWriter();
        defang(html, neuterImages, writer);
        return writer.toString(); 
    }
    
    public static void defang(InputStream html, boolean neuterImages, Writer out)
    throws IOException {
        XMLInputSource source = new XMLInputSource(null, null, null, html, null);
        defang(source, neuterImages, out);        
    }
    
    public static String defang(Reader htmlReader, boolean neuterImages)
    throws IOException {
        StringWriter writer = new StringWriter();
        defang(htmlReader, neuterImages, writer);
        return writer.toString();
    }
    
    public static void defang(Reader htmlReader, boolean neuterImages, Writer out)
    throws IOException {
        XMLInputSource source = new XMLInputSource(null, null, null, htmlReader, null);
        defang(source, neuterImages, out);
    }
    
    /**
     * @param source HTML source
     * @param neuterImages <tt>true</tt> to remove images
     * @param maxChars maximum number of characters to return, or <tt><=0</tt> for no limit
     */
    public static void defang(XMLInputSource source, boolean neuterImages, Writer out)
    throws IOException {
        // create writer filter
        // TODO: uft-8 right?
        /*
        org.cyberneko.html.filters.Writer writer =
            new org.cyberneko.html.filters.Writer(out, "utf-8");
            */
        DefangWriter writer = new DefangWriter(out, "utf-8");
        
        DefangFilter defang = new DefangFilter(neuterImages);
        
        // setup filter chain
        XMLDocumentFilter[] filters = {
            defang,
            writer,
        };

        // create HTML parser
        XMLParserConfiguration parser = new HTMLConfiguration();
        parser.setProperty("http://cyberneko.org/html/properties/filters", filters);
        parser.setProperty("http://cyberneko.org/html/properties/names/elems", "match"); 

        parser.setFeature("http://cyberneko.org/html/features/balance-tags", false); 
        parser.setFeature("http://xml.org/sax/features/namespaces", false);
        // parse document
    
        parser.parse(source);
    }

    public static void main(String[] args) throws IOException {
        String html = new String(ByteUtil.getContent(new File(args[0])));
        System.out.println(defang(html, true));
    }
}
