/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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
package com.zimbra.cs.service.mail;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.DocumentDispatcher;
import com.zimbra.soap.DocumentService;

/**
 * @zm-service-description The Mail Service includes commands for managing mail and calendar information.
 *
 * @since May 26, 2004
 * @author schemers
 */
public final class MailService implements DocumentService {

    @Override
    public void registerHandlers(DocumentDispatcher dispatcher) {

        // noop
        dispatcher.registerHandler(MailConstants.NO_OP_REQUEST, new NoOp());

        // UUID generation
        dispatcher.registerHandler(MailConstants.GENERATE_UUID_REQUEST, new GenerateUUID());

        // searching
        dispatcher.registerHandler(MailConstants.BROWSE_REQUEST, new Browse());
        dispatcher.registerHandler(MailConstants.SEARCH_REQUEST, new Search());
        dispatcher.registerHandler(MailConstants.SEARCH_CONV_REQUEST, new SearchConv());
        dispatcher.registerHandler(MailConstants.REJECT_SAVE_SEARCH_PROMPT_REQUEST, new RejectSaveSearchPrompt());
        dispatcher.registerHandler(MailConstants.CLEAR_SEARCH_HISTORY_REQUEST, new ClearSearchHistory());

        // dumpster
        dispatcher.registerHandler(MailConstants.EMPTY_DUMPSTER_REQUEST, new EmptyDumpster());

        // items
        dispatcher.registerHandler(MailConstants.GET_ITEM_REQUEST, new GetItem());
        dispatcher.registerHandler(MailConstants.ITEM_ACTION_REQUEST, new ItemAction());
        dispatcher.registerHandler(MailConstants.GET_METADATA_REQUEST, new GetCustomMetadata());
        dispatcher.registerHandler(MailConstants.SET_METADATA_REQUEST, new SetCustomMetadata());
        dispatcher.registerHandler(MailConstants.GET_MAILBOX_METADATA_REQUEST, new GetMailboxMetadata());
        dispatcher.registerHandler(MailConstants.SET_MAILBOX_METADATA_REQUEST, new SetMailboxMetadata());
        dispatcher.registerHandler(MailConstants.MODIFY_MAILBOX_METADATA_REQUEST, new ModifyMailboxMetadata());

        // conversations
        dispatcher.registerHandler(MailConstants.GET_CONV_REQUEST, new GetConv());
        dispatcher.registerHandler(MailConstants.CONV_ACTION_REQUEST, new ConvAction());

        // messages
        dispatcher.registerHandler(MailConstants.GET_MSG_REQUEST, new GetMsg());
        dispatcher.registerHandler(MailConstants.GET_MSG_METADATA_REQUEST, new GetMsgMetadata());
        dispatcher.registerHandler(MailConstants.MSG_ACTION_REQUEST, new MsgAction());
        dispatcher.registerHandler(MailConstants.SEND_MSG_REQUEST, new SendMsg());
        dispatcher.registerHandler(MailConstants.SEND_REPORT_REQUEST, new SendDeliveryReport());
        dispatcher.registerHandler(MailConstants.SEND_SHARE_NOTIFICATION_REQUEST, new SendShareNotification());
        dispatcher.registerHandler(MailConstants.BOUNCE_MSG_REQUEST, new BounceMsg());
        dispatcher.registerHandler(MailConstants.ADD_MSG_REQUEST, new AddMsg());
        dispatcher.registerHandler(MailConstants.SAVE_DRAFT_REQUEST, new SaveDraft());
        dispatcher.registerHandler(MailConstants.REMOVE_ATTACHMENTS_REQUEST, new RemoveAttachments());

        // folders
        dispatcher.registerHandler(MailConstants.GET_FOLDER_REQUEST, new GetFolder());
        dispatcher.registerHandler(MailConstants.CREATE_FOLDER_REQUEST, new CreateFolder());
        dispatcher.registerHandler(MailConstants.FOLDER_ACTION_REQUEST, new FolderAction());

        // tags
        dispatcher.registerHandler(MailConstants.GET_TAG_REQUEST, new GetTag());
        dispatcher.registerHandler(MailConstants.CREATE_TAG_REQUEST, new CreateTag());
        dispatcher.registerHandler(MailConstants.TAG_ACTION_REQUEST, new TagAction());

        // saved searches
        dispatcher.registerHandler(MailConstants.GET_SEARCH_FOLDER_REQUEST, new GetSearchFolder());
        dispatcher.registerHandler(MailConstants.CREATE_SEARCH_FOLDER_REQUEST, new CreateSearchFolder());
        dispatcher.registerHandler(MailConstants.MODIFY_SEARCH_FOLDER_REQUEST, new ModifySearchFolder());

        // mountpoints
        dispatcher.registerHandler(MailConstants.CREATE_MOUNTPOINT_REQUEST, new CreateMountpoint());
        dispatcher.registerHandler(MailConstants.ENABLE_SHARED_REMINDER_REQUEST, new EnableSharedReminder());

        // contacts
        dispatcher.registerHandler(MailConstants.GET_CONTACTS_REQUEST, new GetContacts());
        dispatcher.registerHandler(MailConstants.CREATE_CONTACT_REQUEST, new CreateContact());
        dispatcher.registerHandler(MailConstants.MODIFY_CONTACT_REQUEST, new ModifyContact());
        dispatcher.registerHandler(MailConstants.CONTACT_ACTION_REQUEST, new ContactAction());
        dispatcher.registerHandler(MailConstants.EXPORT_CONTACTS_REQUEST, new ExportContacts());
        dispatcher.registerHandler(MailConstants.IMPORT_CONTACTS_REQUEST, new ImportContacts());
        dispatcher.registerHandler(MailConstants.GET_CONTACT_BACKUP_LIST_REQUEST, new GetContactBackupList());

        // notes
        if (LC.notes_enabled.booleanValue()) {
            dispatcher.registerHandler(MailConstants.GET_NOTE_REQUEST, new GetNote());
            dispatcher.registerHandler(MailConstants.CREATE_NOTE_REQUEST, new CreateNote());
            dispatcher.registerHandler(MailConstants.NOTE_ACTION_REQUEST, new NoteAction());
        }

        // sync
        dispatcher.registerHandler(MailConstants.SYNC_REQUEST, new Sync());

        // Filter rules
        dispatcher.registerHandler(MailConstants.GET_FILTER_RULES_REQUEST, new GetFilterRules());
        dispatcher.registerHandler(MailConstants.MODIFY_FILTER_RULES_REQUEST, new ModifyFilterRules());
        dispatcher.registerHandler(MailConstants.APPLY_FILTER_RULES_REQUEST, new ApplyFilterRules());
        dispatcher.registerHandler(MailConstants.GET_OUTGOING_FILTER_RULES_REQUEST, new GetOutgoingFilterRules());
        dispatcher.registerHandler(MailConstants.MODIFY_OUTGOING_FILTER_RULES_REQUEST, new ModifyOutgoingFilterRules());
        dispatcher.registerHandler(MailConstants.APPLY_OUTGOING_FILTER_RULES_REQUEST, new ApplyOutgoingFilterRules());

        // Calendar

        dispatcher.registerHandler(MailConstants.GET_APPT_SUMMARIES_REQUEST, new GetApptSummaries());
        dispatcher.registerHandler(MailConstants.GET_APPOINTMENT_REQUEST, new GetAppointment());
        dispatcher.registerHandler(MailConstants.SET_APPOINTMENT_REQUEST, new SetAppointment());
        dispatcher.registerHandler(MailConstants.CREATE_APPOINTMENT_REQUEST, new CreateAppointment());
        dispatcher.registerHandler(MailConstants.CREATE_APPOINTMENT_EXCEPTION_REQUEST, new CreateAppointmentException());
        dispatcher.registerHandler(MailConstants.MODIFY_APPOINTMENT_REQUEST, new ModifyAppointment());
        dispatcher.registerHandler(MailConstants.CANCEL_APPOINTMENT_REQUEST, new CancelAppointment());
        dispatcher.registerHandler(MailConstants.FORWARD_APPOINTMENT_REQUEST, new ForwardAppointment());
        dispatcher.registerHandler(MailConstants.FORWARD_APPOINTMENT_INVITE_REQUEST, new ForwardAppointmentInvite());
        dispatcher.registerHandler(MailConstants.ADD_APPOINTMENT_INVITE_REQUEST, new AddAppointmentInvite());
        dispatcher.registerHandler(MailConstants.COUNTER_APPOINTMENT_REQUEST, new CounterAppointment());
        dispatcher.registerHandler(MailConstants.DECLINE_COUNTER_APPOINTMENT_REQUEST, new DeclineCounterAppointment());
        dispatcher.registerHandler(MailConstants.IMPORT_APPOINTMENTS_REQUEST, new ImportAppointments());

        dispatcher.registerHandler(MailConstants.GET_TASK_SUMMARIES_REQUEST, new GetTaskSummaries());
        dispatcher.registerHandler(MailConstants.GET_TASK_REQUEST, new GetTask());
        dispatcher.registerHandler(MailConstants.SET_TASK_REQUEST, new SetTask());
        dispatcher.registerHandler(MailConstants.CREATE_TASK_REQUEST, new CreateTask());
        dispatcher.registerHandler(MailConstants.CREATE_TASK_EXCEPTION_REQUEST, new CreateTaskException());
        dispatcher.registerHandler(MailConstants.MODIFY_TASK_REQUEST, new ModifyTask());
        dispatcher.registerHandler(MailConstants.CANCEL_TASK_REQUEST, new CancelTask());
        dispatcher.registerHandler(MailConstants.ADD_TASK_INVITE_REQUEST, new AddTaskInvite());
        dispatcher.registerHandler(MailConstants.COMPLETE_TASK_INSTANCE_REQUEST, new CompleteTaskInstance());

        dispatcher.registerHandler(MailConstants.GET_CALITEM_SUMMARIES_REQUEST, new GetCalendarItemSummaries());
//        dispatcher.registerHandler(GET_CALITEM_REQUEST, new GetCalendarItem());
//        dispatcher.registerHandler(SET_CALITEM_REQUEST, new SetCalendarItem());
//        dispatcher.registerHandler(CREATE_CALITEM_REQUEST, new CreateCalendarItem());
//        dispatcher.registerHandler(CREATE_CALITEM_EXCEPTION_REQUEST, new CreateCalendarItemException());
//        dispatcher.registerHandler(MODIFY_CALITEM_REQUEST, new ModifyCalendarItem());
//        dispatcher.registerHandler(CANCEL_CALITEM_REQUEST, new CancelCalendarItem());

        dispatcher.registerHandler(MailConstants.SEND_INVITE_REPLY_REQUEST, new SendInviteReply());
        dispatcher.registerHandler(MailConstants.ICAL_REPLY_REQUEST, new ICalReply());
        dispatcher.registerHandler(MailConstants.GET_FREE_BUSY_REQUEST, new GetFreeBusy());
        dispatcher.registerHandler(MailConstants.GET_WORKING_HOURS_REQUEST, new GetWorkingHours());
        dispatcher.registerHandler(MailConstants.GET_ICAL_REQUEST, new GetICal());
        dispatcher.registerHandler(MailConstants.ANNOUNCE_ORGANIZER_CHANGE_REQUEST, new AnnounceOrganizerChange());
        dispatcher.registerHandler(MailConstants.DISMISS_CALITEM_ALARM_REQUEST, new DismissCalendarItemAlarm());
        dispatcher.registerHandler(MailConstants.SNOOZE_CALITEM_ALARM_REQUEST, new SnoozeCalendarItemAlarm());
        dispatcher.registerHandler(MailConstants.GET_MINI_CAL_REQUEST, new GetMiniCal());
        dispatcher.registerHandler(MailConstants.GET_RECUR_REQUEST, new GetRecur());
        dispatcher.registerHandler(MailConstants.EXPAND_RECUR_REQUEST, new ExpandRecur());
        dispatcher.registerHandler(MailConstants.CHECK_RECUR_CONFLICTS_REQUEST, new CheckRecurConflicts());

        dispatcher.registerHandler(MailConstants.SEND_VERIFICATION_CODE_REQUEST, new SendVerificationCode());
        dispatcher.registerHandler(MailConstants.VERIFY_CODE_REQUEST, new VerifyCode());
        dispatcher.registerHandler(MailConstants.INVALIDATE_REMINDER_DEVICE_REQUEST, new InvalidateReminderDevice());

        // spell check
        dispatcher.registerHandler(MailConstants.CHECK_SPELLING_REQUEST, new CheckSpelling());
        dispatcher.registerHandler(MailConstants.GET_SPELL_DICTIONARIES_REQUEST, new GetSpellDictionaries());

        // Documents
        dispatcher.registerHandler(MailConstants.SAVE_DOCUMENT_REQUEST, new com.zimbra.cs.service.doc.SaveDocument());
        dispatcher.registerHandler(MailConstants.DIFF_DOCUMENT_REQUEST, new com.zimbra.cs.service.doc.DiffDocument());
        dispatcher.registerHandler(MailConstants.LIST_DOCUMENT_REVISIONS_REQUEST, new com.zimbra.cs.service.doc.ListDocumentRevisions());
        dispatcher.registerHandler(MailConstants.PURGE_REVISION_REQUEST, new com.zimbra.cs.service.mail.PurgeRevision());

        // data source
        dispatcher.registerHandler(MailConstants.GET_DATA_SOURCES_REQUEST, new GetDataSources());
        dispatcher.registerHandler(MailConstants.CREATE_DATA_SOURCE_REQUEST, new CreateDataSource());
        dispatcher.registerHandler(MailConstants.MODIFY_DATA_SOURCE_REQUEST, new ModifyDataSource());
        dispatcher.registerHandler(MailConstants.TEST_DATA_SOURCE_REQUEST, new TestDataSource());
        dispatcher.registerHandler(MailConstants.DELETE_DATA_SOURCE_REQUEST, new DeleteDataSource());
        dispatcher.registerHandler(MailConstants.IMPORT_DATA_REQUEST, new ImportData());
        dispatcher.registerHandler(MailConstants.GET_IMPORT_STATUS_REQUEST, new GetImportStatus());
        dispatcher.registerHandler(MailConstants.GET_DATA_SOURCE_USAGE_REQUEST, new GetDataSourceUsage());

        // waitset
        dispatcher.registerHandler(MailConstants.CREATE_WAIT_SET_REQUEST, new CreateWaitSet());
        dispatcher.registerHandler(MailConstants.WAIT_SET_REQUEST, new WaitSetRequest());
        dispatcher.registerHandler(MailConstants.DESTROY_WAIT_SET_REQUEST, new DestroyWaitSet());

        // Account ACL
        dispatcher.registerHandler(MailConstants.GET_PERMISSION_REQUEST, new GetPermission());
        dispatcher.registerHandler(MailConstants.CHECK_PERMISSION_REQUEST, new CheckPermission());
        dispatcher.registerHandler(MailConstants.GRANT_PERMISSION_REQUEST, new GrantPermission());
        dispatcher.registerHandler(MailConstants.REVOKE_PERMISSION_REQUEST, new RevokePermission());

        // folder ACl
        dispatcher.registerHandler(MailConstants.GET_EFFECTIVE_FOLDER_PERMS_REQUEST, new GetEffectiveFolderPerms());

        // yahoo auth
        dispatcher.registerHandler(MailConstants.GET_YAHOO_COOKIE_REQUEST, new GetYahooCookie());
        dispatcher.registerHandler(MailConstants.GET_YAHOO_AUTH_TOKEN_REQUEST, new GetYahooAuthToken());

        dispatcher.registerHandler(MailConstants.AUTO_COMPLETE_REQUEST, new AutoComplete());
        dispatcher.registerHandler(MailConstants.RANKING_ACTION_REQUEST, new RankingAction());

        // comments
        dispatcher.registerHandler(MailConstants.ADD_COMMENT_REQUEST, new AddComment());
        dispatcher.registerHandler(MailConstants.GET_COMMENTS_REQUEST, new GetComments());

        // share
        dispatcher.registerHandler(MailConstants.GET_SHARE_NOTIFICATIONS_REQUEST, new GetShareNotifications());

        dispatcher.registerHandler(MailConstants.GET_SYSTEM_RETENTION_POLICY_REQUEST, new GetSystemRetentionPolicy());

        // IMAP
        dispatcher.registerHandler(MailConstants.GET_IMAP_RECENT_REQUEST, new GetIMAPRecent());
        dispatcher.registerHandler(MailConstants.GET_IMAP_RECENT_CUTOFF_REQUEST, new GetIMAPRecentCutoff());
        dispatcher.registerHandler(MailConstants.RECORD_IMAP_SESSION_REQUEST, new RecordIMAPSession());
        dispatcher.registerHandler(MailConstants.IMAP_COPY_REQUEST, new ImapCopy());
        dispatcher.registerHandler(MailConstants.SAVE_IMAP_SUBSCRIPTIONS_REQUEST, new SaveIMAPSubscriptions());
        dispatcher.registerHandler(MailConstants.LIST_IMAP_SUBSCRIPTIONS_REQUEST, new ListIMAPSubscriptions());
        dispatcher.registerHandler(MailConstants.OPEN_IMAP_FOLDER_REQUEST, new OpenImapFolder());
        dispatcher.registerHandler(MailConstants.BEGIN_TRACKING_IMAP_REQUEST, new BeginTrackingImap());
        dispatcher.registerHandler(MailConstants.GET_LAST_ITEM_ID_IN_MAILBOX_REQUEST, new GetLastItemIdInMailbox());
        dispatcher.registerHandler(MailConstants.GET_MODIFIED_ITEMS_IDS_REQUEST, new GetModifiedItemsIDs());
        dispatcher.registerHandler(MailConstants.RESET_RECENT_MESSAGE_COUNT_REQUEST, new ResetRecentMessageCount());

        // Contacts API
        dispatcher.registerHandler(MailConstants.RESTORE_CONTACTS_REQUEST, new RestoreContacts());

        // SearchAction API
        dispatcher.registerHandler(MailConstants.SEARCH_ACTION_REQUEST, new SearchAction());

        // Profile Image API
        dispatcher.registerHandler(MailConstants.MODIFY_PROFILE_IMAGE_REQUEST, new ModifyProfileImage());

        // Password reset API
        dispatcher.registerHandler(MailConstants.RECOVER_ACCOUNT_REQUEST, new RecoverAccount());
        dispatcher.registerHandler(MailConstants.SET_RECOVERY_EMAIL_REQUEST, new SetRecoveryAccount());
    }
}
