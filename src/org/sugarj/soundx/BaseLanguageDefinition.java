package org.sugarj.soundx;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.sugarj.AbstractBaseLanguage;
import org.sugarj.SXBldLanguage;
import org.sugarj.common.Environment;
import org.sugarj.common.FileCommands;
import org.sugarj.common.Log;
import org.sugarj.common.cleardep.Stamper;
import org.sugarj.common.cleardep.TimeStamper;
import org.sugarj.common.path.AbsolutePath;
import org.sugarj.common.path.RelativePath;
import org.sugarj.common.path.Path;
import org.sugarj.driver.Driver;
import org.sugarj.driver.DriverParameters;
import org.sugarj.driver.Result;
import org.sugarj.driver.cli.Main;
import org.sugarj.stdlib.StdLib;

public class BaseLanguageDefinition {
	private static BaseLanguageDefinition instance = new BaseLanguageDefinition();

	public static BaseLanguageDefinition getInstance() {
		return instance;
	}

	private BaseLanguageDefinition() {
	}

	private Path binDir;
	private Path srcDir;
	private RelativePath bldPath;

	public void process(String bldFilename, Path pluginDirectory) {
		setBinDirFromPluginDirectory(pluginDirectory);
		setSrcDirFromPluginDirectory(pluginDirectory);
		bldPath = new RelativePath(bldFilename);
		bldPath.setBasePath(srcDir);
		
		runCompiler();
		// IProgressMonitor monitor = new NullProgressMonitor();
		// Result result = Driver.run(DriverParameters.create(env, baseLang,
		// sourceFile, monitor));
		// result.get
	}

	private Result runCompiler() {
		IProgressMonitor monitor = new NullProgressMonitor();
		AbstractBaseLanguage baseLang = SXBldLanguage.getInstance();
		Environment environment = new Environment(true, StdLib.stdLibDir, Stamper.DEFAULT);
	    environment.setCacheDir(new RelativePath(new AbsolutePath(FileCommands.TMP_DIR), ".sugarjcache"));
	    environment.setAtomicImportParsing(false);
	    environment.setNoChecking(false);
		environment.setBin(binDir);
		environment.setGenerateFiles(true);
		Debug.print("binDir " + binDir);
		Debug.print("srcDir " + srcDir);
		Debug.print("bldPath " + bldPath);
		//environment.addToSourcePath(srcDir);
		Debug.print("source path " + environment.getSourcePath());
		Result result = null;
		try {
			result = Driver.run(DriverParameters.create(environment, baseLang, bldPath,
					monitor));
		} catch (Exception e) {
			throw new RuntimeException(e.toString());
		}
		return result;
	}

	private void setBinDirFromPluginDirectory(Path pluginDirectory) {
		binDir = new AbsolutePath(pluginDirectory.getAbsolutePath()
				+ Environment.sep + "bin");
	}

	private void setSrcDirFromPluginDirectory(Path pluginDirectory) {
		srcDir = new AbsolutePath(pluginDirectory.getAbsolutePath()
				+ Environment.sep + "src");
	}
}
