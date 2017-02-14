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

public interface ZimletDesc {
    public void setName(String name);
    public void setVersion(String version);
    public void setDescription(String description);
    public void setExtension(String extension);
    public void setTarget(String target);
    public void setLabel(String label);
    public void setElements(Iterable <Object> elements);
    public void addElement(Object element);

    public String getName();
    public String getVersion();
    public String getDescription();
    public String getExtension();
    public String getTarget();
    public String getLabel();
    public List<Object> getElements();
}
