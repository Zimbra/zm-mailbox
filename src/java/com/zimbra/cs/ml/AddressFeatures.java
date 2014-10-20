package com.zimbra.cs.ml;

import javax.mail.internet.InternetAddress;

import com.zimbra.common.service.ServiceException;

/**
 *
 * @author iraykin
 *
 */
public abstract class AddressFeatures implements FeatureSet {

	protected static void encodeAddrs(InternetAddress[] addrs, String featureName, Features features) throws ServiceException {
		for (int i = 0; i < addrs.length; i++) {
			InternetAddress addr = addrs[i];
			features.addTextFeature(featureName, addr.getAddress());
		}
	}
}
