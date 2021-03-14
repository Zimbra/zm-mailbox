/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.account.gal;

public class GalNamedFilter {
    /*
     * GAL autocomplete
     */
    private static final String ZIMBRA_ACCOUNT_AUTO_COMPLETE = "zimbraAccountAutoComplete";
    private static final String ZIMBRA_CALENDAR_RESOURCE_AUTO_COMPLETE = "zimbraResourceAutoComplete"; 
    private static final String ZIMBRA_GROUP_AUTO_COMPLETE = "zimbraGroupAutoComplete"; 
    
    /*
     * GAL search
     */
    private static final String ZIMBRA_ACCOUNTS = "zimbraAccounts";
    private static final String ZIMBRA_CALENDAR_RESOURCES = "zimbraResources";
    private static final String ZIMBRA_GROUPS = "zimbraGroups";
    
    /*
     * GAL sync
     */
    private static final String ZIMBRA_ACCOUNT_SYNC = "zimbraAccountSync";
    private static final String ZIMBRA_CALENDAR_RESOURCE_SYNC = "zimbraResourceSync"; 
    private static final String ZIMBRA_GROUP_SYNC = "zimbraGroupSync"; 
    
    public static String getZimbraCalendarResourceFilter(GalOp galOp) {
        String filter = null;
        
        if (galOp == GalOp.autocomplete)
            filter = GalNamedFilter.ZIMBRA_CALENDAR_RESOURCE_AUTO_COMPLETE;
        else if (galOp == GalOp.search)
            filter = GalNamedFilter.ZIMBRA_CALENDAR_RESOURCES;
        else if (galOp == GalOp.sync)
            filter = GalNamedFilter.ZIMBRA_CALENDAR_RESOURCE_SYNC;
        
        return filter;
    }
    
    public static String getZimbraGroupFilter(GalOp galOp) {
        String filter = null;
        
        if (galOp == GalOp.autocomplete)
            filter = GalNamedFilter.ZIMBRA_GROUP_AUTO_COMPLETE;
        else if (galOp == GalOp.search)
            filter = GalNamedFilter.ZIMBRA_GROUPS;
        else if (galOp == GalOp.sync)
            filter = GalNamedFilter.ZIMBRA_GROUP_SYNC;
        
        return filter;
    }
    
    public static String getZimbraAccountFilter(GalOp galOp) {
        String filter = null;
        
        if (galOp == GalOp.autocomplete)
            filter = GalNamedFilter.ZIMBRA_ACCOUNT_AUTO_COMPLETE;
        else if (galOp == GalOp.search)
            filter = GalNamedFilter.ZIMBRA_ACCOUNTS;
        else if (galOp == GalOp.sync)
            filter = GalNamedFilter.ZIMBRA_ACCOUNT_SYNC;
        
        return filter;
    }
}
