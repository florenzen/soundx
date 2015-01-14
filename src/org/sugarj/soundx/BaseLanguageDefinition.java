package org.sugarj.soundx;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.spoofax.interpreter.terms.IStrategoList;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.jsglr.client.SGLR;
import org.spoofax.jsglr.client.imploder.TreeBuilder;
import org.spoofax.terms.StrategoAppl;
import org.spoofax.terms.StrategoConstructor;
import org.spoofax.terms.StrategoList;
import org.spoofax.terms.TermFactory;
import org.strategoxt.HybridInterpreter;
import org.sugarj.AbstractBaseLanguage;
import org.sugarj.SXBldLanguage;
import org.sugarj.common.ATermCommands;
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
import org.sugarj.driver.SDFCommands;
import org.sugarj.driver.cli.Main;
import org.sugarj.stdlib.StdLib;
import org.sugarj.util.Pair;

public class BaseLanguageDefinition {
	private static BaseLanguageDefinition instance = new BaseLanguageDefinition();

	public static BaseLanguageDefinition getInstance() {
		return instance;
	}

	private BaseLanguageDefinition() {
		interp = new HybridInterpreter();
	}

	private final String soundXFileName = "org/sugarj/soundx/SoundX.str";
	private final String soundXModuleName = "org/sugarj/soundx/SoundX";
	private HybridInterpreter interp;
	private Path binDir;
	private Path srcDir;
	private RelativePath bldPath;
	private String baseLanguageName;
	private Path sdfPath;
	private Path strPath;
	private Path servPath;
	private SoundXBaseLanguage blInstance;

	public void process(String bldFilename, Path pluginDirectory) {
		blInstance = SoundXBaseLanguage.getInstance();
		setBinDirFromPluginDirectory(pluginDirectory);
		setSrcDirFromPluginDirectory(pluginDirectory);
		bldPath = new RelativePath(bldFilename);
		bldPath.setBasePath(srcDir);
		Debug.print("bldPath " + bldPath);
		setBaseLanguageName();
		setGeneratedFilePaths();

		if (generatedFilesOutdated()) {
			Debug.print("Generated files are outdated, run lang-sxbld");
			runCompiler();
			postProcess();
		} else
			Debug.print("Generated files are up-to-date");

		blInstance.ensureFile(soundXFileName);
		initSoundXBaseLanguage();
	}

	private void initSoundXBaseLanguage() {
		blInstance.setBaseFileExtension("st");
		blInstance.setSugarFileExtension("xst");
		blInstance.setLanguageName(baseLanguageName);
		blInstance.setInitEditor(servPath);
		blInstance.setInitGrammar(sdfPath);
		blInstance.setInitTrans(strPath);
	}

	private void setGeneratedFilePaths() {
		String defFileName = baseLanguageName + ".sdf";
		String strFileName = baseLanguageName + ".str";
		String servFileName = baseLanguageName + ".serv";
		sdfPath = new AbsolutePath(binDir.getAbsolutePath() + Environment.sep
				+ defFileName);
		strPath = new AbsolutePath(binDir.getAbsolutePath() + Environment.sep
				+ strFileName);
		servPath = new AbsolutePath(binDir.getAbsolutePath() + Environment.sep
				+ servFileName);
		Debug.print("sdfPath " + sdfPath);
	}

	private void setBaseLanguageName() {
		baseLanguageName = FileCommands.fileName(bldPath);
	}

	private void postProcess() {
		postProcessStratego();
		postProcessSdf();
	}

	private void postProcessStratego() {
		IStrategoTerm strTerm = parseStratego();
		Debug.print("Stratego parse result " + strTerm);
		IStrategoTerm strTermNoImports = fixStrategoImports(strTerm);
		String strString = ppStratego(strTermNoImports);
		Debug.print("Post processed Stratego " + strString);
		try {
			FileCommands.writeToFile(strPath, strString);
		} catch (Exception e) {
			throw new RuntimeException(e.toString());
		}
	}

	private void postProcessSdf() {
		IStrategoTerm sdfTerm = parseSdf();
		Debug.print("SDF parse result " + sdfTerm);
		IStrategoTerm sdfTermNoImports = fixSdfImports(sdfTerm);
		IStrategoTerm sdfTermFixed = null;
		try {
			sdfTermFixed = ATermCommands.fixSDF(sdfTermNoImports, interp);
		} catch (Exception e) {
			new RuntimeException(e.toString());
		}
		Debug.print("Deleted imports");
		String sdfString = ppSdf(sdfTermFixed);
		Debug.print("Post processed Sdf " + sdfString);
		try {
			FileCommands.writeToFile(sdfPath, sdfString);
		} catch (Exception e) {
			throw new RuntimeException(e.toString());
		}
	}

	private IStrategoTerm fixSdfImports(IStrategoTerm term) {
		IStrategoTerm header = term.getSubterm(0);
		IStrategoTerm body = term.getSubterm(2);
		IStrategoTerm imports = TermFactory.EMPTY_LIST;
		return new StrategoAppl(new StrategoConstructor("module", 3),
				new IStrategoTerm[] { header, imports, body },
				term.getAnnotations(), term.getStorageType());
	}

	private IStrategoTerm fixStrategoImports(IStrategoTerm term) {
		IStrategoTerm header = term.getSubterm(0);
		IStrategoList decls = null;
		if (term.getSubterm(1) instanceof IStrategoList)
			decls = (IStrategoList) term.getSubterm(1);
		else
			throw new RuntimeException(
					"Second argument of Stratego module not a list");
		LinkedList<IStrategoTerm> declsNoImports = new LinkedList<IStrategoTerm>();

		for (IStrategoTerm decl : decls) {
			if (decl.getTermType() == IStrategoTerm.APPL) {
				if (!((StrategoAppl) decl).getConstructor().getName()
						.equals("Imports"))
					declsNoImports.addLast(decl);
			} else
				declsNoImports.addLast(decl);
		}

		declsNoImports.addFirst(ATermCommands.atermFromString("Imports([Import(\"" + soundXModuleName + "\")])"));
		StrategoList declsTerm = TermFactory.EMPTY_LIST;
		for (Iterator<IStrategoTerm> i = declsNoImports.descendingIterator(); i
				.hasNext();) {
			IStrategoTerm declTerm = i.next();
			declsTerm = new StrategoList(declTerm, declsTerm,
					TermFactory.EMPTY_LIST, decls.getStorageType());
		}

		Debug.print("Subterm decls " + declsTerm);
		IStrategoTerm trm = new StrategoAppl(new StrategoConstructor("Module",
				2), new IStrategoTerm[] { header, declsTerm },
				term.getAnnotations(), term.getStorageType());
		Debug.print("Result term " + trm);
		return ATermCommands.atermFromString(trm.toString()); // Without the
																// to-and-from
																// String the
																// pretty
																// printer
																// crashes.
	}

	private IStrategoTerm parseSdf() {
		Path sdfTbl = blInstance.ensureFile("org/sugarj/languages/Sdf2.tbl");
		return parse(sdfTbl, sdfPath, "Sdf2Module");
	}

	private IStrategoTerm parseStratego() {
		Path strTbl = blInstance
				.ensureFile("org/sugarj/languages/Stratego.tbl");
		return parse(strTbl, strPath, "StrategoModule");
	}

	private IStrategoTerm parse(Path table, Path inputFile, String startSymbol) {
		SGLR parser = null;
		try {
			parser = new SGLR(new TreeBuilder(),
					ATermCommands.parseTableManager.loadFromFile(table
							.getAbsolutePath()));
			parser.setUseStructureRecovery(true);
		} catch (Exception e) {
			throw new RuntimeException(e.toString());
		}

		Object parseResult = null;

		try {
			String fileContent = FileCommands.readFileAsString(inputFile);
			parseResult = parser.parseMax(fileContent,
					sdfPath.getAbsolutePath(), startSymbol);
		} catch (Exception e) {
			Debug.print("Exception during parsing" + e.toString());
			throw new RuntimeException(e.toString());
		}
		Object[] os = (Object[]) parseResult;
		IStrategoTerm term = (IStrategoTerm) os[0];
		return term;
	}

	private String ppSdf(IStrategoTerm term) {
		String result = null;
		try {
			result = SDFCommands.prettyPrintSDF(term, interp);
		} catch (Exception e) {
			throw new RuntimeException(e.toString());
		}
		return result;
	}

	private String ppStratego(IStrategoTerm term) {
		String result = null;
		try {
			result = SDFCommands.prettyPrintSTR(term, interp);
		} catch (Exception e) {
			throw new RuntimeException(e.toString());
		}
		return result;
	}

	private boolean generatedFilesOutdated() {
		if (FileCommands.fileExists(sdfPath)
				&& FileCommands.fileExists(strPath)) {
			return !(FileCommands.isModifiedLater(sdfPath, bldPath) && FileCommands
					.isModifiedLater(strPath, bldPath));
		} else
			return true;
	}

	private Result runCompiler() {
		IProgressMonitor monitor = new NullProgressMonitor();
		AbstractBaseLanguage baseLang = SXBldLanguage.getInstance();
		Environment environment = new Environment(true, StdLib.stdLibDir,
				Stamper.DEFAULT);
		environment.setCacheDir(new RelativePath(new AbsolutePath(
				FileCommands.TMP_DIR), ".sugarjcache"));
		environment.setAtomicImportParsing(false);
		environment.setNoChecking(false);
		environment.setBin(binDir);
		environment.setGenerateFiles(true);
		Result result = null;
		try {
			result = Driver.run(DriverParameters.create(environment, baseLang,
					bldPath, monitor));
			// TODO generate proper editor services file here
			String editorServicesHeader = "module " + baseLanguageName + "\n";
			FileCommands.writeToFile(servPath, editorServicesHeader);
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
