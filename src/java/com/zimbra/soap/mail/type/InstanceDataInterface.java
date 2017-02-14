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

import java.util.List;

public interface InstanceDataInterface
extends CommonInstanceDataAttrsInterface {
    public void setStartTime(Long startTime);
    public void setIsException(Boolean isException);
    public void setOrganizer(CalOrganizer organizer);
    public void setCategories(Iterable <String> categories);
    public void addCategory(String category);
    public void setGeo(GeoInfo geo);
    public void setFragment(String fragment);
    public Long getStartTime();
    public Boolean getIsException();
    public CalOrganizer getOrganizer();
    public List<String> getCategories();
    public GeoInfo getGeo();
    public String getFragment();
}
