package com.zimbra.cs.ml;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.analytics.MessageBehaviorSet;
/**
 *
 * @author iraykin
 *
 */
public interface BehaviorClassifier {

	public InternalLabel classify(MessageBehaviorSet behaviors) throws ServiceException;
}
