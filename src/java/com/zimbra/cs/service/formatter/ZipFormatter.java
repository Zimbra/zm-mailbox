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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Pattern;

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
import com.zimbra.common.util.zip.ZipEntry;
import com.zimbra.common.util.zip.ZipOutputStream;
import com.zimbra.common.util.zip.ZipShort;

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
            String filename = context.params.get("filename");
            
            if (filename == null || filename.equals("")) {
                filename = context.hasPart() ? "attachments.zip" : "items.zip";
            } else {
                Date date = new Date();
                DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT);
                SimpleDateFormat sdf = new SimpleDateFormat(".H-m-s");

                filename += '.' + df.format(date).replace('/', '-') +
                    sdf.format(date) + ".zip";
            }

            String cd = Part.ATTACHMENT + "; filename=" + HttpUtil.encodeFilename(context.req, filename);
            context.resp.addHeader("Content-Disposition", cd.toString());
            context.resp.setContentType("application/x-zip-compressed");

            iterator = getMailItems(context, getDefaultStartTime(), getDefaultEndTime(), 500);
            if (!iterator.hasNext())
            	return;

            // create the ZIP file
            out = new ZipOutputStream(context.resp.getOutputStream());
            out.setEncoding(Mime.P_CHARSET_UTF8);
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
                    	entry.setExtra(getXZimbraHeadersBytes(item));
                        out.putNextEntry(entry);
                        try {
                            InputStream is = ((Message) item).getContentStream();
                            if (!TarFormatter.shouldReturnBody(context)) {
                                is = new HeadersOnlyInputStream(is);
                            }
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
                    entry.setExtra(getXZimbraHeadersBytes(item));
                    out.putNextEntry(entry);
                    out.write(vcf.formatted.getBytes(Mime.P_CHARSET_UTF8));
                    out.closeEntry();
                } else if (item instanceof CalendarItem) {
                    // We aren't currently adding calendar items to the zip stream, but this block
                    // of code is added to highlight the need to hide private calendar items
                    // when/if we included calendar items to the zip later.

                    // Don't return private appointments/tasks if the requester is not the mailbox owner.
                    CalendarItem calItem = (CalendarItem) item;
                    if (calItem.isPublic() || calItem.allowPrivateAccess(context.authAccount, context.isUsingAdminPrivileges())) {
                        // do nothing for now
                    }
                } else if (item instanceof Document) {
                	String ext = "";
                	if (item.getType() == MailItem.TYPE_WIKI)
                		ext = ".wiki";
                	ZipEntry entry = new ZipEntry(getZipEntryName(item, item.getName(), ext, context, usedNames));
                	entry.setExtra(getXZimbraHeadersBytes(item));
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

    private static final byte[] ZIP_EXTRA_FIELD_HEADER_ID_X_ZIMBRA_HEADERS = { (byte) 0xFF, (byte) 0xFF };
    private static final byte[] BACKWARD_COMPAT_LINE = "x: y\r\n".getBytes();

    private static byte[] getXZimbraHeadersBytes(MailItem item) {
        byte[] extra = null;
        byte[] data = SyncFormatter.getXZimbraHeadersBytes(item);
        if (data != null && data.length > 0) {
            extra = new byte[4 + BACKWARD_COMPAT_LINE.length + data.length];

            // Zip Header ID = 0xFFFF
            extra[0] = ZIP_EXTRA_FIELD_HEADER_ID_X_ZIMBRA_HEADERS[0];
            extra[1] = ZIP_EXTRA_FIELD_HEADER_ID_X_ZIMBRA_HEADERS[1];

            // Data Size (in little endian)
            byte[] dataSize = ZipShort.getBytes(BACKWARD_COMPAT_LINE.length + data.length);
            extra[2] = dataSize[0];
            extra[3] = dataSize[1];

            // HACK: To keep ZCO happy...
            // Insert a dummy line so the Header ID and Data Size bytes aren't treated as an X- header name.
            System.arraycopy(BACKWARD_COMPAT_LINE, 0, extra, 4, BACKWARD_COMPAT_LINE.length);

            // Finally the actual data.
            System.arraycopy(data, 0, extra, 4 + BACKWARD_COMPAT_LINE.length, data.length);
        } else {
            extra = new byte[0];
        }
        return extra;
    }

    public static byte[] parseXZimbraHeadersBytes(byte[] extra) {
        // If it starts with 0xFFFF [len] it is the new-style data.  If it doesn't, it must be
        // old-style data from when we weren't doing zip extra field correctly.
        byte[] data = extra;
        if (extra != null && extra.length >= 4) {
            if (extra[0] == ZIP_EXTRA_FIELD_HEADER_ID_X_ZIMBRA_HEADERS[0] &&
                extra[1] == ZIP_EXTRA_FIELD_HEADER_ID_X_ZIMBRA_HEADERS[1]) {
                int len = ZipShort.getValue(extra, 2);
                if (len == extra.length - 4) {
                    data = new byte[len];
                    System.arraycopy(extra, 4, data, 0, len);
                }
            }
        }
        return data;
    }
}
