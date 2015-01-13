package org.sugarj.soundx;

import static org.sugarj.common.ATermCommands.getApplicationSubterm;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.spoofax.interpreter.terms.IStrategoTerm;
import org.sugarj.AbstractBaseProcessor;
import org.sugarj.SXBldLanguage;
import org.sugarj.common.ATermCommands;
import org.sugarj.common.Environment;
import org.sugarj.common.FileCommands;
import org.sugarj.common.StringCommands;
import org.sugarj.common.path.Path;
import org.sugarj.common.path.RelativePath;

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

	/*
	 * processing stuff follows here
	 */

	@Override
	public void init(Set<RelativePath> sourceFiles, Environment environment) {
		if (sourceFiles.size() != 1)
			throw new IllegalArgumentException(
					"stlc can only compile one source file at a time.");

		this.environment = environment;
		this.sourceFile = sourceFiles.iterator().next();
		outFile = environment.createOutPath(FileCommands
				.dropExtension(sourceFile.getRelativePath()) + ".st-src");
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
							+ ".st");
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
