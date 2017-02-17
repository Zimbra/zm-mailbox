/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.dav.property;

import org.dom4j.Element;

import com.zimbra.cs.dav.DavElements;

public class ResourceTypeProperty extends ResourceProperty {

    public ResourceTypeProperty(Element elem) {
        super(elem);
    }

    public boolean isCollection() {
        for (Element child : super.getChildren()) {
            if (child.getName().equals(DavElements.P_COLLECTION)) {
                return true;
            }
        }
        return false;
    }

    public boolean isAddressBook() {
        for (Element child : super.getChildren()) {
            if (child.getName().equals(DavElements.P_ADDRESSBOOK)) {
                return true;
            }
        }
        return false;
    }
    public boolean isCalendar() {
        for (Element child : super.getChildren()) {
            if (child.getName().equals(DavElements.P_CALENDAR)) {
                return true;
            }
        }
        return false;
    }
}
