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

package com.zimbra.soap.mail.type;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.type.SrchSortBy;

/*
  <search id="..." name="..." query="..." [types="..."] [sortBy="..."] l="{folder}"/>+

 */
// Root element name needed to differentiate between types of folder
// MailConstants.E_SEARCH == "search"
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=MailConstants.E_SEARCH)
public class SearchFolder
extends Folder {

    private static Splitter COMMA_SPLITTER = Splitter.on(",");
    private static Joiner COMMA_JOINER = Joiner.on(",");

    @XmlAttribute(name=MailConstants.A_QUERY, required=false)
    private String query;

    @XmlAttribute(name=MailConstants.A_SORTBY, required=false)
    private SrchSortBy sortBy;

    private List<ItemType> types = new ArrayList<ItemType>();

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public SrchSortBy getSortBy() {
        return sortBy;
    }

    public void setSortBy(SrchSortBy sortBy) {
        this.sortBy = sortBy;
    }

    @XmlAttribute(name=MailConstants.A_SEARCH_TYPES, required=false)
    public String getTypes() {
        return COMMA_JOINER.join(types);
    }

    public void setTypes(String types)
    throws ServiceException {
        for (String typeString : COMMA_SPLITTER.split(types)) {
            addType(typeString);
        }
    }

    public void addType(ItemType type) {
        types.add(type);
    }

    public void addType(String typeString)
    throws ServiceException {
        addType(ItemType.fromString(typeString));
    }
}
