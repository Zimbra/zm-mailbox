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

import com.google.common.base.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.CertMgrConstants;

@XmlAccessorType(XmlAccessType.NONE)
@XmlType(propOrder = {})
public class CSRSubject {

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
    public String getC() { return c; }
    public String getSt() { return st; }
    public String getL() { return l; }
    public String getO() { return o; }
    public String getOu() { return ou; }
    public String getCn() { return cn; }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
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
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
