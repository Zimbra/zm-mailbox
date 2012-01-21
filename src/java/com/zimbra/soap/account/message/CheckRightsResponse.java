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
package com.zimbra.soap.account.message;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.collect.Lists;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.account.type.CheckRightsTargetInfo;

@XmlRootElement(name=AccountConstants.E_CHECK_RIGHTS_RESPONSE)
public class CheckRightsResponse {

    /**
     * @zm-api-field-description Rights information for targets
     */
    @XmlElement(name=MailConstants.E_TARGET /* target */, required=true)
    private List<CheckRightsTargetInfo> targets = Lists.newArrayList();

    public CheckRightsResponse() {
        this(null);
    }

    public CheckRightsResponse(List<CheckRightsTargetInfo> targets) {
        if (targets != null) {
            setTargets(targets);
        }
    }

    public void setTargets(List<CheckRightsTargetInfo> targets) {
        this.targets = Lists.newArrayList(targets);
    }

    public void addTarget(CheckRightsTargetInfo target) {
        targets.add(target);
    }

    public List<CheckRightsTargetInfo> getTargets() {
        return targets;
    }
}
