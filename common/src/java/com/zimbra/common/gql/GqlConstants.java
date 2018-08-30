/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2018 Synacor, Inc.
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

package com.zimbra.common.gql;

public class GqlConstants {
    // modify search folder spec constants
    public static final String CLASS_MODIFY_SEARCH_FOLDER_SPEC = "ModifySearchFolderSpec";
    public static final String SEARCH_FOLDER_ID = "searchFolderId";
    public static final String QUERY = "query";
    public static final String SEARCH_TYPES = "searchTypes";
    public static final String SORT_BY = "sortBy";

    // new search folder spec constants
    public static final String CLASS_NEW_SEARCH_FOLDER_SPEC = "NewSearchFolderSpec";
    public static final String NAME = "name";
    public static final String FLAGS = "flags";
    public static final String COLOR = "color";
    public static final String PARENT_FOLDER_ID = "parentFolderId";
    public static final String RGB = "rgb";

    // search folder constants
    public static final String CLASS_SEARCH_FOLDER = "SearchFolder";

    // account info constants
    public static final String CLASS_ACCOUNT_INFO = "AccountInfo";
    public static final String ATTRIBUTES = "attributes";
    public static final String SOAP_URL = "soapURL";
    public static final String PUBLIC_URL = "publicURL";
    public static final String CHANGE_PASSWORD_URL = "changePasswordURL";
    public static final String COMMUNITY_URL = "communityURL";
    public static final String ADMIN_URL = "adminURL";
    public static final String BOSH_URL = "boshURL";

    // named value constants
    public static final String CLASS_NAMED_VALUE = "NamedValue";
    public static final String VALUE = "value";

    // end session constants
    public static final String CLEAR_COOKIES= "clearCookies";
    
    // shared class constants
    public static final String ATTACHMENTS = "attachments";
    public static final String ATTACHMENT_ID = "attachmentId";
    public static final String CONTENT = "content";
    public static final String CONTENT_ID = "contentId";
    public static final String FOLDER_ID = "folderId";
    public static final String FRAGMENT = "fragment";
    public static final String HEADERS = "headers";
    public static final String IDENTITY_ID = "identityId";
    public static final String IN_REPLY_TO = "inReplyTo";
    public static final String INVITE = "invite";
    public static final String MIME_PARTS = "mimeParts";
    
    // message constants
    public static final String CLASS_MESSAGE = "Message";
    public static final String EMAILADDRESSES = "emailAddresses";
    public static final String MESSAGE = "message";
    public static final String MIME_PART = "mimePart";
    public static final String ORIGINAL_ID = "originalId";
    public static final String REPLY_TYPE = "replyType";
    public static final String SUBJECT = "subject";
    public static final String TIMEZONES = "timezones";

    // message specification constants
    public static final String CLASS_MESSAGE_SPECIFICATION = "MsgSpec";
    public static final String GQL_MESSAGE_SPECIFICATIONS = "messageSpecifications";
    public static final String HIDE_IMAGES = "hideImages";
    public static final String INLINED_TEXT_LENGTH = "inlinedTextLength";
    public static final String MARK_MESSAGE_AS_READ = "markMessageAsRead";
    public static final String MESSAGE_PART = "messagePart";
    public static final String RECURRENCE_DATE_ID = "recurrenceDateId";
    public static final String INCLUDE_GROUP_INFO = "includeGroupInfo";
    public static final String INCLUDE_RAW_MESSAGE = "includeRawMessage";
    public static final String INCLUDE_URL_CONTENT = "includeUrlContent";

    // invitation info constants
    public static final String CLASS_INVITATION_INFO = "InvitationInfo";
    public static final String INVITE_ID = "id";

    // invite component constants
    public static final String CLASS_INVITE_COMPONENT = "InviteComponent";

    // mime part info constants
    public static final String CLASS_MIME_PART_INFO = "MimePartInfo";

    // attribute name constants
    public static final String CLASS_ATTRIBUTE_NAME = "AttributeName";

    // message content constants
    public static final String CLASS_MESSAGE_CONTENT = "MsgContent";

    // attachments info constants
    public static final String CLASS_ATTACHMENTS_INFO = "AttachmentsInfo";

    // attachment specification constants
    public static final String CLASS_ATTACHMENT_SPECIFICATIONS = "AttachSpec";
    public static final String OPTIONAL = "optional";

    // email address info constants
    public static final String CLASS_EMAIL_ADDRESS_INFO = "EmailAddrInfo";
    public static final String ADDRESS = "address";
    public static final String ADDRESS_TYPE = "addressType";
    public static final String PERSONAL = "personal";

    // send message constants
    public static final String ADD_SENT_BY = "addSentBy";
    public static final String IS_CALENDAR_FORWARD = "isCalendarForward";
    public static final String DO_SKIP_SAVED = "doSkipSave";
    public static final String DO_FETCH_SAVED = "doFetchSaved";
    public static final String SEND_UID = "sendUid";

    // message to send constants
    public static final String CLASS_MESSAGE_TO_SEND = "MessageToSend";
    public static final String DRAFT_ID = "draftId";
    public static final String DO_SEND_DRAFT = "doSendDraft";
    public static final String DATA_SOURCE_ID = "dataSourceId";

    // message info constants
    public static final String CLASS_MESSAGE_INFO = "MessageInfo";

    // message with group info constants
    public static final String CLASS_MESSAGE_WITH_GROUP_INFO = "MsgWithGroupInfo";

    // search constants
    public static final String CLASS_SEARCH_REQUEST = "SearchRequest";
    public static final String SEARCH_PARAMS = "params";
    public static final String SEARCH_REQUEST = "SearchRequest";
    public static final String INCLUDE_MEMBER_OF = "includeMemberOf";
    public static final String MSG_CONTENT = "msgContent";
    public static final String CURSOR = "cursor";
    public static final String LOCALE = "locale";
    public static final String CALENDAR_TIME_ZONE = "calTz";
    public static final String OFFSET = "offset";
    public static final String LIMIT = "limit";
    public static final String FIELD = "field";
    public static final String FULL_CONVERSATION = "fullConversation";
    public static final String RESULT_MODE = "resultMode";
    public static final String PREFETCH = "prefetch";
    public static final String INCLUDE_RECEPIENTS = "includeRecipients";
    public static final String NEUTER_IMAGES = "neuterImages";
    public static final String INCLUDE_IS_EXPANDABLE = "includeIsExpandable";
    public static final String INCLUDE_HTML = "includeHtml";
    public static final String MAX_INLINED_LENGTH = "maxInlinedLength";
    public static final String MARK_READ = "markRead";
    public static final String FETCH = "fetch";
    public static final String QUICK = "quick";
    public static final String GROUP_BY = "groupBy";
    public static final String IN_DUMPSTER = "inDumpster";
    public static final String CALENDAR_ITEM_EXPANDED_END = "calItemExpandEnd";
    public static final String CALENDAR_ITEM_EXPANDED_START = "calItemExpandStart";
    public static final String ALLOWABLE_TASK_STATUS = "allowableTaskStatus";
    public static final String INCLUDE_TAG_MUTED = "includeTagMuted";
    public static final String INCLUDE_TAG_DELETED = "includeTagDeleted";
    public static final String CLASS_CONVERSATION_SEARCH_RESPONSE = "ConversationSearchResponse";
    public static final String CLASS_MESSAGE_SEARCH_RESPONSE = "MessageSearchResponse";
    public static final String SEARCH_HITS = "searchHits";
    public static final String CLASS_SEARCH_RESPONSE = "SearchResponse";
    public static final String QUERY_INFOS = "queryInfos";
    public static final String TOTAL_SIZE = "totalSize";
    public static final String QUERY_MORE = "queryMore";
    public static final String QUERY_OFFSET = "queryOffset";
    public static final String CLASS_CALENDAR_TIME_ZONE_INFO = "CalTZInfo";
    public static final String ID = "id";
    public static final String IDS = "ids";
    public static final String TIME_ZONE_STANDARD_OFFSET = "tzStdOffset";
    public static final String TIME_ZONE_DAY_OFFSET = "tzDayOffset";
    public static final String STANDARD_TIME_ZONE_ONSET = "standardTzOnset";
    public static final String DAYLIGHT_TIME_ZONE_ONSET = "daylightTzOnset";
    public static final String STANDARD_TIME_ZONE_NAME = "standardTZName";
    public static final String DAYLIGHT_TIME_ZONE_NAME = "daylightTZName";
    public static final String CONVERSATION_HIT_INFO = "ConversationHitInfo";
    public static final String SORT_FIELD = "sortField";
    public static final String MESSAGE_HITS = "messageHits";
    public static final String CLASS_CONVERSATION_MESSAGE_HIT_INFO = "ConversationMsgHitInfo";
    public static final String SIZE = "size";
    public static final String AUTO_SEND_TIME = "autoSendTime";
    public static final String DATE = "date";
    public static final String CLASS_CONVERSATION_SUMMARY = "ConversationSummary";
    public static final String NUM = "num";
    public static final String NUM_UNREAD = "numUnread";
    public static final String TAGS = "tags";
    public static final String TAG_NAMES = "tagNames";
    public static final String ELIDED = "elided";
    public static final String CHANGE_DATE = "changeDate";
    public static final String METADATAS = "metadatas";
    public static final String EMAILS = "emails";
    public static final String CLASS_EMAIL_INFO = "EmailInfo";
    public static final String MODIFIED_FIELD_SEQUENCE = "modifiedSequence";
    public static final String DISPLAY = "display";
    public static final String GROUP = "group";
    public static final String IS_GROUP_MEMBERS_EXPANDABLE = "isGroupMembersExpandable";
    public static final String CLASS_MAIL_CUSTOM_METADATA = "MailCustomMetadata";
    public static final String SECTION = "section";
    public static final String FOLDER = "folder";
    public static final String CONVERSATION_ID = "conversationId";
    public static final String REVISION = "revision";
    public static final String MODIFIED_SEQUENCE = "modifiedSequence";
    public static final String CLASS_MESSAGE_HIT_INFO = "MessageHitInfo";
    public static final String CONTENT_MATCHED = "contentMatched";
    public static final String MESSAGE_PART_HITS = "messagePartHits";
    public static final String IMAP_UID = "imapUid";
    public static final String CALENDAR_INTENDED_FOR = "calendarIntendedFor";
    public static final String ORIG_ID = "origId";
    public static final String DRAFT_REPLY_TYPE = "draftReplyType";
    public static final String DRAFT_ACCOUNT_ID = "draftAccountId";
    public static final String DRAFT_AUTO_SEND_TIME = "draftAutoSendTime";
    public static final String SENT_DATE = "sentDate";
    public static final String RESENT_DATE = "resentDate";
    public static final String PART = "part";
    public static final String MESSAGE_ID_HEADER = "messageIdHeader";
    public static final String MESSAGE_ID = "messageId";
    public static final String CONTENT_ELEMS = "contentElems";
    public static final String CLASS_CURSOR_INFO = "CursorInfo";
    public static final String SORT_VAL = "sortVal";
    public static final String END_SORT_VAL = "endSortVal";
    public static final String INCLUDE_OFFSET = "includeOffset";
    public static final String CLASS_TZ_ONSET_INFO = "TzOnsetInfo";
    public static final String WEEK = "week";
    public static final String DAY_OF_WEEK = "dayOfWeek";
    public static final String MONTH = "month";
    public static final String DAY_OF_MONTH = "dayOfMonth";
    public static final String HOUR = "hour";
    public static final String MINUTE = "minute";
    public static final String SECOND = "second";
    public static final String CLASS_INCLUDE_RECIPS_SETTING = "IncludeRecipsSettings";

    // contacts constants
    public static final String CLASS_MODIFY_CONTACT_GROUP_MEMBER = "ModifyContactGroupMember";
    public static final String CLASS_MODIFY_CONTACT_ATTRIBUTE = "ModifyContactAttribute";
    public static final String CLASS_MODIFY_CONTACT_SPEC = "ModifyContactSpec";
    public static final String CLASS_CONTACT_GROUP_MEMBER = "ContactGroupMember";
    public static final String CLASS_CONTACT_ATTRIBUTE = "ContactAttribute";
    public static final String CLASS_CONTACT_INFO = "ContactInfo";
    public static final String CLASS_ACTION_RESULT = "ActionResult";
    public static final String CLASS_NEW_CONTACT_GROUP_MEMBER = "NewContactGroupMember";
    public static final String CLASS_VCARD_INFO = "VCardInfo";
    public static final String CLASS_CONTACT_SPEC = "ContactSpec";
    public static final String CLASS_NEW_CONTACT_ATTRIBUTE = "NewContactAttribute";
    public static final String CLASS_GET_CONTACTS_REQUEST = "GetContactsRequest";
    public static final String MEMBER_ATTRIBUTES = "memberAttributes";
    public static final String CONTACT = "contact";
    public static final String CONTACTS = "contacts";
    public static final String CONTACT_GROUP_MEMBERS = "contactGroupMembers";
    public static final String DO_SYNC = "doSync";
    public static final String DO_DEREF_GROUP_MEMBER = "doDerefGroupMember";
    public static final String INCLUDE_HIDDEN_ATTRS = "includeHiddenAttrs";
    public static final String INCLUDE_CERT_INFO = "includeCertInfo";
    public static final String INCLUDE_IMAP_UID = "includeImapUid";
    public static final String INCLUDE_MODIFIED_SEQUENCE = "includeModifiedSequence";
    public static final String MAX_MEMBERS = "maxMembers";
    public static final String VCARD = "vcard";
    public static final String TYPE = "type";
    public static final String NON_EXISTENT_IDS = "nonExistentIds";
    public static final String DO_VERBOSE = "doVerbose";
    public static final String CONSTRAINT = "constraint";
    public static final String INPUT = "input";
    public static final String DO_REPLACE = "doReplace";
    public static final String IS_EXPANDABLE = "isExpandable";
    public static final String LAST_MODIFIED = "lastModified";
    public static final String FILE_AS = "fileAs";
    public static final String EMAIL = "email";
    public static final String EMAIL2 = "email2";
    public static final String EMAIL3 = "email3";
    public static final String DLIST = "dlist";
    public static final String REFERENCE = "reference";
    public static final String IS_TOO_MANY_MEMBERS = "isTooManyMembers";
    public static final String CONTENT_TYPE = "contentType";
    public static final String CONTENT_FILENAME = "contentFilename";
    public static final String OPERATION = "operation";

    // account preference constants
    public static final String CLASS_GET_PREFS_REQUEST= "GetPrefsRequest";
    public static final String CLASS_ACCOUNT_PREF_INPUT = "AccountPref";
    public static final String PREFERENCES = "preferences";
}
