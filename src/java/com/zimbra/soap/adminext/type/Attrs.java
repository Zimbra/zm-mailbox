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

package com.zimbra.soap.adminext.type;

import java.util.List;
import java.util.Collection;
import java.util.Map;

import com.google.common.collect.Multimap;

import com.zimbra.common.service.ServiceException;

public interface Attrs {
    public Attrs setAttrs(Iterable<Attr> attrs);
    public Attrs setAttrs(Map<String, ? extends Object> attrs)
        throws ServiceException;
    public Attrs addAttr(Attr attr);
    public List<Attr> getAttrs();
    public Multimap<String, String> getAttrsMultimap();
    public Map<String, Object> getAttrsAsOldMultimap();
}
