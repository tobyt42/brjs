package org.bladerunnerjs.plugin;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.IOUtils;


public class BinaryResponseContent implements ResponseContent
{
	
	private InputStream input;

	public BinaryResponseContent(InputStream input) {
		this.input = input;
	}

	public InputStream getInputStream() {
		return input;
	}
	
	@Override
	public void write(OutputStream outputStream) throws IOException
	{
		IOUtils.copy(input, outputStream);
	}

	@Override
	public void close() throws Exception
	{
		input.close();
	}

	@Override
	public void closeQuietly()
	{
		try
		{
			close();
		}
		catch (Exception e)
		{
		}
	}
	
}
