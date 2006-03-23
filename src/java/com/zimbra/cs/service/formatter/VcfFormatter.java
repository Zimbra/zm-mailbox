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
package com.zimbra.cs.service.formatter;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import javax.mail.Part;
import javax.servlet.http.HttpServletResponse;

import com.zimbra.cs.index.MailboxIndex;
import com.zimbra.cs.mailbox.Contact;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.UserServletException;
import com.zimbra.cs.service.UserServlet.Context;
import com.zimbra.cs.service.formatter.VCard;
import com.zimbra.cs.util.HttpUtil;

public class VcfFormatter extends Formatter {

    public String getType() {
        return "vcf";
    }

    public String[] getDefaultMimeTypes() {
        return new String[] { Mime.CT_TEXT_VCARD, "application/vcard" };
    }

    public String getDefaultSearchTypes() {
        return MailboxIndex.SEARCH_FOR_CONTACTS;
    }
    
    public boolean canBeBlocked() {
        return false;
    }

    public void format(Context context, MailItem target) throws IOException, ServiceException {
        Iterator<? extends MailItem> iterator = getMailItems(context, target, getDefaultStartTime(), getDefaultEndTime());

        String filename = target instanceof Contact ? ((Contact) target).getFileAsString() : "contacts";
        String cd = Part.ATTACHMENT + "; filename=" + HttpUtil.encodeFilename(context.req, filename + ".vcf");
        context.resp.addHeader("Content-Disposition", cd);
        context.resp.setContentType(Mime.CT_TEXT_VCARD);
        context.resp.setCharacterEncoding(Mime.P_CHARSET_UTF8);

        int count = 0;
        while (iterator.hasNext()) {
            MailItem item = iterator.next();
            if (!(item instanceof Contact))
                continue;
            VCard vcf = VCard.formatContact((Contact) item);
            context.resp.getOutputStream().write(vcf.formatted.getBytes(Mime.P_CHARSET_UTF8));
            count++;
        }
//        if (count == 0)
//            throw new UserServletException(HttpServletResponse.SC_NO_CONTENT, "no matching contacts");
    }

    public void save(byte[] body, Context context, Folder folder) throws ServiceException, IOException, UserServletException {
        List<VCard> cards = VCard.parseVCard(new String(body, Mime.P_CHARSET_UTF8));
        if (cards == null || cards.size() == 0 || (cards.size() == 1 && cards.get(0).fields.isEmpty()))
            throw new UserServletException(HttpServletResponse.SC_BAD_REQUEST, "no contact fields found in vcard");

        for (VCard vcf : cards) {
            if (vcf.fields.isEmpty())
                continue;
            folder.getMailbox().createContact(context.opContext, vcf.fields, folder.getId(), null);
        }
    }
}
