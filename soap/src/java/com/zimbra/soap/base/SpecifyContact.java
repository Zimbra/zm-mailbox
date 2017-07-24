/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2017 Synacor, Inc.
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

import com.zimbra.soap.mail.type.NewContactAttr;
import com.zimbra.soap.mail.type.NewContactGroupMember;

public interface SpecifyContact <T extends NewContactAttr, M extends NewContactGroupMember> {
    public void setId(Integer id);
    public void setTagNames(String tagNames);
    public void setAttrs(Iterable <T> attrs);
    public void addAttr(T attr);
    public T addAttrWithName(String name);
    public T addAttrWithNameAndValue(String name, String value);
    public void setContactGroupMembers(Iterable <M> contactGroupMembers);
    public void addContactGroupMember(M contactGroupMember);
    public M addContactGroupMemberWithTypeAndValue(String type, String value);
    public Integer getId();
    public String getTagNames();
    public List<T> getAttrs();
    public List<M> getContactGroupMembers();
}
