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

package com.zimbra.soap.admin.message;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.admin.type.CosSelector;
import com.zimbra.soap.mail.type.Policy;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required true
 * @zm-api-command-description Create a system retention policy.
 * <br />
 * The system retention policy SOAP APIs allow the administrator to edit named system retention policies that users
 * can apply to folders and tags.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_CREATE_SYSTEM_RETENTION_POLICY_REQUEST)
public class CreateSystemRetentionPolicyRequest {
    
    /**
     * @zm-api-field-description COS
     */
    @XmlElement(name=AdminConstants.E_COS)
    private CosSelector cos;

    public void setCos(CosSelector cos) {
        this.cos = cos;
    }

    public CosSelector getCos() { return cos; }
    

    public static class PolicyHolder {
        /**
         * @zm-api-field-description Policy
         */
        @XmlElement(name=AdminConstants.E_POLICY, namespace=MailConstants.NAMESPACE_STR)
        Policy policy;

        public PolicyHolder() {
        }

        public PolicyHolder(Policy p) {
            policy = p;
        }
    }

    /**
     * @zm-api-field-description Keep policy details
     */
    @XmlElement(name=AdminConstants.E_KEEP)
    private PolicyHolder keep;

    /**
     * @zm-api-field-description Purge policy details
     */
    @XmlElement(name=AdminConstants.E_PURGE)
    private PolicyHolder purge;

    public CreateSystemRetentionPolicyRequest() {
    }

    public static CreateSystemRetentionPolicyRequest newKeepRequest(Policy policy) {
        CreateSystemRetentionPolicyRequest req = new CreateSystemRetentionPolicyRequest();
        req.keep = new PolicyHolder(policy);
        return req;
    }

    public static CreateSystemRetentionPolicyRequest newPurgeRequest(Policy policy) {
        CreateSystemRetentionPolicyRequest req = new CreateSystemRetentionPolicyRequest();
        req.purge = new PolicyHolder(policy);
        return req;
    }

    public Policy getKeepPolicy() {
        if (keep != null) {
            return keep.policy;
        }
        return null;
    }

    public Policy getPurgePolicy() {
        if (purge != null) {
            return purge.policy;
        }
        return null;
    }
}
