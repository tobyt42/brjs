package org.bladerunnerjs.appserver.filter;

import java.io.EOFException;
import java.io.IOException;
import java.io.Reader;
import java.util.regex.Pattern;

import javax.naming.NamingException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.bladerunnerjs.appserver.util.CommitedResponseCharResponseWrapper;
import org.bladerunnerjs.appserver.util.JndiTokenFinder;
import org.bladerunnerjs.appserver.util.TokenReplacingReader;

public class TokenisingServletFilter implements Filter
{
	private TokenReplacingReader streamTokeniser;
	private JndiTokenFinder tokenFinder;
	private final Pattern validUrl = Pattern.compile("^.*(/|/[a-z]{2}|/[a-z]{2}_[A-Z]{2}|\\.(xml|json|html|htm|jsp))$");
	
	public TokenisingServletFilter() throws ServletException
	{
	}
	
	/* this should only be used for testing */
	public TokenisingServletFilter(JndiTokenFinder tokenFinder) throws ServletException
	{
		this.tokenFinder = tokenFinder;
	}

	@Override
	public void init(FilterConfig filterConfig)
	{
	}
	
	@Override
	public void destroy()
	{
	}
	
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException
	{
		if (shouldProcessResponse(request))
		{
			ServletOutputStream out = response.getOutputStream();
			CommitedResponseCharResponseWrapper responseWrapper = new CommitedResponseCharResponseWrapper((HttpServletResponse) response);
			chain.doFilter(request, responseWrapper);
			
			try
			{
				if (!response.isCommitted()) { // only write the content if the headers havent been commited (an error code hasnt been sent)
					String filteredResponse = IOUtils.toString( getStreamTokeniser(responseWrapper.getReader()) );
					byte[] filteredData = filteredResponse.toString().getBytes(response.getCharacterEncoding());
					response.setContentLength(filteredData.length);
					out.write(filteredData);
					response.flushBuffer();
				}
			}
			catch(EOFException e) {
				// the browser has closed it's connection early -- re-throw
				throw e;
			}
			catch(Exception e)
			{
				throw new ServletException(e);
			}
		}
		else
		{
			chain.doFilter(request, response);
		}
	}

	private boolean shouldProcessResponse(ServletRequest request)
	{
		HttpServletRequest theRequest = (HttpServletRequest) request;
		String requestUrl = theRequest.getRequestURL().toString();
		
		return validUrl.matcher(requestUrl).matches();
	}
	
	private Reader getStreamTokeniser(Reader reader) throws ServletException {
		if (tokenFinder == null) {
			try {
				tokenFinder = new JndiTokenFinder();
			} catch(NamingException ex) {
				throw new ServletException("Error getting context for JNDI lookups. (" + ex + ")", ex);
			}
		}
		if (streamTokeniser == null) {
			streamTokeniser = new TokenReplacingReader(tokenFinder, reader);
		}
		return streamTokeniser;
	}
	
}
