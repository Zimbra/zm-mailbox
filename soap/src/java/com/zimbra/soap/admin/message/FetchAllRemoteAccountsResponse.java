package com.zimbra.soap.admin.message;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.AccountInfo;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = AdminConstants.E_FETCH_ALL_REMOTE_ACCOUNTS_RESPONSE)
public class FetchAllRemoteAccountsResponse {

    /**
     * @zm-api-field-description Information on accounts
     */
    @XmlElement(name=AdminConstants.E_ACCOUNT, required=false)
    private List <AccountInfo> accountList = Lists.newArrayList();

    public FetchAllRemoteAccountsResponse() {
    }

    public void setAccountList(Iterable <AccountInfo> accounts) {
        this.accountList.clear();
        if (accounts != null) {
            Iterables.addAll(this.accountList, accounts);
        }
    }

    public void addAccount(AccountInfo account ) {
        this.accountList.add(account);
    }

    public List <AccountInfo> getAccountList() {
        return Collections.unmodifiableList(accountList);
    }
}
