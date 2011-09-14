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
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;

import org.apache.xerces.xni.parser.XMLInputSource;

/**
 * Does nothing interesting...
 * @author jpowers
 *
 */
public class NoopDefang implements BrowserDefang {
    /*
     * The size of the buffer used while copying input to output 
     */
    private static final int COPY_BUFFER = 256;
    
    @Override
    public String defang(String text, boolean neuterImages) throws IOException {
        return text;
    }

    @Override
    public String defang(InputStream is, boolean neuterImages)
            throws IOException {
        InputStreamReader reader = new InputStreamReader(is, "utf-8");
        return defang(reader, neuterImages);
    }

    @Override
    public String defang(Reader reader, boolean neuterImages)
            throws IOException {
        StringWriter writer = new StringWriter();
        char buffer[] = new char[256];
        int read = 0;
        while((read = reader.read(buffer)) > 0) {
            writer.write(buffer, 0, read);
        }
        
        return writer.toString();
    }

    @Override
    public void defang(InputStream is, boolean neuterImages, Writer out)
            throws IOException {
        InputStreamReader reader = new InputStreamReader(is, "utf-8");
        defang(reader,neuterImages,out);

    }

    @Override
    public void defang(Reader reader, boolean neuterImages, Writer out)
            throws IOException {
        char buffer[] = new char[256];
        int read = 0;
        while((read = reader.read(buffer)) > 0) {
            out.write(buffer, 0, read);
        }
    }


}
