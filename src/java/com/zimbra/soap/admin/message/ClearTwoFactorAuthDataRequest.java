package com.zimbra.soap.admin.message;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.CosSelector;
import com.zimbra.soap.type.AccountSelector;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_CLEAR_TWO_FACTOR_AUTH_DATA_REQUEST)
public class ClearTwoFactorAuthDataRequest {

    @XmlElement(name=AdminConstants.E_COS, required=false)
    private CosSelector cos;

    @XmlElement(name=AdminConstants.E_ACCOUNT, required=false)
    private AccountSelector account;

    @XmlAttribute(name=AdminConstants.A_LAZY_DELETE, required=false)
    private ZmBoolean lazyDelete;

    public ClearTwoFactorAuthDataRequest() {}

    public void setCos(CosSelector cos) {this.cos = cos; }
    public CosSelector getCos() {return cos; }
    public void setAccount(AccountSelector account) {this.account = account; }
    public AccountSelector getAccount() {return account; }
    public void setLazyDelete(ZmBoolean lazy) {this.lazyDelete = lazy; }
    public ZmBoolean getLazyDelete() {return lazyDelete; }
}
