/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2011, 2011 Zimbra, Inc.
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
package com.zimbra.common.soap;

public final class ZimletConstants {

    /* top level */
    public static final String ZIMLET_TAG_ZIMLET = "zimlet";

    /* first level */
    public static final String ZIMLET_ATTR_VERSION         = "version";
    public static final String ZIMLET_ATTR_DESCRIPTION     = "description";
    public static final String ZIMLET_ATTR_NAME            = "name";
    public static final String ZIMLET_ATTR_EXTENSION       = "extension";

    public static final String ZIMLET_TAG_SCRIPT           = "include";
    public static final String ZIMLET_TAG_CSS              = "includeCSS";
    public static final String ZIMLET_TAG_CONTENT_OBJECT   = "contentObject";
    /* value was "panelItem" - believe this was in error */
    public static final String ZIMLET_TAG_PANEL_ITEM       = "zimletPanelItem";

    /* for serverExtension branch */
    public static final String ZIMLET_TAG_SERVER_EXTENSION = "serverExtension";
    public static final String ZIMLET_ATTR_HAS_KEYWORD     = "hasKeyword";
    public static final String ZIMLET_ATTR_MATCH_ON        = "matchOn";
    public static final String ZIMLET_ATTR_EXTENSION_CLASS = "extensionClass";
    public static final String ZIMLET_ATTR_REGEX           = "regex";

    /* config description file */
    public static final String ZIMLET_TAG_CONFIG           = "zimletConfig";

    public static final String ZIMLET_TAG_GLOBAL           = "global";
    public static final String ZIMLET_TAG_HOST             = "host";
    public static final String ZIMLET_TAG_PROPERTY         = "property";

    public static final String ZIMLET_TAG_TARGET           = "target";
    public static final String ZIMLET_TAG_LABEL           = "label";
    public static final String ZIMLET_DISABLE_UI_UNDEPLOY   = "disableUIUndeploy";
}
