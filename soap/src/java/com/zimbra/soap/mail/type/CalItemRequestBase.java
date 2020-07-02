/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.soap.mail.type;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import com.google.common.base.MoreObjects;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.json.jackson.annotate.ZimbraUniqueElement;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.NONE)
public abstract class CalItemRequestBase {

    /**
     * @zm-api-field-tag echo
     * @zm-api-field-description If specified, the created appointment is echoed back in the response as if a
     * GetMsgRequest was made
     */
    @XmlAttribute(name=MailConstants.A_CAL_ECHO /* echo */, required=false)
    private ZmBoolean echo;

    /**
     * @zm-api-field-tag max-inlined-length
     * @zm-api-field-description Maximum inlined length
     */
    @XmlAttribute(name=MailConstants.A_MAX_INLINED_LENGTH /* max */, required=false)
    private Integer maxSize;

    /**
     * @zm-api-field-tag want-html
     * @zm-api-field-description Set if want HTML included in echoing
     */
    @XmlAttribute(name=MailConstants.A_WANT_HTML /* html */, required=false)
    private ZmBoolean wantHtml;

    /**
     * @zm-api-field-tag neuter
     * @zm-api-field-description Set if want "neuter" set for echoed response
     */
    @XmlAttribute(name=MailConstants.A_NEUTER /* neuter */, required=false)
    private ZmBoolean neuter;

    /**
     * @zm-api-field-description Message
     */
    @ZimbraUniqueElement
    @XmlElement(name=MailConstants.E_MSG /* m */, required=false)
    private Msg msg;

    /**
     * @zm-api-field-tag force-send
     * @zm-api-field-description If set, ignore smtp 550 errors when sending the notification to attendees.
     * <br />
     * If unset, throw the soapfaultexception with invalid addresses so that client can give the forcesend option to
     * the end user.
     * <br />
     * The default is 1.
     */
    @XmlAttribute(name=MailConstants.A_CAL_FORCESEND /* forcesend */, required=false)
    private ZmBoolean forceSend;

    protected CalItemRequestBase() {
    }

    protected CalItemRequestBase(Msg msg) {
        this.msg = msg;
    }

    public void setEcho(Boolean echo) { this.echo = ZmBoolean.fromBool(echo); }
    public void setMaxSize(Integer maxSize) { this.maxSize = maxSize; }
    public void setWantHtml(Boolean wantHtml) { this.wantHtml = ZmBoolean.fromBool(wantHtml); }
    public void setNeuter(Boolean neuter) { this.neuter = ZmBoolean.fromBool(neuter); }
    public void setMsg(Msg msg) { this.msg = msg; }
    public void setForceSend(Boolean force) { this.forceSend = ZmBoolean.fromBool(force); }
    public Boolean getEcho() { return ZmBoolean.toBool(echo); }
    public Integer getMaxSize() { return maxSize; }
    public Boolean getWantHtml() { return ZmBoolean.toBool(wantHtml); }
    public Boolean getNeuter() { return ZmBoolean.toBool(neuter); }
    public Msg getMsg() { return msg; }
    public Boolean getForceSend() { return ZmBoolean.toBool(forceSend); }

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper
            .add("echo", echo)
            .add("maxSize", maxSize)
            .add("wantHtml", wantHtml)
            .add("neuter", neuter)
            .add("msg", msg);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
