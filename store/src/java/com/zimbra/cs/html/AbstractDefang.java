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
