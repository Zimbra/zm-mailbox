/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2007, 2009 Zimbra, Inc.
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
