/*
 * ***** BEGIN LICENSE BLOCK *****
 *
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2015, 2016 Synacor, Inc.
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
 *
 * ***** END LICENSE BLOCK *****
 */
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
