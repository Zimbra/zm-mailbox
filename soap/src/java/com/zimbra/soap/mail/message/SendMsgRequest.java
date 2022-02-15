/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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

package com.zimbra.soap.mail.message;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.base.MoreObjects;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.mail.type.MsgToSend;
import com.zimbra.soap.type.ZmBoolean;

// TODO: indicate whether to save in SentMail (or some other folder)
/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required false
 * @zm-api-command-description Send message
 * <ul>
 * <li> Supports (f)rom, (t)o, (c)c, (b)cc, (r)eply-to, (s)ender, read-receipt (n)otification "type" on
 *      <b>&lt;e></b> elements.
 * <li> Only allowed one top-level <b>&lt;mp></b> but can nest <b>&lt;mp></b>s within if multipart/*
 * <li> A leaf <b>&lt;mp></b> can have inlined content
 *      (<b>&lt;mp ct="{content-type}">&lt;content>...&lt;/content>&lt;/mp></b>)
 * <li> A leaf <b>&lt;mp></b> can have referenced content (<b>&lt;mp>&lt;attach ...>&lt;/mp></b>)
 * <li> Any <b>&lt;mp></b> can have a Content-ID header attached to it.
 * <li> On reply/forward, set origid on <b>&lt;m></b> element and set rt to "r" or "w", respectively
 * <li> Can optionally set identity-id to specify the identity being used to compose the message
 * <li> If noSave is set, a copy will <b>not</b> be saved to sent regardless of account/identity settings
 * <li> Can set priority high (!) or low (?) on sent message by specifying "f" attr on <b>&lt;m></b>
 * <li> The message to be sent can be fully specified under the <b>&lt;m></b> element or, to compose the message
 *      remotely remotely, upload it via FileUploadServlet, and submit it through our server using something like:
 *      <pre>
 *         &lt;SendMsgRequest [suid="{send-uid}"] [needCalendarSentByFixup="0|1"]>
 *             &lt;m aid="{uploaded-MIME-body-ID}" [origid="..." rt="r|w"]/>
 *         &lt;/SendMsgRequest>
 *      </pre>
 * <li> If the message is saved to the sent folder then the ID of the message is returned.  Otherwise, no ID is
 *      returned -- just a <b>&lt;m></b> is returned.
 * </ul>
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=MailConstants.E_SEND_MSG_REQUEST)
public class SendMsgRequest {

    /**
     * @zm-api-field-tag add-sent-by
     * @zm-api-field-description If set then Add SENT-BY parameter to ORGANIZER and/or ATTENDEE properties in
     * iCalendar part when sending message on behalf of another user.  Default is unset.
     */
    @XmlAttribute(name=MailConstants.A_NEED_CALENDAR_SENTBY_FIXUP /* needCalendarSentByFixup */, required=false)
    private ZmBoolean needCalendarSentbyFixup;
    
    /**
     * @zm-api-field-tag calendar-forward
     * @zm-api-field-description Indicates whether this a forward of calendar invitation in which
     * case the server sends Forward Invitation Notification, default is unset.
     */
    @XmlAttribute(name=MailConstants.A_IS_CALENDAR_FORWARD /* isCalendarForward */, required=false)
    private ZmBoolean isCalendarForward;

    /**
     * @zm-api-field-tag no-save
     * @zm-api-field-description If set, a copy will <b>not</b> be saved to sent regardless of account/identity
     * settings
     */
    @XmlAttribute(name=MailConstants.A_NO_SAVE_TO_SENT /* noSave */, required=false)
    private ZmBoolean noSaveToSent;

    /**
     * @zm-api-field-tag fetch-saved-msg
     * @zm-api-field-description If set, return the copy of the sent message, if it was saved, in the response.
     */
    @XmlAttribute(name=MailConstants.A_FETCH_SAVED_MSG /* fetchSavedMsg */, required=false)
    private ZmBoolean fetchSavedMsg;

    /**
     * @zm-api-field-tag send-uid
     * @zm-api-field-description Send UID
     */
    @XmlAttribute(name=MailConstants.A_SEND_UID /* suid */, required=false)
    private String sendUid;

    // E_INVITE child is not allowed
    /**
     * @zm-api-field-description Message
     */
    @XmlElement(name=MailConstants.E_MSG /* m */, required=false)
    private MsgToSend msg;

    /**
     * @zm-api-field-tag delivery-receipt-notification
     * @zm-api-field-description If set, delivery receipt notification will be sent.
     */
    @XmlAttribute(name=MailConstants.A_DELIVERY_RECEIPT_NOTIFICATION /* deliveryReport */, required=false)
    private ZmBoolean deliveryReport;

    public SendMsgRequest() {
    }

    public void setNeedCalendarSentbyFixup(Boolean needCalendarSentbyFixup) {
        this.needCalendarSentbyFixup = ZmBoolean.fromBool(needCalendarSentbyFixup);
    }
    
    public void setIsCalendarForward(Boolean isCalendarForward) {
        this.isCalendarForward = ZmBoolean.fromBool(isCalendarForward);
    }

    public void setNoSaveToSent(Boolean noSaveToSent) {
        this.noSaveToSent = ZmBoolean.fromBool(noSaveToSent);
    }

    public void setFetchSavedMsg(Boolean fetchSavedMsg) {
        this.fetchSavedMsg = ZmBoolean.fromBool(fetchSavedMsg);
    }

    public void setDeliveryReport(Boolean deliveryReport) {
        this.deliveryReport = ZmBoolean.fromBool(deliveryReport);
    }

    public void setSendUid(String sendUid) { this.sendUid = sendUid; }
    public void setMsg(MsgToSend msg) { this.msg = msg; }

    public Boolean getNeedCalendarSentbyFixup() { return ZmBoolean.toBool(needCalendarSentbyFixup); }
    public Boolean getIsCalendarForward() { return ZmBoolean.toBool(isCalendarForward); }
    public Boolean getNoSaveToSent() { return ZmBoolean.toBool(noSaveToSent); }
    public Boolean getFetchSavedMsg() { return ZmBoolean.toBool(fetchSavedMsg); }
    public String getSendUid() { return sendUid; }
    public Boolean getDeliveryReport() { return ZmBoolean.toBool(deliveryReport); }
    public MsgToSend getMsg() { return msg; }

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper
            .add("needCalendarSentbyFixup", needCalendarSentbyFixup)
            .add("isCalendarForward", isCalendarForward)
            .add("noSaveToSent", noSaveToSent)
            .add("fetchSavedMsg", fetchSavedMsg)
            .add("sendUid", sendUid)
            .add("deliveryReport", deliveryReport)
            .add("msg", msg);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
