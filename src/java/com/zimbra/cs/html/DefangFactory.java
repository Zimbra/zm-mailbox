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

import com.zimbra.common.mime.MimeConstants;

/**
 * This factory is used to determine the proper defanger based on content type for
 * content that can be natively displayed in most browsers and can have unsavory things
 * added to it (mostly xss script issues).
 * @author jpowers
 *
 */
public class DefangFactory {
    /**
     * The instance of the html defanger 
     */
    private static HtmlDefang htmlDefang = new HtmlDefang();
    
    /**
     * The xml defanger, used for xhtml and svg 
     */
    private static XHtmlDefang xhtmlDefang = new XHtmlDefang();
    
    /**
     * This defanger does nothing. Here for
     * backwards compatibility
     */
    private static NoopDefang noopDefang = new NoopDefang();
    
    /**
     * 
     * @param contentType
     * @return
     */
    public static BrowserDefang getDefanger(String contentType){
        if(contentType == null) {
            return noopDefang;
        }
        String contentTypeLowerCase = contentType.toLowerCase();
        if(contentTypeLowerCase.startsWith(MimeConstants.CT_TEXT_HTML)) {
            return htmlDefang;
        }

        if(contentTypeLowerCase.startsWith(MimeConstants.CT_TEXT_XML) ||
           contentTypeLowerCase.startsWith(MimeConstants.CT_APPLICATION_XHTML) ||
           contentTypeLowerCase.startsWith(MimeConstants.CT_IMAGE_SVG)){
            return xhtmlDefang;
        }
        return noopDefang;
    }
}
