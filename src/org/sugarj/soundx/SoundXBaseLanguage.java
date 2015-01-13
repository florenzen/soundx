package org.sugarj.soundx;

import org.spoofax.interpreter.terms.IStrategoTerm;
import org.sugarj.AbstractBaseLanguage;
import org.sugarj.AbstractBaseProcessor;
import org.sugarj.common.Log;
import org.sugarj.common.path.Path;
import org.sugarj.soundx.Debug;

public class SoundXBaseLanguage extends AbstractBaseLanguage {
	private SoundXBaseLanguage() {}
	
	private static SoundXBaseLanguage instance = new SoundXBaseLanguage();

	public static SoundXBaseLanguage getInstance() {
		return instance;
	}
	
	public void processBaseLanguageDefinition(String bldFilename, Path pluginDirectory) {
		Debug.print("Processing " + bldFilename);
		Debug.print("Plugin directory " + pluginDirectory.toString());
		
		
	}

	@Override
	public AbstractBaseProcessor createNewProcessor() {
		Log.log.setLoggingLevel(Log.ALWAYS);
		return new SoundXBaseProcessor();
	}

	@Override
	public String getVersion() {
		return "0.1";
	}

	@Override
	public String getLanguageName() {
		return "stlc";
	}

	@Override
	public String getSugarFileExtension() {
		return "xst";
	}

	@Override
	public String getBinaryFileExtension() {
		return null;
	}

	@Override
	public String getBaseFileExtension() {
		return "st";
	}

	@Override
	public Path getInitGrammar() {
		return ensureFile("foo");
	}

	@Override
	public String getInitGrammarModuleName() {
		return "foo";
	}

	@Override
	public Path getInitTrans() {
		return ensureFile("foo");
	}

	@Override
	public String getInitTransModuleName() {
		return "foo";
	}

	@Override
	public Path getInitEditor() {
		return ensureFile("foo");
	}

	@Override
	public String getInitEditorModuleName() {
		return "foo";
	}

	@Override
	public boolean isExtensionDecl(IStrategoTerm decl) {
		return false;
	}

	@Override
	public boolean isImportDecl(IStrategoTerm decl) {
		return false;
	}

	@Override
	public boolean isBaseDecl(IStrategoTerm decl) {
		return false;
	}

	@Override
	public boolean isPlainDecl(IStrategoTerm decl) {
		return false;
	}	
}
