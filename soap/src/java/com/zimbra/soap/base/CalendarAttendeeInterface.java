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

package com.zimbra.soap.base;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

@XmlAccessorType(XmlAccessType.NONE)
public interface CalendarAttendeeInterface {
    public void setAddress(String address);
    public void setUrl(String url);
    public void setDisplayName(String displayName);
    public void setSentBy(String sentBy);
    public void setDir(String dir);
    public void setLanguage(String language);
    public void setCuType(String cuType);
    public void setRole(String role);
    public void setPartStat(String partStat);
    public void setRsvp(Boolean rsvp);
    public void setMember(String member);
    public void setDelegatedTo(String delegatedTo);
    public void setDelegatedFrom(String delegatedFrom);

    public String getAddress();
    public String getUrl();
    public String getDisplayName();
    public String getSentBy();
    public String getDir();
    public String getLanguage();
    public String getCuType();
    public String getRole();
    public String getPartStat();
    public Boolean getRsvp();
    public String getMember();
    public String getDelegatedTo();
    public String getDelegatedFrom();

    public void setXParamInterfaces(Iterable<XParamInterface> xParams);
    public void addXParamInterface(XParamInterface xParam);
    public List<XParamInterface> getXParamInterfaces();
}
