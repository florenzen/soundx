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

import static org.sugarj.common.ATermCommands.getApplicationSubterm;
import static org.sugarj.common.ATermCommands.isApplication;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.terms.StrategoAppl;
import org.spoofax.terms.TermFactory;
import org.sugarj.AbstractBaseProcessor;
import org.sugarj.common.ATermCommands;
import org.sugarj.common.Environment;
import org.sugarj.common.FileCommands;
import org.sugarj.common.StringCommands;
import org.sugarj.common.path.Path;
import org.sugarj.common.path.RelativePath;
import org.sugarj.util.Pair;

/**
 * Sugar* language processor for a language defined with SoundX.
 *
 * @author Florian Lorenzen <florian.lorenzen@tu-berlin.de>
 *
 */
public class SoundXBaseProcessor extends AbstractBaseProcessor {

	private static final long serialVersionUID = 8867450741041891584L;

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
	private boolean moduleNameDeclared = false;

	private SoundXBaseLanguage language;

	private IStrategoTerm ppTable;

	public SoundXBaseProcessor(SoundXBaseLanguage language) {
		this.language = language;
	}

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
	public SoundXBaseLanguage getLanguage() {
		return language;
	}

	@Override
	public void init(Set<RelativePath> sourceFiles, Environment environment) {
		if (sourceFiles.size() != 1)
			throw new IllegalArgumentException(getLanguage().getLanguageName()
					+ " can only compile one source file at a time.");

		this.environment = environment;
		this.sourceFile = sourceFiles.iterator().next();
		String srcExt = "." + getLanguage().getBaseFileExtension() + "-src";
		outFile = environment.createOutPath(FileCommands
				.dropExtension(sourceFile.getRelativePath()) + srcExt);
	}

	private void processNamespaceDecl(IStrategoTerm toplevelDecl) {
		moduleHeader = prettyPrint(toplevelDecl);

		Pair<String, Integer> namespaceDecCons = getLanguage()
				.getNamespaceDecCons();
		String namespaceIdentifier = prettyPrint(getApplicationSubterm(
				toplevelDecl, namespaceDecCons.a, namespaceDecCons.b));

		SXNamespaceKind kind = getLanguage().getNamespaceKind();
		if (kind instanceof SXNamespaceFlat) {
			processFlatNamespaceDecl(namespaceIdentifier);
		} else if (kind instanceof SXNamespaceNested) {
			processNestedNamespaceDecl(namespaceIdentifier);
		} else if (kind instanceof SXNamespacePrefixed) {
			processPrefixedNamespaceDecl(namespaceIdentifier);
		}
	}

	private void processFlatNamespaceDecl(String moduleIdentifier) {
		String moduleIdentifierFromFile = FileCommands
				.dropExtension(FileCommands.fileName(sourceFile
						.getRelativePath()));
		relNamespaceName = "";
		moduleName = moduleIdentifier;

		RelativePath objectFile = environment
				.createOutPath(getRelativeNamespaceSep() + moduleName + "."
						+ getLanguage().getBinaryFileExtension());
		generatedModules.add(objectFile);

		if (!moduleIdentifierFromFile.equals(moduleIdentifier))
			throw new RuntimeException("The declared module name '"
					+ moduleIdentifier + "'"
					+ " does not match the expected module name '"
					+ moduleIdentifierFromFile + "'.");
	}

	private void processNestedNamespaceDecl(String moduleIdentifier) {
		char sep = ((SXNamespaceNested) getLanguage().getNamespaceKind())
				.getSeparator();
		String qualifiedModulePath = moduleIdentifier.replace(sep,
				File.separatorChar);
		String moduleIdentifierFromFile = FileCommands
				.dropExtension(FileCommands.fileName(sourceFile
						.getRelativePath()));
		String namespaceNameFromFile = FileCommands.dropFilename(sourceFile
				.getRelativePath());
		String moduleIdentifierFromDecl = FileCommands
				.fileName(qualifiedModulePath);
		String namespaceNameFromDecl = FileCommands
				.dropFilename(qualifiedModulePath);

		relNamespaceName = namespaceNameFromFile;
		moduleName = moduleIdentifierFromFile;

		RelativePath objectFile = environment
				.createOutPath(getRelativeNamespaceSep() + moduleName + "."
						+ getLanguage().getBinaryFileExtension());
		generatedModules.add(objectFile);

		if (!namespaceNameFromFile.equals(namespaceNameFromDecl))
			throw new RuntimeException("The declared namespace '"
					+ namespaceNameFromDecl + "'"
					+ " does not match the expected namespace '"
					+ namespaceNameFromFile + "'.");

		if (!moduleIdentifierFromFile.equals(moduleIdentifierFromDecl))
			throw new RuntimeException("The declared module name '"
					+ moduleIdentifierFromDecl + "'"
					+ " does not match the expected module name '"
					+ moduleIdentifierFromFile + "'.");
	}

	private void processPrefixedNamespaceDecl(String namespaceName) {
		char sep = ((SXNamespacePrefixed) getLanguage().getNamespaceKind())
				.getSeparator();
		String namespaceNameFromDecl = namespaceName.replace(sep,
				File.separatorChar);

		String moduleIdentifierFromFile = FileCommands
				.dropExtension(FileCommands.fileName(sourceFile
						.getRelativePath()));
		String namespaceNameFromFile = FileCommands.dropFilename(sourceFile
				.getRelativePath());

		relNamespaceName = namespaceNameFromFile;
		moduleName = moduleIdentifierFromFile;

		if (!namespaceNameFromFile.equals(namespaceNameFromDecl))
			throw new RuntimeException("The declared namespace '"
					+ namespaceNameFromDecl + "'"
					+ " does not match the expected namespace '"
					+ namespaceNameFromFile + "'.");
	}

	@Override
	public List<String> processBaseDecl(IStrategoTerm toplevelDecl)
			throws IOException {
		if (getLanguage().isNamespaceDec(toplevelDecl)) {
			processNamespaceDecl(toplevelDecl);
			return Collections.emptyList();
		}

		// set module name for prefixed namespaces
		if (getLanguage().getNamespaceKind() instanceof SXNamespacePrefixed) {
			String consName = ((StrategoAppl) toplevelDecl).getConstructor()
					.getName();
			Map<String, Integer> namespaceSuffices = getLanguage()
					.getNamespaceSuffices();
			if (namespaceSuffices.containsKey(consName)) {
				Integer index = namespaceSuffices.get(consName);
				String moduleIdentifierFromDecl = prettyPrint(getApplicationSubterm(
						toplevelDecl, consName, index));
				String moduleIdentifierFromFile = FileCommands
						.dropExtension(FileCommands.fileName(sourceFile
								.getRelativePath()));

				if (!moduleIdentifierFromFile.equals(moduleIdentifierFromDecl))
					throw new RuntimeException("The declared module name '"
							+ moduleIdentifierFromDecl + "'"
							+ " does not match the expected module name '"
							+ moduleIdentifierFromFile + "'.");

				if (moduleNameDeclared) {
					throw new RuntimeException(
							"The module name can only be declared once.");
				} else {
					moduleNameDeclared = true;
					moduleName = moduleIdentifierFromFile;
				}
			}
		}

		String text = null;
		try {
			text = prettyPrint(toplevelDecl);
		} catch (NullPointerException e) {
			ATermCommands.setErrorMessage(toplevelDecl, "pretty printing "
					+ getLanguage().getLanguageName() + " failed");
		}
		if (text != null)
			body.add(text);
		return Collections.emptyList();
	}

	@Override
	public String getModulePathOfImport(IStrategoTerm toplevelDecl) {
		Map<String, Integer> importDecCons = getLanguage().getImportDecCons();
		String consName = ((StrategoAppl) toplevelDecl).getConstructor()
				.getName();
		Integer index = importDecCons.get(consName);
		SXNamespaceKind kind = getLanguage().getNamespaceKind();
		String importedModule = prettyPrint(getApplicationSubterm(toplevelDecl,
				consName, index));
		String importedModulePath = "";
		if (kind instanceof SXNamespaceNested) {
			char sep = ((SXNamespaceNested) kind).getSeparator();
			importedModulePath = importedModule
					.replace(sep, File.separatorChar);
		} else if (kind instanceof SXNamespacePrefixed) {
			char sep = ((SXNamespacePrefixed) kind).getSeparator();
			importedModulePath = importedModule
					.replace(sep, File.separatorChar);
		} else { // take flat namespace as default
			importedModulePath = importedModule;
		}
		return importedModulePath;
	}

	@Override
	public void processModuleImport(IStrategoTerm toplevelDecl)
			throws IOException {
		imports.add(prettyPrint(toplevelDecl));
	}

	public String prettyPrint(IStrategoTerm term) {
		if (ppTable == null)
			ppTable = ATermCommands.readPrettyPrintTable(getLanguage()
					.getPpTable().getAbsolutePath());

		return ATermCommands.prettyPrint(ppTable, term, interp);
	}

	@Override
	public List<Path> compile(List<Path> outFiles, Path bin,
			List<Path> includePaths) throws IOException {
		List<Path> generatedFiles = new LinkedList<Path>();
		for (Path out : outFiles) {
			RelativePath relOut = (RelativePath) out;
			Path compilePath = new RelativePath(bin,
					FileCommands.dropExtension(relOut.getRelativePath()) + "."
							+ getLanguage().getBaseFileExtension());
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
		// SXExtensionBegin or -End may contain a namespace
		// suffix. This is extracted here and checked against the
		// file's name.
		SXNamespaceKind kind = getLanguage().getNamespaceKind();
		if (isApplication(decl, "SXExtensionBegin")
				|| isApplication(decl, "SXExtensionEnd")) {
			if (kind instanceof SXNamespacePrefixed) {
				IStrategoTerm term = null;
				if (isApplication(decl, "SXExtensionBegin"))
					term = getApplicationSubterm(decl, "SXExtensionBegin", 0);
				if (isApplication(decl, "SXExtnsionEnd"))
					term = getApplicationSubterm(decl, "SXExtensionEnd", 0);
				String consName = ((StrategoAppl) term).getConstructor()
						.getName();
				Map<String, Integer> namespaceSuffices = getLanguage()
						.getNamespaceSuffices();
				if (namespaceSuffices.containsKey(consName)) {
					Integer index = namespaceSuffices.get(consName);
					String moduleIdentifierFromDecl = prettyPrint(getApplicationSubterm(
							term, consName, index));
					String moduleIdentifierFromFile = FileCommands
							.dropExtension(FileCommands.fileName(sourceFile
									.getRelativePath()));

					if (!moduleIdentifierFromFile
							.equals(moduleIdentifierFromDecl))
						throw new RuntimeException("The declared module name '"
								+ moduleIdentifierFromDecl + "'"
								+ " does not match the expected module name '"
								+ moduleIdentifierFromFile + "'.");

					if (moduleNameDeclared) {
						throw new RuntimeException(
								"The module name can only be declared once.");
					} else {
						moduleNameDeclared = true;
						moduleName = moduleIdentifierFromFile;
					}
				}
			}
			return TermFactory.EMPTY_LIST;
		} else {
			IStrategoTerm term = getApplicationSubterm(decl, "SXExtensionDecl",
					0);
			return term;
		}
	}
}
