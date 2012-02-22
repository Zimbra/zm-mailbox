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
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.NONE)
public class PurgeRevisionSpec {

    /**
     * @zm-api-field-tag item-id
     * @zm-api-field-description Item ID
     */
    @XmlAttribute(name=MailConstants.A_ID /* id */, required=true)
    private final String id;

    /**
     * @zm-api-field-tag revision
     * @zm-api-field-description Revision
     */
    @XmlAttribute(name=MailConstants.A_VERSION /* ver */, required=true)
    private final int version;

    /**
     * @zm-api-field-tag include-older-revs
     * @zm-api-field-description When set, the server will purge all the old revisions inclusive of the revision
     * specified in the request.
     */
    @XmlAttribute(name=MailConstants.A_INCLUDE_OLDER_REVISIONS /* includeOlderRevisions */, required=false)
    private ZmBoolean includeOlderRevisions;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private PurgeRevisionSpec() {
        this((String) null, -1);
    }

    public PurgeRevisionSpec(String id, int version) {
        this.id = id;
        this.version = version;
    }

    public void setIncludeOlderRevisions(Boolean includeOlderRevisions) {
        this.includeOlderRevisions = ZmBoolean.fromBool(includeOlderRevisions);
    }
    public String getId() { return id; }
    public int getVersion() { return version; }
    public Boolean getIncludeOlderRevisions() { return ZmBoolean.toBool(includeOlderRevisions); }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper
            .add("id", id)
            .add("version", version)
            .add("includeOlderRevisions", includeOlderRevisions);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
