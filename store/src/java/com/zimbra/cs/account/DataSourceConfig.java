/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2013, 2014, 2015, 2016 Synacor, Inc.
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
package com.zimbra.cs.account;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.Element;

import com.zimbra.common.soap.W3cDomUtil;
import com.zimbra.common.soap.XmlParseException;
import com.zimbra.cs.mailclient.imap.Atom;
import com.zimbra.cs.mailclient.imap.Flags;

public final class DataSourceConfig {
    private boolean syncAllFolders = true;

    private final List<Service> services;

    private static final String SYNC_ALL_FOLDERS = "syncAllFolders";
    private static final String SERVICE = "service";

    public static DataSourceConfig read(File file) throws IOException {
        try (FileInputStream is = new FileInputStream(file)) {
            Document doc = W3cDomUtil.parseXMLToDom4jDocUsingSecureProcessing(is);
            return new DataSourceConfig().read(doc.getRootElement());
        } catch (XmlParseException e) {
            throw invalidConfig(e.getMessage());
        }
    }

    private DataSourceConfig() {
        services = new ArrayList<Service>();
    }

    public boolean isSyncAllFolders() {
        return syncAllFolders;
    }

    public List<Service> getServices() {
        return services;
    }

    public Service getService(String name) {
        for (Service service : services) {
            if (name.equalsIgnoreCase(service.getName())) {
                return service;
            }
        }
        return null;
    }

    private DataSourceConfig read(Element element) throws IOException {
        for (Object obj : element.attributes()) {
            Attribute attr = (Attribute) obj;
            String name = attr.getName();
            if (name.equals(SYNC_ALL_FOLDERS)) {
                syncAllFolders = Boolean.valueOf(attr.getValue());
            } else {
                throw invalidConfig("Unrecognized attribute name: " + name);
            }
        }
        for (Object obj : element.elements()) {
            Element el = (Element) obj;
            String name = el.getName();
            if (name.equals(SERVICE)) {
                services.add(new Service().read(el));
            } else {
                throw invalidConfig("Unrecognized element name: " + name);
            }
        }
        return this;
    }

    public static final class Service {
        private String name;
        private boolean saveToSent = true;
        private String calDavTargetUrl;
        private String calDavPrincipalPath;
        private final List<Folder> folders;

        private static final String NAME = "name";
        private static final String SAVE_TO_SENT = "saveToSent";
        private static final String CAL_DAV_TARGET_URL = "calDavTargetUrl";
        private static final String CAL_DAV_PRINCIPAL_PATH = "calDavPrincipalPath";
        private static final String FOLDER = "folder";

        public Service() {
            folders = new ArrayList<Folder>();
        }

        public String getName() {
            return name;
        }

        public boolean isSaveToSent() {
            return saveToSent;
        }

        public String getCalDavTargetUrl() {
            return calDavTargetUrl;
        }

        public String getCalDavPrincipalPath() {
            return calDavPrincipalPath;
        }

        public List<Folder> getFolders() {
            return folders;
        }

        public Folder getFolderByLocalPath(String path) {
            for (Folder folder : folders) {
                if (path.equalsIgnoreCase(folder.getLocalPath())) {
                    return folder;
                }
            }
            return null;
        }

        public Folder getFolderByRemotePath(String path, Flags flags) {
            for (Folder folder : folders) {
                if (path.equalsIgnoreCase(folder.getRemotePath())
                    || (flags != null && flags.contains(folder.getFolderNameFlag()))) {
                    return folder;
                }
            }
            return null;
        }

        private Service read(Element element) {
            for (Object obj : element.attributes()) {
                Attribute attr = (Attribute) obj;
                String name = attr.getName();
                if (name.equals(NAME)) {
                    this.name = attr.getValue();
                } else if (name.equals(SAVE_TO_SENT)) {
                    saveToSent = Boolean.valueOf(attr.getValue());
                } else if (name.equals(CAL_DAV_TARGET_URL)) {
                    calDavTargetUrl = attr.getValue();
                } else if (name.equals(CAL_DAV_PRINCIPAL_PATH)) {
                    calDavPrincipalPath = attr.getValue();
                } else {
                    throw invalidConfig("Unrecognized service attribute name: " + name);
                }
            }
            if (name == null) {
                throw invalidConfig("Missing service name attribute");
            }
            for (Object obj : element.elements()) {
                Element el = (Element) obj;
                String name = el.getName();
                if (name.equals(FOLDER)) {
                    folders.add(new Folder().read(el));
                } else {
                    throw invalidConfig("Unrecognized service element name: " + name);
                }
            }
            return this;
        }
    }

    public static final class Folder {
        private String localPath;
        private String remotePath;
        private boolean ignore;
        private boolean sync;
        private Atom folderNameFlag;

        private static final String LOCAL_PATH = "localPath";
        private static final String REMOTE_PATH = "remotePath";
        private static final String IGNORE = "ignore";
        private static final String SYNC = "sync";
        private static final String FOLDER_NAME = "folderName";

        public String getLocalPath() {
            return localPath;
        }

        public String getRemotePath() {
            return remotePath;
        }

        public boolean isIgnore() {
            return ignore;
        }

        public boolean isSync() {
            return sync;
        }

        public Atom getFolderNameFlag() {
            return folderNameFlag;
        }

        private Folder read(Element element) {
            for (Object obj : element.attributes()) {
                Attribute attr = (Attribute) obj;
                String name = attr.getName();
                if (name.equals(REMOTE_PATH)) {
                    remotePath = attr.getValue();
                } else if (name.equals(LOCAL_PATH)) {
                    localPath = attr.getValue();
                } else if (name.equals(IGNORE)) {
                    ignore = Boolean.valueOf(attr.getValue());
                } else if (name.equals(SYNC)) {
                    sync = Boolean.valueOf(attr.getValue());
                } else if (name.equals(FOLDER_NAME)) {
                    String value = attr.getValue() != null ? attr.getValue() : "";
                    folderNameFlag = new Atom(value);
                } else {
                    throw invalidConfig("Unrecognized folder attribute name: " + name);
                }
            }
            if (!element.elements().isEmpty()) {
                throw invalidConfig("Folder should not contain any sub-elements");
            }
            if (remotePath == null) {
                throw invalidConfig("Missing folder remotePath attribute");
            }
            if (localPath == null) {
                localPath = remotePath;
            }
            if (!localPath.startsWith("/")) {
                localPath = "/" + localPath;
            }
            return this;
        }
    }

    private static IllegalArgumentException invalidConfig(String msg) {
        return new IllegalArgumentException("Invalid configuration: " + msg);
    }

    public static void main(String[] args) throws Exception {
        DataSourceConfig config = DataSourceConfig.read(new File(args[0]));
        System.out.println("config = " + config);
    }
}
