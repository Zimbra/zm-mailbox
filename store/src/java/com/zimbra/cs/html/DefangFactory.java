/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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

import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Provisioning;

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
     * This defanger is used for OWASP
     */
    private static OwaspHtmlSanitizer owaspDefang = new OwaspHtmlSanitizer();
    
    /**
     * 
     * @param contentType
     * @return
     */
    public static BrowserDefang getDefanger(String contentType){
        try {
            boolean isOwasp = Provisioning.getInstance().getConfig().getBooleanAttr(Provisioning.A_zimbraUseOwaspHtmlSanitizer, Boolean.TRUE);
            if (isOwasp) {
                return owaspDefang;
            }
        } catch (ServiceException e) {
            // in case of exception, return noopDefang
            return noopDefang;
        }

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
           contentTypeLowerCase.startsWith(MimeConstants.CT_IMAGE_SVG) ||
           contentTypeLowerCase.startsWith(MimeConstants.CT_TEXT_XML_LEGACY)) {
            return xhtmlDefang;
        }
        return noopDefang;
    }
}
