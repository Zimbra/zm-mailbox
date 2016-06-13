/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2007, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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
import org.cyberneko.html.filters.Writer;

/**
 * extend Writer to override printEntity behavior. IE doesn't like "apos" entities, so write
 * out #39 instead.
 */
public class DefangWriter extends Writer {
    /** Print entity. */
    public DefangWriter(java.io.Writer writer, String encoding) {
        super(writer, encoding);
    }
    
    protected void printEntity(String name) {
        fPrinter.print('&');
        if (name.equals("apos"))
            fPrinter.print("#39");
        else
            fPrinter.print(name);
        fPrinter.print(';');
        fPrinter.flush();
    }
}
