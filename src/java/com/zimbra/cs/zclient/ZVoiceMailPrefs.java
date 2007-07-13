package com.zimbra.cs.zclient;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.VoiceConstants;
import com.zimbra.cs.account.Provisioning;

import java.util.HashMap;

public class ZVoiceMailPrefs extends ZCallFeature {
    private HashMap<String, String> mMap;

	public ZVoiceMailPrefs(String name) {
        super(name);
        mMap = new HashMap<String, String>();
    }

    public String getEmailNotificationAddress() {
        return this.get(VoiceConstants.A_vmPrefEmailNotifAddress);
    }

    public void setEmailNotificationAddress(String address) {
        mMap.put(VoiceConstants.A_vmPrefEmailNotifAddress, address);
    }

    public long getNumberPerPage() {
        return this.getLong(Provisioning.A_zimbraPrefVoiceItemsPerPage);
    }

    public void setNumberPerPage(long number) {
        mMap.put(Provisioning.A_zimbraPrefVoiceItemsPerPage, Long.toString(number));
    }

    public String get(String key) {
        return mMap.get(key);
    }

	public long getLong(String name) {
		String v = get(name);
		try {
			return v == null ? -1 : Long.parseLong(v);
		} catch (NumberFormatException e) {
			return -1;
		}
	}

    public synchronized void assignFrom(ZCallFeature that) {
        super.assignFrom(that);
        if (that instanceof ZVoiceMailPrefs) {
            HashMap<String, String> thatMap = ((ZVoiceMailPrefs) that).mMap;
            for (String name : thatMap.keySet()) {
                String value = thatMap.get(name);
                mMap.put(name, value);
            }
        }
    }

    synchronized void fromElement(Element element) throws ServiceException {
        super.fromElement(element);
        for (Element prefElement : element.listElements(VoiceConstants.E_PREF)) {
            String name = prefElement.getAttribute(MailConstants.A_NAME);
            String value = prefElement.getText();
            mMap.put(name, value);
        }
    }

    void toElement(Element element) throws ServiceException {
        super.toElement(element);
        for (String name : mMap.keySet()) {
            String value = mMap.get(name);
            Element prefElement = element.addElement(VoiceConstants.E_PREF);
            prefElement.addAttribute(MailConstants.A_NAME, name);
            prefElement.setText(value);
        }
    }
}
