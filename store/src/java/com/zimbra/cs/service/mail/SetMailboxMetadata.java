/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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

import java.util.List;
import java.util.Map;

import org.dom4j.QName;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Metadata;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.soap.ZimbraSoapContext;

public class SetMailboxMetadata extends MailDocumentHandler {

    private static final int TOTAL_METADATA_LIMIT = 32768;
    
    private static enum SectionNames {
        zwc, bes
    }
    
    @Override public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Mailbox mbox = getRequestedMailbox(zsc);
        OperationContext octxt = getOperationContext(zsc, context);

        Element meta = request.getElement(MailConstants.E_METADATA);
        String section = meta.getAttribute(MailConstants.A_SECTION);
        section = section.trim();
        if (section.length() == 0 || section.length() > 36 || section.indexOf(':') < 1)
            throw ServiceException.INVALID_REQUEST("invalid mailbox metadata section name", null);
        SectionNames.valueOf(section.substring(0, section.indexOf(':'))); //will throw IllegalArgumentException if not known
        
        Metadata metadata = isModify() ? mbox.getConfig(octxt, section) : null;
        List<Element.KeyValuePair> keyvals = meta.listKeyValuePairs();
        if (!keyvals.isEmpty()) {
            metadata = metadata != null ? metadata : new Metadata();
            for (Element.KeyValuePair kvp : keyvals) {
                String key = kvp.getKey();
                if (key == null || key.equals(""))
                    throw ServiceException.INVALID_REQUEST("empty key not allowed", null);
                String val = kvp.getValue();
                if (val == null || val.equals("")) {
                    if (isModify())
                        metadata.remove(key);
                    else
                        throw ServiceException.INVALID_REQUEST("empty value not allowed", null);
                } else {
                    metadata.put(kvp.getKey(), kvp.getValue());
                }
            }
            if (metadata.isEmpty())
                metadata = null;
            else if (metadata.toString().length() > TOTAL_METADATA_LIMIT)
                throw MailServiceException.TOO_MUCH_METADATA(TOTAL_METADATA_LIMIT);
        } else if (isModify()) {
            throw ServiceException.INVALID_REQUEST("empty key/value set not allowed", null);
        }
        mbox.setConfig(octxt, section, metadata);

        Element response = zsc.createElement(getResponseName());
        return response;
    }
    
    boolean isModify() {
        return false;
    }
    
    QName getResponseName() {
        return MailConstants.SET_MAILBOX_METADATA_RESPONSE;
    }
}
