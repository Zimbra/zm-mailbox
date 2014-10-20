package com.zimbra.cs.ml;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedLongSynchronizer.ConditionObject;

import com.zimbra.cs.analytics.MessageBehavior.BehaviorType;
import com.zimbra.cs.ml.BehaviorRule.ConditionOccurrence;
import com.zimbra.cs.ml.BehaviorRule.TriggerOccurrence;

/** Rules to be used to classify behaviors can be defined here for convenience
 * 
 * @author iraykin
 *
 */
public class BehaviorRules {
	
	public static final BehaviorRule PRIORITY_WHEN_REPLYING_QUICKLY = new BehaviorRule()
	.setDescription("quick reply")
	.setLabel(InternalLabel.PRIORITY_REPLIED)
	.setCondition(BehaviorType.READ)
	.setTrigger(BehaviorType.REPLIED)
	.setMaxTimeBetween(4, TimeUnit.HOURS, TriggerOccurrence.EARLIEST, ConditionOccurrence.EARLIEST);
	
	public static final BehaviorRule PRIORITY_WHEN_INTERACTING_WITH_CONTENT = new BehaviorRule()
	.setDescription("interacted with content")
	.setLabel(InternalLabel.PRIORITY_CONTENT)
	.setCondition(BehaviorType.READ)
	.setTrigger(BehaviorType.CONTENT)
	.setMaxTimeBetween(4, TimeUnit.HOURS, TriggerOccurrence.EARLIEST, ConditionOccurrence.EARLIEST);
	
	public static final BehaviorRule NOT_PRIORITY_WHEN_DELETING_QUICKLY = new BehaviorRule()
	.setDescription("deleted soon after reading")
	.setLabel(InternalLabel.NOT_PRIORITY)
	.setCondition(BehaviorType.READ)
	.setTrigger(BehaviorType.DELETED)
	.setMaxTimeBetween(4, TimeUnit.HOURS, TriggerOccurrence.EARLIEST, ConditionOccurrence.EARLIEST);
	
	public static final BehaviorRule NOT_PRIORITY_WHEN_FLAGGING_AS_SPAM = new BehaviorRule()
	.setDescription("flagged as spam")
	.setLabel(InternalLabel.NOT_PRIORITY)
	.setTrigger(BehaviorType.SPAMMED);
	
	public static final BehaviorRule NOT_PRIORITY_WHEN_DELAYED_READ = new BehaviorRule()
	.setDescription("did not read for a while")
	.setLabel(InternalLabel.NOT_PRIORITY)
	.setCondition(BehaviorType.SEEN)
	.setTrigger(BehaviorType.READ)
	.setMinTimeBetween(1, TimeUnit.DAYS, TriggerOccurrence.EARLIEST, ConditionOccurrence.EARLIEST);
}
