/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.soap.admin.message;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.SyncAdminConstants;
import com.zimbra.common.soap.SyncConstants;
import com.zimbra.soap.admin.type.DeviceId;
import com.zimbra.soap.type.AccountSelector;
import com.zimbra.common.soap.Element;
import com.zimbra.soap.admin.type.DomainSelector;
import com.zimbra.soap.admin.type.CosSelector;

/**
 * @zm-api-command-network-edition
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required true
 * @zm-api-command-description Get the requested device's status
 */
@XmlRootElement(name = SyncAdminConstants.E_GET_DEVICE_STATUS_REQUEST)
public class GetDeviceStatusRequest {

    /**
     * @zm-api-field-tag offset
     * @zm-api-field-description Offset for the resultant list of the devices. Offset and Limit both should be sent together in request.
     */
    @XmlAttribute(name=SyncConstants.A_OFFSET /* offset */, required=false)
    private int offset = 0;

    /**
     * @zm-api-field-tag limit
     * @zm-api-field-description Number of the devices you want in the resultant list of the devices. Offset and Limit both should be sent together in request.
     */
    @XmlAttribute(name=SyncConstants.A_LIMIT /* limit */, required=false)
    private int limit = 0;

    /**
     * @zm-api-field-tag account
     * @zm-api-field-description Account
     */
    @XmlElement(name=AdminConstants.E_ACCOUNT /* account */, required = false)
    private AccountSelector account;

    /**
     * @zm-api-field-tag deviceId
     * @zm-api-field-description Device id
     */
    @XmlElement(name = SyncConstants.E_DEVICE /* device */, required = false)
    private DeviceId deviceId;

    /**
     * @zm-api-field-tag device-status
     * @zm-api-field-description Device status
     */
    @XmlElement(name=SyncConstants.E_STATUS /* status */, required = false)
    private Byte status;

    /**
     * @zm-api-field-tag device-name
     * @zm-api-field-description Device name
     */
    @XmlElement(name=SyncConstants.E_DEVICE_NAME /* deviceName */, required = false)
    private String deviceName;

    /**
     * @zm-api-field-tag device-type
     * @zm-api-field-description Device type
     */
    @XmlElement(name=SyncConstants.E_DEVICE_TYPE /* deviceType */, required = false)
    private String deviceType;

    /**
     * @zm-api-field-tag device-last-used
     * @zm-api-field-description Device last used
     */
    @XmlElement(name=SyncConstants.E_DEVICE_LAST_USED /* deviceLastUsed */, required = false)
    private String deviceLastUsed;

    /**
     * @zm-api-field-tag device-sync-version
     * @zm-api-field-description Device sync version
     */
    @XmlElement(name=SyncConstants.E_DEVICE_SYNC_VERSION /* deviceSyncVersion */, required = false)
    private String deviceSyncVersion;

    /**
     * @zm-api-field-tag filterDevicesByAnd
     * @zm-api-field-description filter devices in and manner or not
     */
    @XmlAttribute(name=SyncConstants.A_FILTERDEVICESBYAND /* filterDevicesByAnd */, required=false)
    private String filterDevicesByAnd = "true";

    /**
     * @zm-api-field-tag domain
     * @zm-api-field-description filter devices by domain
     */
    @XmlElement(name = SyncConstants.E_DOMAIN /* domain */, required = false)
    private DomainSelector domain;

    /**
     * @zm-api-field-tag COS
     * @zm-api-field-description filter devices by COS
     */
    @XmlElement(name = SyncConstants.E_COS /* COS */, required = false)
    private CosSelector cos;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private GetDeviceStatusRequest() {
        this(null);
    }

    public GetDeviceStatusRequest(AccountSelector account) {
        this(account, null, null);
    }

    public GetDeviceStatusRequest(AccountSelector account, DeviceId deviceId, Byte status) {
        this(0, 0, account, deviceId, status, null, null, null, null);
    }

    public GetDeviceStatusRequest(int offset, int limit, AccountSelector account, DeviceId deviceId, Byte status, String deviceName,
            String deviceType, String deviceLastUsed, String deviceSyncVersion) {
        this(offset, limit, account, deviceId, status, deviceName, deviceType, deviceLastUsed, deviceSyncVersion, "true");
    }

    public GetDeviceStatusRequest(int offset, int limit, AccountSelector account, DeviceId deviceId, Byte status,
            String deviceName, String deviceType, String deviceLastUsed, String deviceSyncVersion,
            String filterDevicesByAnd) {
        this(offset, limit, account, deviceId, status, deviceName, deviceType, deviceLastUsed, deviceSyncVersion,
                filterDevicesByAnd, null, null);
    }

    /**
     * @param account
     * @param deviceId
     * @param status
     * @param deviceName
     * @param deviceType
     * @param deviceLastUsed
     * @param deviceSyncVersion
     * @param filterDevicesByAnd
     */
    public GetDeviceStatusRequest(int offset, int limit, AccountSelector account, DeviceId deviceId, Byte status,
            String deviceName, String deviceType, String deviceLastUsed, String deviceSyncVersion,
            String filterDevicesByAnd, DomainSelector domain, CosSelector cos) {
        this.offset = offset;
        this.limit = limit;
        this.account = account;
        this.deviceId = deviceId;
        this.status = status;
        this.deviceName = deviceName;
        this.deviceType = deviceType;
        this.deviceLastUsed = deviceLastUsed;
        this.deviceSyncVersion = deviceSyncVersion;
        this.filterDevicesByAnd = filterDevicesByAnd;
        this.cos = cos;
        this.domain = domain;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public int getLimit() {
        return limit;
    }

    public void setDomain(DomainSelector domain) {
        this.domain = domain;
    }

    public DomainSelector getDomain() {
        return domain;
    }

    public void setCos(CosSelector cos) {
        this.cos = cos;
    }

    public CosSelector getCos() {
        return cos;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public DeviceId getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(DeviceId deviceId) {
        this.deviceId = deviceId;
    }

    public AccountSelector getAccount() {
        return this.account;
    }

    public void setAccount(AccountSelector account) {
        this.account = account;
    }

    public Byte getStatus() {
        return this.status;
    }

    public void setStatus(Byte status) {
        this.status = status;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public String getDeviceLastUsed() {
        return deviceLastUsed;
    }

    public void setDeviceLastUsed(String deviceLastUsed) {
        this.deviceLastUsed = deviceLastUsed;
    }

    public String getDeviceSyncVersion() {
        return deviceSyncVersion;
    }

    public Boolean getFilterDevicesByAnd() throws ServiceException{
        return Element.parseBool(SyncConstants.A_FILTERDEVICESBYAND, filterDevicesByAnd);
    }

    public void setDeviceSyncVersion(String deviceSyncVersion) {
        this.deviceSyncVersion = deviceSyncVersion;
    }

    @Override
    public String toString() {
        return String.format(
                "GetDeviceStatusRequest [account=%s, deviceId=%s, status=%s, deviceName=%s, deviceType=%s, deviceLastUsed=%s, deviceSyncVersion=%s, filterDevicesByAnd=%s, domain=%s, cos=%s",
                account, deviceId, status, deviceName, deviceType, deviceLastUsed, deviceSyncVersion,
                filterDevicesByAnd, domain, cos);
    }
}
