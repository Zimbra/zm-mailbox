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

package com.zimbra.soap.admin.type;

import com.google.common.base.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.soap.BackupConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class MailboxMoveSpec {

    /**
     * @zm-api-field-tag account-email-address
     * @zm-api-field-description Account email address
     */
    @XmlAttribute(name=BackupConstants.A_NAME /* name */, required=true)
    private String name;

    /**
     * @zm-api-field-tag dest-hostname
     * @zm-api-field-description Hostname of destination server
     */
    @XmlAttribute(name=BackupConstants.A_TARGET /* dest */, required=true)
    private String target;

    private MailboxMoveSpec() {
    }

    private MailboxMoveSpec(String name, String target) {
        setName(name);
        setTarget(target);
    }

    public static MailboxMoveSpec createForNameAndTarget(String name, String target) {
        return new MailboxMoveSpec(name, target);
    }

    public void setName(String name) { this.name = name; }
    public void setTarget(String target) { this.target = target; }
    public String getName() { return name; }
    public String getTarget() { return target; }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("name", name)
            .add("target", target);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
