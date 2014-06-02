package org.bladerunnerjs.plugin.plugins.bundlers.cssresource;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.bladerunnerjs.model.Aspect;
import org.bladerunnerjs.model.AssetContainer;
import org.bladerunnerjs.model.AssetLocation;
import org.bladerunnerjs.model.BRJS;
import org.bladerunnerjs.model.Blade;
import org.bladerunnerjs.model.Bladeset;
import org.bladerunnerjs.model.BundlableNode;
import org.bladerunnerjs.model.BundleSet;
import org.bladerunnerjs.model.FileInfo;
import org.bladerunnerjs.model.JsLib;
import org.bladerunnerjs.model.ParsedContentPath;
import org.bladerunnerjs.model.ResourcesAssetLocation;
import org.bladerunnerjs.model.ThemedAssetLocation;
import org.bladerunnerjs.model.Workbench;
import org.bladerunnerjs.model.exception.request.ContentProcessingException;
import org.bladerunnerjs.model.exception.request.MalformedTokenException;
import org.bladerunnerjs.plugin.base.AbstractContentPlugin;
import org.bladerunnerjs.utility.ContentPathParser;
import org.bladerunnerjs.utility.ContentPathParserBuilder;
import org.bladerunnerjs.utility.RelativePathUtility;

public class CssResourceContentPlugin extends AbstractContentPlugin {	
	public static final String ASPECT_THEME_REQUEST = "aspect-theme-request";
	public static final String ASPECT_RESOURCES_REQUEST = "aspect-resources-request";
	public static final String BLADESET_THEME_REQUEST = "bladeset-theme-request";
	public static final String BLADESET_RESOURCES_REQUEST = "bladeset-resources-request";
	public static final String BLADE_THEME_REQUEST = "blade-theme-request";
	public static final String BLADE_RESOURCES_REQUEST = "blade-resources-request";
	public static final String WORKBENCH_THEME_REQUEST = "workbench-theme-request";
	public static final String WORKBENCH_RESOURCES_REQUEST = "workbench-resources-request";
	public static final String LIB_REQUEST = "lib-request";
	
	private final ContentPathParser contentPathParser;
	private BRJS brjs;
	
	{
		ContentPathParserBuilder contentPathParserBuilder = new ContentPathParserBuilder();
		contentPathParserBuilder
			.accepts("cssresource/aspect_<aspect>/theme_<theme>/<resourcePath>").as(ASPECT_THEME_REQUEST)
				.and("cssresource/aspect_<aspect>/resources/<resourcePath>").as(ASPECT_RESOURCES_REQUEST)
				.and("cssresource/bladeset_<bladeset>/theme_<theme>/<resourcePath>").as(BLADESET_THEME_REQUEST)
				.and("cssresource/bladeset_<bladeset>/resources/<resourcePath>").as(BLADESET_RESOURCES_REQUEST)
				.and("cssresource/bladeset_<bladeset>/blade_<blade>/theme_<theme>/<resourcePath>").as(BLADE_THEME_REQUEST)
				.and("cssresource/bladeset_<bladeset>/blade_<blade>/resources/<resourcePath>").as(BLADE_RESOURCES_REQUEST)
				.and("cssresource/bladeset_<bladeset>/blade_<blade>/workbench/theme_<theme>/<resourcePath>").as(WORKBENCH_THEME_REQUEST)
				.and("cssresource/bladeset_<bladeset>/blade_<blade>/workbench/resources/<resourcePath>").as(WORKBENCH_RESOURCES_REQUEST)
				.and("cssresource/lib_<lib>/<resourcePath>").as(LIB_REQUEST)
			.where("aspect").hasForm(ContentPathParserBuilder.NAME_TOKEN)
				.and("bladeset").hasForm(ContentPathParserBuilder.NAME_TOKEN)
				.and("blade").hasForm(ContentPathParserBuilder.NAME_TOKEN)
				.and("lib").hasForm(ContentPathParserBuilder.NAME_TOKEN)
				.and("resourcePath").hasForm(ContentPathParserBuilder.PATH_TOKEN)
				.and("theme").hasForm(ContentPathParserBuilder.NAME_TOKEN);
		
		contentPathParser = contentPathParserBuilder.build();
	}
	
	@Override
	public void setBRJS(BRJS brjs) {
		this.brjs = brjs;
	}
	
	@Override
	public String getRequestPrefix() {
		return "cssresource";
	}
	
	@Override
	public String getGroupName() {
		return null;
	}
	
	@Override
	public ContentPathParser getContentPathParser() {
		return contentPathParser;
	}
	
	@Override
	public List<String> getValidDevContentPaths(BundleSet bundleSet, String... locales) throws ContentProcessingException {
		try
		{
			return getValidContentPaths(bundleSet, locales);
		}
		catch (MalformedTokenException ex)
		{
			throw new ContentProcessingException(ex);
		}
	}
	
	@Override
	public List<String> getValidProdContentPaths(BundleSet bundleSet, String... locales) throws ContentProcessingException {
		try
		{
			return getValidContentPaths(bundleSet, locales);
		}
		catch (MalformedTokenException ex)
		{
			throw new ContentProcessingException(ex);
		}
	}
	
	private ThemedAssetLocation getThemedResourceLocation(AssetContainer container, String themeName){
		
		ThemedAssetLocation result = null;
		for(AssetLocation location: container.assetLocations()){
			if(location instanceof ThemedAssetLocation){
				String locationThemeName = ((ThemedAssetLocation)location).getThemeName();
				if(locationThemeName.equals(themeName)){
					result = ((ThemedAssetLocation)location);
				}
			}
		}
		return result;
	}
	
	@Override
	public void writeContent(ParsedContentPath contentPath, BundleSet bundleSet, OutputStream os) throws ContentProcessingException {
		
		BundlableNode bundlableNode = bundleSet.getBundlableNode();
		String theme = contentPath.properties.get("theme");
		String resourcePath = contentPath.properties.get("resourcePath");
		File resourceFile = null;
		
		if (contentPath.formName.equals(ASPECT_THEME_REQUEST))
		{
			String aspectName = contentPath.properties.get("aspect");
			Aspect Aspect =  bundlableNode.app().aspect(aspectName);
			List<ResourcesAssetLocation> resourceAssetLocations = getResourceAssetLocations(Aspect);
			for(ResourcesAssetLocation location : resourceAssetLocations){
				if(location.getThemeName().equals(theme)){
					resourceFile = location.file(resourcePath);
				}
			}
		}
		else if (contentPath.formName.equals(ASPECT_RESOURCES_REQUEST))
		{
			ThemedAssetLocation location = getThemedResourceLocation(bundlableNode, "common");
			resourceFile = location.file(resourcePath);
		}
		else if (contentPath.formName.equals(BLADESET_THEME_REQUEST))
		{
			Bladeset bladeset = bundlableNode.app().bladeset(contentPath.properties.get("bladeset"));
			ThemedAssetLocation location = getThemedResourceLocation(bladeset, theme);
			resourceFile = location.file(resourcePath);
		}
		else if (contentPath.formName.equals(BLADESET_RESOURCES_REQUEST))
		{
			Bladeset bladeset = bundlableNode.app().bladeset(contentPath.properties.get("bladeset"));
			ThemedAssetLocation location = getThemedResourceLocation(bladeset, "common");
			resourceFile = location.file(resourcePath);
		}
		else if (contentPath.formName.equals(BLADE_THEME_REQUEST))
		{
			Bladeset bladeset = bundlableNode.app().bladeset(contentPath.properties.get("bladeset"));
			Blade blade = bladeset.blade(contentPath.properties.get("blade"));
			ThemedAssetLocation location = getThemedResourceLocation(blade, theme);
			resourceFile = location.file(resourcePath);
			
		}
		else if (contentPath.formName.equals(BLADE_RESOURCES_REQUEST))
		{
			Bladeset bladeset = bundlableNode.app().bladeset(contentPath.properties.get("bladeset"));
			Blade blade = bladeset.blade(contentPath.properties.get("blade"));
			ThemedAssetLocation location = getThemedResourceLocation(blade, "common");
			resourceFile = location.file(resourcePath);
		}
		else if (contentPath.formName.equals(WORKBENCH_THEME_REQUEST))
		{
			//TODO: this needs implementing
			// Workbenches dont have themes ?
		}
		else if (contentPath.formName.equals(WORKBENCH_RESOURCES_REQUEST))
		{
			Bladeset bladeset = bundlableNode.app().bladeset(contentPath.properties.get("bladeset"));
			Blade blade = bladeset.blade(contentPath.properties.get("blade"));
			Workbench workbench = blade.workbench();
			ThemedAssetLocation location = getThemedResourceLocation(workbench, "common");
			resourceFile = location.file(resourcePath);
		}
		else if (contentPath.formName.equals(LIB_REQUEST))
		{
			JsLib jsLib = bundlableNode.app().jsLib(contentPath.properties.get("lib"));
			resourceFile = jsLib.file(resourcePath);
		}
		else
		{
			throw new ContentProcessingException("Cannot handle request with form name " + contentPath.formName);
		}
		
		//TODO: remove this line once all request types are handled
		if (resourceFile == null) { return; }
		
		try(InputStream input = new FileInputStream(resourceFile);)
		{
			IOUtils.copy(input, os);
		}
		catch(IOException e) {
			throw new ContentProcessingException(e);
		}
		finally
		{
			// TODO: see if we can remove this flush() since there doesn't seem to be any particular reason for it
			try {
				os.flush();
			}
			catch (IOException e) {
				throw new ContentProcessingException(e);
			}
		}
	}
	
	private List<String> getValidContentPaths(BundleSet bundleSet, String... locales) throws MalformedTokenException {
		List<String> contentPaths = new ArrayList<>();
		
		for(AssetContainer assetContainer : bundleSet.getBundlableNode().scopeAssetContainers())
		{
			contentPaths.addAll( getValidContentPaths(assetContainer) );
		}
		
		return contentPaths;
	}

	
	private List< String> getValidContentPaths(AssetContainer assetContainer) throws MalformedTokenException
	{		
		List<String> contentPaths = new ArrayList<>();
		
		String themeRequestName = "";
		String resourcesRequestName = "";
		String[] requestArgs = new String[0];
		
		
		if (assetContainer instanceof Aspect)
		{
			Aspect aspect = (Aspect) assetContainer;
			themeRequestName = ASPECT_THEME_REQUEST;
			resourcesRequestName = ASPECT_RESOURCES_REQUEST;
			requestArgs = new String[] { aspect.getName() };
		}
		else if (assetContainer instanceof Bladeset)
		{
			Bladeset bladeset = (Bladeset) assetContainer;
			themeRequestName = BLADESET_THEME_REQUEST;
			resourcesRequestName = BLADESET_RESOURCES_REQUEST;
			requestArgs = new String[] { bladeset.getName() };
		}
		else if (assetContainer instanceof Blade)
		{
			Blade blade = (Blade) assetContainer;
			Bladeset bladeset = brjs.locateAncestorNodeOfClass(blade, Bladeset.class);
			themeRequestName = BLADE_THEME_REQUEST;
			resourcesRequestName = BLADE_RESOURCES_REQUEST;
			requestArgs = new String[] { bladeset.getName(), blade.getName() };
		}
		else if (assetContainer instanceof Workbench)
		{
			Workbench workbench = (Workbench) assetContainer;
			Blade blade = brjs.locateAncestorNodeOfClass(workbench, Blade.class);
			Bladeset bladeset = brjs.locateAncestorNodeOfClass(blade, Bladeset.class);
			themeRequestName = WORKBENCH_THEME_REQUEST;
			resourcesRequestName = WORKBENCH_RESOURCES_REQUEST;
			requestArgs = new String[] { bladeset.getName(), blade.getName() };
		}
		else {
			return contentPaths;
		}
		
		// TODO: move themes off the ResourcesAssetLocation, since otherwise we are tied to the BRJS conformant asset-location plug-in
 		File resourcesDir = assetContainer.assetLocation("resources").dir();
		contentPaths.addAll(calculateContentPathsForThemesAndResources(assetContainer, themeRequestName, resourcesDir, resourcesRequestName, requestArgs));
		
		return contentPaths;
	}
	
	private List<ResourcesAssetLocation> getResourceAssetLocations(AssetContainer container){
		
		List<ResourcesAssetLocation> result = new ArrayList<ResourcesAssetLocation>();
		for (AssetLocation location: container.assetLocations()){
			if(location instanceof ResourcesAssetLocation){
				ResourcesAssetLocation resourcesLocation = (ResourcesAssetLocation)location;
				result.add(resourcesLocation);
			}
		}
		return result;
	}
	
	private List<String> calculateContentPathsForThemesAndResources(AssetContainer container, String themeRequestName, File resourcesDir, String resourcesRequestName, String... requestArgs) throws MalformedTokenException
	{
		List<String> contentPaths = new ArrayList<>();
		for (ResourcesAssetLocation location: getResourceAssetLocations(container)){
			File assetLocationDir = location.dir();
			FileInfo assetLocationDirInfo = brjs.getFileInfo(assetLocationDir);
			if (assetLocationDirInfo.isDirectory()){
				for (File file : assetLocationDirInfo.nestedFiles()){	
					if (!file.getName().endsWith(".css")){
						if(assetLocationDir.getName().endsWith("resources")){
							String assetPath = RelativePathUtility.get(resourcesDir, file);
			    			String[] createRequestArgs = ArrayUtils.addAll( requestArgs, new String[] { assetPath } );
							contentPaths.add( contentPathParser.createRequest(resourcesRequestName, createRequestArgs) );
						}else{
							String assetPath = RelativePathUtility.get(assetLocationDir, file);
							String[] createRequestArgs = ArrayUtils.addAll( requestArgs, new String[] { location.getThemeName(), assetPath } );
							String request = contentPathParser.createRequest(themeRequestName, createRequestArgs);
							contentPaths.add(request );
						}
					}
				}
			}
		}
		
		return contentPaths;
	}
}
