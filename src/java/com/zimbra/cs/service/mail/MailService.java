/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007 Zimbra, Inc.
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

/*
 * Created on May 26, 2004
 */
package com.zimbra.cs.service.mail;


import com.zimbra.soap.DocumentDispatcher;
import com.zimbra.soap.DocumentService;
import com.zimbra.common.soap.MailConstants;

/**
 * @author schemers
 */
public class MailService implements DocumentService {

    public void registerHandlers(DocumentDispatcher dispatcher) {

        // noop
        dispatcher.registerHandler(MailConstants.NO_OP_REQUEST, new NoOp());

        // searching
        dispatcher.registerHandler(MailConstants.BROWSE_REQUEST, new Browse());
        dispatcher.registerHandler(MailConstants.SEARCH_REQUEST, new Search());
        dispatcher.registerHandler(MailConstants.SEARCH_CONV_REQUEST, new SearchConv());

        // items
        dispatcher.registerHandler(MailConstants.GET_ITEM_REQUEST, new GetItem());
        dispatcher.registerHandler(MailConstants.ITEM_ACTION_REQUEST, new ItemAction());

        // conversations
        dispatcher.registerHandler(MailConstants.GET_CONV_REQUEST, new GetConv());
        dispatcher.registerHandler(MailConstants.CONV_ACTION_REQUEST, new ConvAction());

        // messages
        dispatcher.registerHandler(MailConstants.GET_MSG_REQUEST, new GetMsg());
        dispatcher.registerHandler(MailConstants.GET_MSG_METADATA_REQUEST, new GetMsgMetadata());
        dispatcher.registerHandler(MailConstants.MSG_ACTION_REQUEST, new MsgAction());
        dispatcher.registerHandler(MailConstants.SEND_MSG_REQUEST, new SendMsg());
        dispatcher.registerHandler(MailConstants.ADD_MSG_REQUEST, new AddMsg());
        dispatcher.registerHandler(MailConstants.SAVE_DRAFT_REQUEST, new SaveDraft());

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

        // saved searches
        dispatcher.registerHandler(MailConstants.CREATE_MOUNTPOINT_REQUEST, new CreateMountpoint());

        // contacts
        dispatcher.registerHandler(MailConstants.GET_CONTACTS_REQUEST, new GetContacts());
        dispatcher.registerHandler(MailConstants.CREATE_CONTACT_REQUEST, new CreateContact());
        dispatcher.registerHandler(MailConstants.MODIFY_CONTACT_REQUEST, new ModifyContact());
        dispatcher.registerHandler(MailConstants.CONTACT_ACTION_REQUEST, new ContactAction());
        dispatcher.registerHandler(MailConstants.EXPORT_CONTACTS_REQUEST, new ExportContacts());
        dispatcher.registerHandler(MailConstants.IMPORT_CONTACTS_REQUEST, new ImportContacts());

        // notes
        dispatcher.registerHandler(MailConstants.GET_NOTE_REQUEST, new GetNote());
        dispatcher.registerHandler(MailConstants.CREATE_NOTE_REQUEST, new CreateNote());
        dispatcher.registerHandler(MailConstants.NOTE_ACTION_REQUEST, new NoteAction());

        // sync
        dispatcher.registerHandler(MailConstants.SYNC_REQUEST, new Sync());

        // filter rules
        dispatcher.registerHandler(MailConstants.GET_RULES_REQUEST, new GetRules());
        dispatcher.registerHandler(MailConstants.SAVE_RULES_REQUEST, new SaveRules());


        // Calendar

        dispatcher.registerHandler(MailConstants.GET_APPT_SUMMARIES_REQUEST, new GetApptSummaries());
        dispatcher.registerHandler(MailConstants.GET_APPOINTMENT_REQUEST, new GetAppointment());
        dispatcher.registerHandler(MailConstants.SET_APPOINTMENT_REQUEST, new SetAppointment());
        dispatcher.registerHandler(MailConstants.CREATE_APPOINTMENT_REQUEST, new CreateAppointment());
        dispatcher.registerHandler(MailConstants.CREATE_APPOINTMENT_EXCEPTION_REQUEST, new CreateAppointmentException());
        dispatcher.registerHandler(MailConstants.MODIFY_APPOINTMENT_REQUEST, new ModifyAppointment());
        dispatcher.registerHandler(MailConstants.CANCEL_APPOINTMENT_REQUEST, new CancelAppointment());
        dispatcher.registerHandler(MailConstants.IMPORT_APPOINTMENTS_REQUEST, new ImportAppointments());

        dispatcher.registerHandler(MailConstants.GET_TASK_SUMMARIES_REQUEST, new GetTaskSummaries());
        dispatcher.registerHandler(MailConstants.GET_TASK_REQUEST, new GetTask());
        dispatcher.registerHandler(MailConstants.SET_TASK_REQUEST, new SetTask());
        dispatcher.registerHandler(MailConstants.CREATE_TASK_REQUEST, new CreateTask());
        dispatcher.registerHandler(MailConstants.CREATE_TASK_EXCEPTION_REQUEST, new CreateTaskException());
        dispatcher.registerHandler(MailConstants.MODIFY_TASK_REQUEST, new ModifyTask());
        dispatcher.registerHandler(MailConstants.CANCEL_TASK_REQUEST, new CancelTask());
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
        dispatcher.registerHandler(MailConstants.GET_ICAL_REQUEST, new GetICal());
        dispatcher.registerHandler(MailConstants.ANNOUNCE_ORGANIZER_CHANGE_REQUEST, new AnnounceOrganizerChange());
        dispatcher.registerHandler(MailConstants.DISMISS_CALITEM_ALARM_REQUEST, new DismissCalendarItemAlarm());

        // spell check
        dispatcher.registerHandler(MailConstants.CHECK_SPELLING_REQUEST, new CheckSpelling());

        // TODO: move to a different service.
        // wiki
        dispatcher.registerHandler(MailConstants.SAVE_DOCUMENT_REQUEST, new com.zimbra.cs.service.wiki.SaveDocument());
        dispatcher.registerHandler(MailConstants.SAVE_WIKI_REQUEST, new com.zimbra.cs.service.wiki.SaveWiki());
        dispatcher.registerHandler(MailConstants.GET_WIKI_REQUEST, new com.zimbra.cs.service.wiki.GetWiki());
        dispatcher.registerHandler(MailConstants.WIKI_ACTION_REQUEST, new com.zimbra.cs.service.wiki.WikiAction());

        // data source
        dispatcher.registerHandler(MailConstants.GET_DATA_SOURCES_REQUEST, new GetDataSources());
        dispatcher.registerHandler(MailConstants.CREATE_DATA_SOURCE_REQUEST, new CreateDataSource());
        dispatcher.registerHandler(MailConstants.MODIFY_DATA_SOURCE_REQUEST, new ModifyDataSource());
        dispatcher.registerHandler(MailConstants.TEST_DATA_SOURCE_REQUEST, new TestDataSource());
        dispatcher.registerHandler(MailConstants.DELETE_DATA_SOURCE_REQUEST, new DeleteDataSource());
        dispatcher.registerHandler(MailConstants.IMPORT_DATA_REQUEST, new ImportData());
        dispatcher.registerHandler(MailConstants.GET_IMPORT_STATUS_REQUEST, new GetImportStatus());
        
        // waitset
        dispatcher.registerHandler(MailConstants.CREATE_WAIT_SET_REQUEST, new CreateWaitSet());
        dispatcher.registerHandler(MailConstants.WAIT_SET_REQUEST, new WaitSetRequest());
        dispatcher.registerHandler(MailConstants.DESTROY_WAIT_SET_REQUEST, new DestroyWaitSet());
        
        
    }
}
