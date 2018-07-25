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

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.base.CreateWaitSetResp;
import com.zimbra.soap.type.IdAndType;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_ADMIN_CREATE_WAIT_SET_RESPONSE)
public class AdminCreateWaitSetResponse implements CreateWaitSetResp {

    /**
     * @zm-api-field-tag waitset-id
     * @zm-api-field-description WaitSet ID
     */
    @XmlAttribute(name=MailConstants.A_WAITSET_ID /* waitSet */, required=true)
    private String waitSetId;

    /**
     * @zm-api-field-tag default-interests
     * @zm-api-field-description Default interest types: comma-separated list.  Currently:
     * <table>
     * <tr> <td> <b>f</b> </td> <td> folders </td> </tr>
     * <tr> <td> <b>m</b> </td> <td> messages </td> </tr>
     * <tr> <td> <b>c</b> </td> <td> contacts </td> </tr>
     * <tr> <td> <b>a</b> </td> <td> appointments </td> </tr>
     * <tr> <td> <b>t</b> </td> <td> tasks </td> </tr>
     * <tr> <td> <b>d</b> </td> <td> documents </td> </tr>
     * <tr> <td> <b>all</b> </td> <td> all types (equiv to "f,m,c,a,t,d") </td> </tr>
     * </table>
     * <p>This will be used if <b>types</b> isn't specified for an account</p>
     */
    @XmlAttribute(name=MailConstants.A_DEFTYPES /* defTypes */, required=true)
    private String defaultInterests;

    /**
     * @zm-api-field-tag sequence
     * @zm-api-field-description Sequence
     */
    @XmlAttribute(name=MailConstants.A_SEQ /* seq */, required=true)
    private int sequence;

    /**
     * @zm-api-field-description Error information
     */
    @XmlElement(name=MailConstants.E_ERROR /* error */, required=false)
    private final List<IdAndType> errors = Lists.newArrayList();

    /**
     * no-argument constructor wanted by JAXB
     */
    public AdminCreateWaitSetResponse() {
        this((String) null, (String) null, -1);
    }

    public AdminCreateWaitSetResponse(String waitSetId, String defaultInterests, int sequence) {
        this.waitSetId = waitSetId;
        this.defaultInterests = defaultInterests;
        this.sequence = sequence;
    }

    @Override
    public void setErrors(Iterable <IdAndType> errors) {
        this.errors.clear();
        if (errors != null) {
            Iterables.addAll(this.errors,errors);
        }
    }

    @Override
    public AdminCreateWaitSetResponse addError(IdAndType error) {
        this.errors.add(error);
        return this;
    }

    @Override
    public String getWaitSetId() { return waitSetId; }
    @Override
    public String getDefaultInterests() { return defaultInterests; }
    @Override
    public int getSequence() { return sequence; }
    @Override
    public List<IdAndType> getErrors() {
        if (errors.isEmpty()) {
            return null;
        }
        return Collections.unmodifiableList(errors);
    }

    public MoreObjects.ToStringHelper addToStringInfo(
                MoreObjects.ToStringHelper helper) {
        return helper
            .add("waitSetId", waitSetId)
            .add("defaultInterests", defaultInterests)
            .add("sequence", sequence)
            .add("errors", errors);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this))
                .toString();
    }

    @Override
    public AdminCreateWaitSetResponse setWaitSetId(String wsid) {
        waitSetId = wsid;
        return this;
    }

    @Override
    public AdminCreateWaitSetResponse setDefaultInterests(String defInterests) {
        defaultInterests = defInterests;
        return this;
    }

    @Override
    public AdminCreateWaitSetResponse setSequence(int seq) {
        sequence = seq;
        return this;
    }
}
