package org.bladerunnerjs.model.aliasing;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.TransformerException;

import org.apache.commons.io.FileUtils;
import org.bladerunnerjs.model.exception.request.BundlerFileProcessingException;
import org.bladerunnerjs.model.utility.FileModifiedChecker;
import org.bladerunnerjs.model.utility.XmlStreamReaderFactory;
import org.bladerunnerjs.model.utility.stax.XmlStreamReader;
import org.bladerunnerjs.specutil.XmlBuilderSerializer;
import org.codehaus.stax2.validation.XMLValidationSchema;
import org.codehaus.stax2.validation.XMLValidationSchemaFactory;

import com.ctc.wstx.msv.RelaxNGSchemaFactory;
import com.esotericsoftware.yamlbeans.parser.Parser.ParserException;
import com.google.common.base.Joiner;
import com.jamesmurty.utils.XMLBuilder;

public class AliasesFile extends File {
	private static final long serialVersionUID = 1L;
	private static XMLValidationSchema aliasesSchema;
	
	private final FileModifiedChecker fileModifiedChecker;
	private List<AliasDefinition> aliasDefinitions = new ArrayList<>();
	private List<String> groupNames = new ArrayList<>();
	private String scenario;
	
	static {
		XMLValidationSchemaFactory schemaFactory = new RelaxNGSchemaFactory();
		
		try
		{
			aliasesSchema = schemaFactory.createSchema(SchemaConverter.convertToRng("org/bladerunnerjs/model/aliasing/aliases.rnc"));
		}
		catch (XMLStreamException | SchemaCreationException e)
		{
			throw new RuntimeException(e);
		}
	}
	
	public AliasesFile(File parent, String child) {
		super(parent, child);
		fileModifiedChecker = new FileModifiedChecker(this);
	}
	
	public AliasDefinition getAlias(AliasName aliasName) throws BundlerFileProcessingException {
		AliasDefinition aliasDefinition = null;
		
		for(AliasDefinition nextAliasDefinition : aliasDefinitions()) {
			if(nextAliasDefinition.getName().equals(aliasName.getName())) {
				aliasDefinition = nextAliasDefinition;
				break;
			}
		}
		
		return aliasDefinition;
	}
	
	public String scenarioName() throws BundlerFileProcessingException {
		if(fileModifiedChecker.fileModifiedSinceLastCheck()) {
			reparseFile();
		}
		
		return scenario;
	}
	
	public void setScenarioName(String scenarioName) {
		this.scenario = scenarioName;
	}
	
	public List<String> groupNames() throws BundlerFileProcessingException {
		if(fileModifiedChecker.fileModifiedSinceLastCheck()) {
			reparseFile();
		}
		
		return groupNames;
	}
	
	public void setGroupNames(List<String> groupNames) {
		this.groupNames = groupNames;
	}
	
	public List<AliasDefinition> aliasDefinitions() throws BundlerFileProcessingException {
		if(fileModifiedChecker.fileModifiedSinceLastCheck()) {
			reparseFile();
		}
		
		return aliasDefinitions;
	}
	
	// TODO: should we switch away from AliasDefinition, given that there are no interfaces, scenarios or groups at this level?
	public void addAliasDefinition(AliasDefinition aliasDefinition) {
		aliasDefinitions.add(aliasDefinition);
	}
	
	public void write() throws IOException {
		try {
			XMLBuilder builder = XMLBuilder.create("aliases").ns("http://schema.caplin.com/CaplinTrader/aliases");
			
			if(scenario != null) {
				builder.a("useScenario", scenario);
			}
			
			if(!groupNames.isEmpty()) {
				builder.a("useGroups", Joiner.on(" ").join(groupNames));
			}
			
			for(AliasDefinition aliasDefinition : aliasDefinitions) {
				builder.e("alias").a("name", aliasDefinition.getName()).a("class", aliasDefinition.getClassName());
			}
			
			FileUtils.write(this, XmlBuilderSerializer.serialize(builder));
		}
		catch(ParserException | TransformerException | ParserConfigurationException | FactoryConfigurationError e) {
			throw new IOException(e);
		}
	}
	
	private void reparseFile() throws BundlerFileProcessingException {
		aliasDefinitions = new ArrayList<>();
		groupNames = new ArrayList<>();
		
		if(exists()) {
			try(XmlStreamReader streamReader = XmlStreamReaderFactory.createReader(this, aliasesSchema)) {
				while(streamReader.hasNextTag()) {
					streamReader.nextTag();
					
					if(streamReader.getEventType() == XMLStreamReader.START_ELEMENT) {
						switch(streamReader.getLocalName()) {
							case "aliases":
								processAliases(streamReader);
								break;
							
							case "alias":
								processAlias(streamReader);
								break;
						}
					}
				}
			}
			catch (XMLStreamException e) {
				Location location = e.getLocation();
				
				throw new BundlerFileProcessingException(this, location.getLineNumber(), location.getColumnNumber(), e.getMessage());
			}
			catch (FileNotFoundException e) {
				throw new BundlerFileProcessingException(this, e);
			}
		}
	}
	
	private void processAliases(XmlStreamReader streamReader) {
		scenario = streamReader.getAttributeValue("useScenario");
		
		String useGroups = streamReader.getAttributeValue("useGroups");
		if(useGroups != null) {
			groupNames = Arrays.asList(useGroups.split(" "));
		}
	}
	
	private void processAlias(XmlStreamReader streamReader) {
		String aliasName = streamReader.getAttributeValue("name");
		String aliasClass = streamReader.getAttributeValue("class");
		
		aliasDefinitions.add(new AliasDefinition(aliasName, aliasClass, null));
	}
}
