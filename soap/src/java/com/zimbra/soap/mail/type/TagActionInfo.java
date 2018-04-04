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

package com.zimbra.soap.mail.type;

import com.google.common.base.MoreObjects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class TagActionInfo {

    /**
     * @zm-api-field-tag tag-ids
     * @zm-api-field-description Tag IDs for successfully applied operation
     */
    @XmlAttribute(name=MailConstants.A_ID /* id */, required=true)
    private String successes;

    /**
     * @zm-api-field-tag tag-names
     * @zm-api-field-description Names of tags affected by successfully applied operation
     * <br />
     * Only present if <b>"tn"</b> was specified in the request
     */
    @XmlAttribute(name=MailConstants.A_TAG_NAMES /* tn */, required=false)
    private String successNames;

    /**
     * @zm-api-field-tag operation
     * @zm-api-field-description Operation - "read|!read|color|delete|rename|update|retentionpolicy"
     */
    @XmlAttribute(name=MailConstants.A_OPERATION /* op */, required=true)
    private String operation;

    public TagActionInfo() {
    }

    public void setSuccesses(String successes) { this.successes = successes; }
    public void setSuccessNames(String successNames) { this.successNames = successNames; }
    public void setOperation(String operation) { this.operation = operation; }
    public String getSuccesses() { return successes; }
    public String getSuccessNames() { return successNames; }
    public String getOperation() { return operation; }

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper
            .add("successes", successes)
            .add("successNames", successNames)
            .add("operation", operation);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
