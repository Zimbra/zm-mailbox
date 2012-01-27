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

package com.zimbra.soap.type;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Multimap;

import com.zimbra.common.service.ServiceException;

/*
 * Used for JAXB objects representing elements which have child node(s) of form:
 *     <a n="{key}">{value}</a>
 */
public interface KeyValuePairs {
    public void setKeyValuePairs(Iterable<KeyValuePair> keyValues);
    public void setKeyValuePairs(
            Map<String, ? extends Object> keyValues)
    throws ServiceException;
    public void addKeyValuePair(KeyValuePair keyValue);
    public List<KeyValuePair> getKeyValuePairs();
    public Multimap<String, String> getKeyValuePairsMultimap();
    public Map<String, Object> getKeyValuePairsAsOldMultimap();
    public String firstValueForKey(String key);
    public List<String> valuesForKey(String key);
}
