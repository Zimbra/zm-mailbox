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

package com.zimbra.soap.mail.message;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.mail.type.RetentionPolicy;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=MailConstants.E_GET_SYSTEM_RETENTION_POLICY_RESPONSE)
public class GetSystemRetentionPolicyResponse {

    /**
     * @zm-api-field-description System Retention policy
     */
    @XmlElement(name=MailConstants.E_RETENTION_POLICY)
    private RetentionPolicy retentionPolicy;

    /**
     * No-argument constructor for JAXB.
     */
    public GetSystemRetentionPolicyResponse() {
    }

    public GetSystemRetentionPolicyResponse(RetentionPolicy rp) {
        retentionPolicy = rp;
    }

    public RetentionPolicy getRetentionPolicy() {
        return retentionPolicy;
    }
}
