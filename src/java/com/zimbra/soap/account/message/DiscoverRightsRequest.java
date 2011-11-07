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
package com.zimbra.soap.account.message;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.zimbra.common.soap.AccountConstants;

@XmlRootElement(name=AccountConstants.E_DISCOVER_RIGHTS_REQUEST)
public class DiscoverRightsRequest {
    @XmlElement(name=AccountConstants.E_RIGHT /* right */, required=true)
    private List<String> rights = Lists.newArrayList();
    
    public DiscoverRightsRequest() {
        this(null);
    }
    
    public DiscoverRightsRequest(Iterable <String> rights) {
        setRights(rights);
    }
    
    public void setRights(Iterable <String> rights) {
        this.rights.clear();
        if (rights != null) {
            Iterables.addAll(this.rights,rights);
        }
    }

    public void addRight(String right) {
        this.rights.add(right);
    }
    
    public List<String> getRights() {
        return rights;
    }
}
