/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010 Zimbra, Inc.
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

    public static final QName GET_VOICE_MAIL_PREFS_REQUEST  = QName.get("GetVoiceMailPrefsRequest", NAMESPACE);
    public static final QName GET_VOICE_MAIL_PREFS_RESPONSE = QName.get("GetVoiceMailPrefsResponse", NAMESPACE);

    public static final QName MODIFY_VOICE_MAIL_PREFS_REQUEST  = QName.get("ModifyVoiceMailPrefsRequest", NAMESPACE);
    public static final QName MODIFY_VOICE_MAIL_PREFS_RESPONSE = QName.get("ModifyVoiceMailPrefsResponse", NAMESPACE);

    public static final QName MODIFY_VOICE_MAIL_PIN_REQUEST  = QName.get("ModifyVoiceMailPinRequest", NAMESPACE);
    public static final QName MODIFY_VOICE_MAIL_PIN_RESPONSE = QName.get("ModifyVoiceMailPinResponse", NAMESPACE);

    public static final QName MODIFY_FROM_NUM_REQUEST  = QName.get("ModifyFromNumRequest", NAMESPACE);
    public static final QName MODIFY_FROM_NUM_RESPONSE = QName.get("ModifyFromNumResponse", NAMESPACE);

    public static final QName GET_VOICE_INFO_REQUEST  = QName.get("GetVoiceInfoRequest", NAMESPACE);
    public static final QName GET_VOICE_INFO_RESPONSE = QName.get("GetVoiceInfoResponse", NAMESPACE);

    public static final QName GET_VOICE_FOLDER_REQUEST  = QName.get("GetVoiceFolderRequest", NAMESPACE);
    public static final QName GET_VOICE_FOLDER_RESPONSE = QName.get("GetVoiceFolderResponse", NAMESPACE);
    
    public static final QName SEARCH_VOICE_REQUEST = QName.get("SearchVoiceRequest", NAMESPACE);
    public static final QName SEARCH_VOICE_RESPONSE = QName.get("SearchVoiceResponse", NAMESPACE);

    public static final QName GET_VOICE_MSG_REQUEST  = QName.get("GetVoiceMsgRequest", NAMESPACE);
    public static final QName GET_VOICE_MSG_RESPONSE = QName.get("GetVoiceMsgResponse", NAMESPACE);

    public static final QName VOICE_MSG_ACTION_REQUEST  = QName.get("VoiceMsgActionRequest", NAMESPACE);
    public static final QName VOICE_MSG_ACTION_RESPONSE = QName.get("VoiceMsgActionResponse", NAMESPACE);

    public static final QName GET_VOICE_FEATURES_REQUEST  = QName.get("GetVoiceFeaturesRequest", NAMESPACE);
    public static final QName GET_VOICE_FEATURES_RESPONSE = QName.get("GetVoiceFeaturesResponse", NAMESPACE);

    public static final QName MODIFY_VOICE_FEATURES_REQUEST  = QName.get("ModifyVoiceFeaturesRequest", NAMESPACE);
    public static final QName MODIFY_VOICE_FEATURES_RESPONSE = QName.get("ModifyVoiceFeaturesResponse", NAMESPACE);

    public static final QName RESET_VOICE_FEATURES_REQUEST  = QName.get("ResetVoiceFeaturesRequest", NAMESPACE);
    public static final QName RESET_VOICE_FEATURES_RESPONSE = QName.get("ResetVoiceFeaturesResponse", NAMESPACE);

    public static final QName UPLOAD_VOICE_MAIL_REQUEST  = QName.get("UploadVoiceMailRequest", NAMESPACE);
    public static final QName UPLOAD_VOICE_MAIL_RESPONSE = QName.get("UploadVoiceMailResponse", NAMESPACE);

    
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
    public static final String A_NUM_PUBLISHABLE = "numpublishable";

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
