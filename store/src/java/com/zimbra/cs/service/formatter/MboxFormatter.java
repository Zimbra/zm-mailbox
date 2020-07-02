/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2020 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.formatter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.mail.MessagingException;
import javax.mail.Part;
import javax.servlet.ServletException;

import org.apache.http.HttpException;

import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.HttpUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.index.SortBy;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailItem.Type;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxMaintenance;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.service.UserServletContext;
import com.zimbra.cs.service.UserServletException;
import com.zimbra.cs.service.formatter.FormatterFactory.FormatType;

/**
 *
 * @author jyotiranjan.jena
 * This class is implemented for exporting user's email in .mbox file format
 */
public class MboxFormatter extends Formatter {

    private static final String NEW_MAIL_IDENTIFIER = "From ";
    private static final String STRING_TO_BE_REPLACED = ">From ";
    private static final String SPACE_SEPARATOR = " ";

    @Override
    public FormatType getType() {
        return FormatType.MBOX;
    }

    @Override
    public void formatCallback(UserServletContext context) throws UserServletException, ServiceException, IOException,
            ServletException, MessagingException, HttpException {

        int dot;
        String ext = "." + getType();
        String filename = context.params.get("filename");
        String lock = context.params.get("lock");
        MailboxMaintenance maintenance = null;
        PrintWriter writer = null;
        try {
            if (filename == null || filename.equals("")) {
                Date date = new Date();
                DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT);
                SimpleDateFormat sdf = new SimpleDateFormat(".H-m-s");

                if (context.hasPart()) {
                    filename = "attachments";
                } else {
                    filename = context.targetMailbox.getAccountId() + '.' + df.format(date).replace('/', '-') + sdf.format(date);
                }
            }
            else if ((dot = filename.lastIndexOf('.')) != -1) {
                String userProvidedExtension = filename.substring(dot + 1);
                if("mbox".equals(userProvidedExtension)) {
                    filename = filename.substring(0, dot);
                } else {
                    throw ServiceException.FAILURE(String.format("Unsupported file extension : %s. Only support .mbox for fmt=%s", userProvidedExtension, context.format), null);
                }
            }
            if (!filename.endsWith(ext)) {
                filename += ext;
            }

            context.resp.addHeader("Content-Disposition", HttpUtil.createContentDisposition(context.req, Part.ATTACHMENT, filename));
            context.resp.setContentType(MimeConstants.CT_MBOX);
            Charset charset = context.getCharset() != null ? context.getCharset() : Charset.defaultCharset();
            writer = new PrintWriter(new OutputStreamWriter(context.resp.getOutputStream(), charset));

            if (lock != null && (lock.equals("1") || lock.equals("t") || lock.equals("true"))) {
                maintenance = MailboxManager.getInstance().beginMaintenance(context.targetMailbox.getAccountId(), context.targetMailbox.getId());
            }

            if (context.requestedItems != null) {
                try {
                    for (UserServletContext.Item item : context.requestedItems)
                        writeMailItemToFile(item.mailItem, writer);
                } catch (Exception e) {
                    warn(e);
                }
            } else if (context.target != null && !(context.target instanceof Folder)) {
                try {
                    writeMailItemToFile(context.target, writer);
                } catch (Exception e) {
                    warn(e);
                }
            } else {
                // get the target mailbox from context and get list of MailItems for the folder
                Mailbox mbox = context.targetMailbox;
                Folder folder = (Folder) context.target;
                OperationContext octxt = new OperationContext(context.getAuthAccount(), context.isUsingAdminPrivileges());

                ZimbraLog.account.info("Getting mailitems for %s", folder.getName());
                List<MailItem> mailitems = mbox.getItemList(octxt, Type.MESSAGE, folder.getId(), SortBy.DATE_DESC);
                for(MailItem item : mailitems) {
                    writeMailItemToFile(item, writer);
                }
                // get the SubFolders and for each SubFolder get the MailItem and write to file
                String recursive = context.params.get("recursive");
                if (recursive != null && Integer.parseInt(recursive) == 1) {
                    ZimbraLog.account.info("Write subfolders mailitems recursively");
                    writeToFileForSubFolders(folder, octxt, mbox, writer);
                }
           }
        } finally {
            if (maintenance != null) {
                MailboxManager.getInstance().endMaintenance(maintenance, true, true);
            }
            if (writer != null) {
                try {
                    writer.close();
                } catch (Exception e) {
                    ZimbraLog.misc.debug(e);
                }
            }
        }
    }

    /**
     * This method writes each mime to mbox file
     * @param mailItem MailItem
     * @param writer writer
     * @throws ServiceException throws ServiceException
     */
    private void writeMailItemToFile(MailItem mailItem, PrintWriter writer) throws ServiceException {
        InputStream is = null;
        BufferedReader reader = null;
        try {
            // write the first line for each new MailItem
            writer.write(addStartingLineForNewMailItem(mailItem).concat(System.lineSeparator()));
            is = mailItem.getContentStream();
            reader = new BufferedReader(new InputStreamReader(is));
            StringBuffer out = new StringBuffer();
            String line;
            while ((line = reader.readLine()) != null) {
                // Check if line starts with `From `, then replace it with `>From `.
                if (line.startsWith(NEW_MAIL_IDENTIFIER)) {
                    out.append(STRING_TO_BE_REPLACED.concat(line.substring(5, line.length()))).append(System.lineSeparator());
                } else {
                    out.append(line).append(System.lineSeparator());
                }
            }
            writer.write(out.toString());
            writer.println();
            writer.flush();
        } catch (Exception e) {
            ZimbraLog.misc.debug(e.getMessage(), e.getCause());
            throw ServiceException.FAILURE("Could not write to file", null);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception e) {
                    ZimbraLog.misc.debug(e);
                }
            }
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ioe) {
                    ZimbraLog.misc.debug(ioe);
                }
            }
        }
    }

    /**
     *  This method returns a String which is the identifier line for each mime in a mbox file.
     * @param mailItem MailItem
     * @return String
     */
    private String addStartingLineForNewMailItem(MailItem mailItem) throws ServiceException {
        StringBuffer sb = new StringBuffer();
        sb.append(NEW_MAIL_IDENTIFIER);
        sb.append(mailItem.getSender());
        sb.append(SPACE_SEPARATOR);
        sb.append(new Date(mailItem.getDate()));
        sb.append(SPACE_SEPARATOR);
        sb.append(mailItem.getPath());
        return sb.toString();
    }

    /**
     * This method writes all subfolder's mime recursively to the .mbox file using PrintWriter
     * @param parent Parent Folder
     * @param octxt operation context
     * @param mbox Logged in user mailbox
     * @param writer Writer
     * @throws ServiceException throw ServiceException
     */
    private void writeToFileForSubFolders(Folder parent, OperationContext octxt, Mailbox mbox, PrintWriter writer) throws ServiceException {
        ZimbraLog.account.info("Subfolders are exist for %s, Fetching all subfolders", parent.getName());
        List<Folder> subFolders = parent.getSubfolders(octxt);
        for (Folder subFolder : subFolders) {
            List<MailItem> subFolderMailitems = mbox.getItemList(octxt, Type.MESSAGE, subFolder.getId(), SortBy.DATE_DESC);
            for (MailItem item : subFolderMailitems) {
                writeMailItemToFile(item, writer);
            }
            if (subFolder.hasSubfolders()) {
                writeToFileForSubFolders(subFolder, octxt, mbox, writer);
            }
        }
        ZimbraLog.account.info("Subfolders MailItems has been written successfully to MBOX file");
    }

    private void warn(Exception e) {
        if (e.getCause() == null) {
            ZimbraLog.misc.warn("Mbox Formatter warning: %s", e, e);
        } else {
            ZimbraLog.misc.warn("Mbox Formatter warning: %s: %s", e, e.getCause().toString(), e.getCause());
        }
    }
}
