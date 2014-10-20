package com.zimbra.cs.ml;

import java.util.LinkedList;
import java.util.List;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Pair;
import com.zimbra.cs.analytics.BehaviorSummary;
import com.zimbra.cs.analytics.MessageBehaviorSet;

/** Class that analyzes a MessageBehaviorSet to determine if it represents a message that should be 
 * added as a training item for either Priority or Not Priority
 * @author iraykin
 *
 */
public class RuleBasedClassifier implements BehaviorClassifier {

	private List<Pair<BehaviorRule, Precedence>> rules = new LinkedList<Pair<BehaviorRule, Precedence>>();
	
	public enum Precedence {
		NORMAL, FINAL;
	}

	public RuleBasedClassifier() {
	}
	
	public void addNextRule(BehaviorRule rule, Precedence precedence) {
		rules.add(new Pair<BehaviorRule, Precedence>(rule, precedence));
	}
	
	@Override
	public InternalLabel classify(MessageBehaviorSet behaviors) throws ServiceException {
		if (rules.isEmpty()) {
			throw ServiceException.FAILURE("no behavior rules specified", new Throwable());
		}
		BehaviorSummary summary = BehaviorSummary.fromAllBehaviors(behaviors);
		InternalLabel curLabel = InternalLabel.UNCLASSIFIED;
		for (Pair<BehaviorRule, Precedence> pair: rules) {
			BehaviorRule rule = pair.getFirst();
			Precedence precedence = pair.getSecond();
			InternalLabel label = rule.classify(summary);
			if (label != InternalLabel.UNCLASSIFIED) {
				if (precedence == Precedence.FINAL) {
					return label;
				} else {
					curLabel = label;
				}
			}
		}
		return curLabel;
	}
}
