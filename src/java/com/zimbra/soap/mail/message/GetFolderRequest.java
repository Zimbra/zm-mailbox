/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011 Zimbra, Inc.
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

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.mail.type.GetFolderSpec;

/*
<GetFolderRequest [visible="0|1"] [needGranteeName="0|1"] [view="{folder-view-constraint}"] [depth="{subfolder-levels}"]>
  [<folder [l="{base-folder-id}"] [path="{fully-qualified-path}"]/>]
</GetFolderRequest>

 */

@XmlRootElement(name=MailConstants.E_GET_FOLDER_REQUEST)
@XmlType(propOrder = {})
public class GetFolderRequest {

    @XmlAttribute(name=MailConstants.A_VISIBLE, required=false)
    private Boolean isVisible;

    @XmlAttribute(name=MailConstants.A_NEED_GRANTEE_NAME, required=false)
    private boolean needGranteeName;

    @XmlAttribute(name=MailConstants.A_DEFAULT_VIEW, required=false)
    private String viewConstraint;

    @XmlAttribute(name=MailConstants.A_FOLDER_DEPTH, required=false)
    private Integer treeDepth;

    @XmlElement(name=MailConstants.E_FOLDER, required=false)
    private GetFolderSpec folder;

    public GetFolderRequest() {
    }

    public GetFolderRequest(GetFolderSpec folder) {
        this(folder, null);
    }

    public GetFolderRequest(GetFolderSpec folder, Boolean isVisible) {
        setFolder(folder);
        setVisible(isVisible);
    }

    public Boolean isVisible() {
        return isVisible;
    }

    public boolean isNeedGranteeName() {
        return needGranteeName;
    }

    public String getViewConstraint() {
        return viewConstraint;
    }

    public Integer getTreeDepth() {
        return treeDepth;
    }

    public GetFolderSpec getFolder() {
        return folder;
    }

    public void setVisible(Boolean isVisible) {
        this.isVisible = isVisible;
    }

    public void setNeedGranteeName(boolean needGranteeName) {
        this.needGranteeName = needGranteeName;
    }

    public void setViewConstraint(String viewConstraint) {
        this.viewConstraint = viewConstraint;
    }

    public void setTreeDepth(Integer treeDepth) {
        this.treeDepth = treeDepth;
    }

    public void setFolder(GetFolderSpec folder) {
        this.folder = folder;
    }
}
