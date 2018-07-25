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

package com.zimbra.soap.mail.message;

import com.google.common.base.MoreObjects;
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

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper
            .add("error", error)
            .add("crumb", crumb)
            .add("y", y)
            .add("t", t);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
