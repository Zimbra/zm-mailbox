/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.mailbox.calendar;

import java.util.Locale;

import com.zimbra.cs.util.L10nUtil;
import com.zimbra.cs.util.L10nUtil.Module;

public class CalendarL10n {

    public static String getMessage(MsgKey key, Locale locale, Object... args) {
        return L10nUtil.getMessage(Module.calendar,
                                   key.toString(),
                                   locale,
                                   args);
    }

    /**
     * List all calendar-specific message keys here
     */
    public static enum MsgKey {
        subjectCancelled,
        cancelRemovedFromAttendeeList,
        cancelAppointment,
        cancelAppointmentInstance,
        cancelAppointmentInstanceWhich,

        replySubjectAccept,
        replySubjectTentative,
        replySubjectDecline,

        defaultReplyAccept,
        defaultReplyTentativelyAccept,
        defaultReplyDecline,
        defaultReplyOther,
        resourceDefaultReplyAccept,
        resourceDefaultReplyTentativelyAccept,
        resourceDefaultReplyDecline,

        resourceReplyOriginalInviteSeparatorLabel,

        resourceConflictDateTimeFormat,
        resourceConflictTimeOnlyFormat,

        resourceDeclineReasonRecurring,
        resourceDeclineReasonConflict,
        resourceConflictScheduledBy
    }
}
