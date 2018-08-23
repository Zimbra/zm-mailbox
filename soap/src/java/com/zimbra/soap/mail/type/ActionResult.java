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

package com.zimbra.soap.mail.type;

import java.util.Collections;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlTransient;

import com.google.common.base.MoreObjects;
import com.google.common.base.Splitter;
import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class ActionResult {

    private final static Splitter COMMA_SPLITTER = Splitter.on(",");

    /**
     * @zm-api-field-tag success-ids
     * @zm-api-field-description Comma-separated list of ids which have been successfully processed
     */
    @XmlAttribute(name=MailConstants.A_ID /* id */, required=true)
    private final String id;

    /**
     * @zm-api-field-tag operation
     * @zm-api-field-description Operation
     */
    @XmlAttribute(name=MailConstants.A_OPERATION /* op */, required=true)
    private final String operation;

    /**
     * @zm-api-field-tag non-existent-ids
     * @zm-api-field-description Comma-separated list of non-existent ids (if requested)
     */
    @XmlAttribute(name=MailConstants.A_NON_EXISTENT_IDS /* nei */, required=false)
    protected String nonExistentIds;

    /**
     * @zm-api-field-tag newly-created-ids
     * @zm-api-field-description Comma-separated list of newly created ids (if requested)
     */
    @XmlAttribute(name=MailConstants.A_NEWLY_CREATED_IDS /* nci */, required=false)
    private String newlyCreatedIds;

    /**
     * no-argument constructor wanted by JAXB
     */
    protected ActionResult() {
        this((String) null, (String) null);
    }

    public ActionResult(String id, String operation) {
        this.id = id;
        this.operation = operation;
    }

    public String getId() { return id; }
    public String getOperation() { return operation; }

    public void setNonExistentIds(String ids) { this.nonExistentIds = ids; };
    public String getNonExistentIds() { return nonExistentIds; };
    public void setNewlyCreatedIds(String newlyCreatedIds) { this.newlyCreatedIds = newlyCreatedIds; }
    @XmlTransient
    public Iterable<String> getNewlyCreatedIds() {
        if (null == newlyCreatedIds) {
            return Collections.emptyList();
        }
        return COMMA_SPLITTER.split(newlyCreatedIds);
    }

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper
            .add("id", id)
            .add("operation", operation)
            .add("nei", nonExistentIds)
            .add("nci", newlyCreatedIds);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }

}
