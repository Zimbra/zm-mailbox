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

import com.google.common.base.MoreObjects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.type.NamedValue;

@XmlAccessorType(XmlAccessType.NONE)
public class ActivityInfo {

    // Values from enum com.zimbra.cs.mailbox.MailboxOperation
    /**
     * @zm-api-field-tag operation
     * @zm-api-field-description Operation
     */
    @XmlAttribute(name=MailConstants.A_OPERATION /* op */, required=true)
    private final String operation;

    /**
     * @zm-api-field-tag timestamp
     * @zm-api-field-description Timestamp
     */
    @XmlAttribute(name=MailConstants.A_TS /* ts */, required=true)
    private final long timeStamp;

    /**
     * @zm-api-field-tag item-id
     * @zm-api-field-description item-id
     */
    @XmlAttribute(name=MailConstants.A_ITEMID /* itemId */, required=true)
    private final String itemId;

    /**
     * @zm-api-field-tag version
     * @zm-api-field-description Version
     */
    @XmlAttribute(name=MailConstants.A_VERSION /* ver */, required=false)
    private Integer version;

    /**
     * @zm-api-field-tag user-agent
     * @zm-api-field-description User agent
     */
    @XmlAttribute(name=MailConstants.A_USER_AGENT /* ua */, required=false)
    private String userAgent;

    /**
     * @zm-api-field-tag email-address-of-user-performing-action
     * @zm-api-field-description Email address of user performing action
     */
    @XmlAttribute(name=MailConstants.A_EMAIL /* email */, required=false)
    private String email;

    /**
     * @zm-api-field-description Extra data for some of the operations
     */
    @XmlElement(name=MailConstants.E_ARG /* arg */, required=false)
    private List<NamedValue> args = Lists.newArrayList();

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private ActivityInfo() {
        this((String) null, -1L, (String) null);
    }

    public ActivityInfo(String operation, long timeStamp, String itemId) {
        this.operation = operation;
        this.timeStamp = timeStamp;
        this.itemId = itemId;
    }

    public static ActivityInfo fromOperationTimeStampItemId(
            String operation, long timeStamp, String itemId) {
        return new ActivityInfo(operation, timeStamp, itemId);
    }

    public void setVersion(Integer version) { this.version = version; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    public void setEmail(String email) { this.email = email; }
    public void setArgs(Iterable <NamedValue> args) {
        this.args.clear();
        if (args != null) {
            Iterables.addAll(this.args,args);
        }
    }

    public void setArgs(Map<String,String> args) {
        if (args != null) {
            for (Entry<String,String> entry : args.entrySet()) {
                this.addArg(new NamedValue(entry.getKey(),entry.getValue()));
            }
        }
    }

    public void addArg(NamedValue arg) {
        this.args.add(arg);
    }

    public String getOperation() { return operation; }
    public long getTimeStamp() { return timeStamp; }
    public String getItemId() { return itemId; }
    public Integer getVersion() { return version; }
    public String getUserAgent() {return userAgent; }
    public String getEmail() { return email; }
    public List<NamedValue> getArgs() { return args; }

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper
            .add("operation", operation)
            .add("timeStamp", timeStamp)
            .add("itemId", itemId)
            .add("version", version)
            .add("client", userAgent)
            .add("email", email)
            .add("args", args);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
