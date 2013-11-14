package org.bladerunnerjs.core.plugin.bundler.xml;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.bladerunnerjs.core.plugin.bundler.BundlerPlugin;
import org.bladerunnerjs.model.AbstractAssetFileFactory;
import org.bladerunnerjs.model.AssetFile;
import org.bladerunnerjs.model.AssetFileAccessor;
import org.bladerunnerjs.model.BRJS;
import org.bladerunnerjs.model.BundleSet;
import org.bladerunnerjs.model.FullyQualifiedLinkedAssetFile;
import org.bladerunnerjs.model.LinkedAssetFile;
import org.bladerunnerjs.model.ParsedRequest;
import org.bladerunnerjs.model.RequestParser;
import org.bladerunnerjs.model.AssetLocation;
import org.bladerunnerjs.model.SourceFile;
import org.bladerunnerjs.model.AssetContainer;
import org.bladerunnerjs.model.exception.request.BundlerProcessingException;
import org.bladerunnerjs.model.utility.RequestParserBuilder;


public class XMLBundlerPlugin implements BundlerPlugin
{

	private RequestParser requestParser;
	
	{
		RequestParserBuilder requestParserBuilder = new RequestParserBuilder();
		requestParserBuilder.accepts("xml.bundle").as("bundle-request");
		requestParser = requestParserBuilder.build();
	}
	
	@Override
	public String getTagName()
	{
		return "xml";
	}

	@Override
	public void writeDevTagContent(Map<String, String> tagAttributes, BundleSet bundleSet, String locale, Writer writer) throws IOException
	{
		throw new RuntimeException("Not implemented!");
	}

	@Override
	public void writeProdTagContent(Map<String, String> tagAttributes, BundleSet bundleSet, String locale, Writer writer) throws IOException
	{
		throw new RuntimeException("Not implemented!");
	}

	@Override
	public void setBRJS(BRJS brjs)
	{
	}

	@Override
	public String getMimeType()
	{
		return "application/xml";
	}

	@Override
	public AssetFileAccessor getAssetFileAccessor()
	{
		return new XMLBundlerAssetFileAccessor();
	}

	@Override
	public RequestParser getRequestParser()
	{
		return requestParser;
	}

	@Override
	public List<String> generateRequiredDevRequestPaths(BundleSet bundleSet, String locale) throws BundlerProcessingException
	{
		throw new RuntimeException("Not implemented!");
	}

	@Override
	public List<String> generateRequiredProdRequestPaths(BundleSet bundleSet, String locale) throws BundlerProcessingException
	{
		throw new RuntimeException("Not implemented!");
	}

	@Override
	public void handleRequest(ParsedRequest request, BundleSet bundleSet, OutputStream os) throws BundlerProcessingException
	{
		throw new RuntimeException("Not implemented!");
	}
	
	
	
	public class XMLBundlerAssetFileAccessor implements AssetFileAccessor
	{

		@Override
		public List<SourceFile> getSourceFiles(AssetContainer assetContainer)
		{
			return Arrays.asList();
		}

		@Override
		public List<LinkedAssetFile> getLinkedResourceFiles(AssetLocation assetLocation)
		{
			//TODO: remove this "src" - it should be known by the model
			return new XmlFileSetFactory().findFiles(assetLocation.getAssetContainer(), assetLocation.dir(), new SuffixFileFilter("xml"), null);
		}

		@Override
		public List<AssetFile> getResourceFiles(AssetLocation assetLocation)
		{
			return Arrays.asList();
		}

	}
	
	
	//TODO: get rid of this
		private class XmlFileSetFactory extends AbstractAssetFileFactory<LinkedAssetFile> {
			@Override
			public LinkedAssetFile createFile(AssetContainer assetContainer, File file) {
				return new FullyQualifiedLinkedAssetFile(assetContainer, file);
			}
		}
	
}
