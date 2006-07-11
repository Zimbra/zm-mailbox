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

package com.zimbra.cs.zclient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.TreeMap;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.zimbra.cs.account.GalContact;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.servlet.ZimbraServlet;
import com.zimbra.cs.util.StringUtil;
import com.zimbra.cs.util.Zimbra;
import com.zimbra.cs.zclient.ZConversation.ZMessageSummary;
import com.zimbra.cs.zclient.ZMailbox.OwnerBy;
import com.zimbra.cs.zclient.ZMailbox.SharedItemBy;
import com.zimbra.cs.zclient.ZMailbox.SortBy;
import com.zimbra.cs.zclient.ZSearchParams.Cursor;
import com.zimbra.cs.zclient.ZTag.Color;
import com.zimbra.cs.zclient.soap.ZSoapMailbox;
import com.zimbra.soap.Element;
import com.zimbra.soap.SoapFaultException;
import com.zimbra.soap.SoapTransport.DebugListener;

/**
 * @author schemers
 */
public class ZMailboxUtil implements DebugListener {
 
    private boolean mInteractive = false;
    private boolean mVerbose = false;
    private boolean mDebug = false;
    private String mAccount = null;
    private String mPassword = null;
    private String mUrl = "http://localhost";
    private Command mCommand;
    
    public void setDebug(boolean debug) { mDebug = debug; }
    
    public void setVerbose(boolean verbose) { mVerbose = verbose; }
    
    public void setAccount(String account) { mAccount = account; }

    public void setPassword(String password) { mPassword = password; }
    
    public void setUrl(String url) throws SoapFaultException { 
        try {
            URI uri = new URI(url);
            if (uri.getPath() == null || uri.getPath().length() <= 1) {
                if (url.charAt(url.length()-1) == '/') 
                    url = url.substring(0, url.length()-1) + ZimbraServlet.USER_SERVICE_URI;
                else 
                    url = url + ZimbraServlet.USER_SERVICE_URI;                
            }
            mUrl = url;
        } catch (URISyntaxException e) {
            throw SoapFaultException.CLIENT_ERROR("invlaid URL: "+url, e);
        }
    }

    private void usage() {
        
        if (mCommand != null) {
            System.out.printf("usage:  %s(%s) %s\n", mCommand.getName(), mCommand.getAlias(), mCommand.getHelp());
        }

        if (mInteractive)
            return;
        
        System.out.println("");
        System.out.println("zmmailbox [args] [cmd] [cmd-args ...]");
        System.out.println("");
        System.out.println("  -h/--help                                display usage");
        System.out.println("  -u/--url      http[s]://{host}[:{port}]  server hostname and optional port");
        System.out.println("  -a/--account  {name}                     account name to auth as");
        System.out.println("  -p/--password {pass}                     password for account");
        System.out.println("  -v/--verbose                             verbose mode (dumps full exception stack trace)");
        System.out.println("  -d/--debug                               debug mode (dumps SOAP messages)");
        System.out.println("");
        doHelp(null);
        System.exit(1);
    }

    public static enum Category {

        COMMANDS("help on all commands"),
        CONTACT("help on contact-related commands"),
        CONVERSATION("help on conversation-related commands"),
        FOLDER("help on folder-related commands"),
        ITEM("help on item-related commands"),
        MESSAGE("help on message-related commands"),
        MISC("help on misc commands"), 
        SEARCH("help on search-related commands"),         
        TAG("help on tag-related commands");        

        String mDesc;

        public String getDescription() { return mDesc; }
        
        Category(String desc) {
            mDesc = desc;
        }
    }
    
    enum Command {
        
        CREATE_FOLDER("createFolder", "cf", "{folder-name} [{default-view}]", Category.FOLDER, 1, 2),
        CREATE_MOUNTPOINT("createMountpoint", "cm", "{folder-name} {owner-id-or-name} {remote-item-id-or-path} {default-view}", Category.FOLDER, 4, 4),
        CREATE_SEARCH_FOLDER("createSearchFolder", "csf", "{folder-name} {query} [{types}] [{sort-by}]", Category.FOLDER, 2, 4),        
        CREATE_TAG("createTag", "ct", "{tag-name} {tag-color}", Category.TAG, 2, 2),
        DELETE_CONVERSATION("deleteConversation", "dc", "{conv-ids} [{tcon}]", Category.CONVERSATION, 1, 2),
        DELETE_ITEM("deleteItem", "di", "{item-ids} [{tcon}]", Category.ITEM, 1, 2),        
        DELETE_FOLDER("deleteFolder", "df", "{folder-path}", Category.FOLDER, 1, 1),
        DELETE_MESSAGE("deleteMessage", "dm", "{msg-ids}", Category.MESSAGE, 1, 1),
        DELETE_TAG("deleteTag", "dt", "{tag-name}", Category.TAG, 1, 1),
        EMPTY_FOLDER("emptyFolder", "ef", "{folder-path}", Category.FOLDER, 1, 1),        
        EXIT("exit", "quit", "", Category.MISC, 0, 0),
        FLAG_CONVERSATION("flagConversation", "fc", "{conv-ids} [0|1*] [{tcon}]", Category.CONVERSATION, 1, 3),
        FLAG_ITEM("flagItem", "fi", "{item-ids} [0|1*] [{tcon}]", Category.ITEM, 1, 3),
        FLAG_MESSAGE("flagMessage", "fm", "{msg-ids} [0|1*]", Category.MESSAGE, 1, 2),
        GET_ALL_TAGS("getAllTags", "gat", "[-v]", Category.TAG, 0, 1),        
        GET_ALL_FOLDERS("getAllFolders", "gaf", "[-v]", Category.FOLDER, 0, 1),
        GET_ALL_MOUNTPOINTS("getAllMountpoints", "gam", "[-v]", Category.FOLDER, 0, 1),
        GET_CONVERSATION("getConversation", "gc", "{conv-id}", Category.CONVERSATION, 1, 2),
        GET_MESSAGE("getMessage", "gm", "{msg-id}", Category.MESSAGE, 1, 1),        
        HELP("help", "?", "commands", Category.MISC, 0, 1),
        IMPORT_URL_INTO_FOLDER("importURLIntoFolder", "iuif", "{folder-path} {url}", Category.FOLDER, 2, 2),
        MARK_CONVERSATION_READ("markConversationRead", "mcr", "{conv-ids} [0|1*] [{tcon}]", Category.CONVERSATION, 1, 3),
        MARK_CONVERSATION_SPAM("markConversationSpam", "mcs", "{conv-ids} [0|1*] [{dest-folder-path}] [{tcon}]", Category.CONVERSATION, 1, 4),
        MARK_ITEM_READ("markItemRead", "mir", "{item-ids} [0|1*] [{tcon}]", Category.ITEM, 1, 3),
        MARK_FOLDER_READ("markFolderRead", "mfr", "{folder-path}", Category.FOLDER, 1, 1),        
        MARK_MESSAGE_READ("markMessageRead", "mmr", "{msg-ids} [0|1*]", Category.MESSAGE, 1, 2),
        MARK_MESSAGE_SPAM("markMessageSpam", "mms", "{msg-ids} [0|1*] [{dest-folder-path}]", Category.MESSAGE, 1, 3),
        MARK_TAG_READ("markTagRead", "mtr", "{tag-name}", Category.TAG, 1, 1),
        MODIFY_FOLDER_CHECKED("modifyFolderChecked", "mfch", "{folder-path} [0|1*]", Category.FOLDER, 1, 2),
        MODIFY_FOLDER_COLOR("modifyFolderColor", "mfc", "{folder-path} {new-color}", Category.FOLDER, 2, 2),
        MODIFY_FOLDER_EXCLUDE_FREE_BUSY("modifyFolderExcludeFreeBusy", "mfefb", "{folder-path} [0|1*]", Category.FOLDER, 1, 2),        
        MODIFY_FOLDER_URL("modifyFolderURL", "mfu", "{folder-path} {url}", Category.FOLDER, 2, 2),                
        MODIFY_TAG_COLOR("modifyTagColor", "mtc", "{tag-name} {tag-color}", Category.TAG, 2, 2),
        MOVE_CONVERSATION("moveConversation", "mc", "{conv-ids} {dest-folder-path} [{tcon}]", Category.CONVERSATION, 2, 3),
        MOVE_ITEM("moveItem", "mi", "{item-ids} {dest-folder-path} [{tcon}]", Category.ITEM, 1, 2),        
        MOVE_MESSAGE("moveMessage", "mm", "{msg-ids} {dest-folder-path}", Category.MESSAGE, 2, 2),
        NOOP("noOp", "no", "", Category.MISC, 0, 0),
        RENAME_FOLDER("renameFolder", "rf", "{folder-path} {new-folder-path}", Category.FOLDER, 2, 2),        
        RENAME_TAG("renameTag", "rt", "{tag-name} {new-tag-name}", Category.TAG, 2, 2),
        SEARCH("search", "s", "{query}", Category.SEARCH, 1, 1),
        SEARCH_CURRENT("searchCurrent", "sc", "", Category.SEARCH, 0, 0),        
        SEARCH_NEXT("searchNext", "sn", "", Category.SEARCH, 0, 0),
        SEARCH_PREVIOUS("searchPrevious", "sp", "", Category.SEARCH, 0, 0),        
        SYNC_FOLDER("syncFolder", "sf", "{folder-path}", Category.FOLDER, 1, 1),                        
        TAG_CONVERSATION("tagConversation", "tc", "{conv-ids} {tag-name} [0|1*] [{tcon}]", Category.CONVERSATION, 2, 4),
        TAG_ITEM("tagItem", "ti", "{item-ids} {tag-name} [0|1*] [{tcon}]", Category.ITEM, 2, 4),
        TAG_MESSAGE("tagMessage", "tm", "{msg-ids} {tag-name} [0|1*]", Category.MESSAGE, 2, 3);

        private String mName;
        private String mAlias;
        private String mHelp;
        private Category mCat;
        private int mMinArgLength = 0;
        private int mMaxArgLength = Integer.MAX_VALUE;

        public String getName() { return mName; }
        public String getAlias() { return mAlias; }
        public String getHelp() { return mHelp; }
        public Category getCategory() { return mCat; }
        public boolean hasHelp() { return mHelp != null; }
        public boolean checkArgsLength(String args[]) {
            int len = args == null ? 0 : args.length - 1;
            return len >= mMinArgLength && len <= mMaxArgLength;
        }

        private Command(String name, String alias) {
            mName = name;
            mAlias = alias;
        }

        private Command(String name, String alias, String help, Category cat)  {
            mName = name;
            mAlias = alias;
            mHelp = help;
            mCat = cat;
        }

        private Command(String name, String alias, String help, Category cat, int minArgLength, int maxArgLength)  {
            mName = name;
            mAlias = alias;
            mHelp = help;
            mCat = cat;
            mMinArgLength = minArgLength;
            mMaxArgLength = maxArgLength;            
        }
        
    }
    
    private Map<String,Command> mCommandIndex;
    private ZMailbox mMbox;
    private String mPrompt = "mbox> ";
    ZSearchParams mSearchParams;
    ZSearchResult mSearchResult;
    
    private boolean isId(String value) {
        return (value.length() == 36 &&
                value.charAt(8) == '-' &&
                value.charAt(13) == '-' &&
                value.charAt(18) == '-' &&
                value.charAt(23) == '-');
    }
    
    private void addCommand(Command command) {
        String name = command.getName().toLowerCase();
        if (mCommandIndex.get(name) != null)
            throw new RuntimeException("duplicate command: "+name);
        
        String alias = command.getAlias().toLowerCase();
        if (mCommandIndex.get(alias) != null)
            throw new RuntimeException("duplicate command: "+alias);
        
        mCommandIndex.put(name, command);
        mCommandIndex.put(alias, command);
    }
    
    private void initCommands() {
        mCommandIndex = new HashMap<String, Command>();

        for (Command c : Command.values())
            addCommand(c);
    }
    
    private Command lookupCommand(String command) {
        return mCommandIndex.get(command.toLowerCase());
    }

    private ZMailboxUtil() {
        initCommands();
    }
    
    public void initMailbox() throws ServiceException, IOException {
        mMbox = ZSoapMailbox.getMailbox(mAccount, mPassword, mUrl, mDebug ? this : null);
        mPrompt = String.format("mbox %s> ", mAccount);
    }
    
    private ZTag lookupTag(String idOrName) throws SoapFaultException {
        ZTag tag = mMbox.getTagByName(idOrName);
        if (tag == null) tag = mMbox.getTagById(idOrName);
        if (tag == null) throw SoapFaultException.CLIENT_ERROR("unknown tag: "+idOrName, null);
        return tag;
    }
    
    /**
     * takes a list of ids or names, and trys to resolve them all to valid tag ids
     * 
     * @param idsOrNames
     * @return
     * @throws SoapFaultException
     */
    private String lookupTagIds(String idsOrNames) throws SoapFaultException {
        StringBuilder ids = new StringBuilder();
        for (String t : idsOrNames.split(",")) {
            ZTag tag = lookupTag(t);
            if (ids.length() > 0) ids.append(",");
            ids.append(tag.getId());
        }
        return ids.toString();
    }
    
    private String lookupFolderId(String pathOrId) throws ServiceException {
        return lookupFolderId(pathOrId, false);
    }

    private String translateId(String indexOrId) throws ServiceException {
        StringBuilder ids = new StringBuilder();
        for (String t : indexOrId.split(",")) {
            
            // TODO: REGEX? ^\d+s$
            if (t.length() > 1 && t.charAt(t.length()-1) == 's') {
                t = t.substring(0, t.length()-1);
                //System.out.println(t);                
                int i = t.indexOf('-');
                if (i != -1) {
                    int start = Integer.parseInt(t.substring(0, i));
                    String es = t.substring(i+1, t.length());
//                    System.out.println(es);
                    int end = Integer.parseInt(t.substring(i+1, t.length()));
                    for (int j = start; j <= end; j++) {
                        String id = mSearchIndexToId.get(j);
                        if (id == null) throw SoapFaultException.CLIENT_ERROR("unknown search result index: "+t, null);
                        if (ids.length() > 0) ids.append(",");                        
                        ids.append(id);
                    }
                } else {
                    String id = mSearchIndexToId.get(Integer.parseInt(t));
                    if (id == null) throw SoapFaultException.CLIENT_ERROR("unknown search result index: "+t, null);
                    if (ids.length() > 0) ids.append(",");                    
                    ids.append(id);
                }
            } else {
                if (ids.length() > 0) ids.append(",");                
                ids.append(t);
            }
        }
        return ids.toString();
    }

    private String lookupFolderId(String pathOrId, boolean parent) throws ServiceException {
        if (parent) pathOrId = ZMailbox.getParentPath(pathOrId);
        if (pathOrId == null || pathOrId.length() == 0) return null;
        ZFolder folder = mMbox.getFolderById(pathOrId);
        if (folder == null) folder = mMbox.getFolderByPath(pathOrId);
        if (folder == null) throw SoapFaultException.CLIENT_ERROR("unknown folder: "+pathOrId, null);
        return folder.getId();
    }
    
    private String param(String[] args, int index, String defaultValue) {
        return args.length > index ? args[index] : defaultValue;
    }
    
    private boolean paramb(String[] args, int index, boolean defaultValue) {
        return args.length > index ? args[index].equals("1") : defaultValue;
    }
    
    private String param(String[] args, int index) {
        return param(args, index, null);
    }
    
    private boolean execute(String args[]) throws ServiceException, ArgException, IOException {
        
        mCommand = lookupCommand(args[0]);
        
        if (mCommand == null)
            return false;
        
        if (!mCommand.checkArgsLength(args)) {
            usage();
            return true;
        }
        
        switch(mCommand) {
        case CREATE_FOLDER:
            mMbox.createFolder(
                    lookupFolderId(args[1], true), 
                    ZMailbox.getBasePath(args[1]), 
                    args.length == 3 ? ZFolder.View.fromString(args[2]) : null);
            break;
        case CREATE_MOUNTPOINT:
            String cmPath = args[1];
            String cmOwner = args[2];
            String cmItem = args[3];
            mMbox.createMountpoint(
                        lookupFolderId(cmPath, true), 
                        ZMailbox.getBasePath(cmPath),
                        ZFolder.View.fromString(args[4]),
                        (isId(cmOwner) ? OwnerBy.BY_ID : OwnerBy.BY_NAME),
                        cmOwner,
                        (isId(cmItem) ? SharedItemBy.BY_ID : SharedItemBy.BY_PATH),
                        cmItem);
            break;
        case CREATE_SEARCH_FOLDER:
            mMbox.createSearchFolder(
                    lookupFolderId(args[1], true), 
                    ZMailbox.getBasePath(args[1]),
                    args[2],
                    param(args, 3),
                    args.length == 5 ? SortBy.fromString(param(args, 4)) : null);
            break;
        case CREATE_TAG:
            mMbox.createTag(args[1], Color.fromString(args[2]));
            break;
        case DELETE_CONVERSATION:
            mMbox.deleteConversation(translateId(args[1]), param(args, 2));
            break;
        case DELETE_FOLDER:
            mMbox.deleteFolder(lookupFolderId(args[1]));
            break;            
        case DELETE_ITEM:
            mMbox.deleteItem(args[1], param(args, 2));
            break;            
        case DELETE_MESSAGE:
            mMbox.deleteMessage(args[1]);
            break;
        case DELETE_TAG:
            mMbox.deleteTag(lookupTag(args[1]).getId());
            break;
        case EMPTY_FOLDER:
            mMbox.emptyFolder(lookupFolderId(args[1]));
            break;                        
        case EXIT:
            System.exit(0);
            break;
        case FLAG_CONVERSATION:
            mMbox.flagConversation(translateId(args[1]), paramb(args, 2, true), param(args, 3));
            break;            
        case FLAG_ITEM:
            mMbox.flagItem(args[1], paramb(args, 2, true), param(args, 3));
            break;                        
        case FLAG_MESSAGE:
            mMbox.flagMessage(args[1], paramb(args, 2, true));
            break;            
        case GET_ALL_FOLDERS:
            doGetAllFolders(args); 
            break;
        case GET_ALL_MOUNTPOINTS:
            doGetAllMountpoints(args); 
            break;
        case GET_ALL_TAGS:
            doGetAllTags(args); 
            break;            
        case GET_CONVERSATION:
            doGetConversation(args);
            break;
        case GET_MESSAGE:
            doGetMessage(args);
            break;            
        case HELP:
            doHelp(args); 
            break;
        case IMPORT_URL_INTO_FOLDER:
            mMbox.importURLIntoFolder(lookupFolderId(args[1]), args[2]);
            break;
        case MARK_CONVERSATION_READ:
            mMbox.markConversationRead(translateId(args[1]), paramb(args, 2, true), param(args, 3));
            break;
        case MARK_ITEM_READ:
            mMbox.markItemRead(args[1], paramb(args, 2, true), param(args, 3));
            break;
        case MARK_FOLDER_READ:
            mMbox.markFolderRead(lookupFolderId(args[1]));
            break;                                    
        case MARK_MESSAGE_READ:
            mMbox.markMessageRead(args[1], paramb(args, 2, true));
            break;
        case MARK_CONVERSATION_SPAM:            
            mMbox.markConversationSpam(translateId(args[1]), paramb(args, 2, true), lookupFolderId(param(args, 3)), param(args, 4));
            break;            
        case MARK_MESSAGE_SPAM:            
            mMbox.markMessageSpam(args[1], paramb(args, 2, true), lookupFolderId(param(args, 3)));
            break;            
        case MARK_TAG_READ:
            mMbox.markTagRead(lookupTag(args[1]).getId());
            break;
        case MODIFY_FOLDER_CHECKED:
            mMbox.modifyFolderChecked(lookupFolderId(args[1]), paramb(args, 2, true));
            break;                        
        case MODIFY_FOLDER_COLOR:
            mMbox.modifyFolderColor(lookupFolderId(args[1]), ZFolder.Color.fromString(args[2]));
            break;                        
        case MODIFY_FOLDER_EXCLUDE_FREE_BUSY:
            mMbox.modifyFolderExcludeFreeBusy(lookupFolderId(args[1]), paramb(args, 2, true));
            break;
        case MODIFY_FOLDER_URL:
            mMbox.modifyFolderURL(lookupFolderId(args[1]), args[2]);
            break;
        case MODIFY_TAG_COLOR:
            mMbox.modifyTagColor(lookupTag(args[1]).getId(), Color.fromString(args[2]));            
            break;
        case MOVE_CONVERSATION:
            mMbox.moveConversation(translateId(args[1]), lookupFolderId(param(args, 2)), param(args, 3));
            break;                        
        case MOVE_ITEM:
            mMbox.moveItem(args[1], lookupFolderId(param(args, 2)), param(args, 3));
            break;                                    
        case MOVE_MESSAGE:
            mMbox.moveMessage(args[1], lookupFolderId(param(args, 2)));
            break;                        
        case NOOP:
            mMbox.noOp();
            break;
        case RENAME_FOLDER:
            mMbox.renameFolder(lookupFolderId(args[1]), args[2]);
            break;        
        case RENAME_TAG:
            mMbox.renameTag(lookupTag(args[1]).getId(), args[2]);
            break;
        case SEARCH:
            doSearch(args);
            break;
        case SEARCH_NEXT:
            doSearchNext(args);
            break;
        case SEARCH_CURRENT:
            doSearchCurrent(args);
            break;            
        case SEARCH_PREVIOUS:
            doSearchPrevious(args);
            break;
        case SYNC_FOLDER:
            mMbox.syncFolder(lookupFolderId(args[1]));
            break;
        case TAG_CONVERSATION:
            mMbox.tagConversation(translateId(args[1]), lookupTag(args[2]).getId(), paramb(args, 3, true), param(args, 4));
            break;
        case TAG_ITEM:
            mMbox.tagItem(args[1], lookupTag(args[2]).getId(), paramb(args, 3, true), param(args, 4));
            break;
        case TAG_MESSAGE:
            mMbox.tagMessage(args[1], lookupTag(args[2]).getId(), paramb(args, 3, true));
            break;
        default:
            return false;
        }
        return true;
    }

    private String emailAddrs(List<ZEmailAddress> addrs) {
        StringBuilder sb = new StringBuilder();
        for (ZEmailAddress e : addrs) {
            if (sb.length() >0) sb.append(", ");
            sb.append(e.getDisplay());
        }
        return sb.toString();
    }
    
    
    private Stack<Cursor> mSearchCursors = new Stack<Cursor>();
    private Stack<Integer> mSearchOffsets = new Stack<Integer>();
    private Map<Integer, String> mSearchIndexToId = new HashMap<Integer, String>();

    private void doSearch(String[] args) throws ServiceException {
        mSearchParams = new ZSearchParams(args[1]);
        mSearchCursors.clear();
        mSearchOffsets.clear();
        mSearchOffsets.push(0);
        mSearchIndexToId.clear();
        //System.out.println(result);
        dumpSearch(mMbox.search(mSearchParams));                
    }
    
    private void doSearchCurrent(String[] args) throws ServiceException {
        ZSearchResult sr = mSearchResult;
        if (sr == null || !sr.hasMore())
            return;
        dumpSearch(mSearchResult);
    }

    private void doSearchNext(String[] args) throws ServiceException {
        ZSearchParams sp = mSearchParams;
        ZSearchResult sr = mSearchResult;
        if (sp == null || sr == null || !sr.hasMore())
            return;

        List<ZSearchHit> hits = sr.getHits();
        if (hits.size() == 0) return;
        ZSearchHit lastHit = hits.get(hits.size()-1);
        Cursor cursor = new Cursor(lastHit.getId(), lastHit.getSortFied());
        mSearchCursors.push(cursor);
        mSearchOffsets.push(mSearchOffsets.peek() + hits.size());
        sp.setCursor(cursor);
        dumpSearch(mMbox.search(sp));        
    }

    private void doSearchPrevious(String[] args) throws ServiceException {
        ZSearchParams sp = mSearchParams;
        ZSearchResult sr = mSearchResult;
        if (sp == null || sr == null || mSearchCursors.size() == 0)
            return;
        mSearchCursors.pop();
        mSearchOffsets.pop();
        sp.setCursor(mSearchCursors.size() > 0 ? mSearchCursors.peek() : null);
        dumpSearch(mMbox.search(sp));
    }

    private int colWidth(int num) {
        int i = 1;
        while (num > 10) {
            i++;
            num /= 10;
        }
        return i;
    }

    private void dumpSearch(ZSearchResult sr) throws ServiceException {
        mSearchResult =  sr;
        //System.out.println(result);
        int offset = mSearchOffsets.peek();
        int first = offset+1;
        int last = offset+sr.getHits().size();

        System.out.printf("num: %d, more: %s, hits: %d - %d%n%n", sr.getHits().size(), sr.hasMore(), first, last);
        int width = colWidth(last);
        
        final int FROM_LEN = 20;
        
        Calendar c = Calendar.getInstance();
        String headerFormat = String.format("%%%d.%ds   %%10.10s  %%-20.20s  %%-50.50s  %%s%%n", width, width);
        //String headerFormat = String.format("%10.10s  %-20.20s  %-50.50s  %-6.6s  %s%n");
        
        String itemFormat = String.format("%%%d.%ds.  %%10.10s  %%-20.20s  %%-50.50s  %%tD %%<tR%%n", width, width);
        //String itemFormat = "%10.10s  %-20.20s  %-50.50s  %-6.6s  %tD %5$tR%n";

        System.out.format(headerFormat, "", "Id", "From", "Subject", "Date");
        System.out.format(headerFormat, "", "----------", "--------------------", "--------------------------------------------------", "--------------");
        int i = first;
        for (ZSearchHit hit: sr.getHits()) {
            if (hit instanceof ZConversationHit) {
                ZConversationHit ch = (ZConversationHit) hit;
                c.setTimeInMillis(ch.getDate());
                String sub = ch.getSubject();
                String from = emailAddrs(ch.getRecipients());
                if (ch.getMessageCount() > 1) {
                    String numMsg = " ("+ch.getMessageCount()+")";
                    int space = FROM_LEN - numMsg.length();
                    from = ( (from.length() < space) ? from : from.substring(0, space)) + numMsg;
                }
                //if (ch.getFragment() != null || ch.getFragment().length() > 0)
                //    sub += " (" + ch.getFragment()+")";
                mSearchIndexToId.put(i, ch.getId());
                System.out.format(itemFormat, i++, ch.getId(), from, sub, c);
            }
        }
        System.out.println();
    }

    private void doGetAllTags(String[] args) throws ServiceException {
        boolean verbose = (args.length == 2) && args[1].equals("-v");
        for (String tag: mMbox.getAllTagNames()) {
            dumpTag(tag, verbose);
        }
    }        

    private void dumpTag(String tagName, boolean verbose) throws ServiceException {
        if (verbose) {
            ZTag tag = mMbox.getTagByName(tagName);
            if (tag != null) System.out.println(tag);
        } else {
            System.out.println(tagName);
        }
    }

    private void doDumpFolder(ZFolder folder, boolean verbose, boolean recurse) {
        if (verbose) {
            System.out.println(folder);
        } else {
            System.out.println(folder.getPath());
        }
        if (recurse) {
            for (ZFolder child : folder.getSubFolders()) {
                doDumpFolder(child, verbose, recurse);
            }
        }
    }

    private void doGetAllFolders(String[] args) throws ServiceException {
        boolean verbose = (args.length == 2) && args[1].equals("-v");
        doDumpFolder(mMbox.getUserRoot(), verbose, true);
    }        

    private void doDumpMountpoints(ZFolder folder, boolean verbose, boolean recurse) {
        for (ZMountpoint link: folder.getLinks()) {
            if (verbose) {
                System.out.println(link);
            } else {
                System.out.println(link.getPath());
            }
        }

        if (recurse) {
            for (ZFolder child : folder.getSubFolders()) {
                doDumpMountpoints(child, verbose, recurse);
            }
        }
    }

    private void doGetAllMountpoints(String[] args) throws ServiceException {
        boolean verbose = (args.length == 2) && args[1].equals("-v");
        doDumpMountpoints(mMbox.getUserRoot(), verbose, true);
    }        

    private void doGetConversation(String[] args) throws ServiceException {
        int i = 1;
        boolean verbose = (args.length == 3) && args[1].equals("-v");
        if (verbose) i = 2;
        ZConversation conv = mMbox.getConversation(translateId(args[i]));
        if (verbose) {
            System.out.println(conv);
        } else {

            System.out.format("%nSubject: %s%nTags: %s%nFlags: %s%nNumber-of-Messages: %d%n%n",
                    conv.getSubject(), conv.getTagIds(), conv.getFlags(), conv.getMessageCount());
            
            System.out.format("%10.10s  %-12.12s  %-50.50s  %s%n", 
                    "Id", "Sender", "Fragment", "Date");
            System.out.format("%10.10s  %-12.12s  %-50.50s  %s%n", 
                    "----------", "------------", "--------------------------------------------------", "---------------");
            for (ZMessageSummary ms : conv.getMessageSummaries()) {
                System.out.format("%10.10s  %-12.12s  %-50.50s  %tD %4$tR%n", 
                        ms.getId(), ms.getSender().getDisplay(), ms.getFragment(), ms.getDate());
            }
            System.out.println();
        }
    }        

    private void doGetMessage(String[] args) throws ServiceException {
        ZMessage msg = mMbox.getMessage(args[1], true, false, false, null, null); // TODO: optionally pass in these args
        System.out.println(msg);
    }        
    
    private void dumpContact(GalContact contact) throws ServiceException {
        System.out.println("# name "+contact.getId());
        Map<String, Object> attrs = contact.getAttrs();
        dumpAttrs(attrs);
        System.out.println();
    }
    
    private void dumpAttrs(Map<String, Object> attrsIn) {
        TreeMap<String, Object> attrs = new TreeMap<String, Object>(attrsIn);

        for (Map.Entry<String, Object> entry : attrs.entrySet()) {
            String name = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof String[]) {
                String sv[] = (String[]) value;
                for (int i = 0; i < sv.length; i++) {
                    System.out.println(name+": "+sv[i]);
                }
            } else if (value instanceof String){
                System.out.println(name+": "+value);
            }
        }
    }
    
    /*
    private Account lookupAccount(String key) throws ServiceException {
        Account a = null;
        switch(guessType(key)) {
        case BY_ID:
            a = mProv.get(AccountBy.id, key);
            break;
        case BY_EMAIL:
            a = mProv.get(AccountBy.name, key);
            break;
        case BY_NAME:
            a = mProv.get(AccountBy.name, key);            
            break;
        }
        if (a == null)
            throw AccountServiceException.NO_SUCH_ACCOUNT(key);
        else
            return a;
    }
*/
    private Map<String, Object> getMap(String[] args, int offset) throws ArgException {
        Map<String, Object> attrs = new HashMap<String, Object>();
        for (int i = offset; i < args.length; i+=2) {
            String n = args[i];
            if (i+1 >= args.length)
                throw new ArgException("not enough arguments");
            String v = args[i+1];
            StringUtil.addToMultiMap(attrs, n, v);
        }
        return attrs;
    }

    private void interactive() throws IOException {
        mInteractive = true;
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            System.out.print(mPrompt);
            String line = in.readLine();
            if (line == null || line.length() == -1)
                break;
            if (mVerbose) {
                System.out.println(line);
            }
            String args[] = StringUtil.parseLine(line);
            if (args.length == 0)
                continue;
            try {
                if (!execute(args)) {
                    System.out.println("Unknown command. Type: 'help commands' for a list");
                }
            } catch (ServiceException e) {
                Throwable cause = e.getCause();
                System.err.println("ERROR: " + e.getCode() + " (" + e.getMessage() + ")" + 
                        (cause == null ? "" : " (cause: " + cause.getClass().getName() + " " + cause.getMessage() + ")"));
                if (mVerbose) e.printStackTrace(System.err);
            } catch (ArgException e) {
                    usage();
            }
        }
    }

    public static void main(String args[]) throws IOException, ParseException, SoapFaultException {
        Zimbra.toolSetup();
        
        ZMailboxUtil pu = new ZMailboxUtil();
        CommandLineParser parser = new GnuParser();
        Options options = new Options();
        options.addOption("h", "help", false, "display usage");
        options.addOption("u", "url", true, "http[s]://host[:port] of server to connect to");
        options.addOption("a", "account", true, "account name (not used with --ldap)");
        options.addOption("p", "password", true, "password for account");
        options.addOption("v", "verbose", false, "verbose mode");
        options.addOption("d", "debug", false, "debug mode");        
        
        CommandLine cl = null;
        boolean err = false;
        
        try {
            cl = parser.parse(options, args, true);
        } catch (ParseException pe) {
            System.err.println("error: " + pe.getMessage());
            err = true;
        }
            
        if (err || cl.hasOption('h')) {
            pu.usage();
        }
        
        pu.setVerbose(cl.hasOption('v'));
        if (cl.hasOption('u')) pu.setUrl(cl.getOptionValue('u'));
        if (cl.hasOption('a')) pu.setAccount(cl.getOptionValue('a'));
        if (cl.hasOption('p')) pu.setPassword(cl.getOptionValue('p'));
        if (cl.hasOption('d')) pu.setDebug(true);

        args = cl.getArgs();
        
        try {
            pu.initMailbox();
            if (args.length < 1) {
                pu.interactive();
            } else {
                try {
                    if (!pu.execute(args))
                        pu.usage();
                } catch (ArgException e) {
                    pu.usage();
                }
            }
        } catch (ServiceException e) {
            Throwable cause = e.getCause();
            System.err.println("ERROR: " + e.getCode() + " (" + e.getMessage() + ")" + 
                    (cause == null ? "" : " (cause: " + cause.getClass().getName() + " " + cause.getMessage() + ")"));  
            System.exit(2);
        }
    }
    
    class ArgException extends Exception {
        ArgException(String msg) {
            super(msg);
        }
    }

    private void doHelp(String[] args) {
        Category cat = null;
        if (args != null && args.length >= 2) {
            String s = args[1].toUpperCase();
            try {
                cat = Category.valueOf(s);
            } catch (IllegalArgumentException e) {
                for (Category c : Category.values()) {
                    if (c.name().startsWith(s)) {
                        cat = c;
                        break;
                    }
                }
            }
        }

        if (args == null || args.length == 1 || cat == null) {
            System.out.println(" zmmailbox is used for mailbox management. Try:");
            System.out.println("");
            for (Category c: Category.values()) {
                System.out.printf("     zmmailbox help %-15s %s\n", c.name().toLowerCase(), c.getDescription());
            }
            
        }
        
        if (cat != null) {
            System.out.println("");            
            for (Command c : Command.values()) {
                if (!c.hasHelp()) continue;
                if (cat == Category.COMMANDS || cat == c.getCategory())
                    System.out.printf("  %s(%s) %s\n", c.getName(), c.getAlias(), c.getHelp());
            }
        
        }
        System.out.println();
    }

    private long mSendStart;
    
    public void receiveSoapMessage(Element envelope) {
        long end = System.currentTimeMillis();        
        System.out.printf("======== SOAP RECEIVE =========\n");
        System.out.println(envelope.prettyPrint());
        System.out.printf("=============================== (%d msecs)\n", end-mSendStart);
        
    }

    public void sendSoapMessage(Element envelope) {
        mSendStart = System.currentTimeMillis();
        System.out.println("========== SOAP SEND ==========");
        System.out.println(envelope.prettyPrint());
        System.out.println("===============================");
    }
}
