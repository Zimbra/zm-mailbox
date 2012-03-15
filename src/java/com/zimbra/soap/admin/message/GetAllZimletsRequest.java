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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;

/**
 * @zm-api-command-description Get all Zimlets
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_GET_ALL_ZIMLETS_REQUEST)
public class GetAllZimletsRequest {

    /**
     * @zm-api-field-tag exclude
     * @zm-api-field-description {exclude} can be "none|extension|mail"
     * <table>
     * <tr> <td> <b>extension</b> </td> <td> return only mail Zimlets </td> </tr>
     * <tr> <td> <b>mail</b> </td> <td> return only admin extensions </td> </tr>
     * <tr> <td> <b>none [default]</b> </td> <td> return both mail and admin zimlets </td> </tr>
     * </table>
     */
    @XmlAttribute(name=AdminConstants.A_EXCLUDE, required=false)
    private final String exclude;

    public GetAllZimletsRequest() {
        this((String) null);
    }

    public GetAllZimletsRequest(String exclude) {
        this.exclude = exclude;
    }

    public String getExclude() { return exclude; }
}
