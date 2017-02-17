/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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

    @Override
    public String[] getMimeTypes() {
        return mimeTypes;
    }

    public void setMimeTypes(String... value) {
        mimeTypes = value;
    }

    @Override
    public String getExtension() {
        return extension;
    }

    public void setExtension(String value) {
        extension = value;
    }

    @Override
    public String getHandlerClass() {
        return handlerClass;
    }

    public void setHandlerClass(String value) {
        handlerClass = value;
    }

    @Override
    public boolean isIndexingEnabled() {
        return indexingEnabled;
    }

    public void setIndexingEnabled(boolean value) {
        indexingEnabled = value;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public void setDescription(String value) {
        description = value;
    }

    @Override
    public Set<String> getFileExtensions() {
        return fileExtensions;
    }

    public void setFileExtensions(String... value) {
        fileExtensions.clear();
        for (String ext : value) {
            fileExtensions.add(ext);
        }
    }

    @Override
    public int getPriority() {
        return priority;
    }

    public void setPriority(int value) {
        priority = value;
    }

}
