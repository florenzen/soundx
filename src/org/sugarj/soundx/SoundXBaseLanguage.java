package org.sugarj.soundx;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.spoofax.interpreter.terms.IStrategoTerm;
import org.sugarj.AbstractBaseLanguage;
import org.sugarj.AbstractBaseProcessor;
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
	private Set<Pair<String, Integer>> importDecCons;
	private Set<String> bodyDecCons;

	protected void setNamespaceDecCons(Pair<String, Integer> namespaceDecCons) {
		this.namespaceDecCons = namespaceDecCons;
	}

	protected void setImportDecCons(Set<Pair<String, Integer>> importDecCons) {
		this.importDecCons = importDecCons;
	}

	protected void setBodyDecCons(Set<String> bodyDecCons) {
		this.bodyDecCons = bodyDecCons;
	}

	private SoundXBaseLanguage() {}
	
	private static SoundXBaseLanguage instance = new SoundXBaseLanguage();

	public static SoundXBaseLanguage getInstance() {
		return instance;
	}
	
	public void processBaseLanguageDefinition(String bldFilename, Path pluginDirectory) {
		Debug.print("Processing " + bldFilename);
		Debug.print("Plugin directory " + pluginDirectory.toString());
		
		BaseLanguageDefinition bld = BaseLanguageDefinition.getInstance();
		bld.process(bldFilename, pluginDirectory);
	}

	@Override
	public SoundXBaseProcessor createNewProcessor() {
		Log.log.setLoggingLevel(Log.ALWAYS);
		return new SoundXBaseProcessor();
	}

	@Override
	public String getVersion() {
		return "0.1";
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
		return false;
	}

	@Override
	public boolean isExtensionDecl(IStrategoTerm decl) {
		return false;
	}

	@Override
	public boolean isImportDecl(IStrategoTerm decl) {
		return false;
	}

	@Override
	public boolean isBaseDecl(IStrategoTerm decl) {
		return false;
	}

	@Override
	public boolean isPlainDecl(IStrategoTerm decl) {
		return false;
	}	
}
