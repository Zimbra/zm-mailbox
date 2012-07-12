/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012 Zimbra, Inc.
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

package com.zimbra.soap.voice.type;

import com.google.common.base.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class VoiceMsgActionInfo {

    /**
     * @zm-api-field-tag successes
     * @zm-api-field-description List of ids that were acted on
     */
    @XmlAttribute(name=MailConstants.A_ID /* id */, required=true)
    private String successes;

    /**
     * @zm-api-field-tag operation-move|read|empty
     * @zm-api-field-description Operation - <b>move|read|empty</b>
     */
    @XmlAttribute(name=MailConstants.A_OPERATION /* op */, required=true)
    private String operation;

    public VoiceMsgActionInfo() {
    }

    public void setSuccesses(String successes) { this.successes = successes; }
    public void setOperation(String operation) { this.operation = operation; }
    public String getSuccesses() { return successes; }
    public String getOperation() { return operation; }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper
            .add("successes", successes)
            .add("operation", operation);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
