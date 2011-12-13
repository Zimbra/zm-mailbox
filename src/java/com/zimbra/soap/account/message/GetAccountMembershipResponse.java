/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010 Zimbra, Inc.
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

package com.zimbra.soap.account.message;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AccountConstants;
import com.zimbra.soap.account.type.DLInfo;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name=AccountConstants.E_GET_ACCOUNT_MEMBERSHIP_RESPONSE)
public class GetAccountMembershipResponse {

    @XmlElement(name=AccountConstants.E_DL)
    private List<DLInfo> dlList = new ArrayList<DLInfo>();

    public GetAccountMembershipResponse() {
    }

    public GetAccountMembershipResponse setDlList(Collection<DLInfo> dls) {
        this.dlList.clear();
        if (dls != null) {
            this.dlList.addAll(dls);
        }
        return this;
    }

    public GetAccountMembershipResponse addDl(DLInfo dl) {
        dlList.add(dl);
        return this;
    }

    public List<DLInfo> getDlList() {
        return Collections.unmodifiableList(dlList);
    }
}
