/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.qa.unittest;

import com.zimbra.cs.mime.MimeHandler;
import com.zimbra.cs.mime.MimeHandlerManager;
import com.zimbra.cs.mime.handler.TextEnrichedHandler;
import com.zimbra.cs.mime.handler.TextHtmlHandler;
import com.zimbra.cs.mime.handler.UnknownTypeHandler;

import junit.framework.TestCase;


public class TestMimeHandler extends TestCase {

    /**
     * Confirms <tt>MimeHandler</tt> lookups return the correct instance.
     */
    public void testMimeHandler()
    throws Exception {
        checkHandler("text/html", "html", TextHtmlHandler.class);
        checkHandler("text/html", "htm", TextHtmlHandler.class);
        checkHandler("text/html", null, TextHtmlHandler.class);
        checkHandler(null, "html", TextHtmlHandler.class);
        checkHandler(null, "htm", TextHtmlHandler.class);
        
        checkHandler("text/enriched", "txe", TextEnrichedHandler.class);
        checkHandler(null, "txe", TextEnrichedHandler.class);
        checkHandler("text/enriched", null, TextEnrichedHandler.class);
        
        checkHandler("application/octet-stream", "exe", UnknownTypeHandler.class);
        checkHandler("application/octet-stream", null, UnknownTypeHandler.class);
        checkHandler(null, "exe", UnknownTypeHandler.class);
        
        checkHandler(null, null, UnknownTypeHandler.class);
        checkHandler("", "", UnknownTypeHandler.class);
    }
    
    private void checkHandler(String mimeType, String extension, Class handlerClass)
    throws Exception {
        MimeHandler handler = MimeHandlerManager.getMimeHandler(mimeType, "filename." + extension);
        assertEquals("mimeType=" + mimeType + ", ext=" + extension, handlerClass.getName(), handler.getClass().getName());
    }
}
