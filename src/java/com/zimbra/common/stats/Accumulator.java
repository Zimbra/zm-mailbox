/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.common.stats;

import java.util.List;

/**
 * Defines an interface to an object that keeps track of
 * one or more statistics.
 */
public interface Accumulator {

    /**
     * Returns stat names.  The size of the <code>List</code> must match the size of the
     * <code>List</code> returned by {@link #getData()}.
     */
    public List<String> getNames();
    
    /**
     * Returns stat values.  The size of the <code>List</code> must match the size of the
     * <code>List</code> returned by {@link #getNames()}.
     */
    public List<Object> getData();
    
    /**
     * Resets the values tracked by this <code>Accumulator</code>.
     */
    public void reset();
}
