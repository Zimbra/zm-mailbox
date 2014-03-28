/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014 Zimbra Software, LLC.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.4 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
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
