package com.zimbra.client;

import org.json.JSONException;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.soap.admin.type.DataSourceType;
import com.zimbra.soap.mail.type.DataSourceNameOrId;
import com.zimbra.soap.mail.type.MailOAuthDataSource;
import com.zimbra.soap.mail.type.OAuthDataSourceNameOrId;
import com.zimbra.soap.type.DataSource;
import com.zimbra.soap.type.DataSources;
import com.zimbra.soap.type.OAuthDataSource;

public class ZOAuthDataSource extends ZDataSource implements ToZJSONObject {
    public ZOAuthDataSource(OAuthDataSource data) {
        this.data = DataSources.newOAuthDataSource(data);
    }

    public ZOAuthDataSource(String name, boolean enabled, String refreshToken, String refreshTokenURL, String folderId, String importClass, boolean isImportOnly)
    throws ServiceException {
        data = DataSources.newOAuthDataSource();
        data.setName(name);
        data.setEnabled(enabled);
        ((OAuthDataSource)data).setRefreshToken(refreshToken);
        ((OAuthDataSource)data).setRefreshTokenUrl(refreshTokenURL);
        data.setImportClass(importClass);
        try {
            data.setFolderId(folderId);
        } catch (NumberFormatException e) {
            ZimbraLog.datasource.error("Cannot create ZOAuthDataSource with name %s and import class %s, because folder ID is invalid: %s", name, importClass, folderId);
            throw ServiceException.INVALID_REQUEST("Invalid folder id", e);
        }
        data.setImportOnly(isImportOnly);
    }

    private OAuthDataSource getData() {
        return ((OAuthDataSource)data);
    }

    @Override
    public DataSource toJaxb() {
        MailOAuthDataSource jaxbObject = new MailOAuthDataSource();
        jaxbObject.setId(data.getId());
        jaxbObject.setName(data.getName());
        jaxbObject.setRefreshToken(getData().getRefreshToken());
        jaxbObject.setRefreshTokenUrl(getData().getRefreshTokenUrl());
        jaxbObject.setFolderId(data.getFolderId());
        jaxbObject.setImportOnly(data.isImportOnly());
        jaxbObject.setImportClass(data.getImportClass());
        jaxbObject.setEnabled(data.isEnabled());
        return jaxbObject;
    }

    @Override
    public DataSourceNameOrId toJaxbNameOrId() {
        OAuthDataSourceNameOrId jaxbObject = OAuthDataSourceNameOrId.createForId(data.getId());
        return jaxbObject;
    }

    @Override
    public DataSourceType getType() { return DataSourceType.oauth; }

    @Override
    public ZJSONObject toZJSONObject() throws JSONException {
        ZJSONObject zjo = new ZJSONObject();
        zjo.put("id", data.getId());
        zjo.put("name", data.getName());
        zjo.put("enabled", data.isEnabled());
        zjo.put("refreshToken", getData().getRefreshToken());
        zjo.put("refreshTokenUrl", getData().getRefreshTokenUrl());
        zjo.put("importClass", getData().getImportClass());
        zjo.put("folderId", data.getFolderId());
        zjo.put("importOnly", data.isImportOnly());
        return zjo;
    }
    public String getFolderId() { return data.getFolderId(); }
    public ZOAuthDataSource setFolderId(String folderid) {
        data.setFolderId(folderid);
        return this;
    }
    
    public ZOAuthDataSource setRefreshToken(String val) {
        getData().setRefreshToken(val);
        return this;
    }

    public ZOAuthDataSource setRefreshTokenURL(String val) {
        getData().setRefreshTokenUrl(val);
        return this;
    }

    public String getRefreshToken() {
        return getData().getRefreshToken();
    }

    public String getRefreshTokenUrl() {
        return getData().getRefreshTokenUrl();
    }

    public String getImportClass() {
        return data.getImportClass();
    }
    public void setImportClass(String importClass) {
        data.setImportClass(importClass);
    }
}
