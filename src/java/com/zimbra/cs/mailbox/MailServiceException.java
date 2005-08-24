/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: ZPL 1.1
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Jun 1, 2004
 */
package com.zimbra.cs.mailbox;

import com.zimbra.cs.service.ServiceException;


/**
 * @author schemers
 */
public class MailServiceException extends ServiceException {

    public static final String MAINTENANCE     = "mail.MAINTENANCE";
    public static final String NO_SUCH_MBOX    = "mail.NO_SUCH_MBOX";
	public static final String NO_SUCH_ITEM    = "mail.NO_SUCH_ITEM";
	public static final String NO_SUCH_CONV    = "mail.NO_SUCH_CONV";
	public static final String NO_SUCH_MSG     = "mail.NO_SUCH_MSG";
	public static final String NO_SUCH_PART    = "mail.NO_SUCH_PART";
	public static final String NO_SUCH_CONTACT = "mail.NO_SUCH_CONTACT";
	public static final String NO_SUCH_FOLDER  = "mail.NO_SUCH_FOLDER";
	public static final String NO_SUCH_NOTE    = "mail.NO_SUCH_NOTE";
	public static final String NO_SUCH_APPT    = "mail.NO_SUCH_APPT";
	public static final String NO_SUCH_DOC     = "mail.NO_SUCH_DOC";
    public static final String NO_SUCH_TAG     = "mail.NO_SUCH_TAG";
    public static final String NO_SUCH_UPLOAD  = "mail.NO_SUCH_UPLOAD";
    public static final String SCAN_ERROR      = "mail.SCAN_ERROR";
    public static final String UPLOAD_REJECTED = "mail.UPLOAD_REJECTED";
    public static final String TOO_MANY_TAGS   = "mail.TOO_MANY_TAGS";
    public static final String TOO_MANY_UPLOADS  = "mail.TOO_MANY_UPLOADS";
    public static final String TOO_MANY_CONTACTS = "mail.TOO_MANY_CONTACTS";
    public static final String UNABLE_TO_IMPORT_CONTACTS = "mail.UNABLE_TO_IMPORT_CONTACTS";    
    public static final String QUOTA_EXCEEDED  = "mail.QUOTA_EXCEEDED";
    public static final String INVALID_ID      = "mail.INVALID_ID";
    public static final String INVALID_NAME    = "mail.INVALID_NAME";
	public static final String WRONG_MAILBOX   = "mail.WRONG_MAILBOX";
	public static final String CANNOT_CONTAIN  = "mail.CANNOT_CONTAIN";
    public static final String CANNOT_COPY     = "mail.CANNOT_COPY";
	public static final String CANNOT_TAG      = "mail.CANNOT_TAG";
	public static final String CANNOT_PARENT   = "mail.CANNOT_PARENT";
	public static final String IS_NOT_CHILD    = "mail.IS_NOT_CHILD";
	public static final String MODIFY_CONFLICT = "mail.MODIFY_CONFLICT";
	public static final String IMMUTABLE_OBJECT  = "mail.IMMUTABLE_OBJECT";
	public static final String ALREADY_EXISTS    = "mail.ALREADY_EXISTS";
	public static final String QUERY_PARSE_ERROR = "mail.QUERY_PARSE_ERROR";
    public static final String MESSAGE_PARSE_ERROR = "mail.MESSAGE_PARSE_ERROR";
    public static final String ICALENDAR_PARSE_ERROR = "mail.ICALENDAR_PARSE_ERROR";
    public static final String NO_SUCH_APPOINTMENT = "mail.NO_SUCH_APPOINTMENT";
    public static final String MUST_BE_ORGANIZER = "mail.MUST_BE_ORGANIZER";
    public static final String CANNOT_CANCEL_INSTANCE_OF_EXCEPTION = "mail.CANNOT_CANCEL_INSTANCE_OF_EXCEPTION";
    public static final String INVITE_OUT_OF_DATE = "mail.INVITE_OUT_OF_DATE";
        

    /**
     * A public inner subclass whose purpose is to group various NoSuchItemXYZ
     * exceptions into a common type so that one can write a catch block to
     * catch all such exceptions.  Without this, it is necessary to catch
     * MailServiceException, then string-compare the code with all NO_SUCH_*
     * codes.
     */
    public static class NoSuchItemException extends MailServiceException {
        NoSuchItemException(String message, String code, boolean isReceiversFault, Throwable cause) {
            super(message, code, isReceiversFault, cause);
        }
    }

    private MailServiceException(String message, String code, boolean isReceiversFault) {
        super(message, code, isReceiversFault);
    }

    MailServiceException(String message, String code, boolean isReceiversFault, Throwable cause) {
        super(message, code, isReceiversFault, cause);
    }

    public static MailServiceException MAINTENANCE(int id) {
        return new MailServiceException("mailbox in maintenance mode: "+ id, MAINTENANCE, RECEIVERS_FAULT, null);
    }

    public static MailServiceException NO_SUCH_MBOX(int id) {
        return new MailServiceException("no such mailbox: "+ id, NO_SUCH_MBOX, SENDERS_FAULT, null);
    }

    public static MailServiceException NO_SUCH_MBOX(String accountId) {
        return new MailServiceException("no mailbox for account: "+ accountId, NO_SUCH_MBOX, SENDERS_FAULT, null);
    }

    public static MailServiceException NO_SUCH_ITEM(int id) {
        return new NoSuchItemException("no such item: "+ id, NO_SUCH_ITEM, SENDERS_FAULT, null);
    }

    public static MailServiceException NO_SUCH_CONV(int id) {
        return new NoSuchItemException("no such conversation: "+ id, NO_SUCH_CONV, SENDERS_FAULT, null);
    }

    public static MailServiceException NO_SUCH_MSG(int id) {
        return new NoSuchItemException("no such message: "+ id, NO_SUCH_MSG, SENDERS_FAULT, null);
    }

    public static MailServiceException NO_SUCH_PART(String name) {
        return new MailServiceException("no such message part: " + name, NO_SUCH_PART, SENDERS_FAULT, null);
    }

    public static MailServiceException NO_SUCH_CONTACT(int id) {
        return new NoSuchItemException("no such contact: " + id, NO_SUCH_CONTACT, SENDERS_FAULT, null);
    }

    public static MailServiceException NO_SUCH_FOLDER(int id) {
        return new NoSuchItemException("no such folder id: " + id, NO_SUCH_FOLDER, SENDERS_FAULT, null);
    }
    
    public static MailServiceException NO_SUCH_FOLDER(String path) {
        return new NoSuchItemException("no such folder path: " + path, NO_SUCH_FOLDER, SENDERS_FAULT, null);
    }
    
    public static MailServiceException NO_SUCH_NOTE(int id) {
        return new NoSuchItemException("no such note: " + id, NO_SUCH_NOTE, SENDERS_FAULT, null);
    }

    public static MailServiceException NO_SUCH_APPT(int id) {
        return new NoSuchItemException("no such appointment: " + id, NO_SUCH_APPT, SENDERS_FAULT, null);
    }

    public static MailServiceException NO_SUCH_APPT(String uid) {
        return new NoSuchItemException("no such appointment: " + uid, NO_SUCH_APPT, SENDERS_FAULT, null);
    }

    public static MailServiceException NO_SUCH_DOC(int id) {
        return new NoSuchItemException("no such document: " + id, NO_SUCH_DOC, SENDERS_FAULT, null);
    }

    public static MailServiceException NO_SUCH_TAG(int id) {
        return new NoSuchItemException("no such tag: " + id, NO_SUCH_TAG, SENDERS_FAULT, null);
    }

    public static MailServiceException NO_SUCH_TAG(String name) {
        return new NoSuchItemException("no such tag: " + name, NO_SUCH_TAG, SENDERS_FAULT, null);
    }

    public static MailServiceException NO_SUCH_UPLOAD(String uploadId) {
        return new MailServiceException("upload not found: " + uploadId, NO_SUCH_UPLOAD, SENDERS_FAULT, null);
    }

    public static MailServiceException SCAN_ERROR(String file) {
        return new MailServiceException("upload could not be scanned: file '" + file + "'", SCAN_ERROR, RECEIVERS_FAULT);
    }

    public static MailServiceException UPLOAD_REJECTED(String file, String reason) {
        return new MailServiceException("upload rejected: file '" + file + "': " + reason, UPLOAD_REJECTED, SENDERS_FAULT);
    }

    public static MailServiceException TOO_MANY_TAGS() {
        return new MailServiceException("exceeded limit of " + MailItem.MAX_TAG_COUNT + " tags", TOO_MANY_TAGS, SENDERS_FAULT, null);
    }

    public static MailServiceException TOO_MANY_UPLOADS(String uploadId) {
        return new MailServiceException("more than 1 file uploaded: " + uploadId, TOO_MANY_UPLOADS, SENDERS_FAULT, null);
    }

    public static MailServiceException TOO_MANY_CONTACTS(int limit) {
        return new MailServiceException("exceeded limit of " + limit + " contacts", TOO_MANY_CONTACTS, SENDERS_FAULT, null);
    }

    public static MailServiceException UNABLE_TO_IMPORT_CONTACTS(String msg, Throwable t) {
        return new MailServiceException(msg, UNABLE_TO_IMPORT_CONTACTS, false, t);
    }

    public static MailServiceException QUOTA_EXCEEDED(long limit) {
        return new MailServiceException("mailbox exceeded quota of " + limit + " bytes", QUOTA_EXCEEDED, SENDERS_FAULT, null);
    }

    public static MailServiceException INVALID_ID(int id) {
        return new MailServiceException("item id out of range: " + id, INVALID_ID, SENDERS_FAULT, null);
    }

    public static MailServiceException INVALID_NAME(String name) {
        return new MailServiceException("invalid tag/folder name: " + name, INVALID_NAME, SENDERS_FAULT, null);
    }

    public static MailServiceException WRONG_MAILBOX() {
        return new MailServiceException("cannot combine objects from different mailboxes", WRONG_MAILBOX, SENDERS_FAULT, null);
    }

    public static MailServiceException CANNOT_TAG() {
        return new MailServiceException("cannot apply tag to object", CANNOT_TAG, SENDERS_FAULT, null);
    }

    public static MailServiceException CANNOT_CONTAIN() {
        return new MailServiceException("cannot put object in that folder", CANNOT_CONTAIN, SENDERS_FAULT, null);
    }

    public static MailServiceException CANNOT_COPY(int id) {
        return new MailServiceException("cannot copy object: " + id, CANNOT_COPY, SENDERS_FAULT, null);
    }

    public static MailServiceException CANNOT_PARENT() {
        return new MailServiceException("cannot make object a child of that parent", CANNOT_PARENT, SENDERS_FAULT, null);
    }

    public static MailServiceException IS_NOT_CHILD() {
        return new MailServiceException("tried to remove object from non-parent", IS_NOT_CHILD, SENDERS_FAULT, null);
    }

    public static MailServiceException MODIFY_CONFLICT() {
        return new MailServiceException("modify conflict", MODIFY_CONFLICT, SENDERS_FAULT, null);
    }

    public static MailServiceException IMMUTABLE_OBJECT(int id) {
        return new MailServiceException("cannot modify immutable object: " + id, IMMUTABLE_OBJECT, SENDERS_FAULT, null);
    }

    public static MailServiceException ALREADY_EXISTS(String name) {
        return new MailServiceException("object with that name already exists: " + name, ALREADY_EXISTS, SENDERS_FAULT, null);
    }

    public static MailServiceException ALREADY_EXISTS(int id, Throwable t) {
        return new MailServiceException("object with that id already exists: " + id, ALREADY_EXISTS, SENDERS_FAULT, t);
    }

    public static MailServiceException QUERY_PARSE_ERROR(String query, Throwable t) {
        return new MailServiceException("couldn't parse query: " + query, QUERY_PARSE_ERROR, SENDERS_FAULT, t);
    }

    public static MailServiceException MESSAGE_PARSE_ERROR(Throwable t) {
        return new MailServiceException("couldn't parse message", MESSAGE_PARSE_ERROR, SENDERS_FAULT, t);
    }
    
    public static MailServiceException ICALENDAR_PARSE_ERROR(String s, Throwable t) {
        return new MailServiceException("couldn't parse iCalendar information: "+s, ICALENDAR_PARSE_ERROR, SENDERS_FAULT, t);
    }

    public static MailServiceException NO_SUCH_APPOINTMENT(String uid, String msg) {
        return new MailServiceException("No such Appointment: "+uid+" "+msg, NO_SUCH_APPOINTMENT, SENDERS_FAULT, null);
    }
    
    public static MailServiceException MUST_BE_ORGANIZER(String request) {
        return new MailServiceException("You are not Organizer in call to: "+request, MUST_BE_ORGANIZER, SENDERS_FAULT, null);
    }
    
    public static MailServiceException CANNOT_CANCEL_INSTANCE_OF_EXCEPTION(String request) {
        return new MailServiceException("You cannot cancel a specific instance of an Exception, specify the default invite instead: "+request, MUST_BE_ORGANIZER, SENDERS_FAULT, null);
    }

    public static MailServiceException INVITE_OUT_OF_DATE(String request) {
        return new MailServiceException("The specified Invite is out of date (has been updated): "+request, MUST_BE_ORGANIZER, SENDERS_FAULT, null);
    }
}
