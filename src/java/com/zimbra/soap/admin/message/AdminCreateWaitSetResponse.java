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

package com.zimbra.soap.admin.message;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.type.IdAndType;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_ADMIN_CREATE_WAIT_SET_RESPONSE)
public class AdminCreateWaitSetResponse {

    /**
     * @zm-api-field-tag waitset-id
     * @zm-api-field-description WaitSet ID
     */
    @XmlAttribute(name=MailConstants.A_WAITSET_ID /* waitSet */, required=true)
    private final String waitSetId;

    /**
     * @zm-api-field-tag default-interests
     * @zm-api-field-description Default interest types: comma-separated list.  Currently:
     * <table>
     * <tr> <td> <b>c</b> </td> <td> contacts </td> </tr>
     * <tr> <td> <b>m</b> </td> <td> msgs (and subclasses) </td> </tr>
     * <tr> <td> <b>a</b> </td> <td> appointments </td> </tr>
     * <tr> <td> <b>t</b> </td> <td> tasks </td> </tr>
     * <tr> <td> <b>d</b> </td> <td> documents </td> </tr>
     * <tr> <td> <b>all</b> </td> <td> all types (equiv to "c,m,a,t,d") * </td> </tr>
     * </table>
     */
    @XmlAttribute(name=MailConstants.A_DEFTYPES, required=true)
    private final String defaultInterests;

    /**
     * @zm-api-field-tag sequence
     * @zm-api-field-description Sequence
     */
    @XmlAttribute(name=MailConstants.A_SEQ, required=true)
    private final int sequence;

    /**
     * @zm-api-field-description Error information
     */
    @XmlElement(name=MailConstants.E_ERROR, required=false)
    private List<IdAndType> errors = Lists.newArrayList();

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private AdminCreateWaitSetResponse() {
        this((String) null, (String) null, -1);
    }

    public AdminCreateWaitSetResponse(String waitSetId,
                    String defaultInterests, int sequence) {
        this.waitSetId = waitSetId;
        this.defaultInterests = defaultInterests;
        this.sequence = sequence;
    }

    public void setErrors(Iterable <IdAndType> errors) {
        this.errors.clear();
        if (errors != null) {
            Iterables.addAll(this.errors,errors);
        }
    }

    public AdminCreateWaitSetResponse addError(IdAndType error) {
        this.errors.add(error);
        return this;
    }

    public String getWaitSetId() { return waitSetId; }
    public String getDefaultInterests() { return defaultInterests; }
    public int getSequence() { return sequence; }
    public List<IdAndType> getErrors() {
        return Collections.unmodifiableList(errors);
    }
}
