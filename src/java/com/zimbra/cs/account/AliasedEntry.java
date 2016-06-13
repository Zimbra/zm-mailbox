/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.account;

import java.util.Set;

import com.zimbra.common.service.ServiceException;

public interface AliasedEntry {

    /*
     * entry with aliases
     */
    
    /**
     * returns whether addr is the entry's primary address or 
     * one of the aliases.
     * 
     * @param addr
     * @return
     */
    public boolean isAddrOfEntry(String addr);
    
    public String[] getAliases() throws ServiceException;
    
    /**
     * returns all addresses of the entry, including primary address and 
     * all aliases.
     * 
     * @return
     */
    public Set<String> getAllAddrsSet();
}
