package com.zimbra.cs.ml;
/**
 *
 * @author iraykin
 *
 */
public class DummyClassifierResult implements ClassifierResult {
	private Label label;

	public DummyClassifierResult(Label label) {
		this.label = label;
	}
	@Override
	public Label getLabel() {
		return label;
	}

	@Override
	public String getReason() {
		return null;
	}

}
