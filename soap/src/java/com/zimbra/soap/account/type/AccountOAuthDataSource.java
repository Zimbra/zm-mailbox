package com.zimbra.soap.account.type;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.type.OAuthDataSource;
@XmlType(propOrder = {})
public class AccountOAuthDataSource extends AccountDataSource implements OAuthDataSource {
    public AccountOAuthDataSource() {
    }

    public AccountOAuthDataSource(OAuthDataSource data) {
        super(data);
        refreshToken = data.getRefreshToken();
        refreshTokenUrl = data.getRefreshTokenUrl();
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
