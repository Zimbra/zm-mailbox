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
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.soap.type.SearchFilterCondition;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.NONE)
public class EntrySearchFilterSingleCond implements SearchFilterCondition {

    /**
     * @zm-api-field-tag not
     * @zm-api-field-description Negation flag
     * <br />
     * If set to <b>1 (true)</b> then negate the compound condition
     */
    @XmlAttribute(name=AccountConstants.A_ENTRY_SEARCH_FILTER_NEGATION /* not */, required=false)
    private ZmBoolean not;

    /**
     * @zm-api-field-tag single-cond-attr
     * @zm-api-field-description Attribute name
     */
    @XmlAttribute(name=AccountConstants.A_ENTRY_SEARCH_FILTER_ATTR /* attr */, required=true)
    private String attr;

    /**
     * @zm-api-field-tag single-cond-op
     * @zm-api-field-description Operator.  Valid operators are:
     * <table>
     * <tr> <td> <b>eq</b>         </td> <td> attr equals value (integer or string) </td> </tr>
     * <tr> <td> <b>has</b>        </td> <td> attr has value (substring search) </td> </tr>
     * <tr> <td> <b>ge</b>         </td> <td> attr greater than or equal to integer value </td> </tr>
     * <tr> <td> <b>le</b>         </td> <td> attr less than or equal to integer value </td> </tr>
     * <tr> <td> <b>gt</b>         </td> <td> attr greater than (but not equal to) equal to integer value </td> </tr>
     * <tr> <td> <b>lt</b>         </td> <td> attr less than (but not equal to) to integer value </td> </tr>
     * <tr> <td> <b>startswith</b> </td> <td> attr starts with value (string) </td> </tr>
     * <tr> <td> <b>endswith</b>   </td> <td> attr ends with value (string) </td> </tr>
     * </table>
     */
    @XmlAttribute(name=AccountConstants.A_ENTRY_SEARCH_FILTER_OP /* op */, required=true)
    private String op;

    /**
     * @zm-api-field-value single-cond-value
     * @zm-api-field-description value
     */
    @XmlAttribute(name=AccountConstants.A_ENTRY_SEARCH_FILTER_VALUE /* value */, required=true)
    private String value;

    public EntrySearchFilterSingleCond() {
        this((String) null, (String) null, (String) null, (Boolean) null);
    }

    public EntrySearchFilterSingleCond(String attr, String op, String value, Boolean not) {
        setAttr(attr);
        setOp(op);
        setValue(value);
        setNot(not);
    }

    public void setAttr(String attr) { this.attr = attr; }
    public void setOp(String op) { this.op = op; }
    public void setValue(String value) { this.value = value; }
    @Override
    public void setNot(Boolean not) { this.not = ZmBoolean.fromBool(not); }

    public String getAttr() { return attr; }
    public String getOp() { return op; }
    public String getValue() { return value; }
    @Override
    public Boolean isNot() { return ZmBoolean.toBool(not); }
}
