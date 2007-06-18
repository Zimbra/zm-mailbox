package com.zimbra.cs.zclient;

import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.cs.account.Provisioning;

import java.util.Map;
import java.util.HashMap;
import java.util.List;

public class ZSignature {

    private String mName;
    private String mId;
    private Map<String, Object> mAttrs;

    public ZSignature(Element e) throws ServiceException {
        mName = e.getAttribute(AccountConstants.A_NAME);
        mId = e.getAttribute(AccountConstants.A_ID, null);
        mAttrs = new HashMap<String, Object>();
        for (Element a : e.listElements(AccountConstants.E_A)) {
            StringUtil.addToMultiMap(mAttrs, a.getAttribute(AccountConstants.A_NAME), a.getText());
        }
    }

    public ZSignature(String name, Map<String, Object> attrs) {
        mName = name;
        mAttrs = attrs;
        mId = get(Provisioning.A_zimbraPrefSignatureId);
    }

    public String getName() {
        return mName;
    }

    public String getId() {
        return mId;
    }

    /**
     * @param name name of pref to get
     * @return null if unset, or first value in list
     */
    public String get(String name) {
        Object value = mAttrs.get(name);
        if (value == null) {
            return null;
        } else if (value instanceof String[]) {
            return ((String[])value)[0];
        } else if (value instanceof List) {
            return (String) ((List)value).get(0);
        } else {
            return value.toString();
        }
    }

    public Map<String, Object> getAttrs() {
        return new HashMap<String, Object>(mAttrs);
    }

    public boolean getBool(String name) {
        return Provisioning.TRUE.equals(get(name));
    }

    public String getSignature() { return get(Provisioning.A_zimbraPrefMailSignature); }

    public Element toElement(Element parent) {
        Element sig = parent.addElement(AccountConstants.E_SIGNATURE);
        sig.addAttribute(AccountConstants.A_NAME, mName);
        if (mId != null) sig.addAttribute(AccountConstants.A_ID, mId);
        for (Map.Entry<String,Object> entry : mAttrs.entrySet()) {
            if (entry.getValue() instanceof String[]) {
                String[] values = (String[]) entry.getValue();
                for (String value : values) {
                    Element a = sig.addElement(AccountConstants.E_A);
                    a.addAttribute(AccountConstants.A_NAME, entry.getKey());
                    a.setText(value);
                }
            } else {
                Element a = sig.addElement(AccountConstants.E_A);
                a.addAttribute(AccountConstants.A_NAME, entry.getKey());
                a.setText(entry.getValue().toString());
            }
        }
        return sig;
    }

    public String toString() {
        ZSoapSB sb = new ZSoapSB();
        sb.beginStruct();
        sb.add("name", mName);
        sb.add("id", mId);
        sb.beginStruct("attrs");
        for (Map.Entry<String, Object> entry : mAttrs.entrySet()) {
            if (entry.getValue() instanceof String[]) {
                String[] values = (String[]) entry.getValue();
                sb.add(entry.getKey(), values, false, true);
            } else {
                sb.add(entry.getKey(), entry.getValue().toString());
            }
        }
        sb.endStruct();
        sb.endStruct();
        return sb.toString();
    }

}
