/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012 Zimbra, Inc.
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
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.base.Objects;
import com.zimbra.common.soap.OctopusXmlConstants;
import com.zimbra.soap.type.ZmBoolean;

/**
 * @zm-api-command-description Get notifications
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=OctopusXmlConstants.E_GET_NOTIFICATIONS_REQUEST)
public class GetNotificationsRequest {


    /**
     * @zm-api-field-tag mark-seen
     * @zm-api-field-description If set then all the notifications will be marked as seen.
     * Default: <b>unset</b>
     */
    @XmlAttribute(name=OctopusXmlConstants.A_MARKSEEN /* markSeen */, required=false)
    private ZmBoolean markSeen;

    private GetNotificationsRequest() {
    }

    public Boolean isMarkSeen() {
        return ZmBoolean.toBool(markSeen, false);
    }

    public void setMarkSeen(boolean markSeen) {
        this.markSeen = ZmBoolean.fromBool(markSeen);
    }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper;
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
