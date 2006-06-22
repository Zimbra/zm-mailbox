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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.Map;

import javax.mail.Part;
import javax.servlet.http.HttpServletResponse;

import com.zimbra.cs.index.MailboxIndex;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.operation.Operation;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.UserServletException;
import com.zimbra.cs.service.UserServlet.Context;
import com.zimbra.cs.util.HttpUtil;

public class CsvFormatter extends Formatter {

    public static class Format {};
    public static class Save {};
    static int sFormatLoad = Operation.setLoad(CsvFormatter.Format.class, 10);
    static int sSaveLoad = Operation.setLoad(CsvFormatter.Save.class, 10);
    int getFormatLoad() { return  sFormatLoad; }
    int getSaveLoad() { return sSaveLoad; }
    
    public String getType() {
        return "csv";
    }

    public String[] getDefaultMimeTypes() {
        return new String[] { "text/csv", "text/comma-separated-values", Mime.CT_TEXT_PLAIN };
    }

    public String getDefaultSearchTypes() {
        return MailboxIndex.SEARCH_FOR_CONTACTS;
    }

    public void formatCallback(Context context, MailItem item) throws IOException, ServiceException {
        Iterator<? extends MailItem> iterator = null;
        StringBuffer sb = new StringBuffer();
        try {
            iterator = getMailItems(context, item, -1, -1);
            ContactCSV.toCSV(iterator, sb);
        } finally {
            if (iterator instanceof QueryResultIterator)
                ((QueryResultIterator) iterator).finished();
        }

        // todo: get from folder name
        String filename = context.itemPath;
        if (filename == null || filename.length() == 0)
            filename = "contacts";
        String cd = Part.ATTACHMENT + "; filename=" + HttpUtil.encodeFilename(context.req, filename + ".csv");
        context.resp.addHeader("Content-Disposition", cd);
        context.resp.setCharacterEncoding(Mime.P_CHARSET_UTF8);
        context.resp.setContentType("text/csv");
        context.resp.getOutputStream().print(sb.toString());
    }

    public boolean canBeBlocked() {
        return false;
    }

    public void saveCallback(byte[] body, Context context, Folder folder) throws UserServletException, ServiceException {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(body), "UTF-8"));
            for (Map<String, String> fields : ContactCSV.getContacts(reader))
                folder.getMailbox().createContact(context.opContext, fields, folder.getId(), null);
        } catch (ContactCSV.ParseException e) {
            throw new UserServletException(HttpServletResponse.SC_BAD_REQUEST, "could not parse csv file");
        } catch (UnsupportedEncodingException uee) {
            throw new UserServletException(HttpServletResponse.SC_BAD_REQUEST, "could not parse csv file");
        }
    }
}
