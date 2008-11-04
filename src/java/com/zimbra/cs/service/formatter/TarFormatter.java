/*
 * ***** BEGIN LICENSE BLOCK *****
 *
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008 Zimbra, Inc.
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.mail.MessagingException;
import javax.mail.Part;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServletResponse;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.HttpUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.util.tar.TarEntry;
import com.zimbra.common.util.tar.TarInputStream;
import com.zimbra.common.util.tar.TarOutputStream;
import com.zimbra.cs.index.MailboxIndex;
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
import com.zimbra.cs.mailbox.SearchFolder;
import com.zimbra.cs.mailbox.Tag;
import com.zimbra.cs.mailbox.Task;
import com.zimbra.cs.mailbox.WikiItem;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.mailbox.Mailbox.SetCalendarItemData;
import com.zimbra.cs.mailbox.MailboxManager.MailboxLock;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.mime.ParsedContact;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.service.UserServlet;
import com.zimbra.cs.service.UserServletException;
import com.zimbra.cs.service.UserServlet.Context;
import com.zimbra.cs.service.util.ItemData;

public class TarFormatter extends Formatter {
    private Pattern ILLEGAL_FILE_CHARS = Pattern.compile("[\\/\\:\\*\\?\\\"\\<\\>\\|]");
    private Pattern ILLEGAL_FOLDER_CHARS = Pattern.compile("[\\:\\*\\?\\\"\\<\\>\\|]");
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
        String filename = context.params.get("filename");
        String lock = context.params.get("lock");
        MailboxLock ml = null;
        Set<String> names = new HashSet<String>(4096);
        String query = context.getQueryString();
        byte[] sysTypes = {
            MailItem.TYPE_FOLDER, MailItem.TYPE_SEARCHFOLDER, MailItem.TYPE_TAG,
            MailItem.TYPE_FLAG, MailItem.TYPE_MOUNTPOINT
        };
        byte[] searchTypes = {
            MailItem.TYPE_MESSAGE, MailItem.TYPE_CONTACT,
            MailItem.TYPE_DOCUMENT, MailItem.TYPE_NOTE,
            MailItem.TYPE_APPOINTMENT, MailItem.TYPE_WIKI, MailItem.TYPE_TASK,
            MailItem.TYPE_CHAT
        };
        TarOutputStream tos = null;
        String types = context.getTypesString();

        try {
            if (filename == null || filename.equals("")) {
                Date date = new Date();
                DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT);
                SimpleDateFormat sdf = new SimpleDateFormat(".H-m-s");

                filename = context.targetMailbox.getAccountId() + '.' +
                    df.format(date).replace('/', '-') + sdf.format(date);
            }
            filename += ".tgz";
            context.resp.addHeader("Content-Disposition", Part.ATTACHMENT +
                "; filename=" + HttpUtil.encodeFilename(context.req, filename));
            context.resp.setContentType("application/x-tar-compressed");
            if (types != null && !types.equals("")) {
                Arrays.sort(searchTypes = MailboxIndex.parseTypesString(types));
                sysTypes = new byte[0];
                if (Arrays.binarySearch(searchTypes, MailItem.TYPE_CONVERSATION) >= 0) {
                    int i = 0;
                    byte[] newTypes = new byte[searchTypes.length - 1];
                
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
                    ZimbraLog.misc.warn("%s: %s", e, e.getCause().toString());
                }
            } else if (context.target != null && !(context.target instanceof
                Folder)) {
                try {
                    tos = saveItem(context, context.target, fldrs, cnts, names,
                        false, tos);
                } catch (Exception e) {
                    ZimbraLog.misc.warn("%s: %s", e, e.getCause().toString());
                }
            } else {
                ZimbraQueryResults results = null;

                if (context.target instanceof Folder) {
                    Folder f = (Folder)context.target;
                    
                    if (f.getId() != Mailbox.ID_FOLDER_USER_ROOT)
                        query = "under:\"" + f.getPath() + "\"" +
                            (query == null ? "" : " " + query); 
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
                    conversations = true;
                    query = "is:local";
                }
                try {
                    results = context.targetMailbox.search(context.opContext,
                        query, searchTypes, MailboxIndex.SortBy.NONE, 4096);
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
                    ZimbraLog.misc.warn("%s: %s", e, e.getCause().toString());
                } finally {
                    if (results != null)
                        results.doneWithSearchResults();
                }
                if (tos == null)
                    throw new UserServletException(HttpServletResponse.
                        SC_NO_CONTENT, "No data found");
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
        Set<String> names, boolean version, TarOutputStream tos)
    throws ServiceException {
        String ext = null, name = null;
        Integer fid = mi.getFolderId();
        InputStream is = null;
        String fldr;
        
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
            ext = "appt";
            break;
        case MailItem.TYPE_CHAT:
            ext = "chat";
            break;
        case MailItem.TYPE_CONTACT:
            Contact ct = (Contact)mi;
            
            if ((name = ct.get(Contact.A_fullName)) == null) {
                String last = ct.get(Contact.A_lastName);

                if ((name = ct.get(Contact.A_firstName)) == null) {
                    if ((name = ct.get(Contact.A_nickname)) == null)
                        if ((name = last) == null)
                            name = ct.get(Contact.A_email);
                } else if ((name = ct.get(Contact.A_middleName)) == null) {
                    name = ct.get(Contact.A_firstName) + (last == null ? "" :
                        ' ' + ct.get(Contact.A_lastName));
                } else {
                    name = ct.get(Contact.A_firstName) + ' ' + name +
                        (last == null ? "" : ' ' + ct.get(Contact.A_lastName));
                }
            }
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
            
            fldr = f.getPath();
            if (fldr.startsWith("/"))
                fldr = fldr.substring(1);
            fldr = ILLEGAL_FOLDER_CHARS.matcher(fldr).replaceAll("_");
            fldrs.put(fid, fldr);
            cnts.put(fid, 1);
        } else if (!(mi instanceof Folder)) {
            final int BATCH = 500;
            int cnt = cnts.get(fid) + 1;
            
            cnts.put(fid, cnt);
            cnt /= BATCH;
            if (cnt > 0)
                fldr = fldr + '!' + cnt + '/'; 
        }
        try {
            String path = getEntryName(mi, fldr, name, ext, names);
            TarEntry entry = new TarEntry(path + ".meta");
            byte[] meta = new ItemData(mi).encode();
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
            entry.setMinorDeviceId(mi.getId());
            entry.setModTime(mi.getChangeDate());
            entry.setSize(meta.length);
            if (tos == null) {
                String charset = context.params.get("charset");
                
                tos = new TarOutputStream(new GZIPOutputStream(
                    context.resp.getOutputStream()), charset == null ? "UTF-8" :
                    charset);
            }
            tos.setLongFileMode(TarOutputStream.LONGFILE_GNU);
            if (shouldReturnMeta(context)) {
                tos.putNextEntry(entry);
                tos.write(meta);
                tos.closeEntry();
            }
            if (is != null) {
                entry.setName(path);
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
                    if (remain != 0)
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
                } else {
                    // Read headers into memory, since we need to write the size first.
                    byte[] headerData = HeadersOnlyInputStream.getHeaders(is);
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
    
    /**
     * Returns <tt>true</tt> if {@link UserServlet#QP_META} is not
     * specified or set to a non-zero value.
     */
    static boolean shouldReturnMeta(Context context) {
        String bodyVal = context.params.get(UserServlet.QP_META);
        if (bodyVal != null && bodyVal.equals("0")) {
            return false;
        }
        return true;
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
            int[] ids = null;
            if (context.reqListIds != null) {
                ids = context.reqListIds.clone();
                Arrays.sort(ids);
            }

            Resolve r = resolve == null ? Resolve.Skip : Resolve.valueOf(
                resolve.substring(0,1).toUpperCase() +
                resolve.substring(1).toLowerCase());

            long interval = 45 * 1000;
            if (timeout != null)
                interval = Long.parseLong(timeout);

            byte[] searchTypes = null;
            if (types != null && !types.equals("")) {
                Arrays.sort(searchTypes = MailboxIndex.parseTypesString(types));
                for (byte type : searchTypes) {
                    if (type == MailItem.TYPE_CONVERSATION) {
                        byte[] newTypes = new byte[types.length() - 1];
    
                        for (byte t : searchTypes)
                            if (t != MailItem.TYPE_CONVERSATION)
                                newTypes[newTypes.length] = t;
                        searchTypes = newTypes;
                        break;
                    }
                }
            }

            TarInputStream tis = new TarInputStream(new GZIPInputStream(
                context.getRequestInputStream(-1)), charset == null ? "UTF-8" : charset);

            List<Folder> flist = fldr.getSubfolderHierarchy();
            if (subfolder != null && !subfolder.equals("")) {
                fldr = createPath(context, fmap, fldr.getPath() + subfolder, Folder.TYPE_UNKNOWN);
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
        
                        int[] delIdsArray = new int[delIds.size()];
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
                while ((te = tis.getNextEntry()) != null) {
                    if (System.currentTimeMillis() - last > interval) {
                        updateClient(context, true);
                        last = System.currentTimeMillis();
                    }
                    if (te.getName().endsWith(".meta")) {
                        if (id != null)
                            recoverItem(context, fldr, fmap, digestMap, idMap,
                                ids, searchTypes, r, id, tis, null, errs);
                        id = new ItemData(readTarEntry(tis, te));
                        continue;
                    }
                    if (id == null) {
                        addError(errs, te.getName(), "item content missing meta");
                    } else if (id.ud.type != te.getMajorDeviceId() ||
                        id.ud.id != te.getMinorDeviceId() ||
                        (id.ud.getBlobDigest() != null && id.ud.size !=
                        te.getSize())) {
                        addError(errs, te.getName(),
                            "mismatched item content and meta");
                    } else {
                        recoverItem(context, fldr, fmap, digestMap, idMap, ids,
                            searchTypes, r, id, tis, te, errs);
                    }
                    id = null;
                }
                if (id != null)
                    recoverItem(context, fldr, fmap, digestMap, idMap, ids,
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

    public static byte[] readTarEntry(TarInputStream tis, TarEntry te) throws IOException {
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

    private void recoverItem(Context context, Folder fldr,
        Map<Object, Folder> fmap, Map<String, Integer> digestMap,
        Map<Integer, Integer> idMap, int[] ids, byte[] searchTypes, Resolve r,
        ItemData id, TarInputStream tis, TarEntry te, StringBuffer errs) throws
        MessagingException, ServiceException {
        try {
            Mailbox mbox = fldr.getMailbox();
            MailItem mi = MailItem.constructItem(mbox,id.ud);
            MailItem newItem = null, oldItem = null;
            OperationContext oc = context.opContext;
            String path;
            ParsedMessage pm;
            boolean root = fldr.getId() == Mailbox.ID_FOLDER_ROOT ||
                fldr.getId() == Mailbox.ID_FOLDER_USER_ROOT;
    
            if ((ids != null && Arrays.binarySearch(ids, id.ud.id) < 0) ||
                (searchTypes != null && Arrays.binarySearch(searchTypes,
                id.ud.type) < 0))
                return;
            if (id.ud.getBlobDigest() != null && te == null)
                addError(errs, id.path, "missing item blob for meta");
            if (root)
                path = id.path;
            else if (id.path.equals("/"))
                path = fldr.getPath();
            else
                path = fldr.getPath() + id.path;
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
                            ci.getAllReplies(), ad == null ? 0 : ad.getNextAt());
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
                                String(readTarEntry(tis, te), "UTF-8"));
                    }
                    break;
                }
                if (oldItem == null) {
                    newItem = mbox.createNote(oc, new String(readTarEntry(tis, te),
                        "UTF-8"), note.getBounds(), note.getColor(), fldr.getId());
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
}
