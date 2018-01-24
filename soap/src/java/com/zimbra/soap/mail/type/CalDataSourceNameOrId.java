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

package com.zimbra.soap.mail.type;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

@XmlAccessorType(XmlAccessType.NONE)
public class CalDataSourceNameOrId extends DataSourceNameOrId {
    public static CalDataSourceNameOrId createForName(String name) {
        CalDataSourceNameOrId obj = new CalDataSourceNameOrId();
        obj.setName(name);
        return obj;
    }

    public static CalDataSourceNameOrId createForId(String id) {
        CalDataSourceNameOrId obj = new CalDataSourceNameOrId();
        obj.setId(id);
        return obj;
    }
}
