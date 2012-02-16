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
@XmlRootElement(name=MailConstants.E_GET_YAHOO_COOKIE_RESPONSE)
public class GetYahooCookieResponse {

    /**
     * @zm-api-field-tag error
     * @zm-api-field-description Error
     */
    @XmlAttribute(name="error", required=false)
    private String error;

    /**
     * @zm-api-field-tag crumb
     * @zm-api-field-description Crumb
     */
    @XmlAttribute(name="crumb", required=false)
    private String crumb;

    /**
     * @zm-api-field-tag y
     * @zm-api-field-description y
     */
    @XmlAttribute(name="y", required=false)
    private String y;

    /**
     * @zm-api-field-tag t
     * @zm-api-field-description t
     */
    @XmlAttribute(name="t", required=false)
    private String t;

    public GetYahooCookieResponse() {
    }

    public void setError(String error) { this.error = error; }
    public void setCrumb(String crumb) { this.crumb = crumb; }
    public void setY(String y) { this.y = y; }
    public void setT(String t) { this.t = t; }
    public String getError() { return error; }
    public String getCrumb() { return crumb; }
    public String getY() { return y; }
    public String getT() { return t; }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper
            .add("error", error)
            .add("crumb", crumb)
            .add("y", y)
            .add("t", t);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
