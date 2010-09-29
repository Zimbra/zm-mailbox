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

package com.zimbra.soap.mail.message;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.soap.mail.type.Folder;

/*
<GetFolderRequest [visible="0|1"] [needGranteeName="0|1"]>
  [<folder [l="{base-folder-id}"] [path="{fully-qualified-path}"]/>]
</GetFolderRequest>

 */
@XmlRootElement(name="GetFolderRequest")
@XmlType(propOrder = {})
public class GetFolderRequest {

    @XmlAttribute(name="visible") private Boolean isVisible;
    @XmlAttribute private boolean needGranteeName;
    @XmlElement private Folder folder;
    
    public GetFolderRequest() {
    }
    
    public GetFolderRequest(Folder folder) {
        this(folder, null);
    }
    
    public GetFolderRequest(Folder folder, Boolean isVisible) {
        setFolder(folder);
        setVisible(isVisible);
    }
    
    public Boolean isVisible() {
        return isVisible;
    }
    
    public boolean isNeedGranteeName() {
        return needGranteeName;
    }
    
    public Folder getFolder() {
        return folder;
    }
    
    public void setVisible(Boolean isVisible) {
        this.isVisible = isVisible;
    }
    
    public void setNeedGranteeName(boolean needGranteeName) {
        this.needGranteeName = needGranteeName;
    }
    
    public void setFolder(Folder folder) {
        this.folder = folder;
    }
}
