/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2022 Synacor, Inc.
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

package com.zimbra.soap.admin.type;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.soap.AdminConstants;

import org.json.JSONException;
import org.json.JSONObject;

@XmlAccessorType(XmlAccessType.NONE)

public class VolumeExternalOpenIOInfo extends BaseExternalVolume {

    /**
     * @zm-api-field-description Specifies the standard HTTP URL for OpenIO
     */
    @XmlAttribute(name=AdminConstants.A_VOLUME_URL /* url */, required=false)
    private String url;

    /**
     * @zm-api-field-description Specifies OpenIO account name
     */
    @XmlAttribute(name=AdminConstants.A_VOLUME_ACCOUNT/* account */, required=false)
    private String account;

    /**
     * @zm-api-field-description Specifies OpenIO namespace
     */
    @XmlAttribute(name=AdminConstants.A_VOLUME_NAMESPACE /* namespace */, required=false)
    private String nameSpace;

    /**
     * @zm-api-field-description Specifies OpenIO proxy port
     */
    @XmlAttribute(name=AdminConstants.A_VOLUME_PROXY_PORT /* proxyPort */, required=false)
    private Integer proxyPort = -1;

    /**
     * @zm-api-field-description Specifies OpenIO account port
     */
    @XmlAttribute(name=AdminConstants.A_VOLUME_ACCOUNT_PORT /* accountPort */, required=false)
    private Integer accountPort = -1;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getNameSpace() {
        return nameSpace;
    }

    public void setNameSpace(String nameSpace) {
        this.nameSpace = nameSpace;
    }

    public Integer getProxyPort() {
        return proxyPort;
    }

    public void setProxyPort(Integer proxyPort) {
        this.proxyPort = proxyPort;
    }

    public Integer getAccountPort() {
        return accountPort;
    }

    public void setAccountPort(Integer accountPort) {
        this.accountPort = accountPort;
    }

	@Override
    public JSONObject toJSON(VolumeInfo volInfo) throws JSONException {
        VolumeExternalOpenIOInfo volExtOpenIOInfo = volInfo.getVolumeExternalOpenIOInfo();
        JSONObject volExtInfoObj = new JSONObject();
        volExtInfoObj.put(AdminConstants.A_VOLUME_ID, String.valueOf(volInfo.getId()));
        volExtInfoObj.put(AdminConstants.A_VOLUME_STORAGE_TYPE, volExtOpenIOInfo.getStorageType());
        volExtInfoObj.put(AdminConstants.A_VOLUME_URL, String.valueOf(volExtOpenIOInfo.getUrl()));
        volExtInfoObj.put(AdminConstants.A_VOLUME_ACCOUNT, String.valueOf(volExtOpenIOInfo.getAccount()));
        volExtInfoObj.put(AdminConstants.A_VOLUME_NAMESPACE, String.valueOf(volExtOpenIOInfo.getNameSpace()));
        volExtInfoObj.put(AdminConstants.A_VOLUME_PROXY_PORT, String.valueOf(volExtOpenIOInfo.getProxyPort()));
        volExtInfoObj.put(AdminConstants.A_VOLUME_ACCOUNT_PORT, String.valueOf(volExtOpenIOInfo.getAccountPort()));
        return volExtInfoObj;
    }

    public VolumeExternalOpenIOInfo toExternalOpenIoInfo(JSONObject properties) throws JSONException {
        VolumeExternalOpenIOInfo volExtOpenIOInfo = new VolumeExternalOpenIOInfo();
        volExtOpenIOInfo.setUrl(properties.getString(AdminConstants.A_VOLUME_URL));
        volExtOpenIOInfo.setAccount(properties.getString(AdminConstants.A_VOLUME_ACCOUNT));
        volExtOpenIOInfo.setNameSpace(properties.getString(AdminConstants.A_VOLUME_NAMESPACE));
        volExtOpenIOInfo.setAccountPort(Integer.parseInt(properties.getString(AdminConstants.A_VOLUME_ACCOUNT_PORT)));
        volExtOpenIOInfo.setProxyPort(Integer.parseInt(properties.getString(AdminConstants.A_VOLUME_PROXY_PORT)));
        volExtOpenIOInfo.setStorageType(properties.getString(AdminConstants.A_VOLUME_STORAGE_TYPE));
        return volExtOpenIOInfo;
    }
}
