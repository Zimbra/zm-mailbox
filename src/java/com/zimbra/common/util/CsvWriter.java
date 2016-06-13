/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.common.util;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;

/**
 * @author pshao
 */
public class CsvWriter {

    private BufferedWriter writer;
    
    public CsvWriter(Writer writer) throws IOException {
        this.writer = new BufferedWriter(writer);
    }
    
    public void writeRow(String... values) throws IOException {
        StringBuilder line = new StringBuilder();
        
        boolean first = true;
        for (String value : values) {
            if (!first) {
                line.append(","); 
            } else {
                first = false;
            }
            line.append(value);
        }
        line.append("\n");
        writer.write(line.toString());
    }
    
    public void close() throws IOException {
        writer.close();
    }
}
