/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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
package com.zimbra.common.soap;

import org.dom4j.Namespace;
import org.dom4j.QName;

public final class MailConstants {

    public static final class ShareConstants {
        public static final String NAMESPACE_STR = "urn:zimbraShare";
        public static final String VERSION = "0.2";

        public static final String E_SHARE = "share";
        public static final String E_REVOKE = "revoke";
        public static final String E_GRANTEE = "grantee";
        public static final String E_GRANTOR = "grantor";
        public static final String E_LINK = "link";
        public static final String E_NOTES = "notes";

        public static final String A_VERSION = "version";
        public static final String A_ACTION = "action";
        public static final String A_ID = "id";
        public static final String A_EMAIL = "email";
        public static final String A_NAME = "name";
        public static final String A_VIEW = "view";
        public static final String A_PERM = "perm";
        public static final String A_EXPIRE = "expire";

        public static final String ACTION_NEW = "new";
        public static final String ACTION_EDIT = "edit";

        public static final Namespace NAMESPACE = Namespace.get(NAMESPACE_STR);
        public static final QName SHARE = QName.get(E_SHARE, NAMESPACE);
        public static final QName REVOKE = QName.get(E_REVOKE, NAMESPACE);
    }

    private MailConstants() {
    }

    public static final String NAMESPACE_STR = "urn:zimbraMail";
    public static final Namespace NAMESPACE = Namespace.get(NAMESPACE_STR);

    public static final String E_NO_OP_REQUEST = "NoOpRequest";
    public static final String E_NO_OP_RESPONSE = "NoOpResponse";
    public static final String E_GENERATE_UUID_REQUEST = "GenerateUUIDRequest";
    public static final String E_GENERATE_UUID_RESPONSE = "GenerateUUIDResponse";
    public static final String E_SEARCH_REQUEST = "SearchRequest";
    public static final String E_SEARCH_RESPONSE = "SearchResponse";
    public static final String E_SEARCH_CONV_REQUEST = "SearchConvRequest";
    public static final String E_SEARCH_CONV_RESPONSE = "SearchConvResponse";
    public static final String E_REJECT_SAVE_SEARCH_PROMPT_REQUEST = "RejectSaveSearchPromptRequest";
    public static final String E_REJECT_SAVE_SEARCH_PROMPT_RESPONSE = "RejectSaveSearchPromptResponse";
    public static final String E_CLEAR_SEARCH_HISTORY_REQUEST = "ClearSearchHistoryRequest";
    public static final String E_CLEAR_SEARCH_HISTORY_RESPONSE = "ClearSearchHistoryResponse";
    public static final String E_SEARCH_SUGGEST_REQUEST = "SearchSuggestRequest";
    public static final String E_SEARCH_SUGGEST_RESPONSE = "SearchSuggestResponse";
    public static final String E_GET_SEARCH_HISTORY_REQUEST = "GetSearchHistoryRequest";
    public static final String E_GET_SEARCH_HISTORY_RESPONSE = "GetSearchHistoryResponse";
    public static final String E_BROWSE_REQUEST = "BrowseRequest";
    public static final String E_BROWSE_RESPONSE = "BrowseResponse";
    public static final String E_EMPTY_DUMPSTER_REQUEST = "EmptyDumpsterRequest";
    public static final String E_EMPTY_DUMPSTER_RESPONSE = "EmptyDumpsterResponse";
    public static final String E_GET_ITEM_REQUEST = "GetItemRequest";
    public static final String E_GET_ITEM_RESPONSE = "GetItemResponse";
    public static final String E_ITEM_ACTION_REQUEST = "ItemActionRequest";
    public static final String E_ITEM_ACTION_RESPONSE = "ItemActionResponse";
    public static final String E_GET_METADATA_REQUEST = "GetCustomMetadataRequest";
    public static final String E_GET_METADATA_RESPONSE = "GetCustomMetadataResponse";
    public static final String E_SET_METADATA_REQUEST = "SetCustomMetadataRequest";
    public static final String E_SET_METADATA_RESPONSE = "SetCustomMetadataResponse";
    public static final String E_GET_MAILBOX_METADATA_REQUEST = "GetMailboxMetadataRequest";
    public static final String E_GET_MAILBOX_METADATA_RESPONSE = "GetMailboxMetadataResponse";
    public static final String E_SET_MAILBOX_METADATA_REQUEST = "SetMailboxMetadataRequest";
    public static final String E_SET_MAILBOX_METADATA_RESPONSE = "SetMailboxMetadataResponse";
    public static final String E_MODIFY_MAILBOX_METADATA_REQUEST = "ModifyMailboxMetadataRequest";
    public static final String E_MODIFY_MAILBOX_METADATA_RESPONSE = "ModifyMailboxMetadataResponse";
    public static final String E_GET_CONV_REQUEST = "GetConvRequest";
    public static final String E_GET_CONV_RESPONSE = "GetConvResponse";
    public static final String E_CONV_ACTION_REQUEST = "ConvActionRequest";
    public static final String E_CONV_ACTION_RESPONSE = "ConvActionResponse";
    public static final String E_GET_MSG_REQUEST = "GetMsgRequest";
    public static final String E_GET_MSG_RESPONSE = "GetMsgResponse";
    public static final String E_GET_MSG_METADATA_REQUEST = "GetMsgMetadataRequest";
    public static final String E_GET_MSG_METADATA_RESPONSE = "GetMsgMetadataResponse";
    public static final String E_MSG_ACTION_REQUEST = "MsgActionRequest";
    public static final String E_MSG_ACTION_RESPONSE = "MsgActionResponse";
    public static final String E_SEND_MSG_REQUEST = "SendMsgRequest";
    public static final String E_SEND_MSG_RESPONSE = "SendMsgResponse";
    public static final String E_SEND_REPORT_REQUEST = "SendDeliveryReportRequest";
    public static final String E_SEND_REPORT_RESPONSE = "SendDeliveryReportResponse";
    public static final String E_SEND_SHARE_NOTIFICATION_REQUEST = "SendShareNotificationRequest";
    public static final String E_SEND_SHARE_NOTIFICATION_RESPONSE = "SendShareNotificationResponse";
    public static final String E_BOUNCE_MSG_REQUEST = "BounceMsgRequest";
    public static final String E_BOUNCE_MSG_RESPONSE = "BounceMsgResponse";
    public static final String E_ADD_MSG_REQUEST = "AddMsgRequest";
    public static final String E_ADD_MSG_RESPONSE = "AddMsgResponse";
    public static final String E_SAVE_DRAFT_REQUEST = "SaveDraftRequest";
    public static final String E_SAVE_DRAFT_RESPONSE = "SaveDraftResponse";
    public static final String E_REMOVE_ATTACHMENTS_REQUEST = "RemoveAttachmentsRequest";
    public static final String E_REMOVE_ATTACHMENTS_RESPONSE = "RemoveAttachmentsResponse";
    public static final String E_CREATE_FOLDER_REQUEST = "CreateFolderRequest";
    public static final String E_CREATE_FOLDER_RESPONSE = "CreateFolderResponse";
    public static final String E_GET_FOLDER_REQUEST = "GetFolderRequest";
    public static final String E_GET_FOLDER_RESPONSE = "GetFolderResponse";
    public static final String E_FOLDER_ACTION_REQUEST = "FolderActionRequest";
    public static final String E_FOLDER_ACTION_RESPONSE = "FolderActionResponse";
    public static final String E_CREATE_TAG_REQUEST = "CreateTagRequest";
    public static final String E_CREATE_TAG_RESPONSE = "CreateTagResponse";
    public static final String E_GET_TAG_REQUEST = "GetTagRequest";
    public static final String E_GET_TAG_RESPONSE = "GetTagResponse";
    public static final String E_TAG_ACTION_REQUEST = "TagActionRequest";
    public static final String E_TAG_ACTION_RESPONSE = "TagActionResponse";
    public static final String E_CREATE_SEARCH_FOLDER_REQUEST = "CreateSearchFolderRequest";
    public static final String E_CREATE_SEARCH_FOLDER_RESPONSE = "CreateSearchFolderResponse";
    public static final String E_GET_SEARCH_FOLDER_REQUEST = "GetSearchFolderRequest";
    public static final String E_GET_SEARCH_FOLDER_RESPONSE = "GetSearchFolderResponse";
    public static final String E_MODIFY_SEARCH_FOLDER_REQUEST = "ModifySearchFolderRequest";
    public static final String E_MODIFY_SEARCH_FOLDER_RESPONSE = "ModifySearchFolderResponse";
    public static final String E_CREATE_MOUNTPOINT_REQUEST = "CreateMountpointRequest";
    public static final String E_CREATE_MOUNTPOINT_RESPONSE = "CreateMountpointResponse";
    public static final String E_ENABLE_SHARED_REMINDER_REQUEST = "EnableSharedReminderRequest";
    public static final String E_ENABLE_SHARED_REMINDER_RESPONSE = "EnableSharedReminderResponse";
    public static final String E_CREATE_CONTACT_REQUEST = "CreateContactRequest";
    public static final String E_CREATE_CONTACT_RESPONSE = "CreateContactResponse";
    public static final String E_MODIFY_CONTACT_REQUEST = "ModifyContactRequest";
    public static final String E_MODIFY_CONTACT_RESPONSE = "ModifyContactResponse";
    public static final String E_GET_CONTACTS_REQUEST = "GetContactsRequest";
    public static final String E_GET_CONTACTS_RESPONSE = "GetContactsResponse";
    public static final String E_GET_CONTACT_BACKUP_LIST_REQUEST = "GetContactBackupListRequest";
    public static final String E_GET_CONTACT_BACKUP_LIST_RESPONSE = "GetContactBackupListResponse";
    public static final String E_BACKUPS = "backups";
    public static final String E_BACKUP = "backup";
    public static final String E_IMPORT_CONTACTS_REQUEST = "ImportContactsRequest";
    public static final String E_IMPORT_CONTACTS_RESPONSE = "ImportContactsResponse";
    public static final String E_EXPORT_CONTACTS_REQUEST = "ExportContactsRequest";
    public static final String E_EXPORT_CONTACTS_RESPONSE = "ExportContactsResponse";
    public static final String E_CONTACT_ACTION_REQUEST = "ContactActionRequest";
    public static final String E_CONTACT_ACTION_RESPONSE = "ContactActionResponse";
    public static final String E_CREATE_NOTE_REQUEST = "CreateNoteRequest";
    public static final String E_CREATE_NOTE_RESPONSE = "CreateNoteResponse";
    public static final String E_GET_NOTE_REQUEST = "GetNoteRequest";
    public static final String E_GET_NOTE_RESPONSE = "GetNoteResponse";
    public static final String E_NOTE_ACTION_REQUEST = "NoteActionRequest";
    public static final String E_NOTE_ACTION_RESPONSE = "NoteActionResponse";
    public static final String E_SYNC_REQUEST = "SyncRequest";
    public static final String E_SYNC_RESPONSE = "SyncResponse";
    public static final String E_GET_RULES_REQUEST = "GetRulesRequest";
    public static final String E_GET_RULES_RESPONSE = "GetRulesResponse";
    public static final String E_SAVE_RULES_REQUEST = "SaveRulesRequest";
    public static final String E_SAVE_RULES_RESPONSE = "SaveRulesResponse";
    public static final String E_GET_FILTER_RULES_REQUEST = "GetFilterRulesRequest";
    public static final String E_GET_FILTER_RULES_RESPONSE = "GetFilterRulesResponse";
    public static final String E_MODIFY_FILTER_RULES_REQUEST = "ModifyFilterRulesRequest";
    public static final String E_MODIFY_FILTER_RULES_RESPONSE = "ModifyFilterRulesResponse";
    public static final String E_APPLY_FILTER_RULES_REQUEST = "ApplyFilterRulesRequest";
    public static final String E_APPLY_FILTER_RULES_RESPONSE = "ApplyFilterRulesResponse";
    public static final String E_GET_OUTGOING_FILTER_RULES_REQUEST = "GetOutgoingFilterRulesRequest";
    public static final String E_GET_OUTGOING_FILTER_RULES_RESPONSE = "GetOutgoingFilterRulesResponse";
    public static final String E_MODIFY_OUTGOING_FILTER_RULES_REQUEST = "ModifyOutgoingFilterRulesRequest";
    public static final String E_MODIFY_OUTGOING_FILTER_RULES_RESPONSE = "ModifyOutgoingFilterRulesResponse";
    public static final String E_APPLY_OUTGOING_FILTER_RULES_REQUEST = "ApplyOutgoingFilterRulesRequest";
    public static final String E_APPLY_OUTGOING_FILTER_RULES_RESPONSE = "ApplyOutgoingFilterRulesResponse";
    public static final String E_GET_APPT_SUMMARIES_REQUEST = "GetApptSummariesRequest";
    public static final String E_GET_APPOINTMENT_REQUEST = "GetAppointmentRequest";
    public static final String E_SET_APPOINTMENT_REQUEST = "SetAppointmentRequest";
    public static final String E_CREATE_APPOINTMENT_REQUEST = "CreateAppointmentRequest";
    public static final String E_CREATE_APPOINTMENT_EXCEPTION_REQUEST = "CreateAppointmentExceptionRequest";
    public static final String E_MODIFY_APPOINTMENT_REQUEST = "ModifyAppointmentRequest";
    public static final String E_CANCEL_APPOINTMENT_REQUEST = "CancelAppointmentRequest";
    public static final String E_FORWARD_APPOINTMENT_REQUEST = "ForwardAppointmentRequest";
    public static final String E_FORWARD_APPOINTMENT_INVITE_REQUEST = "ForwardAppointmentInviteRequest";
    public static final String E_ADD_APPOINTMENT_INVITE_REQUEST = "AddAppointmentInviteRequest";
    public static final String E_ADD_APPOINTMENT_INVITE_RESPONSE = "AddAppointmentInviteResponse";
    public static final String E_COUNTER_APPOINTMENT_REQUEST = "CounterAppointmentRequest";
    public static final String E_COUNTER_APPOINTMENT_RESPONSE = "CounterAppointmentResponse";
    public static final String E_DECLINE_COUNTER_APPOINTMENT_REQUEST = "DeclineCounterAppointmentRequest";
    public static final String E_DECLINE_COUNTER_APPOINTMENT_RESPONSE = "DeclineCounterAppointmentResponse";
    public static final String E_IMPORT_APPOINTMENTS_REQUEST = "ImportAppointmentsRequest";
    public static final String E_IMPORT_APPOINTMENTS_RESPONSE = "ImportAppointmentsResponse";
    public static final String E_GET_TASK_SUMMARIES_REQUEST = "GetTaskSummariesRequest";
    public static final String E_GET_TASK_REQUEST = "GetTaskRequest";
    public static final String E_SET_TASK_REQUEST = "SetTaskRequest";
    public static final String E_CREATE_TASK_REQUEST = "CreateTaskRequest";
    public static final String E_CREATE_TASK_EXCEPTION_REQUEST = "CreateTaskExceptionRequest";
    public static final String E_MODIFY_TASK_REQUEST = "ModifyTaskRequest";
    public static final String E_ADD_TASK_INVITE_REQUEST = "AddTaskInviteRequest";
    public static final String E_ADD_TASK_INVITE_RESPONSE = "AddTaskInviteResponse";
    public static final String E_CANCEL_TASK_REQUEST = "CancelTaskRequest";
    public static final String E_COMPLETE_TASK_INSTANCE_REQUEST = "CompleteTaskInstanceRequest";
    public static final String E_GET_CALITEM_SUMMARIES_REQUEST = "GetCalendarItemSummariesRequest";
    public static final String E_SEND_INVITE_REPLY_REQUEST = "SendInviteReplyRequest";
    public static final String E_ICAL_REPLY_REQUEST = "ICalReplyRequest";
    public static final String E_GET_FREE_BUSY_REQUEST = "GetFreeBusyRequest";
    public static final String E_GET_FREE_BUSY_RESPONSE = "GetFreeBusyResponse";
    public static final String E_GET_WORKING_HOURS_REQUEST = "GetWorkingHoursRequest";
    public static final String E_GET_ICAL_REQUEST = "GetICalRequest";
    public static final String E_ANNOUNCE_ORGANIZER_CHANGE_REQUEST = "AnnounceOrganizerChangeRequest";
    public static final String E_DISMISS_CALITEM_ALARM_REQUEST = "DismissCalendarItemAlarmRequest";
    public static final String E_SNOOZE_CALITEM_ALARM_REQUEST = "SnoozeCalendarItemAlarmRequest";
    public static final String E_GET_MINI_CAL_REQUEST = "GetMiniCalRequest";
    public static final String E_GET_RECUR_REQUEST = "GetRecurRequest";
    public static final String E_EXPAND_RECUR_REQUEST = "ExpandRecurRequest";
    public static final String E_CHECK_RECUR_CONFLICTS_REQUEST = "CheckRecurConflictsRequest";
    public static final String E_GET_SPELL_DICTIONARIES_REQUEST = "GetSpellDictionariesRequest";
    public static final String E_GET_SPELL_DICTIONARIES_RESPONSE = "GetSpellDictionariesResponse";
    public static final String E_CHECK_SPELLING_REQUEST = "CheckSpellingRequest";
    public static final String E_CHECK_SPELLING_RESPONSE = "CheckSpellingResponse";
    public static final String E_SAVE_DOCUMENT_REQUEST = "SaveDocumentRequest";
    public static final String E_SAVE_DOCUMENT_RESPONSE = "SaveDocumentResponse";
    public static final String E_DIFF_DOCUMENT_REQUEST = "DiffDocumentRequest";
    public static final String E_DIFF_DOCUMENT_RESPONSE = "DiffDocumentResponse";
    public static final String E_LIST_DOCUMENT_REVISIONS_REQUEST = "ListDocumentRevisionsRequest";
    public static final String E_LIST_DOCUMENT_REVISIONS_RESPONSE = "ListDocumentRevisionsResponse";
    public static final String E_PURGE_REVISION_REQUEST = "PurgeRevisionRequest";
    public static final String E_PURGE_REVISION_RESPONSE = "PurgeRevisionResponse";
    public static final String E_CREATE_DATA_SOURCE_REQUEST = "CreateDataSourceRequest";
    public static final String E_CREATE_DATA_SOURCE_RESPONSE = "CreateDataSourceResponse";
    public static final String E_GET_DATA_SOURCES_REQUEST = "GetDataSourcesRequest";
    public static final String E_GET_DATA_SOURCES_RESPONSE = "GetDataSourcesResponse";
    public static final String E_MODIFY_DATA_SOURCE_REQUEST = "ModifyDataSourceRequest";
    public static final String E_MODIFY_DATA_SOURCE_RESPONSE = "ModifyDataSourceResponse";
    public static final String E_TEST_DATA_SOURCE_REQUEST = "TestDataSourceRequest";
    public static final String E_TEST_DATA_SOURCE_RESPONSE = "TestDataSourceResponse";
    public static final String E_DELETE_DATA_SOURCE_REQUEST = "DeleteDataSourceRequest";
    public static final String E_DELETE_DATA_SOURCE_RESPONSE = "DeleteDataSourceResponse";
    public static final String E_IMPORT_DATA_REQUEST = "ImportDataRequest";
    public static final String E_IMPORT_DATA_RESPONSE = "ImportDataResponse";
    public static final String E_GET_IMPORT_STATUS_REQUEST = "GetImportStatusRequest";
    public static final String E_GET_IMPORT_STATUS_RESPONSE = "GetImportStatusResponse";
    public static final String E_CREATE_WAIT_SET_REQUEST = "CreateWaitSetRequest";
    public static final String E_CREATE_WAIT_SET_RESPONSE = "CreateWaitSetResponse";
    public static final String E_WAIT_SET_REQUEST = "WaitSetRequest";
    public static final String E_WAIT_SET_RESPONSE = "WaitSetResponse";
    public static final String E_DESTROY_WAIT_SET_REQUEST = "DestroyWaitSetRequest";
    public static final String E_DESTROY_WAIT_SET_RESPONSE = "DestroyWaitSetResponse";
    public static final String E_GET_PERMISSION_REQUEST = "GetPermissionRequest";
    public static final String E_GET_PERMISSION_RESPONSE = "GetPermissionResponse";
    public static final String E_CHECK_PERMISSION_REQUEST = "CheckPermissionRequest";
    public static final String E_CHECK_PERMISSION_RESPONSE = "CheckPermissionResponse";
    public static final String E_GRANT_PERMISSION_REQUEST = "GrantPermissionRequest";
    public static final String E_GRANT_PERMISSION_RESPONSE = "GrantPermissionResponse";
    public static final String E_REVOKE_PERMISSION_REQUEST = "RevokePermissionRequest";
    public static final String E_REVOKE_PERMISSION_RESPONSE = "RevokePermissionResponse";
    public static final String E_GET_EFFECTIVE_FOLDER_PERMS_REQUEST = "GetEffectiveFolderPermsRequest";
    public static final String E_GET_EFFECTIVE_FOLDER_PERMS_RESPONSE = "GetEffectiveFolderPermsResponse";
    public static final String E_GET_YAHOO_COOKIE_REQUEST = "GetYahooCookieRequest";
    public static final String E_GET_YAHOO_COOKIE_RESPONSE = "GetYahooCookieResponse";
    public static final String E_GET_YAHOO_AUTH_TOKEN_REQUEST = "GetYahooAuthTokenRequest";
    public static final String E_GET_YAHOO_AUTH_TOKEN_RESPONSE = "GetYahooAuthTokenResponse";
    public static final String E_AUTO_COMPLETE_REQUEST = "AutoCompleteRequest";
    public static final String E_AUTO_COMPLETE_RESPONSE = "AutoCompleteResponse";
    public static final String E_RANKING_ACTION_REQUEST = "RankingActionRequest";
    public static final String E_RANKING_ACTION_RESPONSE = "RankingActionResponse";
    public static final String E_SEND_VERIFICATION_CODE_REQUEST = "SendVerificationCodeRequest";
    public static final String E_SEND_VERIFICATION_CODE_RESPONSE = "SendVerificationCodeResponse";
    public static final String E_VERIFY_CODE_REQUEST = "VerifyCodeRequest";
    public static final String E_VERIFY_CODE_RESPONSE = "VerifyCodeResponse";
    public static final String E_INVALIDATE_REMINDER_DEVICE_REQUEST = "InvalidateReminderDeviceRequest";
    public static final String E_INVALIDATE_REMINDER_DEVICE_RESPONSE = "InvalidateReminderDeviceResponse";
    public static final String E_ADD_COMMENT_REQUEST = "AddCommentRequest";
    public static final String E_ADD_COMMENT_RESPONSE = "AddCommentResponse";
    public static final String E_GET_COMMENTS_REQUEST = "GetCommentsRequest";
    public static final String E_GET_COMMENTS_RESPONSE = "GetCommentsResponse";
    public static final String E_GET_SHARE_NOTIFICATIONS_REQUEST = "GetShareNotificationsRequest";
    public static final String E_GET_SHARE_NOTIFICATIONS_RESPONSE = "GetShareNotificationsResponse";
    public static final String E_GET_DATA_SOURCE_USAGE_REQUEST = "GetDataSourceUsageRequest";
    public static final String E_GET_DATA_SOURCE_USAGE_RESPONSE = "GetDataSourceUsageResponse";
    public static final String E_GET_SMART_FOLDERS_REQUEST = "GetSmartFoldersRequest";
    public static final String E_GET_SMART_FOLDERS_RESPONSE = "GetSmartFoldersResponse";

    // IMAP
    public static final String E_LIST_IMAP_SUBSCRIPTIONS_REQUEST = "ListIMAPSubscriptionsRequest";
    public static final String E_LIST_IMAP_SUBSCRIPTIONS_RESPONSE = "ListIMAPSubscriptionsResponse";
    public static final String E_SAVE_IMAP_SUBSCRIPTIONS_REQUEST = "SaveIMAPSubscriptionsRequest";
    public static final String E_SAVE_IMAP_SUBSCRIPTIONS_RESPONSE = "SaveIMAPSubscriptionsResponse";
    public static final String E_RESET_RECENT_MESSAGE_COUNT_REQUEST = "ResetRecentMessageCountRequest";
    public static final String E_RESET_RECENT_MESSAGE_COUNT_RESPONSE = "ResetRecentMessageCountResponse";
    public static final String E_OPEN_IMAP_FOLDER_REQUEST = "OpenIMAPFolderRequest";
    public static final String E_OPEN_IMAP_FOLDER_RESPONSE = "OpenIMAPFolderResponse";
    public static final String E_GET_MODIFIED_ITEMS_IDS_REQUEST = "GetModifiedItemsIDsRequest";
    public static final String E_GET_MODIFIED_ITEMS_IDS_RESPONSE = "GetModifiedItemsIDsResponse";
    public static final String E_GET_LAST_ITEM_ID_IN_MAILBOX_REQUEST = "GetLastItemIdInMailboxRequest";
    public static final String E_GET_LAST_ITEM_ID_IN_MAILBOX_RESPONSE = "GetLastItemIdInMailboxResponse";
    public static final String E_BEGIN_TRACKING_IMAP_REQUEST = "BeginTrackingIMAPRequest";
    public static final String E_BEGIN_TRACKING_IMAP_RESPONSE = "BeginTrackingIMAPResponse";
    public static final String E_GET_SYSTEM_RETENTION_POLICY_REQUEST = "GetSystemRetentionPolicyRequest";
    public static final String E_GET_SYSTEM_RETENTION_POLICY_RESPONSE = "GetSystemRetentionPolicyResponse";
    public static final String E_RECORD_IMAP_SESSION_REQUEST = "RecordIMAPSessionRequest";
    public static final String E_RECORD_IMAP_SESSION_RESPONSE = "RecordIMAPSessionResponse";
    public static final String E_GET_IMAP_RECENT_REQUEST = "GetIMAPRecentRequest";
    public static final String E_GET_IMAP_RECENT_RESPONSE = "GetIMAPRecentResponse";
    public static final String E_GET_IMAP_RECENT_CUTOFF_REQUEST = "GetIMAPRecentCutoffRequest";
    public static final String E_GET_IMAP_RECENT_CUTOFF_RESPONSE = "GetIMAPRecentCutoffResponse";
    public static final String E_IMAP_COPY_REQUEST = "IMAPCopyRequest";
    public static final String E_IMAP_COPY_RESPONSE = "IMAPCopyResponse";

    // noop
    public static final QName NO_OP_REQUEST = QName.get(E_NO_OP_REQUEST, NAMESPACE);
    public static final QName NO_OP_RESPONSE = QName.get(E_NO_OP_RESPONSE, NAMESPACE);

    // UUID generation
    public static final QName GENERATE_UUID_REQUEST = QName.get(E_GENERATE_UUID_REQUEST, NAMESPACE);
    public static final QName GENERATE_UUID_RESPONSE = QName.get(E_GENERATE_UUID_RESPONSE, NAMESPACE);

    // searching
    public static final QName SEARCH_REQUEST = QName.get(E_SEARCH_REQUEST, NAMESPACE);
    public static final QName SEARCH_RESPONSE = QName.get(E_SEARCH_RESPONSE, NAMESPACE);
    public static final QName SEARCH_CONV_REQUEST = QName.get(E_SEARCH_CONV_REQUEST, NAMESPACE);
    public static final QName SEARCH_CONV_RESPONSE = QName.get(E_SEARCH_CONV_RESPONSE, NAMESPACE);
    public static final QName BROWSE_REQUEST = QName.get(E_BROWSE_REQUEST, NAMESPACE);
    public static final QName BROWSE_RESPONSE = QName.get(E_BROWSE_RESPONSE, NAMESPACE);

    // search history
    public static final QName REJECT_SAVE_SEARCH_PROMPT_REQUEST = QName.get(E_REJECT_SAVE_SEARCH_PROMPT_REQUEST, NAMESPACE);
    public static final QName REJECT_SAVE_SEARCH_PROMPT_RESPONSE = QName.get(E_REJECT_SAVE_SEARCH_PROMPT_RESPONSE, NAMESPACE);
    public static final QName CLEAR_SEARCH_HISTORY_REQUEST = QName.get(E_CLEAR_SEARCH_HISTORY_REQUEST, NAMESPACE);
    public static final QName CLEAR_SEARCH_HISTORY_RESPONSE = QName.get(E_CLEAR_SEARCH_HISTORY_RESPONSE, NAMESPACE);
    public static final QName SEARCH_SUGGEST_REQUEST = QName.get(E_SEARCH_SUGGEST_REQUEST, NAMESPACE);
    public static final QName SEARCH_SUGGEST_RESPONSE = QName.get(E_SEARCH_SUGGEST_RESPONSE, NAMESPACE);
    public static final QName GET_SEARCH_HISTORY_REQUEST = QName.get(E_GET_SEARCH_HISTORY_REQUEST, NAMESPACE);
    public static final QName GET_SEARCH_HISTORY_RESPONSE = QName.get(E_GET_SEARCH_HISTORY_RESPONSE, NAMESPACE);

    // dumpster
    public static final QName EMPTY_DUMPSTER_REQUEST = QName.get(E_EMPTY_DUMPSTER_REQUEST, NAMESPACE);
    public static final QName EMPTY_DUMPSTER_RESPONSE = QName.get(E_EMPTY_DUMPSTER_RESPONSE, NAMESPACE);

    // generic items
    public static final QName GET_ITEM_REQUEST = QName.get(E_GET_ITEM_REQUEST, NAMESPACE);
    public static final QName GET_ITEM_RESPONSE = QName.get(E_GET_ITEM_RESPONSE, NAMESPACE);
    public static final QName ITEM_ACTION_REQUEST = QName.get(E_ITEM_ACTION_REQUEST, NAMESPACE);
    public static final QName ITEM_ACTION_RESPONSE = QName.get(E_ITEM_ACTION_RESPONSE, NAMESPACE);
    public static final QName GET_METADATA_REQUEST = QName.get(E_GET_METADATA_REQUEST, NAMESPACE);
    public static final QName GET_METADATA_RESPONSE = QName.get(E_GET_METADATA_RESPONSE, NAMESPACE);
    public static final QName SET_METADATA_REQUEST = QName.get(E_SET_METADATA_REQUEST, NAMESPACE);
    public static final QName SET_METADATA_RESPONSE = QName.get(E_SET_METADATA_RESPONSE, NAMESPACE);
    public static final QName GET_MAILBOX_METADATA_REQUEST = QName.get(E_GET_MAILBOX_METADATA_REQUEST, NAMESPACE);
    public static final QName GET_MAILBOX_METADATA_RESPONSE = QName.get(E_GET_MAILBOX_METADATA_RESPONSE, NAMESPACE);
    public static final QName SET_MAILBOX_METADATA_REQUEST = QName.get(E_SET_MAILBOX_METADATA_REQUEST, NAMESPACE);
    public static final QName SET_MAILBOX_METADATA_RESPONSE = QName.get(E_SET_MAILBOX_METADATA_RESPONSE, NAMESPACE);
    public static final QName MODIFY_MAILBOX_METADATA_REQUEST = QName.get(E_MODIFY_MAILBOX_METADATA_REQUEST, NAMESPACE);
    public static final QName MODIFY_MAILBOX_METADATA_RESPONSE = QName.get(E_MODIFY_MAILBOX_METADATA_RESPONSE, NAMESPACE);

    // conversations
    public static final QName GET_CONV_REQUEST = QName.get(E_GET_CONV_REQUEST, NAMESPACE);
    public static final QName GET_CONV_RESPONSE = QName.get(E_GET_CONV_RESPONSE, NAMESPACE);
    public static final QName CONV_ACTION_REQUEST = QName.get(E_CONV_ACTION_REQUEST, NAMESPACE);
    public static final QName CONV_ACTION_RESPONSE = QName.get(E_CONV_ACTION_RESPONSE, NAMESPACE);
    // messages
    public static final QName GET_MSG_REQUEST = QName.get(E_GET_MSG_REQUEST, NAMESPACE);
    public static final QName GET_MSG_RESPONSE = QName.get(E_GET_MSG_RESPONSE, NAMESPACE);
    public static final QName GET_MSG_METADATA_REQUEST = QName.get(E_GET_MSG_METADATA_REQUEST, NAMESPACE);
    public static final QName GET_MSG_METADATA_RESPONSE = QName.get(E_GET_MSG_METADATA_RESPONSE, NAMESPACE);
    public static final QName MSG_ACTION_REQUEST = QName.get(E_MSG_ACTION_REQUEST, NAMESPACE);
    public static final QName MSG_ACTION_RESPONSE = QName.get(E_MSG_ACTION_RESPONSE, NAMESPACE);
    //   SendMsg/AddMsg/SaveDraft
    public static final QName SEND_MSG_REQUEST = QName.get(E_SEND_MSG_REQUEST, NAMESPACE);
    public static final QName SEND_MSG_RESPONSE = QName.get(E_SEND_MSG_RESPONSE, NAMESPACE);
    public static final QName SEND_REPORT_REQUEST = QName.get(E_SEND_REPORT_REQUEST, NAMESPACE);
    public static final QName SEND_REPORT_RESPONSE = QName.get(E_SEND_REPORT_RESPONSE, NAMESPACE);
    public static final QName SEND_SHARE_NOTIFICATION_REQUEST = QName.get(E_SEND_SHARE_NOTIFICATION_REQUEST, NAMESPACE);
    public static final QName SEND_SHARE_NOTIFICATION_RESPONSE = QName.get(E_SEND_SHARE_NOTIFICATION_RESPONSE, NAMESPACE);
    public static final QName BOUNCE_MSG_REQUEST = QName.get(E_BOUNCE_MSG_REQUEST, NAMESPACE);
    public static final QName BOUNCE_MSG_RESPONSE = QName.get(E_BOUNCE_MSG_RESPONSE, NAMESPACE);
    public static final QName ADD_MSG_REQUEST = QName.get(E_ADD_MSG_REQUEST, NAMESPACE);
    public static final QName ADD_MSG_RESPONSE = QName.get(E_ADD_MSG_RESPONSE, NAMESPACE);
    public static final QName SAVE_DRAFT_REQUEST = QName.get(E_SAVE_DRAFT_REQUEST, NAMESPACE);
    public static final QName SAVE_DRAFT_RESPONSE = QName.get(E_SAVE_DRAFT_RESPONSE, NAMESPACE);
    public static final QName REMOVE_ATTACHMENTS_REQUEST = QName.get(E_REMOVE_ATTACHMENTS_REQUEST, NAMESPACE);
    public static final QName REMOVE_ATTACHMENTS_RESPONSE = QName.get(E_REMOVE_ATTACHMENTS_RESPONSE, NAMESPACE);
    // folders
    public static final QName CREATE_FOLDER_REQUEST = QName.get(E_CREATE_FOLDER_REQUEST, NAMESPACE);
    public static final QName CREATE_FOLDER_RESPONSE = QName.get(E_CREATE_FOLDER_RESPONSE, NAMESPACE);
    public static final QName GET_FOLDER_REQUEST = QName.get(E_GET_FOLDER_REQUEST, NAMESPACE);
    public static final QName GET_FOLDER_RESPONSE = QName.get(E_GET_FOLDER_RESPONSE, NAMESPACE);
    public static final QName FOLDER_ACTION_REQUEST = QName.get(E_FOLDER_ACTION_REQUEST, NAMESPACE);
    public static final QName FOLDER_ACTION_RESPONSE = QName.get(E_FOLDER_ACTION_RESPONSE, NAMESPACE);
    // tags
    public static final QName CREATE_TAG_REQUEST = QName.get(E_CREATE_TAG_REQUEST, NAMESPACE);
    public static final QName CREATE_TAG_RESPONSE = QName.get(E_CREATE_TAG_RESPONSE, NAMESPACE);
    public static final QName GET_TAG_REQUEST = QName.get(E_GET_TAG_REQUEST, NAMESPACE);
    public static final QName GET_TAG_RESPONSE = QName.get(E_GET_TAG_RESPONSE, NAMESPACE);
    public static final QName TAG_ACTION_REQUEST = QName.get(E_TAG_ACTION_REQUEST, NAMESPACE);
    public static final QName TAG_ACTION_RESPONSE = QName.get(E_TAG_ACTION_RESPONSE, NAMESPACE);
    // smart folders
    public static final QName GET_SMART_FOLDERS_REQUEST = QName.get(E_GET_SMART_FOLDERS_REQUEST, NAMESPACE);
    public static final QName GET_SMART_FOLDERS_RESPONSE = QName.get(E_GET_SMART_FOLDERS_RESPONSE, NAMESPACE);
    // saved searches
    public static final QName CREATE_SEARCH_FOLDER_REQUEST = QName.get(E_CREATE_SEARCH_FOLDER_REQUEST, NAMESPACE);
    public static final QName CREATE_SEARCH_FOLDER_RESPONSE = QName.get(E_CREATE_SEARCH_FOLDER_RESPONSE, NAMESPACE);
    public static final QName GET_SEARCH_FOLDER_REQUEST = QName.get(E_GET_SEARCH_FOLDER_REQUEST, NAMESPACE);
    public static final QName GET_SEARCH_FOLDER_RESPONSE = QName.get(E_GET_SEARCH_FOLDER_RESPONSE, NAMESPACE);
    public static final QName MODIFY_SEARCH_FOLDER_REQUEST = QName.get(E_MODIFY_SEARCH_FOLDER_REQUEST, NAMESPACE);
    public static final QName MODIFY_SEARCH_FOLDER_RESPONSE = QName.get(E_MODIFY_SEARCH_FOLDER_RESPONSE, NAMESPACE);
    // mountpoints
    public static final QName CREATE_MOUNTPOINT_REQUEST = QName.get(E_CREATE_MOUNTPOINT_REQUEST, NAMESPACE);
    public static final QName CREATE_MOUNTPOINT_RESPONSE = QName.get(E_CREATE_MOUNTPOINT_RESPONSE, NAMESPACE);
    public static final QName ENABLE_SHARED_REMINDER_REQUEST = QName.get(E_ENABLE_SHARED_REMINDER_REQUEST, NAMESPACE);
    public static final QName ENABLE_SHARED_REMINDER_RESPONSE = QName.get(E_ENABLE_SHARED_REMINDER_RESPONSE, NAMESPACE);
    // contacts
    public static final QName CREATE_CONTACT_REQUEST = QName.get(E_CREATE_CONTACT_REQUEST, NAMESPACE);
    public static final QName CREATE_CONTACT_RESPONSE = QName.get(E_CREATE_CONTACT_RESPONSE, NAMESPACE);
    public static final QName MODIFY_CONTACT_REQUEST = QName.get(E_MODIFY_CONTACT_REQUEST, NAMESPACE);
    public static final QName MODIFY_CONTACT_RESPONSE = QName.get(E_MODIFY_CONTACT_RESPONSE, NAMESPACE);
    public static final QName GET_CONTACTS_REQUEST = QName.get(E_GET_CONTACTS_REQUEST, NAMESPACE);
    public static final QName GET_CONTACTS_RESPONSE = QName.get(E_GET_CONTACTS_RESPONSE, NAMESPACE);
    public static final QName IMPORT_CONTACTS_REQUEST = QName.get(E_IMPORT_CONTACTS_REQUEST, NAMESPACE);
    public static final QName IMPORT_CONTACTS_RESPONSE = QName.get(E_IMPORT_CONTACTS_RESPONSE, NAMESPACE);
    public static final QName EXPORT_CONTACTS_REQUEST = QName.get(E_EXPORT_CONTACTS_REQUEST, NAMESPACE);
    public static final QName EXPORT_CONTACTS_RESPONSE = QName.get(E_EXPORT_CONTACTS_RESPONSE, NAMESPACE);
    public static final QName CONTACT_ACTION_REQUEST = QName.get(E_CONTACT_ACTION_REQUEST, NAMESPACE);
    public static final QName CONTACT_ACTION_RESPONSE = QName.get(E_CONTACT_ACTION_RESPONSE, NAMESPACE);
    public static final QName GET_CONTACT_BACKUP_LIST_REQUEST = QName.get(E_GET_CONTACT_BACKUP_LIST_REQUEST, NAMESPACE);
    public static final QName GET_CONTACT_BACKUP_LIST_RESPONSE = QName.get(E_GET_CONTACT_BACKUP_LIST_RESPONSE, NAMESPACE);
    // notes
    public static final QName CREATE_NOTE_REQUEST = QName.get(E_CREATE_NOTE_REQUEST, NAMESPACE);
    public static final QName CREATE_NOTE_RESPONSE = QName.get(E_CREATE_NOTE_RESPONSE, NAMESPACE);
    public static final QName GET_NOTE_REQUEST = QName.get(E_GET_NOTE_REQUEST, NAMESPACE);
    public static final QName GET_NOTE_RESPONSE = QName.get(E_GET_NOTE_RESPONSE, NAMESPACE);
    public static final QName NOTE_ACTION_REQUEST = QName.get(E_NOTE_ACTION_REQUEST, NAMESPACE);
    public static final QName NOTE_ACTION_RESPONSE = QName.get(E_NOTE_ACTION_RESPONSE, NAMESPACE);
    // sync for Outlook
    public static final QName SYNC_REQUEST = QName.get(E_SYNC_REQUEST, NAMESPACE);
    public static final QName SYNC_RESPONSE = QName.get(E_SYNC_RESPONSE, NAMESPACE);

    // Filter rules
    public static final QName GET_FILTER_RULES_REQUEST = QName.get(E_GET_FILTER_RULES_REQUEST, NAMESPACE);
    public static final QName GET_FILTER_RULES_RESPONSE = QName.get(E_GET_FILTER_RULES_RESPONSE, NAMESPACE);
    public static final QName MODIFY_FILTER_RULES_REQUEST = QName.get(E_MODIFY_FILTER_RULES_REQUEST, NAMESPACE);
    public static final QName MODIFY_FILTER_RULES_RESPONSE = QName.get(E_MODIFY_FILTER_RULES_RESPONSE, NAMESPACE);
    public static final QName APPLY_FILTER_RULES_REQUEST = QName.get(E_APPLY_FILTER_RULES_REQUEST, NAMESPACE);
    public static final QName APPLY_FILTER_RULES_RESPONSE = QName.get(E_APPLY_FILTER_RULES_RESPONSE, NAMESPACE);
    public static final QName GET_OUTGOING_FILTER_RULES_REQUEST = QName.get(E_GET_OUTGOING_FILTER_RULES_REQUEST, NAMESPACE);
    public static final QName GET_OUTGOING_FILTER_RULES_RESPONSE = QName.get(E_GET_OUTGOING_FILTER_RULES_RESPONSE, NAMESPACE);
    public static final QName MODIFY_OUTGOING_FILTER_RULES_REQUEST = QName.get(E_MODIFY_OUTGOING_FILTER_RULES_REQUEST, NAMESPACE);
    public static final QName MODIFY_OUTGOING_FILTER_RULES_RESPONSE = QName.get(E_MODIFY_OUTGOING_FILTER_RULES_RESPONSE, NAMESPACE);
    public static final QName APPLY_OUTGOING_FILTER_RULES_REQUEST = QName.get(E_APPLY_OUTGOING_FILTER_RULES_REQUEST, NAMESPACE);
    public static final QName APPLY_OUTGOING_FILTER_RULES_RESPONSE = QName.get(E_APPLY_OUTGOING_FILTER_RULES_RESPONSE, NAMESPACE);

    // Calendar
    public static final QName GET_APPT_SUMMARIES_REQUEST = QName.get(E_GET_APPT_SUMMARIES_REQUEST, NAMESPACE);
    public static final QName GET_APPOINTMENT_REQUEST = QName.get(E_GET_APPOINTMENT_REQUEST, NAMESPACE);
    public static final QName SET_APPOINTMENT_REQUEST = QName.get(E_SET_APPOINTMENT_REQUEST, NAMESPACE);
    public static final QName CREATE_APPOINTMENT_REQUEST = QName.get(E_CREATE_APPOINTMENT_REQUEST, NAMESPACE);
    public static final QName CREATE_APPOINTMENT_EXCEPTION_REQUEST = QName.get(E_CREATE_APPOINTMENT_EXCEPTION_REQUEST, NAMESPACE);
    public static final QName MODIFY_APPOINTMENT_REQUEST = QName.get(E_MODIFY_APPOINTMENT_REQUEST, NAMESPACE);
    public static final QName CANCEL_APPOINTMENT_REQUEST = QName.get(E_CANCEL_APPOINTMENT_REQUEST, NAMESPACE);
    public static final QName FORWARD_APPOINTMENT_REQUEST = QName.get(E_FORWARD_APPOINTMENT_REQUEST, NAMESPACE);
    public static final QName FORWARD_APPOINTMENT_INVITE_REQUEST = QName.get(E_FORWARD_APPOINTMENT_INVITE_REQUEST, NAMESPACE);
    public static final QName ADD_APPOINTMENT_INVITE_REQUEST = QName.get(E_ADD_APPOINTMENT_INVITE_REQUEST, NAMESPACE);
    public static final QName ADD_APPOINTMENT_INVITE_RESPONSE = QName.get(E_ADD_APPOINTMENT_INVITE_RESPONSE, NAMESPACE);
    public static final QName COUNTER_APPOINTMENT_REQUEST = QName.get(E_COUNTER_APPOINTMENT_REQUEST, NAMESPACE);
    public static final QName COUNTER_APPOINTMENT_RESPONSE = QName.get(E_COUNTER_APPOINTMENT_RESPONSE, NAMESPACE);
    public static final QName DECLINE_COUNTER_APPOINTMENT_REQUEST = QName.get(E_DECLINE_COUNTER_APPOINTMENT_REQUEST, NAMESPACE);
    public static final QName DECLINE_COUNTER_APPOINTMENT_RESPONSE = QName.get(E_DECLINE_COUNTER_APPOINTMENT_RESPONSE, NAMESPACE);
    public static final QName IMPORT_APPOINTMENTS_REQUEST = QName.get(E_IMPORT_APPOINTMENTS_REQUEST, NAMESPACE);
    public static final QName IMPORT_APPOINTMENTS_RESPONSE = QName.get(E_IMPORT_APPOINTMENTS_RESPONSE, NAMESPACE);

    public static final QName GET_TASK_SUMMARIES_REQUEST = QName.get(E_GET_TASK_SUMMARIES_REQUEST, NAMESPACE);
    public static final QName GET_TASK_REQUEST = QName.get(E_GET_TASK_REQUEST, NAMESPACE);
    public static final QName SET_TASK_REQUEST = QName.get(E_SET_TASK_REQUEST, NAMESPACE);
    public static final QName CREATE_TASK_REQUEST = QName.get(E_CREATE_TASK_REQUEST, NAMESPACE);
    public static final QName CREATE_TASK_EXCEPTION_REQUEST = QName.get(E_CREATE_TASK_EXCEPTION_REQUEST, NAMESPACE);
    public static final QName MODIFY_TASK_REQUEST = QName.get(E_MODIFY_TASK_REQUEST, NAMESPACE);
    public static final QName ADD_TASK_INVITE_REQUEST = QName.get(E_ADD_TASK_INVITE_REQUEST, NAMESPACE);
    public static final QName ADD_TASK_INVITE_RESPONSE = QName.get(E_ADD_TASK_INVITE_RESPONSE, NAMESPACE);
    public static final QName CANCEL_TASK_REQUEST = QName.get(E_CANCEL_TASK_REQUEST, NAMESPACE);
    public static final QName COMPLETE_TASK_INSTANCE_REQUEST = QName.get(E_COMPLETE_TASK_INSTANCE_REQUEST, NAMESPACE);

    public static final QName GET_CALITEM_SUMMARIES_REQUEST = QName.get(E_GET_CALITEM_SUMMARIES_REQUEST, NAMESPACE);

//    public static final QName GET_CALITEM_REQUEST = QName.get("GetCalendarItemRequest", NAMESPACE);
//    public static final QName SET_CALITEM_REQUEST = QName.get("SetCalendarItemRequest", NAMESPACE);
//    public static final QName CREATE_CALITEM_REQUEST = QName.get("CreateCalendarItemRequest", NAMESPACE);
//    public static final QName CREATE_CALITEM_EXCEPTION_REQUEST = QName.get("CreateCalendarItemExceptionRequest", NAMESPACE);
//    public static final QName MODIFY_CALITEM_REQUEST = QName.get("ModifyCalendarItemRequest", NAMESPACE);
//    public static final QName CANCEL_CALITEM_REQUEST = QName.get("CancelCalendarItemRequest", NAMESPACE);

    public static final QName SEND_INVITE_REPLY_REQUEST = QName.get(E_SEND_INVITE_REPLY_REQUEST, NAMESPACE);
    public static final QName ICAL_REPLY_REQUEST = QName.get(E_ICAL_REPLY_REQUEST, NAMESPACE);
    public static final QName GET_FREE_BUSY_REQUEST = QName.get(E_GET_FREE_BUSY_REQUEST, NAMESPACE);
    public static final QName GET_FREE_BUSY_RESPONSE = QName.get(E_GET_FREE_BUSY_RESPONSE, NAMESPACE);
    public static final QName GET_WORKING_HOURS_REQUEST = QName.get(E_GET_WORKING_HOURS_REQUEST, NAMESPACE);
    public static final QName GET_ICAL_REQUEST = QName.get(E_GET_ICAL_REQUEST, NAMESPACE);
    public static final QName ANNOUNCE_ORGANIZER_CHANGE_REQUEST = QName.get(E_ANNOUNCE_ORGANIZER_CHANGE_REQUEST, NAMESPACE);
    public static final QName DISMISS_CALITEM_ALARM_REQUEST = QName.get(E_DISMISS_CALITEM_ALARM_REQUEST, NAMESPACE);
    public static final QName SNOOZE_CALITEM_ALARM_REQUEST = QName.get(E_SNOOZE_CALITEM_ALARM_REQUEST, NAMESPACE);
    public static final QName GET_MINI_CAL_REQUEST = QName.get(E_GET_MINI_CAL_REQUEST, NAMESPACE);

    public static final QName GET_RECUR_REQUEST = QName.get(E_GET_RECUR_REQUEST, NAMESPACE);
    public static final QName EXPAND_RECUR_REQUEST = QName.get(E_EXPAND_RECUR_REQUEST, NAMESPACE);
    public static final QName CHECK_RECUR_CONFLICTS_REQUEST = QName.get(E_CHECK_RECUR_CONFLICTS_REQUEST, NAMESPACE);

    // spell checking
    public static final QName GET_SPELL_DICTIONARIES_REQUEST = QName.get(E_GET_SPELL_DICTIONARIES_REQUEST, NAMESPACE);
    public static final QName GET_SPELL_DICTIONARIES_RESPONSE = QName.get(E_GET_SPELL_DICTIONARIES_RESPONSE, NAMESPACE);

    public static final QName CHECK_SPELLING_REQUEST = QName.get(E_CHECK_SPELLING_REQUEST, NAMESPACE);
    public static final QName CHECK_SPELLING_RESPONSE = QName.get(E_CHECK_SPELLING_RESPONSE, NAMESPACE);

    // documents
    public static final QName SAVE_DOCUMENT_REQUEST = QName.get(E_SAVE_DOCUMENT_REQUEST, NAMESPACE);
    public static final QName SAVE_DOCUMENT_RESPONSE = QName.get(E_SAVE_DOCUMENT_RESPONSE, NAMESPACE);
    public static final QName DIFF_DOCUMENT_REQUEST = QName.get(E_DIFF_DOCUMENT_REQUEST, NAMESPACE);
    public static final QName DIFF_DOCUMENT_RESPONSE = QName.get(E_DIFF_DOCUMENT_RESPONSE, NAMESPACE);
    public static final QName LIST_DOCUMENT_REVISIONS_REQUEST = QName.get(E_LIST_DOCUMENT_REVISIONS_REQUEST, NAMESPACE);
    public static final QName LIST_DOCUMENT_REVISIONS_RESPONSE = QName.get(E_LIST_DOCUMENT_REVISIONS_RESPONSE, NAMESPACE);
    public static final QName PURGE_REVISION_REQUEST = QName.get(E_PURGE_REVISION_REQUEST, NAMESPACE);
    public static final QName PURGE_REVISION_RESPONSE = QName.get(E_PURGE_REVISION_RESPONSE, NAMESPACE);

    // data sources
    public static final QName CREATE_DATA_SOURCE_REQUEST = QName.get(E_CREATE_DATA_SOURCE_REQUEST, NAMESPACE);
    public static final QName CREATE_DATA_SOURCE_RESPONSE = QName.get(E_CREATE_DATA_SOURCE_RESPONSE, NAMESPACE);
    public static final QName GET_DATA_SOURCES_REQUEST = QName.get(E_GET_DATA_SOURCES_REQUEST, NAMESPACE);
    public static final QName GET_DATA_SOURCES_RESPONSE = QName.get(E_GET_DATA_SOURCES_RESPONSE, NAMESPACE);
    public static final QName MODIFY_DATA_SOURCE_REQUEST = QName.get(E_MODIFY_DATA_SOURCE_REQUEST, NAMESPACE);
    public static final QName MODIFY_DATA_SOURCE_RESPONSE = QName.get(E_MODIFY_DATA_SOURCE_RESPONSE, NAMESPACE);
    public static final QName TEST_DATA_SOURCE_REQUEST = QName.get(E_TEST_DATA_SOURCE_REQUEST, NAMESPACE);
    public static final QName TEST_DATA_SOURCE_RESPONSE = QName.get(E_TEST_DATA_SOURCE_RESPONSE, NAMESPACE);
    public static final QName DELETE_DATA_SOURCE_REQUEST = QName.get(E_DELETE_DATA_SOURCE_REQUEST, NAMESPACE);
    public static final QName DELETE_DATA_SOURCE_RESPONSE = QName.get(E_DELETE_DATA_SOURCE_RESPONSE, NAMESPACE);
    public static final QName IMPORT_DATA_REQUEST = QName.get(E_IMPORT_DATA_REQUEST, NAMESPACE);
    public static final QName IMPORT_DATA_RESPONSE = QName.get(E_IMPORT_DATA_RESPONSE, NAMESPACE);
    public static final QName GET_IMPORT_STATUS_REQUEST = QName.get(E_GET_IMPORT_STATUS_REQUEST, NAMESPACE);
    public static final QName GET_IMPORT_STATUS_RESPONSE = QName.get(E_GET_IMPORT_STATUS_RESPONSE, NAMESPACE);
    public static final QName GET_DATA_SOURCE_USAGE_REQUEST = QName.get(E_GET_DATA_SOURCE_USAGE_REQUEST, NAMESPACE);
    public static final QName GET_DATA_SOURCE_USAGE_RESPONSE = QName.get(E_GET_DATA_SOURCE_USAGE_RESPONSE, NAMESPACE);

    public static final QName CREATE_WAIT_SET_REQUEST = QName.get(E_CREATE_WAIT_SET_REQUEST, NAMESPACE);
    public static final QName CREATE_WAIT_SET_RESPONSE = QName.get(E_CREATE_WAIT_SET_RESPONSE, NAMESPACE);
    public static final QName WAIT_SET_REQUEST = QName.get(E_WAIT_SET_REQUEST, NAMESPACE);
    public static final QName WAIT_SET_RESPONSE = QName.get(E_WAIT_SET_RESPONSE, NAMESPACE);
    public static final QName DESTROY_WAIT_SET_REQUEST = QName.get(E_DESTROY_WAIT_SET_REQUEST, NAMESPACE);
    public static final QName DESTROY_WAIT_SET_RESPONSE = QName.get(E_DESTROY_WAIT_SET_RESPONSE, NAMESPACE);

    // account ACL
    public static final QName GET_PERMISSION_REQUEST = QName.get(E_GET_PERMISSION_REQUEST, NAMESPACE);
    public static final QName GET_PERMISSION_RESPONSE = QName.get(E_GET_PERMISSION_RESPONSE, NAMESPACE);
    public static final QName CHECK_PERMISSION_REQUEST = QName.get(E_CHECK_PERMISSION_REQUEST, NAMESPACE);
    public static final QName CHECK_PERMISSION_RESPONSE = QName.get(E_CHECK_PERMISSION_RESPONSE, NAMESPACE);
    public static final QName GRANT_PERMISSION_REQUEST = QName.get(E_GRANT_PERMISSION_REQUEST, NAMESPACE);
    public static final QName GRANT_PERMISSION_RESPONSE = QName.get(E_GRANT_PERMISSION_RESPONSE, NAMESPACE);
    public static final QName REVOKE_PERMISSION_REQUEST = QName.get(E_REVOKE_PERMISSION_REQUEST, NAMESPACE);
    public static final QName REVOKE_PERMISSION_RESPONSE = QName.get(E_REVOKE_PERMISSION_RESPONSE, NAMESPACE);

    // folder ACL
    public static final QName GET_EFFECTIVE_FOLDER_PERMS_REQUEST = QName.get(E_GET_EFFECTIVE_FOLDER_PERMS_REQUEST, NAMESPACE);
    public static final QName GET_EFFECTIVE_FOLDER_PERMS_RESPONSE = QName.get(E_GET_EFFECTIVE_FOLDER_PERMS_RESPONSE, NAMESPACE);

    // Yahoo Auth
    public static final QName GET_YAHOO_COOKIE_REQUEST = QName.get(E_GET_YAHOO_COOKIE_REQUEST, NAMESPACE);
    public static final QName GET_YAHOO_COOKIE_RESPONSE = QName.get(E_GET_YAHOO_COOKIE_RESPONSE, NAMESPACE);
    public static final QName GET_YAHOO_AUTH_TOKEN_REQUEST = QName.get(E_GET_YAHOO_AUTH_TOKEN_REQUEST, NAMESPACE);
    public static final QName GET_YAHOO_AUTH_TOKEN_RESPONSE = QName.get(E_GET_YAHOO_AUTH_TOKEN_RESPONSE, NAMESPACE);

    // autocomplete
    public static final QName AUTO_COMPLETE_REQUEST = QName.get(E_AUTO_COMPLETE_REQUEST, NAMESPACE);
    public static final QName AUTO_COMPLETE_RESPONSE = QName.get(E_AUTO_COMPLETE_RESPONSE, NAMESPACE);

    // contact ranking mgmt
    public static final QName RANKING_ACTION_REQUEST = QName.get(E_RANKING_ACTION_REQUEST, NAMESPACE);
    public static final QName RANKING_ACTION_RESPONSE = QName.get(E_RANKING_ACTION_RESPONSE, NAMESPACE);

    // device verification for sms reminders
    public static final QName SEND_VERIFICATION_CODE_REQUEST = QName.get(E_SEND_VERIFICATION_CODE_REQUEST, NAMESPACE);
    public static final QName SEND_VERIFICATION_CODE_RESPONSE = QName.get(E_SEND_VERIFICATION_CODE_RESPONSE, NAMESPACE);
    public static final QName VERIFY_CODE_REQUEST = QName.get(E_VERIFY_CODE_REQUEST, NAMESPACE);
    public static final QName VERIFY_CODE_RESPONSE = QName.get(E_VERIFY_CODE_RESPONSE, NAMESPACE);
    public static final QName INVALIDATE_REMINDER_DEVICE_REQUEST = QName.get(E_INVALIDATE_REMINDER_DEVICE_REQUEST, NAMESPACE);
    public static final QName INVALIDATE_REMINDER_DEVICE_RESPONSE = QName.get(E_INVALIDATE_REMINDER_DEVICE_RESPONSE, NAMESPACE);

    // comments
    public static final QName ADD_COMMENT_REQUEST = QName.get(E_ADD_COMMENT_REQUEST, NAMESPACE);
    public static final QName ADD_COMMENT_RESPONSE = QName.get(E_ADD_COMMENT_RESPONSE, NAMESPACE);
    public static final QName GET_COMMENTS_REQUEST = QName.get(E_GET_COMMENTS_REQUEST, NAMESPACE);
    public static final QName GET_COMMENTS_RESPONSE = QName.get(E_GET_COMMENTS_RESPONSE, NAMESPACE);

    // sharing
    public static final QName GET_SHARE_NOTIFICATIONS_REQUEST = QName.get(E_GET_SHARE_NOTIFICATIONS_REQUEST, NAMESPACE);
    public static final QName GET_SHARE_NOTIFICATIONS_RESPONSE = QName.get(E_GET_SHARE_NOTIFICATIONS_RESPONSE, NAMESPACE);

    public static final QName GET_SYSTEM_RETENTION_POLICY_REQUEST = QName.get(E_GET_SYSTEM_RETENTION_POLICY_REQUEST, NAMESPACE);
    public static final QName GET_SYSTEM_RETENTION_POLICY_RESPONSE = QName.get(E_GET_SYSTEM_RETENTION_POLICY_RESPONSE, NAMESPACE);

    // IMAP
    public static final QName LIST_IMAP_SUBSCRIPTIONS_REQUEST = QName.get(E_LIST_IMAP_SUBSCRIPTIONS_REQUEST, NAMESPACE);
    public static final QName LIST_IMAP_SUBSCRIPTIONS_RESPONSE = QName.get(E_LIST_IMAP_SUBSCRIPTIONS_RESPONSE, NAMESPACE);
    public static final QName SAVE_IMAP_SUBSCRIPTIONS_REQUEST = QName.get(E_SAVE_IMAP_SUBSCRIPTIONS_REQUEST, NAMESPACE);
    public static final QName SAVE_IMAP_SUBSCRIPTIONS_RESPONSE = QName.get(E_SAVE_IMAP_SUBSCRIPTIONS_RESPONSE, NAMESPACE);
    public static final QName RESET_RECENT_MESSAGE_COUNT_REQUEST = QName.get(E_RESET_RECENT_MESSAGE_COUNT_REQUEST, NAMESPACE);
    public static final QName RESET_RECENT_MESSAGE_COUNT_RESPONSE = QName.get(E_RESET_RECENT_MESSAGE_COUNT_RESPONSE, NAMESPACE);
    public static final QName OPEN_IMAP_FOLDER_REQUEST = QName.get(E_OPEN_IMAP_FOLDER_REQUEST, NAMESPACE);
    public static final QName OPEN_IMAP_FOLDER_RESPONSE = QName.get(E_OPEN_IMAP_FOLDER_RESPONSE, NAMESPACE);
    public static final QName GET_MODIFIED_ITEMS_IDS_REQUEST = QName.get(E_GET_MODIFIED_ITEMS_IDS_REQUEST, NAMESPACE);
    public static final QName GET_MODIFIED_ITEMS_IDS_RESPONSE = QName.get(E_GET_MODIFIED_ITEMS_IDS_RESPONSE, NAMESPACE);
    public static final QName GET_LAST_ITEM_ID_IN_MAILBOX_REQUEST = QName.get(E_GET_LAST_ITEM_ID_IN_MAILBOX_REQUEST, NAMESPACE);
    public static final QName GET_LAST_ITEM_ID_IN_MAILBOX_RESPONSE = QName.get(E_GET_LAST_ITEM_ID_IN_MAILBOX_RESPONSE, NAMESPACE);
    public static final QName BEGIN_TRACKING_IMAP_REQUEST = QName.get(E_BEGIN_TRACKING_IMAP_REQUEST, NAMESPACE);
    public static final QName BEGIN_TRACKING_IMAP_RESPONSE = QName.get(E_BEGIN_TRACKING_IMAP_RESPONSE, NAMESPACE);
    public static final QName GET_IMAP_RECENT_REQUEST = QName.get(E_GET_IMAP_RECENT_REQUEST, NAMESPACE);
    public static final QName GET_IMAP_RECENT_RESPONSE = QName.get(E_GET_IMAP_RECENT_RESPONSE, NAMESPACE);
    public static final QName GET_IMAP_RECENT_CUTOFF_REQUEST = QName.get(E_GET_IMAP_RECENT_CUTOFF_REQUEST, NAMESPACE);
    public static final QName GET_IMAP_RECENT_CUTOFF_RESPONSE = QName.get(E_GET_IMAP_RECENT_CUTOFF_RESPONSE, NAMESPACE);
    public static final QName RECORD_IMAP_SESSION_REQUEST = QName.get(E_RECORD_IMAP_SESSION_REQUEST, NAMESPACE);
    public static final QName RECORD_IMAP_SESSION_RESPONSE = QName.get(E_RECORD_IMAP_SESSION_RESPONSE, NAMESPACE);
    public static final QName IMAP_COPY_REQUEST = QName.get(E_IMAP_COPY_REQUEST, NAMESPACE);
    public static final QName IMAP_COPY_RESPONSE = QName.get(E_IMAP_COPY_RESPONSE, NAMESPACE);

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
    public static final String E_COMMENT = "comment";
    public static final String E_LINK = "lk";
    public static final String E_SMART_FOLDER = "sf";

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
    public static final String E_METADATA = "meta";
    public static final String A_SECTION = "section";

    // Old filter rules constants
    public static final String E_RULE = "r";
    public static final String E_RULES = "rules";
    public static final String E_CONDITION_GROUP = "g";
    public static final String E_CONDITION = "c";
    public static final String E_FILTER_ARG = "arg";

    // Filter rules constants
    public static final String E_FILTER_RULES = "filterRules";
    public static final String E_FILTER_RULE = "filterRule";
    public static final String E_NESTED_RULE = "nestedRule";
    public static final String E_FILTER_TESTS = "filterTests";

    public static final String E_HEADER_TEST = "headerTest";
    public static final String E_TRUE_TEST = "trueTest";
    public static final String E_HEADER_EXISTS_TEST = "headerExistsTest";
    public static final String E_MIME_HEADER_TEST = "mimeHeaderTest";
    public static final String E_ADDRESS_TEST = "addressTest";
    public static final String E_ENVELOPE_TEST = "envelopeTest";
    public static final String E_SIZE_TEST = "sizeTest";
    public static final String E_DATE_TEST = "dateTest";
    public static final String E_CURRENT_TIME_TEST = "currentTimeTest";
    public static final String E_CURRENT_DAY_OF_WEEK_TEST = "currentDayOfWeekTest";
    public static final String E_BODY_TEST = "bodyTest";
    public static final String E_ATTACHMENT_TEST = "attachmentTest";
    public static final String E_ADDRESS_BOOK_TEST = "addressBookTest";
    public static final String E_CONTACT_RANKING_TEST = "contactRankingTest";
    public static final String E_ME_TEST = "meTest";
    public static final String E_INVITE_TEST = "inviteTest";
    public static final String E_CONVERSATION_TEST = "conversationTest";
    public static final String E_FACEBOOK_TEST = "facebookTest";
    public static final String E_LINKEDIN_TEST = "linkedinTest";
    public static final String E_SOCIALCAST_TEST = "socialcastTest";
    public static final String E_TWITTER_TEST = "twitterTest";
    public static final String E_LIST_TEST = "listTest";
    public static final String E_BULK_TEST = "bulkTest";
    public static final String E_METHOD = "method";
    public static final String E_IMPORTANCE_TEST = "importanceTest";
    public static final String E_FLAGGED_TEST = "flaggedTest";
    public static final String E_COMMUNITY_REQUESTS_TEST = "communityRequestsTest";
    public static final String E_COMMUNITY_CONTENT_TEST = "communityContentTest";
    public static final String E_COMMUNITY_CONNECTIONS_TEST = "communityConnectionsTest";

    public static final String E_FILTER_ACTIONS = "filterActions";
    public static final String E_ACTION_KEEP = "actionKeep";
    public static final String E_ACTION_DISCARD = "actionDiscard";
    public static final String E_ACTION_FILE_INTO = "actionFileInto";
    public static final String E_ACTION_TAG = "actionTag";
    public static final String E_ACTION_FLAG = "actionFlag";
    public static final String E_ACTION_REDIRECT = "actionRedirect";
    public static final String E_ACTION_REPLY = "actionReply";
    public static final String E_ACTION_NOTIFY = "actionNotify";
    public static final String E_ACTION_RFCCOMPLIANTNOTIFY = "actionRFCCompliantNotify";
    public static final String E_ACTION_STOP = "actionStop";
    public static final String E_ACTION_REJECT = "actionReject";
    public static final String E_ACTION_EREJECT = "actionEreject";
    public static final String E_ACTION_LOG = "actionLog";
    public static final String E_ACTION_ADDHEADER = "actionAddheader";
    public static final String E_ACTION_DELETEHEADER = "actionDeleteheader";
    public static final String E_ACTION_REPLACEHEADER = "actionReplaceheader";

    public static final String A_ADDRESS_PART = "part";
    public static final String A_STRING_COMPARISON = "stringComparison";
    public static final String A_VALUE_COMPARISON = "valueComparison";
    public static final String A_COUNT_COMPARISON = "countComparison";
    public static final String A_VALUE_COMPARISON_COMPARATOR = "valueComparisonComparator";
    public static final String A_CASE_SENSITIVE = "caseSensitive";
    public static final String A_NUMBER_COMPARISON = "numberComparison";
    public static final String A_DATE_COMPARISON = "dateComparison";
    public static final String A_CONDITION = "condition";
    public static final String A_NEGATIVE = "negative";
    public static final String A_ABS_FOLDER_PATH = "absFolderPath";
    public static final String A_FOLDER_PATH = "folderPath";
    public static final String A_FLAG_NAME = "flagName";
    public static final String A_TAG_NAME = "tagName";
    public static final String A_INDEX = "index";
    public static final String A_TIME = "time";
    public static final String A_MAX_BODY_SIZE = "maxBodySize";
    public static final String A_ORIG_HEADERS = "origHeaders";
    public static final String A_WHERE = "where";
    public static final String A_IMP = "imp";
    public static final String E_FILTER_VARIABLES = "filterVariables";
    public static final String E_FILTER_VARIABLE = "filterVariable";
    public static final String A_COPY = "copy";
    public static final String A_LEVEL = "level";

    // Sieve filter: RFC compliant 'notify' action
    public static final String A_FROM = "from";
    public static final String A_IMPORTANCE = "importance";
    public static final String A_OPTIONS = "options";
    public static final String A_MESSAGE = "message";
    public static final String A_METHOD = "method";

    // grants and shares
    public static final String E_ACL = "acl";
    public static final String A_INTERNAL_GRANT_EXPIRY = "internalGrantExpiry";
    public static final String A_GUEST_GRANT_EXPIRY = "guestGrantExpiry";
    public static final String E_GRANT = "grant";
    public static final String E_NOTES = "notes";
    public static final String E_SHARE = "share";
    public static final String E_REVOKE = "revoke";
    public static final String A_EXPIRE = "expire";
    public static final String A_ZIMBRA_ID = "zid";
    public static final String A_RIGHTS = "perm";
    public static final String A_GRANT_TYPE = "gt";
    public static final String A_PASSWORD = "pw";
    public static final String A_ACCESSKEY = "key";
    public static final String A_EXPIRY = "expiry";

    // account ACLs
    public static final String E_ACE = "ace";
    public static final String E_RIGHT = "right";
    public static final String E_TARGET = "target";
    public static final String A_ALLOW = "allow";
    public static final String A_DENY = "deny";
    public static final String A_RIGHT = "right";
    public static final String A_TARGET_BY = "by";
    public static final String A_TARGET_TYPE = "type";

    // email addresses
    public static final String E_EMAIL = "e";
    public static final String A_ADDRESS = "a";
    public static final String A_PERSONAL = "p";
    public static final String A_DISPLAY = "d";
    public static final String A_ADDRESS_TYPE = "t";

    // group info in addresses
    public static final String A_IS_GROUP = "isGroup";
    public static final String A_EXP = "exp";
    public static final String A_NEED_EXP = "needExp";

    public static final String A_DESC_ENABLED = "descEnabled";
    public static final String A_PATH = "path";
    public static final String A_NAME = "name";
    public static final String A_VALUE = "value";
    public static final String A_DATE = "d";
    public static final String A_SENT_DATE = "sd";
    public static final String A_RESENT_DATE = "rd";
    public static final String A_SIZE = "s";
    public static final String A_FLAGS = "f";
    public static final String A_ID = "id";
    public static final String A_UUID = "uuid";
    public static final String A_IDS = "ids";
    public static final String A_CONV_ID = "cid";
    public static final String A_DRAFT_ID = "did";
    public static final String A_SEND_FROM_DRAFT = "sfd";
    public static final String A_MESSAGE_ID = "mid";
    public static final String A_REF = "ref";
    public static final String A_TARGET_CONSTRAINT = "tcon";
    public static final String A_TAG = "tag";
    public static final String A_TAGS = "t";
    public static final String A_TAG_NAMES = "tn";
    public static final String A_FOLDER = "l";
    public static final String A_FOLDER_UUID = "luuid";
    public static final String A_VISIBLE = "visible";
    public static final String A_DELETABLE = "deletable";
    public static final String A_URL = "url";
    public static final String A_NUM = "n";
    public static final String A_IMAP_UID ="i4uid";
    public static final String A_IMAP_NUM = "i4n";
    public static final String A_IMAP_MODSEQ = "i4ms";
    public static final String A_IMAP_UIDNEXT = "i4next";
    public static final String A_IMAP_RECENT_CUTOFF = "cutoff";
    public static final String A_TOTAL_SIZE = "total";
    public static final String A_OPERATION = "op";
    public static final String A_RECURSIVE = "recursive";
    public static final String A_FOLDER_DEPTH = "depth";
    public static final String A_DEFAULT_VIEW = "view";
    public static final String A_TRAVERSE = "tr";
    public static final String A_BROKEN = "broken";
    public static final String A_UNREAD = "u";
    public static final String A_IMAP_UNREAD = "i4u";
    public static final String A_COLOR = "color";
    public static final String A_RGB = "rgb";
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
    public static final String A_FULL_CONVERSATION = "fullConversation";
    public static final String A_PREFETCH = "prefetch";
    public static final String A_VERBOSE = "verbose";
    public static final String A_CSVFORMAT = "csvfmt";
    public static final String A_CSVLOCALE = "csvlocale";
    public static final String A_CSVSEPARATOR = "csvsep";
    public static final String A_NEED_GRANTEE_NAME = "needGranteeName";
    public static final String A_INVALID = "invalid";
    public static final String A_REMINDER = "reminder";
    public static final String A_RETURN_HIDDEN_ATTRS = "returnHiddenAttrs";
    public static final String A_RETURN_CERT_INFO = "returnCertInfo";
    public static final String A_MAX_MEMBERS = "maxMembers";
    public static final String A_TOO_MANY_MEMBERS = "tooManyMembers";
    public static final String A_ACTIVESYNC_DISABLED = "activesyncdisabled";
    public static final String A_WEB_OFFLINE_SYNC_DAYS = "webOfflineSyncDays";
    public static final String A_NUM_DAYS = "numDays";
    public static final String A_NON_EXISTENT_IDS = "nei";
    public static final String A_NEWLY_CREATED_IDS = "nci";
    public static final String E_CONTACT_MEMBER_OF = "memberOf";

    // contact group
    public static final String E_CONTACT_GROUP_MEMBER = "m";
    public static final String E_CONTACT_GROUP_MEMBER_ATTRIBUTE = "ma";
    public static final String A_CONTACT_GROUP_MEMBER_TYPE = "type";
    public static final String A_CONTACT_GROUP_MEMBER_VALUE = "value";
    public static final String A_DEREF_CONTACT_GROUP_MEMBER = "derefGroupMember";

    // messages
    public static final String E_MIMEPART = "mp";
    public static final String E_SUBJECT = "su";
    public static final String E_FRAG = "fr";
    public static final String E_MSG_ID_HDR = "mid";
    public static final String E_IN_REPLY_TO = "irt";
    public static final String E_CONTENT = "content";
    public static final String E_ORIG_CONTENT = "origContent";
    public static final String E_SHARE_NOTIFICATION = "shr";
    public static final String E_DL_SUBSCRIPTION_NOTIFICATION = "dlSubs";
    public static final String A_PART = "part";
    public static final String A_BODY = "body";
    public static final String A_CONTENT_TYPE = "ct";
    public static final String A_CONTENT_DISPOSITION = "cd";
    public static final String A_CONTENT_DESCRIPTION = "cde";
    public static final String A_CONTENT_ID = "ci";
    public static final String A_CONTENT_LOCATION = "cl";
    public static final String A_CONTENT_NAME = "name";
    public static final String A_CONTENT_FILENAME = "filename";
    public static final String A_NO_ICAL = "noICal";
    public static final String A_RAW = "raw";
    public static final String A_USE_CONTENT_URL = "useContentUrl";
    public static final String A_HEADER = "header";
    public static final String A_WANT_HTML = "html";
    public static final String A_WANT_IMAP_UID = "wantImapUid";
    public static final String A_WANT_MODIFIED_SEQUENCE = "wantModSeq";
    public static final String A_MARK_READ = "read";
    public static final String A_NEUTER = "neuter";
    public static final String A_MAX_INLINED_LENGTH = "max";
    public static final String A_TRUNCATED_CONTENT = "truncated";
    public static final String A_FILTER_SENT = "filterSent";
    public static final String A_WANT_CONTENT = "wantContent";

    // send/save draft
    public static final String E_ATTACH = "attach";
    public static final String E_HEADER = A_HEADER;
    public static final String A_ATTACHMENT_ID = "aid";
    public static final String A_OPTIONAL = "optional";
    public static final String A_ORIG_ID = "origid";
    public static final String A_REPLY_TYPE = "rt";
    public static final String A_IDENTITY_ID = "idnt";
    public static final String A_DATASOURCE_ID = "dsId";
    public static final String A_NO_SAVE_TO_SENT = "noSave";
    public static final String A_FETCH_SAVED_MSG = "fetchSavedMsg";
    public static final String A_SEND_UID = "suid";
    public static final String A_FOR_ACCOUNT = "forAcct";
    public static final String A_AUTO_SEND_TIME = "autoSendTime";

    // mountpoints
    public static final String A_REMOTE_ID = "rid";
    public static final String A_REMOTE_UUID = "ruuid";
    public static final String A_OWNER_NAME = "owner";
    public static final String A_OWNER_FOLDER_NAME = "oname";

    // browse
    public static final String A_BROWSE_BY = "browseBy";
    public static final String A_BROWSE_DOMAIN_HEADER = "h";
    public static final String A_FREQUENCY = "freq";
    public static final String A_MAX_TO_RETURN = "maxToReturn";
    public static final String A_REGEX = "regex";

    // search
    public static final String E_QUERY = "query";
    public static final String E_HIT_MIMEPART = "hp";
    public static final String E_SUGEST = "suggest";
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
    public static final String A_INCLUDE_TAG_DELETED = "includeTagDeleted";
    public static final String A_INCLUDE_TAG_MUTED = "includeTagMuted";
    public static final String A_ALLOWABLE_TASK_STATUS = "allowableTaskStatus";
    public static final String A_IN_DUMPSTER = "inDumpster";
    public static final String A_WARMUP = "warmup";
    public static final String A_QUICK = "quick";
    public static final String A_SCORE = "score";
    public static final String E_HIT = "hit";
    public static final String E_SEARCH_HISTORY = "searchHistory";
    public static final String A_SAVE_SEARCH_PROMPT = "saveSearchPrompt";
    public static final String A_LOG_SEARCH = "logSearch";
    public static final String A_RELEVANCE_SORT_SUPPORTED = "relevanceSortSupported";


    // search-result paging
    public static final String E_CURSOR = "cursor";
    public static final String A_QUERY_CONTEXT = "context";
    public static final String A_QUERY_OFFSET = "offset";
    public static final String A_QUERY_LIMIT = "limit";
    public static final String A_QUERY_MORE = "more";
    public static final String A_INCLUDE_OFFSET = "includeOffset";

    // sync
    public static final String E_DELETED = "deleted";
    public static final String A_TOKEN = "token";
    public static final String A_REVISION = "rev";
    public static final String A_FETCH_IF_EXISTS = "fie";
    public static final String A_CHANGE_DATE = "md";
    public static final String A_MODIFIED_SEQUENCE = "ms";
    public static final String A_SYNC = "sync";
    public static final String A_TYPED_DELETES = "typed";
    public static final String A_CALENDAR_CUTOFF = "calCutoff";
    public static final String A_MSG_CUTOFF = "msgCutoff";
    public static final String A_GAL_DEFINITION_LAST_MODIFIED = "galDefinitionLastModified";
    public static final String A_GALSYNC_THROTTLED = "throttled";
    public static final String A_GALSYNC_FULLSYNC_RECOMMENDED = "fullSyncRecommended";
    public static final String A_DELETE_LIMIT = "deleteLimit";
    public static final String A_CHANGE_LIMIT = "changeLimit";
    public static final String A_REMAIN = "remain";

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
    public static final String E_CAL_DESC_HTML = "descHtml";
    public static final String E_INSTANCE = "inst";
    public static final String E_FREEBUSY_USER = "usr";
    public static final String E_FREEBUSY_FREE = "f";
    public static final String E_FREEBUSY_BUSY = "b";
    public static final String E_FREEBUSY_BUSY_TENTATIVE = "t";
    public static final String E_FREEBUSY_BUSY_UNAVAILABLE = "u";
    public static final String E_FREEBUSY_NODATA = "n";
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
    public static final String E_CAL_EVENT_ID = "eventId";
    public static final String E_CAL_EVENT_SUBJECT = "subject";
    public static final String E_CAL_EVENT_LOCATION = "location";
    public static final String E_CAL_EVENT_ISPRIVATE = "isPrivate";
    public static final String E_CAL_EVENT_ISREMINDERSET = "isReminderSet";
    public static final String E_CAL_EVENT_ISMEETING = "isMeeting";
    public static final String E_CAL_EVENT_ISRECURRING = "isRecurring";
    public static final String E_CAL_EVENT_ISEXCEPTION = "isException";
    public static final String E_CAL_EVENT_HASPERMISSION = "hasPermission";

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

    public static final String E_CAL_CATEGORY = "category";
    public static final String E_CAL_COMMENT = "comment";
    public static final String E_CAL_CONTACT = "contact";
    public static final String E_CAL_CONTENT = "content";
    public static final String E_CAL_GEO = "geo";

    public static final String E_CAL_XPROP = "xprop";
    public static final String E_CAL_XPARAM = "xparam";

    public static final String E_CAL_ECHO = "echo";

    public static final String A_CAL_METHOD = "method";
    public static final String A_CAL_ALARM_DISMISSED_AT = "dismissedAt";
    public static final String A_CAL_ALARM_SNOOZE_UNTIL = "until";
    public static final String A_CAL_NEXT_ALARM = "nextAlarm";
    public static final String A_CAL_NO_NEXT_ALARM = "noNextAlarm";
    public static final String A_CAL_ALARM_INSTANCE_START = "alarmInstStart";
    public static final String A_CAL_INCLUDE_CONTENT = "includeContent";
    public static final String A_CAL_INCLUDE_INVITES = "includeInvites";
    public static final String A_NEED_CALENDAR_SENTBY_FIXUP = "needCalendarSentByFixup";
    public static final String A_IS_CALENDAR_FORWARD = "isCalendarForward";
    public static final String A_UID = "uid";
    public static final String A_SUMMARY = "summary";
    public static final String A_CAL_ID = "calItemId";
    public static final String A_CAL_ITEM_FOLDER = "ciFolder";
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
    public static final String A_CAL_RECURRENCE_ID_Z = "ridZ";  // recurrence id in "Z" (UTC) timezone
    public static final String A_CAL_START_TIME = "s";
    public static final String A_CAL_END_TIME = "e";
    public static final String A_CAL_DURATION = "d";
    public static final String A_CAL_NEW_DURATION = "dur";
    public static final String A_CAL_DATETIME = "d";
    public static final String A_CAL_DATETIME_UTC = "u";
    public static final String A_CAL_TZ_OFFSET = "tzo";
    public static final String A_CAL_TZ_OFFSET_DUE = "tzoDue";
    public static final String A_CAL_SUB_ID = "subId";
    public static final String A_CAL_INV_ID = "invId";
    public static final String A_APPT_ID_DEPRECATE_ME = "apptId";
    public static final String A_CAL_STATUS = "status";
    public static final String A_CAL_PARTSTAT = "ptst";
    public static final String A_APPT_FREEBUSY = "fb";
    public static final String A_APPT_FREEBUSY_ACTUAL = "fba";
    public static final String A_APPT_FREEBUSY_EXCLUDE_UID = "excludeUid";
    public static final String A_APPT_TRANSPARENCY = "transp";
    public static final String A_CAL_CLASS = "class";
    public static final String A_CAL_ALL = "all";
    public static final String A_CAL_ALLDAY = "allDay";
    public static final String A_CAL_DRAFT = "draft";
    public static final String A_CAL_NEVER_SENT = "neverSent";
    public static final String A_CAL_NO_BLOB = "noBlob";
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
    public static final String A_CAL_HAS_EXCEPTIONS = "hasEx";
    public static final String A_CAL_UPDATE_ORGANIZER = "updateOrganizer";
    public static final String A_CAL_THIS_AND_FUTURE = "thisAndFuture";
    public static final String A_CAL_TIMEZONE= "tz";
    public static final String A_CAL_ISORG = "isOrg";
    public static final String A_CAL_ATTENDEE = "at";
    public static final String A_CAL_PRIORITY = "priority";
    public static final String A_CAL_URL = "url";
    public static final String A_TASK_PERCENT_COMPLETE = "percentComplete";
    public static final String A_TASK_DUE_DATE = "dueDate";
    public static final String A_TASK_COMPLETED = "completed";
    public static final String A_WAIT = "wait";
    public static final String A_DELEGATE = "delegate";
    public static final String A_TIMEOUT = "timeout";
    public static final String A_WAIT_DISALLOWED = "waitDisallowed";
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
    public static final String A_CAL_TZ_STDNAME = "stdname";
    public static final String A_CAL_TZ_DAYNAME = "dayname";
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

    public static final String A_CAL_GEO_LATITUDE = "lat";
    public static final String A_CAL_GEO_LONGITUDE = "lon";

    public static final String E_CAL_MINICAL_DATE = "date";

    public static final String A_CAL_INTENDED_FOR = "cif";
    public static final String A_CAL_CHANGES = "changes";
    public static final String A_CAL_ECHO = "echo";
    public static final String A_CAL_CODE = "code";
    public static final String A_CAL_FORCESEND = "forcesend";

    public static final String A_CAL_ORPHAN = "orphan";

    // spell checking
    public static final String A_AVAILABLE = "available";
    public static final String E_MISSPELLED = "misspelled";
    public static final String A_WORD = "word";
    public static final String A_SUGGESTIONS = "suggestions";
    public static final String E_DICTIONARY = "dictionary";
    public static final String A_DICTIONARY = "dictionary";
    public static final String A_IGNORE = "ignore";

    // data sources
    public static final String E_DS = "dsrc";
    public static final String E_DS_POP3 = "pop3";
    public static final String E_DS_IMAP = "imap";
    public static final String E_DS_CALDAV = "caldav";
    public static final String E_DS_YAB = "yab";
    public static final String E_DS_RSS = "rss";
    public static final String E_DS_GAL = "gal";
    public static final String E_DS_CAL = "cal";
    public static final String E_DS_UNKNOWN = "unknown";
    public static final String E_DS_LAST_ERROR = "lastError";
    public static final String A_DS_IS_ENABLED = "isEnabled";
    public static final String A_DS_IS_IMPORTONLY = "importOnly";
    public static final String A_DS_HOST = "host";
    public static final String A_DS_PORT = "port";
    public static final String A_DS_CONNECTION_TYPE = "connectionType";
    public static final String A_DS_USERNAME = "username";
    public static final String A_DS_PASSWORD = "password";
    public static final String A_DS_SMTP_ENABLED = "smtpEnabled";
    public static final String A_DS_SMTP_HOST = "smtpHost";
    public static final String A_DS_SMTP_PORT = "smtpPort";
    public static final String A_DS_SMTP_CONNECTION_TYPE = "smtpConnectionType";
    public static final String A_DS_SMTP_AUTH_REQUIRED = "smtpAuthRequired";
    public static final String A_DS_SMTP_USERNAME = "smtpUsername";
    public static final String A_DS_SMTP_PASSWORD = "smtpPassword";
    public static final String A_DS_TYPE = "type";
    public static final String A_DS_SUCCESS = "success";
    public static final String A_DS_ERROR = "error";
    public static final String A_DS_IS_RUNNING = "isRunning";
    public static final String A_DS_LEAVE_ON_SERVER = "leaveOnServer";
    public static final String A_DS_POLLING_INTERVAL = "pollingInterval";
    public static final String A_DS_EMAIL_ADDRESS = "emailAddress";
    public static final String A_DS_USE_ADDRESS_FOR_FORWARD_REPLY ="useAddressForForwardReply";
    public static final String A_DS_DEFAULT_SIGNATURE = "defaultSignature";
    public static final String A_DS_FORWARD_REPLY_SIGNATURE = "forwardReplySignature";
    public static final String A_DS_FROM_DISPLAY = "fromDisplay";
    public static final String A_DS_REPLYTO_ADDRESS = "replyToAddress";
    public static final String A_DS_REPLYTO_DISPLAY = "replyToDisplay";
    public static final String A_DS_IMPORT_CLASS = "importClass";
    public static final String A_DS_FAILING_SINCE = "failingSince";
    public static final String A_DS_OAUTH_TOKEN = "oauthToken";
    public static final String A_DS_TEST = "test";
    public static final String A_DS_USAGE = "usage";
    public static final String A_DS_CLIENT_ID = "clientId";
    public static final String A_DS_CLIENT_SECRET = "clientSecret";
    public static final String A_DS_REFRESH_TOKEN = "refreshToken";
    public static final String A_DS_REFRESH_TOKEN_URL = "refreshTokenUrl";

    public static final String A_ACCT_RELATIVE_PATH = "acctRelPath";

    public static final String A_CREATOR = "cr";
    public static final String A_TYPE = "t";
    public static final String E_WIKIWORD = "w";
    public static final String E_DOC = "doc";
    public static final String E_UPLOAD = "upload";
    public static final String A_VERSION = "ver";
    public static final String A_USER_AGENT = "ua";
    public static final String A_METADATA_VERSION = "mdver";
    public static final String A_SUBJECT = "su";
    public static final String A_LAST_EDITED_BY = "leb";
    public static final String A_COUNT = "count";
    public static final String A_ARGS = "args";
    public static final String A_REST_URL = "rest";
    public static final String A_V1 = "v1";
    public static final String A_V2 = "v2";
    public static final String E_CHUNK = "chunk";
    public static final String A_DISP = "disp";
    public static final String A_DESC = "desc";
    public static final String A_LOCKOWNER_ID = "loid";
    public static final String A_LOCKOWNER_EMAIL = "loe";
    public static final String A_LOCKTIMESTAMP = "lt";
    public static final String E_REVISION = "revision";
    public static final String A_INCLUDE_OLDER_REVISIONS = "includeOlderRevisions";

    // WaitSet
    public static final String E_WAITSET_ADD = "add";
    public static final String E_WAITSET_UPDATE = "update";
    public static final String E_WAITSET_REMOVE = "remove";
    public static final String E_ERROR = "error";
    public static final String A_ACCOUNT = "account";
    public static final String A_WAITSET_ID = "waitSet";
    public static final String A_SEQ = "seq";
    public static final String A_BLOCK = "block";
    public static final String A_CANCELED = "canceled";
    public static final String A_DEFTYPES = "defTypes";
    public static final String A_ALL_ACCOUNTS = "allAccounts";
    public static final String A_TYPES = "types";
    public static final String A_FOLDER_INTERESTS = "folderInterests";
    public static final String A_CHANGED_FOLDERS = "changedFolders";
    public static final String A_CHANGE = "change";
    public static final String A_CHANGE_ID = "changeid";
    public static final String E_A = "a";
    public static final String A_EXPAND = "expand";
    public static final String E_PENDING_FOLDER_MODIFICATIONS = "mods";
    public static final String E_CREATED = "created";
    public static final String E_MODIFIED_MSGS = "modMsgs";
    public static final String E_MODIFIED_TAGS = "modTags";
    public static final String E_MODIFIED_FOLDERS = "modFolders";

    // AutoComplete
    public static final String E_MATCH = "match";
    public static final String A_LIMIT = "limit";
    public static final String A_FOLDERS = "folders";
    public static final String A_INCLUDE_GAL = "includeGal";
    public static final String A_EMAIL = "email";
    public static final String A_RANKING = "ranking";
    public static final String A_CANBECACHED = "canBeCached";
    public static final String A_DISPLAYNAME = "display";
    public static final String A_MATCH_TYPE = "type";
    public static final String A_FIRSTNAME = "first";
    public static final String A_MIDDLENAME = "middle";
    public static final String A_LASTNAME = "last";
    public static final String A_FULLNAME = "full";
    public static final String A_NICKNAME = "nick";
    public static final String A_COMPANY = "company";
    public static final String A_FILEAS = "fileas";
    public static final String A_LDAP_OFFSET = "ldapOffset";

    // device verification for reminders
    public static final String A_VERIFICATION_CODE = "code";
    public static final String A_VERIFICATION_SUCCESS = "success";

    public static final String A_USER = "user";
    public static final String A_TS = "ts";
    public static final String A_ITEMID = "itemId";
    public static final String A_ITEM_NAME = "itemName";

    public static final String E_DEVICE = "device";
    public static final String A_CREATED = "created";
    public static final String A_ACCESSED = "accessed";

    public static final String A_PARENT_ID = "parentId";
    public static final String A_TEXT = "text";
    public static final String E_ARG = "arg";

    public static final String E_GRANTOR = "grantor";
    public static final String E_GRANTEE = "grantee";

    public static final String A_ACCOUNT_ID = "accountId";

    // Retention policy
    public static final String E_RETENTION_POLICY = "retentionPolicy";
    public static final String E_KEEP = "keep";
    public static final String E_PURGE = "purge";
    public static final String E_POLICY = "policy";
    public static final String A_LIFETIME = "lifetime";
    public static final String A_RETENTION_POLICY_TYPE = "type";

    public static final String E_WATCHER = "watcher";

    public static final String E_FILTER = "filter";
    public static final String A_SESSION = "session";

    public static final String A_ACTION = "action";

    public static final String OP_TAG = "tag";
    public static final String OP_FLAG = "flag";
    public static final String OP_PRIORITY = "priority";
    public static final String OP_READ = "read";
    public static final String OP_COLOR = "color";
    public static final String OP_HARD_DELETE = "delete";
    public static final String OP_RECOVER = "recover";  // recover by copying then deleting from dumpster
    public static final String OP_DUMPSTER_DELETE = "dumpsterdelete";  // delete from dumpster
    public static final String OP_MOVE = "move";
    public static final String OP_COPY = "copy";
    public static final String OP_SPAM = "spam";
    public static final String OP_TRASH = "trash";
    public static final String OP_RENAME = "rename";
    public static final String OP_UPDATE = "update";
    public static final String OP_LOCK = "lock";
    public static final String OP_UNLOCK = "unlock";
    public static final String OP_INHERIT = "inherit";
    public static final String OP_MUTE = "mute";
    public static final String OP_RESET_IMAP_UID = "resetimapuid";
    public static final String OP_SEEN = "seen";

    // Contacts API
    public static final String E_RESTORE_CONTACTS_REQUEST = "RestoreContactsRequest";
    public static final String E_RESTORE_CONTACTS_RESPONSE = "RestoreContactsResponse";
    public static final QName RESTORE_CONTACTS_REQUEST = QName.get(E_RESTORE_CONTACTS_REQUEST, NAMESPACE);
    public static final QName RESTORE_CONTACTS_RESPONSE = QName.get(E_RESTORE_CONTACTS_RESPONSE, NAMESPACE);
    public static final String A_CONTACTS_BACKUP_FILE_NAME = "contactsBackupFileName";
    public static final String A_CONTACTS_RESTORE_RESOLVE = "resolve";
    public static final String A_CONTACTS_BACKUP_FOLDER_NAME = "ContactsBackup";

    // SearchAction API
    public static final String E_SEARCH_ACTION_REQUEST = "SearchActionRequest";
    public static final String E_SEARCH_ACTION_RESPONSE = "SearchActionResponse";
    public static final String E_BULK_ACTION = "BulkAction";
    public static final QName SEARCH_ACTION_REQUEST = QName.get(E_SEARCH_ACTION_REQUEST, NAMESPACE);
    public static final QName SEARCH_ACTION_RESPONSE = QName.get(E_SEARCH_ACTION_RESPONSE, NAMESPACE);

    // Profile image API
    public static final String E_MODIFY_PROFILE_IMAGE_REQUEST = "ModifyProfileImageRequest";
    public static final String E_MODIFY_PROFILE_IMAGE_RESPONSE = "ModifyProfileImageResponse";
    public static final QName MODIFY_PROFILE_IMAGE_REQUEST = QName.get(E_MODIFY_PROFILE_IMAGE_REQUEST, NAMESPACE);
    public static final QName MODIFY_PROFILE_IMAGE_RESPONSE = QName.get(E_MODIFY_PROFILE_IMAGE_RESPONSE, NAMESPACE);

    // Password reset API
    public static final String E_SET_RECOVERY_ACCOUNT_REQUEST = "SetRecoveryAccountRequest";
    public static final String E_SET_RECOVERY_ACCOUNT_RESPONSE = "SetRecoveryAccountResponse";
    public static final QName SET_RECOVERY_EMAIL_REQUEST = QName.get(E_SET_RECOVERY_ACCOUNT_REQUEST, NAMESPACE);
    public static final QName SET_RECOVERY_EMAIL_RESPONSE = QName.get(E_SET_RECOVERY_ACCOUNT_RESPONSE, NAMESPACE);
    public static final String A_RECOVERY_ATTEMPTS_LEFT = "recoveryAttemptsLeft";
    public static final String A_RECOVERY_ACCOUNT_VERIFICATION_CODE = "recoveryAccountVerificationCode";
    public static final String E_RECOVER_ACCOUNT_REQUEST = "RecoverAccountRequest";
    public static final String E_RECOVER_ACCOUNT_RESPONSE = "RecoverAccountResponse";
    public static final QName RECOVER_ACCOUNT_REQUEST = QName.get(E_RECOVER_ACCOUNT_REQUEST, NAMESPACE);
    public static final QName RECOVER_ACCOUNT_RESPONSE = QName.get(E_RECOVER_ACCOUNT_RESPONSE, NAMESPACE);
    public static final String A_RECOVERY_ACCOUNT = "recoveryAccount";
    public static final String A_CHANNEL = "channel";

    // Related Contacts
    public static final String E_GET_RELATED_CONTACTS_REQUEST = "GetRelatedContactsRequest";
    public static final String E_GET_RELATED_CONTACTS_RESPONSE = "GetRelatedContactsResponse";
    public static final QName GET_RELATED_CONTACTS_REQUEST = QName.get(E_GET_RELATED_CONTACTS_REQUEST, NAMESPACE);
    public static final QName GET_RELATED_CONTACTS_RESPONSE = QName.get(E_GET_RELATED_CONTACTS_RESPONSE, NAMESPACE);
    public static final String E_AFFINITY_TARGET = "targetContact";
    public static final String A_TARGET_EMAIL = "contact";
    public static final String A_TARGET_AFFINITY_FIELD = "field";
    public static final String A_REQUESTED_AFFINITY_FIELD = "requestedField";
    public static final String E_RELATED_CONTACTS = "relatedContacts";
    public static final String E_RELATED_CONTACT = "relatedContact";
    public static final String A_AFFINITY_SCOPE = "scope";

    // Contact Analytics
    public static final String E_GET_CONTACT_FREQUENCY_REQUEST = "GetContactFrequencyRequest";
    public static final String E_GET_CONTACT_FREQUENCY_RESPONSE = "GetContactFrequencyResponse";
    public static final QName GET_CONTACT_FREQUENCY_REQUEST = QName.get(E_GET_CONTACT_FREQUENCY_REQUEST, NAMESPACE);
    public static final QName GET_CONTACT_FREQUENCY_RESPONSE = QName.get(E_GET_CONTACT_FREQUENCY_RESPONSE, NAMESPACE);
    public static final String A_CONTACT_FREQUENCY_GRAPH_SPEC = "spec";
    public static final String A_CONTACT_FREQUENCY_GRAPH_RANGE = "range";
    public static final String A_CONTACT_FREQUENCY_GRAPH_INTERVAL = "interval";
    public static final String E_CONTACT_FREQUENCY_DATA = "data";
    public static final String E_CONTACT_FREQUENCY_DATA_POINT = "dataPoint";
    public static final String A_CONTACT_FREQUENCY_LABEL = "label";
    public static final String A_CONTACT_FREQUENCY_VALUE = "value";
    public static final String A_CONTACT_FREQUENCY_OFFSET_IN_MINUTES= "offsetInMinutes";
}
