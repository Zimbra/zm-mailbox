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

package com.zimbra.soap.mail.type;

import java.util.List;

public interface CalendaringDataInterface
extends CommonInstanceDataAttrsInterface {
    public void setDate(Long date);
    public void setOrganizer(CalOrganizer organizer);
    public void setCategories(Iterable <String> categories);
    public void addCategory(String category);
    public void setGeo(GeoInfo geo);
    public void setFragment(String fragment);
    // used in interface instead of methods related to JAXB field
    public void setCalendaringInstances(
            Iterable <InstanceDataInterface> instances);
    // used in interface instead of methods related to JAXB field
    public void addCalendaringInstance(InstanceDataInterface instance);
    public void setAlarmData(AlarmDataInfo alarmData);

    public Long getDate();
    public CalOrganizer getOrganizer();
    public List<String> getCategories();
    public GeoInfo getGeo();
    public String getFragment();
    // used in interface instead of methods related to JAXB field
    public List<InstanceDataInterface> getCalendaringInstances();
    public AlarmDataInfo getAlarmData();

    // see CommonCalendaringData
    public void setFlags(String flags);
    public void setTags(String tags);
    public void setFolderId(String folderId);
    public void setSize(Long size);
    public void setChangeDate(Long changeDate);
    public void setModifiedSequence(Integer modifiedSequence);
    public void setRevision(Integer revision);
    public void setId(String id);

    public String getXUid();
    public String getUid();
    public String getFlags();
    public String getTags();
    public String getFolderId();
    public Long getSize();
    public Long getChangeDate();
    public Integer getModifiedSequence();
    public Integer getRevision();
    public String getId();
}
