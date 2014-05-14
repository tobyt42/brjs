package org.bladerunnerjs.appserver;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.bladerunnerjs.model.App;
import org.bladerunnerjs.model.BRJS;
import org.bladerunnerjs.model.BrowsableNode;
import org.bladerunnerjs.model.RequestMode;
import org.bladerunnerjs.model.exception.InvalidSdkDirectoryException;
import org.bladerunnerjs.model.exception.request.ContentProcessingException;
import org.bladerunnerjs.model.exception.request.MalformedRequestException;
import org.bladerunnerjs.model.exception.request.ResourceNotFoundException;
import org.bladerunnerjs.utility.PageAccessor;
import org.bladerunnerjs.utility.RelativePathUtility;


public class BRJSDevServlet extends HttpServlet {
	private static final long serialVersionUID = 1964608537461568895L;
	
	private App app;
	private ServletContext servletContext;
	private BRJS brjs;
	
	@Override
	public void init(ServletConfig config) throws ServletException
	{
		super.init(config);
		servletContext = config.getServletContext();
		
		try {
			BRJSThreadSafeModelAccessor.initializeModel(servletContext);
		}
		catch (InvalidSdkDirectoryException e) {
			throw new ServletException(e);
		}
		
		try {
			brjs = BRJSThreadSafeModelAccessor.aquireModel();
			app = brjs.locateAncestorNodeOfClass(new File(servletContext.getRealPath("/")), App.class);
			
			if(app == null) {
 				throw new ServletException("Unable to calculate app for Servlet. Context path for expected app was '" + servletContext.getRealPath("/") + "'.");
 			}
		}
		finally {
			BRJSThreadSafeModelAccessor.releaseModel();
		}
	}
	
	@Override
	public void destroy() {
		BRJSThreadSafeModelAccessor.destroy();
	}
	
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String requestPath = request.getRequestURI().replaceFirst("^" + request.getContextPath() + request.getServletPath(), "");
		
		try {
			BRJSThreadSafeModelAccessor.aquireModel();
			app.handleLogicalRequest(requestPath, response.getOutputStream(), new BRJSPageAccessor(request, response));
		}
		catch (MalformedRequestException | ResourceNotFoundException | ContentProcessingException e) {
			throw new ServletException(e);
		}
		finally {
			BRJSThreadSafeModelAccessor.releaseModel();
		}
	}
	
	private class BRJSPageAccessor implements PageAccessor {
		private final HttpServletRequest request;
		private final HttpServletResponse response;
		
		public BRJSPageAccessor(HttpServletRequest request, HttpServletResponse response) {
			this.request = request;
			this.response = response;
		}
		
		@Override
		public void serveIndexPage(BrowsableNode browsableNode, String locale) throws IOException {
			try {
				RequestDispatcher requestDispatcher = servletContext.getRequestDispatcher("/" + RelativePathUtility.get(app.dir(), browsableNode.file("index.html")));
				CharResponseWrapper responseWrapper = new CharResponseWrapper(response);
				requestDispatcher.include(request, responseWrapper);
				
				ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
				try (Writer writer =  new OutputStreamWriter(byteArrayOutputStream, brjs.bladerunnerConf().getBrowserCharacterEncoding()))
				{
					browsableNode.filterIndexPage(getIndexPage(responseWrapper), locale, writer, RequestMode.Dev);
				}
				
				byte[] byteArray = byteArrayOutputStream.toByteArray();
				response.setContentType("text/html");
				response.setCharacterEncoding(brjs.bladerunnerConf().getBrowserCharacterEncoding());
				response.setContentLength(byteArray.length);
				response.getOutputStream().write(byteArray);
			}
			catch (Exception ex)
			{
				response.sendError(500, ex.toString());
			}
		}
		
		private String getIndexPage(CharResponseWrapper responseWrapper) throws IOException, UnsupportedEncodingException {
			StringWriter bufferedResponseStringWriter = new StringWriter();
			
			try(Reader reader = responseWrapper.getReader()) {
				IOUtils.copy(reader, bufferedResponseStringWriter);
			}
			
			return bufferedResponseStringWriter.toString();
		}
	}
}
