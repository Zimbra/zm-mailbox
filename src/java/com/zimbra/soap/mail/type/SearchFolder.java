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

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.zimbra.common.service.ServiceException;

/*
  <search id="..." name="..." query="..." [types="..."] [sortBy="..."] l="{folder}"/>+

 */
@XmlRootElement(name="search")
@XmlType(propOrder = {})
public class SearchFolder
extends Folder {

    @XmlEnum
    public enum SortBy {
        @XmlEnumValue("dateDesc") dateDesc,
        @XmlEnumValue("dateAsc") dateAsc,
        @XmlEnumValue("subjDesc") subjDesc,
        @XmlEnumValue("subjAsc") subjAsc,
        @XmlEnumValue("nameDesc") nameDesc,
        @XmlEnumValue("nameAsc") nameAsc,
        @XmlEnumValue("durDesc") durDesc,
        @XmlEnumValue("durAsc") durAsc,
        @XmlEnumValue("none") none,
        @XmlEnumValue("taskDueAsc") taskDueAsc,
        @XmlEnumValue("taskStatusDesc") taskDueDesc,
        @XmlEnumValue("taskStatusAsc") taskStatusAsc,
        @XmlEnumValue("taskStatusDesc") taskStatusDesc,
        @XmlEnumValue("taskPercCompletedAsc") taskPercCompletedAsc,
        @XmlEnumValue("taskPercCompletedDesc") taskPercCompletedDesc;

        public static SortBy fromString(String s) throws ServiceException {
            try {
                return SortBy.valueOf(s);
            } catch (IllegalArgumentException e) {
                throw ServiceException.INVALID_REQUEST("invalid sortBy: "+s+", valid values: " +
                    Arrays.asList(SortBy.values()), e);
            }
        }
    }
    
    private static Splitter COMMA_SPLITTER = Splitter.on(",");
    private static Joiner COMMA_JOINER = Joiner.on(",");
    
    @XmlAttribute private String query;
    private List<ItemType> types = new ArrayList<ItemType>();
    @XmlAttribute private SortBy sortBy;
    
    public String getQuery() {
        return query;
    }
    
    public void setQuery(String query) {
        this.query = query;
    }
    
    public SortBy getSortBy() {
        return sortBy;
    }
    
    public void setSortBy(SortBy sortBy) {
        this.sortBy = sortBy;
    }
    
    @XmlAttribute public String getTypes() {
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
