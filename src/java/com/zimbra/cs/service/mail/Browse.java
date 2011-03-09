/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011 Zimbra, Inc.
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
package com.zimbra.cs.service.mail;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.index.BrowseTerm;
import com.zimbra.cs.index.DomainBrowseTerm;
import com.zimbra.cs.mailbox.Mailbox.BrowseBy;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * @since May 26, 2004
 * @author schemers
 */
public class Browse extends MailDocumentHandler  {

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Mailbox mbox = getRequestedMailbox(zsc);

        String browseBy = request.getAttribute(MailConstants.A_BROWSE_BY);
        BrowseBy parsedBrowseBy = BrowseBy.valueOf(browseBy.toLowerCase());
        String regex = request.getAttribute(MailConstants.A_REGEX, "").toLowerCase();
        int max = (int)(request.getAttributeLong(MailConstants.A_MAX_TO_RETURN, 0));

        List<BrowseTerm> terms;
        try {
            terms = mbox.browse(getOperationContext(zsc, context), parsedBrowseBy, regex, max);
        } catch (IOException e) {
            throw ServiceException.FAILURE("Failed to browse terms", e);
        }

        Element response = zsc.createElement(MailConstants.BROWSE_RESPONSE);

        for (BrowseTerm term : terms) {
            if (term instanceof DomainBrowseTerm) {
                DomainBrowseTerm domain = (DomainBrowseTerm) term;
                Element e = response.addElement(MailConstants.E_BROWSE_DATA).setText(domain.getText());
                String flags = domain.getHeaderFlags();
                if (!flags.isEmpty()) {
                    e.addAttribute(MailConstants.A_BROWSE_DOMAIN_HEADER, flags);
                }
                e.addAttribute(MailConstants.A_FREQUENCY, domain.getFreq());
            } else {
                Element e = response.addElement(MailConstants.E_BROWSE_DATA).setText(term.getText());
                e.addAttribute(MailConstants.A_FREQUENCY, term.getFreq());
            }
        }
        return response;
    }
}
