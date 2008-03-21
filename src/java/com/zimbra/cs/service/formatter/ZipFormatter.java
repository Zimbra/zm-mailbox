/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007 Zimbra, Inc.
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
import java.io.InputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.mail.MessagingException;
import javax.mail.Part;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimePart;

import com.zimbra.cs.index.MailboxIndex;
import com.zimbra.cs.mailbox.CalendarItem;
import com.zimbra.cs.mailbox.Contact;
import com.zimbra.cs.mailbox.Document;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.service.UserServlet;
import com.zimbra.cs.service.UserServlet.Context;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.HttpUtil;

public class ZipFormatter extends Formatter {
    
    private Pattern ILLEGAL_CHARS = Pattern.compile("[\\/\\:\\*\\?\\\"\\<\\>\\|]");

    @Override
    public String getType() {
        return "zip";
    }

    @Override
    public String[] getDefaultMimeTypes() {
        return new String[] { "application/x-zip-compressed" };
    }

    @Override
    public String getDefaultSearchTypes() {
        return MailboxIndex.SEARCH_FOR_MESSAGES + ',' + MailboxIndex.SEARCH_FOR_CONTACTS;
    }

    @Override
    public boolean canBeBlocked() {
        return true;
    }

    @Override
    public void formatCallback(Context context) throws IOException, ServiceException {
        Iterator<? extends MailItem> iterator = null;
        ZipOutputStream out = null;
        try {
            iterator = getMailItems(context, getDefaultStartTime(), getDefaultEndTime(), 500);

            // TODO: get name from folder/search/query/etc
            String filename = context.hasPart() ? "attachments.zip" : "items.zip";
            String cd = Part.ATTACHMENT + "; filename=" + HttpUtil.encodeFilename(context.req, filename);
            context.resp.addHeader("Content-Disposition", cd.toString());
            context.resp.setContentType("application/x-zip-compressed");

            if (!iterator.hasNext())
            	return;

            // create the ZIP file
            out = new ZipOutputStream(context.resp.getOutputStream());
            String zlv = context.params.get(UserServlet.QP_ZLV);
            if (zlv != null && zlv.length() > 0) {
            	try {
            		int level = Integer.parseInt(zlv);
            		if (level >= 0 && level <=9) {
            			out.setLevel(level);
            		}
            	} catch (NumberFormatException x) {}
            }
            
            Set<String> usedNames = new HashSet<String>();

            while (iterator.hasNext()) {
                MailItem item = iterator.next();
                if (item instanceof Message) {
                    if (!context.hasPart()) {
                        // add ZIP entry to output stream
                    	ZipEntry entry = new ZipEntry(getZipEntryName(item, item.getSubject(), ".eml", context, usedNames));
                    	entry.setExtra(SyncFormatter.getXZimbraHeadersBytes(item));
                        out.putNextEntry(entry);
                        try {
                            InputStream is = ((Message) item).getContentStream();
                            ByteUtil.copy(is, true, out, false);
                        } finally {
                            out.closeEntry();
                        }
                    } else {
                        MimeMessage mm = ((Message) item).getMimeMessage();
                        for (String part : context.getPart().split(","))
                            addPartToZip(mm, part, out, context, usedNames);
                    }
                } else if (item instanceof Contact) {
                    VCard vcf = VCard.formatContact((Contact) item);

                    // add ZIP entry to output stream
                    ZipEntry entry = new ZipEntry(getZipEntryName(item, vcf.fn, ".vcf", context, usedNames));
                    entry.setExtra(SyncFormatter.getXZimbraHeadersBytes(item));
                    out.putNextEntry(entry);
                    out.write(vcf.formatted.getBytes(Mime.P_CHARSET_UTF8));
                    out.closeEntry();
                } else if (item instanceof CalendarItem) {
                    // We aren't currently adding calendar items to the zip stream, but this block
                    // of code is added to highlight the need to hide private calendar items
                    // when/if we included calendar items to the zip later.

                    // Don't return private appointments/tasks if the requester is not the mailbox owner.
                    CalendarItem calItem = (CalendarItem) context.target;
                    if (calItem.isPublic() || calItem.allowPrivateAccess(context.authAccount)) {
                        // do nothing for now
                    }
                } else if (item instanceof Document) {
                	String ext = "";
                	if (item.getType() == MailItem.TYPE_WIKI)
                		ext = ".wiki";
                	ZipEntry entry = new ZipEntry(getZipEntryName(item, item.getName(), ext, context, usedNames));
                    entry.setExtra(SyncFormatter.getXZimbraHeadersBytes(item));
                    out.putNextEntry(entry);
                    ByteUtil.copy(item.getContentStream(), true, out, false);
                }
            }
        } finally {
            if (iterator instanceof QueryResultIterator)
                ((QueryResultIterator) iterator).finished();
            // complete the ZIP file
            if (out != null)
                out.close();
        }
    }

    private String getZipEntryName(MailItem item, String title, String suffix, Context context, Set<String> used)
    throws ServiceException {
        String path = "";
        if (item != null) {
            Folder folder = item.getMailbox().getFolderById(context.opContext, item.getFolderId());
            path = folder.getPath();
            if (path.startsWith("/"))
                path = path.substring(1);
            path = ILLEGAL_CHARS.matcher(path).replaceAll("_") + (path.equals("") ? "" : "/");
        }

        // TODO: more bullet proofing on path lengths and illegal chars        
        if (title == null)
            title = "";
        if (title.length() > 115)
            title = title.substring(0, 114);
        title = ILLEGAL_CHARS.matcher(title).replaceAll("_").trim();

        String filename;
        int counter = 0;
        do {
            filename = path + title;
            if (counter > 0)
                filename += "-" + counter;
            filename += suffix;
            counter++;
        } while (used != null && used.contains(filename.toLowerCase()));
        used.add(filename.toLowerCase());
        return filename;
    }

    private void addPartToZip(MimeMessage mm, String part, ZipOutputStream out, Context context, Set<String> usedNames) throws ServiceException, IOException {
        try {
            MimePart mp = Mime.getMimePart(mm, part);
            if (mp == null)
                throw MailServiceException.NO_SUCH_PART(part);
            String extension = "", partname = Mime.getFilename(mp);
            if (partname == null) {
                partname = "attachment";
            } else {
                int dot = partname.lastIndexOf('.');
                if (dot != -1 && dot < partname.length() - 1) {
                    extension = partname.substring(dot);
                    partname = partname.substring(0, dot);
                }
            }

            // add ZIP entry to output stream
            out.putNextEntry(new ZipEntry(getZipEntryName(null, partname, extension, context, usedNames)));
            try {
                ByteUtil.copy(mp.getInputStream(), true, out, false);
            } finally {
                out.closeEntry();
            }
        } catch (MessagingException e) {
            throw MailServiceException.MESSAGE_PARSE_ERROR(e);
        }
    }
}
