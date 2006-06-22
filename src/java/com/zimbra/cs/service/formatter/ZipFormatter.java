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
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s):
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.formatter;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.mail.Part;
import javax.servlet.http.HttpServletResponse;

import com.zimbra.cs.index.MailboxIndex;
import com.zimbra.cs.mailbox.Contact;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.operation.Operation;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.UserServletException;
import com.zimbra.cs.service.UserServlet.Context;
import com.zimbra.cs.util.ByteUtil;
import com.zimbra.cs.util.HttpUtil;

public class ZipFormatter extends Formatter {
    
    public static class Format {};
    public static class Save {};
    static int sFormatLoad = Operation.setLoad(ZipFormatter.Format.class, 10);
    static int sSaveLoad = Operation.setLoad(ZipFormatter.Save.class, 10);
    int getFormatLoad() { return  sFormatLoad; }
    int getSaveLoad() { return sSaveLoad; }

    private Pattern ILLEGAL_CHARS = Pattern.compile("[\\/\\:\\*\\?\\\"\\<\\>\\|]");

    public String getType() {
        return "zip";
    }

    public String[] getDefaultMimeTypes() {
        return new String[] { "application/x-zip-compressed" };
    }

    public String getDefaultSearchTypes() {
        return MailboxIndex.SEARCH_FOR_MESSAGES + ',' + MailboxIndex.SEARCH_FOR_CONTACTS;
    }

    public boolean canBeBlocked() {
        return true;
    }

    public void formatCallback(Context context, MailItem target) throws IOException, ServiceException {
        Iterator<? extends MailItem> iterator = null;
        ZipOutputStream out = null;
        try {
            iterator = getMailItems(context, target, getDefaultStartTime(), getDefaultEndTime());

            // TODO: get name from folder/search/query/etc
            String filename = "items.zip";
            String cd = Part.ATTACHMENT + "; filename=" + HttpUtil.encodeFilename(context.req, filename);
            context.resp.addHeader("Content-Disposition", cd.toString());
            context.resp.setContentType("application/x-zip-compressed");
    
            // create the ZIP file
            out = new ZipOutputStream(context.resp.getOutputStream());
            HashSet<String> usedNames = new HashSet<String>();
    
            while (iterator.hasNext()) {
                MailItem item = iterator.next();
                if (item instanceof Message) {
                    InputStream is = ((Message) item).getRawMessage();
        
                    // add ZIP entry to output stream
                    out.putNextEntry(new ZipEntry(getZipEntryName(item, item.getSubject(), ".eml", context, usedNames)));
                    try {
                        ByteUtil.copy(is, true, out, false);
                    } finally {
                        out.closeEntry();
                    }
                } else if (item instanceof Contact) {
                    VCard vcf = VCard.formatContact((Contact) item);
    
                    // add ZIP entry to output stream
                    out.putNextEntry(new ZipEntry(getZipEntryName(item, vcf.fn, ".vcf", context, usedNames)));
                    out.write(vcf.formatted.getBytes(Mime.P_CHARSET_UTF8));
                    out.closeEntry();
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

    private String getZipEntryName(MailItem item, String title, String suffix, Context context, HashSet<String> used)
    throws ServiceException {
        Folder folder = item.getMailbox().getFolderById(context.opContext, item.getFolderId());
        String filename, path = folder.getPath();
        if (path.startsWith("/"))
            path = path.substring(1);
        path = ILLEGAL_CHARS.matcher(path).replaceAll("_") + (path.equals("") ? "" : "/");

        // TODO: more bullet proofing on path lengths and illegal chars        
        if (title == null)
            title = "";
        if (title.length() > 115)
            title = title.substring(0, 114);
        title = ILLEGAL_CHARS.matcher(title).replaceAll("_").trim();

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

    // FIXME: should add each item to the specified folder...
    public void saveCallback(byte[] body, Context context, Folder folder) throws UserServletException {
        throw new UserServletException(HttpServletResponse.SC_BAD_REQUEST, "format not supported for save");
    }
}
