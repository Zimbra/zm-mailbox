/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.mail.Part;
import javax.servlet.http.HttpServletResponse;

import com.zimbra.cs.index.MailboxIndex;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.service.UserServlet;
import com.zimbra.cs.service.UserServletException;
import com.zimbra.cs.service.UserServlet.Context;
import com.zimbra.cs.service.mail.ImportContacts;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.HttpUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.mime.MimeConstants;

public class CsvFormatter extends Formatter {

    public String getType() {
        return "csv";
    }

    public String[] getDefaultMimeTypes() {
        return new String[] { "text/csv", "text/comma-separated-values", MimeConstants.CT_TEXT_PLAIN };
    }

    public String getDefaultSearchTypes() {
        return MailboxIndex.SEARCH_FOR_CONTACTS;
    }

    public void formatCallback(Context context) throws IOException, ServiceException {
        Iterator<? extends MailItem> iterator = null;
        StringBuffer sb = new StringBuffer();
        try {
            iterator = getMailItems(context, -1, -1, Integer.MAX_VALUE);
            String format = context.req.getParameter(UserServlet.QP_CSVFORMAT);
            String locale = context.req.getParameter(UserServlet.QP_CSVLOCALE);
            String separator = context.req.getParameter(UserServlet.QP_CSVSEPARATOR);
            Character sepChar = null;
            if ((separator != null) && (separator.length() > 0))
                    sepChar = separator.charAt(0);
            if (locale == null)
                locale = context.locale.toString();
            ContactCSV contactCSV = new ContactCSV();
            contactCSV.toCSV(format, locale, sepChar, iterator, sb);
        } catch (ContactCSV.ParseException e) {
            throw MailServiceException.UNABLE_TO_IMPORT_CONTACTS("could not generate CSV", e);
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
        context.resp.setCharacterEncoding(MimeConstants.P_CHARSET_UTF8);
        context.resp.setContentType("text/csv");
        context.resp.getWriter().print(sb.toString());
    }

    public boolean canBeBlocked() {
        return false;
    }

    public boolean supportsSave() {
        return true;
    }

    public void saveCallback(Context context, String contentType, Folder folder, String filename)
    throws UserServletException, ServiceException, IOException {
        InputStreamReader isr = new InputStreamReader(context.getRequestInputStream(), MimeConstants.P_CHARSET_UTF8);
        BufferedReader reader = new BufferedReader(isr);
        
        try {
            String format = context.params.get(UserServlet.QP_CSVFORMAT);
            String locale = context.req.getParameter(UserServlet.QP_CSVLOCALE);
            if (locale == null)
                locale = context.locale.toString();
            List<Map<String, String>> contacts = ContactCSV.getContacts(reader, format, locale);
            ItemId iidFolder = new ItemId(folder);
            
            ImportContacts.ImportCsvContacts(context.opContext, context.targetMailbox, iidFolder, contacts);
        } catch (ContactCSV.ParseException e) {
            ZimbraLog.misc.debug("ContactCSV - ParseException thrown", e);
            throw new UserServletException(HttpServletResponse.SC_BAD_REQUEST,
                    "Could not parse csv file - Reason : " + e.getMessage());
        } finally {
            reader.close();
        }
    }
}
