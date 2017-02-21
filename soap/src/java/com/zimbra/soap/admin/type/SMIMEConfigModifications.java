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
