package com.zimbra.soap.type;

public interface OAuthDataSource extends DataSource {
    public void setRefreshToken(String refreshToken);
    public String getRefreshToken();

    public void setRefreshTokenUrl(String refreshTokenUrl);
    public String getRefreshTokenUrl();

}
