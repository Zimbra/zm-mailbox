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

package com.zimbra.soap.mail.type;

import com.google.common.base.Objects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;
import java.util.Map;

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
    public void setEmail(String email) { this.email = email; }
    public void setArgs(Iterable <NamedValue> args) {
        this.args.clear();
        if (args != null) {
            Iterables.addAll(this.args,args);
        }
    }

    public void setArgs(Map<String,String> args) {
        if (args != null) {
            for (String key : args.keySet()) {
                this.addArg(new NamedValue(key,args.get(key)));
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
    public String getEmail() { return email; }
    public List<NamedValue> getArgs() { return args; }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper
            .add("operation", operation)
            .add("timeStamp", timeStamp)
            .add("itemId", itemId)
            .add("version", version)
            .add("email", email)
            .add("args", args);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
