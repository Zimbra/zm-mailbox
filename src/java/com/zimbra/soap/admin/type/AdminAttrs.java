/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010 Zimbra, Inc.
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

package com.zimbra.soap.admin.type;

import java.util.List;
import java.util.Collection;
import java.util.Map;

import com.zimbra.common.service.ServiceException;

public interface AdminAttrs {
    public AdminAttrs setAttrs(Collection<Attr> attrs);
    public AdminAttrsImpl setAttrs(Map<String, ? extends Object> attrs) throws ServiceException;
    public AdminAttrs addAttr(Attr attr);
    public List<Attr> getAttrs();
}
