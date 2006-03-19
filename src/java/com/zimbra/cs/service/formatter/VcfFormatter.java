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
import java.util.HashSet;
import java.util.Iterator;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.mail.Part;
import javax.mail.internet.ContentDisposition;
import javax.mail.internet.ParseException;
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

public class VcfFormatter extends Formatter {

    private Pattern ILLEGAL_CHARS = Pattern.compile("[\\/\\:\\*\\?\\\"\\<\\>\\|]");

    public String getType() {
        return "vcf";
    }

    public String getDefaultSearchTypes() {
        return MailboxIndex.SEARCH_FOR_CONTACTS;
    }
    
    public boolean canBeBlocked() {
        return false;
    }

    public void format(Context context, MailItem item) throws IOException, ServiceException {
        ContentDisposition cd;
        try {
            cd = new ContentDisposition(Part.ATTACHMENT);
        } catch (ParseException e) {
            throw ServiceException.FAILURE(e.getMessage(), e);
        }

        if (item instanceof Contact) {
            VCard vcf = VCard.formatContact((Contact) item);

            cd.setParameter("filename", getZipEntryName(vcf, null));
            context.resp.addHeader("Content-Disposition", cd.toString());
            context.resp.setContentType(Mime.CT_TEXT_VCARD);
            context.resp.setCharacterEncoding(Mime.P_CHARSET_UTF8);
            context.resp.getOutputStream().write(vcf.formatted.getBytes(Mime.P_CHARSET_UTF8));
            return;
        }

        // passed-in item is a folder or a tag or somesuch -- get the list of contacts
        Iterator iterator = getMailItems(context, item, getDefaultStartTime(), getDefaultEndTime());

        cd.setParameter("filename", "contacts.zip");
        context.resp.addHeader("Content-Disposition", cd.toString());
        context.resp.setContentType("application/x-zip-compressed");

        // create the ZIP file
        ZipOutputStream out = new ZipOutputStream(context.resp.getOutputStream());
        HashSet<String> usedNames = new HashSet<String>();

        while (iterator.hasNext()) {
            MailItem itItem = (MailItem) iterator.next();
            if (!(itItem instanceof Contact))
                continue;
            VCard vcf = VCard.formatContact((Contact) itItem);

            // add ZIP entry to output stream.
            out.putNextEntry(new ZipEntry(getZipEntryName(vcf, usedNames)));
            out.write(vcf.formatted.getBytes(Mime.P_CHARSET_UTF8));
            out.closeEntry();
        }
        // complete the ZIP file
        out.close();
    }

    private String getZipEntryName(VCard vcf, HashSet<String> used) {
        // TODO: more bullet proofing on path lengths and illegal chars
        String fn = vcf.fn, folder = (used == null ? "" : "contacts/"), path;
        if (fn.length() > 115)
            fn = fn.substring(0, 114);
        int counter = 0;
        do {
            path = folder + ILLEGAL_CHARS.matcher(fn).replaceAll("_");
            if (counter > 0)
                path += "-" + counter;
            counter++;
        } while (used != null && used.contains(path));
        return path + ".vcf";
    }

    public void save(byte[] body, Context context, Folder folder) throws ServiceException, IOException, UserServletException {
        VCard vcf = VCard.parseVCard(new String(body, "utf-8"));
        if (vcf.fields.isEmpty())
            throw new UserServletException(HttpServletResponse.SC_BAD_REQUEST, "no contact fields found in vcard");
        folder.getMailbox().createContact(context.opContext, vcf.fields, folder.getId(), null);
    }
}
