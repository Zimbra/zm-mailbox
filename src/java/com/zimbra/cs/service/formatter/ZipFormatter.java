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
import java.io.InputStream;
import java.util.Iterator;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.mail.Part;
import javax.mail.internet.ContentDisposition;
import javax.mail.internet.ParseException;


import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.UserServlet.Context;
import com.zimbra.cs.util.ByteUtil;

public class ZipFormatter extends Formatter {

    private Pattern ILLEGAL_CHARS = Pattern.compile("[\\/\\:\\*\\?\\\"\\<\\>\\|]");
    
    public void format(Context context, MailItem mailItem) throws IOException, ServiceException {
        
        Iterator iterator = getMailItems(context, mailItem, getDefaultStartTime(), getDefaultEndTime());
        
        context.resp.setContentType("application/x-zip-compressed");

        
        try {
            ContentDisposition cd =  new ContentDisposition(Part.ATTACHMENT);
            // TODO: get name from folder/search/query/etc
            String filename = "messages.zip";
            cd.setParameter("filename", filename);
            context.resp.addHeader("Content-Disposition", cd.toString());
        } catch (ParseException e) {
            throw ServiceException.FAILURE(e.getMessage(), e);
        }
        
        // Create the ZIP file
        ZipOutputStream out = new ZipOutputStream(context.resp.getOutputStream());

        while(iterator.hasNext()) {
            MailItem itItem = (MailItem) iterator.next();
            if (!(itItem instanceof Message)) continue;
            Message message = (Message) itItem;
            
            // Add ZIP entry to output stream.
            out.putNextEntry(new ZipEntry(getZipEntryName(message, context)));
        
            // Transfer bytes from the file to the ZIP file
            InputStream is = message.getRawMessage();
            ByteUtil.copy(is, out);
            is.close();
            // Complete the entry
            out.closeEntry();
        }
        // Complete the ZIP file
        out.close();
    }

    private String getZipEntryName(Message m, Context context) throws ServiceException {
        Folder f = context.targetMailbox.getFolderById(context.opContext, m.getFolderId());
        String folderPath = f.getPath();
        if (folderPath.startsWith("/")) {
            if (folderPath.length() == 1) folderPath = "";
            else folderPath = folderPath.substring(1);
        }

        // TODO: more bullet proofing on path lengths and illegal chars        
        String subject = m.getSubject();
        if (subject == null) subject = "";
        if (subject.length() > 128) subject = subject.substring(0, 127); 
        return folderPath + (folderPath.length() > 0 ? "/" : "") + ILLEGAL_CHARS.matcher(subject).replaceAll("_") + " "+ m.getId() + ".eml";
    }

    public String getType() {
        return "zip";
    }
}
