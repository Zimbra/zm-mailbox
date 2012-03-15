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

package com.zimbra.soap.admin.type;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.soap.AdminConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class SMIMEConfigModifications extends AdminAttrsImpl {

    /**
     * @zm-api-field-tag config-name
     * @zm-api-field-description Config name
     */
    @XmlAttribute(name=AdminConstants.A_NAME /* name */, required=true)
    private final String name;

    // Appears to only have values AdminConstants.OP_MODIFY and
    // AdminConstants.OP_REMOVE
    /**
     * @zm-api-field-tag operation
     * @zm-api-field-description Operation
     * <table>
     * <tr> <td> <b>modify [default]</b> </td> 
     *      <td> modify the SMIME config: modify/add/remove specified attributes of the config. </td> </tr>
     * <tr> <td> <b>remove</b> </td>
     *      <td> remove the SMIME config: remove all attributes of the config. Must not include an attr map under
     *           the <b>&lt;config></b> element.</td> </tr>
     * </table>
     */
    @XmlAttribute(name=AdminConstants.A_OP /* op */, required=true)
    private final String operation;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private SMIMEConfigModifications() {
        this((String) null, (String) null);
    }

    public SMIMEConfigModifications(String name, String operation) {
        this.name = name;
        this.operation = operation;
    }

    public String getName() { return name; }
    public String getOperation() { return operation; }
}
