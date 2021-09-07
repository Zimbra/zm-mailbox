/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2021 Synacor, Inc.
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
package com.zimbra.cs.mailbox;

public class MobileDevice {
	private int mailboxId;
	private String appVersion;
	private String deviceToken;
	private String deviceOSName;
	private String deviceOSVersion;

	public MobileDevice(int mailboxId, String appVersion, String deviceToken, String deviceOSName,
			String deviceOSVersion) {
		this.mailboxId = mailboxId;
		this.appVersion = appVersion;
		this.deviceToken = deviceToken;
		this.deviceOSName = deviceOSName;
		this.deviceOSVersion = deviceOSVersion;
	}
	public int getMailboxId() {
		return mailboxId;
	}
	public void setMailboxId(int mailboxId) {
		this.mailboxId = mailboxId;
	}
	public String getAppVersion() {
		return appVersion;
	}
	public void setAppVersion(String appVersion) {
		this.appVersion = appVersion;
	}
	public String getDeviceToken() {
		return deviceToken;
	}
	public void setDeviceToken(String deviceToken) {
		this.deviceToken = deviceToken;
	}
	public String getDeviceOSName() {
		return deviceOSName;
	}
	public void setDeviceOSName(String deviceOSName) {
		this.deviceOSName = deviceOSName;
	}
	public String getDeviceOSVersion() {
		return deviceOSVersion;
	}
	public void setDeviceOSVersion(String deviceOSVersion) {
		this.deviceOSVersion = deviceOSVersion;
	}
	@Override
	public String toString() {
		return "MobileDevice [mailboxId=" + mailboxId + ", appVersion=" + appVersion + ", deviceToken=" + deviceToken
				+ ", deviceOSName=" + deviceOSName + ", deviceOSVersion=" + deviceOSVersion + "]";
	}

	


}
