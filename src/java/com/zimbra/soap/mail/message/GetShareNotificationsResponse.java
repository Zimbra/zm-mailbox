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
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.mail.type.ShareNotificationInfo;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=MailConstants.E_GET_SHARE_NOTIFICATIONS_RESPONSE)
public class GetShareNotificationsResponse {

    /**
     * @zm-api-field-description Share notification information
     */
    @XmlElement(name=MailConstants.E_SHARE /* share */, required=false)
    private List<ShareNotificationInfo> shares = Lists.newArrayList();

    public GetShareNotificationsResponse() {
    }

    public void setShares(Iterable <ShareNotificationInfo> shares) {
        this.shares.clear();
        if (shares != null) {
            Iterables.addAll(this.shares,shares);
        }
    }

    public void addShare(ShareNotificationInfo share) {
        this.shares.add(share);
    }

    public List<ShareNotificationInfo> getShares() {
        return Collections.unmodifiableList(shares);
    }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper
            .add("shares", shares);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
