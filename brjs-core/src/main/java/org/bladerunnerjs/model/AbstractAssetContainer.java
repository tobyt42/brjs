package org.bladerunnerjs.model;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.bladerunnerjs.core.plugin.bundler.BundlerPlugin;
import org.bladerunnerjs.model.engine.Node;
import org.bladerunnerjs.model.engine.NodeItem;
import org.bladerunnerjs.model.engine.RootNode;

public abstract class AbstractAssetContainer extends AbstractBRJSNode implements AssetContainer {
	private final NodeItem<DirNode> src = new NodeItem<>(DirNode.class, "src");
	private final NodeItem<DirNode> resources = new NodeItem<>(DirNode.class, "resources");
	protected final AssetContainerLocations assetContainerLocations;
	
	public AbstractAssetContainer(RootNode rootNode, File dir) {
		init(rootNode, rootNode, dir);
		
		assetContainerLocations = new AssetContainerLocations(this, src().dir(), resources().dir());
	}
	
	public DirNode src() {
		return item(src);
	}
	
	public DirNode resources()
	{
		return item(resources);
	}
	
	@Override
	public App getApp() {
		Node node = this.parentNode();
		
		while(!(node instanceof App)) {
			node = node.parentNode();
		}
		
		return (App) node;
	}
	
	@Override
	public List<SourceFile> sourceFiles() {
		List<SourceFile> sourceFiles = new ArrayList<SourceFile>();
			
		for(BundlerPlugin bundlerPlugin : ((BRJS) rootNode).bundlerPlugins()) {
			for (AssetLocation assetLocation : getAllAssetLocations())
			{
				sourceFiles.addAll(bundlerPlugin.getAssetFileAccessor().getSourceFiles(assetLocation));
			}
		}
		
		return sourceFiles;
	}
	
	@Override
	public SourceFile sourceFile(String requirePath) {
		for(SourceFile sourceFile : sourceFiles()) {
			if(sourceFile.getRequirePath().equals(requirePath)) {
				return sourceFile;
			}
		}
		
		return null;
	}
	
	@Override
	public List<AssetLocation> getAllAssetLocations() {
		return assetContainerLocations.getAllAssetLocations();
	}
	
	@Override
	public AssetLocation getAssetLocation(File dir) {
		return assetContainerLocations.getAssetLocation(dir);
	}
	
	
	protected AssetContainerLocations getAssetContainerLocations()
	{
		return assetContainerLocations;
	}
}
