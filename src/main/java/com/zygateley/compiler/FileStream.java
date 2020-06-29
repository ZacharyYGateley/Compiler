package com.zygateley.compiler;

import java.io.InputStream;
import java.io.IOException;
import java.nio.file.*;

/**
 * Hide file IO behind this class
 * so that future revisions or portability is
 * more clear. 
 * 
 * @author Zachary Gateley
 *
 */
public class FileStream {
	private static final int EOF = 0;
	
	private Path __file__;
	private InputStream __stream__; 
	
	public FileStream(String rootFileName) throws IOException {
		String home = System.getProperty("user.home");
		__file__ = Paths.get(home, rootFileName);
		if (Files.notExists(__file__)) { 
			throw new IOException("File not found"); 
		}
	}
	
	public boolean open() throws IOException {
		this.__stream__ = Files.newInputStream(this.__file__);
		return true;
	}
	
	public int read() throws IOException {
		if (this.__stream__.available() <=  0) {
			return FileStream.EOF;
		}
		int input = this.__stream__.read();
		return input;
	}
	
	public boolean close() throws IOException {
		this.__stream__.close();
		return false;
	}
}
