package com.zygateley.compiler;

import java.io.*;

/**
 * Hide file IO behind this class
 * so that future revisions or portability is
 * more clear. 
 * 
 * @author Zachary Gateley
 *
 */
public class FileIO {
	/**
	 * Get the absolute path of this application.
	 * Optionally, send a non-empty relative file path/name
	 * that will be added at the end of the result.
	 * 
	 * @param fileName optional relative path from application root,
	 * 		will be returned as application absolutePath suffix
	 * @return absolute path to application. May contain disk label (e.g. /c:/)
	 */
	public static String getAbsolutePath(Class<?> source, String fileName) {
		if (fileName == null) fileName = "";
		String classPath = source.getClassLoader().getResource("./").getPath().replaceAll("%20",  " ");
		/*
		for (int i = 0; i < 2; i++) {
			classPath = classPath.substring(0, classPath.lastIndexOf('/'));
		}
		*/
		classPath = classPath.substring(0, classPath.lastIndexOf('/') + 1);
		String file = classPath + fileName;
		return file;
	}
	
	/**
	 * Instantiate a (pushback) file reader for a given (absolute path) file name.
	 * 
	 * @param absolutePath file name, given as absolute from system root
	 * @return PushbackReader of said file
	 * @throws IOException
	 */
	public static PushbackReader getReader(String absolutePath) throws IOException {
		return new PushbackReader(new FileReader(absolutePath));
	}
	
	/**
	 * Instantiate a file writer for a given (absolute path) file name.
	 * Method-chained to <strong>getWriter(String absolutePath, boolean force)</strong>
	 * with ask-to-overwrite flag (force=false)..
	 * 
	 * @param absolutePath file name, given as absolute from system root
	 * @return FileWriter for said file
	 * @throws IOException
	 * @throws InterruptedException 
	 */
	public static FileWriter getWriter(String absolutePath) throws IOException {
		return getWriter(absolutePath, false);
	}
	
	/**
	 * Instantiate a file writer for a given (absolute path) file name.
	 * 
	 * @param absolutePath file name, given as absolute from system root
	 * @param force false: ask to overwrite file. true: overwrite if file exists without asking. 
	 * @return FileWriter for said file
	 * @throws IOException
	 * @throws InterruptedException 
	 */
	public static FileWriter getWriter(String absolutePath, boolean force) throws IOException {
		File file = new File(absolutePath);
		if (!file.createNewFile() && !force) {
			System.out.print("\nOK to overwrite \"" + absolutePath);
			System.out.print("\"? (y / n) ");
			String response = ("" + (char) System.in.read()).toLowerCase();
			while (System.in.available() > 0) {
				System.in.read();
			}
			System.out.println("");
			switch (response) {
			case "y":
				break;
			default:
				response = null;
				return null;
			}
		}
		return new FileWriter(file);
	}
	
	/**
	 * For Process ... = Runtime.exec(...) calls,
	 * output any console output or errors resulting from
	 * the process.
	 * 
	 * @param process any process that may have getInputStream() and getErrorStream()
	 * @throws IOException
	 */
	public static void processOutput(Process process) throws IOException {
		for (int j = 0; j < 2; j++) {
			BufferedReader std = new BufferedReader(
					new InputStreamReader((j == 0 ? process.getInputStream() : process.getErrorStream()))
					);
		
			// Read the output from the command
			int i;
			while (std.ready()) {
				i = std.read();
				// 	Avoid garbage output
				if (i < 128) System.out.print((char) i);
				if (i == 10) System.out.print("\t");
			}
		}
	}
	
	/**
	 * If the disk name (e.g. /c:/) appears in the passed absolute path,
	 * remove this from the path and return the new absolute path
	 * without the disk name.
	 * 
	 * @param absolutePath file system path
	 * @return file system path without a disk name
	 */
	public static String clipDiskName(String absolutePath) {
		int colonAt = absolutePath.indexOf(":");
		if (colonAt > -1) {
			int slashAt = absolutePath.indexOf("/");
			boolean slashPrefix = false;
			if (slashAt == 0) {
				absolutePath = absolutePath.substring(1);
				colonAt--;
				slashAt = absolutePath.indexOf("/");
				slashPrefix = true;
			}
			if (colonAt < slashAt) {
				absolutePath = absolutePath.substring(slashAt);
			}
			else if (slashPrefix) {
				absolutePath = "/" + absolutePath;
			}
		}
		return absolutePath;
	}
	
	public static boolean writeResource(Class<?> source, String fileName, File destPath) throws IOException {
		InputStream sourceStream = source.getResourceAsStream(fileName);
		if (sourceStream == null || sourceStream.available() == 0) {
			throw new IOException("File does not exist: " + fileName);
		}
		// Ensure the output directory exists
		destPath.mkdir();
		// Do not overwrite if already written
		File destFile = new File(destPath + "/" + fileName);
		if (!destFile.createNewFile()) {
			// File already created
			sourceStream.close();
			return false;
		}
		// Get source stream
		//File sourceFile = new File(sourcePath + "/" + fileName);
		DataInputStream input = new DataInputStream(sourceStream);
		DataOutputStream output = new DataOutputStream(new FileOutputStream(destFile));
		while (input.available() > 0) {
			output.write(input.readAllBytes());
		}
		input.close();
		output.close();
		return true;
	}
}
