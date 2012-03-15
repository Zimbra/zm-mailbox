/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.soap.mail.message;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.mail.type.SetCalendarItemInfo;

/**
 * @zm-api-command-description Add an invite to an appointment.
 * <br />
 * The invite corresponds to a VEVENT component.  Based on the UID specified (required), a new appointment is created
 * in the default calendar if necessary.  If an appointment with the same UID exists, the appointment is updated with
 * the new invite only if the invite is not outdated, according to the iCalendar sequencing rule (based on SEQUENCE,
 * RECURRENCE-ID and DTSTAMP).
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=MailConstants.E_ADD_APPOINTMENT_INVITE_REQUEST)
public class AddAppointmentInviteRequest extends SetCalendarItemInfo {
}
