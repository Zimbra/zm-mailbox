/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2022 Synacor, Inc.
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
package com.zimbra.cs.volume;

public final class VolumeCLIConstants {

    /** attributes for generic purpose **/
    public static final String O_A     = "a";
    public static final String O_D     = "d";
    public static final String O_L     = "l";
    public static final String O_E     = "e";
    public static final String O_DC    = "dc";
    public static final String O_SC    = "sc";
    public static final String O_TS    = "ts";
    public static final String O_ID    = "id";
    public static final String O_T     = "t";
    public static final String O_N     = "n";
    public static final String O_P     = "p";
    public static final String O_C     = "c";
    public static final String O_CT    = "ct";
    public static final String O_SMC   = "smc";
    public static final String O_UN    = "un";

    /** attributes for external storetype **/
    public static final String O_ST    = "st";
    public static final String O_VP    = "vp";
    public static final String O_STP   = "stp";
    public static final String O_GBID  = "gbid";
    public static final String O_UFA   = "ufa";
    public static final String O_UFAT  = "ufat";
    public static final String O_UIT   = "uit";

    /** attributes for store type OpenIO **/
    public static final String O_AP    = "ap";
    public static final String O_PP    = "pp";
    public static final String O_ACC   = "acc";
    public static final String O_NS    = "ns";
    public static final String O_URL   = "url";
    public static final String OPENIO  = "OPENIO";

    /** attributes for error handling **/
    public static final String NOT_ALLOWED_INTERNAL  = " is not allowed for internal storetype";
    public static final String NOT_ALLOWED_EXTERNAL  = " is not allowed for external storetype";
    public static final String NOT_ALLOWED           = " is not allowed for edit";
    public static final String MISSING_ATTRS         = " is missing";
    public static final String NOT_ALLOWED_ID        = "id cannot be specified when adding a volume";
    public static final String INVALID_STORE_TYPE    = "invalid storetype";
    public static final String HELP_EXTERNAL_NAME    = "  only name can be edited for external store volumes ";

    /** attributes for zmvolume main menu options **/
    public static final String H_OPT_ADD             = "add";
    public static final String H_OPT_DEL             = "delete";
    public static final String H_OPT_EDIT            = "edit";
    public static final String H_OPT_LIST            = "list";
    public static final String H_OPT_DISP_CURR       = "displayCurrent";
    public static final String H_OPT_SET_CURR        = "setCurrent";
    public static final String H_OPT_TURN_OFF_SDRY   = "turnOffSecondary";

    /** attributes for maintaining title of zmvolume cli options **/
    public static final String H_TLE_INT_VOL_ADD     = "Options for adding Internal Volumes";
    public static final String H_TLE_EXT_S3_VOL_ADD  = "Options for adding External S3 Volumes";
    public static final String H_TLE_EXT_OI_VOL_ADD  = "Options for adding External OPENIO Volumes";
    public static final String H_TLE_INT_VOL_EDIT    = "Options for editing Internal Volumes";
    public static final String H_TLE_EXT_VOL_EDIT    = "Options for editing External Volumes";
    public static final String H_TLE_VOL_DEL         = "Options for deleting Volumes";
    public static final String H_TLE_OTHER_VOL_OPTS  = "Other options for Internal and External Volumes";

    /** attributes for maintaining description of zmvolume cli options **/
    public static final String H_DESC_VOL_ADD        = "Adds a volume";
    public static final String H_DESC_VOL_EDIT       = "Edits a volume";
    public static final String H_DESC_VOL_DEL        = "Deletes a volume";
    public static final String H_DESC_VOL_DISP_CURR  = "Displays the current volumes";
    public static final String H_DESC_VOL_SET_CURR   = "Sets the current volume";
    public static final String H_DESC_VOL_OFF_CURR   = "Turns off the current secondary message volume";
    public static final String H_DESC_VOL_LST        = "Lists volumes";
    public static final String H_DESC_VOL_ID         = "Volume ID";
    public static final String H_DESC_VOL_NAME       = "Volume name";
    public static final String H_DESC_VOL_TYPE_PSI   = "Volume type (primaryMessage, secondaryMessage, or index)";
    public static final String H_DESC_VOL_TYPE_PS    = "Volume type (primaryMessage, secondaryMessage)";
    public static final String H_DESC_VOL_RP         = "Root path";
    public static final String H_DESC_VOL_STI        = "Store type: internal";
    public static final String H_DESC_VOL_STE        = "Store type: external";
    public static final String H_DESC_VOL_STP_S3     = "Storage type: S3";
    public static final String H_DESC_VOL_STP_OI     = "Storage type: OPENIO";
    public static final String H_DESC_VOL_CB         = "Compress blobs: true or false";
    public static final String H_DESC_VOL_CT         = "Compression threshold: default 4KB";
    public static final String H_DESC_VOL_SMC        = "Non-default store manager class path";
    public static final String H_DESC_VOL_PRE        = "Volume Prefix";
    public static final String H_DESC_VOL_BKT_ID     = "S3 Bucket ID";
    public static final String H_DESC_VOL_AWS_UIT    = "AWS only - Use Intelligent tiering storage class";
    public static final String H_DESC_VOL_AWS_UFA    = "AWS only - Use Infrequent access storage class";
    public static final String H_DESC_VOL_AWS_UFAT   = "AWS only - Use Infrequent access storage class blob size threshold";
    public static final String H_DESC_VOL_URL        = "URL of OpenIO";
    public static final String H_DESC_VOL_NS         = "Namespace";
    public static final String H_DESC_VOL_PP         = "Proxy port";
    public static final String H_DESC_VOL_AP         = "Account port";
    public static final String H_DESC_VOL_ACC        = "Name of account";
    public static final String H_DESC_VOL_ST         = "Store type: internal or external";
    public static final String H_DESC_VOL_STP        = "Supported storage types are S3 and OPENIO";
    public static final String H_STORE_MANAGER_CLASS = "Optional parameter to specify non-default store manager class path";
    public static final String H_DESC_VOL_UN         = "Unified Storage for S3 (true, false)";

    /** attributes for maintaining example of zmvolume cli options **/
    public static final String H_EXP_INT_VOL_ADD     = "Example: zmvolume -a -t primaryMessage -p /opt/zimbra/sda1 -n internalVolName";
    public static final String H_EXP_EXT_S3_VOL_ADD  = "Example: zmvolume -a -t primaryMessage -p /opt/zimbra/sda1 -n externalVolName -st external -stp S3 -gbid 35d6a471-5218-45f2-b883-62926df94f9b";
    public static final String H_EXP_EXT_OI_VOL_ADD  = "Example: zmvolume -a -t primaryMessage -p /opt/zimbra/sda1 -n externalVolName -st external -pp 6006 -ap 6000 -acc MY_ACCOUNT -url http://10.10.10.10 -stp OPENIO -ns OPENIO";
    public static final String H_EXP_INT_VOL_EDIT    = "Example: zmvolume -e -id 20 -n newVolName -t secondaryMessage";
    public static final String H_EXP_EXT_VOL_EDIT    = "Example: zmvolume -e -id 10 -n newVolName";
    public static final String H_EXP_VOL_DEL         = "Example: zmvolume -d -id 20";

    public static final String H_NOTE_EXT_OI_VOL_ADD = "Note: Type index volume is not supported for external OPENIO volume";
    public static final String H_NOTE_EXT_S3_VOL_ADD = "Note: Type index volume is not supported for external S3 volume";
    public static final String H_NOTE_EXT_VOL_EDIT   = "Note: Only volume name can be edited for external volumes";

    /** attributes for other zmvolume cli options **/
    public static final String STR_SPACE             = " ";
    public static final String STR_EMPTY             = "";
    public static final String STR_MAIN_CLI_ARGS     = "{ -a | -d | -l | -e | -dc | -sc | -ts }";
    public static final String STR_MAIN_CMD_NAME     = "zmvolume";
    public static final String STR_ERR_NO_ACTION     = "No action " + STR_MAIN_CLI_ARGS + " is specified";
    public static final int    INT_ALIGN_LEFT        = 60;
}
