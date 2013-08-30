/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2013 Zimbra Software, LLC.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.4 ("License"); you may not use this file except in
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
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;

import org.apache.xerces.xni.parser.XMLInputSource;

/**
 * Utility class that implements many of the more mundane methods.
 * @author jpowers
 *
 */
public abstract class AbstractDefang implements BrowserDefang {

    public String defang(String html, boolean neuterImages) throws IOException {
        return defang(new StringReader(html), neuterImages);
    }
    
    public String defang(InputStream html, boolean neuterImages)
    throws IOException {
        StringWriter writer = new StringWriter();
        defang(html, neuterImages, writer);
        return writer.toString(); 
    }
    
    
    public String defang(Reader htmlReader, boolean neuterImages)
    throws IOException {
        StringWriter writer = new StringWriter();
        defang(htmlReader, neuterImages, writer);
        return writer.toString();
    }
    
    
    
}
