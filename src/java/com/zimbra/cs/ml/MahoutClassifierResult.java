package com.zimbra.cs.ml;

import org.apache.mahout.math.Vector;

import com.google.common.base.Objects;

/**A Wrapper around the vector output from the CrossFoldLearner.
 * Interprets the vector data into a Label.
 * @author iraykin
 *
 */
public class MahoutClassifierResult implements ClassifierResult {
	private InternalLabel label;
	private double value;
	private String reason;
	
	public MahoutClassifierResult(Vector out) {
		value = out.maxValue();
		int maxValueIdx = out.maxValueIndex();
		this.label = InternalLabel.fromId(maxValueIdx);
	}

	@Override
	public Label getLabel() {
		return label.getExternalLabel();
	}
	
	public InternalLabel getInternalLabel() {
		return label;
	}
	
	public double getValue() {
		return value;
	}
	
	@Override
	public String toString() {
		return Objects.toStringHelper(this)
				.add("Classification", label.toString())
				.add("Value", String.format("%.2f", value))
				.add("Reason", reason).toString();
	}

	public void setReason(String reason) {
		this.reason = reason;
	}
	
	public String getReason() {
		return reason;
	}
}
