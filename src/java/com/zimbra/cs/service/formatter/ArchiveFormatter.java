/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2011 Zimbra, Inc.
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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.mail.Part;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimePart;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.io.nio.SelectChannelEndPoint;
import org.eclipse.jetty.server.AbstractHttpConnection;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.io.Closeables;
import com.zimbra.common.calendar.ZCalendar.ZCalendarBuilder;
import com.zimbra.common.calendar.ZCalendar.ZICalendarParseHandler;
import com.zimbra.common.calendar.ZCalendar.ZVCalendar;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.mailbox.ContactConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.BufferStream;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.HttpUtil;
import com.zimbra.common.util.HttpUtil.Browser;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.index.SortBy;
import com.zimbra.cs.index.ZimbraQueryResults;
import com.zimbra.cs.mailbox.ACL;
import com.zimbra.cs.mailbox.Appointment;
import com.zimbra.cs.mailbox.CalendarItem;
import com.zimbra.cs.mailbox.CalendarItem.Instance;
import com.zimbra.cs.mailbox.Chat;
import com.zimbra.cs.mailbox.Contact;
import com.zimbra.cs.mailbox.Conversation;
import com.zimbra.cs.mailbox.DeliveryOptions;
import com.zimbra.cs.mailbox.Document;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.MailServiceException.NoSuchItemException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.SetCalendarItemData;
import com.zimbra.cs.mailbox.MailboxMaintenance;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.Message.CalendarItemInfo;
import com.zimbra.cs.mailbox.Mountpoint;
import com.zimbra.cs.mailbox.Note;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mailbox.SearchFolder;
import com.zimbra.cs.mailbox.Tag;
import com.zimbra.cs.mailbox.Task;
import com.zimbra.cs.mailbox.WikiItem;
import com.zimbra.cs.mailbox.calendar.IcsImportParseHandler;
import com.zimbra.cs.mailbox.calendar.IcsImportParseHandler.ImportInviteVisitor;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.mailbox.util.TagUtil;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.mime.ParsedContact;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.service.UserServlet;
import com.zimbra.cs.service.UserServletContext;
import com.zimbra.cs.service.UserServletException;
import com.zimbra.cs.service.mail.ImportContacts;
import com.zimbra.cs.service.util.ItemData;
import com.zimbra.cs.service.util.ItemId;

public abstract class ArchiveFormatter extends Formatter {
    private Pattern ILLEGAL_FILE_CHARS = Pattern.compile("[\\/\\:\\*\\?\\\"\\<\\>\\|\\\0]");
    private Pattern ILLEGAL_FOLDER_CHARS = Pattern.compile("[\\:\\*\\?\\\"\\<\\>\\|\\\0]");
    private static String UTF8 = "UTF-8";
    public static enum Resolve { Modify, Replace, Reset, Skip }
    public static final String PARAM_RESOLVE = "resolve";

    private static final class SortPath implements Comparator<MailItem> {
        SortPath() {
        }

        @Override
        public int compare(MailItem m1, MailItem m2) {
            try {
                int ret = m1.getPath().compareTo(m2.getPath());
                return ret == 0 ? m1.getName().compareTo(m2.getName()) : ret;
            } catch (Exception e) {
                return 0;
            }
        }
    }

    public abstract interface ArchiveInputEntry {
        public long getModTime();
        public String getName();
        public long getSize();
        public int getType();
        public boolean isUnread();
    }

    public abstract interface ArchiveOutputEntry {
        public void setUnread();
        public void setSize(long size);
    }

    public abstract interface ArchiveInputStream {
        public void close() throws IOException;
        public InputStream getInputStream();
        public ArchiveInputEntry getNextEntry() throws IOException;
        public int read(byte[] buf, int offset, int len) throws IOException;
    }

    public abstract interface ArchiveOutputStream {
        public void close() throws IOException;
        public void closeEntry() throws IOException;
        public OutputStream getOutputStream();
        public int getRecordSize();
        public ArchiveOutputEntry newOutputEntry(String path, String name,
            int type, long date);
        public void putNextEntry(ArchiveOutputEntry entry) throws IOException;
        public void write(byte[] buf) throws IOException;
        public void write(byte[] buf, int offset, int len) throws IOException;
    }

    @Override
    public Set<MailItem.Type> getDefaultSearchTypes() {
        return SEARCH_FOR_EVERYTHING;
    }

    @Override public boolean supportsSave() { return true; }

    protected boolean getDefaultMeta() { return true; }

    protected abstract ArchiveInputStream getInputStream(UserServletContext context,
        String charset) throws IOException, ServiceException, UserServletException;

    protected abstract ArchiveOutputStream getOutputStream(UserServletContext context,
        String charset) throws IOException;

    @Override
    public void formatCallback(UserServletContext context) throws IOException,
            ServiceException, UserServletException {
        // Disable the jetty timeout
        disableJettyTimeout();

        HashMap<Integer, Integer> cnts = new HashMap<Integer, Integer>();
        int dot;
        HashMap<Integer, String> fldrs = new HashMap<Integer, String>();
        String emptyname = context.params.get("emptyname");
        String ext = "." + getType();
        String filename = context.params.get("filename");
        String lock = context.params.get("lock");
        String query = context.getQueryString();
        Set<String> names = new HashSet<String>(4096);
        Set<MailItem.Type> sysTypes = EnumSet.of(MailItem.Type.FOLDER, MailItem.Type.SEARCHFOLDER, MailItem.Type.TAG,
                MailItem.Type.FLAG, MailItem.Type.MOUNTPOINT);
        Set<MailItem.Type> searchTypes = EnumSet.of(MailItem.Type.MESSAGE, MailItem.Type.CONTACT,
                MailItem.Type.DOCUMENT, MailItem.Type.WIKI, MailItem.Type.APPOINTMENT, MailItem.Type.TASK,
                MailItem.Type.CHAT, MailItem.Type.NOTE);
        ArchiveOutputStream aos = null;
        String types = context.getTypesString();
        MailboxMaintenance maintenance = null;
        try {
            if (filename == null || filename.equals("")) {
                Date date = new Date();
                DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT);
                SimpleDateFormat sdf = new SimpleDateFormat(".H-m-s");

                if (context.hasPart())
                    filename = "attachments";
                else
                    filename = context.targetMailbox.getAccountId() + '.' +
                        df.format(date).replace('/', '-') + sdf.format(date);
            } else if ((dot = filename.lastIndexOf('.')) != -1) {
                ext = filename.substring(dot);
            }
            if (!filename.endsWith(ext))
                filename += ext;
            if (emptyname != null && !emptyname.equals("") &&
                !emptyname.endsWith(ext))
                emptyname += ext;
            context.resp.addHeader("Content-Disposition", Part.ATTACHMENT +
                "; filename=" + HttpUtil.encodeFilename(context.req, filename));
            if (ext.equals(".tar"))
                context.resp.setContentType("application/x-tar");
            else if (ext.equals(".tgz"))
                context.resp.setContentType("application/x-compressed-tar");
            else if (ext.equals(".zip"))
                context.resp.setContentType("application/zip");
            if (!Strings.isNullOrEmpty(types)) {
                try {
                    searchTypes = MailItem.Type.setOf(types);
                } catch (IllegalArgumentException e) {
                    throw MailServiceException.INVALID_TYPE(e.getMessage());
                }
                sysTypes.clear();
                // do not include conversations even when requested
                // (to be compatible with 7.1.4 and later. bug 67407)
                searchTypes.remove(MailItem.Type.CONVERSATION);
            }
            if (lock != null && (lock.equals("1") || lock.equals("t") || lock.equals("true"))) {
                maintenance = MailboxManager.getInstance().beginMaintenance(
                        context.targetMailbox.getAccountId(), context.targetMailbox.getId());
            }
            Charset charset = context.getCharset();
            CharsetEncoder encoder = charset.newEncoder();
            if (context.requestedItems != null) {
                try {
                    for (UserServletContext.Item item : context.requestedItems)
                        aos = saveItem(context, item.mailItem, fldrs, cnts, item.versioned,
                                aos, encoder, names);
                } catch (Exception e) {
                    warn(e);
                }
            } else if (context.target != null && !(context.target instanceof
                Folder)) {
                try {
                    aos = saveItem(context, context.target, fldrs, cnts,
                            false, aos, encoder, names);
                } catch (Exception e) {
                    warn(e);
                }
            } else {
                ZimbraQueryResults results = null;
                boolean saveTargetFolder = false;

                if (context.target instanceof Folder) {
                    Folder f = (Folder)context.target;

                    if (f.getId() != Mailbox.ID_FOLDER_USER_ROOT) {
                        saveTargetFolder = true;
                        query = "under:\"" + f.getPath() + "\"" +
                            (query == null ? "" : " " + query);
                    }
                }
                if (query == null || query.equals("")) {
                    SortPath sp = new SortPath();

                    for (MailItem.Type type : sysTypes) {
                        List<MailItem> items = context.targetMailbox.getItemList(context.opContext, type);

                        Collections.sort(items, sp);
                        for (MailItem item : items)
                            aos = saveItem(context, item, fldrs, cnts,
                                    false, aos, encoder, names);
                    }
                    query = "is:local";
                }
                results = context.targetMailbox.index.search(context.opContext,
                    query, searchTypes, SortBy.NONE,
                    LC.zimbra_archive_formatter_search_chunk_size.intValue());
                try {
                    while (results.hasNext()) {
                        if (saveTargetFolder) {
                            saveTargetFolder = false;
                            aos = saveItem(context, context.target,
                                    fldrs, cnts, false, aos, encoder, names);
                        }
                        aos = saveItem(context, results.getNext().getMailItem(),
                                fldrs, cnts, false, aos, encoder, names);
                    }
                    Closeables.closeQuietly(results);
                    results = null;
                } catch (Exception e) {
                    warn(e);
                } finally {
                    Closeables.closeQuietly(results);
                }
            }
            if (aos == null) {
                if (emptyname == null) {
                    context.resp.setHeader("Content-Disposition", null);
                    throw new UserServletException(HttpServletResponse.SC_NO_CONTENT, "No data found");
                }
                context.resp.setHeader("Content-Disposition", Part.ATTACHMENT +
                        "; filename=" + HttpUtil.encodeFilename(context.req, emptyname));
                aos = getOutputStream(context, UTF8);
            }
        } finally {
            if (maintenance != null) {
                MailboxManager.getInstance().endMaintenance(maintenance, true, true);
            }
            if (aos != null) {
                try {
                    aos.close();
                } catch (Exception e) {
                }
            }
        }
    }


    /**
     * Implemented for bug 56458..
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
        if (LC.zimbra_archive_formatter_disable_timeout.booleanValue()) {
            EndPoint endPoint = AbstractHttpConnection.getCurrentConnection().getEndPoint();
            if (endPoint instanceof SelectChannelEndPoint) {
                SelectChannelEndPoint scEndPoint = (SelectChannelEndPoint) endPoint;
                scEndPoint.setMaxIdleTime(0);
            }
        }
    }

    private ArchiveOutputStream saveItem(UserServletContext context, MailItem mi,
        Map<Integer, String> fldrs, Map<Integer, Integer> cnts,
        boolean version, ArchiveOutputStream aos,
        CharsetEncoder charsetEncoder, Set<String> names) throws ServiceException {

        String ext = null, name = null;
        String extra = null;
        Integer fid = mi.getFolderId();
        String fldr;
        InputStream is = null;
        String metaParam = context.params.get(UserServlet.QP_META);
        boolean meta = metaParam == null ? getDefaultMeta() : !metaParam.equals("0");

        if (!version && mi.isTagged(Flag.FlagInfo.VERSIONED)) {
            for (MailItem rev : context.targetMailbox.getAllRevisions(
                context.opContext, mi.getId(), mi.getType())) {
                if (mi.getVersion() != rev.getVersion())
                    aos = saveItem(context, rev, fldrs, cnts, true,
                            aos, charsetEncoder, names);
            }
        }
        switch (mi.getType()) {
            case APPOINTMENT:
                Appointment appt = (Appointment)mi;

                if (!appt.isPublic() && !appt.allowPrivateAccess(
                        context.getAuthAccount(), context.isUsingAdminPrivileges())) {
                    return aos;
                }
                if (meta) {
                    name = appt.getSubject();
                    ext = "appt";
                } else {
                    ext = "ics";
                }
                break;
            case CHAT:
                ext = "chat";
                break;
            case CONTACT:
                Contact ct = (Contact)mi;

                name = ct.getFileAsString();
                if (!meta)
                    ext = "vcf";
                break;
            case FLAG:
                return aos;
            case FOLDER:
            case MOUNTPOINT:
            case SEARCHFOLDER:
                if (mi.getId() == Mailbox.ID_FOLDER_ROOT) {
                    name = "ROOT";
                } else if (mi.getId() == Mailbox.ID_FOLDER_USER_ROOT) {
                    name = "USER_ROOT";
                } else {
                    name = mi.getName();
                }
                break;
            case MESSAGE:
                Message msg = (Message)mi;

                if (msg.hasCalendarItemInfos()) {
                    Set<Integer> calItems = new HashSet<Integer>();

                    for (Iterator<CalendarItemInfo> it = msg.getCalendarItemInfoIterator();
                        it.hasNext(); )
                        calItems.add(it.next().getCalendarItemId());
                    for (Integer i : calItems) {
                        if (extra == null)
                            extra = "calendar=" + i.toString();
                        else
                            extra += ',' + i.toString();
                    }
                }
                ext = "eml";
                break;
            case NOTE:
                ext = "note";
                break;
            case TASK:
                Task task = (Task) mi;
                if (!task.isPublic() && !task.allowPrivateAccess(
                        context.getAuthAccount(), context.isUsingAdminPrivileges())) {
                    return aos;
                }
                ext = "task";
                break;
            case VIRTUAL_CONVERSATION:
                return aos;
            case WIKI:
                ext = "wiki";
                break;
        }
        fldr = fldrs.get(fid);
        if (fldr == null) {
            Folder f = mi.getMailbox().getFolderById(context.opContext, fid);

            cnts.put(fid, 1);
            fldr = f.getPath();
            if (fldr.startsWith("/")) {
                fldr = fldr.substring(1);
            }
            fldr = sanitize(fldr, charsetEncoder);
            fldr = ILLEGAL_FOLDER_CHARS.matcher(fldr).replaceAll("_");
            fldrs.put(fid, fldr);
        } else if (!(mi instanceof Folder)) {
            final int BATCH = 500;
            int cnt = cnts.get(fid) + 1;

            cnts.put(fid, cnt);
            cnt /= BATCH;
            if (cnt > 0)
                fldr = fldr + '!' + cnt;
        }
        int targetBaseLength = 0;
        if (context.noHierarchy()) {
            // Parent hierarchy is not needed, so construct the folder names without parent hierarchy.
            // e.g> represent "inbox/subfolder/target" as "target".
            String targetPath = null;
            if (context.itemPath.endsWith("/")) // inbox/subfolder/target/
                targetPath = context.itemPath.substring(0, context.itemPath.lastIndexOf("/"));
            else // inbox/subfolder/target
                targetPath = context.itemPath;
            targetBaseLength = targetPath.lastIndexOf('/'); // "inbox/subfolder".length()
            if (targetBaseLength >= fldr.length()) // fldr is "inbox/subfolder"
                fldr = "";
            else if (targetBaseLength > 0) { // fldr is "inbox/subfolder/target"
                fldr = fldr.substring(targetBaseLength+1);
            }
        }
        try {
            ArchiveOutputEntry aoe;
            byte data[] = null;
            String path = mi instanceof Contact ? getEntryName(mi, fldr, name, ext,
                charsetEncoder, names) : getEntryName(mi, fldr, name, ext,
                charsetEncoder, !(mi instanceof Document));
            long miSize = mi.getSize();

            if (miSize == 0 && mi.getDigest() != null) {
                ZimbraLog.misc.debug("blob db size 0 for item %d", mi.getId());
                return aos;
            }
            try {
                is = mi.getContentStream();
            } catch (Exception e) {
                ZimbraLog.misc.error("missing blob for item %d: expected %d",
                    mi.getId(), miSize);
                return aos;
            }
            if (aos == null) {
                aos = getOutputStream(context, charsetEncoder.charset().name());
            }
            
            if ((mi instanceof CalendarItem) &&
                    (context.getStartTime() != TIME_UNSPECIFIED || context.getEndTime() != TIME_UNSPECIFIED)) {
                Collection<Instance> instances = ((CalendarItem)mi).expandInstances(
                        context.getStartTime(), context.getEndTime(), false);
                if (instances.isEmpty())
                    return aos;
            }
            
            aoe = aos.newOutputEntry(path + ".meta", mi.getType().toString(), mi.getType().toByte(), mi.getDate());
            if (mi instanceof Message && (mi.getFlagBitmask() & Flag.ID_UNREAD) != 0) {
                aoe.setUnread();
            }
            if (meta) {
                ItemData itemData = new ItemData(mi, extra);
                if (context.noHierarchy()) {
                    // Parent hierarchy is not needed, so change the path in the metadata to start from target.
                    // itemData.path is of the form /Inbox/subfolder/target and after this step it becomes /target.
                    if (targetBaseLength > 0 && ((targetBaseLength+1) < itemData.path.length())) {
                        itemData.path = itemData.path.substring(targetBaseLength+1);
                    }
                }
                byte[] metaData = itemData.encode();

                aoe.setSize(metaData.length);
                aos.putNextEntry(aoe);
                aos.write(metaData);
                aos.closeEntry();
            } else if (mi instanceof CalendarItem) {
                Browser browser = HttpUtil.guessBrowser(context.req);
                List<CalendarItem> calItems = new ArrayList<CalendarItem>();
                boolean needAppleICalHacks = Browser.APPLE_ICAL.equals(browser);
                boolean useOutlookCompatMode = Browser.IE.equals(browser);
                OperationContext octxt = new OperationContext(
                        context.getAuthAccount(), context.isUsingAdminPrivileges());
                StringWriter writer = new StringWriter();
                calItems.add((CalendarItem)mi);
                context.targetMailbox.writeICalendarForCalendarItems(
                        writer, octxt, calItems, useOutlookCompatMode, true,
                        needAppleICalHacks, true);
                data = writer.toString().getBytes(charsetEncoder.charset());
            } else if (mi instanceof Contact) {
                VCard vcf = VCard.formatContact((Contact)mi);

                data = vcf.formatted.getBytes(charsetEncoder.charset());
            } else if (mi instanceof Message) {
                if (context.hasPart()) {
                    MimeMessage mm = ((Message)mi).getMimeMessage();
                    Set<String> attachmentNames = new HashSet<String>();
                    for (String part : context.getPart().split(",")) {
                        BufferStream bs;
                        MimePart mp = Mime.getMimePart(mm, part);
                        long sz;

                        if (mp == null)
                            throw MailServiceException.NO_SUCH_PART(part);
                        name = Mime.getFilename(mp);
                        ext = null;
                        sz = mp.getSize();
                        if (sz == -1)
                            sz = miSize;
                        if (name == null) {
                            name = "attachment";
                        } else {
                            int dot = name.lastIndexOf('.');

                            if (dot != -1 && dot < name.length() - 1) {
                                ext = name.substring(dot + 1);
                                name = name.substring(0, dot);
                            }
                        }
                        bs = new BufferStream(sz, 1024 * 1024);
                        InputStream stream = mp.getInputStream();
                        try {
                            bs.readFrom(stream);
                        } finally {
                            // close the stream, it could be an instance of PipedInputStream.
                            ByteUtil.closeStream(stream);
                        }
                        aoe = aos.newOutputEntry(
                                getEntryName(mi, "", name, ext, charsetEncoder, attachmentNames),
                                mi.getType().toString(), mi.getType().toByte(), mi.getDate());
                        sz = bs.getSize();
                        aoe.setSize(sz);
                        aos.putNextEntry(aoe);
                        bs.copyTo(aos.getOutputStream());
                        bs.close();
                        aos.closeEntry();
                    }
                    return aos;
                }
            }
            aoe = aos.newOutputEntry(path, mi.getType().toString(), mi.getType().toByte(), mi.getDate());
            if (data != null) {
                aoe.setSize(data.length);
                aos.putNextEntry(aoe);
                aos.write(data);
                aos.closeEntry();
            } else if (is != null) {
                if (context.shouldReturnBody()) {
                    byte buf[] = new byte[aos.getRecordSize() * 20];
                    int in;
                    long remain = miSize;

                    aoe.setSize(miSize);
                    aos.putNextEntry(aoe);
                    while (remain > 0 && (in = is.read(buf)) >= 0) {
                        aos.write(buf, 0, remain < in ? (int)remain : in);
                        remain -= in;
                    }
                    if (remain != 0) {
                        ZimbraLog.misc.error("mismatched blob size for item %d: expected %d",
                             mi.getId(), miSize);
                        if (remain > 0) {
                            Arrays.fill(buf, (byte)' ');
                            while (remain > 0) {
                                aos.write(buf, 0, remain < buf.length ?
                                    (int)remain : buf.length);
                                remain -= buf.length;
                            }
                        }
                        aos.closeEntry();
                        aoe = aos.newOutputEntry(path + ".err", mi.getType().toString(), mi.getType().toByte(),
                                mi.getDate());
                        aoe.setSize(0);
                        aos.putNextEntry(aoe);
                    }
                } else {
                    // Read headers into memory to compute size
                    byte headerData[] = HeadersOnlyInputStream.getHeaders(is);

                    aoe.setSize(headerData.length);
                    aos.putNextEntry(aoe);
                    aos.write(headerData);
                }
                aos.closeEntry();
            }
        } catch (Exception e) {
            throw ServiceException.FAILURE("archive error", e);
        } finally {
            ByteUtil.closeStream(is);
        }
        return aos;
    }

    /**
     * Get entry name using set of previously created names to guarantee uniqueness
     */
    private String getEntryName(MailItem mi, String fldr, String name,
        String ext, CharsetEncoder encoder, Set<String> names) {
        String path = getEntryName(mi, fldr, name, ext, encoder, false);
        int counter = 0;
        String lpath;
        do {
            path = fldr.equals("") ? name : fldr + '/' + name;
            if (counter > 0)
                path += String.format("-%02d", counter);
            if (ext != null)
                path += '.' + ext;
            counter++;
            lpath = path.toLowerCase();
        } while (names.contains(lpath));
        names.add(lpath);
        return path;
    }

    /**
     * Get entry name. If prefix is true guarantee uniqueness by prepending itemId. If prefix is false caller must guarantee uniqueness
     */
    private String getEntryName(MailItem mi, String fldr, String name,
            String ext, CharsetEncoder encoder, boolean prefix) {
        String path;

        if (Strings.isNullOrEmpty(name)) {
            name = mi.getName();
        }
        if (Strings.isNullOrEmpty(name)) {
            name = mi.getSubject();
        }
        if (prefix && !Strings.isNullOrEmpty(name)) {
            name = Strings.padStart(mi.getId()+"", 10, '0') + "-" +
                sanitize(name, encoder);
        }
        if (Strings.isNullOrEmpty(name)) {
            name = mi.getType().toString() + '-' + mi.getId();
        } else if (name.length() > 121) {
            name = name.substring(0, 120);
        }
        if (mi.isTagged(Flag.FlagInfo.VERSIONED)) {
            // prepend the version before the extension of up to four characters
            int dot = name.lastIndexOf('.');
            if (dot >= 0 && dot >= name.length() - 5) {
                name = name.substring(0, dot) + String.format("-%05d", mi.getVersion()) + name.substring(dot);
            } else {
                name += String.format("-%05d", mi.getVersion());
            }
        }
        name = ILLEGAL_FILE_CHARS.matcher(name).replaceAll("_").trim();
        while (name.endsWith(".")) {
            name = name.substring(0, name.length() - 1).trim();
        }
        path = fldr.equals("") ? name : fldr + '/' + name;
        if (ext != null) {
            path += '.' + ext;
        }
        return path;
    }

    @Override
    public void saveCallback(UserServletContext context, String contentType, Folder fldr,
            String file) throws IOException, ServiceException {
        Exception ex = null;
        ItemData id = null;
        Map<String, Integer> digestMap = new HashMap<String, Integer>();
        List<ServiceException> errs = new LinkedList<ServiceException>();
        List<Folder> flist;
        Map<Object, Folder> fmap = new HashMap<Object, Folder>();
        Map<Integer, Integer> idMap = new HashMap<Integer, Integer>();
        long last = System.currentTimeMillis();
        String types = context.getTypesString();
        String resolve = context.params.get(PARAM_RESOLVE);
        String subfolder = context.params.get("subfolder");
        String timestamp = context.params.get("timestamp");
        String timeout = context.params.get("timeout");

        try {
            ArchiveInputStream ais;
            int ids[] = null;
            long interval = 45 * 1000;
            Resolve r = resolve == null ? Resolve.Skip : Resolve.valueOf(
                resolve.substring(0,1).toUpperCase() +
                resolve.substring(1).toLowerCase());
            if (timeout != null)
                interval = Long.parseLong(timeout);
            Set<MailItem.Type> searchTypes = null;

            if (context.reqListIds != null) {
                ids = context.reqListIds.clone();
                Arrays.sort(ids);
            }
            if (!Strings.isNullOrEmpty(types)) {
                try {
                    searchTypes = MailItem.Type.setOf(types);
                } catch (IllegalArgumentException e) {
                    throw MailServiceException.INVALID_TYPE(e.getMessage());
                }
                searchTypes.remove(MailItem.Type.CONVERSATION);
            }
            Charset charset = context.getCharset();
            try {
                ais = getInputStream(context, charset.name());
            } catch (Exception e) {
                String filename = context.params.get(UserServlet.UPLOAD_NAME);

                throw FormatterServiceException.INVALID_FORMAT(filename == null ?
                    "unknown" : filename);
            }
            if (!Strings.isNullOrEmpty(subfolder)) {
                fldr = createPath(context, fmap, fldr.getPath() + subfolder, Folder.Type.UNKNOWN);
            }
            flist = fldr.getSubfolderHierarchy();
            if (r == Resolve.Reset) {
                for (Folder f : flist) {
                    if (context.targetMailbox.isImmutableSystemFolder(f.getId())) {
                        continue;
                    }
                    try {
                        List<Integer> delIds;

                        if (System.currentTimeMillis() - last > interval) {
                            updateClient(context, true);
                            last = System.currentTimeMillis();
                        }
                        if (searchTypes == null) {
                            delIds = context.targetMailbox.listItemIds(
                                    context.opContext, MailItem.Type.UNKNOWN, f.getId());
                        } else {
                            delIds = context.targetMailbox.getItemIds(context.opContext, f.getId()).getIds(searchTypes);
                        }
                        if (delIds == null)
                            continue;

                        int delIdsArray[] = new int[delIds.size()];
                        int i = 0;

                        for (Integer del : delIds)
                            if (del >= Mailbox.FIRST_USER_ID)
                                delIdsArray[i++] = del;
                        while (i < delIds.size())
                            delIdsArray[i++] = Mailbox.ID_AUTO_INCREMENT;
                        context.targetMailbox.delete(context.opContext, delIdsArray, MailItem.Type.UNKNOWN, null);
                    } catch (MailServiceException e) {
                        if (e.getCode() != MailServiceException.NO_SUCH_FOLDER) {
                            r = Resolve.Replace;
                            addError(errs, e);
                        }
                    } catch (Exception e) {
                        r = Resolve.Replace;
                        addError(errs, FormatterServiceException.UNKNOWN_ERROR(
                            f.getName(), e));
                    }
                }
                context.targetMailbox.purge(MailItem.Type.UNKNOWN);
                flist = fldr.getSubfolderHierarchy();
            }
            for (Folder f : flist) {
                fmap.put(f.getId(), f);
                fmap.put(f.getPath(), f);
            }
            try {
                ArchiveInputEntry aie;
                Boolean meta = false;

                while ((aie = ais.getNextEntry()) != null) {
                    if (System.currentTimeMillis() - last > interval) {
                        updateClient(context, true);
                        last = System.currentTimeMillis();
                    }
                    if (aie.getName().startsWith("__MACOSX/")) {
                        continue;
                    } else if (aie.getName().endsWith(".meta")) {
                        meta = true;
                        if (id != null) {
                            addItem(context, fldr, fmap, digestMap, idMap, ids, searchTypes, r, id, ais, null, errs);
                        }
                        try {
                            id = new ItemData(readArchiveEntry(ais, aie));
                        } catch (Exception e) {
                            addError(errs, FormatterServiceException.INVALID_FORMAT(
                                aie.getName()));
                        }
                        continue;
                    } else if (aie.getName().endsWith(".err")) {
                        addError(errs, FormatterServiceException.MISMATCHED_SIZE(
                            aie.getName()));
                    } else if (id == null) {
                        if (meta) {
                            addError(errs, FormatterServiceException.MISSING_META(aie.getName()));
                        } else {
                            addData(context, fldr, fmap, searchTypes, r, timestamp == null || !timestamp.equals("0"),
                                    ais, aie, errs);
                        }
                    } else if ((aie.getType() != 0 && id.ud.type != aie.getType()) ||
                        (id.ud.getBlobDigest() != null && aie.getSize() != -1 &&
                        id.ud.size != aie.getSize())) {
                        addError(errs, FormatterServiceException.MISMATCHED_META(aie.getName()));
                    } else {
                        addItem(context, fldr, fmap, digestMap, idMap, ids, searchTypes, r, id, ais, aie, errs);
                    }
                    id = null;
                }
                if (id != null) {
                    addItem(context, fldr, fmap, digestMap, idMap, ids, searchTypes, r, id, ais, null, errs);
                }
            } catch (Exception e) {
                if (id == null)
                    addError(errs, FormatterServiceException.UNKNOWN_ERROR(e));
                else
                    addError(errs, FormatterServiceException.UNKNOWN_ERROR(id.path, e));
                id = null;
            } finally {
                if (ais != null)
                    ais.close();
            }
        } catch (Exception e) {
            ex = e;
        }
        try {
            updateClient(context, ex, errs);
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw ServiceException.FAILURE("Archive formatter failure", e);
        }
    }

    private void addError(List<ServiceException> errs, ServiceException ex) {
        StringBuilder s = new StringBuilder(ex.getLocalizedMessage() == null ?
            ex.toString() : ex.getLocalizedMessage());

        errs.add(ex);
        if (!ex.getArgs().isEmpty()) {
            s.append(':');
            for (ServiceException.Argument arg : ex.getArgs())
                s.append(' ').append(arg.name).append('=').append(arg.value);
        }
        ZimbraLog.misc.warn(s, ex);
    }

    private Folder createParent(UserServletContext context, Map<Object, Folder> fmap, String path, MailItem.Type view)
    throws ServiceException {
        String parent = path.substring(0, path.lastIndexOf('/'));
        if (parent.equals("")) {
            parent = "/";
        }
        return createPath(context, fmap, parent, view);
    }

    private Folder createPath(UserServletContext context, Map<Object, Folder> fmap, String path, MailItem.Type view)
    throws ServiceException {
        Folder fldr;

        if ((fldr = fmap.get(path)) == null) {
            try {
                fldr = context.targetMailbox.getFolderByPath(context.opContext, path);
            } catch (Exception e) {
                fldr = context.targetMailbox.createFolder(context.opContext, path, (byte)0, view);
            }
            fmap.put(fldr.getId(), fldr);
            fmap.put(fldr.getPath(), fldr);
        }
        if (view != Folder.Type.UNKNOWN && fldr.getDefaultView() != Folder.Type.UNKNOWN &&
                fldr.getDefaultView() != view && !((view == MailItem.Type.DOCUMENT || view == MailItem.Type.WIKI) &&
                        (fldr.getDefaultView() == Folder.Type.DOCUMENT || fldr.getDefaultView() == Folder.Type.WIKI))) {
            throw FormatterServiceException.INVALID_TYPE(view.toString(), path);
        }
        return fldr;
    }

    private static final String[] NO_TAGS = new String[0];

    @VisibleForTesting
    static String[] getTagNames(OperationContext octxt, Mailbox mbox, ItemData id)
    throws IOException {
        if (Strings.isNullOrEmpty(id.tags)) {
            return NO_TAGS;
        }

        try {
            String serialized = id.tags;

            // pre 6.0.6 versions had numeric tags strings, not names
            if (Character.isDigit(serialized.charAt(0)) && serialized.indexOf(':') == -1) {
                return TagUtil.tagIdStringToNames(mbox, octxt, serialized);
            } else {
                return ItemData.getTagNames(serialized);
            }
        } catch (Exception e) {
            throw new IOException("tag error: " + e);
        }
    }

    private static byte[] readArchiveEntry(ArchiveInputStream ais, ArchiveInputEntry aie)
    throws IOException {
        if (aie == null)
            return null;

        int dsz = (int) aie.getSize();
        byte[] data;

        if (dsz == 0) {
            return null;
        } else if (dsz == -1) {
            data = ByteUtil.getContent(ais.getInputStream(), -1, false);
        } else {
            data = new byte[dsz];
            if (ais.read(data, 0, dsz) != dsz) {
                throw new IOException("archive read err");
            }
        }
        return data;
    }

    private String string(String s) { return s == null ? new String() : s; }

    private void warn(Exception e) {
        if (e.getCause() == null) {
            ZimbraLog.misc.warn("Archive Formatter warning: %s", e, e);
        } else {
            ZimbraLog.misc.warn("Archive Formatter warning: %s: %s", e,
                e.getCause().toString(), e.getCause());
        }
    }

    private void addItem(UserServletContext context, Folder fldr, Map<Object, Folder> fmap, Map<String, Integer> digestMap,
            Map<Integer, Integer> idMap, int[] ids, Set<MailItem.Type> types, Resolve r, ItemData id,
            ArchiveInputStream ais, ArchiveInputEntry aie, List<ServiceException> errs)
    throws ServiceException {
        try {
            Mailbox mbox = fldr.getMailbox();
            MailItem mi = MailItem.constructItem(mbox, id.ud);
            MailItem newItem = null, oldItem = null;
            OperationContext oc = context.opContext;
            String path;
            ParsedMessage pm;
            boolean root = fldr.getId() == Mailbox.ID_FOLDER_ROOT ||
                fldr.getId() == Mailbox.ID_FOLDER_USER_ROOT ||
                id.path.startsWith(fldr.getPath() + '/');

            if ((ids != null && Arrays.binarySearch(ids, id.ud.id) < 0) ||
                    (types != null && !types.contains(MailItem.Type.of(id.ud.type)))) {
                return;
            }
            if (id.ud.getBlobDigest() != null && aie == null) {
                addError(errs, FormatterServiceException.MISSING_BLOB(id.path));
                return;
            }
            if (root) {
                path = id.path;
            } else {
                path = fldr.getPath() + id.path;
            }
            if (path.endsWith("/") && !path.equals("/")) {
                path = path.substring(0, path.length() - 1);
            }
            if (mbox.isImmutableSystemFolder(id.ud.folderId)) {
                return;
            }
            switch (mi.getType()) {
            case APPOINTMENT:
            case TASK:
                CalendarItem ci = (CalendarItem)mi;

                fldr = createPath(context, fmap, path,
                        ci.getType() == MailItem.Type.APPOINTMENT ? MailItem.Type.APPOINTMENT : MailItem.Type.TASK);
                if (!root || r != Resolve.Reset) {
                    CalendarItem oldCI = null;

                    try {
                        oldCI = mbox.getCalendarItemByUid(oc, ci.getUid());
                    } catch (Exception e) {
                    }
                    if (oldCI != null && r == Resolve.Replace)
                        mbox.delete(oc, oldCI.getId(), oldCI.getType());
                    else
                        oldItem = oldCI;
                }
                if (oldItem == null || r != Resolve.Skip) {
                    CalendarItem.AlarmData ad = ci.getAlarmData();
                    byte[] data = readArchiveEntry(ais, aie);
                    Map<Integer, MimeMessage> blobMimeMsgMap = data == null ?
                        null : CalendarItem.decomposeBlob(data);
                    SetCalendarItemData defScid = new SetCalendarItemData();
                    SetCalendarItemData exceptionScids[] = null;
                    Invite invs[] = ci.getInvites();
                    MimeMessage mm;

                    if (invs != null && invs.length > 0) {
                        defScid.invite = invs[0];
                        if (blobMimeMsgMap != null &&
                                (mm = blobMimeMsgMap.get(defScid.invite.getMailItemId())) != null) {
                            defScid.message = new ParsedMessage(mm, mbox.attachmentsIndexingEnabled());
                        }
                        if (invs.length > 1) {
                            exceptionScids = new SetCalendarItemData[invs.length - 1];
                            for (int i = 1; i < invs.length; i++) {
                                SetCalendarItemData scid = new SetCalendarItemData();

                                scid.invite = invs[i];
                                if (blobMimeMsgMap != null &&
                                        (mm = blobMimeMsgMap.get(defScid.invite.getMailItemId())) != null) {
                                    scid.message = new ParsedMessage(mm, mbox.attachmentsIndexingEnabled());
                                }
                                exceptionScids[i - 1] = scid;
                            }
                        }
                        newItem = mbox.setCalendarItem(oc, oldItem != null &&
                            r == Resolve.Modify ? oldItem.getFolderId() :
                            fldr.getId(), ci.getFlagBitmask(),
                            ci.getTags(), defScid, exceptionScids,
                            ci.getAllReplies(), ad == null ?
                            CalendarItem.NEXT_ALARM_KEEP_CURRENT : ad.getNextAt());
                    }
                }
                break;
            case CHAT:
                Chat chat = (Chat)mi;
                byte[] content = readArchiveEntry(ais, aie);
                pm = new ParsedMessage(content, mi.getDate(), mbox.attachmentsIndexingEnabled());
                fldr = createPath(context, fmap, path, MailItem.Type.CHAT);
                if (root && r != Resolve.Reset) {
                    Chat oldChat = null;

                    try {
                        oldChat = mbox.getChatById(oc, chat.getId());
                        if (oldChat.getFolderId() != fldr.getId())
                            oldChat = null;
                    } catch (Exception e) {
                    }
                    if (oldChat != null &&
                        chat.getSender().equals(oldChat.getSender()) &&
                        chat.getSubject().equals(oldChat.getSubject())) {
                        if (r == Resolve.Replace) {
                            mbox.delete(oc, oldChat.getId(),
                                oldChat.getType());
                        } else {
                            oldItem = oldChat;
                            if (r == Resolve.Modify)
                                newItem = mbox.updateChat(oc, pm,
                                    oldItem.getId());
                        }
                    }
                }
                if (oldItem == null)
                    newItem = mbox.createChat(oc, pm, fldr.getId(),
                        chat.getFlagBitmask(), chat.getTags());
                break;
            case CONVERSATION:
                Conversation cv = (Conversation) mi;

                if (r != Resolve.Reset && r != Resolve.Skip) {
                    try {
                        oldItem = mbox.getConversationByHash(oc, Mailbox.getHash(
                            cv.getSubject()));
                    } catch (Exception e) {
                    }
                }
                break;
            case CONTACT:
                Contact ct = (Contact) mi;

                fldr = createPath(context, fmap, path, Folder.Type.CONTACT);
                if (root && r != Resolve.Reset) {
                    Contact oldContact = null;

                    try {
                        oldContact = mbox.getContactById(oc, ct.getId());
                        if (oldContact.getFolderId() != fldr.getId())
                            oldContact = null;
                    } catch (Exception e) {
                    }

                    if (oldContact != null) {
                        String email = string(ct.get(ContactConstants.A_email));
                        String first = string(ct.get(ContactConstants.A_firstName));
                        String name = string(ct.get(ContactConstants.A_fullName));
                        String oldemail = string(oldContact.get(ContactConstants.A_email));
                        String oldfirst = string(oldContact.get(ContactConstants.A_firstName));
                        String oldname = string(oldContact.get(ContactConstants.A_fullName));

                        if (email.equals(oldemail) && first.equals(oldfirst) &&
                            name.equals(oldname)) {
                            if (r == Resolve.Replace) {
                                mbox.delete(oc, oldContact.getId(),
                                    oldContact.getType());
                            } else {
                                oldItem = oldContact;
                                if (r == Resolve.Modify)
                                    mbox.modifyContact(oc, oldItem.getId(),
                                        new ParsedContact(ct.getFields(),
                                        readArchiveEntry(ais, aie)));
                            }
                        }
                    }
                }
                if (oldItem == null)
                    newItem = mbox.createContact(oc, new ParsedContact(
                        ct.getFields(), readArchiveEntry(ais, aie)), fldr.getId(),
                        ct.getTags());
                break;
            case DOCUMENT:
            case WIKI:
                Document doc = (Document)mi;
                Document oldDoc = null;
                Integer oldId = idMap.get(mi.getId());

                fldr = createParent(context, fmap, path,
                        doc.getType() ==  MailItem.Type.DOCUMENT ? MailItem.Type.DOCUMENT : MailItem.Type.WIKI);
                if (oldId == null) {
                    try {
                        for (Document listDoc : mbox.getDocumentList(oc,
                            fldr.getId())) {
                            if (doc.getName().equals(listDoc.getName())) {
                                oldDoc = listDoc;
                                idMap.put(doc.getId(), oldDoc.getId());
                                break;
                            }
                        }
                    } catch (Exception e) {
                    }
                } else {
                    oldDoc = mbox.getDocumentById(oc, oldId);
                }
                if (oldDoc != null) {
                    if (r == Resolve.Replace && oldId == null) {
                        mbox.delete(oc, oldDoc.getId(), oldDoc.getType());
                    } else if (doc.getVersion() < oldDoc.getVersion()) {
                        return;
                    } else {
                        oldItem = oldDoc;
                        if (doc.getVersion() > oldDoc.getVersion())
                            newItem = mbox.addDocumentRevision(oc,
                                oldDoc.getId(), doc.getCreator(),
                                doc.getName(), doc.getDescription(), doc.isDescriptionEnabled(),
                                ais.getInputStream());
                        if (r != Resolve.Skip)
                            mbox.setDate(oc, oldDoc.getId(), doc.getType(),
                                doc.getDate());
                    }
                }
                if (oldItem == null) {
                    if (mi.getType() == MailItem.Type.DOCUMENT) {
                        newItem = mbox.createDocument(oc, fldr.getId(),
                            doc.getName(), doc.getContentType(),
                            doc.getCreator(), doc.getDescription(), ais.getInputStream());
                    } else {
                        WikiItem wi = (WikiItem)mi;

                        newItem = mbox.createWiki(oc, fldr.getId(),
                            wi.getWikiWord(), wi.getCreator(), wi.getDescription(),
                            ais.getInputStream());
                    }
                    mbox.setDate(oc, newItem.getId(), doc.getType(),
                        doc.getDate());
                    idMap.put(doc.getId(), newItem.getId());
                }
                break;
            case FLAG:
                return;
            case FOLDER:
                String aclParam = context.params.get("acl");
                boolean doACL = aclParam == null || !aclParam.equals("0");
                Folder f = (Folder)mi;
                ACL acl = f.getACL();
                Folder oldF = null;
                MailItem.Type view = f.getDefaultView();
                if (view == MailItem.Type.CONVERSATION || view == MailItem.Type.FLAG || view == MailItem.Type.TAG) {
                    break;
                }
                try {
                    oldF = mbox.getFolderByPath(oc, path);
                } catch (Exception e) {
                }
                if (oldF != null) {
                    oldItem = oldF;
                    if (r != Resolve.Skip) {
                        if (!f.getUrl().equals(oldF.getUrl()))
                            mbox.setFolderUrl(oc, oldF.getId(), f.getUrl());
                        if (doACL) {
                            ACL oldACL = oldF.getACL();

                            if ((acl == null && oldACL != null) ||
                                (acl != null && (oldACL == null || !acl.equals(oldACL))))
                                mbox.setPermissions(oc, oldF.getId(), acl);
                        }
                    }
                }
                if (oldItem == null) {
                    fldr = createParent(context, fmap, path, Folder.Type.UNKNOWN);
                    newItem = fldr = mbox.createFolder(oc, f.getName(),
                        fldr.getId(), (byte)0, f.getDefaultView(),
                        f.getFlagBitmask(), f.getColor(), f.getUrl());
                    if (doACL && acl != null)
                        mbox.setPermissions(oc, fldr.getId(), acl);
                    fmap.put(fldr.getId(), fldr);
                    fmap.put(fldr.getPath(), fldr);
                }
                break;
            case MESSAGE:
                Message msg = (Message)mi;
                Message oldMsg = null;

                fldr = createPath(context, fmap, path, Folder.Type.MESSAGE);
                if (root && r != Resolve.Reset) {
                    try {
                        oldMsg = mbox.getMessageById(oc, msg.getId());
                        if (!msg.getDigest().equals(oldMsg.getDigest()) ||
                            oldMsg.getFolderId() != fldr.getId())
                            oldMsg = null;
                    } catch (Exception e) {
                    }
                }
                if (oldMsg == null) {
                    Integer digestId = digestMap.get(path);

                    if (digestId == null) {
                        digestMap.clear();
                        digestMap.put(path, -1);
                        try {
                            for (MailItem item : mbox.getItemList(oc, MailItem.Type.MESSAGE, fldr.getId())) {
                                digestMap.put(item.getDigest(), item.getId());
                            }
                        } catch (Exception e) {
                        }
                    }
                    digestId = digestMap.get(mi.getDigest());
                    if (digestId != null) {
                        oldMsg = mbox.getMessageById(oc, digestId);
                        if (!msg.getDigest().equals(oldMsg.getDigest()))
                            oldMsg = null;
                    }
                }
                if (oldMsg != null) {
                    if (r == Resolve.Replace)
                        mbox.delete(oc, oldMsg.getId(), oldMsg.getType());
                    else
                        oldItem = oldMsg;
                }
                if (oldItem == null) {
                    DeliveryOptions opt = new DeliveryOptions().
                        setFolderId(fldr.getId()).setNoICal(true).
                        setFlags(msg.getFlagBitmask()).
                        setTags(msg.getTags());
                    newItem = mbox.addMessage(oc, ais.getInputStream(),
                        (int)aie.getSize(), msg.getDate(), opt, null);
                }
                break;
            case MOUNTPOINT:
                Mountpoint mp = (Mountpoint)mi;
                MailItem oldMP = null;

                try {
                    oldMP = mbox.getItemByPath(oc, path);
                    if (oldMP.getType() == mi.getType())
                        oldMP = null;
                } catch (Exception e) {
                }
                if (oldMP != null) {
                    if (r == Resolve.Modify || r == Resolve.Replace)
                        mbox.delete(oc, oldMP.getId(), oldMP.getType());
                    else
                        oldItem = oldMP;
                }
                if (oldItem == null) {
                    fldr = createParent(context, fmap, path, Folder.Type.UNKNOWN);
                    newItem = mbox.createMountpoint(context.opContext,
                        fldr.getId(), mp.getName(), mp.getOwnerId(),
                        mp.getRemoteId(), mp.getRemoteUuid(), mp.getDefaultView(),
                        mp.getFlagBitmask(), mp.getColor(), mp.isReminderEnabled());
                }
                break;
            case NOTE:
                Note note = (Note)mi;
                Note oldNote = null;

                fldr = createPath(context, fmap, path, MailItem.Type.NOTE);
                try {
                    for (Note listNote : mbox.getNoteList(oc, fldr.getId())) {
                        if (note.getSubject().equals(listNote.getSubject())) {
                            oldNote = listNote;
                            break;
                        }
                    }
                } catch (Exception e) {
                }
                if (oldNote != null) {
                    if (r == Resolve.Replace) {
                        mbox.delete(oc, oldNote.getId(), oldNote.getType());
                    } else {
                        oldItem = oldNote;
                        if (r == Resolve.Modify)
                            mbox.editNote(oc, oldItem.getId(), new
                                String(readArchiveEntry(ais, aie), UTF8));
                    }
                    break;
                }
                if (oldItem == null) {
                    newItem = mbox.createNote(oc, new String(readArchiveEntry(
                        ais, aie), UTF8), note.getBounds(), note.getColor(),
                        fldr.getId());
                }
                break;
            case SEARCHFOLDER:
                SearchFolder sf = (SearchFolder)mi;
                MailItem oldSF = null;

                try {
                    oldSF = mbox.getItemByPath(oc, path);
                    if (oldSF.getType() == mi.getType())
                        oldSF = null;
                } catch (Exception e) {
                }
                if (oldSF != null) {
                    if (r == Resolve.Modify)
                        mbox.modifySearchFolder(oc, oldSF.getId(),
                            sf.getQuery(), sf.getReturnTypes(),
                            sf.getSortField());
                    else if (r == Resolve.Replace)
                        mbox.delete(oc, oldSF.getId(), oldSF.getType());
                    else
                        oldItem = oldSF;
                }
                if (oldItem == null) {
                    fldr = createParent(context, fmap, path, MailItem.Type.UNKNOWN);
                    newItem = mbox.createSearchFolder(oc, fldr.getId(), sf.getName(), sf.getQuery(),
                            sf.getReturnTypes(), sf.getSortField(), sf.getFlagBitmask(), sf.getColor());
                }
                break;
            case TAG:
                Tag tag = (Tag)mi;

                try {
                    Tag oldTag = mbox.getTagByName(tag.getName());

                    oldItem = oldTag;
                } catch (Exception e) {
                }
                if (oldItem == null)
                    newItem = mbox.createTag(oc, tag.getName(), tag.getColor());
                break;
            case VIRTUAL_CONVERSATION:
                return;
            }
            if (newItem != null) {
                if (mi.getColor() != newItem.getColor()) {
                    mbox.setColor(oc, newItem.getId(), newItem.getType(), mi.getColor());
                }
                if (!id.flags.equals(newItem.getFlagString()) || !id.tagsEqual(newItem)) {
                    mbox.setTags(oc, newItem.getId(), newItem.getType(), Flag.toBitmask(id.flags),
                            getTagNames(oc, mbox, id), null);
                }
            } else if (oldItem != null && r == Resolve.Modify) {
                if (mi.getColor() != oldItem.getColor()) {
                    mbox.setColor(oc, oldItem.getId(), oldItem.getType(), mi.getColor());
                }
                if (!id.flags.equals(oldItem.getFlagString()) || !id.tagsEqual(oldItem)) {
                    mbox.setTags(oc, oldItem.getId(), oldItem.getType(), Flag.toBitmask(id.flags),
                            getTagNames(oc, mbox, id), null);
                }
            }
        } catch (MailServiceException e) {
            if (e.getCode() == MailServiceException.QUOTA_EXCEEDED) {
                throw e;
            } else if (r != Resolve.Skip ||
                e.getCode() != MailServiceException.ALREADY_EXISTS) {
                addError(errs, e);
            }
        } catch (Exception e) {
            addError(errs, FormatterServiceException.UNKNOWN_ERROR(id.path, e));
        }
    }

    private void addData(UserServletContext context, Folder fldr, Map<Object, Folder> fmap, Set<MailItem.Type> types, Resolve r,
            boolean timestamp, ArchiveInputStream ais, ArchiveInputEntry aie, List<ServiceException> errs)
            throws ServiceException {
        try {
            int defaultFldr;
            Mailbox mbox = fldr.getMailbox();
            String dir, file;
            String name = aie.getName();
            int idx = name.lastIndexOf('/');
            MailItem newItem = null, oldItem;
            OperationContext oc = context.opContext;
            BufferedReader reader;
            MailItem.Type type, view;

            if (idx == -1) {
                file = name;
                dir = "";
            } else {
                file = name.substring(idx + 1);
                dir = name.substring(0, idx + 1);
                if (!dir.startsWith("/"))
                    dir = '/' + dir;
            }
            if (file.length() == 0) {
                return;
            } else if (file.endsWith(".csv") || file.endsWith(".vcf")) {
                defaultFldr = Mailbox.ID_FOLDER_CONTACTS;
                type = MailItem.Type.CONTACT;
                view = MailItem.Type.CONTACT;
            } else if (file.endsWith(".eml")) {
                defaultFldr = Mailbox.ID_FOLDER_INBOX;
                type = MailItem.Type.MESSAGE;
                view = MailItem.Type.MESSAGE;
            } else if (file.endsWith(".ics")) {
                if (dir.startsWith("Tasks/")) {
                    defaultFldr = Mailbox.ID_FOLDER_TASKS;
                    type = MailItem.Type.TASK;
                    view = MailItem.Type.TASK;
                } else {
                    defaultFldr = Mailbox.ID_FOLDER_CALENDAR;
                    type = MailItem.Type.APPOINTMENT;
                    view = MailItem.Type.APPOINTMENT;
                }
            } else if (file.endsWith(".wiki")) {
                defaultFldr = Mailbox.ID_FOLDER_NOTEBOOK;
                type = MailItem.Type.WIKI;
                view = MailItem.Type.WIKI;
            } else {
                defaultFldr = Mailbox.ID_FOLDER_BRIEFCASE;
                type = MailItem.Type.DOCUMENT;
                view = MailItem.Type.DOCUMENT;
            }
            if (types != null && !types.contains(type)) {
                return;
            }
            if (dir.equals("")) {
                if (fldr.getPath().equals("/")) {
                    fldr = mbox.getFolderById(oc, defaultFldr);
                }
                if (fldr.getDefaultView() != MailItem.Type.UNKNOWN && fldr.getDefaultView() != view &&
                    !((view == MailItem.Type.DOCUMENT || view == MailItem.Type.WIKI) &&
                            (fldr.getDefaultView() == MailItem.Type.DOCUMENT ||
                                    fldr.getDefaultView() == MailItem.Type.WIKI))) {
                    throw FormatterServiceException.INVALID_TYPE(view.toString(), fldr.getPath());
                }
            } else {
                String s = fldr.getPath();

                if (!s.endsWith("/"))
                    s += '/';
                if (dir.startsWith(s))
                    dir = dir.substring(s.length());
                fldr = createPath(context, fmap, fldr.getPath() + dir, view);
            }
            switch (type) {
            case APPOINTMENT:
            case TASK:
                boolean continueOnError = context.ignoreAndContinueOnError();
                boolean preserveExistingAlarms = context.preserveAlarms();
                InputStream is = ais.getInputStream();

                try {
                    if (aie.getSize() <=
                        LC.calendar_ics_import_full_parse_max_size.intValue()) {
                        List<ZVCalendar> icals = ZCalendarBuilder.buildMulti(is, UTF8);
                        ImportInviteVisitor visitor = new ImportInviteVisitor(oc,
                            fldr, preserveExistingAlarms);

                        Invite.createFromCalendar(context.targetAccount, null,
                            icals, true, continueOnError, visitor);
                    } else {
                        ZICalendarParseHandler handler =
                            new IcsImportParseHandler(oc, context.targetAccount,
                                fldr, continueOnError, preserveExistingAlarms);
                        ZCalendarBuilder.parse(is, UTF8, handler);
                    }
                } finally {
                    is.close();
                }
                break;
            case CONTACT:
                if (file.endsWith(".csv")) {
                    reader = new BufferedReader(new InputStreamReader(
                        ais.getInputStream(), UTF8));
                    ImportContacts.ImportCsvContacts(oc, context.targetMailbox,
                        new ItemId(fldr), ContactCSV.getContacts(reader, null));
                } else {
                    List<VCard> cards = VCard.parseVCard(new String(
                        readArchiveEntry(ais, aie), UTF8));

                    if (cards == null || cards.size() == 0 ||
                        (cards.size() == 1 && cards.get(0).fields.isEmpty())) {
                        addError(errs, FormatterServiceException.
                            MISSING_VCARD_FIELDS(name));
                        return;
                    }
                    for (VCard vcf : cards) {
                        if (vcf.fields.isEmpty())
                            continue;
                        mbox.createContact(oc, vcf.asParsedContact(),
                            fldr.getId(), null);
                    }
                }
                break;
            case DOCUMENT:
            case WIKI:
                String creator = context.getAuthAccount() == null ? null : context.getAuthAccount().getName();

                try {
                    oldItem = mbox.getItemByPath(oc, file, fldr.getId());
                    if (oldItem.getType() != type) {
                        addError(errs, FormatterServiceException.MISMATCHED_TYPE(name));
                    } else if (r == Resolve.Replace) {
                        mbox.delete(oc, oldItem.getId(), type);
                        throw MailServiceException.NO_SUCH_ITEM(oldItem.getId());
                    } else if (r != Resolve.Skip) {
                        newItem = mbox.addDocumentRevision(oc, oldItem.getId(),
                            creator, oldItem.getName(), null, ais.getInputStream());
                    }
                } catch (NoSuchItemException e) {
                    if (type == MailItem.Type.WIKI) {
                        newItem = mbox.createWiki(oc, fldr.getId(), file, creator, null, ais.getInputStream());
                    } else {
                        newItem = mbox.createDocument(oc, fldr.getId(), file, null, creator, null, ais.getInputStream());
                    }
                }
                if (newItem != null) {
                    if (timestamp)
                        mbox.setDate(oc, newItem.getId(), type, aie.getModTime());
                }
                break;
            case MESSAGE:
                int flags = aie.isUnread() ? Flag.BITMASK_UNREAD : 0;
                DeliveryOptions opt = new DeliveryOptions().
                    setFolderId(fldr.getId()).setNoICal(true).setFlags(flags);

                mbox.addMessage(oc, ais.getInputStream(), (int)aie.getSize(),
                    timestamp ? aie.getModTime() : ParsedMessage.DATE_HEADER, opt, null);
                break;
            }
        } catch (Exception e) {
            if (e instanceof MailServiceException &&
                ((MailServiceException)e).getCode() == MailServiceException.QUOTA_EXCEEDED)
                throw (MailServiceException)e;
            else
                addError(errs, FormatterServiceException.UNKNOWN_ERROR(
                    aie.getName(), e));
        }
    }

    /**
     * Replaces characters which can't be encoded by the charset with '#'.
     *
     * @param str string to sanitize
     * @param encoder charset encoder
     * @return sanitized string or unchanged original string
     */
    private String sanitize(String str, CharsetEncoder encoder) {
        if (encoder.canEncode(str)) {
            return str;
        } else {
            StringBuilder buf = new StringBuilder(str.length());
            for (int i = 0; i < str.length(); i++) {
                char c = str.charAt(i);
                if (encoder.canEncode(c)) {
                    buf.append(c);
                } else {
                    buf.append('#');
                }
            }
            return buf.toString();
        }
    }

}
