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
 * Created on May 26, 2004
 */
package com.zimbra.cs.service.mail;


import org.dom4j.Namespace;
import org.dom4j.QName;

import com.zimbra.soap.DocumentDispatcher;
import com.zimbra.soap.DocumentService;

/**
 * @author schemers
 */
public class MailService implements DocumentService {

	public static final String NAMESPACE_STR = "urn:zimbraMail";
	public static final Namespace NAMESPACE = Namespace.get(NAMESPACE_STR);
	
    // noop
    public static final QName NO_OP_REQUEST = QName.get("NoOpRequest", NAMESPACE);
    public static final QName NO_OP_RESPONSE = QName.get("NoOpResponse", NAMESPACE);

	// searching
    public static final QName SEARCH_REQUEST = QName.get("SearchRequest", NAMESPACE);
    public static final QName SEARCH_RESPONSE = QName.get("SearchResponse", NAMESPACE);
    public static final QName SEARCH_CONV_REQUEST = QName.get("SearchConvRequest", NAMESPACE);
    public static final QName SEARCH_CONV_RESPONSE = QName.get("SearchConvResponse", NAMESPACE);
    public static final QName BROWSE_REQUEST = QName.get("BrowseRequest", NAMESPACE);
    public static final QName BROWSE_RESPONSE = QName.get("BrowseResponse", NAMESPACE);

    // generic items
    public static final QName ITEM_ACTION_REQUEST = QName.get("ItemActionRequest", NAMESPACE);
    public static final QName ITEM_ACTION_RESPONSE = QName.get("ItemActionResponse", NAMESPACE);
    // conversations
	public static final QName GET_CONV_REQUEST = QName.get("GetConvRequest", NAMESPACE);
	public static final QName GET_CONV_RESPONSE = QName.get("GetConvResponse", NAMESPACE);
	public static final QName CONV_ACTION_REQUEST = QName.get("ConvActionRequest", NAMESPACE);
	public static final QName CONV_ACTION_RESPONSE = QName.get("ConvActionResponse", NAMESPACE);
	// messages
	public static final QName GET_MSG_REQUEST = QName.get("GetMsgRequest", NAMESPACE);
	public static final QName GET_MSG_RESPONSE = QName.get("GetMsgResponse", NAMESPACE);
	public static final QName MSG_ACTION_REQUEST = QName.get("MsgActionRequest", NAMESPACE);
	public static final QName MSG_ACTION_RESPONSE = QName.get("MsgActionResponse", NAMESPACE);
	//   SendMsg/AddMsg/SendAppt
	public static final QName SEND_MSG_REQUEST = QName.get("SendMsgRequest", NAMESPACE);
	public static final QName SEND_MSG_RESPONSE = QName.get("SendMsgResponse", NAMESPACE);
	public static final QName ADD_MSG_REQUEST = QName.get("AddMsgRequest", NAMESPACE);
	public static final QName ADD_MSG_RESPONSE = QName.get("AddMsgResponse", NAMESPACE);
    public static final QName SEND_APPT_REQUEST = QName.get("SendApptRequest", NAMESPACE);
    public static final QName SEND_APPT_RESPONSE = QName.get("SendApptResponse", NAMESPACE);
    public static final QName SAVE_DRAFT_REQUEST = QName.get("SaveDraftRequest", NAMESPACE);
    public static final QName SAVE_DRAFT_RESPONSE = QName.get("SaveDraftResponse", NAMESPACE);
	// folders
	public static final QName CREATE_FOLDER_REQUEST = QName.get("CreateFolderRequest", NAMESPACE);
	public static final QName CREATE_FOLDER_RESPONSE = QName.get("CreateFolderResponse", NAMESPACE);
	public static final QName GET_FOLDER_REQUEST = QName.get("GetFolderRequest", NAMESPACE);
	public static final QName GET_FOLDER_RESPONSE = QName.get("GetFolderResponse", NAMESPACE);
	public static final QName FOLDER_ACTION_REQUEST = QName.get("FolderActionRequest", NAMESPACE);
	public static final QName FOLDER_ACTION_RESPONSE = QName.get("FolderActionResponse", NAMESPACE);
	// tags
	public static final QName CREATE_TAG_REQUEST = QName.get("CreateTagRequest", NAMESPACE);
	public static final QName CREATE_TAG_RESPONSE = QName.get("CreateTagResponse", NAMESPACE);
	public static final QName GET_TAG_REQUEST = QName.get("GetTagRequest", NAMESPACE);
	public static final QName GET_TAG_RESPONSE = QName.get("GetTagResponse", NAMESPACE);
	public static final QName TAG_ACTION_REQUEST = QName.get("TagActionRequest", NAMESPACE);
	public static final QName TAG_ACTION_RESPONSE = QName.get("TagActionResponse", NAMESPACE);
	// saved searches
	public static final QName CREATE_SEARCH_FOLDER_REQUEST = QName.get("CreateSearchFolderRequest", NAMESPACE);
	public static final QName CREATE_SEARCH_FOLDER_RESPONSE = QName.get("CreateSearchFolderResponse", NAMESPACE);
	public static final QName GET_SEARCH_FOLDER_REQUEST = QName.get("GetSearchFolderRequest", NAMESPACE);
	public static final QName GET_SEARCH_FOLDER_RESPONSE = QName.get("GetSearchFolderResponse", NAMESPACE);
	public static final QName MODIFY_SEARCH_FOLDER_REQUEST = QName.get("ModifySearchFolderRequest", NAMESPACE);
	public static final QName MODIFY_SEARCH_FOLDER_RESPONSE = QName.get("ModifySearchFolderResponse", NAMESPACE);
	// contacts
	public static final QName CREATE_CONTACT_REQUEST = QName.get("CreateContactRequest", NAMESPACE);
	public static final QName CREATE_CONTACT_RESPONSE = QName.get("CreateContactResponse", NAMESPACE);
	public static final QName MODIFY_CONTACT_REQUEST = QName.get("ModifyContactRequest", NAMESPACE);
	public static final QName MODIFY_CONTACT_RESPONSE = QName.get("ModifyContactResponse", NAMESPACE);
	public static final QName GET_CONTACTS_REQUEST = QName.get("GetContactsRequest", NAMESPACE);
	public static final QName GET_CONTACTS_RESPONSE = QName.get("GetContactsResponse", NAMESPACE);
    public static final QName IMPORT_CONTACTS_REQUEST = QName.get("ImportContactsRequest", NAMESPACE);
    public static final QName IMPORT_CONTACTS_RESPONSE = QName.get("ImportContactsResponse", NAMESPACE);
    public static final QName EXPORT_CONTACTS_REQUEST = QName.get("ExportContactsRequest", NAMESPACE);
    public static final QName EXPORT_CONTACTS_RESPONSE = QName.get("ExportContactsResponse", NAMESPACE);

	public static final QName CONTACT_ACTION_REQUEST = QName.get("ContactActionRequest", NAMESPACE);
	public static final QName CONTACT_ACTION_RESPONSE = QName.get("ContactActionResponse", NAMESPACE);
	// notes
	public static final QName CREATE_NOTE_REQUEST = QName.get("CreateNoteRequest", NAMESPACE);
	public static final QName CREATE_NOTE_RESPONSE = QName.get("CreateNoteResponse", NAMESPACE);
	public static final QName GET_NOTE_REQUEST = QName.get("GetNoteRequest", NAMESPACE);
	public static final QName GET_NOTE_RESPONSE = QName.get("GetNoteResponse", NAMESPACE);
	public static final QName NOTE_ACTION_REQUEST = QName.get("NoteActionRequest", NAMESPACE);
	public static final QName NOTE_ACTION_RESPONSE = QName.get("NoteActionResponse", NAMESPACE);
	// sync for Outlook
	public static final QName SYNC_REQUEST = QName.get("SyncRequest", NAMESPACE);
	public static final QName SYNC_RESPONSE = QName.get("SyncResponse", NAMESPACE);
	// filter rules
	public static final QName GET_RULES_REQUEST = QName.get("GetRulesRequest", NAMESPACE);
	public static final QName GET_RULES_RESPONSE = QName.get("GetRulesResponse", NAMESPACE);
    public static final QName SAVE_RULES_REQUEST = QName.get("SaveRulesRequest", NAMESPACE);
    public static final QName SAVE_RULES_RESPONSE = QName.get("SaveRulesResponse", NAMESPACE);
    // calendaring / appointment
    public static final QName GET_APPT_SUMMARIES_REQUEST = QName.get("GetApptSummariesRequest", NAMESPACE);
    public static final QName GET_APPT_SUMMARIES_RESPONSE = QName.get("GetApptSummariesResponse", NAMESPACE);
    public static final QName SEND_INVITE_REPLY_REQUEST = QName.get("SendInviteReplyRequest", NAMESPACE);
    public static final QName SEND_INVITE_REPLY_RESPONSE= QName.get("SendInviteReplyResponse", NAMESPACE);
    public static final QName CREATE_APPOINTMENT_REQUEST = QName.get("CreateAppointmentRequest", NAMESPACE);
    public static final QName CREATE_APPOINTMENT_RESPONSE= QName.get("CreateAppointmentResponse", NAMESPACE);
    public static final QName MODIFY_APPOINTMENT_REQUEST = QName.get("ModifyAppointmentRequest", NAMESPACE);
    public static final QName MODIFY_APPOINTMENT_RESPONSE= QName.get("ModifyAppointmentResponse", NAMESPACE);
    public static final QName CANCEL_APPOINTMENT_REQUEST = QName.get("CancelAppointmentRequest", NAMESPACE);
    public static final QName CANCEL_APPOINTMENT_RESPONSE= QName.get("CancelAppointmentResponse", NAMESPACE);
    public static final QName GET_APPOINTMENT_REQUEST = QName.get("GetAppointmentRequest", NAMESPACE);
    public static final QName GET_APPOINTMENT_RESPONSE= QName.get("GetAppointmentResponse", NAMESPACE);
    
    public static final QName CREATE_APPOINTMENT_EXCEPTION_REQUEST = QName.get("CreateAppointmentExceptionRequest", NAMESPACE);
    public static final QName CREATE_APPOINTMENT_EXCEPTION_RESPONSE= QName.get("CreateAppointmentExceptionResponse", NAMESPACE);
    public static final QName MODIFY_APPOINTMENT_EXCEPTION_REQUEST = QName.get("ModifyAppointmentExceptionRequest", NAMESPACE);
    public static final QName MODIFY_APPOINTMENT_EXCEPTION_RESPONSE= QName.get("ModifyAppointmentExceptionResponse", NAMESPACE);
    public static final QName CANCEL_APPOINTMENT_EXCEPTION_REQUEST = QName.get("CancelAppointmentExceptionRequest", NAMESPACE);
    public static final QName CANCEL_APPOINTMENT_EXCEPTION_RESPONSE= QName.get("CancelAppointmentExceptionResponse", NAMESPACE);
    public static final QName SET_APPOINTMENT_REQUEST = QName.get("SetAppointmentRequest", NAMESPACE);
    public static final QName SET_APPOINTMENT_RESPONSE= QName.get("SetAppointmentResponse", NAMESPACE);
    
    
    public static final QName GET_FREE_BUSY_REQUEST = QName.get("GetFreeBusyRequest", NAMESPACE);
    public static final QName GET_FREE_BUSY_RESPONSE = QName.get("GetFreeBusyResponse", NAMESPACE);
    public static final QName GET_ICAL_REQUEST = QName.get("GetICalRequest", NAMESPACE);
    public static final QName GET_ICAL_RESPONSE = QName.get("GetICalResponse", NAMESPACE);
    // spell checking
    public static final QName CHECK_SPELLING_REQUEST = QName.get("CheckSpellingRequest", NAMESPACE);
    public static final QName CHECK_SPELLING_RESPONSE = QName.get("CheckSpellingResponse", NAMESPACE);
    // objects
//    public static final QName OBJECT_ACTION_REQUEST = QName.get("ObjectActionRequest", NAMESPACE);
//    public static final QName OBJECT_ACTION_RESPONSE = QName.get("ObjectActionResponse", NAMESPACE);
    // admin/debug command hook
    public static final QName CONSOLE_REQUEST = QName.get("ConsoleRequest", NAMESPACE);    
    public static final QName CONSOLE_RESPONSE = QName.get("ConsoleResponse", NAMESPACE);    


    public static final String E_MAILBOX = "mbx";
    public static final String E_CONV = "c";
	public static final String E_MSG = "m";
	public static final String E_NOTE = "note";
	public static final String E_TAG = "tag";
    public static final String E_CONTACT = "cn";
	public static final String E_FOLDER = "folder";
    public static final String E_SEARCH = "search";
	public static final String E_MIMEPART = "mp";
    
	public static final String E_EMAIL = "e";
	public static final String E_SUBJECT = "su";
    public static final String E_FRAG = "fr";
    public static final String E_MSG_ID_HDR = "mid";
    public static final String E_IN_REPLY_TO = "irt";
    public static final String E_ATTACH = "attach";
    public static final String E_QUERY = "query";
	public static final String E_CONTENT = "content";
	public static final String E_PARAM = "p";
    public static final String E_BROWSE_DATA = "bd";
    public static final String E_ACTION = "action";
    public static final String E_ATTRIBUTE = "a";
    public static final String E_NAME = "name";    
    public static final String E_LOCATION = "loc";    
    public static final String E_DESCRIPTION = "desc";    

    public static final String E_DELETED = "deleted";    
    
    // filter rules
    public static final String E_RULE = "r";
    public static final String E_RULES = "rules";
    public static final String E_CONDITION_GROUP = "g";
    public static final String E_CONDITION = "c";
    public static final String E_FILTER_ARG = "arg";

    public static final String A_REVISION = "rev";
    public static final String A_NAME = "name";
    public static final String A_DATE = "d";
    public static final String A_SENT_DATE = "sd";
    public static final String A_ADDRESS = "a";
    public static final String A_PERSONAL = "p";
    public static final String A_DISPLAY = "d";
    public static final String A_ADDRESS_TYPE = "t";
    public static final String A_ADD_TO_AB = "add";
    public static final String A_SIZE = "s";
    public static final String A_FLAGS = "f";
    public static final String A_ID = "id";
    public static final String A_IDS = "ids";
    public static final String A_CONV_ID = "cid";
    public static final String A_MESSAGE_ID = "mid";
    public static final String A_ATTACHMENT_ID = "aid";
    public static final String A_ORIG_ID = "origid";
    public static final String A_REPLY_TYPE = "rt";
    public static final String A_REF = "ref";
    public static final String A_TARGET_CONSTRAINT = "tcon";
    public static final String A_TAG = "tag";
    public static final String A_TAGS = "t";
    public static final String A_FOLDER = "l";
    public static final String A_NO_ICAL = "noICal";
    public static final String A_WANT_HTML = "html";
    public static final String A_RAW = "raw";
    public static final String A_MARK_READ = "read";
    public static final String A_URL = "url";
    public static final String A_NUM = "n";
    public static final String A_PART = "part";
    public static final String A_BODY = "body";
    public static final String A_CONTENT_TYPE = "ct";
    public static final String A_CONTENT_DISPOSTION = "cd";
    public static final String A_CONTENT_DESCRIPTION = "cde";
    public static final String A_CONTENT_ID = "ci";
    public static final String A_CONTENT_LOCATION = "cl";    
    public static final String A_CONTENT_NAME = "name";
    public static final String A_CONTENT_FILENAME = "filename";
    public static final String A_OPERATION = "op";
	public static final String A_QUERY = "query";
	public static final String A_UNREAD = "u";
	public static final String A_COLOR = "color";
    public static final String A_CREATED_DATE = "cd";
    public static final String A_ATTRIBUTE_NAME = "n";
    public static final String A_REPLACE = "replace";
	public static final String A_BOUNDS = "pos";    
    public static final String A_STATUS = "status";

    // object actions
    public static final String A_OBJECT = "obj";
    public static final String A_OBJECT_TYPE = "ot";

    // browse
    public static final String A_BROWSE_BY = "browseBy";
    public static final String A_BROWSE_DOMAIN_HEADER = "h";

    // search
    public static final String A_SCORE = "score";
    public static final String A_GROUPBY = "groupBy";
    public static final String A_SEARCH_TYPES = "types";
    public static final String A_SORTBY = "sortBy";
    public static final String A_FETCH = "fetch";
    public static final String A_RECIPIENTS = "recip";
    public static final String A_CONTENTMATCHED = "cm";
    public static final String A_ITEM_TYPE = "t";

    // sync
    public static final String A_TOKEN = "token";
    public static final String A_FETCH_IF_EXISTS = "fie";
    public static final String A_CHANGE_DATE = "md";

    // filter rules
    public static final String A_LHS = "k0";
    public static final String A_RHS = "k1";
    public static final String A_MODIFIER = "mod";
    public static final String A_ACTIVE = "active";

    // search-result paging
    public static final String A_QUERY_CONTEXT = "context";
    public static final String A_QUERY_OFFSET = "offset";
    public static final String A_QUERY_LIMIT = "limit";
    public static final String A_QUERY_MORE= "more";
    
    // calendar / appointment
    public static final String E_APPOINTMENT = "appt";
    public static final String E_INVITE = "inv";
    public static final String E_INVITE_COMPONENT = "comp";
    public static final String E_INSTANCE = "inst";
    public static final String E_FREEBUSY_USER    = "usr";
    public static final String E_FREEBUSY_FREE    = "f";
    public static final String E_FREEBUSY_BUSY    = "b";
    public static final String E_FREEBUSY_BUSY_TENTATIVE = "t";
    public static final String E_FREEBUSY_BUSY_UNAVAILABLE = "u";
    public static final String E_FREEBUSY_NO_DATA = "n";
    public static final String E_APPT_ORGANIZER = "or";
    public static final String E_APPT_ICAL = "ical";
    public static final String E_APPT_ATTENDEE = "at";
    public static final String E_APPT_RECUR = "recur";
    public static final String E_APPT_DATE = "date";
    public static final String E_APPT_ADD = "add";
    public static final String E_APPT_EXCLUDE = "exclude";
    public static final String E_APPT_EXCEPTION_RULE = "except";
    public static final String E_APPT_CANCELLATION = "cancel";
    public static final String E_APPT_DURATION = "dur";
    public static final String E_APPT_START_TIME = "s";
    public static final String E_APPT_END_TIME = "e";

    public static final String E_APPT_RULE = "rule";
    public static final String E_APPT_RULE_UNTIL = "until";
    public static final String E_APPT_RULE_COUNT = "count";
    public static final String E_APPT_RULE_INTERVAL = "interval";
    public static final String E_APPT_RULE_BYSECOND = "bysecond";
    public static final String E_APPT_RULE_BYMINUTE = "byminute";
    public static final String E_APPT_RULE_BYHOUR = "byhour";
    public static final String E_APPT_RULE_BYDAY = "byday";
    public static final String E_APPT_RULE_BYDAY_WKDAY = "wkday";
    public static final String E_APPT_RULE_BYMONTHDAY = "bymonthday";
    public static final String E_APPT_RULE_BYYEARDAY = "byyearday";
    public static final String E_APPT_RULE_BYWEEKNO = "byweekno";
    public static final String E_APPT_RULE_BYMONTH = "bymonth";
    public static final String E_APPT_RULE_BYSETPOS = "bysetpos";
    public static final String E_APPT_RULE_WKST = "wkst";
    public static final String E_APPT_RULE_XNAME = "rule-x-name";

    public static final String A_UID = "uid";
    public static final String A_DEFAULT = "default";
    public static final String A_EXCEPT = "except";
    public static final String A_VERB = "verb";
    public static final String A_APPT_COMPONENT_NUM = "compNum";
    public static final String A_APPT_IS_EXCEPTION = "ex";
    public static final String A_APPT_RECURRENCE_ID = "recurId";
    public static final String A_APPT_RECURRENCE_RANGE_TYPE = "rangeType";
    public static final String A_APPT_START_TIME = "s";
    public static final String A_APPT_END_TIME = "e";
    public static final String A_APPT_DURATION = "d";
    public static final String A_APPT_DATETIME = "d";
    public static final String A_APPT_SUB_ID = "subId";
    public static final String A_APPT_INV_ID = "invId";
    public static final String A_APPT_ID = "apptId";
    public static final String A_APPT_STATUS = "status";
    public static final String A_APPT_PARTSTAT = "ptst";
    public static final String A_APPT_FREEBUSY = "fb";
    public static final String A_APPT_FREEBUSY_ACTUAL = "fba";
    public static final String A_APPT_TRANSPARENCY = "transp";
    public static final String A_APPT_ALLDAY = "allDay";
    public static final String A_APPT_LOCATION = "loc";
    public static final String A_APPT_NEEDS_REPLY = "needsReply";
    public static final String A_APPT_TYPE = "type";
    public static final String A_APPT_SENDUPDATE = "sendUpd";
    public static final String A_APPT_ROLE = "role";
    public static final String A_APPT_RSVP = "rsvp";
    public static final String A_APPT_OTHER_ATTENDEES = "otherAtt";
    public static final String A_APPT_ALARM = "alarm";
    public static final String A_APPT_RECUR = "recur";
    public static final String A_APPT_ACCEPT = "accept";
    public static final String A_APPT_DECLINE = "decline";
    public static final String A_APPT_TENTATIVE= "tentative";
    public static final String A_APPT_UPDATE_ORGANIZER = "updateOrganizer";
    public static final String A_APPT_THIS_AND_FUTURE = "thisAndFuture";
    public static final String A_APPT_TIMEZONE= "tz";
    public static final String A_APPT_ISORG = "isOrg";

    public static final String A_APPT_RULE_FREQ = "freq";
    public static final String A_APPT_RULE_COUNT_NUM = "num";
    public static final String A_APPT_RULE_INTERVAL_IVAL = "ival";
    public static final String A_APPT_RULE_BYSECOND_SECLIST = "seclist";
    public static final String A_APPT_RULE_BYMINUTE_MINLIST = "minlist";
    public static final String A_APPT_RULE_BYHOUR_HRLIST = "hrlist";
    public static final String A_APPT_RULE_BYDAY_WKDAY_ORDWK = "ordwk";
    public static final String A_APPT_RULE_DAY = "day";
    public static final String A_APPT_RULE_BYMONTHDAY_MODAYLIST = "modaylist";
    public static final String A_APPT_RULE_BYYEARDAY_YRDAYLIST = "yrdaylist";
    public static final String A_APPT_RULE_BYWEEKNO_WKLIST = "wklist";
    public static final String A_APPT_RULE_BYMONTH_MOLIST = "molist";
    public static final String A_APPT_RULE_BYSETPOS_POSLIST = "poslist";
    public static final String A_APPT_RULE_XNAME_NAME = "name";
    public static final String A_APPT_RULE_XNAME_VALUE = "value";
    
    public static final String A_APPT_DURATION_NEGATIVE= "neg";
    public static final String A_APPT_DURATION_WEEKS = "w";
    public static final String A_APPT_DURATION_DAYS = "d";
    public static final String A_APPT_DURATION_HOURS = "h";
    public static final String A_APPT_DURATION_MINUTES = "m";
    public static final String A_APPT_DURATION_SECONDS = "s";

    // spell checking
    public static final String A_AVAILABLE = "available";
    public static final String E_MISSPELLED = "misspelled";
    public static final String A_WORD = "word";
    public static final String A_SUGGESTIONS = "suggestions";


    public void registerHandlers(DocumentDispatcher dispatcher) {

        // noop
        dispatcher.registerHandler(NO_OP_REQUEST, new NoOp());
        
    	// searching
        dispatcher.registerHandler(BROWSE_REQUEST, new Browse());
        dispatcher.registerHandler(SEARCH_REQUEST, new Search());
        dispatcher.registerHandler(SEARCH_CONV_REQUEST, new SearchConv());

        // items
        dispatcher.registerHandler(ITEM_ACTION_REQUEST, new ItemAction());

        // conversations
        dispatcher.registerHandler(GET_CONV_REQUEST, new GetConv());
        dispatcher.registerHandler(CONV_ACTION_REQUEST, new ConvAction());

        // messages
        dispatcher.registerHandler(GET_MSG_REQUEST, new GetMsg());
        dispatcher.registerHandler(MSG_ACTION_REQUEST, new MsgAction());
        dispatcher.registerHandler(SEND_MSG_REQUEST, new SendMsg());
        dispatcher.registerHandler(ADD_MSG_REQUEST, new AddMsg());
        dispatcher.registerHandler(SAVE_DRAFT_REQUEST, new SaveDraft());

        // folders
        dispatcher.registerHandler(GET_FOLDER_REQUEST, new GetFolder());
        dispatcher.registerHandler(CREATE_FOLDER_REQUEST, new CreateFolder());
        dispatcher.registerHandler(FOLDER_ACTION_REQUEST, new FolderAction());

        // tags
        dispatcher.registerHandler(GET_TAG_REQUEST, new GetTag());
        dispatcher.registerHandler(CREATE_TAG_REQUEST, new CreateTag());
        dispatcher.registerHandler(TAG_ACTION_REQUEST, new TagAction());

        // saved searches
        dispatcher.registerHandler(GET_SEARCH_FOLDER_REQUEST, new GetSearchFolder());
        dispatcher.registerHandler(CREATE_SEARCH_FOLDER_REQUEST, new CreateSearchFolder());
        dispatcher.registerHandler(MODIFY_SEARCH_FOLDER_REQUEST, new ModifySearchFolder());

        // contacts
        dispatcher.registerHandler(GET_CONTACTS_REQUEST, new GetContacts());
        dispatcher.registerHandler(CREATE_CONTACT_REQUEST, new CreateContact());
        dispatcher.registerHandler(MODIFY_CONTACT_REQUEST, new ModifyContact());
        dispatcher.registerHandler(CONTACT_ACTION_REQUEST, new ContactAction());
        dispatcher.registerHandler(EXPORT_CONTACTS_REQUEST, new ExportContacts());
        dispatcher.registerHandler(IMPORT_CONTACTS_REQUEST, new ImportContacts());

        // notes
        dispatcher.registerHandler(GET_NOTE_REQUEST, new GetNote());
        dispatcher.registerHandler(CREATE_NOTE_REQUEST, new CreateNote());
        dispatcher.registerHandler(NOTE_ACTION_REQUEST, new NoteAction());

        // sync
        dispatcher.registerHandler(SYNC_REQUEST, new Sync());

        // filter rules
        dispatcher.registerHandler(GET_RULES_REQUEST, new GetRules());
        dispatcher.registerHandler(SAVE_RULES_REQUEST, new SaveRules());
        
        // calendar 
        dispatcher.registerHandler(GET_APPT_SUMMARIES_REQUEST, new GetApptSummaries());
        dispatcher.registerHandler(SEND_INVITE_REPLY_REQUEST, new SendInviteReply());
        dispatcher.registerHandler(CREATE_APPOINTMENT_REQUEST, new CreateAppointment());
        dispatcher.registerHandler(MODIFY_APPOINTMENT_REQUEST, new ModifyAppointment());
        dispatcher.registerHandler(CANCEL_APPOINTMENT_REQUEST, new CancelAppointment());
        dispatcher.registerHandler(GET_APPOINTMENT_REQUEST, new GetAppointment());

        dispatcher.registerHandler(CREATE_APPOINTMENT_EXCEPTION_REQUEST, new CreateAppointmentException());
        dispatcher.registerHandler(MODIFY_APPOINTMENT_EXCEPTION_REQUEST, new ModifyAppointmentException());
        dispatcher.registerHandler(CANCEL_APPOINTMENT_EXCEPTION_REQUEST, new CancelAppointmentException());
        dispatcher.registerHandler(SET_APPOINTMENT_REQUEST, new SetAppointment());
        
        dispatcher.registerHandler(GET_FREE_BUSY_REQUEST, new GetFreeBusy());
        dispatcher.registerHandler(GET_ICAL_REQUEST, new GetICal());
        
        // spell check
        dispatcher.registerHandler(CHECK_SPELLING_REQUEST, new CheckSpelling());
        
        // objects
//        dispatcher.registerHandler(OBJECT_ACTION_REQUEST, new ObjectAction());
        
        // console Request 
        dispatcher.registerHandler(CONSOLE_REQUEST, new ConsoleRequest());
	}
}
