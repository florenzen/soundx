package org.sugarj;

import static org.sugarj.common.ATermCommands.isApplication;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.spoofax.interpreter.terms.IStrategoTerm;
import org.sugarj.common.Log;
import org.sugarj.common.path.Path;

/**
 * @author Sebastian Erdweg <seba at informatik uni-marburg de>
 */

public class SXBldLanguage extends AbstractBaseLanguage {

	private SXBldLanguage() {
	}

	private static SXBldLanguage instance = new SXBldLanguage();

	public static SXBldLanguage getInstance() {
		return instance;
	}

	/**
	 * @see org.sugarj.AbstractBaseLanguage#createNewProcessor()
	 */
	@Override
	public AbstractBaseProcessor createNewProcessor() {
		// TODO leave log level unchanged
		Log.log.setLoggingLevel(Log.ALWAYS);
		return new SXBldProcessor();
	}

	@Override
	public String getLanguageName() {
		return "SXBld";
	}

	@Override
	public String getVersion() {
		return "sxbld-0.1";
	}

	@Override
	public String getBinaryFileExtension() {
		return null;
	}

	@Override
	public String getBaseFileExtension() {
		return "sxbldi";
	}

	@Override
	public String getSugarFileExtension() {
		return "sxbld";
	}

	@Override
	public Path getInitGrammar() {
		return ensureFile("org/sugarj/sxbld/initGrammar.sdf");
	}

	@Override
	public String getInitGrammarModuleName() {
		return "org/sugarj/sxbld/initGrammar";
	}

	@Override
	public Path getInitTrans() {
		return ensureFile("org/sugarj/sxbld/initTrans.str");
	}

	@Override
	public String getInitTransModuleName() {
		return "org/sugarj/sxbld/initTrans";
	}

	@Override
	public Path getInitEditor() {
		return ensureFile("org/sugarj/sxbld/initEditor.serv");
	}

	@Override
	public String getInitEditorModuleName() {
		return "org/sugarj/sxbld/initEditor";
	}

	@Override
	public List<Path> getPackagedGrammars() {
		List<Path> grammars = new LinkedList<Path>(super.getPackagedGrammars());
	    grammars.add(ensureFile("org/sugarj/languages/SXBld.def"));
	    return Collections.unmodifiableList(grammars);
	}

	@Override
	public boolean isExtensionDecl(IStrategoTerm decl) {
		if (isApplication(decl, "SXBldExtensionDecl"))
			return true;
		return false;
	}

	@Override
	public boolean isImportDecl(IStrategoTerm decl) {
		return false;
	}

	@Override
	public boolean isBaseDecl(IStrategoTerm decl) {
		return isApplication(decl, "SXBldBaseDecl")
				|| isNamespaceDec(decl);
	}

	@Override
	public boolean isPlainDecl(IStrategoTerm decl) {
		return false;
	}

	public boolean isNamespaceDec(IStrategoTerm decl) {
		return isApplication(decl, "SXBldHeader");
	}
}
