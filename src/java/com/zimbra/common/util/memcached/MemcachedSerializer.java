/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010 Zimbra, Inc.
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

package com.zimbra.common.util.memcached;

import com.zimbra.common.service.ServiceException;

/**
 * Serializes an object of type V to String, and deserializes a String to a V object.
 *
 * @param <V>
 */
public interface MemcachedSerializer<V> {

    public Object serialize(V value) throws ServiceException;
    public V deserialize(Object obj) throws ServiceException;
}
