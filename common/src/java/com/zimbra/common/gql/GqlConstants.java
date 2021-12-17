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

    // new folder spec constants
    public static final String CLASS_NEW_FOLDER_SPEC = "NewFolderSpec";
    public static final String VIEW = "view";
    public static final String URL = "url";
    public static final String FETCH_IF_EXISTS = "fetchIfExists";
    public static final String SYNC_TO_URL = "syncToUrl";
    public static final String GRANTS = "grants";

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
    public static final String CLASS_ACCOUNT_CONTACT_GROUP_MEMBER = "AccountContactGroupMember";
    public static final String CLASS_MAIL_CONTACT_GROUP_MEMBER = "MailContactGroupMember";
    public static final String CLASS_CONTACT_ATTRIBUTE = "ContactAttribute";
    public static final String CLASS_ACCOUNT_CONTACT_INFO = "AccountContactInfo";
    public static final String CLASS_MAIL_CONTACT_INFO = "MailContactInfo";
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

    // session constants
    public static final String CLASS_SESSION_INFO = "SessionInfo";
    public static final String SESSION_ID = "sessionId";
    public static final String BROWSER_INFO = "browserInfo";
    public static final String CREATED_DATE = "createdDate";
    public static final String LAST_ACCESSED = "lastAccessed";
    public static final String USER_AGENT = "userAgent";
    public static final String REQUEST_IP_ADDRESS = "requestIPAddress";
    public static final String CLEAR_ALL_SOAP_SESSIONS = "clearAllSoapSessions";
    public static final String EXCLUDE_CURRENT_SESSION = "excludeCurrentSession";

    // get info constants
    public static final String CLASS_GET_INFO_REQUEST = "GetInfoRequest";
    public static final String CLASS_GET_INFO_RESPONSE = "GetInfoResponse";
    public static final String ENUM_INFO_SECTION = "InfoSection";
    public static final String SECTIONS = "sections";
    public static final String RIGHTS = "rights";
    public static final String ATTACHMENT_SIZE_LIMIT = "attachmentSizeLimit";
    public static final String DOCUMENT_SIZE_LIMIT = "documentSizeLimit";
    public static final String IS_SPELL_CHECK_ENABLED = "isSpellCheckEnabled";
    public static final String VERSION = "version";
    public static final String ACCOUNT_ID = "accountId";
    public static final String PROFILE_IMAGE_ID = "profileImageId";
    public static final String ACCOUNT_NAME = "accountName";
    public static final String CRUMB = "crumb";
    public static final String LIFETIME = "lifetime";
    public static final String ADMIN_DELEGATED = "adminDelegated";
    public static final String REST_URL = "restUrl";
    public static final String QUOTA_USED = "quotaUsed";
    public static final String PREVIOUS_SESSION_TIME = "previousSessionTime";
    public static final String LAST_WRITE_ACCESS_TIME = "lastWriteAccessTime";
    public static final String RECENT_MESSAGE_COUNT = "recentMessageCount";
    public static final String CLASS_COS= "Cos";
    public static final String CLASS_ACCOUNT_ZIMLET_INFO = "AccountZimletInfo";
    public static final String CLASS_ACCOUNT_ZIMLET_CONTEXT = "AccountZimletContext";
    public static final String ZIMLET_BASE_URL = "zimletBaseUrl";
    public static final String ZIMLET_PRIORITY = "zimletPriority";
    public static final String ZIMLET_PRESENCE = "zimletPresence";
    public static final String CLASS_ACCOUNT_ZIMLET_DESC = "AccountZimletDesc";
    public static final String DESCRIPTION = "description";
    public static final String EXTENSION = "extension";
    public static final String TARGET = "target";
    public static final String LABEL = "label";
    public static final String ELEMENTS = "elements";
    public static final String CLASS_ACCOUNT_ZIMLET_CONFIG_INFO = "AccountZimletConfigInfo";
    public static final String CLASS_ACCOUNT_ZIMLET_GLOBAL_CONFIG_INFO = "AccountZimletGlobalConfigInfo";
    public static final String CLASS_ACCOUNT_ZIMLET_PROPERTY = "AccountZimletProperty";
    public static final String CLASS_ACCOUNT_ZIMLET_HOST_CONFIG_INFO = "AccountZimletHostConfigInfo";
    public static final String CLASS_PREF = "Pref";
    public static final String PREFS = "prefs";
    public static final String CLASS_ATTR = "Attr";
    public static final String ATTRS = "attrs";
    public static final String ZIMLETS = "zimlets";
    public static final String CLASS_PROP = "Prop";
    public static final String PROPS = "props";
    public static final String ZIMLET = "zimlet";
    public static final String IDENTITY = "Identity";
    public static final String CLASS_IDENTITIES = "Identities";
    public static final String CLASS_SIGNATURE = "Signature";
    public static final String CID = "cid";
    public static final String CLASS_SIGNATURE_CONTENT = "SignatureContent";
    public static final String CONTENT_LIST = "contentList";
    public static final String SIGNATURES = "signatures";
    public static final String CLASS_ACCOUNT_DATA_SOURCE = "AccountDataSource";
    public static final String ENABLED = "enabled";
    public static final String IMPORT_ONLY = "importOnly";
    public static final String HOST = "host";
    public static final String PORT = "port";
    public static final String ENUM_ADS_CONNECTION_TYPE_ENUM = "AdsConnectionType";
    public static final String ADS_CONNECTION_TYPE = "adsConnectionType";
    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";
    public static final String POLLING_INTERVAL = "pollingInterval";
    public static final String EMAIL_ADDRESS = "emailAddress";
    public static final String USE_ADDRESS_FOR_FORWARD_REPLY = "useAddressForForwardReply";
    public static final String DEFAULT_SIGNATURE = "defaultSignature";
    public static final String FORWARD_REPLY_SIGNATURE = "forwardReplySignature";
    public static final String FROM_DISPLAY = "fromDisplay";
    public static final String REPLY_TO_ADDRESS = "replyToAddress";
    public static final String REPLY_TO_DISPLAY = "replyToDisplay";
    public static final String IMPORT_CLASS = "importClass";
    public static final String FAILING_SINCE = "failingSince";
    public static final String LAST_ERROR = "lastError";
    public static final String REFRESH_TOKEN = "refreshToken";
    public static final String ENUM_CONNECTION_TYPE = "ConnectionType";
    public static final String DATA_SOURCES = "dataSources";
    public static final String GLOBAL = "global";
    public static final String ZIMLET_GLOBAL_CONFIG_PROPERTIES = "globalConfigProperties";
    public static final String ZIMLET_HOST_CONFIG_PROPERTIES = "hostConfigProperties";
    public static final String ZIMLET_CONTEXT = "zimletContext";
    public static final String ZIMLET_CONFIG = "zimletConfig";
    public static final String ZIMLET_HANDLER_CONFIG = "zimletHandlerConfig";
    public static final String CLASS_CHILD_ACCOUNT = "ChildAccount";
    public static final String CHILD_ACCOUNTS = "childAccounts";
    public static final String IS_VISIBLE = "isVisible";
    public static final String IS_ACTIVE = "isActive";
    public static final String CLASS_DISCOVER_RIGHTS_INFO = "DiscoverRightsInfo";
    public static final String RIGHT = "right";
    public static final String CLASS_DISCOVER_RIGHTS_TARGET = "DiscoverRightsTarget";
    public static final String ENUM_TARGET_TYPE = "TargetType";
    public static final String DISPLAY_NAME = "displayName";
    public static final String CLASS_DISCOVER_RIGHTS_EMAIL = "DiscoverRightsEmail";
    public static final String ADDR = "addr";
    public static final String TARGETS = "targets";
    public static final String DISCOVERED_RIGHTS = "discoveredRights";
    public static final String CLASS_LICENSE_INFO = "LicenseInfo";
    public static final String STATUS = "status";
    public static final String CLASS_LICENSE_ATTR = "LicenseAttr";
    public static final String LICENSE = "license";
    public static final String IS_TRACKING_IMAP = "isTrackingIMAP";
    public static final String CLASS_ATTRS_IMPL = "AttrsImpl";
    public static final String CLASS_ZIMLET_SERVER_EXTENSION = "ZimletServerExtension";
    public static final String HAS_KEYWORD = "hasKeyword";
    public static final String EXTENSION_CLASS = "extensionClass";
    public static final String REGEX = "regex";
    public static final String CLASS_ACCOUNT_ZIMLET_INCLUDE = "AccountZimletInclude";
    public static final String CLASS_ACCOUNT_ZIMLET_INCLUDE_CSS = "AccountZimletIncludeCSS";
    public static final String CLASS_ACCOUNT_ZIMLET_TARGET = "AccountZimletTarget";
    public static final String CLASS_ADMIN_ZIMLET_PROPERTY = "AdminZimletProperty";
    public static final String ENUM_LICENSE_STATUS = "LicenseStatus";

    // Change password related constants
    public static final String CLASS_CHANGE_PASSWORD_RESPONSE = "ChangePasswordResponse";
    public static final String OLD_PASSWORD = "oldPassword";
    public static final String NEW_PASSWORD = "newPassword";
    public static final String ACCOUNT_SELECTOR = "accountSelector";
    public static final String AUTH_TOKEN = "authToken";
    public static final String VIRTUAL_HOST = "virtualHost";

    public static final String CLASS_OPERATION_VALUE = "OperationValue";
    public static final String CLASS_WHITE_BLACK_LIST_RESPONSE = "WhiteBlackListResponse";
    public static final String WHITE_LIST_ENTRIES = "whiteListEntries";
    public static final String BLACK_LIST_ENTRIES = "blackListEntries";

    public static final String CLASS_NAME_ID = "NameId";
    public static final String IDENTIFIER = "identifier";
    public static final String SIGNATURE = "signature";

    // auto complete constants
    public static final String CLASS_AUTO_COMPLETE_RESPONSE = "AutoCompleteResponse";
    public static final String CLASS_AUTO_COMPLETE_MATCH = "AutoCompleteMatch";
    public static final String CLASS_SEARCH_TYPE = "SearchType";
    public static final String RANKING = "ranking";
    public static final String IS_GROUP = "isGroup";
    public static final String FIRST_NAME = "firstName";
    public static final String MIDDLE_NAME = "middleName";
    public static final String LAST_NAME = "lastName";
    public static final String NICKNAME = "nickname";
    public static final String COMPANY = "company";
    public static final String FOLDERS = "folders";
    public static final String INCLUDE_GAL = "includeGal";
    public static final String IS_CACHEABLE = "isCacheable";
    public static final String MATCHES = "matches";

    public static final String STORE_LOOKUP_OPT = "storeLookupOption";
    public static final String SOURCE_LOOKUP_OPT = "sourceLookupOption";
    public static final String STORE_TYPES = "storeTypes";
    public static final String STORE_TYPE = "storeType";
    public static final String CERTIFICATES = "certificates";
    public static final String CLASS_STORE_LOOKUP_OPT_TYPE = "StoreLookupOptionType";
    public static final String CLASS_SOURCE_LOOKUP_OPT_TYPE = "SourceLookupOptionType";
    public static final String CLASS_SMIME_STORE_TYPE = "SMIMEStoreType";
    public static final String CLASS_SMIME_PUBLIC_CERTS_INFO = "SMIMEPublicCertsInfo";
    public static final String CLASS_SMIME_PUBLIC_CERT_INFO = "SMIMEPublicCertInfo";

    // calendar constants
    public static final String CLASS_CALENDAR_ACTION_RESPONSE = "CalendarActionResponse";
    public static final String CLASS_CREATE_APPOINTMENT_RESPONSE = "CreateAppointmentResponse";
    public static final String CLASS_CREATE_APPOINTMENT_EXCEPTION_RESPONSE = "CreateAppointmentExceptionResponse";
    public static final String CLASS_MODIFY_APPOINTMENT_RESPONSE = "ModifyAppointmentResponse";
    public static final String CLASS_REPLY_TO_INVITE_RESPONSE = "ReplyToInviteResponse";
    public static final String CLASS_INVITE_REPLY_VERB_INPUT = "InviteReplyVerb";
    public static final String CLASS_DT_TIME_INFO = "DtTimeInfo";
    public static final String CLASS_RAW_INVITE = "RawInvite";
    public static final String CLASS_GEO_INFORMATION = "GeoInformation";
    public static final String CLASS_CALENDAR_ATTENDEE = "CalendarAttendee";
    public static final String CLASS_XPARAM = "XParam";
    public static final String CLASS_XPROP = "XProp";
    public static final String CLASS_ALARM_INFORMATION = "AlarmInformation";
    public static final String CLASS_ALARM_TRIGGER_INFORMATION = "AlarmTriggerInformation";
    public static final String CLASS_DATE_ATTR = "DateAttr";
    public static final String CLASS_DURATION_INFORMATION = "DurationInformation";
    public static final String CLASS_CALENDAR_ATTACHMENT = "CalendarAttachment";
    public static final String CLASS_CALENDAR_ORGANIZER = "CalendarOrganizer";
    public static final String CLASS_EXCEPTION_RECURRENCE_ID_INFORMATION = "ExceptionRecurrenceIdInformation";
    public static final String CLASS_RECURRENCE_INFORMATION = "RecurrenceInformation";
    public static final String CLASS_INVITE_COMPONENT_COMMON = "InviteComponentCommon";
    public static final String CLASS_EXCEPTION_RULE_INFORMATION = "ExceptionRuleInformation";
    public static final String CLASS_RECURRENCE_ID_INFORMATION = "RecurrenceIdInformation";
    public static final String CLASS_SINGLE_DATES = "SingleDates";
    public static final String CLASS_DATE_TIME_VALUE = "DateTimeValue";
    public static final String CLASS_HEADER = "Header";
    public static final String CLASS_SIMPLE_REPEATING_RULE = "SimpleRepeatingRule";
    public static final String CLASS_DATE_TIME_STRING_ATTRIBUTE = "DateTimeStringAttribute";
    public static final String CLASS_NUMBER_ATTRIBUTE = "NumberAttribute";
    public static final String CLASS_INTERVAL_RULE = "IntervalRule";
    public static final String CLASS_BY_SECOND_RULE = "BySecondRule";
    public static final String CLASS_BY_MINUTE_RULE = "ByMinuteRule";
    public static final String CLASS_BY_HOUR_RULE = "ByHourRule";
    public static final String CLASS_BY_DAY_RULE = "ByDayRule";
    public static final String CLASS_BY_MONTH_DAY_RULE = "ByMonthDayRule";
    public static final String CLASS_BY_YEAR_DAY_RULE = "ByYearDayRule";
    public static final String CLASS_BY_WEEK_NO_RULE = "ByWeekNoRule";
    public static final String CLASS_BY_MONTH_RULE = "ByMonthRule";
    public static final String CLASS_BY_SET_POS_RULE = "BySetPosRule";
    public static final String CLASS_WEEK_START_RULE = "WeekStartRule";
    public static final String CLASS_XNAME_RULE = "XNameRule";
    public static final String CLASS_INVITE_AS_MP = "InviteAsMP";
    public static final String CLASS_MP_INVITE_INFORMATION = "MPInviteInformation";
    public static final String CLASS_CALENDAR_REPLY = "CalendarReply";
    public static final String UID = "uid";
    public static final String SUMMARY = "summary";
    public static final String UTC_TIME = "utcTime";
    public static final String DATE_TIME = "dateTime";
    public static final String MAX_SIZE = "maxSize";
    public static final String COMPONENT_NUMBER = "componentNumber";
    public static final String INCLUDE_ECHO = "includeEcho";
    public static final String ECHO = "echo";
    public static final String DO_NEUTER = "doNeuter";
    public static final String DO_FORCE = "doForce";
    public static final String INSTANCE = "instance";
    public static final String TIMEZONE = "timezone";
    public static final String EXCEPTION_ID = "exceptionId";
    public static final String VERB = "verb";
    public static final String DO_UPDATE_ORGANIZER = "doUpdateOrganizer";
    public static final String CALENDAR_ITEM_ID = "calendarItemId";
    public static final String CALENDAR_INVITE_ID = "calendarInviteId";
    public static final String CATEGORIES = "categories";
    public static final String COMMENTS = "comments";
    public static final String GEO = "geo";
    public static final String ATTENDEES = "attendees";
    public static final String ALARMS = "alarms";
    public static final String XPROPS = "xProps";
    public static final String HTML_DESCRIPTION = "htmlDescription";
    public static final String ORGANIZER = "organizer";
    public static final String RECURRENCE = "recurrence";
    public static final String START_DATE = "startDate";
    public static final String END_DATE = "endDate";
    public static final String DURATION = "duration";
    public static final String LATITUDE = "latitude";
    public static final String LONGITUDE = "longitude";
    public static final String SENT_BY = "sentBy";
    public static final String DIRECTORY = "directory";
    public static final String LANGUAGE = "language";
    public static final String CALENDAR_USER_TYPE = "calendarUserType";
    public static final String ROLE = "role";
    public static final String PARTICIPATION_STATUS = "participationStatus";
    public static final String RSVP = "rsvp";
    public static final String MEMBER = "member";
    public static final String DELEGATED_TO = "delegatedTo";
    public static final String DELEGATED_FROM = "delegatedFrom";
    public static final String XPARAMS = "xParams";
    public static final String ACTION = "action";
    public static final String TRIGGER = "trigger";
    public static final String REPEAT = "repeat";
    public static final String ATTACHMENT = "attachment";
    public static final String ABSOLUTE = "absolute";
    public static final String RELATIVE = "relative";
    public static final String IS_NEGATIVE = "isNegative";
    public static final String WEEKS = "weeks";
    public static final String DAYS = "days";
    public static final String HOURS = "hours";
    public static final String MINUTES = "minutes";
    public static final String SECONDS = "seconds";
    public static final String RELATED = "related";
    public static final String REPEAT_COUNT = "repeatCount";
    public static final String URI = "uri";
    public static final String BINARY_64_DATA = "binary64Data";
    public static final String RECURRENCE_RANGE_TYPE = "recurrenceRangeType";
    public static final String RULES = "rules";
    public static final String METHOD = "method";
    public static final String PRIORITY = "priority";
    public static final String LOCATION = "location";
    public static final String PERCENT_COMPLETE = "percentComplete";
    public static final String COMPLETED = "completed";
    public static final String NO_BLOB = "noBlob";
    public static final String FREE_BUSY_ACTUAL = "freeBusyActual";
    public static final String FREE_BUSY = "freeBusy";
    public static final String TRANSPARENCY = "transparency";
    public static final String IS_ORGANIZER = "isOrganizer";
    public static final String XUID = "xUid";
    public static final String CALENDAR_ITEM_FOLDER = "calendarItemFolder";
    public static final String CALENDAR_ITEM_TYPE = "calendarItemType";
    public static final String CALENDAR_CLASS = "calendarClass";
    public static final String IS_EXCEPTION = "isException";
    public static final String RECURRENCE_ID_Z = "recurrenceIdZ";
    public static final String IS_ALL_DAY = "isAllDay";
    public static final String IS_DRAFT = "isDraft";
    public static final String IS_NEVER_SENT = "isNeverSent";
    public static final String CHANGES = "changes";
    public static final String SEQUENCE = "sequence";
    public static final String RULES_ADD = "rulesAdd";
    public static final String RULES_EXCLUDE = "rulesExclude";
    public static final String RULES_EXCEPT = "rulesExcept";
    public static final String RULES_CANCEL = "rulesCancel";
    public static final String RULES_DATES = "rulesDates";
    public static final String RULES_SIMPLE = "rulesSimple";
    public static final String ADD = "add";
    public static final String EXCLUDE = "exclude";
    public static final String RECURRENCE_ID = "recurrenceId";
    public static final String DATE_TIME_VALUES = "dateTimeValues";
    public static final String UNTIL = "until";
    public static final String COUNT = "count";
    public static final String INTERVAL = "interval";
    public static final String BY_SECOND = "bySecond";
    public static final String BY_MINUTE = "byMinute";
    public static final String BY_HOUR = "byHour";
    public static final String BY_DAY = "byDay";
    public static final String BY_MONTH_DAY = "byMonthDay";
    public static final String BY_YEAR_DAY = "byYearDay";
    public static final String BY_WEEK_NO = "byWeekNo";
    public static final String BY_MONTH = "byMonth";
    public static final String WEEK_START = "weekStart";
    public static final String BY_SET_POS = "bySetPos";
    public static final String X_NAMES = "xNames";
    public static final String FREQUENCY = "frequency";
    public static final String LIST = "list";
    public static final String DAY = "day";
    public static final String CALENDAR_REPLIES = "calendarReplies";
    public static final String INVITE_COMPONENTS = "inviteComponents";
    public static final String ATTENDEE = "attendee";

    // free busy user
    public static final String CLASS_FREE_BUSY_USER_SPEC = "FreeBusyUserSpec";
    public static final String CLASS_FREE_BUSY_USER_INFORMATION = "FreeBusyUserInformation";
    public static final String CLASS_FREE_BUSY_SLOT = "FreeBusySlot";
    public static final String START_TIME = "startTime";
    public static final String END_TIME = "endTime";
    public static final String EXCLUDE_UID = "excludeUid";
    public static final String FREE_BUSY_USERS = "freeBusyUsers";

    //GAL Search
    public static final String CLASS_BASIC_SEARCH_REQUEST = "BasicSearchInput";
    public static final String CLASS_GAL_SEARCH_REQUEST = "GALSearch";
    public static final String CLASS_GAL_SEARCH_RESPONSE = "GALSearchResponse";
    public static final String CLASS_SEARCH_BY = "SearchBy";
    public static final String CLASS_MEMBER_OF_SELECTOR = "MemberOfSelector";
    public static final String CLASS_SEARCH_FILTER = "SearchFilter";
    public static final String CLASS_SEARCH_FILTER_MULTIPLE_CONDITIONS = "SearchFilterMultipleConditions";
    public static final String CLASS_SEARCH_FILTER_SINGLE_CONDITION = "SearchFilterSingleCondition";
    public static final String CLASS_ACCOUNT_CUSTOM_METADATA = "AccountCustomMetadata";
    public static final String SEARCH_BY = "searchBy";
    public static final String SEARCH_TYPE = "searchType";
    public static final String INCLUDE_IS_OWNER = "includeIsOwner";
    public static final String INCLUDE_IS_MEMBER = "includeIsMember";
    public static final String INCLUDE_SMIME_CERTS = "includeSMIMECerts";
    public static final String GAL_ACCOUNT_ID = "galAccountId";
    public static final String SEARCH_FILTER = "searchFilter";
    public static final String SINGLE_CONDITION = "singleCondition";
    public static final String MULTIPLE_CONDITION = "multipleCondition";
    public static final String SEARCH_FILTER_OR = "or";
    public static final String SEARCH_FILTER_NEGATION = "not";
    public static final String SEARCH_FILTER_ATTR = "attr";
    public static final String SEARCH_FILTER_OP = "operation";
    public static final String SEARCH_FILTER_VALUE = "value";
    public static final String PAGINATION_SUPPORTED = "paginationSupported";
    public static final String TOKENIZE_KEY = "tokenizeKey";
    public static final String IS_OWNER = "isOwner";
    public static final String IS_MEMBER = "isMember";

    //Task Constants
    public static final String CLASS_CREATE_TASK_RESPONSE = "CreateTaskResponse";
    public static final String CLASS_CREATE_TASK_EXCEPTION_RESPONSE = "CreateTaskExceptionResponse";
    public static final String CLASS_MODIFY_TASK_RESPONSE = "ModifyTaskResponse";

    //Mountpoint constants
    public static final String CLASS_MOUNTPOINT = "Mountpoint";
    public static final String CLASS_OWNER_SELECTOR = "OwnerSelector";
    public static final String CLASS_FOLDER_SELECTOR = "FolderSelector";
    public static final String OWNER_SELECTOR = "ownerSelector";
    public static final String REMOTE_FOLDER_SELECTOR = "remoteFolderSelector";
    public static final String OWNER = "owner";
    public static final String REMOTE_FOLDER = "remoteFolder";
    public static final String REMINDER_ENABLED = "reminderEnabled";
    public static final String OWNER_EMAIL = "ownerEmail";
    public static final String OWNER_ACCOUNT_ID = "ownerAccountId";
    public static final String REMOTE_FOLDER_ID = "remoteFolderId";
    public static final String OWNER_FOLDER_NAME = "ownerFolderName";
    public static final String IS_BROKEN = "isBroken";
    public static final String REMOTE_UUID = "remoteUUID";

    //sharing constants
    public static final String CLASS_ACTION = "Action";
    public static final String ITEM_ID = "itemId";
    public static final String EMAIL_ADDRESSES = "emailAddresses";
    public static final String NOTES = "notes";
    public static final String INTERNAL = "internal";
    public static final String INCLUDE_SELF = "includeSelf";
    public static final String GRANTEE = "grantee";
    public static final String CLASS_GRANTEE_SELECTOR = "GranteeSelector";
    public static final String CLASS_SHARE_INFO = "ShareInfo";
    public static final String OWNER_ID = "ownerId";
    public static final String OWNER_NAME = "ownerName";
    public static final String FOLDER_UUID = "folderUuid";
    public static final String FOLDER_PATH = "folderPath";
    public static final String GRANTEE_TYPE = "granteeType";
    public static final String GRNATEE_NAME = "granteeName";
    public static final String GRANTEE_DISPLAY_NAME = "granteeDisplayName";
    public static final String MOUNTPOINT_ID = "mountpointId";

    // auth
    public static final String CLASS_ADMIN_AUTH_REQUEST = "AdminAuthRequest";
    public static final String CLASS_AUTH_RESPONSE = "AuthResponse";
    public static final String ACCOUNT = "account";
    public static final String TWO_FACTOR_CODE = "twoFactorCode";
    public static final String DO_PERSIST_COOKIE = "doPersistCookie";
    public static final String IS_CSRF_SUPPORTED = "isCsrfSupported";
    public static final String CSRF_TOKEN = "csrfToken";

    // GetAccount - admin api
    public static final String APPLY_COS = "applyCos";
}
