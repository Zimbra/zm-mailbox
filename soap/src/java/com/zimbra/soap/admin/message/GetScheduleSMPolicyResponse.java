/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2022 Synacor, Inc.
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

package com.zimbra.soap.admin.message;

import com.google.common.base.MoreObjects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.HsmConstants;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=HsmConstants.E_GET_SCHEDULE_SM_POLICY_RESPONSE)
public class GetScheduleSMPolicyResponse {

    /**
     * @zm-api-field-description <b>1 (true)</b> if an HSM session is currently running, <b>0 (false)</b>
     * if the information returned applies to the last completed HSM session
     */
    @XmlAttribute(name=HsmConstants.A_SM_SCHEDULE_POLICY_ENABLED /* running */, required=true)
    private final ZmBoolean smSchedulePolicyEnabled;

    /**
     * @zm-api-field-tag error-text
     * @zm-api-field-description The error message, if an error occurred while processing the last
     * <b>&lt;HsmRequest></b>
     */
    @XmlAttribute(name=HsmConstants.A_ERROR /* error */, required=false)
    private String error;

    /**
     * @zm-api-field-tag total-mailboxes
     * @zm-api-field-description Total number of mailboxes that should be processed by the HSM session
     */
    @XmlAttribute(name=HsmConstants.A_SM_SCHEDULE_POLICY_START_TIME /* scheduletime */, required=true)
    private Integer smSchedulePolicyStartTime;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private GetScheduleSMPolicyResponse() {
        this(false);
    }

    public GetScheduleSMPolicyResponse(boolean smSchedulePolicyEnabled) {
        this.smSchedulePolicyEnabled = ZmBoolean.fromBool(smSchedulePolicyEnabled);
    }

    public void setError(String error) {
        this.error = error;
    }
    
    public void setSmScheduleStartTime(Integer smSchedulePolicyStartTime) {
        this.smSchedulePolicyStartTime = smSchedulePolicyStartTime;
    }

    public boolean getSmScheduleEnabled() {
        return ZmBoolean.toBool(smSchedulePolicyEnabled);
    }

    public String getError() {
        return error;
    }

    public Integer getSmScheduleStartTime() {
        return smSchedulePolicyStartTime;
    }

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper.add("smSchedulePolicyEnabled", smSchedulePolicyEnabled)
                .add("smSchedulePolicyStartTime", smSchedulePolicyStartTime).add("error", error);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
