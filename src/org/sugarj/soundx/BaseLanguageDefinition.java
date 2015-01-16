package org.sugarj.soundx;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
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

public class BaseLanguageDefinition {
	private static BaseLanguageDefinition instance = new BaseLanguageDefinition();

	public static BaseLanguageDefinition getInstance() {
		return instance;
	}

	private BaseLanguageDefinition() {
		interp = new HybridInterpreter();
	}

	private String toplevelDeclarationNonterminal;
	private Pair<String, Integer> namespaceDecCons;
	private Map<String, Integer> importDecCons = new HashMap<String, Integer>();
	private Set<String> bodyDecCons = new HashSet<String>();
	private String baseLanguageName;

	private final String soundXStrFileName = "org/sugarj/soundx/SoundX.str";
	private final String soundXSdfFileName = "org/sugarj/soundx/SoundX.sdf";
	private final String soundXModuleName = "org/sugarj/soundx/SoundX";
	private HybridInterpreter interp;
	private Path binDir;
	private Path srcDir;
	private RelativePath bldPath;
	private Path sdfPath;
	private Path strPath;
	private Path servPath;
	private Path defPath;
	private Path ppPath;
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

	private void generateDefFile() {
		try {
			FileCommands.writeToFile(defPath, "definition\n\n");
			String sdfText = FileCommands.readFileAsString(sdfPath);
			FileCommands.appendToFile(defPath, sdfText);
		} catch (Exception e) {
			throw new RuntimeException(e.toString());
		}
	}

	private void initSoundXBaseLanguage() {
		blInstance.setBaseFileExtension("st");
		blInstance.setSugarFileExtension("xst");
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

	private void setBaseLanguageName() {
		baseLanguageName = FileCommands.fileName(bldPath);
	}

	private void postProcess() {
		postProcessStratego();
		postProcessSdf();
	}

	private void postProcessStratego() {
		IStrategoTerm strTerm = parseStratego();
		extractDeclarations(strTerm);
		// Debug.print("Stratego parse result " + strTerm);
		IStrategoTerm strTermNoImports = fixStrategoImports(strTerm);
		String strString = ppStratego(strTermNoImports);
		// Debug.print("Post processed Stratego " + strString);
		try {
			FileCommands.writeToFile(strPath, strString);
		} catch (Exception e) {
			throw new RuntimeException(e.toString());
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
									throw new RuntimeException(
											"Error reading end of body-decs list");
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
								throw new RuntimeException(
										"Error reading body-decs");
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
									throw new RuntimeException(
											"Error reading end of body-decs list");
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
								throw new RuntimeException(
										"Error reading body-decs");
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
			throw new RuntimeException(e.toString());
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
			new RuntimeException(e.toString());
		}
		Debug.print("Deleted imports");
		String sdfString = ppSdf(sdfTermFixed);
		// Debug.print("Post processed Sdf " + sdfString);
		try {
			FileCommands.writeToFile(sdfPath, sdfString);
		} catch (Exception e) {
			throw new RuntimeException(e.toString());
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
