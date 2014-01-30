package org.bladerunnerjs.plugin.plugins.bundlers.compositejs;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import org.bladerunnerjs.model.BRJS;
import org.bladerunnerjs.model.BundleSet;
import org.bladerunnerjs.model.ParsedContentPath;
import org.bladerunnerjs.model.exception.ConfigException;
import org.bladerunnerjs.model.exception.request.BundlerProcessingException;
import org.bladerunnerjs.model.exception.request.MalformedRequestException;
import org.bladerunnerjs.plugin.ContentPlugin;
import org.bladerunnerjs.plugin.InputSource;
import org.bladerunnerjs.plugin.MinifierPlugin;
import org.bladerunnerjs.plugin.base.AbstractContentPlugin;
import org.bladerunnerjs.utility.ContentPathParser;
import org.bladerunnerjs.utility.ContentPathParserBuilder;


public class CompositeJsContentPlugin extends AbstractContentPlugin {
	private ContentPathParser contentPathParser = (new ContentPathParserBuilder()).build();
	private BRJS brjs;
	
	{
		ContentPathParserBuilder contentPathParserBuilder = new ContentPathParserBuilder();
		contentPathParserBuilder
			.accepts("js/dev/<locale>/<minifier-setting>/bundle.js").as("dev-bundle-request")
				.and("js/prod/<locale>/<minifier-setting>/bundle.js").as("prod-bundle-request")
			.where("locale").hasForm("[a-z]{2}(_[A-Z]{2})?")
				.and("minifier-setting").hasForm("[a-z-]+");
		
		contentPathParser = contentPathParserBuilder.build();
	}
	
	@Override
	public void setBRJS(BRJS brjs) {
		this.brjs = brjs;
	}
	
	@Override
	public String getRequestPrefix() {
		return "js";
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
	public List<String> getValidDevContentPaths(BundleSet bundleSet, List<String> locales) throws BundlerProcessingException {
		return generateRequiredRequestPaths(true, bundleSet, locales);
	}
	
	@Override
	public List<String> getValidProdContentPaths(BundleSet bundleSet, List<String> locales) throws BundlerProcessingException {
		return generateRequiredRequestPaths(false, bundleSet, locales);
	}
	
	@Override
	public void writeContent(ParsedContentPath contentPath, BundleSet bundleSet, OutputStream os) throws BundlerProcessingException {
		if(contentPath.formName.equals("dev-bundle-request") || contentPath.formName.equals("prod-bundle-request")) {
			try {
				String minifierSetting = contentPath.properties.get("minifier-setting");
				MinifierPlugin minifierPlugin = brjs.plugins().minifier(minifierSetting);
				
				try(Writer writer = new OutputStreamWriter(os)) {
					List<InputSource> inputSources = getInputSourcesFromOtherBundlers(contentPath, bundleSet);
					minifierPlugin.minify(minifierSetting, inputSources, writer);
				}
			}
			catch(IOException e) {
				throw new BundlerProcessingException(e);
			}
			
		}
		else {
			throw new BundlerProcessingException("unknown request form '" + contentPath.formName + "'.");
		}
	}
	
	private List<String> generateRequiredRequestPaths(boolean isDev, BundleSet bundleSet, List<String> locales) throws BundlerProcessingException {
		List<String> requestPaths = new ArrayList<>();
		
		for(ContentPlugin contentPlugin : brjs.plugins().contentProviders("text/javascript")) {
			if(isDev) {
				requestPaths.addAll(contentPlugin.getValidDevContentPaths(bundleSet, locales));
			}
			else {
				requestPaths.addAll(contentPlugin.getValidProdContentPaths(bundleSet, locales));
			}
		}
		
		return requestPaths;
	}
	
	private List<InputSource> getInputSourcesFromOtherBundlers(ParsedContentPath contentPath, BundleSet bundleSet) throws BundlerProcessingException {
		List<InputSource> inputSources = new ArrayList<>();
		
		try {
			String charsetName = brjs.bladerunnerConf().getDefaultOutputEncoding();
			
			for(ContentPlugin contentPlugin : brjs.plugins().contentProviders("text/javascript")) {
				List<String> locales = new ArrayList<>();
				locales.add(contentPath.properties.get("locale"));
				
				List<String> requestPaths = (contentPath.formName.equals("dev-bundle-request")) ? contentPlugin.getValidDevContentPaths(bundleSet, locales) :
					contentPlugin.getValidProdContentPaths(bundleSet, locales);
				ContentPathParser contentPathParser = contentPlugin.getContentPathParser();
				
				for(String requestPath : requestPaths) {
					ParsedContentPath parsedContentPath = contentPathParser.parse(requestPath);
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					
					contentPlugin.writeContent(parsedContentPath, bundleSet, baos);
					inputSources.add(new InputSource(requestPath, baos.toString(charsetName), contentPlugin, bundleSet));
				}
			}
		}
		catch(ConfigException | IOException | MalformedRequestException e) {
			throw new BundlerProcessingException(e);
		}
		
		return inputSources;
	}
}
