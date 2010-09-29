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

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/*
            <link id="1" name="new-mount-point" l="1" n="6" u="1" f="u" owner="user1@example.com" zid="151bd192-e19a-40be-b8c9-259b21ffac48" rid="2" oname="user1folder">

 */
@XmlRootElement(name="link")
@XmlType(propOrder = {})
public class Mountpoint
extends Folder {

    @XmlAttribute(required=true, name="owner") private String ownerEmail;
    @XmlAttribute(required=true, name="zid") private String ownerAccountId;
    @XmlAttribute(required=true, name="rid") private int remoteFolderId;
    @XmlAttribute(required=true, name="oname") private String remoteFolderName;
    
    public Mountpoint() {
    }
    
    public String getOwnerEmail() {
        return ownerEmail;
    }
    
    public String getOwnerAccountId() {
        return ownerAccountId;
    }
    
    public int getRemoteFolderId() {
        return remoteFolderId;
    }
    
    public String getRemoteFolderName() {
        return remoteFolderName;
    }
    
    public void setOwnerEmail(String ownerEmail) {
        this.ownerEmail = ownerEmail;
    }
    
    public void setOwnerAccountId(String accountId) {
        this.ownerAccountId = accountId;
    }
    
    public void setRemoteFolderId(int remoteFolderId) {
        this.remoteFolderId = remoteFolderId;
    }
    
    public void setRemoteFolderName(String remoteFolderName) {
        this.remoteFolderName = remoteFolderName;
    }
}
