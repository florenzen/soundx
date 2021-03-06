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

import static org.sugarj.common.ATermCommands.getApplicationSubterm;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.spoofax.interpreter.terms.IStrategoTerm;
import org.sugarj.common.ATermCommands;
import org.sugarj.common.Environment;
import org.sugarj.common.FileCommands;
import org.sugarj.common.StringCommands;
import org.sugarj.common.path.Path;
import org.sugarj.common.path.RelativePath;

/**
 * Sugar* language processor for SoundX base language definitions.
 *
 * @author Florian Lorenzen <florian.lorenzen at tu-berlin de>
 */
public class SXBldProcessor extends AbstractBaseProcessor {
	private static final long serialVersionUID = 6325786656556068937L;

	private String moduleHeader;
	private List<String> imports = new LinkedList<String>();
	private List<String> body = new LinkedList<String>();
	private boolean hasExtension = false;

	private Set<RelativePath> generatedModules = new HashSet<RelativePath>();

	private Environment environment;
	private String relNamespaceName;
	private RelativePath sourceFile;
	private Path outFile;
	private String moduleName;

	private IStrategoTerm ppTable;

	@Override
	public String getGeneratedSource() {
		if (moduleHeader == null)
			return "";

		if (hasExtension && body.isEmpty())
			return "";

		return moduleHeader + "\n"
				+ StringCommands.printListSeparated(imports, "\n") + "\n"
				+ StringCommands.printListSeparated(body, "\n");
	}

	@Override
	public Path getGeneratedSourceFile() {
		return outFile;
	}

	@Override
	public String getNamespace() {
		return relNamespaceName;
	}

	@Override
	public SXBldLanguage getLanguage() {
		return SXBldLanguage.getInstance();
	}

	@Override
	public void init(Set<RelativePath> sourceFiles, Environment environment) {
		if (sourceFiles.size() != 1)
			throw new IllegalArgumentException(
					"SXBld can only compile one source file at a time.");

		this.environment = environment;
		this.sourceFile = sourceFiles.iterator().next();
		outFile = environment.createOutPath(FileCommands
				.dropExtension(sourceFile.getRelativePath()) + ".sxbldi-src");
	}

	private void processNamespaceDecl(IStrategoTerm toplevelDecl)
			throws IOException {
		String qualifiedModuleName = prettyPrint(getApplicationSubterm(
				toplevelDecl, "SXBldNamespaceDecl", 0));
		String declaredModuleName = FileCommands.fileName(qualifiedModuleName);
		moduleName = FileCommands.dropExtension(FileCommands
				.fileName(sourceFile.getRelativePath()));
		String declaredRelNamespaceName = FileCommands
				.dropFilename(qualifiedModuleName);
		relNamespaceName = FileCommands.dropFilename(sourceFile
				.getRelativePath());

		RelativePath objectFile = environment
				.createOutPath(getRelativeNamespaceSep() + moduleName + "."
						+ getLanguage().getBinaryFileExtension());
		generatedModules.add(objectFile);

		moduleHeader = prettyPrint(toplevelDecl);

		if (!declaredRelNamespaceName.equals(relNamespaceName))
			throw new RuntimeException("The declared namespace '"
					+ declaredRelNamespaceName + "'"
					+ " does not match the expected namespace '"
					+ relNamespaceName + "'.");

		if (!declaredModuleName.equals(moduleName))
			throw new RuntimeException("The declared module name '"
					+ declaredModuleName + "'"
					+ " does not match the expected module name '" + moduleName
					+ "'.");
	}

	@Override
	public List<String> processBaseDecl(IStrategoTerm toplevelDecl)
			throws IOException {
		if (getLanguage().isNamespaceDec(toplevelDecl)) {
			processNamespaceDecl(toplevelDecl);
			return Collections.emptyList();
		}

		String text = null;
		try {
			text = prettyPrint(toplevelDecl);
		} catch (NullPointerException e) {
			ATermCommands.setErrorMessage(toplevelDecl,
					"pretty printing SXBld failed");
		}
		if (text != null)
			body.add(text);
		return Collections.emptyList();
	}

	@Override
	public String getModulePathOfImport(IStrategoTerm toplevelDecl) {
		return null;
	}

	@Override
	public void processModuleImport(IStrategoTerm toplevelDecl)
			throws IOException {
		imports.add(prettyPrint(toplevelDecl));
	}

	public String prettyPrint(IStrategoTerm term) {
		if (ppTable == null)
			ppTable = ATermCommands.readPrettyPrintTable(getLanguage()
					.ensureFile("org/sugarj/languages/SXBld.pp")
					.getAbsolutePath());

		return ATermCommands.prettyPrint(ppTable, term, interp);
	}

	@Override
	public List<Path> compile(List<Path> outFiles, Path bin,
			List<Path> includePaths) throws IOException {
		List<Path> generatedFiles = new LinkedList<Path>();
		for (Path out : outFiles) {
			RelativePath relOut = (RelativePath) out;
			Path compilePath = new RelativePath(bin,
					FileCommands.dropExtension(relOut.getRelativePath())
							+ ".sxbldi");
			FileCommands.copyFile(out, compilePath);
			generatedFiles.add(compilePath);
		}
		return generatedFiles;
	}

	@Override
	public boolean isModuleExternallyResolvable(String relModulePath) {
		return false;
	}

	@Override
	public String getExtensionName(IStrategoTerm decl) throws IOException {
		hasExtension = true;
		return moduleName;
	}

	@Override
	public IStrategoTerm getExtensionBody(IStrategoTerm decl) {
		return getApplicationSubterm(decl, "SXBldExtensionDecl", 0);
	}
}
