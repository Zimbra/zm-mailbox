package com.zimbra.soap.account.message;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.google.common.collect.Iterables;
import com.zimbra.common.soap.AccountConstants;

@XmlRootElement(name=AccountConstants.E_GET_APP_SPECIFIC_PASSWORDS_RESPONSE)
@XmlType(propOrder = {})
public class GetAppSpecificPasswordsResponse {

	public GetAppSpecificPasswordsResponse() {}

	@XmlElementWrapper(name=AccountConstants.E_APP_SPECIFIC_PASSWORDS)
    @XmlElements({
        @XmlElement(name=AccountConstants.E_APP_SPECIFIC_PASSWORD_DATA, type=AppSpecificPasswordData.class)
    })
	private List<AppSpecificPasswordData> appSpecificPasswords = new ArrayList<AppSpecificPasswordData>();

    public void setAppSpecificPasswords(Iterable<AppSpecificPasswordData> appSpecificPasswords) {
        this.appSpecificPasswords.clear();
        if (appSpecificPasswords != null) {
            Iterables.addAll(this.appSpecificPasswords, appSpecificPasswords);
        }
    }

    @XmlElement(name=AccountConstants.E_MAX_APP_PASSWORDS)
    private Integer maxAppPasswords;

    public void addAppSpecificPassword(AppSpecificPasswordData appSpecificPassword) { appSpecificPasswords.add(appSpecificPassword); }

    public List<AppSpecificPasswordData> getAppSpecificPasswords() { return appSpecificPasswords; }

    public void setMaxAppPasswords(int maxAppPasswords) { this.maxAppPasswords = maxAppPasswords; }

    public int getMaxAppPasswords() { return maxAppPasswords; }
}
