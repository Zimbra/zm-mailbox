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
