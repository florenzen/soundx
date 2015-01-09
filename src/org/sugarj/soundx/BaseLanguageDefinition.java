package org.sugarj.soundx;

import org.sugarj.common.Log;

public class BaseLanguageDefinition {
	private static BaseLanguageDefinition instance = new BaseLanguageDefinition();
		
	public static BaseLanguageDefinition getInstance() {
		return instance;
	}
	
	private BaseLanguageDefinition() { }

	
	public void process(String bldFilename) {
		
	}
}
