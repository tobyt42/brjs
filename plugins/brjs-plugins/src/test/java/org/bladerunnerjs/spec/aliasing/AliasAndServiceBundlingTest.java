package org.bladerunnerjs.spec.aliasing;

import org.bladerunnerjs.api.App;
import org.bladerunnerjs.api.AppConf;
import org.bladerunnerjs.api.Aspect;
import org.bladerunnerjs.api.Blade;
import org.bladerunnerjs.api.Bladeset;
import org.bladerunnerjs.api.JsLib;
import org.bladerunnerjs.api.model.exception.NamespaceException;
import org.bladerunnerjs.api.model.exception.UnresolvableRequirePathException;
import org.bladerunnerjs.api.model.exception.request.ContentFileProcessingException;
import org.bladerunnerjs.api.spec.engine.SpecTest;
import org.bladerunnerjs.api.BladeWorkbench;
import org.bladerunnerjs.model.SdkJsLib;
import org.bladerunnerjs.plugin.bundlers.aliasing.AliasDefinitionsReader;
import org.bladerunnerjs.plugin.bundlers.aliasing.AliasNameIsTheSameAsTheClassException;
import org.bladerunnerjs.plugin.bundlers.aliasing.AliasesReader;
import org.bladerunnerjs.plugin.bundlers.aliasing.AmbiguousAliasException;
import org.bladerunnerjs.plugin.bundlers.aliasing.IncompleteAliasException;
import org.bladerunnerjs.plugin.bundlers.aliasing.UnresolvableAliasException;
import org.bladerunnerjs.plugin.plugins.require.AliasDataSourceModule;
import org.bladerunnerjs.utility.FileUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.ctc.wstx.exc.WstxValidationException;

import static org.bladerunnerjs.plugin.bundlers.aliasing.AliasingUtility.*;


public class AliasAndServiceBundlingTest extends SpecTest
{

	private App app;
	private AppConf appConf;
	private Aspect aspect;
	private Bladeset bladeset;
	private Blade blade;
	private JsLib brLib, otherBrLib;
	private StringBuffer response = new StringBuffer();
	private BladeWorkbench workbench;
	private Bladeset defaultBladeset;
	private Blade bladeInDefaultBladeset;

	private AliasesFileBuilder aspectAliasesFile;
	private AliasDefinitionsFileBuilder aspectResourcesAliaseDefinitionsFile;
	private AliasDefinitionsFileBuilder aspectSrcAliaseDefinitionsFile;
	private AliasDefinitionsFileBuilder bladeAliasDefinitionsFile;
	private AliasDefinitionsFileBuilder brLibAliasDefinitionsFile;
	private AliasesFileBuilder worbenchAliasesFile;
	private JsLib sdkLib;
	private AliasDefinitionsFileBuilder sdkLibAliasDefinitionsFile;
	private SdkJsLib brLocaleLib;
	private AliasesCommander aspectAliasesCommander;
	private AliasesVerifier aspectAliasesVerifier;
	private AliasDefinitionsFileBuilder bladesetResourcesAliasDefinitionsFile;
	private AliasDefinitionsFileBuilder bladeResourcesAliasDefinitionsFile;
	private SdkJsLib servicesLib;

	@Before
	public void initTestObjects() throws Exception
	{
		given(brjs)
			.automaticallyFindsBundlerPlugins()
			.and(brjs).automaticallyFindsMinifierPlugins()			
			.and(brjs).hasBeenCreated();
		app = brjs.app("app1");
		appConf = app.appConf();
		aspect = app.aspect("default");
		bladeset = app.bladeset("bs");
		blade = bladeset.blade("b1");
		workbench = blade.workbench();
		sdkLib = brjs.sdkLib("lib");
		brLib = app.jsLib("br");
		otherBrLib = brjs.sdkLib("otherBrLib");
		defaultBladeset = app.defaultBladeset();
		bladeInDefaultBladeset = defaultBladeset.blade("b1");
		brLocaleLib = brjs.sdkLib("br-locale");

		aspectAliasesCommander = new AliasesCommander(this, aspect);
		aspectAliasesVerifier = new AliasesVerifier(this, aspect);
		
		aspectAliasesFile = new AliasesFileBuilder(this, aliasesFile(aspect));
		aspectResourcesAliaseDefinitionsFile = new AliasDefinitionsFileBuilder(this, aliasDefinitionsFile(aspect, "resources"));
		aspectSrcAliaseDefinitionsFile = new AliasDefinitionsFileBuilder(this, aliasDefinitionsFile(aspect, "src"));
		bladesetResourcesAliasDefinitionsFile = new AliasDefinitionsFileBuilder(this, aliasDefinitionsFile(bladeset, "resources"));
		bladeResourcesAliasDefinitionsFile = new AliasDefinitionsFileBuilder(this, aliasDefinitionsFile(blade, "resources"));
		bladeAliasDefinitionsFile = new AliasDefinitionsFileBuilder(this, aliasDefinitionsFile(blade, "src"));
		worbenchAliasesFile = new AliasesFileBuilder(this, aliasesFile(workbench));
		brLibAliasDefinitionsFile = new AliasDefinitionsFileBuilder(this, aliasDefinitionsFile(brLib, "resources"));
		sdkLibAliasDefinitionsFile = new AliasDefinitionsFileBuilder(this, aliasDefinitionsFile(sdkLib, "resources"));
		
		servicesLib = brjs.sdkLib("ServicesLib");
		given(servicesLib).containsFileWithContents("br-lib.conf", "requirePrefix: br")
			.and(servicesLib).hasClasses("br/AliasRegistry", "br/ServiceRegistry");
	}	

	// SDK AliasDefinitions
	@Test
	public void sdkLibAliasDefinitionsShouldNotGetScannedForDependenciesIfTheClassesAreNotReferencedViaIndexPage() throws Exception
	{
		given(brLib).hasClass("br/Class1")
            .and(brLibAliasDefinitionsFile).hasAlias("br.test-class", "otherBrLib.TestClass")
            .and(otherBrLib).classFileHasContent("otherBrLib.TestClass", "I should not be bundled")
            .and(aspect).indexPageRefersTo("br.Class1");
		when(aspect).requestReceivedInDev("js/dev/combined/bundle.js", response);
		then(response).doesNotContainText("otherBrLib.TestClass")
			.and(response).doesNotContainText("I should not be bundled");
	}

	@Test
	public void sdkLibAliasDefinitionsShouldNotGetScannedForDependenciesIfTheClassesAreNotReferencedViaAspectClass() throws Exception
	{
		given(brLib).hasClass("br/Class1")
            .and(brLibAliasDefinitionsFile).hasAlias("br.alias-class", "otherBrLib.TestClass")
            .and(otherBrLib).classFileHasContent("otherBrLib.TestClass", "I should not be bundled")
            .and(aspect).hasCommonJsPackageStyle()
            .and(aspect).classFileHasContent("appns.Class1", "'br.alias-class'")
            .and(aspect).indexPageRefersTo("br.Class1", "appns.Class1");
		when(aspect).requestReceivedInDev("js/dev/combined/bundle.js", response);
		then(response).doesNotContainText("otherBrLib.TestClass")
			.and(response).doesNotContainText("I should not be bundled");
	}

	@Test
	public void sdkLibAliasDefinitionsReferencesAreBundledIfTheyAreReferencedViaIndexPage() throws Exception
	{
		given(brLib).hasClasses("br/Class1", "br/Class2")
            .and(brLibAliasDefinitionsFile).hasAlias("br.alias", "br.Class2")
            .and(aspect).indexPageHasAliasReferences("br.alias");
		when(aspect).requestReceivedInDev("js/dev/combined/bundle.js", response);
		then(response).containsText("br/Class2");
	}

	@Test
	public void aliasClassesReferencedByANamespacedJsSourceModuleAreIncludedInTheBundle() throws Exception
	{
		given(aspect).hasNamespacedJsPackageStyle()
            .and(aspect).hasClasses("appns.Class", "appns.Class1", "appns.Class2", "appns.Class3")
            .and(aspectAliasesFile).hasAlias("br.pre-export-define-time-alias", "appns.Class1")
            .and(aspectAliasesFile).hasAlias("br.post-export-define-time-alias", "appns.Class2")
            .and(aspectAliasesFile).hasAlias("br.use-time-alias", "appns.Class3")
            .and(aspect).classFileHasContent("appns.Class", "'br.pre-export-define-time-alias' function() {'br.use-time-alias'} module.exports = X; 'br.post-export-define-time-alias'")
            .and(aspect).indexPageRefersTo("appns.Class");
		when(aspect).requestReceivedInDev("js/dev/combined/bundle.js", response);
		then(response).containsNamespacedJsClasses("appns.Class1", "appns.Class2", "appns.Class3");
	}

	@Test
	public void aliasClassesReferencedByACommonJsSourceModuleAreIncludedInTheBundle() throws Exception
	{
		given(brLib).hasClasses("br/Class1", "br/Class2")
            .and(brLibAliasDefinitionsFile).hasAlias("br.alias", "br.Class2")
            .and(aspect).hasCommonJsPackageStyle()
            .and(aspect).classFileHasContent("Class1", "require('alias!br.alias')")
            .and(aspect).indexPageRefersTo("appns.Class1");
		when(aspect).requestReceivedInDev("js/dev/combined/bundle.js", response);
		then(response).containsText("br/Class2");
	}

	@Test
	public void serviceClassesReferencedByACommonJsSourceModuleAreIncludedInTheBundle() throws Exception
	{
		given(brLib).hasClasses("br/Class2")
            .and(brLibAliasDefinitionsFile).hasAlias("br.service", "br.Class2")
            .and(aspect).hasCommonJsPackageStyle()
            .and(aspect).classRequires("Class1", "service!br.service")
            .and(aspect).indexPageRefersTo("appns.Class1");
		when(aspect).requestReceivedInDev("js/dev/combined/bundle.js", response);
		then(response).containsText("br/Class2");
	}

	@Test
	public void incompleteServiceClassesReferencedByACommonJsSourceModuleCauseTheAliasToBeIncludedInTheBundle() throws Exception
	{
		given(brLib).hasClasses("br/Interface")
            .and(brLibAliasDefinitionsFile).hasIncompleteAlias("br.service", "br/Interface")
            .and(aspect).hasCommonJsPackageStyle()
            .and(aspect).classRequires("Class1", "service!br.service")
            .and(aspect).indexPageRefersTo("appns.Class1");
		when(aspect).requestReceivedInDev("js/dev/combined/bundle.js", response);
		then(response).containsText("define('service!br.service',")
			.and(response).doesNotContainText("br/Class2");
	}

	@Test
	// test exception isnt thrown for services - services can be defined and configure at run time, which differs from aliases
	public void anExceptionIsntThrownIfAServiceClassesReferencedByACommonJsSourceModuleDoesntExist() throws Exception
	{
		given(brLib).hasClasses("br/Class1", "br/Class2")
            .and(aspect).hasCommonJsPackageStyle()
            .and(aspect).classRequires("Class1", "service!br.service")
            .and(aspect).indexPageRefersTo("appns.Class1");
		when(aspect).requestReceivedInDev("js/dev/combined/bundle.js", response);
		then(exceptions).verifyNoOutstandingExceptions();
	}

	// -----------------------------------

	@Test
	public void weBundleAClassWhoseAliasIsReferredToInTheIndexPage() throws Exception
	{
		given(aspect).hasClass("appns/Class1")
            .and(aspectAliasesFile).hasAlias("the-alias", "appns.Class1")
            .and(aspect).indexPageHasAliasReferences("the-alias");
		when(aspect).requestReceivedInDev("js/dev/combined/bundle.js", response);
		then(response).containsCommonJsClasses("appns.Class1");
	}

	@Test
	public void weBundleAClassIfItTheAliasReferenceIsInDoubleQuotes() throws Exception
	{
		given(aspect).hasClass("appns/Class1")
            .and(aspectAliasesFile).hasAlias("the-alias", "appns.Class1")
            .and(aspect).indexPageHasContent("\"the-alias\"");
		when(aspect).requestReceivedInDev("js/dev/combined/bundle.js", response);
		then(response).containsCommonJsClasses("appns.Class1");
	}

	@Test
	public void weBundleAClassIfItTheAliasReferenceIsInXmlTag() throws Exception
	{
		given(aspect).hasClass("appns/Class1")
            .and(aspectAliasesFile).hasAlias("the-alias", "appns.Class1")
            .and(aspect).indexPageHasContent("<the-alias attr='val'/>");
		when(aspect).requestReceivedInDev("js/dev/combined/bundle.js", response);
		then(response).containsCommonJsClasses("appns.Class1");
	}

	@Test
	public void weBundleAClassIfItTheAliasReferenceIsInASelfClosingXmlTag() throws Exception
	{
		given(aspect).hasClass("appns/Class1")
            .and(aspectAliasesFile).hasAlias("the-alias", "appns.Class1")
            .and(aspect).indexPageHasContent("<the-alias/>");
		when(aspect).requestReceivedInDev("js/dev/combined/bundle.js", response);
		then(response).containsCommonJsClasses("appns.Class1");
	}

	// Blade/Aspect AliasDefinitions
	@Test
	public void weAlsoBundleAClassIfTheAliasIsDefinedInABladeAliasDefinitionsXml() throws Exception
	{
		given(appConf).hasRequirePrefix("appns")
            .and(aspect).hasClass("appns/Class1")
            .and(bladeAliasDefinitionsFile).hasAlias("appns.bs.b1.the-alias", "appns.Class1")
            .and(aspect).indexPageHasAliasReferences("appns.bs.b1.the-alias");
		when(aspect).requestReceivedInDev("js/dev/combined/bundle.js", response);
		then(response).containsCommonJsClasses("appns.Class1");
	}

	public void weBundleAClassWhoseAliasIsReferredToFromAnotherCommonJsClass() throws Exception
	{
		given(aspect).hasClasses("appns.Class1", "appns.Class2")
            .and(aspectAliasesFile).hasAlias("the-alias", "appns.Class2")
            .and(aspect).indexPageRefersTo("appns.Class1")
            .and(aspect).classFileHasContent("appns.Class1", "aliasRegistry.getAlias('the-alias')");
		when(aspect).requestReceivedInDev("js/dev/combined/bundle.js", response);
		then(response).containsDefinedClasses("appns/Class1", "appns/Class2");
	}

	@Test
	public void weBundleAClassWhoseAliasIsReferredToFromAnotherNamespacedClass() throws Exception
	{
		given(aspect).hasNamespacedJsPackageStyle()
            .and(aspect).hasClasses("appns.Class1", "appns.Class2")
            .and(aspectAliasesFile).hasAlias("the-alias", "appns.Class2")
            .and(aspect).indexPageRefersTo("appns.Class1")
            .and(aspect).classDependsOnAlias("appns.Class1", "the-alias");
		when(aspect).requestReceivedInDev("js/dev/combined/bundle.js", response);
		then(response).containsNamespacedJsClasses("appns.Class1", "appns.Class2");
	}

	@Test
	public void bundlingWorksForAliasesDefinedAtTheBladeLevel() throws Exception
	{
		given(appConf).hasRequirePrefix("appns")
            .and(aspect).hasClass("appns/Class1")
            .and(bladeAliasDefinitionsFile).hasAlias("appns.bs.b1.the-alias", "appns.Class1")
            .and(aspect).indexPageHasAliasReferences("appns.bs.b1.the-alias");
		when(aspect).requestReceivedInDev("js/dev/combined/bundle.js", response);
		then(response).containsCommonJsClasses("appns.Class1");
	}

	@Test
	public void weBundleTheCorrespondingInterfaceForAliasesThatSpecifyAnInterface() throws Exception
	{
		given(aspect).hasClasses("appns/TheClass", "appns/TheInterface")
            .and(bladeAliasDefinitionsFile).hasAlias("appns.bs.b1.the-alias", "appns.TheClass", "appns.TheInterface")
            .and(aspect).indexPageHasAliasReferences("appns.bs.b1.the-alias");
		when(aspect).requestReceivedInDev("js/dev/combined/bundle.js", response);
		then(response).containsCommonJsClasses("appns/TheInterface", "appns/TheClass");
	}

	@Test
	public void weBundleTheDependenciesOfClassesIncludedViaAlias() throws Exception
	{
		given(aspect).hasClasses("appns/Class1", "appns/Class2")
            .and(aspect).classRequires("appns/Class1", "appns/Class2")
            .and(aspectAliasesFile).hasAlias("the-alias", "appns.Class1")
            .and(aspect).indexPageHasAliasReferences("the-alias");
		when(aspect).requestReceivedInDev("js/dev/combined/bundle.js", response);
		then(response).containsCommonJsClasses("appns.Class1", "appns.Class2");
	}

	@Test
	public void weDoNotBundleAClassIfADefinedAliasIsNotReferenced() throws Exception
	{
		given(appConf).hasRequirePrefix("appns")
            .and(aspect).hasClass("appns/Class1")
            .and(bladeAliasDefinitionsFile).hasAlias("appns.bs.b1.the-alias", "appns.Class1");
		when(aspect).requestReceivedInDev("js/dev/combined/bundle.js", response);
		then(response).doesNotContainClasses("appns.Class1");
	}

	@Test
	public void aliasesAreNotSetIfTheAliasRegistryClassDoesntExist() throws Exception
	{
		given(aspect).hasClass("appns/Class1")
            .and(aspectAliasesFile).hasAlias("the-alias", "appns.Class1")
            .and(aspect).indexPageRefersTo("appns.Class1");
		when(aspect).requestReceivedInDev("js/dev/combined/bundle.js", response);
		then(response).doesNotContainText("setAliasData(");
	}

	@Test
	public void aliasesAreNotSetIfTheAliasRegistryClassIsNotUsed() throws Exception
	{
		given(aspect).hasClass("appns/Class1")
        .and(brLib).hasClass("br/AliasRegistry")
        .and(aspect).indexPageRefersTo("appns.Class1");
		when(aspect).requestReceivedInDev("js/dev/combined/bundle.js", response);
		then(response).doesNotContainText("setAliasData(");
	}

	@Test
	public void theAliasBlobIsEmptyIfNoAliasesAreUsed() throws Exception
	{
		given(aspect).classRequires("appns/Class1", AliasDataSourceModule.PRIMARY_REQUIRE_PATH)
            .and(aspectAliasesFile).hasAlias("the-alias", "appns.Class1")
            .and(aspect).indexPageRefersTo("appns.Class1");
		when(aspect).requestReceivedInDev("js/dev/combined/bundle.js", response);
		then(response).containsText("module.exports = {};");
	}

	@Test
	public void theAliasBlobContainsAClassReferencedByAlias() throws Exception
	{
		given(aspect).classRequires("appns/Class1", AliasDataSourceModule.PRIMARY_REQUIRE_PATH)
            .and(aspectAliasesFile).hasAlias("the-alias", "appns.Class1")
            .and(aspect).indexPageHasAliasReferences("the-alias");
		when(aspect).requestReceivedInDev("js/dev/combined/bundle.js", response);
		then(response).containsText("module.exports = {'the-alias':{'class':'appns/Class1','className':'appns.Class1'}};");
	}

	@Test
	public void anExceptionIsThrownIfTheClassReferredToByAnAliasDoesntExist() throws Exception
	{
		given(aspectAliasesFile).hasAlias("the-alias", "NonExistentClass")
			.and(aspect).indexPageHasAliasReferences("the-alias");
		when(aspect).requestReceivedInDev("js/dev/combined/bundle.js", response);
		then(exceptions).verifyException(UnresolvableRequirePathException.class, "NonExistentClass");
	}

	@Test
	public void anExceptionIsThrownIfTheInterfaceReferredToByAnAliasDoesntExist() throws Exception
	{
		given(aspect).hasClass("appns/TheClass")
            .and(bladeAliasDefinitionsFile).hasAlias("appns.bs.b1.the-alias", "appns.TheClass", "NonExistentInterface")
            .and(aspect).indexPageHasAliasReferences("appns.bs.b1.the-alias");
		when(aspect).requestReceivedInDev("js/dev/combined/bundle.js", response);
		then(exceptions).verifyException(UnresolvableRequirePathException.class, "NonExistentInterface");
	}

	@Test
	public void aliasesDefinedInMultipleLocationsDontCauseATrieException() throws Exception
	{
		given(brLib).hasClasses("br/Class1")
            .and(brLibAliasDefinitionsFile).hasAlias("br.alias", "br.Class1")
            .and(aspect).hasClass("appns/Class1")
            .and(aspectAliasesFile).hasAlias("br.alias", "appns.Class1")
            .and(aspect).indexPageRefersTo("aliasRegistry.getAlias('br.alias')")
            .and(blade).hasClass("appns/bs/b1/Class1")
            .and(worbenchAliasesFile).hasAlias("br.alias", "appns/bs/b1/Class1");
		when(aspect).requestReceivedInDev("js/dev/combined/bundle.js", response);
		then(exceptions).verifyNoOutstandingExceptions();
	}

	@Test
	public void multipleAliasDefinitionsCanBeInAnAssetContainer() throws Exception
	{
		AliasDefinitionsFileBuilder pkg1AliasDefinitionsFileBuilder = new AliasDefinitionsFileBuilder(this, aliasDefinitionsFile(aspect, "src/appns/pkg1"));
		AliasDefinitionsFileBuilder pkg2AliasDefinitionsFileBuilder = new AliasDefinitionsFileBuilder(this, aliasDefinitionsFile(aspect, "src/appns/pkg1/pkg2"));
		AliasDefinitionsFileBuilder pkg3AliasDefinitionsFileBuilder = new AliasDefinitionsFileBuilder(this, aliasDefinitionsFile(aspect, "src/appns/pkg1/pkg2/pkg3"));
		given(appConf).hasRequirePrefix("appns")
            .and(aspect).hasClasses("appns/pkg1/Class1", "appns/pkg1/pkg2/Class2", "appns/pkg1/pkg2/pkg3/Class3")
            .and(pkg1AliasDefinitionsFileBuilder).hasAlias("appns.alias1", "appns.pkg1.Class1")
            .and(pkg2AliasDefinitionsFileBuilder).hasAlias("appns.alias2", "appns.pkg1.pkg2.Class2")
            .and(pkg3AliasDefinitionsFileBuilder).hasAlias("appns.alias3", "appns.pkg1.pkg2.pkg3.Class3")
            .and(aspect).indexPageHasAliasReferences("\"appns.alias1\" 'appns.alias2' \"appns.alias3\"");
		when(aspect).requestReceivedInDev("js/dev/combined/bundle.js", response);
		then(response).containsCommonJsClasses("appns.pkg1.Class1", "appns.pkg1.pkg2.Class2", "appns.pkg1.pkg2.pkg3.Class3");
	}

	@Ignore
	// TODO: figure out why the bundle is completely empty
	@Test
	public void unknownClassRepresentingAbstractAliasesIsSetAsANullClass() throws Exception
	{
		given(aspect).indexPageRequires("appns/SomeClass")
            .and(aspect).classRequires("appns/SomeClass", "alias!appns/bs/b1/the-alias")
            .and(bladeAliasDefinitionsFile).hasAlias("appns.bs.b1.the-alias", null, "appns/TheInterface")
            .and(aspect).hasClass("appns/TheInterface")
            .and(brLib).hasClass("br/UnknownClass");
		when(aspect).requestReceivedInDev("js/dev/combined/bundle.js", response);
		then(response).containsText("module.exports = {'appns.bs.b1.the-alias':{'interface':'appns/TheInterface','interfaceName':'appns/TheInterface'}};");
	}

	@Test
	public void multipleAliasDefinitionsCanBeInsideResourcesFolderAndSubfoldersAndUsedInANamespacedClass() throws Exception
	{
		given(aspect).hasNamespacedJsPackageStyle()
            .and(aspect).hasClasses("appns.App", "appns.Class1", "appns.Class2", "appns.Class3")
            .and(aspectResourcesAliaseDefinitionsFile).hasAlias("appns.alias1", "appns.Class1")
            .and(aspect).containsFileWithContents("resources/subfolder/aliasDefinitions.xml", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><aliasDefinitions xmlns=\"http://schema.bladerunnerjs.org/aliasDefinitions\">\n" + "<alias defaultClass=\"appns.Class2\" name=\"appns.alias2\"/>\n" + "</aliasDefinitions>")
            .and(aspect).containsFileWithContents("resources/subfolder/subfolder/aliasDefinitions.xml", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><aliasDefinitions xmlns=\"http://schema.bladerunnerjs.org/aliasDefinitions\">\n" + "<alias defaultClass=\"appns.Class3\" name=\"appns.alias3\"/>\n" + "</aliasDefinitions>")
            .and(aspect).indexPageRefersTo("appns.App")
            .and(aspect).classFileHasContent("appns.App", "'appns.alias1' 'appns.alias2' 'appns.alias3'");
		when(aspect).requestReceivedInDev("js/dev/combined/bundle.js", response);
		then(response).containsNamespacedJsClasses("appns.Class1", "appns.Class2", "appns.Class3");
	}

	@Test
	public void multipleAliasDefinitionsCanBeInsideResourcesFolderAndSubfoldersAndUsedInACommonJsClass() throws Exception
	{
		given(aspect).hasClasses("appns/App", "appns/Class1", "appns/Class2", "appns/Class3")
            .and(aspectResourcesAliaseDefinitionsFile).hasAlias("appns.alias1", "appns.Class1")
            .and(aspect).containsFileWithContents("resources/subfolder/aliasDefinitions.xml", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><aliasDefinitions xmlns=\"http://schema.bladerunnerjs.org/aliasDefinitions\">\n" + "<alias defaultClass=\"appns.Class2\" name=\"appns.alias2\"/>\n" + "</aliasDefinitions>")
            .and(aspect).containsFileWithContents("resources/subfolder/subfolder/aliasDefinitions.xml", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><aliasDefinitions xmlns=\"http://schema.bladerunnerjs.org/aliasDefinitions\">\n" + "<alias defaultClass=\"appns.Class3\" name=\"appns.alias3\"/>\n" + "</aliasDefinitions>")
            .and(aspect).indexPageRequires("appns/App")
            .and(aspect).classFileHasContent("appns.App", "require('service!appns.alias1'); require('service!appns.alias2'); require('service!appns.alias3');");
		when(aspect).requestReceivedInDev("js/dev/combined/bundle.js", response);
		then(response).containsCommonJsClasses("appns.Class1", "appns.Class2", "appns.Class3");
	}

	@Test
	public void anExceptionIsThrownInTheAliasNameIsTheSameAsTheDefaultClass() throws Exception
	{
		given(aspect).hasClasses("appns/Class1")
            .and(aspectSrcAliaseDefinitionsFile).hasAlias("appns.Class1", "appns.Class1")
            .and(aspect).indexPageHasAliasReferences("appns.Class1");
		when(aspect).requestReceivedInDev("js/dev/combined/bundle.js", response);
		then(exceptions).verifyException(AliasNameIsTheSameAsTheClassException.class, "appns.Class1");
	}

	@Test
	public void anExceptionIsThrownInTheAliasNameIsTheSameAsTheAssignedClass() throws Exception
	{
		given(aspect).hasClasses("appns/Class1", "appns/AliasClass")
            .and(aspectSrcAliaseDefinitionsFile).hasAlias("appns.AliasClass", "appns.Class1")
            .and(aspectAliasesFile).hasAlias("appns.AliasClass", "appns.AliasClass")
            .and(aspect).indexPageHasAliasReferences("appns.AliasClass");
		when(aspect).requestReceivedInDev("js/dev/combined/bundle.js", response);
		then(exceptions).verifyException(AliasNameIsTheSameAsTheClassException.class, "appns.AliasClass");
	}

	@Test
	public void theAliasBlobContainsAliasDefinitionsOnlyOnceEvenIfTheyAreReferencedMultipleTimes() throws Exception
	{
		given(aspect).hasClasses("appns/Class1", "appns/Class2", "appns/Class3")
            .and(aspect).classRequires("appns/Class1", "alias!the-alias")
            .and(aspect).classRequires("appns/Class2", "alias!the-alias")
            .and(aspectAliasesFile).hasAlias("the-alias", "appns.Class3")
            .and(aspect).indexPageRequires("appns/Class1", "appns/Class2", "br/AliasRegistry");
		when(aspect).requestReceivedInDev("js/dev/combined/bundle.js", response);
		then(response).containsTextOnce("module.exports = require('br/AliasRegistry').getClass('the-alias')");
	}

	@Test
	public void aliasesInDefaultBladesetCanBeBundled() throws Exception
	{
		given(bladeInDefaultBladeset).hasClasses("appns/b1/BladeClass", "appns/b1/Class1")
            .and(bladeInDefaultBladeset).classRequires("appns/b1/BladeClass", "alias!the-alias")
            .and(aspectAliasesFile).hasAlias("the-alias", "appns.b1.Class1")
            .and(aspect).indexPageRequires("appns/b1/BladeClass");
		when(aspect).requestReceivedInDev("js/dev/combined/bundle.js", response);
		then(response).containsText("module.exports = require('br/AliasRegistry').getClass('the-alias')");
	}

	@Test
	public void weBundleABladeClassWhoseAliasIsReferredToFromAnotherCommonJsBladeClass() throws Exception
	{
		given(blade).hasClasses("appns/bs/b1/Class1", "appns/bs/b1/Class2")
            .and(bladeAliasDefinitionsFile).hasAlias("appns.bs.b1.the-alias", "appns.bs.b1.Class2")
            .and(aspect).indexPageRefersTo("appns.bs.b1.Class1")
            .and(blade).classFileHasContent("appns/bs/b1/Class1", "require('alias!appns.bs.b1.the-alias')");
		when(aspect).requestReceivedInDev("js/dev/combined/bundle.js", response);
		then(response).containsDefinedClasses("appns/bs/b1/Class1", "appns/bs/b1/Class2");
	}

	@Test
	// Note: this test was written in an attempt to exactly replicate a bug we were seeing in the product
	public void workbenchesThatRequestTheDevScenarioArentInsteadGivenANonDevNamedGroupInstead() throws Exception
	{
		given(brLib).hasClasses("br/Interface", "br/DevScenarioClass", "br/GroupProductionClass")
            .and(brLibAliasDefinitionsFile).hasIncompleteAlias("br.service", "br/Interface")
            .and(brLibAliasDefinitionsFile).hasScenarioAlias("dev", "br.service", "br/DevScenarioClass")
            .and(brLibAliasDefinitionsFile).hasGroupAlias("br.g1", "br.service", "br/GroupProductionClass")
            .and(workbench).indexPageRequires("appns/WorkbenchClass")
            .and(workbench).classFileHasContent("appns/WorkbenchClass", "require('service!br.service'); require('br/AliasRegistry');")
            .and(worbenchAliasesFile).usesScenario("dev");
		when(workbench).requestReceivedInDev("js/dev/combined/bundle.js", response);
		then(response).containsCommonJsClasses("br/DevScenarioClass")
            .and(response).doesNotContainClasses("br/GroupProductionClass")
            .and(response).containsOrderedTextFragments("define('br/DevScenarioClass'", "define('alias!br.service',", "module.exports = require('br/AliasRegistry').getClass('br.service')", "define('service!br.service',", "module.preventCaching = true", "module.exports = require('br/ServiceRegistry').getService('br.service');");
	}

	@Test
	public void aliasesRequestedForAWorkbenchArentCachedAndReusedForAnAspect() throws Exception
	{
		given(brLib).hasClasses("br/Class", "br/StubClass")
            .and(aspectAliasesFile).hasAlias("the-alias", "br/Class")
            .and(worbenchAliasesFile).hasAlias("the-alias", "br/StubClass")
            .and(aspect).indexPageRequires("alias!the-alias")
            .and(workbench).indexPageRequires("alias!the-alias")
            .and(workbench).hasReceivedRequest("js/dev/combined/bundle.js");
		when(aspect).requestReceivedInDev("js/dev/combined/bundle.js", response);
		then(response).containsText("module.exports = require('br/AliasRegistry').getClass('the-alias')");
	}

	@Test
	public void aliasesRetrievedViaGetServiceAreBundled() throws Exception
	{
		given(brLib).hasClasses("br/ServiceRegistry")
            .and(aspect).hasNamespacedJsPackageStyle()
            .and(aspectAliasesFile).hasAlias("some.service", "appns.ServiceClass")
            .and(aspect).hasClass("appns.ServiceClass")
            .and(aspect).classFileHasContent("appns.App", "AliasRegistry.getClass('some.service')")
            .and(aspect).indexPageRefersTo("appns.App");
		when(aspect).requestReceivedInDev("js/dev/combined/bundle.js", response);
		then(response).containsText("define('alias!some.service'");
	}

	@Test
	public void servicesRetrievedViaGetServiceAreBundled() throws Exception
	{
		given(aspect).hasNamespacedJsPackageStyle()
            .and(aspectAliasesFile).hasAlias("some.service", "appns.ServiceClass")
            .and(aspect).hasClass("appns.ServiceClass")
            .and(aspect).classDependsOn("appns.App", "ServiceRegistry.getService('some.service')")
            .and(aspect).indexPageRefersTo("appns.App");
		when(aspect).requestReceivedInDev("js/dev/combined/bundle.js", response);
		then(response).containsText("define('alias!some.service'")
			.and(response).containsText("define('alias!some.service'");
	}

	@Test
	public void aliasDataIsIncludedInTheBundleWhenAliasesAreUsedInANamespacedJsAspect() throws Exception
	{
		given(brLib).hasClasses("br/ServiceRegistry")
            .and(aspect).hasNamespacedJsPackageStyle()
            .and(aspectAliasesFile).hasAlias("some.service", "appns.ServiceClass")
            .and(aspect).classFileHasContent("appns.App", "AliasRegistry.getClass('some.service')")
            .and(aspect).hasClass("appns.ServiceClass")
            .and(aspect).indexPageRefersTo("appns.App");
		when(aspect).requestReceivedInDev("js/dev/combined/bundle.js", response);
		then(response).containsText("define('" + AliasDataSourceModule.PRIMARY_REQUIRE_PATH + "'")
			.and(response).containsText("define('alias!some.service'");
	}

	@Test
	public void aliasDataIsIncludedInTheBundleWhenServicesAreUsedInANamespacedJsAspect() throws Exception
	{
		given(aspect).hasNamespacedJsPackageStyle()
            .and(aspectAliasesFile).hasAlias("some.service", "appns.ServiceClass")
            .and(aspect).hasClass("appns.ServiceClass")
            .and(aspect).classFileHasContent("appns.App", "ServiceRegistry.getService('some.service')")
            .and(aspect).indexPageRefersTo("appns.App");
		when(aspect).requestReceivedInDev("js/dev/combined/bundle.js", response);
		then(response).containsText("define('" + AliasDataSourceModule.PRIMARY_REQUIRE_PATH + "'")
			.and(response).containsText("define('alias!some.service'");
	}

	@Test
	public void aliasesAndDataIsIncludedInTheBundleWhenServicesFromACommonJSLibraryAreUsed() throws Exception
	{
		given(brLibAliasDefinitionsFile).hasAlias("br.service", "appns.ServiceClass")
            .and(aspect).hasNamespacedJsPackageStyle()
            .and(aspect).hasClass("appns.ServiceClass")
            .and(aspect).classFileHasContent("appns.App", "ServiceRegistry.getService('br.service')")
            .and(aspect).indexPageRefersTo("appns.App");
		when(aspect).requestReceivedInDev("js/dev/combined/bundle.js", response);
		then(response).containsText("define('" + AliasDataSourceModule.PRIMARY_REQUIRE_PATH + "'")
			.and(response).containsText("define('alias!br.service'");
	}

	@Test
	public void aliasesAndDataIsIncludedInTheBundleWhenServicesFromANamespacedSLibraryAreUsed() throws Exception
	{
		given(brLib).hasNamespacedJsPackageStyle()
            .and(brLib).hasClass("br.ServiceClass")
            .and(brLibAliasDefinitionsFile).hasAlias("br.service", "br.ServiceClass")
            .and(aspect).hasNamespacedJsPackageStyle()
            .and(aspect).classFileHasContent("appns.App", "ServiceRegistry.getService('br.service')")
            .and(aspect).indexPageRefersTo("appns.App");
		when(aspect).requestReceivedInDev("js/dev/combined/bundle.js", response);
		then(response).containsText("define('" + AliasDataSourceModule.PRIMARY_REQUIRE_PATH + "'")
			.and(response).containsText("define('alias!br.service'");
	}

	@Test
	public void aliasesAndDataIsIncludedInTheBundleWhenServicesFromANamespacedJSLibraryAreUsedFromAnotherNamespacedJSLibrary() throws Exception
	{
		given(sdkLib).hasNamespacedJsPackageStyle()
            .and(sdkLib).hasClasses("lib.ServiceRegistry")
            .and(sdkLibAliasDefinitionsFile).hasAlias("lib.service", "lib.ServiceClass")
            .and(sdkLib).hasClass("lib.ServiceClass")
            .and(otherBrLib).hasNamespacedJsPackageStyle()
            .and(otherBrLib).classFileHasContent("otherBrLib.ServiceUser", "ServiceRegistry.getService('lib.service')")
            .and(aspect).hasNamespacedJsPackageStyle()
            .and(aspect).classFileHasContent("appns.App", "otherBrLib.ServiceUser();")
            .and(aspect).indexPageRefersTo("appns.App");
		when(aspect).requestReceivedInDev("js/dev/combined/bundle.js", response);
		then(response).containsText("define('" + AliasDataSourceModule.PRIMARY_REQUIRE_PATH + "'")
            .and(response).containsText("define('alias!lib.service'")
            .and(response).containsText("otherBrLib.ServiceUser();");
	}

	@Test
	public void scenarioAliasesAndDataIsIncludedInTheBundleWhenServicesFromANamespacedJSLibraryAreUsedFromAnotherNamespacedJSLibrary() throws Exception
	{
		given(sdkLib).hasNamespacedJsPackageStyle()
            .and(sdkLib).hasClasses("lib.ServiceRegistry")
            .and(sdkLibAliasDefinitionsFile).hasAlias("lib.service", "lib.ServiceClass")
            .and(sdkLibAliasDefinitionsFile).hasScenarioAlias("dev", "lib.service", "lib.DevServiceClass")
            .and(sdkLib).hasClass("lib.ServiceClass")
            .and(otherBrLib).hasNamespacedJsPackageStyle()
            .and(otherBrLib).classFileHasContent("otherBrLib.ServiceUser", "ServiceRegistry.getService('lib.service')")
            .and(aspect).hasNamespacedJsPackageStyle()
            .and(aspect).classFileHasContent("appns.App", "otherBrLib.ServiceUser();")
            .and(aspect).indexPageRefersTo("appns.App");
		when(aspect).requestReceivedInDev("js/dev/combined/bundle.js", response);
		then(response).containsText("define('" + AliasDataSourceModule.PRIMARY_REQUIRE_PATH + "'")
            .and(response).containsText("define('alias!lib.service'")
            .and(response).containsText("otherBrLib.ServiceUser();");
	}
	
	public void theCorrectAliasIsBundledWhenTheAliasRequiresTheAliasInterface() throws Exception {
		given(brLib).hasClasses("br/AliasClass", "br/AliasInterface", "'br/ServiceRegistry")
			.and(brLibAliasDefinitionsFile).hasAlias("br.the-alias", "br/AliasClass", "br/AliasInterface")
    		.and(aspect).indexPageRequires("alias!br.the-alias")
    		.and(aspect).classRequires("appns/AliasOverride", "br/AliasInterface")
    		.and(aspectAliasesFile).hasAlias("br.the-alias", "appns/AliasOverride");
		when(aspect).requestReceivedInDev("js/dev/combined/bundle.js", response);
		then(response).containsText("define('appns/AliasOverride")
			.and(response).containsText("define('br/AliasInterface")
			.and(response).doesNotContainText("define('br/AliasClass");
	}

	@Test 
	public void theLocaleSwitcherAliasCanBeOverridden() throws Exception {
		brLib = brjs.sdkLib("br");
		brLibAliasDefinitionsFile = new AliasDefinitionsFileBuilder(this, aliasDefinitionsFile(brLib, "resources"));
		given(brLocaleLib).classFileHasContent("switcher", "require('service!br.locale-switcher').switchLocale();")
    		.and(servicesLib).classFileHasContent("br/ServiceRegistry", "require('./AliasRegistry');")
    		.and(servicesLib).classFileHasContent("br/AliasRegistry", "require('" + AliasDataSourceModule.PRIMARY_REQUIRE_PATH + "');")
			.and(brLib).hasClasses("br/services/LocaleSwitcher")
			.and(brLib).classFileHasContent("br/services/BRLocaleLoadingSwitcher", "require('br/services/LocaleSwitcher');")
			.and(brLib).classFileHasContent("br/services/BRLocaleForwardingSwitcher", "require('br/services/LocaleSwitcher');")
			.and(brLibAliasDefinitionsFile).hasAlias("br.locale-switcher", "br/services/BRLocaleLoadingSwitcher", "br/services/LocaleSwitcher")
    		.and(aspectAliasesFile).hasAlias("br.locale-switcher", "br/services/BRLocaleForwardingSwitcher")
    		.and(appConf).supportsLocales("en", "de");
		when(app).requestReceived("", response);
		then(response).containsText("define('br/services/BRLocaleForwardingSwitcher")
			.and(response).doesNotContainText("define('br/services/BRLocaleLoadingSwitcher")
			.and(response).containsText("'br.locale-switcher':{'class':'br/services/BRLocaleForwardingSwitcher'");
	}
	
	
	public void classesForAnOverriddenAliasValueAreNotIncludedIfTheyAreNotUsed() throws Exception {
		given(brLib).hasClasses("br/AliasClassDependency", "br/AliasInterface")
			.and(brLib).classFileHasContent("br/AliasClass", "require('br/AliasDependency'); require('br/AliasInterface');")
			.and(brLibAliasDefinitionsFile).hasAlias("br.the-alias", "br/AliasClass", "br/AliasInterface")
    		.and(aspect).indexPageRequires("alias!br.the-alias")
    		.and(aspect).hasClass("AliasOverrideDepedency")
    		.and(brLib).classFileHasContent("br/AliasOverride", "require('br/AliasOverrideDepedency'); require('br/AliasInterface');")
    		.and(aspectAliasesFile).hasAlias("br.the-alias", "appns/AliasOverride");
		when(aspect).requestReceivedInDev("js/dev/combined/bundle.js", response);
		then(response).containsText("define('appns/AliasOverride")
			.and(response).containsText("define('br/AliasInterface")
			.and(response).containsText("define('br/AliasOverrideDepedency")
			.and(response).doesNotContainText("define('br/AliasClass")
			.and(response).doesNotContainText("define('br/AliasDependency");
	}
	
	
	
	/* Tests moved across from the old AliasModelTest */
	
	@Test
	public void settingMultipleGroupsChangesTheAliasesThatAreUsed() throws Exception {
		given(bladesetResourcesAliasDefinitionsFile).hasGroupAlias("appns.bs.b1.g1", "br.alias1", "appns.bs.Class1")
			.and(bladeResourcesAliasDefinitionsFile).hasGroupAlias("appns.bs.b1.g2", "br.alias2", "appns.bs.Class2")
			.and(bladeset).hasClasses("Class1", "Class2")
			.and(aspectAliasesFile).usesGroups("appns.bs.b1.g1", "appns.bs.b1.g2")
			.and(brLibAliasDefinitionsFile).hasAlias("br.alias1", "br.Class")
			.and(brLibAliasDefinitionsFile).hasAlias("br.alias2", "br.Class")
			.and(aspect).indexPageHasAliasReferences("br.alias1", "br.alias2");
		when(aspect).requestReceivedInDev("js/dev/combined/bundle.js", response);
		then(response).containsText("module.exports = {'br.alias1':{'class':'appns/bs/Class1','className':'appns.bs.Class1'},'br.alias2':{'class':'appns/bs/Class2','className':'appns.bs.Class2'}};");
	}
	
	@Test
	public void theInterfaceIsMaintainedWhenAnAliasIsOverriddenInAGroup() throws Exception {
		given(bladesetResourcesAliasDefinitionsFile).hasAlias("appns.bs.the-alias", "appns.bs.Class1", "appns.bs.TheInterface")
			.and(bladeResourcesAliasDefinitionsFile).hasGroupAlias("appns.bs.b1.g1", "appns.bs.the-alias", "appns.bs.Class2")
			.and(bladeset).hasClasses("Class2", "TheInterface")
			.and(aspectAliasesFile).usesGroups("appns.bs.b1.g1")
			.and(aspect).indexPageHasAliasReferences("appns.bs.the-alias");
		when(aspect).requestReceivedInDev("js/dev/combined/bundle.js", response);
		then(response).containsText("module.exports = {'appns.bs.the-alias':{'class':'appns/bs/Class2','className':'appns.bs.Class2','interface':'appns/bs/TheInterface','interfaceName':'appns.bs.TheInterface'}};");
	}
	
	@Test
	public void groupAliasesCanOverrideNonGroupAliases() throws Exception {
		given(bladesetResourcesAliasDefinitionsFile).hasAlias("appns.bs.b1.the-alias", "appns.bs.Class1")
			.and(bladeResourcesAliasDefinitionsFile).hasGroupAlias("appns.bs.b1.g1", "appns.bs.b1.the-alias", "appns.bs.Class2")
			.and(bladeResourcesAliasDefinitionsFile).hasGroupAlias("appns.bs.b1.g2", "appns.bs.b1.the-alias", "appns.bs.Class3")
			.and(bladeset).hasClass("Class2")
			.and(aspectAliasesFile).usesGroups("appns.bs.b1.g1")
			.and(aspect).indexPageHasAliasReferences("appns.bs.b1.the-alias");
		when(aspect).requestReceivedInDev("js/dev/combined/bundle.js", response);
		then(response).containsText("module.exports = {'appns.bs.b1.the-alias':{'class':'appns/bs/Class2','className':'appns.bs.Class2'}};");
	}
	
	@Test
	public void groupAliasesDoNotNeedToBeNamespaced() throws Exception {
		given(brLibAliasDefinitionsFile).hasAlias("br.the-alias", "br.TheClass")
			.and(bladesetResourcesAliasDefinitionsFile).hasGroupAlias("appns.bs.b1.g1", "br.the-alias", "appns.bs.TheClass")
			.and(aspectAliasesFile).usesGroups("appns.bs.b1.g1")
			.and(bladeset).hasClass("TheClass")
			.and(aspect).indexPageHasAliasReferences("br.the-alias");
		when(aspect).requestReceivedInDev("js/dev/combined/bundle.js", response);
		then(response).containsText("module.exports = {'br.the-alias':{'class':'appns/bs/TheClass','className':'appns.bs.TheClass'}};");
	}
	
	@Test
	public void settingAGroupChangesTheAliasesThatAreUsed() throws Exception {
		given(bladesetResourcesAliasDefinitionsFile).hasGroupAlias("appns.bs.b1.g1", "br.the-alias", "appns.bs.Class1")
			.and(bladeResourcesAliasDefinitionsFile).hasGroupAlias("appns.bs.b1.g2", "br.the-alias", "appns.bs.Class2")
			.and(bladeset).hasClass("appns/bs/Class2")
			.and(aspectAliasesFile).usesGroups("appns.bs.b1.g2")
			.and(brLibAliasDefinitionsFile).hasAlias("br.the-alias", "br.Class")
			.and(aspect).indexPageHasAliasReferences("br.the-alias");
		when(aspect).requestReceivedInDev("js/dev/combined/bundle.js", response);
		then(response).containsText("module.exports = {'br.the-alias':{'class':'appns/bs/Class2','className':'appns.bs.Class2'}};");
	}
	
	@Test
	public void usingGroupsCanLeadToAmbiguity() throws Exception {
		given(bladesetResourcesAliasDefinitionsFile).hasGroupAlias("appns.bs.b1.g1", "br.the-alias", "Class1")
			.and(bladeResourcesAliasDefinitionsFile).hasGroupAlias("appns.bs.b1.g1", "br.the-alias", "Class2")
			.and(aspectAliasesFile).usesGroups("appns.bs.b1.g1")
			.and(brLibAliasDefinitionsFile).hasAlias("br.the-alias", "br.Class")
			.and(aspect).indexPageHasAliasReferences("br.the-alias");
		when(aspect).requestReceivedInDev("js/dev/combined/bundle.js", response);
		then(exceptions).verifyException(AmbiguousAliasException.class, "br.the-alias", aspectAliasesFile.getUnderlyingFilePath());
	}
	
	@Test
	public void usingGroupsCanLeadToAmbiguityEvenWhenASingleGroupIsUsed() throws Exception {
		given(bladesetResourcesAliasDefinitionsFile).hasGroupAlias("appns.bs.b1.g1", "br.the-alias", "Class1")
			.and(bladesetResourcesAliasDefinitionsFile).hasGroupAlias("appns.bs.b1.g1", "br.the-alias", "Class2")
			.and(aspectAliasesFile).usesGroups("appns.bs.b1.g1")
			.and(brLibAliasDefinitionsFile).hasAlias("br.the-alias", "br.Class")
			.and(aspect).indexPageHasAliasReferences("br.the-alias");
		when(aspect).requestReceivedInDev("js/dev/combined/bundle.js", response);
		then(exceptions).verifyException(AmbiguousAliasException.class, "br.the-alias", bladesetResourcesAliasDefinitionsFile.getUnderlyingFilePath());
	}
	
	@Test
	public void usingDifferentGroupsCanLeadToAmbiguity() throws Exception {
		given(bladesetResourcesAliasDefinitionsFile).hasGroupAlias("appns.bs.b1.g1", "br.the-alias", "Class1")
			.and(bladeResourcesAliasDefinitionsFile).hasGroupAlias("appns.bs.b1.g2", "br.the-alias", "Class2")
			.and(aspectAliasesFile).usesGroups("appns.bs.b1.g1", "appns.bs.b1.g2")
			.and(brLibAliasDefinitionsFile).hasAlias("br.the-alias", "br.Class")
			.and(aspect).indexPageHasAliasReferences("br.the-alias");
		when(aspect).requestReceivedInDev("js/dev/combined/bundle.js", response);
		then(exceptions).verifyException(AmbiguousAliasException.class, "br.the-alias", aspectAliasesFile.getUnderlyingFilePath());
	}
	
	@Test
	public void multipleScenariosCanBeDefinedForAnAlias() throws Exception {
		given(bladesetResourcesAliasDefinitionsFile).hasAlias("appns.bs.b1.the-alias", "appns.bs.Class1")
			.and(bladesetResourcesAliasDefinitionsFile).hasScenarioAlias("s1", "appns.bs.b1.the-alias", "appns.bs.Class2")
			.and(bladesetResourcesAliasDefinitionsFile).hasScenarioAlias("s2", "appns.bs.b1.the-alias", "appns.bs.Class3")
			.and(bladesetResourcesAliasDefinitionsFile).hasScenarioAlias("s3", "appns.bs.b1.the-alias", "appns.bs.Class4")
			.and(bladeset).hasClass("Class3")
			.and(aspectAliasesFile).usesScenario("s2")
			.and(aspect).indexPageHasAliasReferences("appns.bs.b1.the-alias");
		when(aspect).requestReceivedInDev("js/dev/combined/bundle.js", response);
		then(response).containsText("module.exports = {'appns.bs.b1.the-alias':{'class':'appns/bs/Class3','className':'appns.bs.Class3'}};");
	}
	
	@Test
	public void aliasDefinitionsDefinedWithinBladesMustBeNamespaced() throws Exception {
		given(bladesetResourcesAliasDefinitionsFile).hasAlias("the-alias", "TheClass")
    		.and(aspect).indexPageHasAliasReferences("the-alias");
    	when(aspect).requestReceivedInDev("js/dev/combined/bundle.js", response);
    	then(exceptions).verifyException(NamespaceException.class, "the-alias", "appns.bs.*");
	}
	
	@Test
	public void theInterfaceIsMaintainedWhenAnAliasIsOverriddenInTheScenario() throws Exception {
		given(bladeResourcesAliasDefinitionsFile).hasAlias("appns.bs.b1.the-alias", "appns.bs.b1.Class1", "appns.bs.b1.TheInterface")
			.and(bladeResourcesAliasDefinitionsFile).hasScenarioAlias("s1", "appns.bs.b1.the-alias", "appns.bs.b1.Class2")
			.and(blade).hasClass("TheInterface")
			.and(blade).hasClass("Class2")
			.and(aspectAliasesFile).usesScenario("s1")
			.and(aspect).indexPageHasAliasReferences("appns.bs.b1.the-alias");
		when(aspect).requestReceivedInDev("js/dev/combined/bundle.js", response);
		then(response).containsText("module.exports = {'appns.bs.b1.the-alias':{'class':'appns/bs/b1/Class2','className':'appns.bs.b1.Class2','interface':'appns/bs/b1/TheInterface','interfaceName':'appns.bs.b1.TheInterface'}};");
	}
	
	@Test
	public void settingTheScenarioChangesTheAliasesThatAreUsed() throws Exception {
		given(bladesetResourcesAliasDefinitionsFile).hasAlias("appns.bs.the-alias", "Class1")
			.and(bladesetResourcesAliasDefinitionsFile).hasScenarioAlias("s1", "appns.bs.the-alias", "appns.bs.Class2")
			.and(bladeset).hasClass("Class2")
			.and(aspectAliasesFile).usesScenario("s1")
			.and(aspect).indexPageHasAliasReferences("appns.bs.the-alias");
		when(aspect).requestReceivedInDev("js/dev/combined/bundle.js", response);
		then(response).containsText("module.exports = {'appns.bs.the-alias':{'class':'appns/bs/Class2','className':'appns.bs.Class2'}};");
	}
	
	@Test
	public void groupsCanContainMultipleAliases() throws Exception {
		given(bladesetResourcesAliasDefinitionsFile).hasGroupAlias("appns.bs.b1.g1", "br.alias1", "appns.bs.Class1")
			.and(bladesetResourcesAliasDefinitionsFile).hasGroupAlias("appns.bs.b1.g1", "br.alias2", "appns.bs.Class2")
			.and(bladeset).hasClasses("Class1", "Class2")
			.and(aspectAliasesFile).usesGroups("appns.bs.b1.g1")
			.and(brLibAliasDefinitionsFile).hasAlias("br.alias1", "br.Class")
			.and(brLibAliasDefinitionsFile).hasAlias("br.alias2", "br.Class")
			.and(aspect).indexPageHasAliasReferences("br.alias1", "br.alias2");
		when(aspect).requestReceivedInDev("js/dev/combined/bundle.js", response);
		then(response).containsText("module.exports = {'br.alias1':{'class':'appns/bs/Class1','className':'appns.bs.Class1'},'br.alias2':{'class':'appns/bs/Class2','className':'appns.bs.Class2'}};");
	}

	
	/* 
	 * Alias Model Testing - TODO: convert these to use the bundlers to test each scenario *
	 */
	@Test
	public void aliasesAreRetrievableViaTheModel() throws Exception {
		given(aspectAliasesFile).hasAlias("the-alias", "TheClass");
		then(aspectAliasesVerifier).hasAlias("the-alias", "TheClass");
	}
	
	@Test
	public void aliasDefinitionsAreRetrievableViaTheModel() throws Exception {
		given(bladesetResourcesAliasDefinitionsFile).hasAlias("appns.bs.b1.the-alias", "TheClass", "TheInterface");
		then(aspectAliasesVerifier).hasAlias("appns.bs.b1.the-alias", "TheClass", "TheInterface");
	}
	
	@Test
	public void nonExistentAliasesAreNotRetrievable() throws Exception {
		when(aspectAliasesCommander).retrievesAlias("no-such-alias");
		then(exceptions).verifyException(UnresolvableAliasException.class, "no-such-alias");
	}
	
	@Test
	public void aliasesOverridesMustDefineAClassName() throws Exception {
		given(aspectAliasesFile).hasAlias("the-alias", null);
		when(aspectAliasesCommander).retrievesAlias("the-alias");
		then(exceptions).verifyException(WstxValidationException.class, doubleQuoted("alias"), doubleQuoted("class"))
			.whereTopLevelExceptionIs(ContentFileProcessingException.class);
	}
	
	// TODO - why does this give an IncompleteAliasException at the aspect level, but the test below it for the blade does not
	@Test
	public void aliasesOverridesMustDefineANonEmptyClassName() throws Exception {
		given(aspectAliasesFile).hasAlias("the-alias", "");
		when(aspectAliasesCommander).retrievesAlias("the-alias");
		then(exceptions).verifyException(IncompleteAliasException.class, "the-alias");
	}
	
	@Test
	public void aliasesCanHaveAnEmptyStringClassReferenceIfTheyProviderAnInterfaceReference() throws Exception {
		given(bladesetResourcesAliasDefinitionsFile).hasAlias("appns.bs.b1.the-alias", "");
		when(aspectAliasesCommander).retrievesAlias("appns.bs.b1.the-alias");
		then(exceptions).verifyNoOutstandingExceptions();
	}
	
	@Test
	public void aliasDefinitionsDefinedWithinBladesetsMustBeNamespaced() throws Exception {
		given(bladesetResourcesAliasDefinitionsFile).hasAlias("the-alias", "TheClass");
		when(aspectAliasesCommander).retrievesAlias("the-alias");
		then(exceptions).verifyException(NamespaceException.class, "the-alias", "appns.bs.*");
	}

	@Test
	public void aliasDefinitionsDefinedWithinBladesetsMustBeCorrectlyNamespaced() throws Exception {
		given(bladesetResourcesAliasDefinitionsFile).hasAlias("appns.bsblah.the-alias", "TheClass");
		when(aspectAliasesCommander).retrievesAlias("appns.bsblah.the-alias");
		then(exceptions).verifyException(NamespaceException.class, "appns.bsblah.the-alias", "appns.bs.*");
	}
	
	@Test
	public void aliasDefinitionsCanBeOverriddenWithinTheAliasesFile() throws Exception {
		given(bladesetResourcesAliasDefinitionsFile).hasAlias("appns.bs.b1.the-alias", "Class1")
			.and(aspectAliasesFile).hasAlias("appns.bs.b1.the-alias", "Class2");
		then(aspectAliasesVerifier).hasAlias("appns.bs.b1.the-alias", "Class2");
	}
	
	@Test
	public void aliasDefinitionsCantBeOverriddenWithinTheBladeset() throws Exception {
		given(bladesetResourcesAliasDefinitionsFile).hasAlias("appns.bs.b1.the-alias", "Class1")
			.and(bladesetResourcesAliasDefinitionsFile).hasAlias("appns.bs.b1.the-alias", "Class2");
		when(aspectAliasesCommander).retrievesAlias("appns.bs.b1.the-alias");
		then(exceptions).verifyException(AmbiguousAliasException.class, "appns.bs.b1.the-alias", bladesetResourcesAliasDefinitionsFile.getUnderlyingFilePath());
	}
	
	@Test
	public void unspecifiedAliasDefinitionsPointToTheUnknownClass() throws Exception {
		given(bladesetResourcesAliasDefinitionsFile).hasAlias("appns.bs.b1.the-alias", null, "TheInterface");
		when(aspectAliasesCommander).retrievesAlias("appns.bs.b1.the-alias");
		then(aspectAliasesVerifier).hasAlias("appns.bs.b1.the-alias", "br.UnknownClass", "TheInterface");
	}
	
	@Test
	public void unusedAliasDefinitionsDoNotNeedToBeMadeConcrete() throws Exception {
		given(bladesetResourcesAliasDefinitionsFile).hasAlias("appns.bs.b1.alias1", "TheClass", "Interface1")
			.and(bladeResourcesAliasDefinitionsFile).hasAlias("appns.bs.b1.alias2", null, "Interface2");
		when(aspectAliasesCommander).retrievesAlias("appns.bs.b1.alias1");
		then(exceptions).verifyNoOutstandingExceptions();
	}
	
	@Test
	public void incompleteAliasDefinitionsCanBeMadeConcreteViaDirectOverride() throws Exception {
		given(bladesetResourcesAliasDefinitionsFile).hasAlias("appns.bs.b1.the-alias", null, "TheInterface")
			.and(aspectAliasesFile).hasAlias("appns.bs.b1.the-alias", "TheClass");
		when(aspectAliasesCommander).retrievesAlias("appns.bs.b1.the-alias");
		then(exceptions).verifyNoOutstandingExceptions();
	}
	
	@Test
	public void incompleteAliasDefinitionsCanBeMadeConcreteUsingGroups() throws Exception {
		given(bladesetResourcesAliasDefinitionsFile).hasAlias("appns.bs.b1.the-alias", null, "TheInterface")
			.and(bladeResourcesAliasDefinitionsFile).hasGroupAlias("appns.bs.b1.g1", "appns.bs.b1.the-alias", "TheClass")
			.and(aspectAliasesFile).usesGroups("appns.bs.b1.g1");
		when(aspectAliasesCommander).retrievesAlias("appns.bs.b1.the-alias");
		then(exceptions).verifyNoOutstandingExceptions();
	}
	
	@Test
	public void theNonScenarioAliasIsUsedByDefault() throws Exception {
		given(bladesetResourcesAliasDefinitionsFile).hasAlias("appns.bs.b1.the-alias", "Class1")
			.and(bladeResourcesAliasDefinitionsFile).hasScenarioAlias("s1", "appns.bs.b1.the-alias", "Class2");
		then(aspectAliasesVerifier).hasAlias("appns.bs.b1.the-alias", "Class1");
	}
	
	@Test
	public void aliasesCanStillBeOverriddenWhenTheScenarioIsSet() throws Exception {
		given(bladesetResourcesAliasDefinitionsFile).hasAlias("appns.bs.b1.the-alias", "Class1")
			.and(bladeResourcesAliasDefinitionsFile).hasScenarioAlias("s1", "appns.bs.b1.the-alias", "Class2")
			.and(aspectAliasesFile).usesScenario("s1")
			.and(aspectAliasesFile).hasAlias("appns.bs.b1.the-alias", "Class3");
		then(aspectAliasesVerifier).hasAlias("appns.bs.b1.the-alias", "Class3");
	}
	
	@Test
	public void scenarioAliasesAreAlsoNamespaced() throws Exception {
		given(bladesetResourcesAliasDefinitionsFile).hasScenarioAlias("s1", "the-alias", "Class2")
			.and(bladeResourcesAliasDefinitionsFile).hasAlias("the-alias", "Class1")
			.and(aspectAliasesFile).usesScenario("s1");
		when(aspectAliasesCommander).retrievesAlias("the-alias");
		then(exceptions).verifyException(NamespaceException.class, "the-alias", "appns.bs.b1.*");
	}
	
	@Test
	public void theNonGroupAliasIsUsedByDefault() throws Exception {
		given(bladesetResourcesAliasDefinitionsFile).hasAlias("appns.bs.b1.the-alias", "Class1")
			.and(bladeResourcesAliasDefinitionsFile).hasGroupAlias("appns.bs.b1.g1", "appns.bs.b1.the-alias", "Class2");
		then(aspectAliasesVerifier).hasAlias("appns.bs.b1.the-alias", "Class1");
	}
	
	@Test
	public void aliasesCanStillBeOverriddenWhenAGroupIsSet() throws Exception {
		given(bladesetResourcesAliasDefinitionsFile).hasGroupAlias("appns.bs.b1.g1", "appns.bs.b1.the-alias", "Class1")
			.and(aspectAliasesFile).usesGroups("appns.bs.b1.g1")
			.and(aspectAliasesFile).hasAlias("appns.bs.b1.the-alias", "Class2");
		then(aspectAliasesVerifier).hasAlias("appns.bs.b1.the-alias", "Class2");
	}
	
	@Test
	public void groupIdentifiersMustBeNamespaced() throws Exception {
		given(bladeResourcesAliasDefinitionsFile).hasGroupAlias("g1", "the-alias", "TheClass");
		when(aspectAliasesCommander).retrievesAlias("the-alias");
		then(exceptions).verifyException(NamespaceException.class, "g1", "appns.bs.b1.*");
	}
	
	@Test
	public void theInterfaceIsMaintainedWhenAnAliasIsOverriddenInAliasesFile() throws Exception {
		given(bladesetResourcesAliasDefinitionsFile).hasAlias("appns.bs.b1.the-alias", "Class1", "TheInterface")
			.and(aspectAliasesFile).hasAlias("appns.bs.b1.the-alias", "Class2");
		then(aspectAliasesVerifier).hasAlias("appns.bs.b1.the-alias", "Class2", "TheInterface");
	}
	
	@Test
	public void nestedAliasDefinitionsFilesCanBeUsedInResourcesDirectories() throws Exception {
		// TODO: think of a way of doing this in a more BDD way
		FileUtils.write(blade, blade.file("resources/aliasDefinitions.xml"), "<aliasDefinitions xmlns='http://schema.bladerunnerjs.org/aliasDefinitions'/>");
		FileUtils.write(blade, blade.file("resources/dir/aliasDefinitions.xml"), "<aliasDefinitions xmlns='http://schema.bladerunnerjs.org/aliasDefinitions'/>");
		AliasDefinitionsFileBuilder nestedbladeResourcesAliasDefinitionsFileBuilder = new AliasDefinitionsFileBuilder(this, aliasDefinitionsFile(blade, "resources"));
		
		given(bladesetResourcesAliasDefinitionsFile).hasAlias("appns.bs.b1.alias1", "Class1", "TheInterface")
			.and(nestedbladeResourcesAliasDefinitionsFileBuilder).hasAlias("appns.bs.b1.alias2", "Class2", "TheInterface");
		then(aspectAliasesVerifier).hasAlias("appns.bs.b1.alias1", "Class1", "TheInterface")
			.and(aspectAliasesVerifier).hasAlias("appns.bs.b1.alias2", "Class2", "TheInterface");
	}
	
	
	/* 
	 * End Alias Model Testing *
	 */
	
	@Test
	public void testPackBundlesCanBeCreatedForAspectDefaultTechTestsWhereAliasRegistryIsUsed() throws Exception {
		given(aspect).classRequires("appns/Class1", AliasDataSourceModule.PRIMARY_REQUIRE_PATH)
			.and( aspect.testType("unit").defaultTestTech() ).hasNamespacedJsPackageStyle()
			.and( aspect.testType("unit").defaultTestTech() ).testRefersTo("pkg/test.js", "appns.Class1")
			.and( aspect.testType("unit").defaultTestTech() ).containsResourceFileWithContents("en.properties", "appns.prop=val")
			.and(sdkLib).hasClass("br/AliasRegistry");
		when( aspect.testType("unit").defaultTestTech() ).requestReceivedInDev("js/dev/combined/bundle.js", response);
		then(response).containsText("define('" + AliasDataSourceModule.PRIMARY_REQUIRE_PATH + "'")
			.and(response).containsText("define('appns/Class1'");
	}
	
	@Test
	public void theLegacySchemaCaplinComXmlnsCanBeUsed() throws Exception
	{
		given(aspect).hasNamespacedJsPackageStyle()
	    	.and(aspect).hasClasses("appns.App", "appns.Class1", "appns.Class2")
	    	.and(aspect).containsFileWithContents("resources/aliases.xml", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><aliases xmlns=\"http://schema.caplin.com/CaplinTrader/aliases\">\n" + 
	    				"<alias class=\"appns.Class1\" name=\"appns.alias1\"/>\n" + 
	    			"</aliases>")
    	    .and(aspect).containsFileWithContents("resources/aliasDefinitions.xml", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><aliasDefinitions xmlns=\"http://schema.caplin.com/CaplinTrader/aliasDefinitions\">\n" + 
	    				"<alias defaultClass=\"appns.Class2\" name=\"appns.alias2\"/>\n" + 
    	    		"</aliasDefinitions>")
    	    .and(aspect).indexPageRefersTo("appns.App")
    	    .and(aspect).classFileHasContent("appns.App", "'appns.alias1' 'appns.alias2'");
		when(aspect).requestReceivedInDev("js/dev/combined/bundle.js", response);
		then(response).containsNamespacedJsClasses("appns.Class1", "appns.Class2");
	}
	
	@Test
	public void aMixOfLegacyAndNewNamespacesCanBeUsed() throws Exception
	{
		given(aspect).hasNamespacedJsPackageStyle()
	    	.and(aspect).hasClasses("appns.App", "appns.Class1", "appns.Class2", "appns.Class3")
	    	.and(aspect).containsFileWithContents("resources/aliases.xml", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><aliases xmlns=\"http://schema.caplin.com/CaplinTrader/aliases\">\n" + 
	    				"<alias class=\"appns.Class1\" name=\"appns.alias1\"/>\n" + 
	    			"</aliases>")
    	    .and(aspect).containsFileWithContents("resources/aliasDefinitions.xml", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><aliasDefinitions xmlns=\"http://schema.caplin.com/CaplinTrader/aliasDefinitions\">\n" + 
	    				"<alias defaultClass=\"appns.Class2\" name=\"appns.alias2\"/>\n" + 
    	    		"</aliasDefinitions>")
	    	.and(aspect).containsFileWithContents("resources/foo/aliasDefinitions.xml", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><aliasDefinitions xmlns=\"http://schema.caplin.com/CaplinTrader/aliasDefinitions\">\n" + 
    				"<alias defaultClass=\"appns.Class3\" name=\"appns.alias3\"/>\n" + 
	    		"</aliasDefinitions>")
    	    .and(aspect).indexPageRefersTo("appns.App")
    	    .and(aspect).classFileHasContent("appns.App", "'appns.alias1' 'appns.alias2' 'appns.alias3'");
		when(aspect).requestReceivedInDev("js/dev/combined/bundle.js", response);
		then(response).containsNamespacedJsClasses("appns.Class1", "appns.Class2", "appns.Class3");
	}
	
	@Test
	public void aWarningIsLoggedWhenLegacyNamespacesAreUsed() throws Exception
	{
		given(aspect).hasNamespacedJsPackageStyle()
	    	.and(aspect).hasClasses("appns.App", "appns.Class1", "appns.Class2")
	    	.and(aspect).containsFileWithContents("resources/aliases.xml", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><aliases xmlns=\"http://schema.caplin.com/CaplinTrader/aliases\">\n" + 
	    				"<alias class=\"appns.Class1\" name=\"appns.alias1\"/>\n" + 
	    			"</aliases>")
    	    .and(aspect).containsFileWithContents("resources/aliasDefinitions.xml", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><aliasDefinitions xmlns=\"http://schema.caplin.com/CaplinTrader/aliasDefinitions\">\n" + 
	    				"<alias defaultClass=\"appns.Class2\" name=\"appns.alias2\"/>\n" + 
    	    		"</aliasDefinitions>")
    	    .and(aspect).indexPageRefersTo("appns.App")
    	    .and(aspect).classFileHasContent("appns.App", "'appns.alias1' 'appns.alias2'")
    	    .and(logging).enabled();
		when(aspect).requestReceivedInDev("js/dev/combined/bundle.js", response);
		then(response).containsNamespacedJsClasses("appns.Class1", "appns.Class2")
			.and(logging).warnMessageReceived(AliasesReader.LEGACY_XMLNS_WARN_MSG, "apps/app1/default-aspect/resources/aliases.xml")
			.and(logging).warnMessageReceived(AliasDefinitionsReader.LEGACY_XMLNS_WARN_MSG, "apps/app1/default-aspect/resources/aliasDefinitions.xml");
	}
	
}
