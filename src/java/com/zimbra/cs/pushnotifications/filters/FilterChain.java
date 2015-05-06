/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2015 Zimbra, Inc.
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

package com.zimbra.cs.pushnotifications.filters;

/**
 * Interface to execute a filter chain
 */
public interface FilterChain {

    /**
     * Add a filter into the filter chain
     * @param filter
     */
    public void addFilter(Filter filter);

    /**
     * Execute a filter chain
     * @return TRUE if all filters pass else return FALSE
     */
    public boolean execute();

}
