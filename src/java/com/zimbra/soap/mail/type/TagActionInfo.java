/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012 Zimbra, Inc.
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

package com.zimbra.soap.mail.type;

import com.google.common.base.Objects;
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

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper
            .add("successes", successes)
            .add("successNames", successNames)
            .add("operation", operation);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
