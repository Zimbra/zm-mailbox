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
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import com.google.common.base.MoreObjects;
import com.zimbra.common.soap.CertMgrConstants;
import com.zimbra.soap.base.CertSubjectAttrs;

@XmlAccessorType(XmlAccessType.NONE)
@XmlType(propOrder = {})
public class CSRSubject implements CertSubjectAttrs {

    /**
     * @zm-api-field-description C
     */
    @XmlElement(name=CertMgrConstants.E_subjectAttr_C /* C */, required=false)
    private String c;

    /**
     * @zm-api-field-description ST
     */
    @XmlElement(name=CertMgrConstants.E_subjectAttr_ST /* ST */, required=false)
    private String st;

    /**
     * @zm-api-field-description L
     */
    @XmlElement(name=CertMgrConstants.E_subjectAttr_L /* L */, required=false)
    private String l;

    /**
     * @zm-api-field-description O
     */
    @XmlElement(name=CertMgrConstants.E_subjectAttr_O /* O */, required=false)
    private String o;

    /**
     * @zm-api-field-description OU
     */
    @XmlElement(name=CertMgrConstants.E_subjectAttr_OU /* OU */, required=false)
    private String ou;

    /**
     * @zm-api-field-description CN
     */
    @XmlElement(name=CertMgrConstants.E_subjectAttr_CN /* CN */, required=false)
    private String cn;

    public CSRSubject() {
    }

    public void setC(String c) { this.c = c; }
    public void setSt(String st) { this.st = st; }
    public void setL(String l) { this.l = l; }
    public void setO(String o) { this.o = o; }
    public void setOu(String ou) { this.ou = ou; }
    public void setCn(String cn) { this.cn = cn; }
    @Override
    public String getC() { return c; }
    @Override
    public String getSt() { return st; }
    @Override
    public String getL() { return l; }
    @Override
    public String getO() { return o; }
    @Override
    public String getOu() { return ou; }
    @Override
    public String getCn() { return cn; }

    public MoreObjects.ToStringHelper addToStringInfo(
                MoreObjects.ToStringHelper helper) {
        return helper
            .add("c", c)
            .add("st", st)
            .add("l", l)
            .add("o", o)
            .add("ou", ou)
            .add("cn", cn);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this))
                .toString();
    }
}
