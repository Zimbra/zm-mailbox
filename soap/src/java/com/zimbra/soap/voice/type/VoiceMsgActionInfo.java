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

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper
            .add("successes", successes)
            .add("operation", operation);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
