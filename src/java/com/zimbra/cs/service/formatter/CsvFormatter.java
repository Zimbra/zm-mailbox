/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2011 Zimbra, Inc.
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
import java.io.PushbackInputStream;
import java.nio.charset.Charset;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.mail.Part;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.io.nio.SelectChannelEndPoint;
import org.eclipse.jetty.server.AbstractHttpConnection;

import com.google.common.base.Charsets;
import com.ibm.icu.text.CharsetDetector;
import com.ibm.icu.text.CharsetMatch;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.HttpUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.service.UserServlet;
import com.zimbra.cs.service.UserServletContext;
import com.zimbra.cs.service.UserServletException;
import com.zimbra.cs.service.formatter.FormatterFactory.FormatType;
import com.zimbra.cs.service.mail.ImportContacts;
import com.zimbra.cs.service.util.ItemId;

public class CsvFormatter extends Formatter {

    @Override
    public FormatType getType() {
        return FormatType.CSV;
    }

    @Override
    public String[] getDefaultMimeTypes() {
        return new String[] { "text/csv", "text/comma-separated-values", MimeConstants.CT_TEXT_PLAIN };
    }

    @Override
    public Set<MailItem.Type> getDefaultSearchTypes() {
        return EnumSet.of(MailItem.Type.CONTACT);
    }

    @Override
    public void formatCallback(UserServletContext context) throws IOException, ServiceException {
        // Disable the jetty timeout
        disableJettyTimeout();

        Iterator<? extends MailItem> iterator = null;
        StringBuilder sb = new StringBuilder();
        try {
            iterator = getMailItems(context, -1, -1, Integer.MAX_VALUE);
            String format = context.req.getParameter(UserServlet.QP_CSVFORMAT);
            String locale = context.req.getParameter(UserServlet.QP_CSVLOCALE);
            String separator = context.req.getParameter(UserServlet.QP_CSVSEPARATOR);
            Character sepChar = null;
            if ((separator != null) && (separator.length() > 0))
                    sepChar = separator.charAt(0);
            if (locale == null) {
                locale = context.getLocale().toString();
            }
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
        if (filename.toLowerCase().endsWith(".csv") == false) {
            filename = filename + ".csv";
        }
        String cd = HttpUtil.createContentDisposition(context.req, Part.ATTACHMENT, filename);
        context.resp.addHeader("Content-Disposition", cd);
        context.resp.setCharacterEncoding(context.getCharset().name());
        context.resp.setContentType("text/csv");
        context.resp.getWriter().print(sb.toString());
    }

    @Override
    public boolean supportsSave() {
        return true;
    }
    
    private static final int READ_AHEAD_BUFFER_SIZE = 8192;

    @Override
    public void saveCallback(UserServletContext context, String contentType, Folder folder, String filename)
    throws UserServletException, ServiceException, IOException {
        // Disable the jetty timeout
        disableJettyTimeout();
        // Detect the charset of upload file.
        PushbackInputStream pis = new PushbackInputStream(context.getRequestInputStream(), READ_AHEAD_BUFFER_SIZE);
        byte[] buf = new byte[READ_AHEAD_BUFFER_SIZE];
        int bytesRead = pis.read(buf, 0, READ_AHEAD_BUFFER_SIZE);
        CharsetDetector detector = new CharsetDetector();
        detector.setText(buf);
        CharsetMatch match = detector.detect();
        String guess = match.getName();
        Charset charset;
        if (guess != null) {
            try {
                charset = Charset.forName(guess);
            } catch (IllegalArgumentException e) {
                charset = Charsets.UTF_8;
            }
        } else {
            charset = Charsets.UTF_8;
        }
        if (bytesRead > 0) {
            pis.unread(buf, 0, bytesRead);
        }
        InputStreamReader isr = new InputStreamReader(pis, charset);
        BufferedReader reader = new BufferedReader(isr);

        try {
            String format = context.params.get(UserServlet.QP_CSVFORMAT);
            String locale = context.req.getParameter(UserServlet.QP_CSVLOCALE);
            if (locale == null) {
                locale = context.getLocale().toString();
            }
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

    /**
     * Implemented for bug 77682..
     *
     * Disable the Jetty timeout for the SelectChannelConnector and the SSLSelectChannelConnector
     * for this request.
     *
     * By default (and our normal configuration) Jetty has a 30 second idle timeout (10 if the server is busy) for
     * connection endpoints. There's another task that keeps track of what connections have timeouts and periodically
     * works over a queue and closes endpoints that have been timed out. This plays havoc with downloads to slow connections
     * and whenever we have a long pause while working to create an archive.
     *
     * This method instructs Jetty not to close the connection when the idle time is reached. Given that we don't send a content-length
     * down to the browser for archive responses, we have to close the socket to tell the browser its done. Since we have to do that..
     * leaving this endpoint without a timeout is safe. If the connection was being reused (ie keep-alive) this could have issues, but its not
     * in this case.
     * @throws IOException
     */
    private void disableJettyTimeout() throws IOException {
        if (LC.zimbra_csv_formatter_disable_timeout.booleanValue()) {
            EndPoint endPoint = AbstractHttpConnection.getCurrentConnection().getEndPoint();
            if (endPoint instanceof SelectChannelEndPoint) {
                SelectChannelEndPoint scEndPoint = (SelectChannelEndPoint) endPoint;
                scEndPoint.setMaxIdleTime(0);
            }
        }
    }
}
