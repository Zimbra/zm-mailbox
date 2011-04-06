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
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.type.CustomMetadata;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name=MailConstants.E_SET_MAILBOX_METADATA_REQUEST)
public class SetMailboxMetadataRequest {

    @XmlElement(name=MailConstants.E_METADATA, required=false)
    private CustomMetadata metadata;

    public SetMailboxMetadataRequest() {
    }

    public void setMetadata(CustomMetadata metadata) {
        this.metadata = metadata;
    }

    public CustomMetadata getMetadata() { return metadata; }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
            .add("metadata", metadata)
            .toString();
    }
}
