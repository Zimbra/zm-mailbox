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

import com.zimbra.common.soap.AccountConstants;
import com.zimbra.soap.type.AccountSelector;

/**
 <ChangePasswordRequest>
   <account by="name">...</account>
   <oldPassword>...</oldPassword>
   <password>...</password>
   [<virtualHost>{virtual-host}</virtualHost>]
 </ChangePasswordRequest>
 * @zm-api-command-description Change Password
*/
@XmlRootElement(name=AccountConstants.E_CHANGE_PASSWORD_REQUEST)
@XmlType(propOrder = {})
public class ChangePasswordRequest {
    /**
     * @zm-api-field-description Details of the account
     */
    @XmlElement(name=AccountConstants.E_ACCOUNT, required=true)
    private AccountSelector account;
    /**
     * @zm-api-field-description Old password
     */
    @XmlElement(name=AccountConstants.E_OLD_PASSWORD, required=true)
    private String oldPassword;
    /**
     * @zm-api-field-description New Password to assign
     */
    @XmlElement(name=AccountConstants.E_PASSWORD, required=true)
    private String password;
    /**
     * @zm-api-field-tag virtual-host
     * @zm-api-field-description if specified virtual-host is used to determine the domain of the account name,
     * if it does not include a domain component. For example, if the domain foo.com has a zimbraVirtualHostname of
     * "mail.foo.com", and an auth request comes in for "joe" with a virtualHost of "mail.foo.com", then the request
     * will be equivalent to logging in with "joe@foo.com".
     */
    @XmlElement(name=AccountConstants.E_VIRTUAL_HOST, required=false)
    private String virtualHost;

    public ChangePasswordRequest() {
    }
    
    public ChangePasswordRequest(AccountSelector account, String oldPassword, String newPassword) {
        setAccount(account);
        setOldPassword(oldPassword);
        setPassword(newPassword);
    }
    
    public AccountSelector getAccount() { return account; }
    public String oldPassword() { return oldPassword; }
    public String getPassword() { return password; }
    public String getVirtualHost() { return virtualHost; }
    
    public ChangePasswordRequest setAccount(AccountSelector account) {
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
