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

package com.zimbra.soap.mail.type;

public interface CommonInstanceDataAttrsInterface {
    public void setPartStat(String partStat);
    public void setRecurIdZ(String recurIdZ);
    public void setTzOffset(Long tzOffset);
    public void setFreeBusyActual(String freeBusyActual);
    public void setTaskPercentComplete(String taskPercentComplete);
    public void setIsRecurring(Boolean isRecurring);
    public void setPriority(String priority);
    public void setFreeBusyIntended(String freeBusyIntended);
    public void setTransparency(String transparency);
    public void setName(String name);
    public void setLocation(String location);
    public void setHasOtherAttendees(Boolean hasOtherAttendees);
    public void setHasAlarm(Boolean hasAlarm);
    public void setIsOrganizer(Boolean isOrganizer);
    public void setInvId(String invId);
    public void setComponentNum(Integer componentNum);
    public void setStatus(String status);
    public void setCalClass(String calClass);
    public void setAllDay(Boolean allDay);
    public void setDraft(Boolean draft);
    public void setNeverSent(Boolean neverSent);
    public void setTaskDueDate(Long taskDueDate);
    public void setTaskTzOffsetDue(Integer taskTzOffsetDue);
    public void setColor(Byte color);
    public void setRgb(String rgb);

    // see CommonInstanceDataAttrs
    public String getPartStat();
    public String getRecurIdZ();
    public Long getTzOffset();
    public String getFreeBusyActual();
    public String getTaskPercentComplete();
    public Boolean getIsRecurring();
    public String getPriority();
    public String getFreeBusyIntended();
    public String getTransparency();
    public String getName();
    public String getLocation();
    public Boolean getHasOtherAttendees();
    public Boolean getHasAlarm();
    public Boolean getIsOrganizer();
    public String getInvId();
    public Integer getComponentNum();
    public String getStatus();
    public String getCalClass();
    public Boolean getAllDay();
    public Boolean getDraft();
    public Boolean getNeverSent();
    public Long getTaskDueDate();
    public Integer getTaskTzOffsetDue();
    public GeoInfo getGeo();
    public String getFragment();

    // see InstanceDataAttrs /LegacyInstanceDataAttrs
    public void setDuration(Long duration);
    public Long getDuration();
}
