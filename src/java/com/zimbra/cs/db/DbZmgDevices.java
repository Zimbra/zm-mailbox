/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2015 Zimbra, Inc.
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

package com.zimbra.cs.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.ZmgDevice;
import com.zimbra.cs.db.DbPool.DbConnection;
import com.zimbra.cs.mailbox.Mailbox;

public class DbZmgDevices {

    public static final String TABLE_ZMG_DEVICES = "zmg_devices";
    public static final String MAILBOX_ID = "mailbox_id";
    public static final String DEVICE_ID = "device_id";
    public static final String REG_ID = "reg_id";
    public static final String PUSH_PROVIDER = "push_provider";

    public static final int CI_MAILBOX_ID = 1;
    public static final int CI_DEVICE_ID = 2;
    public static final int CI_REG_ID = 3;
    public static final int CI_PUSH_PROVIDER = 4;

    public static int addDevice(ZmgDevice device) throws ServiceException {
        DbConnection conn = null;
        PreparedStatement stmt = null;
        int result = 0;

        try {
            conn = DbPool.getConnection();
            stmt = conn
                .prepareStatement("REPLACE INTO zmg_devices (mailbox_id, device_id, reg_id, push_provider) VALUES (?,?,?,?)");

            int pos = 1;
            stmt.setInt(pos++, device.getMailboxId());
            stmt.setString(pos++, device.getDeviceId());
            stmt.setString(pos++, device.getRegistrationId());
            stmt.setString(pos++, device.getPushProvider());
            result = stmt.executeUpdate();
            stmt.close();
            conn.commit();
        } catch (ServiceException | SQLException e) {
            throw ServiceException.FAILURE("failed to add device " + device.getDeviceId(), e);
        } finally {
            DbPool.closeStatement(stmt);
            DbPool.quietClose(conn);
        }
        return result;
    }

    public static Collection<ZmgDevice> getDevices(Mailbox mbox) throws ServiceException {
        DbConnection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<ZmgDevice> devices = new ArrayList<ZmgDevice>();

        try {
            conn = DbPool.getConnection(mbox);
            stmt = conn
                .prepareStatement("SELECT mailbox_id, device_id, reg_id, push_provider  FROM zmg_devices WHERE mailbox_id = ?");

            int pos = 1;
            stmt.setInt(pos++, mbox.getId());
            rs = stmt.executeQuery();
            while (rs.next()) {
                ZmgDevice device = new ZmgDevice(rs.getInt(CI_MAILBOX_ID),
                    rs.getString(CI_DEVICE_ID), rs.getString(CI_REG_ID),
                    rs.getString(CI_PUSH_PROVIDER));
                devices.add(device);
            }
            return devices;
        } catch (ServiceException | SQLException e) {
            throw ServiceException.FAILURE("failed to get device for mailbox" + mbox.getId(), e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
            DbPool.quietClose(conn);
        }
    }
}
