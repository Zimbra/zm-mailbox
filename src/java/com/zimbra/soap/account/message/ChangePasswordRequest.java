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

package com.zimbra.soap.account.message;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.soap.account.type.Account;

/*
 <ChangePasswordRequest>
   <account by="name">...</account>
   <oldPassword>...</oldPassword>
   <password>...</password>
   [<virtualHost>{virtual-host}</virtualHost>]
 </ChangePasswordRequest>
*/
@XmlRootElement(name="ChangePasswordRequest")
@XmlType(propOrder = {})
public class ChangePasswordRequest {
    @XmlElement(required = true) private Account account;
    @XmlElement(required = true) private String oldPassword;
    @XmlElement(required = true) private String password;
    @XmlElement private String virtualHost;
    
    public ChangePasswordRequest() {
    }
    
    public ChangePasswordRequest(Account account, String oldPassword, String newPassword) {
        setAccount(account);
        setOldPassword(oldPassword);
        setPassword(newPassword);
    }
    
    public Account getAccount() { return account; }
    public String oldPassword() { return oldPassword; }
    public String getPassword() { return password; }
    public String getVirtualHost() { return virtualHost; }
    
    public ChangePasswordRequest setAccount(Account account) {
        this.account = account;
        return this;
    }
    
    public ChangePasswordRequest setOldPassword(String oldPassword) {
        this.oldPassword = oldPassword;
        return this;
    }
    
    public ChangePasswordRequest setPassword(String password) {
        this.password = password;
        return this;
    }
    
    public ChangePasswordRequest setVirtualHost(String host) {
        virtualHost = host;
        return this;
    }
}
