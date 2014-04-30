package org.bladerunnerjs.model;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.bladerunnerjs.aliasing.NamespaceException;
import org.bladerunnerjs.aliasing.aliasdefinitions.AliasDefinitionsFile;
import org.bladerunnerjs.memoization.MemoizedValue;
import org.bladerunnerjs.model.engine.Node;
import org.bladerunnerjs.model.engine.RootNode;
import org.bladerunnerjs.model.exception.InvalidRequirePathException;
import org.bladerunnerjs.model.exception.RequirePathException;
import org.bladerunnerjs.model.exception.UnresolvableRelativeRequirePathException;
import org.bladerunnerjs.model.exception.modelupdate.ModelUpdateException;
import org.bladerunnerjs.plugin.AssetPlugin;
import org.bladerunnerjs.utility.JsStyleUtility;
import org.bladerunnerjs.utility.NamespaceUtility;
import org.bladerunnerjs.utility.RelativePathUtility;

public class AbstractShallowAssetLocation extends InstantiatedBRJSNode implements AssetLocation {
	protected final AssetContainer assetContainer;
	private AliasDefinitionsFile aliasDefinitionsFile;
	private final Map<String, SourceModule> sourceModules = new HashMap<>();
	protected final AssetLocationUtility assetLocator;
	private List<AssetLocation> dependentAssetLocations = new ArrayList<>();
	
	private final MemoizedValue<String> requirePrefix;
	private final MemoizedValue<String> jsStyle = new MemoizedValue<>("AssetLocation.jsStyle", root(), dir());
	private final MemoizedValue<List<LinkedAsset>> seedResourcesList = new MemoizedValue<>("AssetLocation.seedResources", root(), root().dir());
	
	public AbstractShallowAssetLocation(RootNode rootNode, Node parent, File dir, AssetLocation... dependentAssetLocations)
	{
		this(rootNode, parent, dir);
		this.dependentAssetLocations.addAll( Arrays.asList(dependentAssetLocations) );
	}
	
	public AbstractShallowAssetLocation(RootNode rootNode, Node parent, File dir)
	{
		super(rootNode, parent, dir);
		this.assetContainer = (AssetContainer) parent;
		assetLocator = new AssetLocationUtility(this);
		requirePrefix = new MemoizedValue<>("AssetLocation.requirePrefix", root(), dir(), assetContainer.app().file("app.conf"), root().conf().file("bladerunner.conf"));
	}
	
	@Override
	public String jsStyle() {
		return jsStyle.value(() -> {
			return JsStyleUtility.getJsStyle(dir());
		});
	}
	
	@Override
	public String requirePrefix() {
		return requirePrefix.value(() -> {
			String relativeRequirePath = RelativePathUtility.get(assetContainer.dir(), dir());
			
			return assetContainer.requirePrefix() + "/" + relativeRequirePath;
		});
	}
	
	@Override
	public void assertIdentifierCorrectlyNamespaced(String identifier) throws NamespaceException, RequirePathException {
		String namespace = NamespaceUtility.convertToNamespace(requirePrefix());
		
		if(assetContainer.isNamespaceEnforced() && !identifier.startsWith(namespace)) {
			throw new NamespaceException( "The identifier '" + identifier + "' is not correctly namespaced.\nNamespace '" + namespace + ".*' was expected.");
		}
	}
	
	@Override
	public SourceModule sourceModule(String requirePath) throws RequirePathException
	{
		String canonicalRequirePath = canonicaliseRequirePath(requirePrefix(), requirePath);
		
		if (sourceModules.containsKey(requirePath))
		{
			return sourceModules.get(requirePath);
		}
		
		SourceModule sourceModule = findSourceModuleWithRequirePath(assetContainer().app().getAllAssetContainers(), canonicalRequirePath);
		
		if (sourceModule != null)
		{
			sourceModules.put(requirePath, sourceModule);
			return sourceModule;
		}
	
		throw new InvalidRequirePathException("Unable to find SourceModule for require path '"+requirePath+"'. It either does not exist or it is outside of the scope for this request.");
	}

	private SourceModule findSourceModuleWithRequirePath(List<AssetContainer> assetContainers, String requirePath)
	{
		for (AssetContainer assetContainer : assetContainers)
		{
			for (SourceModule sourceModule : assetContainer.sourceModules())
			{
				if (sourceModule.getRequirePath().equals(requirePath))
				{
					sourceModules.put(requirePath, sourceModule);
					return sourceModule;
				}
			}
		}
		return null;
	}
	
	@Override
	public AliasDefinitionsFile aliasDefinitionsFile() {		
		if(aliasDefinitionsFile == null) {
			aliasDefinitionsFile = new AliasDefinitionsFile(assetContainer, dir(), "aliasDefinitions.xml");
		}
		
		return aliasDefinitionsFile;
	}
		
	@Override
	public List<LinkedAsset> seedResources() {
		return seedResourcesList.value(() -> {
			List<LinkedAsset> seedResources = new LinkedList<LinkedAsset>();
			
			for(AssetPlugin assetPlugin : root().plugins().assetProducers()) {
				seedResources.addAll(assetPlugin.getLinkedAssets(this));
			}
			
			return seedResources;
		});
	}
	
	public List<Asset> bundleResources(AssetPlugin assetPlugin) {
		List<Asset> assets = new ArrayList<>(assetPlugin.getAssets(this));
		assets.addAll(assetPlugin.getLinkedAssets(this));
		
		return assets;
	}

	@Override
	public AssetContainer assetContainer()
	{
		return assetContainer;
	}

	@Override
	public List<AssetLocation> dependentAssetLocations()
	{
		return dependentAssetLocations;
	}
	
	@Override
	public void addTemplateTransformations(Map<String, String> transformations) throws ModelUpdateException {
		// do nothing
	}
	
	@Override
	public <A extends Asset> A obtainAsset(Class<? extends A> assetClass, File dir, String assetName) throws AssetFileInstantationException {
		if(!new File(dir, assetName).getParentFile().equals(dir())) {
			// TODO: this needs to be tested
			throw new AssetFileInstantationException("'" + assetName + "' can only point to a logical resource within the directory '" + dir + "'.");
		}
		
		return assetLocator.obtainAsset(assetClass, dir, assetName);
	}
	
	@Override
	public <A extends Asset> List<A> obtainMatchingAssets(AssetFilter assetFilter, Class<A> assetListClass, Class<? extends A> assetClass) throws AssetFileInstantationException {
		List<A> assets = new ArrayList<>();
		
		if(dir.isDirectory()) {
			addMatchingAssets(dir, assetFilter, assetClass, assets);
		}
		
		return assets;
	}
	
	protected <A extends Asset> void addMatchingAssets(File dir, AssetFilter assetFilter, Class<? extends A> assetClass, List<A> assets) throws AssetFileInstantationException {
		for(File file : root().getFileInfo(dir).files()) {
			if(assetFilter.accept(file.getName())) {
				assets.add(obtainAsset(assetClass, file.getParentFile(), file.getName()));
			}
		}
	}
	
	private String canonicaliseRequirePath(String requirePrefix, String requirePath) throws RequirePathException
	{
		List<String> requirePrefixParts = new LinkedList<String>( Arrays.asList(requirePrefix.split("/")) );
		List<String> requirePathParts = new LinkedList<String>( Arrays.asList(requirePath.split("/")) );
		
		if (!requirePath.contains("../") && !requirePath.contains("./"))
		{
			return requirePath;
		}
		
		Iterator<String> requirePathPartsIterator = requirePathParts.iterator();
		while(requirePathPartsIterator.hasNext())
		{
			String pathPart = requirePathPartsIterator.next();
			switch (pathPart)
			{
				case ".":
					requirePathPartsIterator.remove();
					break;
				case "..":
					requirePathPartsIterator.remove();
					if (requirePrefixParts.size() > 0)
					{
						requirePrefixParts.remove( requirePrefixParts.size()-1 );						
					}
					else
					{
						String msg = String.format("Unable to continue up to parent require path, no more parents remaining. Require path of container was '%s', relative require path was '%s'", requirePrefix, requirePath);
						throw new UnresolvableRelativeRequirePathException(msg);
					}
					break;
				default:
					break;
			}
		}
		
		return StringUtils.join(requirePrefixParts, "/") + "/" + StringUtils.join(requirePathParts, "/");
	}
}
