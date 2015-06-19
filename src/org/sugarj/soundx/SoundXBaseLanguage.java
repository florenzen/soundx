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
package org.sugarj.soundx;

import static org.sugarj.common.ATermCommands.isApplication;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.terms.StrategoAppl;
import org.sugarj.AbstractBaseLanguage;
import org.sugarj.common.Log;
import org.sugarj.common.path.Path;
import org.sugarj.soundx.Debug;
import org.sugarj.util.Pair;

public class SoundXBaseLanguage extends AbstractBaseLanguage {
	private String languageName;
	private String baseFileExtension;
	private String sugarFileExtension;
	private Path initGrammar;
	private Path initTrans;
	private Path initEditor;
	private Path packagedGrammar;

	public Path getPackagedGrammar() {
		return packagedGrammar;
	}

	protected void setPackagedGrammar(Path packagedGrammar) {
		this.packagedGrammar = packagedGrammar;
	}

	private Path ppTable;

	public Path getPpTable() {
		return ppTable;
	}

	protected void setPpTable(Path ppTable) {
		this.ppTable = ppTable;
	}

	private Pair<String, Integer> namespaceDecCons;
	private Map<String, Integer> importDecCons;
	private Set<String> bodyDecCons;
	private SXNamespaceKind namespaceKind;
	private Map<String, Integer> namespaceSuffices;

	public Map<String, Integer> getNamespaceSuffices() {
		return namespaceSuffices;
	}

	protected void setNamespaceSuffices(Map<String, Integer> namespaceSuffices) {
		this.namespaceSuffices = namespaceSuffices;
	}

	public Pair<String, Integer> getNamespaceDecCons() {
		return namespaceDecCons;
	}

	protected void setNamespaceDecCons(Pair<String, Integer> namespaceDecCons) {
		this.namespaceDecCons = namespaceDecCons;
	}

	protected void setImportDecCons(Map<String, Integer> importDecCons2) {
		this.importDecCons = importDecCons2;
	}

	public Map<String, Integer> getImportDecCons() {
		return importDecCons;
	}

	protected void setBodyDecCons(Set<String> bodyDecCons) {
		this.bodyDecCons = bodyDecCons;
	}

	public SXNamespaceKind getNamespaceKind() {
		return namespaceKind;
	}

	protected void setNamespaceKind(SXNamespaceKind namespaceKind) {
		this.namespaceKind = namespaceKind;
	}

	public SoundXBaseLanguage() {
	}

	public void processBaseLanguageDefinition(String bldFilename,
			Path pluginDirectory) {
		Debug.print("Processing " + bldFilename);
		Debug.print("Plugin directory " + pluginDirectory.toString());

		BaseLanguageDefinition bld = new BaseLanguageDefinition();
		bld.process(this, bldFilename, pluginDirectory);
	}

	@Override
	public SoundXBaseProcessor createNewProcessor() {
		Log.log.setLoggingLevel(Log.ALWAYS);
		return new SoundXBaseProcessor(this);
	}

	@Override
	public String getVersion() {
		return "0.1"; // TODO set version base language definition
	}

	@Override
	public String getLanguageName() {
		return languageName;
	}

	protected void setLanguageName(String languageName) {
		this.languageName = languageName;
	}

	@Override
	public String getSugarFileExtension() {
		return sugarFileExtension;
	}

	protected void setSugarFileExtension(String sugarFileExtension) {
		this.sugarFileExtension = sugarFileExtension;
	}

	@Override
	public String getBinaryFileExtension() {
		return null;
	}

	@Override
	public String getBaseFileExtension() {
		return baseFileExtension;
	}

	protected void setBaseFileExtension(String baseFileExtension) {
		this.baseFileExtension = baseFileExtension;
	}

	@Override
	public Path getInitGrammar() {
		return initGrammar;
	}

	protected void setInitGrammar(Path initGrammar) {
		this.initGrammar = initGrammar;
	}

	@Override
	public String getInitGrammarModuleName() {
		return languageName;
	}

	@Override
	public Path getInitTrans() {
		return initTrans;
	}

	protected void setInitTrans(Path initTrans) {
		this.initTrans = initTrans;
	}

	@Override
	public String getInitTransModuleName() {
		return languageName;
	}

	@Override
	public Path getInitEditor() {
		return initEditor;
	}

	protected void setInitEditor(Path initEditor) {
		this.initEditor = initEditor;
	}

	@Override
	public List<Path> getPackagedGrammars() {
		List<Path> grammars = new LinkedList<Path>(super.getPackagedGrammars());
		grammars.add(packagedGrammar);
		return Collections.unmodifiableList(grammars);
	}

	@Override
	public String getInitEditorModuleName() {
		return languageName;
	}

	public boolean isNamespaceDec(IStrategoTerm decl) {
		return isApplication(decl, namespaceDecCons.a);
	}

	@Override
	public boolean isExtensionDecl(IStrategoTerm decl) {
		boolean isExtDec = isApplication(decl, "SXExtensionDecl")
				|| isApplication(decl, "SXExtensionBegin")
				|| isApplication(decl, "SXExtensionEnd");
		
		return isExtDec;
	}

	@Override
	public boolean isImportDecl(IStrategoTerm decl) {
		if (decl.getTermType() == IStrategoTerm.APPL) {
			String consName = ((StrategoAppl) decl).getConstructor().getName();
			return importDecCons.containsKey(consName);
		} else
			return false;
	}

	@Override
	public boolean isBaseDecl(IStrategoTerm decl) {
		return isNamespaceDec(decl) || isBodyDecl(decl);
	}

	private boolean isBodyDecl(IStrategoTerm decl) {
		if (decl.getTermType() == IStrategoTerm.APPL) {
			String consName = ((StrategoAppl) decl).getConstructor().getName();
			return bodyDecCons.contains(consName);
		} else
			return false;
	}

	@Override
	public boolean isPlainDecl(IStrategoTerm decl) {
		return false;
	}
}
