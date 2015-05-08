package org.sugarj.soundx;

public class SXNamespacePrefixed extends SXNamespaceKind {
	private char separator;

	public SXNamespacePrefixed(char separator) {
		this.separator = separator;
	}

	public char getSeparator() {
		return separator;
	}	
}
