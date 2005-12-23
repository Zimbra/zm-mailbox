/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.stats;

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
