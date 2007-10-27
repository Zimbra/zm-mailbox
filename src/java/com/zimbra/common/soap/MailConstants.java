/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007 Zimbra, Inc.
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
package com.zimbra.common.soap;

import org.dom4j.Namespace;
import org.dom4j.QName;


public class MailConstants {

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
    public static final QName GET_ITEM_REQUEST = QName.get("GetItemRequest", NAMESPACE);
    public static final QName GET_ITEM_RESPONSE = QName.get("GetItemResponse", NAMESPACE);
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
    public static final QName GET_MSG_METADATA_REQUEST = QName.get("GetMsgMetadataRequest", NAMESPACE);
    public static final QName GET_MSG_METADATA_RESPONSE = QName.get("GetMsgMetadataResponse", NAMESPACE);
    public static final QName MSG_ACTION_REQUEST = QName.get("MsgActionRequest", NAMESPACE);
    public static final QName MSG_ACTION_RESPONSE = QName.get("MsgActionResponse", NAMESPACE);
    //   SendMsg/AddMsg/SaveDraft
    public static final QName SEND_MSG_REQUEST = QName.get("SendMsgRequest", NAMESPACE);
    public static final QName SEND_MSG_RESPONSE = QName.get("SendMsgResponse", NAMESPACE);
    public static final QName ADD_MSG_REQUEST = QName.get("AddMsgRequest", NAMESPACE);
    public static final QName ADD_MSG_RESPONSE = QName.get("AddMsgResponse", NAMESPACE);
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
    // mountpoints
    public static final QName CREATE_MOUNTPOINT_REQUEST = QName.get("CreateMountpointRequest", NAMESPACE);
    public static final QName CREATE_MOUNTPOINT_RESPONSE = QName.get("CreateMountpointResponse", NAMESPACE);
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


    // Calendar
    public static final QName GET_APPT_SUMMARIES_REQUEST = QName.get("GetApptSummariesRequest", NAMESPACE);
    public static final QName GET_APPOINTMENT_REQUEST = QName.get("GetAppointmentRequest", NAMESPACE);
    public static final QName SET_APPOINTMENT_REQUEST = QName.get("SetAppointmentRequest", NAMESPACE);
    public static final QName CREATE_APPOINTMENT_REQUEST = QName.get("CreateAppointmentRequest", NAMESPACE);
    public static final QName CREATE_APPOINTMENT_EXCEPTION_REQUEST = QName.get("CreateAppointmentExceptionRequest", NAMESPACE);
    public static final QName MODIFY_APPOINTMENT_REQUEST = QName.get("ModifyAppointmentRequest", NAMESPACE);
    public static final QName CANCEL_APPOINTMENT_REQUEST = QName.get("CancelAppointmentRequest", NAMESPACE);
    public static final QName IMPORT_APPOINTMENTS_REQUEST = QName.get("ImportAppointmentsRequest", NAMESPACE);
    public static final QName IMPORT_APPOINTMENTS_RESPONSE = QName.get("ImportAppointmentsResponse", NAMESPACE);

    public static final QName GET_TASK_SUMMARIES_REQUEST = QName.get("GetTaskSummariesRequest", NAMESPACE);
    public static final QName GET_TASK_REQUEST = QName.get("GetTaskRequest", NAMESPACE);
    public static final QName SET_TASK_REQUEST = QName.get("SetTaskRequest", NAMESPACE);
    public static final QName CREATE_TASK_REQUEST = QName.get("CreateTaskRequest", NAMESPACE);
    public static final QName CREATE_TASK_EXCEPTION_REQUEST = QName.get("CreateTaskExceptionRequest", NAMESPACE);
    public static final QName MODIFY_TASK_REQUEST = QName.get("ModifyTaskRequest", NAMESPACE);
    public static final QName CANCEL_TASK_REQUEST = QName.get("CancelTaskRequest", NAMESPACE);
    public static final QName COMPLETE_TASK_INSTANCE_REQUEST = QName.get("CompleteTaskInstanceRequest", NAMESPACE);

    public static final QName GET_CALITEM_SUMMARIES_REQUEST = QName.get("GetCalendarItemSummariesRequest", NAMESPACE);
//    public static final QName GET_CALITEM_REQUEST = QName.get("GetCalendarItemRequest", NAMESPACE);
//    public static final QName SET_CALITEM_REQUEST = QName.get("SetCalendarItemRequest", NAMESPACE);
//    public static final QName CREATE_CALITEM_REQUEST = QName.get("CreateCalendarItemRequest", NAMESPACE);
//    public static final QName CREATE_CALITEM_EXCEPTION_REQUEST = QName.get("CreateCalendarItemExceptionRequest", NAMESPACE);
//    public static final QName MODIFY_CALITEM_REQUEST = QName.get("ModifyCalendarItemRequest", NAMESPACE);
//    public static final QName CANCEL_CALITEM_REQUEST = QName.get("CancelCalendarItemRequest", NAMESPACE);

    public static final QName SEND_INVITE_REPLY_REQUEST = QName.get("SendInviteReplyRequest", NAMESPACE);
    public static final QName ICAL_REPLY_REQUEST = QName.get("ICalReplyRequest", NAMESPACE);
    public static final QName GET_FREE_BUSY_REQUEST = QName.get("GetFreeBusyRequest", NAMESPACE);
    public static final QName GET_ICAL_REQUEST = QName.get("GetICalRequest", NAMESPACE);
    public static final QName ANNOUNCE_ORGANIZER_CHANGE_REQUEST = QName.get("AnnounceOrganizerChangeRequest", NAMESPACE);
    public static final QName DISMISS_CALITEM_ALARM_REQUEST = QName.get("DismissCalendarItemAlarmRequest", NAMESPACE);

    // spell checking
    public static final QName CHECK_SPELLING_REQUEST = QName.get("CheckSpellingRequest", NAMESPACE);
    public static final QName CHECK_SPELLING_RESPONSE = QName.get("CheckSpellingResponse", NAMESPACE);

    // documents and wiki
    public static final QName SAVE_DOCUMENT_REQUEST = QName.get("SaveDocumentRequest", NAMESPACE);
    public static final QName SAVE_DOCUMENT_RESPONSE = QName.get("SaveDocumentResponse", NAMESPACE);
    public static final QName SAVE_WIKI_REQUEST = QName.get("SaveWikiRequest", NAMESPACE);
    public static final QName SAVE_WIKI_RESPONSE = QName.get("SaveWikiResponse", NAMESPACE);
    public static final QName LIST_WIKI_REQUEST = QName.get("ListWikiRequest", NAMESPACE);
    public static final QName LIST_WIKI_RESPONSE = QName.get("ListWikiResponse", NAMESPACE);
    public static final QName GET_WIKI_REQUEST = QName.get("GetWikiRequest", NAMESPACE);
    public static final QName GET_WIKI_RESPONSE = QName.get("GetWikiResponse", NAMESPACE);
    public static final QName WIKI_ACTION_REQUEST = QName.get("WikiActionRequest", NAMESPACE);
    public static final QName WIKI_ACTION_RESPONSE = QName.get("WikiActionResponse", NAMESPACE);

    // data sources
    public static final QName CREATE_DATA_SOURCE_REQUEST = QName.get("CreateDataSourceRequest", NAMESPACE);
    public static final QName CREATE_DATA_SOURCE_RESPONSE = QName.get("CreateDataSourceResponse", NAMESPACE);
    public static final QName GET_DATA_SOURCES_REQUEST = QName.get("GetDataSourcesRequest", NAMESPACE);
    public static final QName GET_DATA_SOURCES_RESPONSE = QName.get("GetDataSourcesResponse", NAMESPACE);
    public static final QName MODIFY_DATA_SOURCE_REQUEST = QName.get("ModifyDataSourceRequest", NAMESPACE);
    public static final QName MODIFY_DATA_SOURCE_RESPONSE = QName.get("ModifyDataSourceResponse", NAMESPACE);
    public static final QName TEST_DATA_SOURCE_REQUEST = QName.get("TestDataSourceRequest", NAMESPACE);
    public static final QName TEST_DATA_SOURCE_RESPONSE = QName.get("TestDataSourceResponse", NAMESPACE);
    public static final QName DELETE_DATA_SOURCE_REQUEST = QName.get("DeleteDataSourceRequest", NAMESPACE);
    public static final QName DELETE_DATA_SOURCE_RESPONSE = QName.get("DeleteDataSourceResponse", NAMESPACE);
    public static final QName IMPORT_DATA_REQUEST = QName.get("ImportDataRequest", NAMESPACE);
    public static final QName IMPORT_DATA_RESPONSE = QName.get("ImportDataResponse", NAMESPACE);
    public static final QName GET_IMPORT_STATUS_REQUEST = QName.get("GetImportStatusRequest", NAMESPACE);
    public static final QName GET_IMPORT_STATUS_RESPONSE = QName.get("GetImportStatusResponse", NAMESPACE);
    
    public static final QName CREATE_WAIT_SET_REQUEST = QName.get("CreateWaitSetRequest", NAMESPACE);
    public static final QName CREATE_WAIT_SET_RESPONSE = QName.get("CreateWaitSetResponse", NAMESPACE);
    public static final QName WAIT_SET_REQUEST = QName.get("WaitSetRequest", NAMESPACE);
    public static final QName WAIT_SET_RESPONSE = QName.get("WaitSetResponse", NAMESPACE);
    public static final QName DESTROY_WAIT_SET_REQUEST = QName.get("DestroyWaitSetRequest", NAMESPACE);
    public static final QName DESTROY_WAIT_SET_RESPONSE = QName.get("DestroyWaitSetResponse", NAMESPACE);
    
    public static final String E_MAILBOX = "mbx";
    public static final String E_ITEM = "item";
    public static final String E_MSG = "m";
    public static final String E_CONV = "c";
    public static final String E_CHAT = "chat";
    public static final String E_NOTE = "note";
	public static final String E_TAG = "tag";
    public static final String E_CONTACT = "cn";
	public static final String E_FOLDER = "folder";
    public static final String E_SEARCH = "search";
    public static final String E_MOUNT = "link";

    public static final String E_INFO = "info";
    public static final String E_LOCALE = "locale";
    public static final String E_PARAM = "p";
    public static final String E_BROWSE_DATA = "bd";
    public static final String E_ACTION = "action";
    public static final String E_ATTRIBUTE = "a";
    public static final String E_NAME = "name";
    public static final String E_DESCRIPTION = "desc";
    public static final String E_VCARD = "vcard";
    public static final String E_SIGNATURE = "signature";

    // filter rules
    public static final String E_RULE = "r";
    public static final String E_RULES = "rules";
    public static final String E_CONDITION_GROUP = "g";
    public static final String E_CONDITION = "c";
    public static final String E_FILTER_ARG = "arg";

    // grants
    public static final String E_ACL = "acl";
    public static final String E_GRANT = "grant";
    public static final String A_ZIMBRA_ID = "zid";
    public static final String A_RIGHTS = "perm";
    public static final String A_GRANT_TYPE = "gt";
    public static final String A_PASSWORD = "pw";

    // email addresses
    public static final String E_EMAIL = "e";
    public static final String A_ADDRESS = "a";
    public static final String A_PERSONAL = "p";
    public static final String A_DISPLAY = "d";
    public static final String A_ADDRESS_TYPE = "t";
    public static final String A_ADD_TO_AB = "add";

    public static final String A_PATH = "path";
    public static final String A_NAME = "name";
    public static final String A_VALUE = "value";
    public static final String A_DATE = "d";
    public static final String A_SENT_DATE = "sd";
    public static final String A_SIZE = "s";
    public static final String A_FLAGS = "f";
    public static final String A_ID = "id";
    public static final String A_IDS = "ids";
    public static final String A_CONV_ID = "cid";
    public static final String A_MESSAGE_ID = "mid";
    public static final String A_REF = "ref";
    public static final String A_TARGET_CONSTRAINT = "tcon";
    public static final String A_TAG = "tag";
    public static final String A_TAGS = "t";
    public static final String A_FOLDER = "l";
    public static final String A_VISIBLE = "visible";
    public static final String A_ESTIMATE_SIZE = "estimateSize";
    public static final String A_URL = "url";
    public static final String A_NUM = "n";
    public static final String A_TOTAL_SIZE = "total";
    public static final String A_OPERATION = "op";
    public static final String A_RECURSIVE = "recursive";
    public static final String A_DEFAULT_VIEW = "view";
	public static final String A_UNREAD = "u";
	public static final String A_COLOR = "color";
    public static final String A_CREATED_DATE = "cd";
    public static final String A_ATTRIBUTE_NAME = "n";
    public static final String A_REPLACE = "replace";
	public static final String A_BOUNDS = "pos";
    public static final String A_STATUS = "status";
    public static final String A_EXCLUDE_FREEBUSY = "excludeFreeBusy";
    public static final String A_FILE_AS_STR = "fileAsStr";
    public static final String A_CONTACT_TYPE = "type";
    public static final String A_ELIDED = "elided";
    public static final String A_CAL_EXPAND_INST_START = "calExpandInstStart";
    public static final String A_CAL_EXPAND_INST_END = "calExpandInstEnd";
    public static final String A_RESULT_MODE = "resultMode";
    public static final String A_PREFETCH = "prefetch";
    public static final String A_VERBOSE = "verbose";
    public static final String A_CSVFORMAT = "csvfmt";

    // messages
    public static final String E_MIMEPART = "mp";
    public static final String E_SUBJECT = "su";
    public static final String E_FRAG = "fr";
    public static final String E_MSG_ID_HDR = "mid";
    public static final String E_IN_REPLY_TO = "irt";
    public static final String E_CONTENT = "content";
    public static final String E_SHARE_NOTIFICATION = "shr";
    public static final String A_PART = "part";
    public static final String A_BODY = "body";
    public static final String A_CONTENT_TYPE = "ct";
    public static final String A_CONTENT_DISPOSTION = "cd";
    public static final String A_CONTENT_DESCRIPTION = "cde";
    public static final String A_CONTENT_ID = "ci";
    public static final String A_CONTENT_LOCATION = "cl";
    public static final String A_CONTENT_NAME = "name";
    public static final String A_CONTENT_FILENAME = "filename";
    public static final String A_NO_ICAL = "noICal";
    public static final String A_RAW = "raw";
    public static final String A_HEADER = "header";
    public static final String A_WANT_HTML = "html";
    public static final String A_MARK_READ = "read";
    public static final String A_NEUTER = "neuter";
    public static final String A_MAX_INLINED_LENGTH = "max";
    public static final String A_TRUNCATED_CONTENT = "truncated";

    // send/save draft
    public static final String E_ATTACH = "attach";
    public static final String A_ATTACHMENT_ID = "aid";
    public static final String A_ORIG_ID = "origid";
    public static final String A_REPLY_TYPE = "rt";
    public static final String A_IDENTITY_ID = "idnt";
    public static final String A_NO_SAVE_TO_SENT = "noSave";
    public static final String A_SEND_UID = "suid";

    // mountpoints
    public static final String A_REMOTE_ID = "rid";
    public static final String A_OWNER_NAME = "owner";

    // browse
    public static final String A_BROWSE_BY = "browseBy";
    public static final String A_BROWSE_DOMAIN_HEADER = "h";

    // search
    public static final String E_QUERY = "query";
    public static final String E_HIT_MIMEPART = "hp";
    public static final String A_SCORE = "score";
    public static final String A_QUERY = "query";
    public static final String A_GROUPBY = "groupBy";
    public static final String A_SEARCH_TYPES = "types";
    public static final String A_SORT_FIELD = "sf";
    public static final String A_SORTBY = "sortBy";
    public static final String A_SORTVAL = "sortVal";
    public static final String A_ENDSORTVAL = "endSortVal";
    public static final String A_FETCH = "fetch";
    public static final String A_NEST_MESSAGES = "nest";
    public static final String A_RECIPIENTS = "recip";
    public static final String A_CONTENTMATCHED = "cm";
    public static final String A_ITEM_TYPE = "t";
    public static final String A_FIELD = "field";
    public static final String A_INCLUDE_TAG_DELETED = "includeTagDeleted"; //

    // search-result paging
    public static final String E_CURSOR = "cursor";
    public static final String A_QUERY_CONTEXT = "context";
    public static final String A_QUERY_OFFSET = "offset";
    public static final String A_QUERY_LIMIT = "limit";
    public static final String A_QUERY_MORE = "more";

    // sync
    public static final String E_DELETED = "deleted";
    public static final String A_TOKEN = "token";
    public static final String A_REVISION = "rev";
    public static final String A_FETCH_IF_EXISTS = "fie";
    public static final String A_CHANGE_DATE = "md";
    public static final String A_MODIFIED_SEQUENCE = "ms";
    public static final String A_SYNC = "sync";
    public static final String A_TYPED_DELETES = "typed";

    // filter rules
    public static final String A_LHS = "k0";
    public static final String A_RHS = "k1";
    public static final String A_MODIFIER = "mod";
    public static final String A_ACTIVE = "active";

    // calendar / appointment
    public static final String E_APPOINTMENT = "appt";
    public static final String E_TASK = "task";
    public static final String E_INVITE = "inv";
    public static final String E_INVITE_COMPONENT = "comp";
    public static final String E_CAL_DESCRIPTION = "desc";
    public static final String E_INSTANCE = "inst";
    public static final String E_FREEBUSY_USER = "usr";
    public static final String E_FREEBUSY_FREE = "f";
    public static final String E_FREEBUSY_BUSY = "b";
    public static final String E_FREEBUSY_BUSY_TENTATIVE = "t";
    public static final String E_FREEBUSY_BUSY_UNAVAILABLE = "u";
    public static final String E_FREEBUSY_NO_DATA = "n";
    public static final String E_CAL_ORGANIZER = "or";
    public static final String E_CAL_ICAL = "ical";
    public static final String E_CAL_ATTENDEE = "at";
    public static final String E_CAL_RECUR = "recur";
    public static final String E_CAL_DATES = "dates";
    public static final String E_CAL_DATE_VAL = "dtval";
    public static final String E_CAL_ADD = "add";
    public static final String E_CAL_EXCLUDE = "exclude";
    public static final String E_CAL_EXCEPT = "except";
    public static final String E_CAL_CANCEL = "cancel";
    public static final String E_CAL_EXCEPTION_ID = "exceptId";
    public static final String E_CAL_DURATION = "dur";
    public static final String E_CAL_START_TIME = "s";
    public static final String E_CAL_END_TIME = "e";
    public static final String E_CAL_REPLIES = "replies";
    public static final String E_CAL_REPLY = "reply";
    public static final String E_CAL_ATTACH = "attach";
    public static final String E_CAL_ALARM = "alarm";
    public static final String E_CAL_ALARM_DATA = "alarmData";
    public static final String E_CAL_ALARM_TRIGGER = "trigger";
    public static final String E_CAL_ALARM_REPEAT = "repeat";
    public static final String E_CAL_ALARM_RELATIVE = "rel";
    public static final String E_CAL_ALARM_ABSOLUTE = "abs";
    public static final String E_CAL_ALARM_DESCRIPTION = "desc";
    public static final String E_CAL_ALARM_SUMMARY = "summary";

    public static final String E_CAL_RULE = "rule";
    public static final String E_CAL_RULE_UNTIL = "until";
    public static final String E_CAL_RULE_COUNT = "count";
    public static final String E_CAL_RULE_INTERVAL = "interval";
    public static final String E_CAL_RULE_BYSECOND = "bysecond";
    public static final String E_CAL_RULE_BYMINUTE = "byminute";
    public static final String E_CAL_RULE_BYHOUR = "byhour";
    public static final String E_CAL_RULE_BYDAY = "byday";
    public static final String E_CAL_RULE_BYDAY_WKDAY = "wkday";
    public static final String E_CAL_RULE_BYMONTHDAY = "bymonthday";
    public static final String E_CAL_RULE_BYYEARDAY = "byyearday";
    public static final String E_CAL_RULE_BYWEEKNO = "byweekno";
    public static final String E_CAL_RULE_BYMONTH = "bymonth";
    public static final String E_CAL_RULE_BYSETPOS = "bysetpos";
    public static final String E_CAL_RULE_WKST = "wkst";
    public static final String E_CAL_RULE_XNAME = "rule-x-name";

    public static final String E_CAL_TZ = "tz";
    public static final String E_CAL_TZ_STANDARD = "standard";
    public static final String E_CAL_TZ_DAYLIGHT = "daylight";

    public static final String E_CAL_XPROP = "xprop";
    public static final String E_CAL_XPARAM = "xparam";

    public static final String A_CAL_ALARM_DISMISSED_AT = "dismissedAt";
    public static final String A_CAL_NEXT_ALARM = "nextAlarm";
    public static final String A_CAL_ALARM_INSTANCE_START = "alarmInstStart";
    public static final String A_CAL_INCLUDE_CONTENT = "includeContent";
    public static final String A_NEED_CALENDAR_SENTBY_FIXUP = "needCalendarSentByFixup";
    public static final String A_UID = "uid";
    public static final String A_CAL_ID = "calItemId";
    public static final String A_DEFAULT = "default";
    public static final String A_VERB = "verb";
    public static final String A_CAL_ITEM_TYPE = "type";
    public static final String A_CAL_COMP = "comp";
    public static final String A_CAL_COMPONENT_NUM = "compNum";
    public static final String A_CAL_SEQUENCE = "seq";
    public static final String A_CAL_IS_EXCEPTION = "ex";
    public static final String A_CAL_RANGE = "range";
    public static final String A_CAL_RECURRENCE_ID = "recurId";
    public static final String A_CAL_RECURRENCE_RANGE_TYPE = "rangeType";
    public static final String A_CAL_START_TIME = "s";
    public static final String A_CAL_END_TIME = "e";
    public static final String A_CAL_DURATION = "d";
    public static final String A_CAL_NEW_DURATION = "dur";
    public static final String A_CAL_DATETIME = "d";
    public static final String A_CAL_TZ_OFFSET = "tzo";
    public static final String A_CAL_SUB_ID = "subId";
    public static final String A_CAL_INV_ID = "invId";
    public static final String A_APPT_ID_DEPRECATE_ME = "apptId";
    public static final String A_CAL_STATUS = "status";
    public static final String A_CAL_PARTSTAT = "ptst";
    public static final String A_APPT_FREEBUSY = "fb";
    public static final String A_APPT_FREEBUSY_ACTUAL = "fba";
    public static final String A_APPT_TRANSPARENCY = "transp";
    public static final String A_CAL_CLASS = "class";
    public static final String A_CAL_ALLDAY = "allDay";
    public static final String A_CAL_LOCATION = "loc";
    public static final String A_CAL_NEEDS_REPLY = "needsReply";
    public static final String A_CAL_SENDUPDATE = "sendUpd";
    public static final String A_CAL_SENTBY = "sentBy";
    public static final String A_CAL_DIR = "dir";
    public static final String A_CAL_LANGUAGE = "lang";
    public static final String A_CAL_CUTYPE = "cutype";
    public static final String A_CAL_ROLE = "role";
    public static final String A_CAL_RSVP = "rsvp";
    public static final String A_CAL_MEMBER = "member";
    public static final String A_CAL_DELEGATED_TO = "delTo";
    public static final String A_CAL_DELEGATED_FROM = "delFrom";
    public static final String A_CAL_OTHER_ATTENDEES = "otherAtt";
    public static final String A_CAL_ALARM = "alarm";
    public static final String A_CAL_RECUR = "recur";
    public static final String A_CAL_UPDATE_ORGANIZER = "updateOrganizer";
    public static final String A_CAL_THIS_AND_FUTURE = "thisAndFuture";
    public static final String A_CAL_TIMEZONE= "tz";
    public static final String A_CAL_ISORG = "isOrg";
    public static final String A_CAL_ATTENDEE = "at";
    public static final String A_CAL_PRIORITY = "priority";
    public static final String A_TASK_PERCENT_COMPLETE = "percentComplete";
    public static final String A_TASK_DUE_DATE = "dueDate";
    public static final String A_TASK_COMPLETED = "completed";
    public static final String A_WAIT = "wait";
    public static final String A_TIMEOUT = "timeout";
    public static final String A_WAIT_DISALLOWED= "waitDisallowed";
    public static final String A_LIMIT_TO_ONE_BLOCKED = "limitToOneBlocked";

    public static final String A_CAL_RULE_FREQ = "freq";
    public static final String A_CAL_RULE_COUNT_NUM = "num";
    public static final String A_CAL_RULE_INTERVAL_IVAL = "ival";
    public static final String A_CAL_RULE_BYSECOND_SECLIST = "seclist";
    public static final String A_CAL_RULE_BYMINUTE_MINLIST = "minlist";
    public static final String A_CAL_RULE_BYHOUR_HRLIST = "hrlist";
    public static final String A_CAL_RULE_BYDAY_WKDAY_ORDWK = "ordwk";
    public static final String A_CAL_RULE_DAY = "day";
    public static final String A_CAL_RULE_BYMONTHDAY_MODAYLIST = "modaylist";
    public static final String A_CAL_RULE_BYYEARDAY_YRDAYLIST = "yrdaylist";
    public static final String A_CAL_RULE_BYWEEKNO_WKLIST = "wklist";
    public static final String A_CAL_RULE_BYMONTH_MOLIST = "molist";
    public static final String A_CAL_RULE_BYSETPOS_POSLIST = "poslist";
    public static final String A_CAL_RULE_XNAME_NAME = "name";
    public static final String A_CAL_RULE_XNAME_VALUE = "value";

    public static final String A_CAL_DURATION_NEGATIVE= "neg";
    public static final String A_CAL_DURATION_WEEKS = "w";
    public static final String A_CAL_DURATION_DAYS = "d";
    public static final String A_CAL_DURATION_HOURS = "h";
    public static final String A_CAL_DURATION_MINUTES = "m";
    public static final String A_CAL_DURATION_SECONDS = "s";

    public static final String A_CAL_TZ_STDOFFSET = "stdoff";
    public static final String A_CAL_TZ_DAYOFFSET = "dayoff";
    public static final String A_CAL_TZ_WEEK = "week";
    public static final String A_CAL_TZ_DAYOFWEEK = "wkday";
    public static final String A_CAL_TZ_MONTH = "mon";
    public static final String A_CAL_TZ_DAYOFMONTH = "mday";
    public static final String A_CAL_TZ_HOUR = "hour";
    public static final String A_CAL_TZ_MINUTE = "min";
    public static final String A_CAL_TZ_SECOND = "sec";

    public static final String A_CAL_ALARM_ACTION = "action";
    public static final String A_CAL_ALARM_RELATED = "related";
    public static final String A_CAL_ALARM_COUNT = "count";
    public static final String A_CAL_ATTACH_URI = "uri";
    public static final String A_CAL_ATTACH_CONTENT_TYPE = "ct";

    // spell checking
    public static final String A_AVAILABLE = "available";
    public static final String E_MISSPELLED = "misspelled";
    public static final String A_WORD = "word";
    public static final String A_SUGGESTIONS = "suggestions";

    // data sources
    public static final String E_DS = "dsrc";
    public static final String E_DS_POP3 = "pop3";
    public static final String E_DS_IMAP = "imap";
    public static final String A_DS_IS_ENABLED = "isEnabled";
    public static final String A_DS_HOST = "host";
    public static final String A_DS_PORT = "port";
    public static final String A_DS_CONNECTION_TYPE = "connectionType";
    public static final String A_DS_USERNAME = "username";
    public static final String A_DS_PASSWORD = "password";
    public static final String A_DS_TYPE = "type";
    public static final String A_DS_SUCCESS = "success";
    public static final String A_DS_ERROR = "error";
    public static final String A_DS_IS_RUNNING = "isRunning";
    public static final String A_DS_LEAVE_ON_SERVER = "leaveOnServer";
    public static final String A_DS_POLLING_INTERVAL = "pollingInterval";
    public static final String A_DS_EMAIL_ADDRESS = "emailAddress";
    public static final String A_DS_USE_ADDRESS_FOR_FORWARD_REPLY ="useAddressForForwardReply";
    public static final String A_DS_DEFAULT_SIGNATURE = "defaultSignature";
    public static final String A_DS_FROM_DISPLAY = "fromDisplay";
    public static final String A_DS_REPLYTO_ADDRESS = "replyToAddress";
    public static final String A_DS_REPLYTO_DISPLAY = "replyToDisplay";
    

    // TODO: move to a different service.
    // wiki
    public static final String A_WIKIWORD = "w";
    public static final String A_CREATOR = "cr";
    public static final String A_TYPE = "t";
    public static final String E_WIKIWORD = "w";
    public static final String E_DOC = "doc";
    public static final String E_UPLOAD = "upload";
    public static final String A_VERSION = "ver";
    public static final String A_SUBJECT = "su";
    public static final String A_MODIFIED_DATE = "md";
    public static final String A_LAST_EDITED_BY = "leb";
    public static final String A_COUNT = "count";
    public static final String A_TRAVERSE = "tr";
    public static final String A_ARGS = "args";
    public static final String A_REST_URL = "rest";
    
    // WaitSet
    public static final String E_WAITSET_ADD = "add";
    public static final String E_WAITSET_UPDATE = "update";
    public static final String E_WAITSET_REMOVE = "remove";
    public static final String E_ERROR = "error";
    public static final String A_WAITSET_ID = "waitSet";
    public static final String A_SEQ = "seq";
    public static final String A_BLOCK = "block";
    public static final String A_CANCELED = "canceled";
    public static final String A_DEFTYPES = "defTypes";
    public static final String A_ALL_ACCOUNTS = "allAccounts";
    public static final String A_TYPES = "types";
    public static final String E_A = "a";
    
    
}
