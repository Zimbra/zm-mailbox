/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.common.soap;

public final class ZimletConstants {

    /* top level */
    public static final String ZIMLET_TAG_ZIMLET = "zimlet";

    /* first level */
    public static final String ZIMLET_ATTR_VERSION         = "version";
    public static final String ZIMLET_ATTR_DESCRIPTION     = "description";
    public static final String ZIMLET_ATTR_ZIMBRAX_SEMVER  = "zimbraXZimletCompatibleSemVer";
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
