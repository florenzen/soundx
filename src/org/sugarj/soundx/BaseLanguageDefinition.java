package org.sugarj.soundx;

import java.nio.file.Files;
import java.nio.file.attribute.FileAttribute;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.spoofax.interpreter.terms.IStrategoList;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.jsglr.client.SGLR;
import org.spoofax.jsglr.client.imploder.TreeBuilder;
import org.spoofax.terms.StrategoAppl;
import org.spoofax.terms.StrategoConstructor;
import org.spoofax.terms.StrategoList;
import org.spoofax.terms.StrategoString;
import org.spoofax.terms.TermFactory;
import org.strategoxt.HybridInterpreter;
import org.strategoxt.lang.Context;
import org.strategoxt.lang.compat.CompatLibrary;
import org.strategoxt.tools.pp_pp_table_0_0;
import org.strategoxt.tools.ppgenerate_0_0;
import org.sugarj.AbstractBaseLanguage;
import org.sugarj.SXBldLanguage;
import org.sugarj.common.ATermCommands;
import org.sugarj.common.Environment;
import org.sugarj.common.FileCommands;
import org.sugarj.common.StringCommands;
import org.sugarj.common.cleardep.Stamper;
import org.sugarj.common.path.AbsolutePath;
import org.sugarj.common.path.RelativePath;
import org.sugarj.common.path.Path;
import org.sugarj.driver.Driver;
import org.sugarj.driver.DriverParameters;
import org.sugarj.driver.Result;
import org.sugarj.driver.SDFCommands;
import org.sugarj.stdlib.StdLib;
import org.sugarj.util.Pair;

// TODO fail if some essential declaration is missing in the
// base language definition (like "interface" or "toplevel declaration").

/**
 * Generation of SugarJ files from a SoundX base language definition.
 *
 * @author Florian Lorenzen <florian.lorenzen@tu-berlin.de>
 */
/**
 * @author florenz
 *
 */
/**
 * @author florenz
 *
 */
public class BaseLanguageDefinition {
	private static BaseLanguageDefinition instance = new BaseLanguageDefinition();

	public static BaseLanguageDefinition getInstance() {
		return instance;
	}

	private BaseLanguageDefinition() {
		interp = new HybridInterpreter();
	}

	/**
	 * File extension of a file in the extended language. It is initialized from
	 * the base language definition.
	 */
	private String extFileExt;

	/**
	 * File extension of a file in the base language. It is initialized from the
	 * base language definition.
	 */
	private String baseFileExt;

	/**
	 * Name of the nonterminal for toplevel declarations. It is initialized from
	 * the base language definition.
	 */
	private String toplevelDeclarationNonterminal;

	/**
	 * Name of the constructor of a namespace declaration and the argument
	 * number of the namespace identifier. It is initialized from the base
	 * language definition.
	 */
	private Pair<String, Integer> namespaceDecCons;

	/**
	 * Names of the constructors of import declarations and the argument number
	 * of the name referred to. They are initialized from the base language
	 * definition.
	 */
	private Map<String, Integer> importDecCons = new HashMap<String, Integer>();

	/**
	 * Names of the constructors of body declarations. They are initialized from
	 * the base language definition.
	 */
	private Set<String> bodyDecCons = new HashSet<String>();

	/**
	 * Name of the base language. It is initialized from the base language
	 * definition.
	 */
	private String baseLanguageName;

	private SoundXBaseLanguage blInstance; // Reference to instance of language
											// library
	private HybridInterpreter interp; // For pretty printing etc.

	// File names and module name of the SoundX module imported
	// by the generated files
	private final String soundXStrFileName = "org/sugarj/soundx/SoundX.str";
	private final String soundXSdfFileName = "org/sugarj/soundx/SoundX.sdf";
	private final String soundXModuleName = "org/sugarj/soundx/SoundX";

	// Paths of the base language plugin
	private Path binDir;
	private Path srcDir;
	private RelativePath bldPath;

	// Paths of the generated files
	private Path sdfPath;
	private Path strPath;
	private Path servPath;
	private Path defPath;
	private Path ppPath;

	/**
	 * Processes a base language definition.
	 * 
	 * @param bldFilename
	 *            Base language definition to process
	 * @param pluginDirectory
	 *            Directory of the base language plugin
	 */
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
			postProcess(); // Also extracts declarations from Stratego file
			generateDefFile();
			generatePPTable();
		} else {
			Debug.print("Generated files are up-to-date");
			IStrategoTerm strTerm = parseStratego();
			extractDeclarations(strTerm);
		}
		blInstance.ensureFile(soundXStrFileName);
		blInstance.ensureFile(soundXSdfFileName);
		initSoundXBaseLanguage();
	}

	/**
	 * Generate the def file from the sdf file
	 */
	private void generateDefFile() {
		try {
			FileCommands.writeToFile(defPath, "definition\n\n");
			String sdfText = FileCommands.readFileAsString(sdfPath);
			FileCommands.appendToFile(defPath, sdfText);
		} catch (Exception e) {
			externalFail("write generated def file", e);
		}
	}

	/**
	 * Copy the declarations extracted from the base language definition to the
	 * language library.
	 */
	private void initSoundXBaseLanguage() {
		blInstance.setBaseFileExtension(baseFileExt);
		blInstance.setSugarFileExtension(extFileExt);
		blInstance.setLanguageName(baseLanguageName);
		blInstance.setInitEditor(servPath);
		blInstance.setInitGrammar(sdfPath);
		blInstance.setInitTrans(strPath);
		blInstance.setPackagedGrammar(defPath);
		blInstance.setPpTable(ppPath);
		blInstance.setImportDecCons(importDecCons);
		blInstance.setBodyDecCons(bodyDecCons);
		blInstance.setNamespaceDecCons(namespaceDecCons);
	}

	/**
	 * Sets the paths for the generated files in the bin directory of the base
	 * language plugin.
	 */
	private void setGeneratedFilePaths() {
		String sdfFileName = baseLanguageName + ".sdf";
		String strFileName = baseLanguageName + ".str";
		String servFileName = baseLanguageName + ".serv";
		String defFileName = baseLanguageName + ".def";
		String ppFileName = baseLanguageName + ".pp";
		sdfPath = new AbsolutePath(binDir.getAbsolutePath() + Environment.sep
				+ sdfFileName);
		strPath = new AbsolutePath(binDir.getAbsolutePath() + Environment.sep
				+ strFileName);
		servPath = new AbsolutePath(binDir.getAbsolutePath() + Environment.sep
				+ servFileName);
		defPath = new AbsolutePath(binDir.getAbsolutePath() + Environment.sep
				+ defFileName);
		ppPath = new AbsolutePath(binDir.getAbsolutePath() + Environment.sep
				+ ppFileName);
		Debug.print("sdfPath " + sdfPath);
	}

	/**
	 * Set base language name from file name of base language definition.
	 */
	private void setBaseLanguageName() {
		baseLanguageName = FileCommands.fileName(bldPath);
	}

	/**
	 * Post processes SugarJ generated sdf and str file.
	 */
	private void postProcess() {
		postProcessStratego();
		postProcessSdf();
	}

	/**
	 * Post process SugarJ generated str file. The imports are replaced by the
	 * single import of the SoundX module. The declarations like the constructor
	 * names for the language processor are extracted and stored.
	 */
	private void postProcessStratego() {
		IStrategoTerm strTerm = parseStratego();
		extractDeclarations(strTerm);
		// Debug.print("Stratego parse result " + strTerm);
		IStrategoTerm strTermImportsFixed = fixStrategoImports(strTerm);
		String strString = ppStratego(strTermImportsFixed);
		// Debug.print("Post processed Stratego " + strString);
		try {
			FileCommands.writeToFile(strPath, strString);
		} catch (Exception e) {
			externalFail("write post processed Stratego code", e);
		}
	}

	private void extractDeclarations(IStrategoTerm term) {
		IStrategoList decls = (IStrategoList) term.getSubterm(1);
		// Strategies([SDefNoArgs("STLC-ToplevelDeclaration",Build(NoAnnoList(Str("\"ToplevelDec\""))))])
		// Strategies([SDefNoArgs("STLC-body-decs",Build(NoAnnoList(List([NoAnnoList(Str("\"SXCons10\""))]))))])
		// Strategies([SDefNoArgs("STLC-namespace-dec",Build(NoAnnoList(Tuple([NoAnnoList(Str("\"SXCons5\"")),NoAnnoList(Int("2"))]))))])
		// Strategies([SDefNoArgs("STLC-import-decs",Build(NoAnnoList(ListTail([NoAnnoList(Tuple([NoAnnoList(Str("\"SXCons7\"")),NoAnnoList(Int("2"))]))],NoAnnoList(List([NoAnnoList(Tuple([NoAnnoList(Str("\"SXCons6\"")),NoAnnoList(Int("2"))]))]))))))])
		for (IStrategoTerm decl : decls) {
			if (ATermCommands.isApplication(decl, "Strategies")) {
				StrategoList defs = (StrategoList) decl.getSubterm(0);
				IStrategoTerm head = defs.head();
				if (ATermCommands.isApplication(head, "SDefNoArgs")) {
					String name = ((StrategoString) head.getSubterm(0))
							.getName();
					IStrategoTerm rhs = head.getSubterm(1);
					if (name.equals(baseLanguageName + "-ToplevelDeclaration")) {
						// rhs = Build(NoAnnoList(Str("\"ToplevelDec\"")))
						toplevelDeclarationNonterminal = unquote(((StrategoString) rhs
								.getSubterm(0).getSubterm(0).getSubterm(0))
								.getName());
						Debug.print("ToplevelDeclaration = "
								+ toplevelDeclarationNonterminal);
					} else if (name.equals(baseLanguageName
							+ "-extensible-file-ext")) {
						extFileExt = unquote(((StrategoString) rhs
								.getSubterm(0).getSubterm(0).getSubterm(0))
								.getName());
						Debug.print("ExtensibleFileExt = " + extFileExt);
					} else if (name.equals(baseLanguageName + "-base-file-ext")) {
						baseFileExt = unquote(((StrategoString) rhs
								.getSubterm(0).getSubterm(0).getSubterm(0))
								.getName());
						Debug.print("BaseFileExt = " + baseFileExt);
					} else if (name.equals(baseLanguageName + "-body-decs")) {
						// rhs =
						// Build(NoAnnoList(List([NoAnnoList(Str("\"SXCons10\""))])))
						IStrategoTerm current = rhs.getSubterm(0);
						// Debug.print("Conses " + list.toString());

						while (current != null) {
							StrategoAppl listCons = (StrategoAppl) current
									.getSubterm(0); // Unwrap NoAnnoList
							// Debug.print("listCons " + listCons);
							String applConsName = listCons.getConstructor()
									.getName();
							// Debug.print("applConsName " + applConsName);
							if (applConsName.equals("List")) {
								StrategoList elems = (StrategoList) listCons
										.getSubterm(0);
								if (elems.size() == 0) {
									current = null;
								} else if (elems.size() == 1) {
									IStrategoTerm elem = elems.head();
									String consName = unquote(((StrategoString) elem
											.getSubterm(0).getSubterm(0))
											.getName());
									bodyDecCons.add(consName);
									Debug.print("body-dec " + consName);
									current = null;
								} else
									fail("Error reading end of body-decs list");
							} else if (applConsName.equals("ListTail")) {
								StrategoList hd = (StrategoList) listCons
										.getSubterm(0);
								// Debug.print("hd " + hd);
								IStrategoTerm elem = hd.head();
								// Debug.print("elem " + elem);
								String consName = unquote(((StrategoString) elem
										.getSubterm(0).getSubterm(0)).getName());
								// Debug.print("consName " + consName);
								bodyDecCons.add(consName);
								Debug.print("body-dec " + consName);

								current = listCons.getSubterm(1);
							} else
								fail("Error reading body-decs");
						}
					} else if (name.equals(baseLanguageName + "-namespace-dec")) {
						// rhs =
						// Build(NoAnnoList(Tuple([NoAnnoList(Str("\"SXCons5\"")),NoAnnoList(Int("2"))]))))])
						StrategoList tupleComps = (StrategoList) rhs
								.getSubterm(0).getSubterm(0).getSubterm(0);
						IStrategoTerm fstComp = tupleComps.head();
						IStrategoTerm sndComp = tupleComps.tail().head();
						String consName = unquote(((StrategoString) fstComp
								.getSubterm(0).getSubterm(0)).getName());
						Integer index = Integer
								.valueOf(((StrategoString) sndComp
										.getSubterm(0).getSubterm(0)).getName());
						namespaceDecCons = new Pair<String, Integer>(consName,
								index);
						Debug.print("sx-namespace-dec " + consName + ","
								+ index);
					} else if (name.equals(baseLanguageName + "-import-decs")) {
						// rhs =
						// Build(NoAnnoList(ListTail([NoAnnoList(Tuple([NoAnnoList(Str("\"SXCons7\"")),NoAnnoList(Int("2"))]))],NoAnnoList(List([NoAnnoList(Tuple([NoAnnoList(Str("\"SXCons6\"")),NoAnnoList(Int("2"))]))]))
						IStrategoTerm current = rhs.getSubterm(0);
						while (current != null) {
							StrategoAppl listCons = (StrategoAppl) current
									.getSubterm(0); // Unwrap NoAnnoList
							String applConsName = listCons.getConstructor()
									.getName();
							if (applConsName.equals("List")) {
								StrategoList elems = (StrategoList) listCons
										.getSubterm(0);
								if (elems.size() == 0) {
									current = null;
								} else if (elems.size() == 1) {
									IStrategoTerm elem = elems.head();
									StrategoList comps = (StrategoList) elem
											.getSubterm(0).getSubterm(0);
									IStrategoTerm fstComp = comps.head();
									IStrategoTerm sndComp = comps.tail().head();
									String consName = unquote(((StrategoString) fstComp
											.getSubterm(0).getSubterm(0))
											.getName());
									Integer index = Integer
											.valueOf(((StrategoString) sndComp
													.getSubterm(0)
													.getSubterm(0)).getName());
									importDecCons.put(consName, index);
									Debug.print("import-dec " + consName + ","
											+ index);
									current = null;
								} else
									fail("Error reading end of body-decs list");
							} else if (applConsName.equals("ListTail")) {
								StrategoList hd = (StrategoList) listCons
										.getSubterm(0);
								IStrategoTerm elem = hd.head();
								StrategoList comps = (StrategoList) elem
										.getSubterm(0).getSubterm(0);
								IStrategoTerm fstComp = comps.head();
								IStrategoTerm sndComp = comps.tail().head();
								String consName = unquote(((StrategoString) fstComp
										.getSubterm(0).getSubterm(0)).getName());
								Integer index = Integer
										.valueOf(((StrategoString) sndComp
												.getSubterm(0).getSubterm(0))
												.getName());
								importDecCons.put(consName, index);
								Debug.print("import-dec " + consName + ","
										+ index);
								current = listCons.getSubterm(1);
							} else
								fail("Error reading body-decs");
						}
					}
				}
			}
		}
	}

	private String unquote(String name) {
		return name.substring(1, name.length() - 1);
	}

	private void generatePPTable() {
		Context ctx = interp.getCompiledContext();

		ctx.addOperatorRegistry(new CompatLibrary());
		// Necessary because SSL_EXT_topdown_fput is not defined otherwise

		IStrategoTerm sdfTerm = parseSdf();
		try {
			IStrategoTerm sdfTermFixed = ATermCommands.fixSDF(sdfTerm, interp);
			// Without fixSDF, ppgenerate does not recognize the attributes

			IStrategoTerm result = ppgenerate_0_0.instance.invoke(ctx,
					sdfTermFixed);
			String table = ((StrategoString) pp_pp_table_0_0.instance.invoke(
					ctx, result)).getName();
			FileCommands.writeToFile(ppPath, table);
		} catch (Exception e) {
			externalFail("generating and writing the pretty printer table", e);
		}

	}

	private void postProcessSdf() {
		IStrategoTerm sdfTerm = parseSdf();
		// Debug.print("SDF parse result " + sdfTerm);
		IStrategoTerm sdfTermNoImports = fixSdfImports(sdfTerm);
		IStrategoTerm sdfTermWithToplevelDec = fixSdfToplevelDec(sdfTermNoImports);
		IStrategoTerm sdfTermFixed = null;
		try {
			sdfTermFixed = ATermCommands.fixSDF(sdfTermWithToplevelDec, interp);
			// Without fixSDF the attributes are pretty printed wrongly
		} catch (Exception e) {
			externalFail("fixing the attributes of the post process SDF code",
					e);
		}
		Debug.print("Fixed SDF imports");
		String sdfString = ppSdf(sdfTermFixed);
		// Debug.print("Post processed Sdf " + sdfString);
		try {
			FileCommands.writeToFile(sdfPath, sdfString);
		} catch (Exception e) {
			externalFail("writing the post processed SDF code", e);
		}
	}

	private IStrategoTerm fixSdfToplevelDec(IStrategoTerm term) {
		IStrategoTerm header = term.getSubterm(0);
		IStrategoTerm imports = term.getSubterm(1);
		StrategoList body = (StrategoList) term.getSubterm(2);
		String cfSection = "exports(context-free-syntax([prod([sort(\""
				+ baseLanguageName + toplevelDeclarationNonterminal
				+ "\")], sort(\"ToplevelDeclaration\"), no-attrs())]))";
		IStrategoTerm cf = ATermCommands.atermFromString(cfSection);
		IStrategoTerm newBody = new StrategoList(cf, body,
				TermFactory.EMPTY_LIST, body.getStorageType());
		return new StrategoAppl(new StrategoConstructor("module", 3),
				new IStrategoTerm[] { header, imports, newBody },
				term.getAnnotations(), term.getStorageType());
	}

	private IStrategoTerm fixSdfImports(IStrategoTerm term) {
		IStrategoTerm header = term.getSubterm(0);
		IStrategoTerm body = term.getSubterm(2);
		IStrategoTerm common = ATermCommands
				.atermFromString("imports(module(unparameterized(\""
						+ soundXModuleName + "\")))");
		IStrategoTerm imports = new StrategoList(common,
				TermFactory.EMPTY_LIST, TermFactory.EMPTY_LIST,
				term.getStorageType());
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
			fail("Second argument of Stratego module not a list");
		LinkedList<IStrategoTerm> declsNoImports = new LinkedList<IStrategoTerm>();

		for (IStrategoTerm decl : decls) {
			if (decl.getTermType() == IStrategoTerm.APPL) {
				if (!((StrategoAppl) decl).getConstructor().getName()
						.equals("Imports"))
					declsNoImports.addLast(decl);
			} else
				declsNoImports.addLast(decl);
		}

		declsNoImports.addFirst(ATermCommands
				.atermFromString("Imports([Import(\"" + soundXModuleName
						+ "\")])"));
		StrategoList declsTerm = TermFactory.EMPTY_LIST;
		for (Iterator<IStrategoTerm> i = declsNoImports.descendingIterator(); i
				.hasNext();) {
			IStrategoTerm declTerm = i.next();
			declsTerm = new StrategoList(declTerm, declsTerm,
					TermFactory.EMPTY_LIST, decls.getStorageType());
		}

		// Debug.print("Subterm decls " + declsTerm);
		IStrategoTerm trm = new StrategoAppl(new StrategoConstructor("Module",
				2), new IStrategoTerm[] { header, declsTerm },
				term.getAnnotations(), term.getStorageType());
		// Debug.print("Result term " + trm);
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
			externalFail(
					"setting up a parser for the table "
							+ table.getAbsolutePath(), e);
		}

		Object parseResult = null;

		try {
			String fileContent = FileCommands.readFileAsString(inputFile);
			parseResult = parser.parseMax(fileContent,
					sdfPath.getAbsolutePath(), startSymbol);
		} catch (Exception e) {
			externalFail("parsing the file " + inputFile.getAbsolutePath(), e);
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
			externalFail("pretty printing an SDF syntax tree", e);
		}
		return result;
	}

	private String ppStratego(IStrategoTerm term) {
		String result = null;
		try {
			result = SDFCommands.prettyPrintSTR(term, interp);
		} catch (Exception e) {
			externalFail("pretty printing a Stratego syntax tree", e);
		}
		return result;
	}

	private boolean generatedFilesOutdated() {
		if (FileCommands.fileExists(sdfPath)
				&& FileCommands.fileExists(strPath)
				&& FileCommands.fileExists(defPath)
				&& FileCommands.fileExists(servPath)
				&& FileCommands.fileExists(ppPath)) {
			return !(FileCommands.isModifiedLater(sdfPath, bldPath)
					&& FileCommands.isModifiedLater(strPath, bldPath)
					&& FileCommands.isModifiedLater(defPath, bldPath)
					&& FileCommands.isModifiedLater(servPath, bldPath) && FileCommands
						.isModifiedLater(ppPath, bldPath));
		} else
			return true;
	}

	private void runCompiler() {
		IProgressMonitor monitor = new NullProgressMonitor();
		AbstractBaseLanguage baseLang = SXBldLanguage.getInstance();
		Environment environment = new Environment(true, StdLib.stdLibDir,
				Stamper.DEFAULT);

		// DEVEL: Create a fresh cache directory.
		// Otherwise, SugarJ will only reread the SXBld implementation when
		// the version changes. If development has finished
		// the FileCommands.TMP_DIR can be used to speed things up.
		String tmpdir = null;
		try {
			tmpdir = Files.createTempDirectory("sugarj-soundx",
					new FileAttribute[] {}).toString();
		} catch (Exception e) {
			externalFail("creating a temporary directory", e);
		}
		RelativePath cacheDir = new RelativePath(new AbsolutePath(tmpdir),
				"sugarjcache");

		environment.setCacheDir(cacheDir);
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
			externalFail("writing the editor services file", e);
		}
		// Check for errors processing the base language definition
		List<String> errors = result.getCollectedErrors();
		if (errors.size() > 0) {
			String messages = StringCommands.printListSeparated(errors, "\n");
			processingError(messages);
		}
	}

	private void setBinDirFromPluginDirectory(Path pluginDirectory) {
		binDir = new AbsolutePath(pluginDirectory.getAbsolutePath()
				+ Environment.sep + "bin");
	}

	private void setSrcDirFromPluginDirectory(Path pluginDirectory) {
		srcDir = new AbsolutePath(pluginDirectory.getAbsolutePath()
				+ Environment.sep + "src");
	}

	private void fail(String desc) {
		throw new RuntimeException(desc);
	}

	private void externalFail(String desc, Exception e) {
		LinkedList<StackTraceElement> trace = new LinkedList<StackTraceElement>();
		for (StackTraceElement elem : e.getStackTrace())
			trace.add(elem);
		String message = "SoundX base language processing failed unexpectedly.\n"
				+ "While trying to "
				+ desc
				+ ", the following exception occured:\n"
				+ e.toString()
				+ "\n"
				+ "The stack trace leading to this problem is:\n"
				+ StringCommands.printListSeparated(trace, "\n");

		throw new RuntimeException(message);
	}

	private void processingError(String desc) {
		String message = "While processing the base language description for "
				+ baseLanguageName + "\n" + "the following error occured:\n"
				+ desc;
		throw new RuntimeException(message);
	}
}
