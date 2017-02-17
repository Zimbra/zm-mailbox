/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.html;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.regex.Pattern;

import org.apache.xerces.xni.parser.XMLInputSource;

/**
 * This interface is implemented by different filters to 
 * keep malicious content out of the text/human readable code that we distribute 
 * down to the browsers.
 * 
 * @author jpowers
 *
 */
public interface BrowserDefang {
    
    // regex for URLs href. TODO: beef this up
    public static final Pattern VALID_URL = Pattern.compile("^(https?://[\\w-].*|mailto:.*|cid:.*|notes:.*|smb:.*|ftp:.*|gopher:.*|news:.*|tel:.*|callto:.*|webcal:.*|feed:.*:|file:.*|#.+)", Pattern.CASE_INSENSITIVE);
    public static final Pattern VALID_IMG = Pattern.compile("^data:|^cid:|\\.(jpg|jpeg|png|gif)$");

    /**
     * Defangs a text element
     * @param text
     * @param neuterImages
     * @return A safe version of that text to allow a browser to display
     * @throws IOException
     */
    public String defang(String text, boolean neuterImages) throws IOException; 
   
    /**
     * Reads an input stream and returns a string representation that's safe for browser consumption
     * @param is
     * @param neuterImages
     * @return
     * @throws IOException
     */
    public String defang(InputStream is, boolean neuterImages) throws IOException; 

    /**
     * Uses a reader as an input, outputs safe text.
     * @param reader
     * @param neuterImages
     * @return
     * @throws IOException
     */
    public String defang(Reader reader, boolean neuterImages) throws IOException; 

    /**
     * Defangs an input stream, writes it out to a writer
     * @param is
     * @param neuterImages
     * @param out
     * @throws IOException
     */
    public void defang(InputStream is, boolean neuterImages, Writer out) throws IOException; 
    
    /**
     * Defangs a reader, sends it to a writer.
     * A wise person might have just make this the interface and gotten rid of the other methods
     * @param reader
     * @param neuterImages
     * @param out
     * @throws IOException
     */
    public void defang(Reader reader, boolean neuterImages, Writer out) throws IOException; 
    
}
