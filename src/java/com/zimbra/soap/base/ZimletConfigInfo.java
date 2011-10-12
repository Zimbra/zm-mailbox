/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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

package com.zimbra.soap.base;

public interface ZimletConfigInfo {
    public void setName(String name);
    public void setVersion(String version);
    public void setDescription(String description);
    public void setExtension(String extension);
    public void setTarget(String target);
    public void setLabel(String label);
    public void setGlobal(ZimletGlobalConfigInfo global);
    public void setHost(ZimletHostConfigInfo host);
    public String getName();
    public String getVersion();
    public String getDescription();
    public String getExtension();
    public String getTarget();
    public String getLabel();
    public ZimletGlobalConfigInfo getGlobal();
    public ZimletHostConfigInfo getHost();
}
