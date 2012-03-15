/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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

package com.zimbra.soap.mail.message;

import com.google.common.base.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=MailConstants.E_IMPORT_APPOINTMENTS_RESPONSE)
public class ImportAppointmentsResponse {

    /**
     * @zm-api-field-tag list-of-created-ids
     * @zm-api-field-description List of created IDs
     */
    @XmlAttribute(name=MailConstants.A_IDS /* ids */, required=true)
    private final String ids;

    /**
     * @zm-api-field-tag num-imported
     * @zm-api-field-description Number of imported appointments
     */
    @XmlAttribute(name=MailConstants.A_NUM /* n */, required=true)
    private final int num;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private ImportAppointmentsResponse() {
        this((String) null, -1);
    }

    public ImportAppointmentsResponse(String ids, int num) {
        this.ids = ids;
        this.num = num;
    }

    public String getIds() { return ids; }
    public int getNum() { return num; }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper
            .add("ids", ids)
            .add("num", num);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
