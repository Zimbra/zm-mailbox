/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2009, 2010 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
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
