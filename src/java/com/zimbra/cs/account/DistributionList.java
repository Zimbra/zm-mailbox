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
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.account;

import java.util.List;
import java.util.Map;

import com.zimbra.cs.service.ServiceException;

public interface DistributionList extends NamedEntry {

    public void addMember(String member) throws ServiceException;

    public void removeMember(String member) throws ServiceException;

    public String[] getAllMembers() throws ServiceException;

    public String[] getAliases() throws ServiceException;
    
    /**
     *      
     * @param directOnly return only DLs this DL is a direct member of
     * @param via if non-null and directOnly is false, this map will containing a mapping from a DL name to the DL it was a member of, if 
     *            member was indirect.
     * @return all the DLs
     * @throws ServiceException
     */
    public List<DistributionList> getDistributionLists(boolean directOnly, Map<String,String> via) throws ServiceException; 
}
