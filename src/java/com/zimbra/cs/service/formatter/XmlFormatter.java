/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2009, 2010 Zimbra, Inc.
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
package com.zimbra.cs.service.formatter;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.index.MailboxIndex;
import com.zimbra.cs.mailbox.CalendarItem;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.CalendarItem.Instance;
import com.zimbra.cs.service.UserServletContext;
import com.zimbra.cs.service.formatter.FormatterFactory.FormatType;
import com.zimbra.cs.service.mail.ToXML;
import com.zimbra.cs.service.util.ItemIdFormatter;

public class XmlFormatter extends Formatter {

    @Override
    public FormatType getType() {
        return FormatType.XML;
    }

   

    @Override
    public Set<MailItem.Type> getDefaultSearchTypes() {
         return MailboxIndex.SEARCH_FOR_EVERYTHING;
    }

    @Override
    public void formatCallback(UserServletContext context) throws ServiceException, IOException {
        Element elt = getFactory().createElement("items");
        ItemIdFormatter ifmt = new ItemIdFormatter(context.authAccount, context.targetMailbox, false);

        Iterator<? extends MailItem> iterator = null;
        try {
            long start = context.getStartTime();
            long end = context.getEndTime();
            boolean hasTimeRange = start != TIME_UNSPECIFIED && end != TIME_UNSPECIFIED;
            iterator = getMailItems(context, start, end, Integer.MAX_VALUE);
            // this is lame
            while (iterator.hasNext()) {
                MailItem item = iterator.next();
                if (item instanceof CalendarItem && hasTimeRange) {
                    // Skip appointments that have no instance in the time range.
                    CalendarItem calItem = (CalendarItem) item;
                    Collection<Instance> instances = calItem.expandInstances(start, end, false);
                    if (instances.isEmpty())
                        continue;
                }
                ToXML.encodeItem(elt, ifmt, context.opContext, item, ToXML.NOTIFY_FIELDS);
            }

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
