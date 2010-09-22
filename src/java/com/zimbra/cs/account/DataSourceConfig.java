/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010 Zimbra, Inc.
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
package com.zimbra.cs.account;

import org.dom4j.io.SAXReader;
import org.dom4j.Element;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Attribute;

import java.util.List;
import java.util.ArrayList;
import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;

public final class DataSourceConfig {
    private boolean syncAllFolders = true;

    private List<Service> services;

    private static final String SYNC_ALL_FOLDERS = "syncAllFolders";
    private static final String SERVICE = "service";
    
    public static DataSourceConfig read(File file) throws IOException {
        SAXReader reader = new SAXReader();
        reader.setIgnoreComments(true);
        FileInputStream is = new FileInputStream(file);
        try {
            Document doc = reader.read(is);
            return new DataSourceConfig().read(doc.getRootElement());
        } catch (DocumentException e) {
            throw invalidConfig(e.getMessage());
        } finally {
            is.close();
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
        private List<Folder> folders;

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

        public Folder getFolderByRemotePath(String path) {
            for (Folder folder : folders) {
                if (path.equalsIgnoreCase(folder.getRemotePath())) {
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

        private static final String LOCAL_PATH = "localPath";
        private static final String REMOTE_PATH = "remotePath";
        private static final String IGNORE = "ignore";
        private static final String SYNC = "sync";

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
