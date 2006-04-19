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
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on May 26, 2004
 */
package com.zimbra.cs.service.mail;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.zimbra.cs.mailbox.BrowseResult;
import com.zimbra.cs.mailbox.BrowseResult.DomainItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.soap.DocumentHandler;
import com.zimbra.soap.Element;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * @author schemers
 */
public class Browse extends DocumentHandler  {
    
    public Element handle(Element request, Map context) throws ServiceException {
        ZimbraSoapContext lc = getZimbraSoapContext(context);
        Mailbox mbox = getRequestedMailbox(lc);
        
        String browseBy = request.getAttribute("browseby", null);
        if (browseBy == null)
            browseBy = request.getAttribute(MailService.A_BROWSE_BY);
        
        BrowseResult browse;
        try {
            browse = mbox.browse(lc.getOperationContext(), browseBy);
        } catch (IOException e) {
            throw ServiceException.FAILURE("IO error", e);
        }
        
        Element response = lc.createElement(MailService.BROWSE_RESPONSE);
        
        List result = browse.getResult();
        if (result != null) {
            for (Iterator mit = result.iterator(); mit.hasNext();) {
                Object o = mit.next();
                if (o instanceof String) {
                    response.addElement(MailService.E_BROWSE_DATA).setText((String) o);
                } else if (o instanceof DomainItem) {
                    DomainItem domain = (DomainItem) o;
                    Element e = response.addElement(MailService.E_BROWSE_DATA).setText(domain.getDomain());
                    String flags = domain.getHeaderFlags();
                    if (!flags.equals(""))
                        e.addAttribute(MailService.A_BROWSE_DOMAIN_HEADER, flags);
                } else {
                    throw new RuntimeException("unknown browse item: " + o.getClass().getName() + " " + o);
                }
            }
        }
        return response;
    }
}
