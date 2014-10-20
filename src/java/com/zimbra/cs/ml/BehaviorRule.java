package com.zimbra.cs.ml;

import java.util.concurrent.TimeUnit;

import com.google.common.base.Objects;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.analytics.BehaviorSummary;
import com.zimbra.cs.analytics.MessageBehavior;
import com.zimbra.cs.analytics.MessageBehavior.BehaviorType;
import com.zimbra.cs.analytics.MessageBehaviorSet;

/** A rule that returns a Label if two conditions are met within a certain amount of time
 *
 * @author iraykin
 *
 */
public class BehaviorRule implements BehaviorClassifier {

	private BehaviorType triggerBehavior = null;
	private TriggerOccurrence triggerBehaviorOccurrence;
	private BehaviorType condition = null;
	private ConditionOccurrence conditionOccurrence;
	private Integer minOccur = -1;
	private Integer maxOccur = Integer.MAX_VALUE;
	private Integer minTimeBetween = null;
	private Integer maxTimeBetween = null;
	private TimeUnit minTimeUnit = null;
	private TimeUnit maxTimeUnit = null;
	private InternalLabel label = null;
	private String description = null;

	public enum TriggerOccurrence {
		EARLIEST, LATEST;
	}

	public enum ConditionOccurrence {
		EARLIEST, LATEST;
	}



	public BehaviorRule(InternalLabel label) {
		setLabel(label);
	}

	public BehaviorRule() {
	}

	public BehaviorRule setLabel(InternalLabel label) {
		this.label = label;
		return this;
	}

	public BehaviorRule setTrigger(BehaviorType behavior) {
		triggerBehavior = behavior;
		return this;
	}

	public BehaviorRule setCondition(BehaviorType behavior) {
		condition = behavior;
		return this;
	}

	public BehaviorRule setMinOccur(Integer minOccur) {
		this.minOccur = minOccur;
		return this;
	}

	public BehaviorRule setMaxOccur(Integer maxOccur) {
		this.maxOccur = maxOccur;
		return this;
	}

	public BehaviorRule setMinTimeBetween(Integer time, TimeUnit unit,
			TriggerOccurrence triggerOccurrence, ConditionOccurrence conditionOccurrence) {
		this.minTimeBetween = time;
		this.minTimeUnit = unit;
		this.triggerBehaviorOccurrence = triggerOccurrence;
		this.conditionOccurrence = conditionOccurrence;
		return this;
	}

	public BehaviorRule setMaxTimeBetween(Integer time, TimeUnit unit,
			TriggerOccurrence triggerOccurrence, ConditionOccurrence conditionOccurrence) {
		this.maxTimeBetween = time;
		this.maxTimeUnit = unit;
		this.triggerBehaviorOccurrence = triggerOccurrence;
		this.conditionOccurrence = conditionOccurrence;
		return this;
	}

	public BehaviorRule setDescription(String description) {
		this.description = description;
		return this;
	}

	@Override
	public InternalLabel classify(MessageBehaviorSet behaviors) throws ServiceException {
		BehaviorSummary summary = BehaviorSummary.fromAllBehaviors(behaviors);
		return classify(summary);
	}

	public InternalLabel classify(BehaviorSummary summary) throws ServiceException {
		MessageBehavior earliestTrigger = summary.getFirstBehaviorByType(triggerBehavior);
		if (earliestTrigger == null) {
			//hasn't happened
			return InternalLabel.UNCLASSIFIED;
		}

		Integer numOccured = summary.getOccurByType(triggerBehavior);
		if (numOccured < minOccur) {
			//hasn't happened enough times
			return InternalLabel.UNCLASSIFIED;
		}

		if (numOccured > maxOccur) {
			//happened too many times
			return InternalLabel.UNCLASSIFIED;
		}

		if (condition != null) {
			MessageBehavior latestTrigger = summary.getLastBehaviorByType(triggerBehavior);
			MessageBehavior earliestCondition = summary.getFirstBehaviorByType(condition);
			MessageBehavior latestCondition = summary.getLastBehaviorByType(condition);
			if (earliestCondition == null) {
				//precondition hasn't been met
				return InternalLabel.UNCLASSIFIED;
			}
			if (maxTimeBetween != null || minTimeBetween != null) { //time constraints
				MessageBehavior trigger = triggerBehaviorOccurrence == TriggerOccurrence.EARLIEST? earliestTrigger: latestTrigger;
				MessageBehavior condition = conditionOccurrence == ConditionOccurrence.EARLIEST? earliestCondition: latestCondition;
				long timeDelta = trigger.getTime() - condition.getTime();

				if (maxTimeBetween != null) {
					if (timeDelta > maxTimeUnit.toMillis(maxTimeBetween)) {
						//precondition happened too long ago
						return InternalLabel.UNCLASSIFIED;
					}
				}

				if (minTimeBetween != null) {
					if (timeDelta < minTimeUnit.toMillis(minTimeBetween)) {
						//precondition happened too recently
						return InternalLabel.UNCLASSIFIED;
					}
				}
			}
		}
		logSuccess(summary.getMsgId());
		return label;
	}

	private void logSuccess(Integer msgId) {
		String desc = description == null? toString(): description;
		ZimbraLog.analytics.info(String.format("Behaviors for message %s clasified as %s by rule \"%s\"",
				msgId, label.toString(), desc));
	}

	@Override
	public String toString() {
		Objects.ToStringHelper helper = Objects.toStringHelper(this)
				.add("trigger behavior", triggerBehavior.toString())
				.add("precondition", condition.toString())
				.add("target label", label);
		if (minOccur > 0) {
			helper.add("min occur", minOccur);
		}
		if (maxOccur < Integer.MAX_VALUE) {
			helper.add("max occur", maxOccur);
		}
		if (minTimeBetween != null) {
			helper.add("min time between", String.format("%s %s",minTimeBetween, minTimeUnit.toString().toLowerCase()));
		}
		if (maxTimeBetween != null) {
			helper.add("max time between", String.format("%s %s",maxTimeBetween, maxTimeUnit.toString().toLowerCase()));
		}
		if (description != null) {
			helper.add("description", description);
		}
		return helper.toString();
	}
}