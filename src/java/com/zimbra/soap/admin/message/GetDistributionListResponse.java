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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.DistributionListInfo;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name=AdminConstants.E_GET_DISTRIBUTION_LIST_RESPONSE)
public class GetDistributionListResponse {

    /*
     * more/total are only present if the list of members is given
     */
    @XmlAttribute(name=AdminConstants.A_MORE, required=false)
    ZmBoolean more;
    @XmlAttribute(name=AdminConstants.A_TOTAL, required=false)
    Integer total;

    @XmlElement(name=AdminConstants.E_DL, required=false)
    DistributionListInfo dl;

    public GetDistributionListResponse() {
        this((DistributionListInfo)null, (Boolean) null, (Integer) null);
    }

    public GetDistributionListResponse(DistributionListInfo dl,
            Boolean more, Integer total) {
        setDl(dl);
        setMore(more);
        setTotal(total);
    }

    public void setDl(DistributionListInfo dl) { this.dl = dl; }
    public void setMore(Boolean more) { this.more = ZmBoolean.fromBool(more); }
    public void setTotal(Integer total) { this.total = total; }

    public DistributionListInfo getDl() { return dl; }
    public Boolean isMore() { return ZmBoolean.toBool(more); }
    public Integer getTotal() { return total; }
}
