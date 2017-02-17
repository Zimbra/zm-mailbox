/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.service.formatter;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
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
         return SEARCH_FOR_EVERYTHING;
    }

    @Override
    public void formatCallback(UserServletContext context) throws ServiceException, IOException {
        Element elt = getFactory().createElement("items");
        ItemIdFormatter ifmt = new ItemIdFormatter(context.getAuthAccount(), context.targetMailbox, false);

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
