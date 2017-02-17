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
public interface PartInfoInterface {
    public PartInfoInterface createFromPartAndContentType(String part,
            String contentType);
    public void setSize(Integer size);
    public void setContentDisposition(String contentDisposition);
    public void setContentFilename(String contentFilename);
    public void setContentId(String contentId);
    public void setLocation(String location);
    public void setBody(Boolean body);
    public void setTruncatedContent(Boolean truncatedContent);
    public void setContent(String content);
    public String getPart();
    public String getContentType();
    public Integer getSize();
    public String getContentDisposition();
    public String getContentFilename();
    public String getContentId();
    public String getLocation();
    public Boolean getBody();
    public Boolean getTruncatedContent();
    public String getContent();
    public void setMimePartInterfaces(Iterable<PartInfoInterface> mimeParts);
    public void addMimePartInterface(PartInfoInterface mimePart);
    public List<PartInfoInterface> getMimePartInterfaces();
}
