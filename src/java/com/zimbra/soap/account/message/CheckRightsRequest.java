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
import com.zimbra.soap.account.type.CheckRightsTargetSpec;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required false
 * @zm-api-command-description Check if the authed user has the specified right(s) on a target.
 */
@XmlRootElement(name=AccountConstants.E_CHECK_RIGHTS_REQUEST)
public class CheckRightsRequest {
    /**
     * @zm-api-field-description The targets
     */
    @XmlElement(name=MailConstants.E_TARGET /* target */, required=true)
    private List<CheckRightsTargetSpec> targets = Lists.newArrayList();

    public CheckRightsRequest() {
        this(null);
    }

    public CheckRightsRequest(Iterable<CheckRightsTargetSpec> targets) {
        if (targets != null) {
            setTargets(targets);
        }
    }

    public void setTargets(Iterable<CheckRightsTargetSpec> targets) {

        this.targets = Lists.newArrayList(targets);
    }

    public void addTarget(CheckRightsTargetSpec target) {
        targets.add(target);
    }

    public List<CheckRightsTargetSpec> getTargets() {
        return targets;
    }
}
