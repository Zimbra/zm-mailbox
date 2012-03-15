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

@XmlAccessorType(XmlAccessType.NONE)
public class AddedComment {

    /**
     * @zm-api-field-tag item-id-of-parent
     * @zm-api-field-description Item ID of parent
     */
    @XmlAttribute(name=MailConstants.A_PARENT_ID /* parentId */, required=true)
    private final String parentId;

    /**
     * @zm-api-field-tag comment-text
     * @zm-api-field-description Comment text
     */
    @XmlAttribute(name=MailConstants.A_TEXT /* text */, required=true)
    private final String text;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private AddedComment() {
        this((String) null, (String) null);
    }

    public AddedComment(String parentId, String text) {
        this.parentId = parentId;
        this.text = text;
    }

    public String getParentId() { return parentId; }
    public String getText() { return text; }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper
            .add("parentId", parentId)
            .add("text", text);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
