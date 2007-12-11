/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.qa.unittest;

import junit.framework.TestCase;

import com.zimbra.cs.mime.MimeHandler;
import com.zimbra.cs.mime.MimeHandlerManager;
import com.zimbra.cs.mime.handler.TextEnrichedHandler;
import com.zimbra.cs.mime.handler.TextHtmlHandler;
import com.zimbra.cs.mime.handler.UnknownTypeHandler;

/**
 * Tests lookups and other server-side operations
 * related to <tt>MimeHandler</tt>s.  This test cannot run on the client
 * side.
 */
public class TestMimeHandler extends TestCase {

    /**
     * Confirms <tt>MimeHandler</tt> lookups return the correct instance.
     */
    public void testMimeHandlerLookup()
    throws Exception {
        checkHandler("text/html", "html", TextHtmlHandler.class);
        checkHandler("text/html", "htm", TextHtmlHandler.class);
        checkHandler("text/enriched", "txe", TextEnrichedHandler.class);
        checkHandler("application/octet-stream", "exe", UnknownTypeHandler.class);
        checkHandler(null, null, UnknownTypeHandler.class);
        checkHandler("", "", UnknownTypeHandler.class);
    }
    
    public static void checkHandler(String mimeType, String extension, Class<?> handlerClass)
    throws Exception {
        String context = "mimeType=" + mimeType + ", ext=" + extension;
        
        // Get by both type and extension
        MimeHandler handler = MimeHandlerManager.getMimeHandler(mimeType, "filename." + extension);
        assertEquals(context, handlerClass.getName(), handler.getClass().getName());
        
        if (mimeType != null) {
            // Get by type only
            handler = MimeHandlerManager.getMimeHandler(mimeType, null);
            assertEquals(context, handlerClass.getName(), handler.getClass().getName());

            // Get by bogus extension
            handler = MimeHandlerManager.getMimeHandler(mimeType, "filename.foobar");
            assertEquals(context, handlerClass.getName(), handler.getClass().getName());
        }
        
        if (extension != null) {
            // Get by extension only
            handler = MimeHandlerManager.getMimeHandler(null, extension);
            assertEquals(context, handlerClass.getName(), handler.getClass().getName());

            // Get by bogus type
            handler = MimeHandlerManager.getMimeHandler("bogus/type", extension);
            assertEquals(context, handlerClass.getName(), handler.getClass().getName());
        }
    }
    
}
