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
package com.zimbra.cs.im;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.SoapProtocol;

/**
 * 
 */
public class PrivacyList implements Iterable<PrivacyListEntry> {
    
    private String mName;
    private List<PrivacyListEntry> mList = new ArrayList<PrivacyListEntry>();
    private Set<Integer> mOrders = new HashSet<Integer>();

    public static final class DuplicateOrderException extends Exception {
        public String toString() {
            return "The privacy list entry's Order value must be unique to the list. " + super.toString();
        }
    }

    public PrivacyList(String name) {
        mName = name;
    }
    
    public String getName() { return mName; }
    
    public String toString() { 
        try { 
            return toXml(null).toString(); 
        } catch (ServiceException ex) {
            ex.printStackTrace();
            return ex.toString(); 
        } 
    }
    
    public Element toXml(Element parent) throws ServiceException {
        Element list;
        if (parent != null)
            list = parent.addElement("list");
        else 
            list = Element.create(SoapProtocol.Soap12, "list");
        
        list.addAttribute("name", mName);
        for (PrivacyListEntry entry : mList) {
            entry.toXml(list);
        }
        return list;
    }
    

    public void addEntry(PrivacyListEntry entry) throws DuplicateOrderException
    {
        if (mOrders.contains(entry.getOrder()))
            throw new DuplicateOrderException();
        
        mOrders.add(entry.getOrder());
        mList.add(entry);
    }
    
    public Iterator<PrivacyListEntry> iterator() { return mList.iterator(); }
}
