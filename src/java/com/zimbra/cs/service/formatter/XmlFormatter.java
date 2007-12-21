/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007 Zimbra, Inc.
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
package com.zimbra.cs.service.formatter;

import java.io.IOException;
import java.util.Iterator;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.index.MailboxIndex;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.service.UserServlet.Context;
import com.zimbra.cs.service.mail.ToXML;
import com.zimbra.cs.service.util.ItemIdFormatter;

public class XmlFormatter extends Formatter {

    @Override
    public String getType() {
        return "xml";
    }

    @Override
    public boolean canBeBlocked() {
        return false;
    }

    @Override
    public String getDefaultSearchTypes() {
        return MailboxIndex.SEARCH_FOR_EVERYTHING;
    }

    @Override
    public void formatCallback(Context context) throws ServiceException, IOException {
        Element elt = getFactory().createElement("items");
        ItemIdFormatter ifmt = new ItemIdFormatter(context.authAccount, context.targetMailbox, false);

        Iterator<? extends MailItem> iterator = null;
        try {
            iterator = getMailItems(context, getDefaultStartTime(), getDefaultEndTime(), Integer.MAX_VALUE);
            while (iterator.hasNext())
                ToXML.encodeItem(elt, ifmt, context.opContext, iterator.next(), ToXML.NOTIFY_FIELDS);

            context.resp.getOutputStream().write(elt.toUTF8());
        } finally {
            if (iterator instanceof QueryResultIterator)
                ((QueryResultIterator) iterator).finished();
        }
    }

    Element.ElementFactory getFactory() {
        return Element.XMLElement.mFactory;
    }
}
