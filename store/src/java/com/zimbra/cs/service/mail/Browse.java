/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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
