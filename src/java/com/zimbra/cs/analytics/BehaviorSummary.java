package com.zimbra.cs.analytics;

import java.util.HashMap;
import java.util.Map;

import com.zimbra.cs.analytics.MessageBehavior.BehaviorType;

/** A reduced set of behaviors for a message, representing
 * key actions taken
 * @author iraykin
 *
 */
public class BehaviorSummary {
	private Integer msgId;

	private MessageBehavior spammed;
	private MessageBehavior deleted;

	private Map<BehaviorType, MessageBehavior> firstBehaviorByType = new HashMap<BehaviorType, MessageBehavior>();
	private Map<BehaviorType, MessageBehavior> lastBehaviorByType = new HashMap<BehaviorType, MessageBehavior>();
	private Map<BehaviorType, Integer> occurByType = new HashMap<BehaviorType, Integer>();

	public BehaviorSummary(Integer messageId) {
		this.msgId = messageId;
	}

	public static BehaviorSummary fromAllBehaviors(MessageBehaviorSet behaviorSet) {
		BehaviorSummary summary = new BehaviorSummary(behaviorSet.getMessageId());

		boolean firstSeen = true;
		boolean firstRead = true;
		boolean firstReplied = true;
		boolean firstContent = true;

		MessageBehavior lastSeen = null;
		MessageBehavior lastRead = null;
		MessageBehavior lastReplied = null;
		MessageBehavior lastInteracted = null;

		for (MessageBehavior behavior: behaviorSet.getBehaviors()) {
			BehaviorType type = behavior.getType();
			switch (type) {
			case SEEN:
				lastSeen = behavior;
				if (firstSeen) {
					summary.setFirstBehavior(behavior);
					summary.incrementByType(type);
					firstSeen = false;
				}
				break;
			case READ:
				lastRead = behavior;
				if (firstRead) {
					summary.setFirstBehavior(behavior);
					summary.incrementByType(type);
					firstRead = false;
				}
				break;
			case REPLIED:
				lastReplied = behavior;
				if (firstReplied) {
					summary.incrementByType(type);
					summary.incrementByType(type);
					firstReplied = false;
				}
				break;
			case SPAMMED:
				summary.setSpammed(behavior);
				summary.setFirstBehavior(behavior);
				summary.incrementByType(type);
				break;
			case DELETED:
				summary.setDeleted(behavior);
				summary.setFirstBehavior(behavior);
				summary.incrementByType(type);
				break;
			case FORWARDED:
			    break; //not sure how this is important yet
			case CONTENT:
				lastInteracted = behavior;
				if (firstContent) {
					summary.setFirstBehavior(behavior);
					summary.incrementByType(type);
				}
				break;
			case RECIEVED:
			    break;
			    default:
			        break;
			}
		}

		summary.setLastBehavior(lastSeen);
		summary.setLastBehavior(lastRead);
		summary.setLastBehavior(lastReplied);
		summary.setLastBehavior(lastInteracted);

		return summary;
	}

	public MessageBehavior getDeleted() {
		return deleted;
	}

	private void setDeleted(MessageBehavior deleted) {
		this.deleted = deleted;
	}

	public MessageBehavior getSpammed() {
		return spammed;
	}

	public void setSpammed(MessageBehavior spammed) {
		this.spammed = spammed;
	}

	public Integer getMsgId() {
		return msgId;
	}

	public Integer getOccurByType(BehaviorType type) {
		if (occurByType.containsKey(type)) {
			return occurByType.get(type);
		} else {
			return 0;
		}
	}
	private void incrementByType(BehaviorType type) {
		if (occurByType.containsKey(type)) {
			occurByType.put(type, occurByType.get(type) + 1);
		} else {
			occurByType.put(type, 1);
		}
	}

	private void updateBehaviorMap(Map<BehaviorType, MessageBehavior> map, MessageBehavior behavior) {
		if (behavior == null) {return; }
		map.put(behavior.getType(), behavior);
	}

	public void setFirstBehavior(MessageBehavior behavior) {
		updateBehaviorMap(firstBehaviorByType, behavior);
	}

	public void setLastBehavior(MessageBehavior behavior) {
		updateBehaviorMap(lastBehaviorByType, behavior);
	}

	public MessageBehavior getFirstBehaviorByType(BehaviorType type) {
		if (firstBehaviorByType.containsKey(type)) {
			return firstBehaviorByType.get(type);
		} else {
			return null;
		}
	}

	public MessageBehavior getLastBehaviorByType(BehaviorType type) {
		if (lastBehaviorByType.containsKey(type)) {
			return lastBehaviorByType.get(type);
		} else {
			return null;
		}
	}
}
