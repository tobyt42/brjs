package org.bladerunnerjs.testing.specutility;

import static org.junit.Assert.*;

import java.util.List;

import org.bladerunnerjs.model.BundlableNode;
import org.bladerunnerjs.plugin.ContentPlugin;
import org.bladerunnerjs.testing.specutility.engine.NodeVerifier;
import org.bladerunnerjs.testing.specutility.engine.SpecTest;

import com.google.common.base.Joiner;

public class BundlableNodeVerifier<T extends BundlableNode> extends NodeVerifier<T> {
	private T bundlableNode;
	
	public BundlableNodeVerifier(SpecTest specTest, T bundlableNode) {
		super(specTest, bundlableNode);
		this.bundlableNode = bundlableNode;
	}
	
	public void prodAndDevRequestsForContentPluginsAre(String contentPluginPrefix, String... expectedRequests) throws Exception {
		ContentPlugin contentPlugin = bundlableNode.root().plugins().contentProvider(contentPluginPrefix);
		List<String> actualDevRequests = contentPlugin.getValidDevContentPaths(bundlableNode.getBundleSet(), bundlableNode.app().appConf().getLocales());
		List<String> actualProdRequests = contentPlugin.getValidProdContentPaths(bundlableNode.getBundleSet(), bundlableNode.app().appConf().getLocales());
		
		assertEquals(Joiner.on(", ").join(expectedRequests), Joiner.on(", ").join(actualDevRequests));
		assertEquals(Joiner.on(", ").join(expectedRequests), Joiner.on(", ").join(actualProdRequests));
	}
	
	public void devRequestsForContentPluginsAre(String contentPluginPrefix, String... expectedRequests) throws Exception {
		ContentPlugin contentPlugin = bundlableNode.root().plugins().contentProvider(contentPluginPrefix);
		List<String> actualRequests = contentPlugin.getValidProdContentPaths(bundlableNode.getBundleSet(), bundlableNode.app().appConf().getLocales());
		
		assertEquals(Joiner.on(", ").join(expectedRequests), Joiner.on(", ").join(actualRequests));
	}
	
	public void prodRequestsForContentPluginsAre(String contentPluginPrefix, String... expectedRequests) throws Exception {
		ContentPlugin contentPlugin = bundlableNode.root().plugins().contentProvider(contentPluginPrefix);
		List<String> actualRequests = contentPlugin.getValidProdContentPaths(bundlableNode.getBundleSet(), bundlableNode.app().appConf().getLocales());
		
		assertEquals(Joiner.on(", ").join(expectedRequests), Joiner.on(", ").join(actualRequests));
	}
}
