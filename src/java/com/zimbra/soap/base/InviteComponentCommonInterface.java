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

package com.zimbra.soap.base;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

@XmlAccessorType(XmlAccessType.NONE)
public interface InviteComponentCommonInterface {
    public InviteComponentCommonInterface create(String method,
            int componentNum, boolean rsvp);
    public void setPriority(String priority);
    public void setName(String name);
    public void setLocation(String location);
    public void setPercentComplete(String percentComplete);
    public void setCompleted(String completed);
    public void setNoBlob(Boolean noBlob);
    public void setFreeBusyActual(String freeBusyActual);
    public void setFreeBusy(String freeBusy);
    public void setTransparency(String transparency);
    public void setIsOrganizer(Boolean isOrganizer);
    public void setXUid(String xUid);
    public void setUid(String uid);
    public void setSequence(Integer sequence);
    public void setDateTime(Long dateTime);
    public void setCalItemId(String calItemId);
    public void setDeprecatedApptId(String deprecatedApptId);
    public void setCalItemFolder(String calItemFolder);
    public void setStatus(String status);
    public void setCalClass(String calClass);
    public void setUrl(String url);
    public void setIsException(Boolean isException);
    public void setRecurIdZ(String recurIdZ);
    public void setIsAllDay(Boolean isAllDay);
    public void setIsDraft(Boolean isDraft);
    public void setNeverSent(Boolean neverSent);
    public void setChanges(String changes);
    public String getMethod();
    public int getComponentNum();
    public boolean getRsvp();
    public String getPriority();
    public String getName();
    public String getLocation();
    public String getPercentComplete();
    public String getCompleted();
    public Boolean getNoBlob();
    public String getFreeBusyActual();
    public String getFreeBusy();
    public String getTransparency();
    public Boolean getIsOrganizer();
    public String getXUid();
    public String getUid();
    public Integer getSequence();
    public Long getDateTime();
    public String getCalItemId();
    public String getDeprecatedApptId();
    public String getCalItemFolder();
    public String getStatus();
    public String getCalClass();
    public String getUrl();
    public Boolean getIsException();
    public String getRecurIdZ();
    public Boolean getIsAllDay();
    public Boolean getIsDraft();
    public Boolean getNeverSent();
    public String getChanges();
}
