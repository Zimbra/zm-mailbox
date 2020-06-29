package com.zimbra.soap.admin.message;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;

import com.google.common.collect.Lists;
import com.zimbra.common.soap.BackupConstants;
import com.zimbra.soap.admin.type.BackupHost;

public class BackupRestoreBase {

    public BackupRestoreBase() {}

    @XmlElementWrapper(name=BackupConstants.E_REMOTE_BACKUP_HOSTS, required=false)
    @XmlElement(name=BackupConstants.E_REMOTE_BACKUP_HOST, type=BackupHost.class, required=false)
    private List<BackupHost> remoteBackupHosts =  Lists.newArrayList();

    public void addRemoteBackupHost(BackupHost host) {
        remoteBackupHosts.add(host);
    }

    public List<BackupHost> getRemoteBackupHosts() {
        return remoteBackupHosts;
    }
}
