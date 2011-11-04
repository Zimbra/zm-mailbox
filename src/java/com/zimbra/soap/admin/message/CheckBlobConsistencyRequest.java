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

package com.zimbra.soap.admin.message;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.IntIdAttr;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name=AdminConstants.E_CHECK_BLOB_CONSISTENCY_REQUEST)
public class CheckBlobConsistencyRequest {

    @XmlAttribute(name=AdminConstants.A_CHECK_SIZE, required=false)
    private final ZmBoolean checkSize;

    @XmlElement(name=AdminConstants.E_VOLUME, required=false)
    private List<IntIdAttr> volumes = Lists.newArrayList();

    @XmlElement(name=AdminConstants.E_MAILBOX, required=false)
    private List<IntIdAttr> mailboxes = Lists.newArrayList();

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private CheckBlobConsistencyRequest() {
        this((Boolean) null);
    }

    public CheckBlobConsistencyRequest(Boolean checkSize) {
        this.checkSize = ZmBoolean.fromBool(checkSize);
    }

    public void setVolumes(Iterable <IntIdAttr> volumes) {
        this.volumes.clear();
        if (volumes != null) {
            Iterables.addAll(this.volumes,volumes);
        }
    }

    public CheckBlobConsistencyRequest addVolume(IntIdAttr volume) {
        this.volumes.add(volume);
        return this;
    }

    public void setMailboxes(Iterable <IntIdAttr> mailboxes) {
        this.mailboxes.clear();
        if (mailboxes != null) {
            Iterables.addAll(this.mailboxes,mailboxes);
        }
    }

    public CheckBlobConsistencyRequest addMailbox(IntIdAttr mailbox) {
        this.mailboxes.add(mailbox);
        return this;
    }

    public Boolean getCheckSize() { return ZmBoolean.toBool(checkSize); }
    public List<IntIdAttr> getVolumes() {
        return Collections.unmodifiableList(volumes);
    }
    public List<IntIdAttr> getMailboxes() {
        return Collections.unmodifiableList(mailboxes);
    }
}
