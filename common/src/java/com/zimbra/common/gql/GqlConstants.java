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
    public static final String ATTRS = "attrs";
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

    // search constants
    public static final String CLASS_SEARCH_REQUEST = "SearchRequest";
    public static final String SEARCH_PARAMS = "params";
    public static final String HEADERS = "headers";
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
    public static final String WANT_RECEPIENTS = "wantRecipients";
    public static final String NEUTER_IMAGES = "neuterImages";
    public static final String NEED_CAN_EXPAND = "needCanExpand";
    public static final String WANT_HTML = "wantHtml";
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
    public static final String FOLDER_ID = "folderId";
    public static final String AUTO_SEND_TIME = "autoSendTime";
    public static final String DATE = "date";
    public static final String CLASS_CONVERSATION_SUMMARY = "ConversationSummary";
    public static final String NUM = "num";
    public static final String NUM_UNREAD = "numUnread";
    public static final String TAGS = "tags";
    public static final String TAG_NAMES = "tagNames";
    public static final String ELIDED = "elided";
    public static final String CHANGE_DATE = "changeDate";
    public static final String MODIFIED_FIELD_SEQUENCE = "modifiedSequence";
    public static final String METADATAS = "metadatas";
    public static final String SUBJECT = "subject";
    public static final String FRAGMENT = "fragment";
    public static final String EMAILS = "emails";
    public static final String CLASS_EMAIL_INFO = "EmailInfo";
    public static final String ADDRESS = "address";
    public static final String DISPLAY = "display";
    public static final String PERSONAL = "personal";
    public static final String ADDRESS_TYPE = "addressType";
    public static final String GROUP = "group";
    public static final String CAN_EXPAND_GROUP_MEMBERS = "canExpandGroupMembers";
    public static final String CLASS_MAIL_CUSTOM_METADATA = "MailCustomMetadata";
    public static final String SECTION = "section";
    public static final String FOLDER = "folder";
    public static final String CONVERSATION_ID = "conversationId";
    public static final String REVISION = "revision";
    public static final String MODIFIED_SEQUENCE = "modifiedSequence";
    public static final String CLASS_MESSAGE_HIT_INFO = "MessageHitInfo";
    public static final String CONTENT_MATCHED = "contentMatched";
    public static final String MESSAGE_PART_HITS = "messagePartHits";
    public static final String CLASS_MESSAGE_INFO = "MessageInfo";
    public static final String IMAP_UID = "imapUid";
    public static final String CALENDAR_INTENDED_FOR = "calendarIntendedFor";
    public static final String ORIG_ID = "origId";
    public static final String DRAFT_REPLY_TYPE = "draftReplyType";
    public static final String IDENTITY_ID = "identityId";
    public static final String DRAFT_ACCOUNT_ID = "draftAccountId";
    public static final String DRAFT_AUTO_SEND_TIME = "draftAutoSendTime";
    public static final String SENT_DATE = "sentDate";
    public static final String RESENT_DATE = "resentDate";
    public static final String PART = "part";
    public static final String MESSAGE_ID_HEADER = "messageIdHeader";
    public static final String IN_REPLY_TO = "inReplyTo";
    public static final String INVITE = "invite";
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


}
