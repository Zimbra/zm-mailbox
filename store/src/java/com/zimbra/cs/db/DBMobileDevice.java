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
package com.zimbra.cs.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.db.DbPool.DbConnection;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MobileDevice;

public class DBMobileDevice {

	public static final String TABLE_ZMG_DEVICES = "zmg_devices";
	public static final String MAILBOX_ID = "mailbox_id";
	public static final String APP_ID = "app_id";
	public static final String REG_ID = "reg_id";
	public static final String PUSH_PROVIDER = "push_provider";
	public static final String OS_NAME = "os_name";
	public static final String OS_VERSION = "os_version";
	public static final String MAX_PAYLOAD_SIZE = "max_payload_size";

	// As we support FCM only
	private static final String PUSH_PROVIDER_VALUE = "FCM";

	public static final int CI_MAILBOX_ID = 1;
	public static final int CI_APP_ID = 2;
	public static final int CI_REG_ID = 3;
	public static final int CI_OS_NAME = 4;
	public static final int CI_OS_VERSION = 5;

	public static int addMobileDevice(MobileDevice mobileDevice) throws ServiceException {
		DbConnection conn = null;
		PreparedStatement stmt = null;
		int result = 0;

		try {
			conn = DbPool.getConnection();
			stmt = conn.prepareStatement("INSERT INTO " + TABLE_ZMG_DEVICES + " (" + MAILBOX_ID + ", " + APP_ID + ", "
					+ REG_ID + ", " + PUSH_PROVIDER + ", " + OS_NAME + ", " + OS_VERSION + ") VALUES (?,?,?,?,?,?)");

			int pos = 1;
			stmt.setInt(pos++, mobileDevice.getMailboxId());
			stmt.setString(pos++, mobileDevice.getAppVersion());
			stmt.setString(pos++, mobileDevice.getDeviceToken());
			stmt.setString(pos++, PUSH_PROVIDER_VALUE);
			stmt.setString(pos++, mobileDevice.getDeviceOSName());
			stmt.setString(pos++, mobileDevice.getDeviceOSVersion());
			result = stmt.executeUpdate();
			conn.commit();
		} catch (ServiceException | SQLException e) {
			throw ServiceException.FAILURE("DBMobileDevice : error adding device token for mailbox id - "
					+ mobileDevice.getMailboxId(), e);
		} finally {
			DbPool.closeStatement(stmt);
			DbPool.quietClose(conn);
		}
		return result;
	}

	public static List<MobileDevice> getMobileDevices(Mailbox mbox) throws ServiceException {
		DbConnection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		List<MobileDevice> devices = new ArrayList<MobileDevice>();

		try {
			conn = DbPool.getConnection();
			stmt = conn.prepareStatement("SELECT " + MAILBOX_ID + ", " + APP_ID + ", " + REG_ID + ", " + OS_NAME + ", "
					+ OS_VERSION + " FROM " + TABLE_ZMG_DEVICES + " WHERE " + MAILBOX_ID + " = ?");

			int pos = 1;
			stmt.setInt(pos++, mbox.getId());
			rs = stmt.executeQuery();
			while (rs.next()) {
				MobileDevice device = new MobileDevice(rs.getInt(CI_MAILBOX_ID), rs.getString(CI_APP_ID),
						rs.getString(CI_REG_ID), rs.getString(CI_OS_NAME), rs.getString(CI_OS_VERSION));
				devices.add(device);
			}
			return devices;
		} catch (ServiceException | SQLException e) {
			throw ServiceException.FAILURE(
					"DBMobileDevice : error getting device token for mailbox id - " + mbox.getId(), e);
		} finally {
			DbPool.closeResults(rs);
			DbPool.closeStatement(stmt);
			DbPool.quietClose(conn);
		}
	}

	public static int removeDevice(MobileDevice mobileDevice) throws ServiceException {
		DbConnection conn = null;
		PreparedStatement stmt = null;
		int result = 0;

		try {
			conn = DbPool.getConnection();
			stmt = conn.prepareStatement("DELETE FROM " + TABLE_ZMG_DEVICES + " where " + REG_ID + " = ?");

			int pos = 1;
			stmt.setString(pos++, mobileDevice.getDeviceToken());
			result = stmt.executeUpdate();
			conn.commit();
		} catch (ServiceException | SQLException e) {
			throw ServiceException.FAILURE(
					"DBMobileDevice : error removing device token - " + mobileDevice.getDeviceToken(), e);
		} finally {
			DbPool.closeStatement(stmt);
			DbPool.quietClose(conn);
		}
		return result;
	}

}
