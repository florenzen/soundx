package org.sugarj.soundx;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.sugarj.common.Log;
import org.sugarj.driver.Driver;
import org.sugarj.driver.DriverParameters;

public class BaseLanguageDefinition {
	private static BaseLanguageDefinition instance = new BaseLanguageDefinition();
		
	public static BaseLanguageDefinition getInstance() {
		return instance;
	}
	
	private BaseLanguageDefinition() { }

	
	public void process(String bldFilename) {
//		IProgressMonitor monitor = new NullProgressMonitor();
//		Result result = Driver.run(DriverParameters.create(env, baseLang, sourceFile, monitor));
//		result.get
	}
}
