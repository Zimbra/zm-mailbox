package com.zimbra.soap.mail.type;

import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.type.OAuthDataSource;

public class MailOAuthDataSource extends MailDataSource implements OAuthDataSource {
    public MailOAuthDataSource() {
    }

    public MailOAuthDataSource(OAuthDataSource data) {
        super(data);
    }

    /**
     * @zm-api-field-tag data-source-refreshToken
     * @zm-api-field-description refresh token for refreshing data source oauth token
     */
    @XmlAttribute(name = MailConstants.A_DS_REFRESH_TOKEN /* refreshToken */, required = false)
    private String refreshToken;

    /**
     * @zm-api-field-tag data-source-refreshTokenUrl
     * @zm-api-field-description refreshTokenUrl for refreshing data source oauth token
     */
    @XmlAttribute(name = MailConstants.A_DS_REFRESH_TOKEN_URL /* refreshTokenUrl */, required = false)
    private String refreshTokenUrl;

    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
    public String getRefreshToken() { return refreshToken; }

    public void setRefreshTokenUrl(String refreshTokenUrl) { this.refreshTokenUrl = refreshTokenUrl; }
    public String getRefreshTokenUrl() { return refreshTokenUrl; }
}
