package org.sugarj.soundx;

public class SXNamespaceNested extends SXNamespaceKind {
	private char separator;

	public SXNamespaceNested(char separator) {
		this.separator = separator;
	}

	public char getSeparator() {
		return separator;
	}	
}
