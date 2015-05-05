package org.sugarj.soundx;

public class SXNamespacePrefixed extends SXNamespaceKind {
	private String separator;

	public SXNamespacePrefixed(String separator) {
		this.separator = separator;
	}

	public String getSeparator() {
		return separator;
	}	
}
