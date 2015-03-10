/*
 * Copyright (c) 2015, TU Berlin
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the following
 *   disclaimer in the documentation and/or other materials provided
 *   with the distribution.
 * - Neither the name of the TU Berlin nor the names of its
 *   contributors may be used to endorse or promote products derived
 *   from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
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
		return isNamespaceDec(decl);
	}

	@Override
	public boolean isPlainDecl(IStrategoTerm decl) {
		return false;
	}

	public boolean isNamespaceDec(IStrategoTerm decl) {
		return isApplication(decl, "SXBldNamespaceDecl");
	}
}
