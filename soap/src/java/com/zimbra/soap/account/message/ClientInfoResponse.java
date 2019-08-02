/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2019 Synacor, Inc.
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
package com.zimbra.soap.account.message;

import com.zimbra.common.soap.AccountConstants;
import com.zimbra.soap.json.jackson.annotate.ZimbraJsonAttribute;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @zm-api-response-description Provides a limited amount of information the client may require about the requested hostname.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AccountConstants.E_CLIENT_INFO_RESPONSE)
public class ClientInfoResponse {

    public ClientInfoResponse () {

    }

    /**
     * @zm-api-field-tag client-info-skin-logo-app-banner
     * @zm-api-field-description Value of the zimbraSkinLogoAppBanner attribute on the domain
     */
    @XmlElement(name=AccountConstants.E_SKIN_LOGO_APP_BANNER /* zimbraSkinLogoAppBanner */, required=false)
    @ZimbraJsonAttribute
    private String SkinLogoAppBanner;

    public String getSkinLogoAppBanner() {
        return SkinLogoAppBanner;
    }

    public void setSkinLogoAppBanner(String skinLogoAppBanner) {
        SkinLogoAppBanner = skinLogoAppBanner;
    }

    /**
     * @zm-api-field-tag client-info-skin-logo-login-banner
     * @zm-api-field-description Value of the zimbraSkinLogoLoginBanner attribute on the domain
     */
    @XmlElement(name=AccountConstants.E_SKIN_LOGO_LOGIN_BANNER /* zimbraSkinLogoLoginBanner */, required=false)
    @ZimbraJsonAttribute
    private String SkinLogoLoginBanner;

    public String getSkinLogoLoginBanner() {
        return SkinLogoLoginBanner;
    }

    public void setSkinLogoLoginBanner(String skinLogoLoginBanner) {
        SkinLogoLoginBanner = skinLogoLoginBanner;
    }

    /**
     * @zm-api-field-tag client-info-skin-logo-url
     * @zm-api-field-description Value of the zimbraSkinLogoURL attribute on the domain
     */
    @XmlElement(name=AccountConstants.E_SKIN_LOGO_URL /* zimbraSkinLogoURL */, required=false)
    @ZimbraJsonAttribute
    private String SkinLogoURL;

    public String getSkinLogoURL() {
        return SkinLogoURL;
    }

    public void setSkinLogoURL(String skinLogoURL) {
        SkinLogoURL = skinLogoURL;
    }

    /**
     * @zm-api-field-tag client-info-web-client-login-url
     * @zm-api-field-description Location the client should redirect if not logged in. Value of the zimbraWebClientLoginURL attribute on the domain
     */
    @XmlElement(name=AccountConstants.E_WEB_CLIENT_LOGIN_URL /* zimbraWebClientLoginURL */, required=false)
    @ZimbraJsonAttribute
    private String WebClientLoginURL;

    public String getWebClientLoginURL() {
        return WebClientLoginURL;
    }

    public void setWebClientLoginURL(String webClientLoginURL) {
        WebClientLoginURL = webClientLoginURL;
    }

    /**
     * @zm-api-field-tag client-info-web-client-logout-url
     * @zm-api-field-description Location the client should redirect to after logging out. Value of the zimbraWebClientLogoutURL attribute on the domain
     */
    @XmlElement(name=AccountConstants.E_WEB_CLIENT_LOGOUT_URL /* zimbraWebClientLogoutURL */, required=false)
    @ZimbraJsonAttribute
    private String WebClientLogoutURL;

    public String getWebClientLogoutURL() {
        return WebClientLogoutURL;
    }

    public void setWebClientLogoutURL(String webClientLogoutURL) {
        WebClientLogoutURL = webClientLogoutURL;
    }

    /**
     * @zm-api-field-tag client-info-web-client-stay-signed-in-disabled
     * @zm-api-field-description Whether or not the webclient should disable 'stay signed in' functionality. Value of the zimbraWebClientStaySignedInDisabled attribute on the domain
     */
    @XmlElement(name=AccountConstants.E_WEB_CLIENT_STAY_SIGNED_IN_DISABLED /* zimbraWebClientStaySignedInDisabled */, required=false)
    @ZimbraJsonAttribute
    private boolean WebClientStaySignedInDisabled;


    public void setWebClientStaySignedInDisabled(boolean webClientStaySignedInDisabled) {
        WebClientStaySignedInDisabled = webClientStaySignedInDisabled;
    }

    /**
     * @zm-api-field-tag client-info-web-client-skin-background-color
     * @zm-api-field-description Value of the zimbraSkinBackgroundColor attribute on the domain
     */
    @XmlElement(name=AccountConstants.E_SKIN_BACKGROUND_COLOR /* zimbraSkinBackgroundColor */, required=false)
    @ZimbraJsonAttribute
    private String SkinBackgroundColor;

    public void setSkinBackgroundColor(String skinBackgroundColor) {
        SkinBackgroundColor = skinBackgroundColor;
    }

    /**
     * @zm-api-field-tag client-info-web-client-skin-foreground-color
     * @zm-api-field-description Value of the zimbraSkinForegroundColor attribute on the domain
     */
    @XmlElement(name=AccountConstants.E_SKIN_FOREGROUND_COLOR /* zimbraSkinForegroundColor */, required=false)
    @ZimbraJsonAttribute
    private String SkinForegroundColor;

    public void setSkinForegroundColor(String color) { this.SkinForegroundColor = color; }
    /**
     * @zm-api-field-tag client-info-web-client-skin-secondary-color
     * @zm-api-field-description Value of the zimbraSkinSecondaryColor attribute on the domain
     */
    @XmlElement(name=AccountConstants.E_SKIN_SECONDARY_COLOR /* zimbraSkinSecondaryColor */, required=false)
    @ZimbraJsonAttribute
    private String SkinSecondaryColor;

    public void setSkinSecondaryColor(String color) { this.SkinSecondaryColor = color; }

    /**
     * @zm-api-field-tag client-info-web-client-skin-selection-color
     * @zm-api-field-description Value of the zimbraSkinSelection-Color attribute on the domain
     */
    @XmlElement(name=AccountConstants.E_SKIN_SELECTION_COLOR /* zimbraSkinSelectionColor */, required=false)
    @ZimbraJsonAttribute
    private String SkinSelectionColor;

    public void setSkinSelectionColor(String color) { this.SkinSelectionColor = color; }

    /**
     * @zm-api-field-tag client-info-web-client-skin-favicon
     * @zm-api-field-description Value of the zimbraSkinFavicon attribute on the domain
     */
    @XmlElement(name=AccountConstants.E_SKIN_FAVICON /* zimbraSkinFavicon */, required=false)
    @ZimbraJsonAttribute
    private String SkinFavicon;

    public void setSkinFavicon(String favicon) { this.SkinFavicon = favicon; }

    /**
     * @zm-api-field-tag client-info-web-client-feature-password-status
     * @zm-api-field-description Value of the zimbraFeatureResetPasswordStatus attribute on the domain
     */
    @XmlElement(name=AccountConstants.E_WEB_CLIENT_FEATURE_PASSWORD_RESET /* zimbraFeatureResetPasswordStatus */, required=false)
    @ZimbraJsonAttribute
    private String FeatureResetPasswordStatus;

    public void setFeatureResetPasswordStatus(String status) { this.FeatureResetPasswordStatus = status; }

}
