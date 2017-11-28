package com.zimbra.soap.mail.type;

import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.soap.MailConstants;

public class TestDataSource {
    /**
     * @zm-api-field-tag data-source-success
     * @zm-api-field-description 0 if data source test failed, 1 if test succeeded
     */
    @XmlAttribute(name = MailConstants.A_DS_SUCCESS /* success */, required = true)
    private int success;

    /**
     * @zm-api-field-tag data-source-error
     * @zm-api-field-description error message passed by DatImport::test method of the datasource being tested
     */
    @XmlAttribute(name = MailConstants.A_DS_ERROR /* error */, required = false)
    private String error;

    public TestDataSource() {
        this.success = 1;
        this.error = null;
    }

    public TestDataSource(String error) {
        if(error != null && !error.isEmpty()) {
            success = 0;
        }
        this.error = error;
    }

    public TestDataSource(int success, String error) {
        this.success = success;
        this.error = error;
    }

    public void setSuccess(int success) { this.success = success; }
    public int getSuccess() { return success; }

    public void setError(String error) { this.error = error; }
    public String getError() { return this.error; }
}
