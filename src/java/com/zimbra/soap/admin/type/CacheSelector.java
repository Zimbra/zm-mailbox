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

package com.zimbra.soap.admin.type;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.NONE)
public class CacheSelector {

    private static Splitter COMMA_SPLITTER = Splitter.on(",");
    private static Joiner COMMA_JOINER = Joiner.on(",");

    private List<CacheEntryType> types = Lists.newArrayList();

    @XmlAttribute(name=AdminConstants.A_ALLSERVERS, required=false)
    private ZmBoolean allServers;

    @XmlElement(name=AdminConstants.E_ENTRY, required=false)
    private List <CacheEntrySelector> entries = Lists.newArrayList();

    public CacheSelector() 
    throws ServiceException {
        this((Boolean)null, (String) null);
    }

    public CacheSelector(Boolean allServers, String types)
    throws ServiceException {
        this.allServers = ZmBoolean.fromBool(allServers);
        setTypes(types);
    }

    public void setAllServers(Boolean allServers) {
        this.allServers = ZmBoolean.fromBool(allServers);
    }

    public void setTypes(String types)
    throws ServiceException {
        for (String typeString : COMMA_SPLITTER.split(types)) {
            addType(typeString);
        }
    }

    public void addType(CacheEntryType type) {
        types.add(type);
    }

    public void addType(String typeString)
    throws ServiceException {
        addType(CacheEntryType.fromString(typeString));
    }

    public void setEntries(Iterable<CacheEntrySelector> entryList) {
        this.entries.clear();
        if (entries != null) {
            Iterables.addAll(this.entries, entryList);
        }
    }

    public void addEntry(CacheEntrySelector entry) {
        this.entries.add(entry);
    }

    public Boolean isAllServers() { return ZmBoolean.toBool(allServers); }
    @XmlAttribute(name=AdminConstants.A_TYPE)
    public String getTypes() { return COMMA_JOINER.join(types); }
    public List<CacheEntrySelector> getEntries() {
        return Collections.unmodifiableList(entries);
    }
}
