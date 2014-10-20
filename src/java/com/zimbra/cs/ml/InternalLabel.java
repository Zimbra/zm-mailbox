package com.zimbra.cs.ml;
/**
 *
 * @author iraykin
 *
 */
public enum InternalLabel {
	UNCLASSIFIED(-1),
	NOT_PRIORITY(0),
	PRIORITY(1),
	PRIORITY_CONTENT(2),
	PRIORITY_REPLIED(3);

	private Integer id;

	private InternalLabel(Integer id) {
		this.id = id;
	}

	public Integer getId() {
		return id;
	}

	public static InternalLabel fromId(Integer id) {
		for (InternalLabel label: InternalLabel.values()) {
			if (label.getId() == id) {
				return label;
			}
		}
		return null;
	}

	public static InternalLabel fromString(String s) {
		for (InternalLabel label: InternalLabel.values()) {
			if (label.toString().equalsIgnoreCase(s)) {
				return label;
			}
		}
		return null;
	}

	public static Integer numDimensions() {
		return InternalLabel.values().length - 1; //UNCLASSIFIED is not really a dimension
	}

	public Label getExternalLabel() {
		if (this == InternalLabel.UNCLASSIFIED) {
			return Label.UNCLASSIFIED;
		} else if (this == InternalLabel.NOT_PRIORITY) {
			return Label.NOT_PRIORITY;
		} else {
			return Label.PRIORITY;
		}
	}
}
