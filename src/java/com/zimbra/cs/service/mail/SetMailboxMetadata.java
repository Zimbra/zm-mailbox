/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010 Zimbra, Inc.
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

    private static final int TOTAL_METADATA_LIMIT = 10000;
    
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
