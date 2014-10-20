package com.zimbra.cs.ml;
/**
 *
 * @author iraykin
 *
 */
public enum Label {
	PRIORITY(1), NOT_PRIORITY(0), UNCLASSIFIED(-1);

	private Integer id;

	private Label(Integer id) {
		this.id = id;
	}

	public Integer getId() {
		return id;
	}

	public static Label fromId(Integer id) {
		for (Label label: Label.values()) {
			if (label.getId() == id) {
				return label;
			}
		}
		return null;
	}

	public static Label fromString(String s) {
		for (Label label: Label.values()) {
			if (label.toString().equalsIgnoreCase(s)) {
				return label;
			}
		}
		return null;
	}
}
