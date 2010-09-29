/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010 Zimbra, Inc.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.google.common.collect.Iterables;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.soap.account.type.Identity;

@XmlRootElement(name="GetIdentitiesResponse")
@XmlType(propOrder = {})
public class GetIdentitiesResponse {

    @XmlElement(name=AccountConstants.E_IDENTITY)
    List<Identity> identities = new ArrayList<Identity>();
    
    public List<Identity> getIdentities() {
        return Collections.unmodifiableList(identities);
    }
    
    public void setIdentities(Iterable<Identity> identities) {
        this.identities.clear();
        if (identities != null) {
            Iterables.addAll(this.identities, identities);
        }
    }
}
