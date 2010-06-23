/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
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
import java.util.Iterator;
import java.util.List;
import javax.mail.Part;
import javax.servlet.http.HttpServletResponse;

import com.zimbra.cs.index.MailboxIndex;
import com.zimbra.cs.mailbox.Contact;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.service.UserServletException;
import com.zimbra.cs.service.UserServlet.Context;
import com.zimbra.cs.service.formatter.VCard;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.HttpUtil;
import com.zimbra.common.mime.MimeConstants;

public class VcfFormatter extends Formatter {

    public String getType() {
        return "vcf";
    }

    public String[] getDefaultMimeTypes() {
        return new String[] { MimeConstants.CT_TEXT_VCARD, MimeConstants.CT_TEXT_VCARD_LEGACY, MimeConstants.CT_TEXT_VCARD_LEGACY2 };
    }

    public String getDefaultSearchTypes() {
        return MailboxIndex.SEARCH_FOR_CONTACTS;
    }
    
    public boolean canBeBlocked() {
        return false;
    }

    public void formatCallback(Context context) throws IOException, ServiceException {
        Iterator<? extends MailItem> iterator = null;
        try {
            iterator = getMailItems(context, getDefaultStartTime(), getDefaultEndTime(), Integer.MAX_VALUE);

            String filename = context.target instanceof Contact ? ((Contact) context.target).getFileAsString() : "contacts";
            String cd = Part.ATTACHMENT + "; filename=" + HttpUtil.encodeFilename(context.req, filename + ".vcf");
            context.resp.addHeader("Content-Disposition", cd);
            context.resp.setContentType(MimeConstants.CT_TEXT_VCARD_LEGACY);  // for backward compatibility
            context.resp.setCharacterEncoding(MimeConstants.P_CHARSET_UTF8);

            int count = 0;
            while (iterator.hasNext()) {
                MailItem item = iterator.next();
                if (!(item instanceof Contact))
                    continue;
                VCard vcf = VCard.formatContact((Contact) item);
                context.resp.getOutputStream().write(vcf.formatted.getBytes(MimeConstants.P_CHARSET_UTF8));
                count++;
            }
//            if (count == 0)
//                throw new UserServletException(HttpServletResponse.SC_NO_CONTENT, "no matching contacts");
        } finally {
            if (iterator instanceof QueryResultIterator)
                ((QueryResultIterator) iterator).finished();
        }
    }

    public boolean supportsSave() {
        return true;
    }

    public void saveCallback(Context context, String contentType, Folder folder, String filename) throws ServiceException, IOException, UserServletException {
        byte[] body = context.getPostBody();
        List<VCard> cards = VCard.parseVCard(new String(body, MimeConstants.P_CHARSET_UTF8));
        
        if (cards == null || cards.size() == 0 || (cards.size() == 1 && cards.get(0).fields.isEmpty()))
            throw new UserServletException(HttpServletResponse.SC_BAD_REQUEST, "no contact fields found in vcard");
        for (VCard vcf : cards) {
            if (vcf.fields.isEmpty())
                continue;
            folder.getMailbox().createContact(context.opContext, vcf.asParsedContact(), folder.getId(), null);
        }
    }
}
