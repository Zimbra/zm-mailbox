/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014 Zimbra, Inc.
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
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
