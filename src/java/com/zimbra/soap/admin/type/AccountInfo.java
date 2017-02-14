/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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

import java.util.Collection;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.soap.AccountConstants;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.NONE)
public class AccountInfo extends AdminObjectInfo {

    /**
     * @zm-api-field-tag is-external
     * @zm-api-field-description Whether the account's <b>zimbraMailTranport</b> points to the designated
     * protocol(lmtp) and server(home server of the account).
     */
    @XmlAttribute(name=AccountConstants.A_IS_EXTERNAL, required=false)
    private final ZmBoolean isExternal;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private AccountInfo() {
        this((String)null, (String)null, (Boolean)null, (Collection <Attr>)null);
    }

    public AccountInfo(String id, String name) {
        this(id, name, (Boolean)null, (Collection <Attr>)null);
    }

    public AccountInfo(String id, String name, Boolean isExternal) {
        this(id, name, isExternal, (Collection <Attr>)null);
    }

    public AccountInfo(String id, String name, Boolean isExternal, Collection <Attr> attrs) {
        super(id, name, attrs);
        this.isExternal = ZmBoolean.fromBool(isExternal);
    }

    public Boolean getIsExternal() { return ZmBoolean.toBool(isExternal); }
}
