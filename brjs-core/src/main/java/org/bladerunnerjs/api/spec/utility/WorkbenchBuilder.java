package org.bladerunnerjs.api.spec.utility;

import org.bladerunnerjs.api.Workbench;
import org.bladerunnerjs.api.spec.engine.BuilderChainer;
import org.bladerunnerjs.api.spec.engine.BundlableNodeBuilder;
import org.bladerunnerjs.api.spec.engine.SpecTest;

public class WorkbenchBuilder extends BundlableNodeBuilder<Workbench<?>>
{
	private final Workbench<?> workbench;
	
	public WorkbenchBuilder(SpecTest modelTest, Workbench<?> workbench)
	{
		super(modelTest, workbench);
		this.workbench = workbench;
	}
	
	public BuilderChainer resourceFileRefersTo(String resourceFileName, String className) throws Exception 
	{
		writeToFile(workbench.file("resources/"+resourceFileName), "<root refs='" + className + "'/>");
		
		return builderChainer;
	}
	
}