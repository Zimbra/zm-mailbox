/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007 Zimbra, Inc.
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

/*
 * Created on May 26, 2004
 */
package com.zimbra.cs.service.mail;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.index.MailboxIndex.BrowseTerm;
import com.zimbra.cs.mailbox.BrowseResult;
import com.zimbra.cs.mailbox.BrowseResult.DomainItem;
import com.zimbra.cs.mailbox.Mailbox.BrowseBy;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * @author schemers
 */
public class Browse extends MailDocumentHandler  {
    
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Mailbox mbox = getRequestedMailbox(zsc);
        
        String browseBy = request.getAttribute(MailConstants.A_BROWSE_BY, null);
        if (browseBy == null)
            browseBy = request.getAttribute(MailConstants.A_BROWSE_BY);
        
        BrowseBy parsedBrowseBy = BrowseBy.valueOf(browseBy.toLowerCase());
        
        String regex = request.getAttribute(MailConstants.A_REGEX, "").toLowerCase();
        
        int max = (int)(request.getAttributeLong(MailConstants.A_MAX_TO_RETURN, 0));
        
        BrowseResult browse;
        try {
            browse = mbox.browse(getOperationContext(zsc, context), parsedBrowseBy, regex, max);
        } catch (IOException e) {
            throw ServiceException.FAILURE("IO error", e);
        }
        
        Element response = zsc.createElement(MailConstants.BROWSE_RESPONSE);
        
        Collection<BrowseTerm> result = browse.getResult();
        if (result != null) {
            for (Iterator<BrowseTerm> mit = result.iterator(); mit.hasNext();) {
                Object o = mit.next();
                if (o instanceof DomainItem) {
                    DomainItem domain = (DomainItem) o;
                    Element e = response.addElement(MailConstants.E_BROWSE_DATA).setText(domain.term);
                    String flags = domain.getHeaderFlags();
                    if (!flags.equals(""))
                        e.addAttribute(MailConstants.A_BROWSE_DOMAIN_HEADER, flags);
                    e.addAttribute("freq", domain.freq);
                } else if (o instanceof BrowseTerm) {
                    BrowseTerm bt = (BrowseTerm)o;
                    Element e = response.addElement(MailConstants.E_BROWSE_DATA).setText(bt.term);
                    e.addAttribute(MailConstants.A_FREQUENCY, bt.freq);
                } else {
                    throw new RuntimeException("unknown browse item: " + o.getClass().getName() + " " + o);
                }
            }
        }
        return response;
    }
}
