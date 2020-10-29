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
import com.zimbra.soap.admin.type.MigrationInfo;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = AdminConstants.E_MIGRATE_USERS_DATA_REQUEST)
public class MigrateUsersDataRequest {

    @XmlElement(name = "migrate", required = true)
    private List<MigrationInfo> migrate = Lists.newArrayList();

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
