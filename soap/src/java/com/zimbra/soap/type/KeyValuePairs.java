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
