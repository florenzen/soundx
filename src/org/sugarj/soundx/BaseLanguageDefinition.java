package org.sugarj.soundx;

import org.sugarj.AbstractBaseLanguage;

public class BaseLanguageDefinition {
	private String bldFilename;

	private SoundXAbstractBaseLanguage abstractBaseLanguage;

	public BaseLanguageDefinition(String bldFilename) {
		this.bldFilename = bldFilename;
		process();
	}

	public AbstractBaseLanguage getAbstractBaseLanguage() {
		return abstractBaseLanguage;
	}
	
	private void process() {}
}
