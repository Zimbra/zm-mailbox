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

package com.zimbra.soap.base;

import java.util.List;

public interface AutoCompleteGalInterface {

    public void setMore(Boolean more);
    public void setTokenizeKey(Boolean tokenizeKey);
    public void setPagingSupported(Boolean pagingSupported);
    public void setContactInterfaces(Iterable <ContactInterface> contacts);
    public void addContactInterface(ContactInterface contact);
    public Boolean getMore();
    public Boolean getTokenizeKey();
    public Boolean getPagingSupported();
    public List<ContactInterface> getContactInterfaces();
}
