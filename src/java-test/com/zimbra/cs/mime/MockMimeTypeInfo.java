/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010 Zimbra, Inc.
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
package com.zimbra.cs.mime;

import java.util.HashSet;
import java.util.Set;

/**
 * Mock implementation of {@link MimeTypeInfo} for testing.
 *
 * @author ysasaki
 */
public class MockMimeTypeInfo implements MimeTypeInfo {
    private String[] mimeTypes = new String[0];
    private Set<String> fileExtensions = new HashSet<String>();
    private String description;
    private boolean indexingEnabled;
    private String extension;
    private String handlerClass;
    private int priority;

    public String[] getMimeTypes() {
        return mimeTypes;
    }

    public void setMimeTypes(String... value) {
        mimeTypes = value;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String value) {
        extension = value;
    }

    public String getHandlerClass() {
        return handlerClass;
    }

    public void setHandlerClass(String value) {
        handlerClass = value;
    }

    public boolean isIndexingEnabled() {
        return indexingEnabled;
    }

    public void setIndexingEnabled(boolean value) {
        indexingEnabled = value;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String value) {
        description = value;
    }

    public Set<String> getFileExtensions() {
        return fileExtensions;
    }

    public void setFileExtensions(String... value) {
        fileExtensions.clear();
        for (String ext : value) {
            fileExtensions.add(ext);
        }
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int value) {
        priority = value;
    }

}
