/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2009, 2010, 2011, 2013, 2014, 2015, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
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
import java.io.Writer;

import org.apache.xerces.xni.parser.XMLDocumentFilter;
import org.apache.xerces.xni.parser.XMLInputSource;
import org.apache.xerces.xni.parser.XMLParserConfiguration;
import org.cyberneko.html.HTMLConfiguration;
import org.cyberneko.html.filters.Purifier;

import com.zimbra.common.util.ByteUtil;

public class HtmlDefang extends AbstractDefang
{
    public void defang(Reader htmlReader, boolean neuterImages, Writer out)
    throws IOException {
        XMLInputSource source = new XMLInputSource(null, null, null, htmlReader, null);
        defang(source, neuterImages, out);
    }
    public void defang(InputStream html, boolean neuterImages, Writer out)
    throws IOException {
        XMLInputSource source = new XMLInputSource(null, null, null, html, null);
        defang(source, neuterImages, out);
    }
    /**
     * @param source HTML source
     * @param neuterImages <tt>true</tt> to remove images
     * @param maxChars maximum number of characters to return, or <tt><=0</tt> for no limit
     */
    protected void defang(XMLInputSource source, boolean neuterImages, Writer out)
    throws IOException {
        // create writer filter
        // TODO: uft-8 right?
        /*
        org.cyberneko.html.filters.Writer writer =
            new org.cyberneko.html.filters.Writer(out, "utf-8");
            */
        DefangWriter writer = new DefangWriter(out, "utf-8");

        DefangFilter defang = new DefangFilter(neuterImages);
        Purifier purifier= new HtmlPurifier();

        // setup filter chain
        XMLDocumentFilter[] filters = {
            purifier,
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
        HtmlDefang defanger = new HtmlDefang();
        System.out.println(defanger.defang(html, true));
    }
}
