/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
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
