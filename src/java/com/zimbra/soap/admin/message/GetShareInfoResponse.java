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

import com.google.common.collect.Lists;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.type.ShareInfo;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_GET_SHARE_INFO_RESPONSE)
public class GetShareInfoResponse {

    /**
     * @zm-api-field-description Share information
     */
    @XmlElement(name=AdminConstants.E_SHARE, required=false)
    private List <ShareInfo> shareInfos = Lists.newArrayList();

    public GetShareInfoResponse() {
    }

    public GetShareInfoResponse(List <ShareInfo> shareInfos) {
        setShareInfos(shareInfos);
    }

    public GetShareInfoResponse setShareInfos(
            Collection <ShareInfo> shareInfos) {
        this.shareInfos.clear();
        if (shareInfos != null) {
            this.shareInfos.addAll(shareInfos);
        }
        return this;
    }

    public GetShareInfoResponse addShareInfo(ShareInfo shareInfo) {
        shareInfos.add(shareInfo);
        return this;
    }

    public List<ShareInfo> getShareInfos() {
        return Collections.unmodifiableList(shareInfos);
    }
}
