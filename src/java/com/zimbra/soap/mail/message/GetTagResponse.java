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
import com.zimbra.soap.mail.type.TagInfo;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=MailConstants.E_GET_TAG_RESPONSE)
public class GetTagResponse {

    /**
     * @zm-api-field-description Information about tags
     */
    @XmlElement(name=MailConstants.E_TAG, required=false)
    private List<TagInfo> tags = Lists.newArrayList();

    public GetTagResponse() {
    }

    public void setTags(Iterable <TagInfo> tags) {
        this.tags.clear();
        if (tags != null) {
            Iterables.addAll(this.tags,tags);
        }
    }

    public GetTagResponse addTag(TagInfo tag) {
        this.tags.add(tag);
        return this;
    }

    public List<TagInfo> getTags() {
        return Collections.unmodifiableList(tags);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
            .add("tags", tags)
            .toString();
    }
}
