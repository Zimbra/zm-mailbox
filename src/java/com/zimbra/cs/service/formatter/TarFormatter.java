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

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
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
import javax.servlet.http.HttpServletResponse;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.HttpUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.util.tar.*;

import org.apache.commons.fileupload.DiskFileUpload;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadBase;

import com.zimbra.cs.index.MailboxIndex;
import com.zimbra.cs.index.ZimbraQueryResults;
import com.zimbra.cs.mailbox.CalendarItem;
import com.zimbra.cs.mailbox.Chat;
import com.zimbra.cs.mailbox.Appointment;
import com.zimbra.cs.mailbox.Contact;
import com.zimbra.cs.mailbox.Conversation;
import com.zimbra.cs.mailbox.Document;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.IncomingBlob;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailItem;
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
import com.zimbra.cs.service.UserServletException;
import com.zimbra.cs.service.UserServlet.Context;
import com.zimbra.cs.service.util.ItemData;

public class TarFormatter extends Formatter {
    private Pattern ILLEGAL_CHARS = Pattern.compile("[\\/\\:\\*\\?\\\"\\<\\>\\|]");
    private static enum Resolve { Modify, Replace, Reset, Skip }

    private static final class SortPath implements Comparator<MailItem> {
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
        ServiceException {
        HashMap<Integer, Integer> cnts = new HashMap<Integer, Integer>();
        String charset = context.params.get("charset");
        boolean conversations = false;
        Date date = new Date();
        DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT);
        SimpleDateFormat sdf = new SimpleDateFormat(".H-m-s");
        HashMap<Integer, String> fldrs = new HashMap<Integer, String>();
        String fname = context.params.get("name");
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
        String types = context.getTypesString();
        TarOutputStream tos = new TarOutputStream(new GZIPOutputStream(
            context.resp.getOutputStream()), charset == null ? "UTF-8" : charset);

        if (fname == null || fname.equals(""))
            fname = context.targetMailbox.getAccountId();
        fname += '.' + df.format(date).replace('/', '-') + sdf.format(date) + ".tgz";
        context.resp.addHeader("Content-Disposition", Part.ATTACHMENT +
            "; filename=" + HttpUtil.encodeFilename(context.req, fname));
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
        tos.setLongFileMode(TarOutputStream.LONGFILE_GNU);
        try {
            if (context.respListItems != null) {
                for (MailItem mi : context.respListItems)
                    saveItem(context, mi, fldrs, cnts, names, false, tos);
            } else if (context.target != null && !(context.target instanceof
                Folder)) {
                saveItem(context, context.target, fldrs, cnts, names, false,
                    tos);
            } else {
                MailboxLock ml = null;
                ZimbraQueryResults results = null;

                try {
                    if (context.target instanceof Folder) {
                        Folder f = (Folder)context.target;
                        
                        if (f.getId() != Mailbox.ID_FOLDER_USER_ROOT)
                            query = "in:" + f.getPath() + (query == null ? "" :
                                " " + query); 
                    }
                    if (query == null || query.equals("")) {
                        String lock = context.params.get("lock");
                        SortPath sp = new SortPath();
                        
                        if (lock != null && (lock.equals("1") ||
                            lock.equals("t") || lock.equals("true")))
                            ml = MailboxManager.getInstance().beginMaintenance(
                                context.targetMailbox.getAccountId(),
                                context.targetMailbox.getId());
                        for (byte type : sysTypes) {
                            List<MailItem> items =
                                context.targetMailbox.getItemList(
                                context.opContext, type);
                            
                            Collections.sort(items, sp);
                            for (MailItem item : items)
                                saveItem(context, item, fldrs, cnts, names,
                                    false, tos);
                        }
                        conversations = true;
                        query = "is:any";
                    }
                    results = context.targetMailbox.search(context.opContext,
                        query, searchTypes, MailboxIndex.SortBy.NONE, 4096);
                    while (results.hasNext())
                        saveItem(context, results.getNext().getMailItem(),
                            fldrs, cnts, names, false, tos);
                    if (conversations)
                        for (MailItem item : context.targetMailbox.getItemList(
                            context.opContext, MailItem.TYPE_CONVERSATION)) 
                            saveItem(context, item, fldrs, cnts, names,
                                false, tos);
                } catch (ServiceException e) {
                    throw e;
                } catch (Exception e) {
                    throw ServiceException.FAILURE("search error", e);
                } finally {
                    if (results != null)
                        results.doneWithSearchResults();
                    if (ml != null)
                        MailboxManager.getInstance().endMaintenance(ml, true,
                            true);
                }
            }
        } finally {
            tos.close();
        }
    }
    
    private void saveItem(Context context, MailItem mi, 
        HashMap<Integer, String> fldrs, HashMap<Integer, Integer> cnts,
        Set<String> names, boolean version, TarOutputStream tos) throws
        ServiceException {
        String ext = null, name = null;
        Integer fid = mi.getFolderId();
        String fldr;
        
        if (!version && mi.isTagged(mi.getMailbox().mVersionedFlag)) {
            for (MailItem rev : context.targetMailbox.getAllRevisions(
                context.opContext, mi.getId(), mi.getType())) {
                if (mi.getVersion() != rev.getVersion())
                    saveItem(context, rev, fldrs, cnts, names, true, tos);
            }
        }
        switch (mi.getType()) {
        case MailItem.TYPE_APPOINTMENT:
            Appointment appt = (Appointment)mi;
            
            if (!appt.isPublic() && !appt.allowPrivateAccess(context.authAccount,
                context.isUsingAdminPrivileges()))
                return;
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
            return;
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
                return;
            ext = "task";
            break;
        case MailItem.TYPE_VIRTUAL_CONVERSATION:
            return;
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
            fldr = ILLEGAL_CHARS.matcher(fldr).replaceAll("_");
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
            InputStream is = mi.getContentStream();
            String path = getEntryName(mi, fldr, name, ext, names);
            TarEntry entry = new TarEntry(path + ".meta");
            byte[] meta = new ItemData(mi).encode();
            
            entry.setGroupName(MailItem.getNameForType(mi));
            entry.setMajorDeviceId(mi.getType());
            entry.setMinorDeviceId(mi.getId());
            entry.setModTime(mi.getChangeDate());
            entry.setSize(meta.length);
            tos.putNextEntry(entry);
            tos.write(meta);
            tos.closeEntry();
            if (is != null) {
                byte buf[] = new byte[tos.getRecordSize() * 20];
                int in;

                entry.setName(path);
                entry.setSize(mi.getSize());
                tos.putNextEntry(entry);
                while ((in = is.read(buf)) >= 0)
                    tos.write(buf, 0, in);
                is.close();
                tos.closeEntry();
            }
        } catch (Exception e) {
            throw ServiceException.FAILURE("archive error", e);
        }
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
        name = ILLEGAL_CHARS.matcher(name).replaceAll("_").trim();
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
    
    private PrintWriter update(Context context, PrintWriter pw, boolean flush)
        throws IOException {
        if (pw == null) {
            if (flush)
                context.resp.setBufferSize(0);
            context.resp.setContentType("text/html");
            pw = context.resp.getWriter();
            pw.print("<html>\n<head>\n</head>\n");
        } else {
            pw.println();
        }
        if (flush)
            pw.flush();
        return pw;
    }

    @SuppressWarnings("unchecked")
    public void saveCallback(Context context, String contentType, Folder fldr,
        String file) throws IOException, ServiceException, UserServletException {
        ItemData id = null;
        String callback = context.params.get("callback");
        String charset = context.params.get("charset");
        StringBuffer errs = new StringBuffer();
        String errstr = "";
        FileItem fi = null;
        List<Folder> flist = fldr.getSubfolderHierarchy();
        Map<Object, Folder> fmap = new HashMap<Object, Folder>();
        Map<Integer, Integer> idMap = new HashMap<Integer, Integer>();
        int[] ids = null;
        long interval = 45 * 1000;
        InputStream is = null;
        long last = System.currentTimeMillis();
        PrintWriter pw = null;
        Resolve r;
        String resolve = context.params.get("resolve");
        String subfolder = context.params.get("subfolder");
        TarEntry te;
        String timeout = context.params.get("timeout");
        TarInputStream tis = null;
        String types = context.getTypesString();
        byte[] searchTypes = null;

        try {
            if (context.reqListIds != null) {
                ids = context.reqListIds.clone();
                Arrays.sort(ids);
            }
            r = resolve == null ? Resolve.Skip : Resolve.valueOf(
                resolve.substring(0,1).toUpperCase() +
                resolve.substring(1).toLowerCase());
            if (timeout != null)
                interval = Long.parseLong(timeout);
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
            if (FileUploadBase.isMultipartContent(context.req)) {
                DiskFileUpload dku = new DiskFileUpload();
                List<FileItem> files;

                dku.setSizeThreshold(1024 * 1024);
                dku.setRepositoryPath(System.getProperty("java.io.tmpdir",
                    "/tmp"));
                try {
                    files = dku.parseRequest(context.req);
                } catch (Exception e) {
                    throw new UserServletException(HttpServletResponse.
                        SC_UNSUPPORTED_MEDIA_TYPE, e.toString());
                }
                for (Iterator<FileItem> it = files.iterator(); it.hasNext(); ) {
                    fi = it.next();
                    if (fi != null && !fi.isFormField()) {
                        is = fi.getInputStream();
                        break;
                    }
                }
                if (is == null)
                    throw new UserServletException(HttpServletResponse.
                        SC_NO_CONTENT, "No file content");
            } else {
                is = context.req.getInputStream();
            }
            tis = new TarInputStream(new GZIPInputStream(is), charset == null ?
                "UTF-8" : charset);
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
                            pw = update(context, pw, true);
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
                while ((te = tis.getNextEntry()) != null) {
                    if (System.currentTimeMillis() - last > interval) {
                        pw = update(context, pw, true);
                        last = System.currentTimeMillis();
                    }
                    if (te.getName().endsWith(".meta")) {
                        if (id != null)
                            recoverItem(context, fldr, fmap, idMap, ids,
                                searchTypes, r, id, tis, null, errs);
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
                        recoverItem(context, fldr, fmap, idMap, ids,
                            searchTypes, r, id, tis, te, errs);
                    }
                    id = null;
                }
                if (id != null)
                    recoverItem(context, fldr, fmap, idMap, ids, searchTypes, r,
                        id, tis, null, errs);
            } catch (Exception e) {
                addError(errs, id == null ? null : id.path,
                    e.getLocalizedMessage());
                id = null;
            } finally {
                if (tis != null)
                    tis.close();
                if (fi != null)
                    fi.delete();
            }
        } catch (Exception e) {
            if (pw != null)
                addError(errs, null, e.getLocalizedMessage());
            else if (e instanceof IOException)
                throw (IOException)e;
            else if (e instanceof ServiceException)
                throw (ServiceException)e;
            else if (e instanceof UserServletException)
                throw (UserServletException)e;
            else
                throw ServiceException.FAILURE("Tar formatter failure", e);
        }
        if (errs.length() > 0) {
            if (errs.indexOf("\n") == -1)
                errstr = "Import error: " + errs;
            else
                errstr = "Import errors:\n" + errs;
            ZimbraLog.misc.warn(errstr);
        }
        if (callback == null) {
            if (pw == null) {
                if (errstr.length() == 0)
                    return;
		throw new UserServletException(HttpServletResponse.SC_CONFLICT,
		    errstr);
            }
            pw.print("<body>\n<pre>\n" + errstr + "\n</pre>\n</body>\n</html>\n");
        } else {
            errstr = errs.substring(0, errs.length() > 2048 ? 2048 :
                errs.length());
            errstr = errstr.replace('\'', '\"');
            errstr = errstr.replace("\\", "\\\\");
            errstr = errstr.replace("\n", "\\n");
            pw = update(context, pw, false);
            pw.print("<body onload=\"window.parent." + callback + "('" +
                errstr + "');\">\n</body>\n</html>\n");
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
    
    private String string(String s) { return s == null ? new String() : s; }

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
    
    private void recoverItem(Context context, Folder fldr,
        Map<Object, Folder> fmap, Map<Integer, Integer> idMap, int[] ids,
        byte[] searchTypes, Resolve r, ItemData id, TarInputStream tis,
        TarEntry te, StringBuffer errs) throws MessagingException, ServiceException {
        try {
            boolean newTags = true;
            Mailbox mbox = fldr.getMailbox();
            MailItem mi = MailItem.constructItem(mbox,id.ud);
            MailItem newItem = null, oldItem = null;
            OperationContext oc = context.opContext;
            Integer oldId = idMap.get(mi.getId());
            String path;
            boolean root = fldr.getId() == Mailbox.ID_FOLDER_ROOT ||
                fldr.getId() == Mailbox.ID_FOLDER_USER_ROOT;
    
            if ((ids != null && Arrays.binarySearch(ids, id.ud.id) < 0) ||
                (searchTypes != null && Arrays.binarySearch(searchTypes,
                id.ud.type) < 0))
                return;
            // allow contacts... w/ missing blob
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
                    try {
                        CalendarItem oldCI = mbox.getCalendarItemByUid(oc,
                            ci.getUid());
                    
                        if (r == Resolve.Replace)
                            mbox.delete(oc, oldCI.getId(), oldCI.getType());
                        else
                            oldItem = oldCI;
                    } catch (Exception e) {
                    }
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
                ParsedMessage pm = new ParsedMessage(readTarEntry(tis, te),
                    mbox.attachmentsIndexingEnabled());
                
                fldr = createPath(context, fmap, path, Folder.TYPE_CHAT);
                if (root && r != Resolve.Reset) {
                    try {
                        Chat oldChat = mbox.getChatById(oc, chat.getId());
                    
                        if (chat.getSender().equals(oldChat.getSender()) &&
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
                    } catch (Exception e) {
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
                    try {
                        Contact oldct = mbox.getContactById(oc, ct.getId());
                        String email = string(ct.get(Contact.A_email));
                        String first = string(ct.get(Contact.A_firstName));
                        String name = string(ct.get(Contact.A_fullName));
                        String oldemail = string(oldct.get(Contact.A_email));
                        String oldfirst = string(oldct.get(Contact.A_firstName));
                        String oldname = string(oldct.get(Contact.A_fullName));
                        
                        if (email.equals(oldemail) && first.equals(oldfirst) &&
                            name.equals(oldname)) {
                            if (r == Resolve.Replace) {
                                mbox.delete(oc, oldct.getId(), oldct.getType());
                            } else {
                                oldItem = oldct;
                                if (r == Resolve.Modify)
                                    mbox.modifyContact(oc, oldItem.getId(),
                                        new ParsedContact(ct.getFields(),
                                        readTarEntry(tis, te)));
                            }
                        }
                    } catch (Exception e) {
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
                
                fldr = createParent(context, fmap, path, doc.getType() ==
                    MailItem.TYPE_DOCUMENT ? Folder.TYPE_DOCUMENT :
                    Folder.TYPE_WIKI);
                newTags = false;
                if (oldId != null) {
                    try {
                        oldItem = mbox.getDocumentById(oc, oldId);
                        if (oldItem != null) {
                            if (doc.getVersion() > oldItem.getVersion()) {
                                newItem = mbox.addDocumentRevision(oc, oldId,
                                    doc.getType(), tis, doc.getCreator(), doc.getName());
                                mbox.setDate(oc, newItem.getId(), doc.getType(),
                                    doc.getDate());
                            } else if (doc.getVersion() == oldItem.getVersion()) {
                                mbox.setDate(oc, oldItem.getId(), doc.getType(),
                                    doc.getDate());
                            } else {
                                return;
                            }
                            break;
                        }
                    } catch (Exception e) {
                    }
                }
                try {
                    for (Document oldDoc : mbox.getDocumentList(oc, fldr.getId())) {
                        if (doc.getName().equals(oldDoc.getName())) {
                            if (r == Resolve.Replace && oldId == null) {
                                mbox.delete(oc, oldDoc.getId(), oldDoc.getType());
                            } else if (doc.getVersion() < oldDoc.getVersion()) {
                                return;
                            } else {
                                idMap.put(doc.getId(), oldDoc.getId());
                                oldItem = oldDoc;
                                if (doc.getVersion() > oldDoc.getVersion())
                                    newItem = mbox.addDocumentRevision(oc,
                                        oldDoc.getId(), doc.getType(), tis,
                                        doc.getCreator(), doc.getName());
                            }
                            break;
                        }
                    }
                } catch (Exception e) {
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
                byte view = f.getDefaultView();
                
                if (view == MailItem.TYPE_CONVERSATION ||
                    view == MailItem.TYPE_FLAG || view == MailItem.TYPE_TAG)
                    break;
                try {
                    Folder oldF = mbox.getFolderByPath(oc, path);
                
                    oldItem = oldF;
                    if (r != Resolve.Skip) {
                        if (!f.getUrl().equals(oldF.getUrl()))
                            mbox.setFolderUrl(oc, oldF.getId(), f.getUrl());
                        if (!f.getEffectiveACL().toString().equals(
                            oldF.getEffectiveACL().toString()))
                            mbox.setPermissions(oc, oldF.getId(),
                                f.getEffectiveACL());
                    }
                } catch (Exception e) {
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
                
                fldr = createPath(context, fmap, path, Folder.TYPE_MESSAGE);
                if (root && r != Resolve.Reset) {
                    try {
                        Message oldMsg = mbox.getMessageById(oc, msg.getId());
                    
                        if (msg.getDigest().equals(oldMsg.getDigest())) {
                            if (r == Resolve.Replace)
                                mbox.delete(oc, oldMsg.getId(), oldMsg.getType());
                            else
                                oldItem = oldMsg;
                        }
                    } catch (Exception e) {
                    }
                }
                if (oldItem == null) {
                    IncomingBlob ib = IncomingBlob.create(tis, (int)te.getSize(),
                        2 * 1024 * 1024);
                    
                    try {
                        newItem = mbox.addMessage(oc, ib.createParsedMessage(
                            msg.getDate(), mbox.attachmentsIndexingEnabled()),
                            fldr.getId(), true, msg.getFlagBitmask(),
                            msg.getTagString());
                    } finally {
                        ib.delete();
                    }
                }
                break;
            case MailItem.TYPE_MOUNTPOINT:
                Mountpoint mp = (Mountpoint)mi;
                
                try {
                    oldItem = mbox.getItemByPath(oc, path);
                    
                    if (oldItem.getType() == mi.getType()) {
                        if (r == Resolve.Modify || r == Resolve.Replace)
                            mbox.delete(oc, oldItem.getId(), oldItem.getType());
                    } else {
                        oldItem = null;
                    }
                } catch (Exception e) {
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
    
                fldr = createPath(context, fmap, path, Folder.TYPE_NOTE);
                try {
                    for (Note oldNote : mbox.getNoteList(oc, fldr.getId())) {
                        if (note.getSubject().equals(oldNote.getSubject())) {
                            if (r == Resolve.Replace) {
                                mbox.delete(oc, oldNote.getId(),
                                    oldNote.getType());
                            } else {
                                oldItem = oldNote;
                                if (r == Resolve.Modify)
                                    mbox.editNote(oc, oldItem.getId(), new
                                        String(readTarEntry(tis, te), "UTF-8"));
                            }
                            break;
                        }
                    }
                } catch (Exception e) {
                }
                if (oldItem == null) {
                    newItem = mbox.createNote(oc, new String(readTarEntry(tis, te),
                        "UTF-8"), note.getBounds(), note.getColor(), fldr.getId());
                    newTags = false;
                }
                break;
            case MailItem.TYPE_SEARCHFOLDER:
                SearchFolder sf = (SearchFolder)mi;
                
                try {
                    oldItem = mbox.getItemByPath(oc, path);
                    
                    if (oldItem.getType() == mi.getType()) {
                        if (r == Resolve.Modify) {
                            mbox.modifySearchFolder(oc, oldItem.getId(),
                                sf.getQuery(), sf.getReturnTypes(),
                                sf.getSortField());
                        } else if (r == Resolve.Replace) {
                            mbox.delete(oc, oldItem.getId(), oldItem.getType());
                            oldItem = null;
                        }
                    } else {
                        oldItem = null;
                    }
                } catch (Exception e) {
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
                if (!newTags && (!id.flags.equals(newItem.getFlagString()) ||
                    !id.tags.equals(newItem.getTagString())))
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
