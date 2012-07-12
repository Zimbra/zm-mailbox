/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2012 Zimbra, Inc.
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

import org.dom4j.QName;
import org.dom4j.Namespace;

public class VoiceConstants {

    public static final String NAMESPACE_STR = "urn:zimbraVoice";
    public static final Namespace NAMESPACE = Namespace.get(NAMESPACE_STR);

    public static final String E_GET_UC_INFO_REQUEST = "GetUCInfoRequest";
    public static final String E_GET_UC_INFO_RESPONSE = "GetUCInfoResponse";

    public static final String E_CHANGE_UC_PASSWORD_REQUEST = "ChangeUCPasswordRequest";
    public static final String E_CHANGE_UC_PASSWORD_RESPONSE = "ChangeUCPasswordResponse";

    public static final String E_GET_VOICE_MAIL_PREFS_REQUEST = "GetVoiceMailPrefsRequest";
    public static final String E_GET_VOICE_MAIL_PREFS_RESPONSE = "GetVoiceMailPrefsResponse";

    public static final String E_MODIFY_VOICE_MAIL_PREFS_REQUEST = "ModifyVoiceMailPrefsRequest";
    public static final String E_MODIFY_VOICE_MAIL_PREFS_RESPONSE = "ModifyVoiceMailPrefsResponse";

    public static final String E_MODIFY_VOICE_MAIL_PIN_REQUEST = "ModifyVoiceMailPinRequest";
    public static final String E_MODIFY_VOICE_MAIL_PIN_RESPONSE = "ModifyVoiceMailPinResponse";

    public static final String E_MODIFY_FROM_NUM_REQUEST = "ModifyFromNumRequest";
    public static final String E_MODIFY_FROM_NUM_RESPONSE = "ModifyFromNumResponse";

    public static final String E_GET_VOICE_INFO_REQUEST = "GetVoiceInfoRequest";
    public static final String E_GET_VOICE_INFO_RESPONSE = "GetVoiceInfoResponse";

    public static final String E_GET_VOICE_FOLDER_REQUEST = "GetVoiceFolderRequest";
    public static final String E_GET_VOICE_FOLDER_RESPONSE = "GetVoiceFolderResponse";

    public static final String E_SEARCH_VOICE_REQUEST = "SearchVoiceRequest";
    public static final String E_SEARCH_VOICE_RESPONSE = "SearchVoiceResponse";

    public static final String E_GET_VOICE_MSG_REQUEST = "GetVoiceMsgRequest";
    public static final String E_GET_VOICE_MSG_RESPONSE = "GetVoiceMsgResponse";

    public static final String E_VOICE_MSG_ACTION_REQUEST = "VoiceMsgActionRequest";
    public static final String E_VOICE_MSG_ACTION_RESPONSE = "VoiceMsgActionResponse";

    public static final String E_GET_VOICE_FEATURES_REQUEST = "GetVoiceFeaturesRequest";
    public static final String E_GET_VOICE_FEATURES_RESPONSE = "GetVoiceFeaturesResponse";

    public static final String E_MODIFY_VOICE_FEATURES_REQUEST = "ModifyVoiceFeaturesRequest";
    public static final String E_MODIFY_VOICE_FEATURES_RESPONSE = "ModifyVoiceFeaturesResponse";

    public static final String E_RESET_VOICE_FEATURES_REQUEST = "ResetVoiceFeaturesRequest";
    public static final String E_RESET_VOICE_FEATURES_RESPONSE = "ResetVoiceFeaturesResponse";

    public static final String E_UPLOAD_VOICE_MAIL_REQUEST = "UploadVoiceMailRequest";
    public static final String E_UPLOAD_VOICE_MAIL_RESPONSE = "UploadVoiceMailResponse";

    public static final QName GET_UC_INFO_REQUEST = QName.get(E_GET_UC_INFO_REQUEST, NAMESPACE);
    public static final QName GET_UC_INFO_RESPONSE = QName.get(E_GET_UC_INFO_RESPONSE, NAMESPACE);

    public static final QName CHANGE_UC_PASSWORD_REQUEST = QName.get(E_CHANGE_UC_PASSWORD_REQUEST, NAMESPACE);
    public static final QName CHANGE_UC_PASSWORD_RESPONSE = QName.get(E_CHANGE_UC_PASSWORD_RESPONSE, NAMESPACE);

    public static final QName GET_VOICE_MAIL_PREFS_REQUEST = QName.get(E_GET_VOICE_MAIL_PREFS_REQUEST, NAMESPACE);
    public static final QName GET_VOICE_MAIL_PREFS_RESPONSE = QName.get(E_GET_VOICE_MAIL_PREFS_RESPONSE, NAMESPACE);

    public static final QName MODIFY_VOICE_MAIL_PREFS_REQUEST = QName.get(E_MODIFY_VOICE_MAIL_PREFS_REQUEST, NAMESPACE);
    public static final QName MODIFY_VOICE_MAIL_PREFS_RESPONSE = QName.get(E_MODIFY_VOICE_MAIL_PREFS_RESPONSE, NAMESPACE);

    public static final QName MODIFY_VOICE_MAIL_PIN_REQUEST = QName.get(E_MODIFY_VOICE_MAIL_PIN_REQUEST, NAMESPACE);
    public static final QName MODIFY_VOICE_MAIL_PIN_RESPONSE = QName.get(E_MODIFY_VOICE_MAIL_PIN_RESPONSE, NAMESPACE);

    public static final QName MODIFY_FROM_NUM_REQUEST = QName.get(E_MODIFY_FROM_NUM_REQUEST, NAMESPACE);
    public static final QName MODIFY_FROM_NUM_RESPONSE = QName.get(E_MODIFY_FROM_NUM_RESPONSE, NAMESPACE);

    public static final QName GET_VOICE_INFO_REQUEST = QName.get(E_GET_VOICE_INFO_REQUEST, NAMESPACE);
    public static final QName GET_VOICE_INFO_RESPONSE = QName.get(E_GET_VOICE_INFO_RESPONSE, NAMESPACE);

    public static final QName GET_VOICE_FOLDER_REQUEST = QName.get(E_GET_VOICE_FOLDER_REQUEST, NAMESPACE);
    public static final QName GET_VOICE_FOLDER_RESPONSE = QName.get(E_GET_VOICE_FOLDER_RESPONSE, NAMESPACE);

    public static final QName SEARCH_VOICE_REQUEST = QName.get(E_SEARCH_VOICE_REQUEST, NAMESPACE);
    public static final QName SEARCH_VOICE_RESPONSE = QName.get(E_SEARCH_VOICE_RESPONSE, NAMESPACE);

    public static final QName GET_VOICE_MSG_REQUEST = QName.get(E_GET_VOICE_MSG_REQUEST, NAMESPACE);
    public static final QName GET_VOICE_MSG_RESPONSE = QName.get(E_GET_VOICE_MSG_RESPONSE, NAMESPACE);

    public static final QName VOICE_MSG_ACTION_REQUEST = QName.get(E_VOICE_MSG_ACTION_REQUEST, NAMESPACE);
    public static final QName VOICE_MSG_ACTION_RESPONSE = QName.get(E_VOICE_MSG_ACTION_RESPONSE, NAMESPACE);

    public static final QName GET_VOICE_FEATURES_REQUEST = QName.get(E_GET_VOICE_FEATURES_REQUEST, NAMESPACE);
    public static final QName GET_VOICE_FEATURES_RESPONSE = QName.get(E_GET_VOICE_FEATURES_RESPONSE, NAMESPACE);

    public static final QName MODIFY_VOICE_FEATURES_REQUEST = QName.get(E_MODIFY_VOICE_FEATURES_REQUEST, NAMESPACE);
    public static final QName MODIFY_VOICE_FEATURES_RESPONSE = QName.get(E_MODIFY_VOICE_FEATURES_RESPONSE, NAMESPACE);

    public static final QName RESET_VOICE_FEATURES_REQUEST = QName.get(E_RESET_VOICE_FEATURES_REQUEST, NAMESPACE);
    public static final QName RESET_VOICE_FEATURES_RESPONSE = QName.get(E_RESET_VOICE_FEATURES_RESPONSE, NAMESPACE);

    public static final QName UPLOAD_VOICE_MAIL_REQUEST = QName.get(E_UPLOAD_VOICE_MAIL_REQUEST, NAMESPACE);
    public static final QName UPLOAD_VOICE_MAIL_RESPONSE = QName.get(E_UPLOAD_VOICE_MAIL_RESPONSE, NAMESPACE);

    public static final String E_PHONE         = "phone";
    public static final String E_CALLLOG       = "cl";
    public static final String E_CALLPARTY     = "cp";
    public static final String E_STOREPRINCIPAL= "storeprincipal";
    public static final String E_VOICEMSG      = "vm";
    public static final String E_UPLOAD        = "upload";
    public static final String E_AUDIO_TYPE    = "audioType";

    public static final String A_ACCOUNT_NUMBER = "accountNumber";
    public static final String A_NAME          = "name";
    public static final String A_LABEL         = "label";
    public static final String A_C2C_DEVICE_ID = "c2cDeviceId";
    public static final String A_ID            = "id";
    public static final String A_VMSG_DURATION = "du";
    public static final String A_PHONE         = "phone";
    public static final String A_OLD_PHONE  = "oldPhone";
    public static final String A_PHONENUM      = "n";
    public static final String A_CITY          = "ci";
    public static final String A_STATE         = "st";
    public static final String A_COUNTRY       = "co";
    public static final String A_VM            = "vm";
    public static final String A_CONTENT_TYPE  = "ct";
    public static final String A_VMAIL_OLD_PIN  = "oldPin";
    public static final String A_VMAIL_PIN  = "pin";
    public static final String A_TYPE          = "type";
    public static final String A_CALLABLE = "callable";
    public static final String A_EDITABLE = "editable";

    public static final String FLAG_UNFORWARDABLE = "p";  // 'p'rivate

    // folder inventory
    public static final String E_VOICE_FOLDER_INVENTORY   = "vfi";

    //
    // call feature elements
    //
    // public static final String E_CALL_FEATURE    = "callfeature";
    public static final String E_CALL_FEATURES   = "callfeatures";
    public static final String E_CALL_FEATURE    = "callfeature";

    public static final String E_ANON_CALL_REJECTION        = "anoncallrejection";
    public static final String E_CALLER_ID_BLOCKING         = "calleridblocking";
    public static final String E_CALL_FORWARD               = "callforward";
    // public static final String E_CALL_FORWARD_COMBINATION   = "callforwardcombination"; // Deprecated in jr-vnes v1.6
    public static final String E_CALL_FORWARD_BUSY_LINE     = "callforwardbusyline";
    public static final String E_CALL_FORWARD_NO_ANSWER     = "callforwardnoanswer";
    public static final String E_CALL_WAITING               = "callwaiting";
    public static final String E_DO_NOT_DISTURB             = "donotdisturb";
    public static final String E_SELECTIVE_CALL_FORWARD     = "selectivecallforward";
    public static final String E_SELECTIVE_CALL_ACCEPTANCE  = "selectivecallacceptance";
    public static final String E_SELECTIVE_CALL_REJECTION   = "selectivecallrejection";
    public static final String E_VOICE_MAIL_PREFS           = "voicemailprefs";

    public static final String A_ACTIVE          = "a";
    public static final String A_FORWARD_TO      = "ft";
    public static final String A_NUM_RING_CYCLES = "nr";
    public static final String A_PHONE_NUMBER    = "pn";
    public static final String A_SUBSCRIBED      = "s";

    public static final String C_UNKNOWN         = "unknown";  // for cases gateway do not return us a value it should,
    // we output unknown to our client instead of throwing
    // an exception

    public static final String DEFAULT_VOICE_VIEW = "voice";

    public static final String E_PREF                          = "pref";
    public static final String A_vmPrefEmailNotifAddress       = "vmPrefEmailNotifAddress";
    public static final String A_vmPrefPin                     = "vmPrefPin";
    public static final String A_vmPrefTimezone                = "vmPrefTimezone";
    public static final String A_vmPrefPlayDateAndTimeInMsgEnv = "vmPrefPlayDateAndTimeInMsgEnv";
    public static final String A_vmPrefAutoPlayNewMsgs         = "vmPrefAutoPlayNewMsgs";
    public static final String A_vmPrefPromptLevel             = "vmPrefPromptLevel";
    public static final String A_vmPrefPlayCallerNameInMsgEnv  = "vmPrefPlayCallerNameInMsgEnv";
    public static final String A_vmPrefSkipPinEntry            = "vmPrefSkipPinEntry";
    public static final String A_vmPrefUserLocale              = "vmPrefUserLocale";
    public static final String A_vmPrefAnsweringLocale         = "vmPrefAnsweringLocale";

    public static final String A_vmPrefGreetingType            = "vmPrefGreetingType";
    public static final String A_vmPrefEmailNotifStatus        = "vmPrefEmailNotifStatus";
    public static final String A_vmPrefPlayTutorial            = "vmPrefPlayTutorial";
    public static final String A_zimbraPrefVoiceItemsPerPage   = "zimbraPrefVoiceItemsPerPage";	

    public static final String A_vmPrefEmailNotifTrans         = "vmPrefEmailNotifTrans";
    public static final String A_vmPrefEmailNotifAttach        = "vmPrefEmailNotifAttach";

    public static final int FID_VOICEMAILINBOX = 1024;
    public static final int FID_MISSEDCALLS = 1025;
    public static final int FID_ANSWEREDCALLS = 1026;
    public static final int FID_PLACEDCALLS = 1027;
    public static final int FID_TRASH = 1028;

    public static final String FNAME_VOICEMAILINBOX = "Voicemail Inbox";
    public static final String FNAME_MISSEDCALLS = "Missed Calls";
    public static final String FNAME_ANSWEREDCALLS = "Answered Calls";
    public static final String FNAME_PLACEDCALLS = "Placed Calls";
    public static final String FNAME_TRASH = "Trash";
 }
