/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014 Zimbra, Inc.
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
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
        if (contentTypeLowerCase.startsWith(MimeConstants.CT_TEXT_HTML) ||
            contentTypeLowerCase.startsWith(MimeConstants.CT_APPLICATION_ZIMBRA_DOC) ||
            contentTypeLowerCase.startsWith(MimeConstants.CT_APPLICATION_ZIMBRA_SLIDES) ||
            contentTypeLowerCase.startsWith(MimeConstants.CT_APPLICATION_ZIMBRA_SPREADSHEET)) {
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
