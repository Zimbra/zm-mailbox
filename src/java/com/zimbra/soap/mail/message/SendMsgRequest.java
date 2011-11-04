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

import com.google.common.base.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.mail.type.CalendarItemMsg;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name=MailConstants.E_SEND_MSG_REQUEST)
public class SendMsgRequest {

    @XmlAttribute(name=MailConstants.A_NEED_CALENDAR_SENTBY_FIXUP /* needCalendarSentByFixup */, required=false)
    private ZmBoolean needCalendarSentbyFixup;

    @XmlAttribute(name=MailConstants.A_NO_SAVE_TO_SENT /* noSave */, required=false)
    private ZmBoolean noSaveToSent;

    @XmlAttribute(name=MailConstants.A_SEND_UID /* suid */, required=false)
    private String sendUid;

    // E_INVITE child is not allowed
    @XmlElement(name=MailConstants.E_MSG /* m */, required=false)
    private CalendarItemMsg msg;

    public SendMsgRequest() {
    }

    public void setNeedCalendarSentbyFixup(Boolean needCalendarSentbyFixup) {
        this.needCalendarSentbyFixup = ZmBoolean.fromBool(needCalendarSentbyFixup);
    }
    public void setNoSaveToSent(Boolean noSaveToSent) {
        this.noSaveToSent = ZmBoolean.fromBool(noSaveToSent);
    }
    public void setSendUid(String sendUid) { this.sendUid = sendUid; }
    public void setMsg(CalendarItemMsg msg) { this.msg = msg; }
    public Boolean getNeedCalendarSentbyFixup() { return ZmBoolean.toBool(needCalendarSentbyFixup); }
    public Boolean getNoSaveToSent() { return ZmBoolean.toBool(noSaveToSent); }
    public String getSendUid() { return sendUid; }
    public CalendarItemMsg getMsg() { return msg; }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("needCalendarSentbyFixup", needCalendarSentbyFixup)
            .add("noSaveToSent", noSaveToSent)
            .add("sendUid", sendUid)
            .add("msg", msg);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
