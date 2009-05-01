/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
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
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.mail.MessagingException;
import javax.mail.Part;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimePart;
import javax.servlet.http.HttpServletResponse;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.HttpUtil;
import com.zimbra.common.util.HttpUtil.Browser;
import com.zimbra.common.util.tar.TarEntry;
import com.zimbra.common.util.tar.TarInputStream;
import com.zimbra.common.util.tar.TarOutputStream;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.index.MailboxIndex;
import com.zimbra.cs.index.SortBy;
import com.zimbra.cs.index.ZimbraQueryResults;
import com.zimbra.cs.mailbox.Appointment;
import com.zimbra.cs.mailbox.CalendarItem;
import com.zimbra.cs.mailbox.Chat;
import com.zimbra.cs.mailbox.Contact;
import com.zimbra.cs.mailbox.Conversation;
import com.zimbra.cs.mailbox.Document;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.Mountpoint;
import com.zimbra.cs.mailbox.Note;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mailbox.SearchFolder;
import com.zimbra.cs.mailbox.Tag;
import com.zimbra.cs.mailbox.Task;
import com.zimbra.cs.mailbox.WikiItem;
import com.zimbra.cs.mailbox.CalendarItem.Instance;
import com.zimbra.cs.mailbox.MailServiceException.NoSuchItemException;
import com.zimbra.cs.mailbox.Mailbox.SetCalendarItemData;
import com.zimbra.cs.mailbox.MailboxManager.MailboxLock;
import com.zimbra.cs.mailbox.Message.CalendarItemInfo;
import com.zimbra.cs.mailbox.calendar.IcsImportParseHandler;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.mailbox.calendar.IcsImportParseHandler.ImportInviteVisitor;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZCalendarBuilder;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZICalendarParseHandler;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZVCalendar;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.mime.ParsedContact;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.service.UserServlet;
import com.zimbra.cs.service.UserServletException;
import com.zimbra.cs.service.UserServlet.Context;
import com.zimbra.cs.service.mail.ImportContacts;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.service.util.ItemData;

public class TarFormatter extends Formatter {
    private Pattern ILLEGAL_FILE_CHARS = Pattern.compile("[\\/\\:\\*\\?\\\"\\<\\>\\|]");
    private Pattern ILLEGAL_FOLDER_CHARS = Pattern.compile("[\\:\\*\\?\\\"\\<\\>\\|]");
    private static String UTF8 = "UTF-8";
    private static enum Resolve { Modify, Replace, Reset, Skip }

    private static final class SortPath implements Comparator<MailItem> {
        SortPath()  { }
        public int compare(MailItem m1, MailItem m2) {
            try {
                int ret = m1.getPath().compareTo(m2.getPath());
                return ret == 0 ? m1.getName().compareTo(m2.getName()) : ret;
            } catch (Exception e) {
                return 0;
            }
        }
    }
    
    @Override public String[] getDefaultMimeTypes() {
        return new String[] { "application/x-tar-compressed" };
    }

    @Override public String getDefaultSearchTypes() {
        return MailboxIndex.SEARCH_FOR_EVERYTHING;
    }

    @Override public String getType() { return "tgz"; }

    @Override public boolean canBeBlocked() { return true; }
    @Override public boolean supportsSave() { return true; }

    @Override public void formatCallback(Context context) throws IOException,
        ServiceException, UserServletException {
        HashMap<Integer, Integer> cnts = new HashMap<Integer, Integer>();
        boolean conversations = false;
        HashMap<Integer, String> fldrs = new HashMap<Integer, String>();
        String emptyname = context.params.get("emptyname");
        String filename = context.params.get("filename");
        String lock = context.params.get("lock");
        MailboxLock ml = null;
        Set<String> names = new HashSet<String>(4096);
        String query = context.getQueryString();
        byte sysTypes[] = {
            MailItem.TYPE_FOLDER, MailItem.TYPE_SEARCHFOLDER, MailItem.TYPE_TAG,
            MailItem.TYPE_FLAG, MailItem.TYPE_MOUNTPOINT
        };
        byte searchTypes[] = {
            MailItem.TYPE_MESSAGE, MailItem.TYPE_CONTACT,
            MailItem.TYPE_DOCUMENT, MailItem.TYPE_WIKI,
            MailItem.TYPE_APPOINTMENT, MailItem.TYPE_TASK, MailItem.TYPE_CHAT,
            MailItem.TYPE_NOTE
        };
        TarOutputStream tos = null;
        String types = context.getTypesString();

        try {
            if (emptyname != null && !emptyname.equals("") &&
                !emptyname.endsWith(".tgz"))
                emptyname += ".tgz";
            if (filename == null || filename.equals("")) {
                Date date = new Date();
                DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT);
                SimpleDateFormat sdf = new SimpleDateFormat(".H-m-s");

                filename = context.targetMailbox.getAccountId() + '.' +
                    df.format(date).replace('/', '-') + sdf.format(date);
            }
            if (!filename.endsWith(".tgz"))
                filename += ".tgz";
            context.resp.addHeader("Content-Disposition", Part.ATTACHMENT +
                "; filename=" + HttpUtil.encodeFilename(context.req, filename));
            context.resp.setContentType("application/x-tar-compressed");
            if (types != null && !types.equals("")) {
                Arrays.sort(searchTypes = MailboxIndex.parseTypesString(types));
                sysTypes = new byte[0];
                if (Arrays.binarySearch(searchTypes, MailItem.TYPE_CONVERSATION) >= 0) {
                    int i = 0;
                    byte newTypes[] = new byte[searchTypes.length - 1];
                
                    for (byte type : searchTypes)
                        if (type != MailItem.TYPE_CONVERSATION)
                            newTypes[i++] = type;
                    conversations = true;
                    searchTypes = newTypes;
                }
            }
            if (lock != null && (lock.equals("1") || lock.equals("t") ||
                lock.equals("true")))
                ml = MailboxManager.getInstance().beginMaintenance(
                    context.targetMailbox.getAccountId(),
                    context.targetMailbox.getId());
            if (context.respListItems != null) {
                try {
                    for (MailItem mi : context.respListItems)
                        tos = saveItem(context, mi, fldrs, cnts, names, false, tos);
                } catch (Exception e) {
                    warn(e);
                }
            } else if (context.target != null && !(context.target instanceof
                Folder)) {
                try {
                    tos = saveItem(context, context.target, fldrs, cnts, names,
                        false, tos);
                } catch (Exception e) {
                    warn(e);
                }
            } else {
                ZimbraQueryResults results = null;

                if (context.target instanceof Folder) {
                    Folder f = (Folder)context.target;
                    
                    if (f.getId() != Mailbox.ID_FOLDER_USER_ROOT) {
                        conversations = false;
                        query = "under:\"" + f.getPath() + "\"" +
                            (query == null ? "" : " " + query);
                    }
                }
                if (query == null || query.equals("")) {
                    SortPath sp = new SortPath();
                    
                    for (byte type : sysTypes) {
                        List<MailItem> items =
                            context.targetMailbox.getItemList(
                            context.opContext, type);
                        
                        Collections.sort(items, sp);
                        for (MailItem item : items)
                            tos = saveItem(context, item, fldrs, cnts, names,
                                false, tos);
                    }
                    if (types == null || types.equals(""))
                        conversations = true;
                    query = "is:local";
                }
                try {
                    results = context.targetMailbox.search(context.opContext,
                        query, searchTypes, SortBy.NONE, 4096);
                } catch (com.zimbra.cs.index.queryparser.ParseException e) {
                    throw ServiceException.PARSE_ERROR(e.getLocalizedMessage(), e);
                }
                try {
                    while (results.hasNext())
                        tos = saveItem(context, results.getNext().getMailItem(),
                            fldrs, cnts, names, false, tos);
                    if (conversations) {
                        for (MailItem item : context.targetMailbox.getItemList(
                            context.opContext, MailItem.TYPE_CONVERSATION)) 
                            tos = saveItem(context, item, fldrs, cnts, names,
                                false, tos);
                    }
                } catch (Exception e) {
                    warn(e);
                } finally {
                    if (results != null)
                        results.doneWithSearchResults();
                }
                if (tos == null) {
                    if (emptyname == null)
                        throw new UserServletException(HttpServletResponse.
                            SC_NO_CONTENT, "No data found");
                    context.resp.setHeader("Content-Disposition", Part.ATTACHMENT +
                        "; filename=" + HttpUtil.encodeFilename(context.req,
                        emptyname));
                    tos = new TarOutputStream(new GZIPOutputStream(
                        context.resp.getOutputStream()));
                }
            }
        } finally {
            if (ml != null)
                MailboxManager.getInstance().endMaintenance(ml, true, true);
            if (tos != null) {
                try {
                    tos.close();
                } catch (Exception e) {
                }
            }
        }
    }
    
    private TarOutputStream saveItem(Context context, MailItem mi, 
        HashMap<Integer, String> fldrs, HashMap<Integer, Integer> cnts,
        Set<String> names, boolean version, TarOutputStream tos) throws
        ServiceException {
        String charset = context.params.get("charset");
        int cnt = 1;
        String ext = null, name = null;
        String extra = null;
        Integer fid = mi.getFolderId();
        String fldr;
        InputStream is = null;
        String metaParam = context.params.get(UserServlet.QP_META);
        boolean meta = metaParam == null || !metaParam.equals("0");
        
        if (charset == null)
            charset = UTF8;
        if (!version && mi.isTagged(Flag.ID_FLAG_VERSIONED)) {
            for (MailItem rev : context.targetMailbox.getAllRevisions(
                context.opContext, mi.getId(), mi.getType())) {
                if (mi.getVersion() != rev.getVersion())
                    tos = saveItem(context, rev, fldrs, cnts, names, true, tos);
            }
        }
        switch (mi.getType()) {
        case MailItem.TYPE_APPOINTMENT:
            Appointment appt = (Appointment)mi;
            
            if (!appt.isPublic() && !appt.allowPrivateAccess(context.authAccount,
                context.isUsingAdminPrivileges()))
                return tos;
            if (meta) {
                name = appt.getSubject();
                ext = "appt";
            } else {
                ext = "ics";
            }
            break;
        case MailItem.TYPE_CHAT:
            ext = "chat";
            break;
        case MailItem.TYPE_CONTACT:
            Contact ct = (Contact)mi;
            
            name = ct.getFileAsString();
            if (!meta)
                ext = "vcf";
            break;
        case MailItem.TYPE_FLAG:
            return tos;
        case MailItem.TYPE_FOLDER:
        case MailItem.TYPE_MOUNTPOINT:
        case MailItem.TYPE_SEARCHFOLDER:
            if (mi.getId() == Mailbox.ID_FOLDER_ROOT)
                name = "ROOT";
            else if (mi.getId() == Mailbox.ID_FOLDER_USER_ROOT)
                name = "USER_ROOT";
            else
                name = mi.getName();
            break;
        case MailItem.TYPE_MESSAGE:
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
        case MailItem.TYPE_NOTE:
            ext = "note";
            break;
        case MailItem.TYPE_TASK:
            Task task = (Task)mi;
            
            if (!task.isPublic() && !task.allowPrivateAccess(context.authAccount,
                context.isUsingAdminPrivileges()))
                return tos;
            ext = "task";
            break;
        case MailItem.TYPE_VIRTUAL_CONVERSATION:
            return tos;
        case MailItem.TYPE_WIKI:
            ext = "wiki";
            break;
        }
        fldr = fldrs.get(fid);
        if (fldr == null) {
            Folder f = mi.getMailbox().getFolderById(context.opContext, fid);
            
            cnts.put(fid, 1);
            fldr = f.getPath();
            if (fldr.startsWith("/"))
                fldr = fldr.substring(1);
            fldr = ILLEGAL_FOLDER_CHARS.matcher(fldr).replaceAll("_");
            fldrs.put(fid, fldr);
        } else if (!(mi instanceof Folder)) {
            final int BATCH = 500;
            
            cnt = cnts.get(fid) + 1;
            cnts.put(fid, cnt);
            if (cnt / BATCH > 0)
                fldr = fldr + '!' + (cnt / BATCH); 
        }
        try {
            byte data[] = null;
            String path = getEntryName(mi, fldr, name, ext, names);
            TarEntry entry = new TarEntry(path + ".meta");
            long miSize = mi.getSize();

            if (miSize == 0 && mi.getDigest() != null) {
                ZimbraLog.misc.error("blob db size 0 for item %d", mi.getId());
                return tos;
            }
            try {
                is = mi.getContentStream();
            } catch (Exception e) {
                ZimbraLog.misc.error("missing blob for item %d: expected %d",
                    mi.getId(), miSize);
                return tos;
            }
            entry.setGroupName(MailItem.getNameForType(mi));
            entry.setMajorDeviceId(mi.getType());
            entry.setModTime(mi.getDate());
            if (mi instanceof Message && (mi.getFlagBitmask() &
                Flag.ID_FLAG_UNREAD) != 0)
                entry.setMode(entry.getMode() & ~0200);
            if (tos == null)
                tos = new TarOutputStream(new GZIPOutputStream(
                    context.resp.getOutputStream()), charset);
            tos.setLongFileMode(TarOutputStream.LONGFILE_GNU);
            if (meta) {
                byte[] metaData = new ItemData(mi, extra).encode();
                
                entry.setSize(metaData.length);
                tos.putNextEntry(entry);
                tos.write(metaData);
                tos.closeEntry();
            } else if (mi instanceof CalendarItem) {
                Browser browser = HttpUtil.guessBrowser(context.req);
                CalendarItem ci = (CalendarItem)mi;
                List<CalendarItem> calItems = new ArrayList<CalendarItem>();
                Collection<Instance> instances = ci.expandInstances(
                    context.getStartTime(), context.getEndTime(), false);
                boolean needAppleICalHacks = Browser.APPLE_ICAL.equals(browser);
                boolean useOutlookCompatMode = Browser.IE.equals(browser);
                OperationContext octxt = new OperationContext(context.authAccount,
                    context.isUsingAdminPrivileges());
                StringWriter writer = new StringWriter();
                
                if (!instances.isEmpty()) {
                    calItems.add(ci);
                    context.targetMailbox.writeICalendarForCalendarItems(
                        writer, octxt, calItems, useOutlookCompatMode, true,
                        needAppleICalHacks, true);
                    data = writer.toString().getBytes(charset);
                }
            } else if (mi instanceof Contact) {
                VCard vcf = VCard.formatContact((Contact)mi);
                
                data = vcf.formatted.getBytes(charset);
            } else if (mi instanceof Message) {
                if (context.hasPart()) {
                    MimeMessage mm = ((Message)mi).getMimeMessage();
                    
                    for (String part : context.getPart().split(",")) {
                        MimePart mp = Mime.getMimePart(mm, part);
                        int sz;
                        
                        if (mp == null)
                            throw MailServiceException.NO_SUCH_PART(part);
                        name = Mime.getFilename(mp);
                        sz = mp.getSize();
                        if (sz == -1)
                            sz = (int)miSize;
                        if (name == null) {
                            name = "attachment";
                        } else {
                            int dot = name.lastIndexOf('.');
                            
                            if (dot != -1 && dot < name.length() - 1) {
                                ext = name.substring(dot);
                                name = name.substring(0, dot);
                            }
                        }
                        entry.setName(getEntryName(mi, fldr, name, ext, names));
                        data = ByteUtil.readInput(mp.getInputStream(), sz, -1);
                        entry.setSize(data.length);
                        tos.putNextEntry(entry);
                        tos.write(data);
                        tos.closeEntry();
                    }
                    return tos;
                }
            }
            entry.setName(path);
            if (data != null) {
                entry.setSize(data.length);
                tos.putNextEntry(entry);
                tos.write(data);
                tos.closeEntry();
            } else if (is != null) {
                if (context.shouldReturnBody()) {
                    byte buf[] = new byte[tos.getRecordSize() * 20];
                    int in;
                    long remain = miSize;

                    entry.setSize(miSize);
                    tos.putNextEntry(entry);
                    while (remain > 0 && (in = is.read(buf)) >= 0) {
                        tos.write(buf, 0, remain < in ? (int)remain : in);
                        remain -= in;
                    }
                    if (remain != 0) {
                        ZimbraLog.misc.error("mismatched blob size for item %d: expected %d",
                             mi.getId(), miSize);
                        if (remain > 0) {
                            Arrays.fill(buf, (byte)' ');
                            while (remain > 0) {
                                tos.write(buf, 0, remain < buf.length ?
                                    (int)remain : buf.length);
                                remain -= buf.length;
                            }
                        }
                        tos.closeEntry();
                        entry.setName(path + ".err");
                        entry.setSize(0);
                        tos.putNextEntry(entry);
                    }
                } else {
                    // Read headers into memory, since we need to write the size first.
                    byte headerData[] = HeadersOnlyInputStream.getHeaders(is);
                    
                    entry.setSize(headerData.length);
                    tos.putNextEntry(entry);
                    tos.write(headerData);
                }
                tos.closeEntry();
            }
        } catch (Exception e) {
            throw ServiceException.FAILURE("archive error", e);
        } finally {
            ByteUtil.closeStream(is);
        }
        return tos;
    }
    
    private String getEntryName(MailItem mi, String fldr, String name,
        String ext, Set<String> names) {
        int counter = 0;
        String lpath, path;
        
        if ((name == null || name.length() == 0) &&
            (name = mi.getName()).length() == 0 &&
            (name = mi.getSubject()).length() == 0)
            name = MailItem.getNameForType(mi) + '-' + mi.getId();
        else if (name.length() > 127)
            name = name.substring(0, 126);
        name = ILLEGAL_FILE_CHARS.matcher(name).replaceAll("_").trim();
        while (name.endsWith("."))
            name = name.substring(0, name.length() - 1).trim();
        do {
            path = fldr.equals("") ? name : fldr + '/' + name;
            if (counter > 0)
                path += "-" + counter;
            if (ext != null)
                path += '.' + ext;
            counter++;
            lpath = path.toLowerCase();
        } while (names.contains(lpath));
        names.add(lpath);
        return path;
    }

    @Override public void saveCallback(Context context, String contentType, Folder fldr, String file)
    throws IOException, ServiceException {
        ItemData id = null;
        Map<String, Integer> digestMap = new HashMap<String, Integer>();
        StringBuffer errs = new StringBuffer();
        Map<Object, Folder> fmap = new HashMap<Object, Folder>();
        Map<Integer, Integer> idMap = new HashMap<Integer, Integer>();
        long last = System.currentTimeMillis();
        String types = context.getTypesString();
        String charset = context.params.get("charset");
        String resolve = context.params.get("resolve");
        String subfolder = context.params.get("subfolder");
        String timeout = context.params.get("timeout");
 
        try {
            int ids[] = null;
            Resolve r = resolve == null ? Resolve.Skip : Resolve.valueOf(
                resolve.substring(0,1).toUpperCase() +
                resolve.substring(1).toLowerCase());
            long interval = 45 * 1000;
            if (timeout != null)
                interval = Long.parseLong(timeout);
            byte searchTypes[] = null;

            if (context.reqListIds != null) {
                ids = context.reqListIds.clone();
                Arrays.sort(ids);
            }
            if (types != null && !types.equals("")) {
                Arrays.sort(searchTypes = MailboxIndex.parseTypesString(types));
                for (byte type : searchTypes) {
                    if (type == MailItem.TYPE_CONVERSATION) {
                        int idx = 0;
                        byte[] newTypes = new byte[searchTypes.length - 1];
    
                        for (byte t : searchTypes)
                            if (t != MailItem.TYPE_CONVERSATION)
                                newTypes[idx++] = t;
                        searchTypes = newTypes;
                        break;
                    }
                }
            }

            TarInputStream tis = new TarInputStream(new GZIPInputStream(
                context.getRequestInputStream(-1)), charset == null ? UTF8 :
                charset);
            List<Folder> flist = fldr.getSubfolderHierarchy();
            
            if (subfolder != null && !subfolder.equals("")) {
                fldr = createPath(context, fmap, fldr.getPath() + subfolder,
                    Folder.TYPE_UNKNOWN);
                flist = fldr.getSubfolderHierarchy();
            }
            if (r == Resolve.Reset) {
                for (Folder f : flist) {
                    try {
                        List<Integer> delIds;
                        
                        if (System.currentTimeMillis() - last > interval) {
                            updateClient(context, true);
                            last = System.currentTimeMillis();
                        }
                        if (searchTypes == null) {
                            delIds = context.targetMailbox.listItemIds(
                                context.opContext, MailItem.TYPE_UNKNOWN,
                                f.getId());
                        } else if (Arrays.binarySearch(searchTypes,
                            MailItem.TYPE_CONVERSATION) < 0) {
                            delIds = context.targetMailbox.getItemIds(
                                context.opContext, f.getId()).getIds(searchTypes);
                        } else {
                            byte[] delTypes = new byte[searchTypes.length - 1];
                            int i = 0;
                            
                            for (byte type : searchTypes)
                                if (type != MailItem.TYPE_CONVERSATION)
                                    delTypes[i++] = type;
                            delIds = context.targetMailbox.getItemIds(
                                context.opContext, f.getId()).getIds(delTypes);
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
                        context.targetMailbox.delete(context.opContext,
                            delIdsArray, MailItem.TYPE_UNKNOWN, null);
                    } catch (Exception e) {
                        if (!(e instanceof MailServiceException) ||
                            ((MailServiceException)e).getCode() !=
                            MailServiceException.NO_SUCH_FOLDER) {
                            r = Resolve.Replace;
                            addError(errs, f.getName(),
                                "unable to reset folder: " + e);
                        }
                    }
                }
                context.targetMailbox.purge(MailItem.TYPE_UNKNOWN);
                flist = fldr.getSubfolderHierarchy();
            }
            for (Folder f : flist) {
                fmap.put(f.getId(), f);
                fmap.put(f.getPath(), f);
            }
            try {
                TarEntry te;
                Boolean meta = false;
                
                while ((te = tis.getNextEntry()) != null) {
                    if (System.currentTimeMillis() - last > interval) {
                        updateClient(context, true);
                        last = System.currentTimeMillis();
                    }
                    if (te.getName().endsWith(".meta")) {
                        meta = true;
                        if (id != null)
                            addItem(context, fldr, fmap, digestMap, idMap,
                                ids, searchTypes, r, id, tis, null, errs);
                        id = new ItemData(readTarEntry(tis, te));
                        continue;
                    }
                    if (te.getName().endsWith(".err")) {
                        addError(errs, te.getName(),
                            "ignored item data size mismatch");
                    } else if (id == null) {
                        if (meta)
                            addError(errs, te.getName(),
                                "item content missing meta information");
                        else
                            addData(context, fldr, fmap, searchTypes, r, tis,
                                te, errs);
                    } else if (id.ud.type != te.getMajorDeviceId() ||
                        (id.ud.getBlobDigest() != null && id.ud.size !=
                        te.getSize())) {
                        addError(errs, te.getName(),
                            "mismatched item content and meta");
                    } else {
                        addItem(context, fldr, fmap, digestMap, idMap, ids,
                            searchTypes, r, id, tis, te, errs);
                    }
                    id = null;
                }
                if (id != null)
                    addItem(context, fldr, fmap, digestMap, idMap, ids,
                        searchTypes, r, id, tis, null, errs);
            } catch (Exception e) {
                addError(errs, id == null ? null : id.path,
                    e.getLocalizedMessage());
                id = null;
            } finally {
                if (tis != null)
                    tis.close();
            }
        } catch (Exception e) {
            if (errs.length() > 0) {
                addError(errs, null, e.getLocalizedMessage());
                throw new IOException(errs.toString());
            }
            throw ServiceException.FAILURE("Tar formatter failure", e);
        }
    }

    private void addError(StringBuffer errs, String path, String err) {
        if (errs.length() != 0)
            errs.append('\n');
        if (path != null)
            errs.append(path + ": ");
        errs.append(err);
        ZimbraLog.misc.info(err);
    }
    
    private Folder createParent(Context context, Map<Object, Folder> fmap,
        String path, byte view) throws ServiceException {
        String parent = path.substring(0, path.lastIndexOf('/'));
        
        if (parent.equals(""))
            parent = "/";
        return createPath(context, fmap, parent, view);
    }

    private Folder createPath(Context context, Map<Object, Folder> fmap,
        String path, byte view) throws ServiceException {
        Folder fldr;
        
        if ((fldr = fmap.get(path)) == null) {
            try {
                fldr = context.targetMailbox.getFolderByPath(context.opContext,
                    path);
            } catch (Exception e) {
                fldr = context.targetMailbox.createFolder(context.opContext,
                    path, (byte) 0, view);
            }
            fmap.put(fldr.getId(), fldr);
            fmap.put(fldr.getPath(), fldr);
        }
        if (view != Folder.TYPE_UNKNOWN && fldr.getDefaultView() !=
            Folder.TYPE_UNKNOWN && fldr.getDefaultView() != view)
            throw ServiceException.INVALID_REQUEST(
                "folder cannot contain item type " +
                Folder.getNameForType(view), null);
        return fldr;
    }

    public static byte[] readTarEntry(TarInputStream tis, TarEntry te) throws
        IOException {
        if (te == null)
            return null;
        
        int dsz = (int)te.getSize();
        byte[] data;
        
        if (dsz == 0)
            return null;
        data = new byte[dsz];
        if (tis.read(data, 0, dsz) != dsz)
            throw new IOException("archive read err");
        return data;
    }

    private String string(String s) { return s == null ? new String() : s; }

    private void warn(Exception e) {
        if (e.getCause() == null)
            ZimbraLog.misc.warn("TarFormatter warning: %s", e);
        else
            ZimbraLog.misc.warn("TarFormatter warning: %s: %s", e, e.getCause().toString());
    }

    private void addItem(Context context, Folder fldr,
        Map<Object, Folder> fmap, Map<String, Integer> digestMap,
        Map<Integer, Integer> idMap, int[] ids, byte[] searchTypes, Resolve r,
        ItemData id, TarInputStream tis, TarEntry te, StringBuffer errs) throws
        MessagingException, ServiceException {
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
                (searchTypes != null && Arrays.binarySearch(searchTypes,
                id.ud.type) < 0))
                return;
            if (id.ud.getBlobDigest() != null && te == null) {
                addError(errs, id.path, "missing item blob for meta");
                return;
            }
            if (root)
                path = id.path;
            else
                path = fldr.getPath() + id.path;
            if (path.endsWith("/") && !path.equals("/"))
                path = path.substring(0, path.length() - 1);
            switch (mi.getType()) {
            case MailItem.TYPE_APPOINTMENT:
            case MailItem.TYPE_TASK:
                CalendarItem ci = (CalendarItem)mi;
    
                fldr = createPath(context, fmap, path, ci.getType() ==
                    MailItem.TYPE_APPOINTMENT ? Folder.TYPE_APPOINTMENT :
                    Folder.TYPE_TASK);
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
                    byte[] data = readTarEntry(tis, te);
                    Map<Integer, MimeMessage> blobMimeMsgMap = data == null ?
                        null : CalendarItem.decomposeBlob(data);
                    SetCalendarItemData defScid = new SetCalendarItemData();
                    SetCalendarItemData exceptionScids[] = null;
                    Invite invs[] = ci.getInvites();
                    MimeMessage mm;
                    
                    if (invs != null && invs.length > 0) {
                        defScid.mInv = invs[0];
                        if (blobMimeMsgMap != null && (mm =
                            blobMimeMsgMap.get(defScid.mInv.getMailItemId())) != null)
                            defScid.mPm = new ParsedMessage(mm,
                                mbox.attachmentsIndexingEnabled());
                        if (invs.length > 1) {
                            exceptionScids = new SetCalendarItemData[invs.length - 1];
                            for (int i = 1; i < invs.length; i++) {
                                SetCalendarItemData scid = new SetCalendarItemData();
                                
                                scid.mInv = invs[i];
                                if (blobMimeMsgMap != null && (mm =
                                    blobMimeMsgMap.get(
                                        defScid.mInv.getMailItemId())) != null)
                                    scid.mPm = new ParsedMessage(mm,
                                        mbox.attachmentsIndexingEnabled());
                                exceptionScids[i - 1] = scid;
                            }
                        }
                        newItem = mbox.setCalendarItem(oc, oldItem != null &&
                            r == Resolve.Modify ? oldItem.getFolderId() :
                            fldr.getId(), ci.getFlagBitmask(),
                            ci.getTagBitmask(), defScid, exceptionScids,
                            ci.getAllReplies(),
                            ad == null ? CalendarItem.NEXT_ALARM_KEEP_CURRENT : ad.getNextAt());
                    }
                }
                break;
            case MailItem.TYPE_CHAT:
                Chat chat = (Chat)mi;
                
                pm = new ParsedMessage(tis, (int)te.getSize(),
                    mi.getDate(), mbox.attachmentsIndexingEnabled());
                fldr = createPath(context, fmap, path, Folder.TYPE_CHAT);
                if (root && r != Resolve.Reset) {
                    Chat oldChat = null;
                    
                    try {
                        oldChat = mbox.getChatById(oc, chat.getId());
                        if (oldChat.getFolderId() != fldr.getFolderId())
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
                        chat.getFlagBitmask(), chat.getTagString());
                break;
            case MailItem.TYPE_CONVERSATION:
                Conversation cv = (Conversation)mi;
    
                if (r != Resolve.Reset && r != Resolve.Skip) {
                    try {
                        oldItem = mbox.getConversationByHash(oc, Mailbox.getHash(
                            cv.getSubject()));
                    } catch (Exception e) {
                    }
                }
                break;
            case MailItem.TYPE_CONTACT:
                Contact ct = (Contact)mi;
                
                fldr = createPath(context, fmap, path, Folder.TYPE_CONTACT);
                if (root && r != Resolve.Reset) {
                    Contact oldContact = null;
                    
                    try {
                        oldContact = mbox.getContactById(oc, ct.getId());
                        if (oldContact.getFolderId() != fldr.getFolderId())
                            oldContact = null;
                    } catch (Exception e) {
                    }
                    
                    if (oldContact != null) {
                        String email = string(ct.get(Contact.A_email));
                        String first = string(ct.get(Contact.A_firstName));
                        String name = string(ct.get(Contact.A_fullName));
                        String oldemail = string(oldContact.get(Contact.A_email));
                        String oldfirst = string(oldContact.get(Contact.A_firstName));
                        String oldname = string(oldContact.get(Contact.A_fullName));
                        
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
                                        readTarEntry(tis, te)));
                            }
                        }
                    }
                }
                if (oldItem == null)
                    newItem = mbox.createContact(oc, new ParsedContact(
                        ct.getFields(), readTarEntry(tis, te)), fldr.getId(),
                        ct.getTagString());
                break;
            case MailItem.TYPE_DOCUMENT:
            case MailItem.TYPE_WIKI:
                Document doc = (Document)mi;
                Document oldDoc = null;
                Integer oldId = idMap.get(mi.getId());
                
                fldr = createParent(context, fmap, path, doc.getType() ==
                    MailItem.TYPE_DOCUMENT ? Folder.TYPE_DOCUMENT :
                    Folder.TYPE_WIKI);
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
                                oldDoc.getId(), doc.getType(), tis,
                                doc.getCreator(), doc.getName());
                        if (r != Resolve.Skip)
                            mbox.setDate(oc, newItem.getId(), doc.getType(),
                                doc.getDate());
                    }
                }
                if (oldItem == null) {
                    if (mi.getType() == MailItem.TYPE_DOCUMENT) {
                        newItem = mbox.createDocument(oc, fldr.getId(),
                            doc.getName(), doc.getContentType(),
                            doc.getCreator(), tis);
                    } else {
                        WikiItem wi = (WikiItem)mi;
                        
                        newItem = mbox.createWiki(oc, fldr.getId(),
                            wi.getWikiWord(), wi.getCreator(), tis);
                    }
                    mbox.setDate(oc, newItem.getId(), doc.getType(),
                        doc.getDate());
                    idMap.put(doc.getId(), newItem.getId());
                }
                break;
            case MailItem.TYPE_FLAG:
                return;
            case MailItem.TYPE_FOLDER:
                Folder f = (Folder)mi;
                Folder oldF = null;
                byte view = f.getDefaultView();
                
                if (view == MailItem.TYPE_CONVERSATION ||
                    view == MailItem.TYPE_FLAG || view == MailItem.TYPE_TAG)
                    break;
                try {
                    oldF = mbox.getFolderByPath(oc, path);
                } catch (Exception e) {
                }
                if (oldF != null) {
                    oldItem = oldF;
                    if (r != Resolve.Skip) {
                        if (!f.getUrl().equals(oldF.getUrl()))
                            mbox.setFolderUrl(oc, oldF.getId(), f.getUrl());
                        if (!f.getEffectiveACL().toString().equals(
                            oldF.getEffectiveACL().toString()))
                            mbox.setPermissions(oc, oldF.getId(),
                                f.getEffectiveACL());
                    }
                }
                if (oldItem == null) {
                    fldr = createParent(context, fmap, path, Folder.TYPE_UNKNOWN);
                    newItem = fldr = mbox.createFolder(oc, f.getName(),
                        fldr.getId(), f.getAttributes(), f.getDefaultView(),
                        f.getFlagBitmask(), f.getColor(), f.getUrl());
                    fmap.put(fldr.getId(), fldr);
                    fmap.put(fldr.getPath(), fldr);
                }
                break;
            case MailItem.TYPE_MESSAGE:
                Message msg = (Message)mi;
                Message oldMsg = null;
                
                fldr = createPath(context, fmap, path, Folder.TYPE_MESSAGE);
                if (root && r != Resolve.Reset) {
                    try {
                        oldMsg = mbox.getMessageById(oc, msg.getId());
                        if (!msg.getDigest().equals(oldMsg.getDigest()) ||
                             oldMsg.getFolderId() != fldr.getFolderId())
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
                            for (MailItem item : mbox.getItemList(oc,
                                MailItem.TYPE_MESSAGE, fldr.getId()))
                                digestMap.put(item.getDigest(), item.getId());
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
                    pm = new ParsedMessage(tis, (int)te.getSize(),
                        msg.getDate(), mbox.attachmentsIndexingEnabled());
                    newItem = mbox.addMessage(oc, pm, fldr.getId(), true,
                        msg.getFlagBitmask(), msg.getTagString());
                }
                break;
            case MailItem.TYPE_MOUNTPOINT:
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
                    fldr = createParent(context, fmap, path, Folder.TYPE_UNKNOWN);
                    newItem = mbox.createMountpoint(context.opContext,
                        fldr.getId(), mp.getName(), mp.getOwnerId(),
                        mp.getRemoteId(), mp.getDefaultView(),
                        mp.getFlagBitmask(), mp.getColor());
                }
                break;
            case MailItem.TYPE_NOTE:
                Note note = (Note)mi;
                Note oldNote = null;
    
                fldr = createPath(context, fmap, path, Folder.TYPE_NOTE);
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
                                String(readTarEntry(tis, te), UTF8));
                    }
                    break;
                }
                if (oldItem == null) {
                    newItem = mbox.createNote(oc, new String(readTarEntry(tis, te),
                        UTF8), note.getBounds(), note.getColor(),
                        fldr.getId());
                }
                break;
            case MailItem.TYPE_SEARCHFOLDER:
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
                    fldr = createParent(context, fmap, path, Folder.TYPE_UNKNOWN);
                    newItem = mbox.createSearchFolder(oc, fldr.getId(),
                        sf.getName(), sf.getQuery(), sf.getReturnTypes(),
                        sf.getSortField(), sf.getColor());
                }
                break;
            case MailItem.TYPE_TAG:
                Tag tag = (Tag)mi;
                
                try {
                    Tag oldTag = mbox.getTagByName(tag.getName());
                    
                    oldItem = oldTag;
                } catch (Exception e) {
                }
                if (oldItem == null)
                    newItem = mbox.createTag(oc, tag.getName(), tag.getColor());
                break;
            case MailItem.TYPE_VIRTUAL_CONVERSATION:
                return;
            }
            if (newItem != null) {
                if (mi.getColor() != newItem.getColor())
                    mbox.setColor(oc, newItem.getId(), newItem.getType(),
                        mi.getColor());
                
                if (!id.flags.equals(newItem.getFlagString()) ||
                    !id.tags.equals(newItem.getTagString()))
                    mbox.setTags(oc, newItem.getId(), newItem.getType(),
                        id.flags, id.tags, null);
            } else if (oldItem != null && r == Resolve.Modify) {
                if (mi.getColor() != oldItem.getColor())
                    mbox.setColor(oc, oldItem.getId(), oldItem.getType(),
                        mi.getColor());
                if (!id.flags.equals(oldItem.getFlagString()) ||
                    !id.tags.equals(oldItem.getTagString()))
                    mbox.setTags(oc, oldItem.getId(), oldItem.getType(),
                        id.flags, id.tags, null);
            }
        } catch (MailServiceException e) {
            if (r != Resolve.Skip ||
                e.getCode() != MailServiceException.ALREADY_EXISTS) {
                addError(errs, id.path, e.getMessage());
            }
        } catch (Exception e) {
            addError(errs, id.path, e.getMessage());
        }
    }

    private void addData(Context context, Folder fldr,
        Map<Object, Folder> fmap, byte[] searchTypes, Resolve r,
        TarInputStream tis, TarEntry te, StringBuffer errs) throws
        MessagingException, ServiceException {
        try {
            int defaultFldr;
            Mailbox mbox = fldr.getMailbox();
            String dir, file;
            String name = te.getName();
            int idx = name.lastIndexOf('/');
            MailItem newItem = null, oldItem;
            OperationContext oc = context.opContext;
            BufferedReader reader;
            byte type, view;

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
                type = MailItem.TYPE_CONTACT;
                view = Folder.TYPE_CONTACT;
            } else if (file.endsWith(".eml")) {
                defaultFldr = Mailbox.ID_FOLDER_INBOX;
                type = MailItem.TYPE_MESSAGE;
                view = Folder.TYPE_MESSAGE;
            } else if (file.endsWith(".ics")) {
                if (dir.startsWith("Tasks/")) {
                    defaultFldr = Mailbox.ID_FOLDER_TASKS;
                    type = MailItem.TYPE_TASK;
                    view = Folder.TYPE_TASK;
                } else {
                    defaultFldr = Mailbox.ID_FOLDER_CALENDAR;
                    type = MailItem.TYPE_APPOINTMENT;
                    view = Folder.TYPE_APPOINTMENT;
                }
            } else if (file.endsWith(".wiki")) {
                defaultFldr = Mailbox.ID_FOLDER_NOTEBOOK;
                type = MailItem.TYPE_WIKI;
                view = Folder.TYPE_WIKI;
            } else {
                defaultFldr = Mailbox.ID_FOLDER_BRIEFCASE;
                type = MailItem.TYPE_DOCUMENT;
                view = Folder.TYPE_DOCUMENT;
            }
            if (searchTypes != null && Arrays.binarySearch(searchTypes, type) < 0)
                return;
            if (dir.equals("")) {
                if (fldr.getPath().equals("/"))
                    fldr = mbox.getFolderById(oc, defaultFldr);
                if (fldr.getDefaultView() != Folder.TYPE_UNKNOWN &&
                    fldr.getDefaultView() != view)
                    throw ServiceException.INVALID_REQUEST(
                        "folder cannot contain item type " +
                        Folder.getNameForType(view), null);
            } else {
                String s = fldr.getPath();
                
                if (!s.endsWith("/"))
                    s += '/';
                if (dir.startsWith(s))
                    dir = dir.substring(s.length());
                fldr = createPath(context, fmap, fldr.getPath() + dir, view);
            }
            switch (type) {
            case MailItem.TYPE_APPOINTMENT:
            case MailItem.TYPE_TASK:
                boolean continueOnError = context.ignoreAndContinueOnError();
                boolean preserveExistingAlarms = context.preserveAlarms();
                
                reader = new BufferedReader(new InputStreamReader(tis, UTF8));
                try {
                    if (te.getSize() <=
                        LC.calendar_ics_import_full_parse_max_size.intValue()) {
                        List<ZVCalendar> icals = ZCalendarBuilder.buildMulti(reader);
                        ImportInviteVisitor visitor = new ImportInviteVisitor(oc,
                            fldr, preserveExistingAlarms);
                        
                        Invite.createFromCalendar(context.targetAccount, null,
                            icals, true, continueOnError, visitor);
                    } else {
                        ZICalendarParseHandler handler =
                            new IcsImportParseHandler(oc, context.targetAccount,
                                fldr, continueOnError, preserveExistingAlarms);
                        ZCalendarBuilder.parse(reader, handler);
                    }
                } finally {
                    reader.close();
                }
                break;
            case MailItem.TYPE_CONTACT:
                if (file.endsWith(".csv")) {
                    reader = new BufferedReader(new InputStreamReader(tis, UTF8));
                    ImportContacts.ImportCsvContacts(oc, context.targetMailbox,
                        new ItemId(fldr), ContactCSV.getContacts(reader, null));
                } else {
                    List<VCard> cards = VCard.parseVCard(new String(
                        readTarEntry(tis, te), UTF8));
                    
                    if (cards == null || cards.size() == 0 ||
                        (cards.size() == 1 && cards.get(0).fields.isEmpty())) {
                        addError(errs, name, "no contact fields found in vcard");
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
            case MailItem.TYPE_DOCUMENT:
            case MailItem.TYPE_WIKI:
                String creator = (context.authAccount == null ? null :
                    context.authAccount.getName());
                
                try {
                    oldItem = mbox.getItemByPath(oc, file, fldr.getId());
                    if (oldItem.getType() != type) {
                        addError(errs, name, "cannot overwrite non matching data");
                    } else if (r == Resolve.Replace) {
                        mbox.delete(oc, oldItem.getId(), type);
                        throw MailServiceException.NO_SUCH_ITEM(oldItem.getId());
                    } else if (r != Resolve.Skip) {
                        newItem = mbox.addDocumentRevision(oc, oldItem.getId(),
                            type, tis, creator, oldItem.getName());
                    }
                } catch (NoSuchItemException e) {
                    if (type == MailItem.TYPE_WIKI) {
                        newItem = mbox.createWiki(oc, fldr.getId(), file,
                            creator, tis);
                    } else {
                        newItem = mbox.createDocument(oc, fldr.getId(),
                            file, null, creator, tis);
                    }
                }
                if (newItem != null) {
                    mbox.setDate(oc, newItem.getId(), type,
                        te.getModTime().getTime());
                    if (type == MailItem.TYPE_WIKI)
                        WikiFormatter.expireCacheItem(fldr);
                }
                break;
            case MailItem.TYPE_MESSAGE:
                ParsedMessage pm = new ParsedMessage(tis, (int)te.getSize(),
                    te.getModTime().getTime(), mbox.attachmentsIndexingEnabled());
                
                mbox.addMessage(oc, pm, fldr.getId(), true,
		    (te.getMode() & 0200) == 0 ? 0 : Flag.ID_FLAG_UNREAD, null);
                break;
            }
        } catch (Exception e) {
            addError(errs, te.getName(), e.getMessage());
        }
    }
}
