/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.soap.voice.type;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

import com.zimbra.common.soap.VoiceConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class CallLogItem extends VoiceCallItem {

    /**
     * @zm-api-field-description Parties involved in the call or voice mail.
     * Information for both calling and called parties is returned
     */
    @XmlElement(name=VoiceConstants.E_CALLPARTY /* cp */, required=false)
    private List<CallLogCallParty> callParties = Lists.newArrayList();

    public CallLogItem() {
    }

    public void setCallParties(Iterable <CallLogCallParty> callParties) {
        this.callParties.clear();
        if (callParties != null) {
            Iterables.addAll(this.callParties, callParties);
        }
    }

    public void addCallParty(CallLogCallParty callParty) {
        this.callParties.add(callParty);
    }

    public List<CallLogCallParty> getCallParties() {
        return callParties;
    }

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        helper = super.addToStringInfo(helper);
        return helper
            .add("callParties", callParties);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
