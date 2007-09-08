package com.zimbra.cs.zclient;

import java.util.HashMap;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Provisioning;

public class ZSignature implements Comparable {

    private String mName;
    private String mId;
    private String mValue;

    public ZSignature(Element e) throws ServiceException {
        mName = e.getAttribute(AccountConstants.A_NAME);
        mId = e.getAttribute(AccountConstants.A_ID, null);
        
        for (Element c : e.listElements(AccountConstants.E_CONTENT)) {
            String type = c.getAttribute(AccountConstants.A_TYPE, null);
            if (type.equals("text/plain"))
                mValue = c.getText();
        }
    }

    public ZSignature(String id, String name, String value) {
        mId = id;
        mName = name;
        mValue = value;
    }

    public ZSignature(String name, String value) {
        mName = name;
        mValue = value;
    }

    public String getName() {
        return mName;
    }

    public String getId() {
        return mId;
    }

    public String getValue() { return mValue; }
    
    public Map<String, Object> getAttrs() {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraSignatureId, mId);
        attrs.put(Provisioning.A_zimbraSignatureName, mName);
        attrs.put(Provisioning.A_zimbraPrefMailSignature, mValue);
        return attrs;
    }
    
    public Element toElement(Element parent) {
        Element sig = parent.addElement(AccountConstants.E_SIGNATURE);
        sig.addAttribute(AccountConstants.A_NAME, mName);
        if (mId != null) sig.addAttribute(AccountConstants.A_ID, mId);
        if (mName != null) sig.addAttribute(AccountConstants.A_NAME, mName);
        if (mValue != null) {
            Element content = sig.addElement(AccountConstants.E_CONTENT);
            content.addAttribute(AccountConstants.A_TYPE, "text/plain");
            content.setText(mValue);
        }
        return sig;
    }

    public String toString() {
        ZSoapSB sb = new ZSoapSB();
        sb.beginStruct();
        sb.add("name", mName);
        sb.add("id", mId);
        sb.add("value", mValue);
        sb.endStruct();
        return sb.toString();
    }

    public int compareTo(Object obj) {
        if (!(obj instanceof ZSignature))
            return 0;
        ZSignature other = (ZSignature) obj;
        return getName().compareTo(other.getName());
    }
}
