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
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s):
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.formatter;

import java.io.IOException;
import java.util.Iterator;

import javax.servlet.http.HttpServletResponse;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.index.MailboxIndex;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.service.UserServletException;
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
    public void formatCallback(Context context, MailItem item) throws ServiceException, IOException {
        Element elt = getFactory().createElement("items");
        ItemIdFormatter ifmt = new ItemIdFormatter(context.authAccount, item.getMailbox(), false);

        Iterator<? extends MailItem> iterator = null;
        try {
            iterator = getMailItems(context, item, getDefaultStartTime(), getDefaultEndTime(), Integer.MAX_VALUE);
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

    @Override
    public void saveCallback(byte[] body, Context context, String contentType, Folder folder, String filename) throws UserServletException {
        throw new UserServletException(HttpServletResponse.SC_BAD_REQUEST, "format not supported for save");
    }

}
