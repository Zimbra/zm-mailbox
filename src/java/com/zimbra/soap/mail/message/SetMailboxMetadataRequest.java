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
import com.zimbra.soap.mail.type.MailCustomMetadata;

/**
 * @zm-api-command-description Set Mailbox Metadata
 * <ul>
 * <li> Setting a mailbox metadata section but providing no key/value pairs will remove the section from mailbox
 *      metadata
 * <li> Empty value not allowed
 * <li> <b>{metadata-section-key}</b> must be no more than 36 characters long and must be in the format of
 *      <b>{namespace}:{section-name}</b>.  currently the only valid namespace is <b>"zwc"</b>.
 * </ul>
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=MailConstants.E_SET_MAILBOX_METADATA_REQUEST)
public class SetMailboxMetadataRequest {

    /**
     * @zm-api-field-description New metadata information
     */
    @XmlElement(name=MailConstants.E_METADATA /* meta */, required=false)
    private MailCustomMetadata metadata;

    public SetMailboxMetadataRequest() {
    }

    public void setMetadata(MailCustomMetadata metadata) { this.metadata = metadata; }
    public MailCustomMetadata getMetadata() { return metadata; }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper
            .add("metadata", metadata);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
