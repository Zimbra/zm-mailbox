/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2020 Synacor, Inc.
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

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.MigrationInfo;
import com.zimbra.soap.type.ZmBoolean;

/**
 * @zm-api-command-admin-auth-required true
 * @zm-api-command-description Migrate users from source system to destination system
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = AdminConstants.E_MIGRATE_USERS_DATA_REQUEST)
public class MigrateUsersDataRequest {

    @XmlAttribute(name = AdminConstants.A_IS_SSL, required = false)
    private ZmBoolean isSsl;

    @XmlElement(name = AdminConstants.E_MIGRATE, required = true)
    private List<MigrationInfo> migrate = Lists.newArrayList();

    /**
     * no-argument constructor wanted by JAXB
     */
    public MigrateUsersDataRequest() {
    }

    public void setMigrationInfo(Iterable <MigrationInfo> migrationInfo) {
        this.migrate.clear();
        if (migrationInfo != null) {
            Iterables.addAll(this.migrate, migrationInfo);
        }
    }

    public void addMigrationInfo(MigrationInfo migrationInfo) {
        this.migrate.add(migrationInfo);
    }

    public List <MigrationInfo> getMigrationList() {
        return Collections.unmodifiableList(migrate);
    }
}
