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
