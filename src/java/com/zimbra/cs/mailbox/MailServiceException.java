/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
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

/*
 * Created on Jun 1, 2004
 */
package com.zimbra.cs.mailbox;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.service.util.ItemId;

import javax.mail.Address;

public class MailServiceException extends ServiceException {
    private static final long serialVersionUID = -2047883574519400960L;

    public static final String MAINTENANCE     = "mail.MAINTENANCE";
    public static final String NO_SUCH_MBOX    = "mail.NO_SUCH_MBOX";
    public static final String NO_SUCH_ITEM    = "mail.NO_SUCH_ITEM";
    public static final String NO_SUCH_CONV    = "mail.NO_SUCH_CONV";
    public static final String NO_SUCH_MSG     = "mail.NO_SUCH_MSG";
    public static final String NO_SUCH_PART    = "mail.NO_SUCH_PART";
    public static final String NO_SUCH_CONTACT = "mail.NO_SUCH_CONTACT";
    public static final String NO_SUCH_FOLDER  = "mail.NO_SUCH_FOLDER";
    public static final String NO_SUCH_MOUNTPOINT = "mail.NO_SUCH_MOUNTPOINT";
    public static final String NO_SUCH_NOTE    = "mail.NO_SUCH_NOTE";
    public static final String NO_SUCH_CALITEM = "mail.NO_SUCH_CALITEM";
    public static final String NO_SUCH_APPT    = "mail.NO_SUCH_APPT";
    public static final String NO_SUCH_TASK    = "mail.NO_SUCH_TASK";
    public static final String NO_SUCH_DOC     = "mail.NO_SUCH_DOC";
    public static final String NO_SUCH_REVISION = "mail.NO_SUCH_REVISION";
    public static final String NO_SUCH_TAG     = "mail.NO_SUCH_TAG";
    public static final String NO_SUCH_UPLOAD  = "mail.NO_SUCH_UPLOAD";
    public static final String NO_SUCH_WAITSET = "mail.NO_SUCH_WAITSET";
    public static final String NO_SUCH_BLOB    = "mail.NO_SUCH_BLOB";
    public static final String NO_SUCH_CHAT    = "mail.NO_SUCH_CHAT";
    
    public static final String ALREADY_EXISTS  = "mail.ALREADY_EXISTS";

    public static final String INVALID_ID      = "mail.INVALID_ID";
    public static final String INVALID_SYNC_TOKEN = "mail.INVALID_SYNC_TOKEN";
    public static final String INVALID_NAME    = "mail.INVALID_NAME";
    public static final String INVALID_TYPE    = "mail.INVALID_TYPE";
    public static final String INVALID_CONTENT_TYPE = "mail.INVALID_CONTENT_TYPE";
    public static final String IS_NOT_CHILD    = "mail.IS_NOT_CHILD";
    public static final String CANNOT_CONTAIN  = "mail.CANNOT_CONTAIN";
    public static final String CANNOT_COPY     = "mail.CANNOT_COPY";
    public static final String CANNOT_TAG      = "mail.CANNOT_TAG";
    public static final String CANNOT_PARENT   = "mail.CANNOT_PARENT";
    public static final String CANNOT_RENAME   = "mail.CANNOT_RENAME";
    public static final String CANNOT_SUBSCRIBE = "mail.CANNOT_SUBSCRIBE";
    public static final String IMMUTABLE_OBJECT = "mail.IMMUTABLE_OBJECT";
    public static final String WRONG_MAILBOX   = "mail.WRONG_MAILBOX";
    public static final String MODIFY_CONFLICT = "mail.MODIFY_CONFLICT";
    public static final String TRY_AGAIN       = "mail.TRY_AGAIN";
    public static final String CANNOT_LOCK     = "mail.CANNOT_LOCK";
    public static final String CANNOT_UNLOCK   = "mail.CANNOT_UNLOCK";
    public static final String LOCKED          = "mail.LOCKED";

    public static final String SCAN_ERROR      = "mail.SCAN_ERROR";
    public static final String UPLOAD_REJECTED = "mail.UPLOAD_REJECTED";
    public static final String TOO_MANY_TAGS   = "mail.TOO_MANY_TAGS";
    public static final String TOO_MANY_UPLOADS  = "mail.TOO_MANY_UPLOADS";
    public static final String TOO_MANY_CONTACTS = "mail.TOO_MANY_CONTACTS";
    public static final String TOO_MUCH_METADATA = "mail.TOO_MUCH_METADATA";
    public static final String UNABLE_TO_IMPORT_CONTACTS = "mail.UNABLE_TO_IMPORT_CONTACTS";
    public static final String UNABLE_TO_EXPORT_CONTACTS = "mail.UNABLE_TO_EXPORT_CONTACTS";
    public static final String UNABLE_TO_IMPORT_APPOINTMENTS = "mail.UNABLE_TO_IMPORT_APPOINTMENTS";    
    public static final String QUOTA_EXCEEDED  = "mail.QUOTA_EXCEEDED";
    public static final String QUERY_PARSE_ERROR = "mail.QUERY_PARSE_ERROR";
    public static final String MESSAGE_PARSE_ERROR = "mail.MESSAGE_PARSE_ERROR";
    public static final String ADDRESS_PARSE_ERROR = "mail.ADDRESS_PARSE_ERROR";
    public static final String ICALENDAR_PARSE_ERROR = "mail.ICALENDAR_PARSE_ERROR";
    public static final String MUST_BE_ORGANIZER = "mail.MUST_BE_ORGANIZER";
    public static final String CANNOT_CANCEL_INSTANCE_OF_EXCEPTION = "mail.CANNOT_CANCEL_INSTANCE_OF_EXCEPTION";
    public static final String INVITE_OUT_OF_DATE = "mail.INVITE_OUT_OF_DATE";
    public static final String SEND_ABORTED_ADDRESS_FAILURE = "mail.SEND_ABORTED_ADDRESS_FAILURE";
    public static final String SEND_PARTIAL_ADDRESS_FAILURE = "mail.SEND_PARTIAL_ADDRESS_FAILURE";
    public static final String SEND_FAILURE = "mail.SEND_FAILURE";
    public static final String TOO_MANY_QUERY_TERMS_EXPANDED = "mail.TOO_MANY_QUERY_TERMS_EXPANDED";
    public static final String MESSAGE_TOO_BIG = "mail.MESSAGE_TOO_BIG";
    public static final String TEXT_INDEX_OUT_OF_SYNC = "mail.TEXT_INDEX_OUT_OF_SYNC";
    
    public static final String INVALID_COMMIT_ID = "mail.INVALID_COMMIT_ID";
    
    public static final String TEMPORARY_ANALYSIS_ERROR = "mail.TEMPORARY_ANALYSIS_ERROR";
    
    public static final String ID              = "id";
    public static final String TOKEN           = "token";
    public static final String ITEM_ID         = "itemId";
    public static final String REMOTE_ACCOUNT_ID = "remoteAcctId";
    public static final String REMOTE_ITEM_ID  = "remoteItemId";
    public static final String REVISION        = "ver";
    public static final String NAME            = "name"; 
    public static final String PATH            = "path"; 
    public static final String UID             = "uid"; 
    public static final String UPLOAD_ID       = "uploadId";
    public static final String LIMIT           = "limit";
    public static final String TYPE            = "type";
    public static final String CURRENT_TOKEN   = "curTok"; 
    public static final String LINE_NO         = "lineNo"; 
    public static final String COL_NO          = "colNo";
    public static final String PARSER_ERR      = "parserErr"; 
        
    /**
     * A public inner subclass whose purpose is to group various NoSuchItemXYZ
     * exceptions into a common type so that one can write a catch block to
     * catch all such exceptions.  Without this, it is necessary to catch
     * MailServiceException, then string-compare the code with all NO_SUCH_*
     * codes.
     */
    public static class NoSuchItemException extends MailServiceException {
        private static final long serialVersionUID = -3367247762350948868L;

        NoSuchItemException(String message, String code, boolean isReceiversFault, Throwable cause, Argument... args) {
            super(message, code, isReceiversFault, cause, args);
        }
        NoSuchItemException(String message, String code, boolean isReceiversFault, Argument... args) {
            super(message, code, isReceiversFault, null, args);
        }
    }

    private MailServiceException(String message, String code, boolean isReceiversFault, Argument... args) {
        super(message, code, isReceiversFault, args);
    }

    MailServiceException(String message, String code, boolean isReceiversFault, Throwable cause, Argument... args) {
        super(message, code, isReceiversFault, cause, args);
    }
    
    public static MailServiceException MAINTENANCE(long id) {
        return new MailServiceException("mailbox in maintenance mode: "+ id, MAINTENANCE, RECEIVERS_FAULT, new Argument(MAILBOX_ID, id, Argument.Type.IID));
    }

    public static MailServiceException NO_SUCH_MBOX(long id) {
        return new MailServiceException("no such mailbox: "+ id, NO_SUCH_MBOX, SENDERS_FAULT, new Argument(MAILBOX_ID, id, Argument.Type.IID));
    }

    public static MailServiceException NO_SUCH_MBOX(String accountId) {
        return new MailServiceException("no mailbox for account: "+ accountId, NO_SUCH_MBOX, SENDERS_FAULT, new Argument(ACCOUNT_ID, accountId, Argument.Type.ACCTID));
    }

    public static MailServiceException NO_SUCH_ITEM(int id) {
        return new NoSuchItemException("no such item: "+ id, NO_SUCH_ITEM, SENDERS_FAULT, new Argument(ITEM_ID, id, Argument.Type.IID));
    }

    public static MailServiceException NO_SUCH_ITEM(String name) {
        return new NoSuchItemException("no such item: "+ name, NO_SUCH_ITEM, SENDERS_FAULT, new Argument(NAME, name, Argument.Type.STR));
    }

    public static MailServiceException NO_SUCH_CONV(int id) {
        return new NoSuchItemException("no such conversation: "+ id, NO_SUCH_CONV, SENDERS_FAULT, new Argument(ITEM_ID, id, Argument.Type.IID));
    }

    public static MailServiceException NO_SUCH_MSG(int id) {
        return new NoSuchItemException("no such message: "+ id, NO_SUCH_MSG, SENDERS_FAULT, new Argument(ITEM_ID, id, Argument.Type.IID));
    }

    public static MailServiceException NO_SUCH_PART(String name) {
        return new MailServiceException("no such message part: " + name, NO_SUCH_PART, SENDERS_FAULT, new Argument(NAME, name, Argument.Type.STR));
    }

    public static MailServiceException NO_SUCH_CONTACT(int id) {
        return new NoSuchItemException("no such contact: " + id, NO_SUCH_CONTACT, SENDERS_FAULT, new Argument(ITEM_ID, id, Argument.Type.IID));
    }

    public static MailServiceException NO_SUCH_FOLDER(int id) {
        return new NoSuchItemException("no such folder id: " + id, NO_SUCH_FOLDER, SENDERS_FAULT, new Argument(ITEM_ID, id, Argument.Type.IID));
    }
    
    public static MailServiceException NO_SUCH_FOLDER(String path) {
        return new NoSuchItemException("no such folder path: " + path, NO_SUCH_FOLDER, SENDERS_FAULT, new Argument(PATH, path, Argument.Type.STR));
    }
    
    public static MailServiceException NO_SUCH_MOUNTPOINT(int id, String remoteAccountId, int remoteId, Throwable t) {
        return new NoSuchItemException("no such mountpoint id: " + id + "; owner = " + remoteAccountId + ", remoteId=" + remoteId,
                                       NO_SUCH_MOUNTPOINT, SENDERS_FAULT, t,
                                       new Argument(ITEM_ID, id, Argument.Type.IID),
                                       new Argument(REMOTE_ACCOUNT_ID, remoteAccountId, Argument.Type.ACCTID),
                                       new Argument(REMOTE_ITEM_ID, remoteId, Argument.Type.IID));
    }

    public static MailServiceException NO_SUCH_NOTE(int id) {
        return new NoSuchItemException("no such note: " + id, NO_SUCH_NOTE, SENDERS_FAULT, new Argument(ITEM_ID, id, Argument.Type.IID));
    }

    public static MailServiceException NO_SUCH_CALITEM(int id) {
        return new NoSuchItemException("no such appointment or task: " + id, NO_SUCH_CALITEM, SENDERS_FAULT, new Argument(ITEM_ID, id, Argument.Type.IID));
    }

    public static MailServiceException NO_SUCH_CALITEM(int id, String msg) {
        return new NoSuchItemException("no such appointment or task: " + id + "; " + msg, NO_SUCH_CALITEM, SENDERS_FAULT, new Argument(ITEM_ID, id, Argument.Type.IID));
    }

    public static MailServiceException NO_SUCH_CALITEM(String uid) {
        return new NoSuchItemException("no such appointment or task: " + uid, NO_SUCH_CALITEM, SENDERS_FAULT, new Argument(UID, uid, Argument.Type.STR));
    }

    public static MailServiceException NO_SUCH_CALITEM(String uid, String msg) {
        return new MailServiceException("no such appointment or task: " + uid + "; " + msg, NO_SUCH_CALITEM, SENDERS_FAULT, new Argument(UID, uid, Argument.Type.STR));
    }
    
    public static MailServiceException NO_SUCH_APPT(int id) {
        return new NoSuchItemException("no such appointment: " + id, NO_SUCH_APPT, SENDERS_FAULT, new Argument(ITEM_ID, id, Argument.Type.IID));
    }

    public static MailServiceException NO_SUCH_APPT(String uid) {
        return new NoSuchItemException("no such appointment: " + uid, NO_SUCH_APPT, SENDERS_FAULT, new Argument(UID, uid, Argument.Type.STR));
    }

    public static MailServiceException NO_SUCH_APPT(String uid, String msg) {
        return new MailServiceException("no such appointment: "+uid+" "+msg, NO_SUCH_APPT, SENDERS_FAULT, new Argument(UID, uid, Argument.Type.STR));
    }
    
    public static MailServiceException NO_SUCH_TASK(int id) {
        return new NoSuchItemException("no such task: " + id, NO_SUCH_TASK, SENDERS_FAULT, new Argument(ITEM_ID, id, Argument.Type.IID));
    }

    public static MailServiceException NO_SUCH_TASK(String uid) {
        return new NoSuchItemException("no such task: " + uid, NO_SUCH_TASK, SENDERS_FAULT, new Argument(UID, uid, Argument.Type.STR));
    }

    public static MailServiceException NO_SUCH_TASK(String uid, String msg) {
        return new MailServiceException("no such task: "+uid+" "+msg, NO_SUCH_TASK, SENDERS_FAULT, new Argument(UID, uid, Argument.Type.STR));
    }

    public static MailServiceException NO_SUCH_DOC(String path) {
        return new NoSuchItemException("no such item: "+ path, NO_SUCH_ITEM, SENDERS_FAULT, new Argument(NAME, path, Argument.Type.STR));
    }

    public static MailServiceException NO_SUCH_DOC(int id) {
        return new NoSuchItemException("no such document: " + id, NO_SUCH_DOC, SENDERS_FAULT, new Argument(ITEM_ID, id, Argument.Type.IID));
    }

    public static MailServiceException NO_SUCH_REVISION(int docId, int version) {
        return new NoSuchItemException("no such revision: " + docId + '/' + version, NO_SUCH_REVISION, SENDERS_FAULT, new Argument(ITEM_ID, docId, Argument.Type.IID), new Argument(REVISION, version, Argument.Type.NUM));
    }

    public static MailServiceException NO_SUCH_TAG(int id) {
        return new NoSuchItemException("no such tag: " + id, NO_SUCH_TAG, SENDERS_FAULT, new Argument(ITEM_ID, id, Argument.Type.IID));
    }

    public static MailServiceException NO_SUCH_TAG(String name) {
        return new NoSuchItemException("no such tag: " + name, NO_SUCH_TAG, SENDERS_FAULT, new Argument(NAME, name, Argument.Type.STR));
    }

    public static MailServiceException NO_SUCH_UPLOAD(String uploadId) {
        return new MailServiceException("upload not found: " + uploadId, NO_SUCH_UPLOAD, SENDERS_FAULT, new Argument(UPLOAD_ID, uploadId, Argument.Type.STR));
    }
    
    public static MailServiceException NO_SUCH_WAITSET(String id) {
        return new MailServiceException("WaitSet not found: " + id, NO_SUCH_WAITSET, SENDERS_FAULT, new Argument(ID, id, Argument.Type.STR));
    }

    public static MailServiceException NO_SUCH_BLOB(long mboxId, int itemId, int revision) {
        return new MailServiceException("No such blob: mailbox=" + mboxId + ", item=" + itemId + ", change=" + revision, NO_SUCH_BLOB, SENDERS_FAULT);
    }

    public static MailServiceException NO_SUCH_CHAT(String threadId) {
        return new MailServiceException("No such chat: threadId="+threadId, NO_SUCH_CHAT, SENDERS_FAULT);
    }
    
    public static MailServiceException SCAN_ERROR(String file) {
        return new MailServiceException("upload could not be scanned: file '" + file + "'", SCAN_ERROR, RECEIVERS_FAULT, new Argument(NAME, file, Argument.Type.STR));
    }

    public static MailServiceException UPLOAD_REJECTED(String file, String reason) {
        return new MailServiceException("upload rejected: file '" + file + "': " + reason, UPLOAD_REJECTED, SENDERS_FAULT, new Argument(NAME, file, Argument.Type.STR), new Argument("reason", reason, Argument.Type.STR));
    }

    // note that this logs "service.TOO_MANY_HOPS" rather than "mail.*"
    public static MailServiceException TOO_MANY_HOPS(ItemId iid) {
        return new MailServiceException("proxy loop detected", TOO_MANY_HOPS, SENDERS_FAULT, new Argument(ITEM_ID, iid.toString(), Argument.Type.IID));
    }

    public static MailServiceException TOO_MANY_TAGS() {
        return new MailServiceException("exceeded limit of " + MailItem.MAX_TAG_COUNT + " tags", TOO_MANY_TAGS, SENDERS_FAULT, new Argument(LIMIT, MailItem.MAX_TAG_COUNT, Argument.Type.NUM));
    }

    public static MailServiceException TOO_MANY_UPLOADS(String uploadId) {
        return new MailServiceException("more than 1 file uploaded: " + uploadId, TOO_MANY_UPLOADS, SENDERS_FAULT, new Argument(UPLOAD_ID, uploadId, Argument.Type.STR));
    }

    public static MailServiceException TOO_MANY_CONTACTS(int limit) {
        return new MailServiceException("exceeded limit of " + limit + " contacts", TOO_MANY_CONTACTS, SENDERS_FAULT, new Argument(LIMIT, limit, Argument.Type.NUM));
    }

    public static MailServiceException TOO_MUCH_METADATA(int limit) {
        return new MailServiceException("exceeded limit of " + limit + " chars of custom metadata", TOO_MUCH_METADATA, SENDERS_FAULT, new Argument(LIMIT, limit, Argument.Type.NUM));
    }

    public static MailServiceException UNABLE_TO_IMPORT_CONTACTS(String msg, Throwable t) {
        return new MailServiceException(msg, UNABLE_TO_IMPORT_CONTACTS, false, t);
    }

    public static MailServiceException UNABLE_TO_EXPORT_CONTACTS(String msg, Throwable t) {
        return new MailServiceException(msg, UNABLE_TO_IMPORT_CONTACTS, false, t);
    }

    public static MailServiceException UNABLE_TO_IMPORT_APPOINTMENTS(String msg, Throwable t) {
        return new MailServiceException(msg, UNABLE_TO_IMPORT_APPOINTMENTS, false, t);
    }

    public static MailServiceException QUOTA_EXCEEDED(long limit) {
        return new MailServiceException("mailbox exceeded quota of " + limit + " bytes", QUOTA_EXCEEDED, SENDERS_FAULT, new Argument(LIMIT, limit, Argument.Type.NUM));
    }

    public static MailServiceException INVALID_ID(int id) {
        return new MailServiceException("item id out of range: " + id, INVALID_ID, SENDERS_FAULT, new Argument(ITEM_ID, id, Argument.Type.IID));
    }
    
    public static MailServiceException INVALID_SYNC_TOKEN(String s) {
        return new MailServiceException("Invalid Sync Token: " + s, INVALID_SYNC_TOKEN, SENDERS_FAULT, new Argument(TOKEN, s, Argument.Type.STR));
    }

    public static MailServiceException INVALID_NAME(String name) {
        return new MailServiceException("invalid name: " + name, INVALID_NAME, SENDERS_FAULT, new Argument(NAME, name, Argument.Type.STR));
    }

    public static MailServiceException INVALID_TYPE(byte type) {
        return new MailServiceException("invalid item type: " + type, INVALID_TYPE, SENDERS_FAULT, new Argument(TYPE, type, Argument.Type.NUM));
    }

    public static MailServiceException INVALID_CONTENT_TYPE(String type) {
        return new MailServiceException("invalid content type: " + type, INVALID_CONTENT_TYPE, SENDERS_FAULT, new Argument(TYPE, type, Argument.Type.STR));
    }

    public static MailServiceException WRONG_MAILBOX() {
        return new MailServiceException("cannot combine objects from different mailboxes", WRONG_MAILBOX, SENDERS_FAULT);
    }

    public static MailServiceException CANNOT_TAG(Tag tag, MailItem item) {
        return new MailServiceException("cannot apply tag \"" + tag.getName() + "\" to item " + item.getId(), CANNOT_TAG, SENDERS_FAULT);
    }

    public static MailServiceException CANNOT_SUBSCRIBE(int id) {
        return new MailServiceException("cannot add subscription to existing folder " + id, CANNOT_SUBSCRIBE, SENDERS_FAULT, new Argument(ITEM_ID, id, Argument.Type.IID));
    }

    public static MailServiceException CANNOT_CONTAIN() {
        return new MailServiceException("cannot put object in that folder", CANNOT_CONTAIN, SENDERS_FAULT);
    }
    
    public static MailServiceException CANNOT_CONTAIN(Folder parent, byte type) {
        String parentName = (parent == null ? "null parent folder" : "folder " + parent.getPath());
        return new MailServiceException(parentName + " cannot contain an item of type " + MailItem.getNameForType(type), CANNOT_CONTAIN, SENDERS_FAULT);
    }

    public static MailServiceException CANNOT_COPY(int id) {
        return new MailServiceException("cannot copy object: " + id, CANNOT_COPY, SENDERS_FAULT, new Argument(ITEM_ID, id, Argument.Type.IID));
    }

    public static MailServiceException CANNOT_PARENT() {
        return new MailServiceException("cannot make object a child of that parent", CANNOT_PARENT, SENDERS_FAULT);
    }

    public static MailServiceException CANNOT_RENAME(byte type) {
        return new MailServiceException("cannot set the name on a " + MailItem.getNameForType(type), CANNOT_RENAME, SENDERS_FAULT);
    }

    public static MailServiceException IS_NOT_CHILD() {
        return new MailServiceException("tried to remove object from non-parent", IS_NOT_CHILD, SENDERS_FAULT);
    }

    public static MailServiceException MODIFY_CONFLICT(Argument... args) {
        return new MailServiceException("modify conflict", MODIFY_CONFLICT, SENDERS_FAULT, args);
    }

    public static MailServiceException IMMUTABLE_OBJECT(int id) {
        return new MailServiceException("cannot modify immutable object: " + id, IMMUTABLE_OBJECT, SENDERS_FAULT, new Argument(ITEM_ID, id, Argument.Type.IID));
    }

    public static MailServiceException ALREADY_EXISTS(String name, Argument... args) {
        return new MailServiceException("object with that name already exists: " + name, ALREADY_EXISTS, SENDERS_FAULT, args);
    }

    public static MailServiceException ALREADY_EXISTS(String name, Throwable t, Argument... args) {
        return new MailServiceException("object with that name already exists: " + name, ALREADY_EXISTS, SENDERS_FAULT, t, args);
    }

    public static MailServiceException ALREADY_EXISTS(int id, Throwable t) {
        return new MailServiceException("object with that id already exists: " + id, ALREADY_EXISTS, SENDERS_FAULT, t, new Argument(ITEM_ID, id, Argument.Type.IID));
    }

    public static MailServiceException QUERY_PARSE_ERROR(String query, Throwable t, String curToken, int offset, String parserErr) {
        return new MailServiceException("Couldn't parse query: " + query, QUERY_PARSE_ERROR, SENDERS_FAULT, t, 
            new Argument(CURRENT_TOKEN, curToken, Argument.Type.STR), 
            new Argument(COL_NO, offset, Argument.Type.NUM),
            new Argument(PARSER_ERR, parserErr, Argument.Type.STR)
        
        );
    }

    public static MailServiceException MESSAGE_PARSE_ERROR(Throwable t) {
        return new MailServiceException("Couldn't parse message", MESSAGE_PARSE_ERROR, SENDERS_FAULT, t);
    }

    public static MailServiceException ADDRESS_PARSE_ERROR(Throwable t) {
        return new MailServiceException("Couldn't parse address", ADDRESS_PARSE_ERROR, SENDERS_FAULT, t);
    }

    public static MailServiceException ADDRESS_PARSE_ERROR(String msg, Throwable t) {
        return new MailServiceException("Couldn't parse address: " + msg, ADDRESS_PARSE_ERROR, SENDERS_FAULT, t);
    }

    public static MailServiceException ICALENDAR_PARSE_ERROR(String s, Throwable t) {
        return new MailServiceException("Couldn't parse iCalendar information: "+s, ICALENDAR_PARSE_ERROR, SENDERS_FAULT, t);
    }

    public static MailServiceException MUST_BE_ORGANIZER(String request) {
        return new MailServiceException("You are not Organizer in call to: "+request, MUST_BE_ORGANIZER, SENDERS_FAULT);
    }
    
    public static MailServiceException CANNOT_CANCEL_INSTANCE_OF_EXCEPTION(String request) {
        return new MailServiceException(
                "You cannot cancel a specific instance of an Exception, specify the default invite instead: "+request,
                CANNOT_CANCEL_INSTANCE_OF_EXCEPTION,
                SENDERS_FAULT);
    }

    public static MailServiceException INVITE_OUT_OF_DATE(String request) {
        return new MailServiceException(
                "The specified Invite is out of date (has been updated): "+request,
                INVITE_OUT_OF_DATE,
                SENDERS_FAULT);
    }
    
    private static MailServiceException internal_SEND_FAILURE(String failureType, String msg, Exception e,  Address[] invalid, Address[] unsent) {
    	int len = 0;
    	if (invalid != null)
    		len += invalid.length;
    	if (unsent != null)
    		len += unsent.length;
    	Argument[] args = new Argument[len];
    	
    	int offset = 0;
        if (invalid != null) {
            for (Address addr : invalid)
                args[offset++] = new Argument("invalid", addr.toString(), Argument.Type.STR);
        }
        if (unsent != null) {
        	for (Address addr : unsent)
        		args[offset++] = new Argument("unsent", addr.toString(), Argument.Type.STR);
        }
    	
    	return new MailServiceException(msg, failureType, SENDERS_FAULT, e, args);
    }
    
    public static MailServiceException SEND_ABORTED_ADDRESS_FAILURE(String msg, Exception e, Address[] invalid, Address[] unsent) {
    	return internal_SEND_FAILURE(SEND_ABORTED_ADDRESS_FAILURE, msg, e, invalid, unsent);
    }

    public static MailServiceException SEND_PARTIAL_ADDRESS_FAILURE(String msg, Exception e, Address[] invalid, Address[] unsent) {
    	return internal_SEND_FAILURE(SEND_PARTIAL_ADDRESS_FAILURE, msg, e, invalid, unsent);
    }

    public static MailServiceException SEND_FAILURE(String msg, Exception e, Address[] invalid, Address[] unsent) {
    	return internal_SEND_FAILURE(SEND_FAILURE, msg, e, invalid, unsent);
    }

    public static MailServiceException TRY_AGAIN(String msg) {
        return new MailServiceException("try again: " + msg, TRY_AGAIN, RECEIVERS_FAULT);
    }
    
    public static MailServiceException TRY_AGAIN(String msg, Exception e) {
        return new MailServiceException("try again: " + msg, TRY_AGAIN, RECEIVERS_FAULT, e);
    }

    public static MailServiceException TOO_MANY_QUERY_TERMS_EXPANDED(String msg, String token, int max) {
        return new MailServiceException(msg, TOO_MANY_QUERY_TERMS_EXPANDED, SENDERS_FAULT, new Argument("TOKEN", token, Argument.Type.STR), new Argument("MAX", max, Argument.Type.NUM));
    }

    public static MailServiceException INVALID_COMMIT_ID(String commitId) {
        return new MailServiceException("CommitId " + commitId + " not found in redo logs", INVALID_COMMIT_ID, SENDERS_FAULT);
    }
    
    public static MailServiceException MESSAGE_TOO_BIG(long maxSize, long actualSize) {
        Argument arg = new Argument("maxSize", maxSize, Argument.Type.NUM);
        return new MailServiceException("Message of size " + actualSize + " exceeded allowed size", MESSAGE_TOO_BIG, false, arg);
    }
    
    public static MailServiceException TEXT_INDEX_OUT_OF_SYNC() {
        return new MailServiceException("Text index is not up to date and query has a text part.", MailServiceException.TEXT_INDEX_OUT_OF_SYNC, RECEIVERS_FAULT); 
    }
    
    public static MailServiceException TEMPORARY_ANALYSIS_ERROR() {
        return new MailServiceException("Temporary Analysis Error", MailServiceException.TEMPORARY_ANALYSIS_ERROR, RECEIVERS_FAULT);
    }

    public static MailServiceException CANNOT_LOCK(int id) {
        return new MailServiceException("cannot lock item: " + id, CANNOT_LOCK, SENDERS_FAULT, new Argument(ITEM_ID, id, Argument.Type.IID));
    }

    public static MailServiceException CANNOT_LOCK(int id, String accountId) {
        return new MailServiceException("cannot lock item: " + id, CANNOT_LOCK, SENDERS_FAULT, new Argument(ITEM_ID, id, Argument.Type.IID), new Argument(ACCOUNT_ID, accountId, Argument.Type.ACCTID));
    }

    public static MailServiceException CANNOT_UNLOCK(int id) {
        return new MailServiceException("cannot unlock item: " + id, CANNOT_UNLOCK, SENDERS_FAULT, new Argument(ITEM_ID, id, Argument.Type.IID));
    }

    public static MailServiceException CANNOT_UNLOCK(int id, String accountId) {
        return new MailServiceException("cannot unlock item: " + id, CANNOT_UNLOCK, SENDERS_FAULT, new Argument(ITEM_ID, id, Argument.Type.IID), new Argument(ACCOUNT_ID, accountId, Argument.Type.ACCTID));
    }
    
    public static MailServiceException LOCKED(int id, String accountId) {
        return new MailServiceException("item is locked: " + id, LOCKED, SENDERS_FAULT, new Argument(ITEM_ID, id, Argument.Type.IID), new Argument(ACCOUNT_ID, accountId, Argument.Type.ACCTID));
    }
}
