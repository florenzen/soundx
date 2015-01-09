package org.sugarj.soundx;

import org.spoofax.interpreter.terms.IStrategoTerm;
import org.sugarj.AbstractBaseLanguage;
import org.sugarj.AbstractBaseProcessor;
import org.sugarj.common.path.Path;

public class SoundXAbstractBaseLanguage extends AbstractBaseLanguage {
	@Override
	public AbstractBaseProcessor createNewProcessor() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getVersion() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getLanguageName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getSugarFileExtension() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getBinaryFileExtension() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getBaseFileExtension() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Path getInitGrammar() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getInitGrammarModuleName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Path getInitTrans() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getInitTransModuleName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Path getInitEditor() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getInitEditorModuleName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isExtensionDecl(IStrategoTerm decl) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isImportDecl(IStrategoTerm decl) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isBaseDecl(IStrategoTerm decl) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isPlainDecl(IStrategoTerm decl) {
		// TODO Auto-generated method stub
		return false;
	}	
}
