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
import com.zimbra.soap.mail.type.TagSpec;

/**
 * @zm-api-command-description Create a tag
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=MailConstants.E_CREATE_TAG_REQUEST)
public class CreateTagRequest {

    /**
     * @zm-api-field-description Tag specification
     */
    @XmlElement(name=MailConstants.E_TAG, required=false)
    private TagSpec tag;

    public CreateTagRequest() {
    }

    public void setTag(TagSpec tag) { this.tag = tag; }
    public TagSpec getTag() { return tag; }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
            .add("tag", tag)
            .toString();
    }
}
