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
	public static String getAbsolutePath(String fileName) {
		String classPath = Application.class.getClassLoader().getResource("./").getPath().replaceAll("%20",  " ");
		for (int i = 0; i < 2; i++) {
			classPath = classPath.substring(0, classPath.lastIndexOf('/'));
		}
		classPath = classPath.substring(0, classPath.lastIndexOf('/') + 1);
		String file = classPath + fileName;
		return file;
	}
	
	public static PushbackReader getReader(String absolutePath) throws IOException {
		return new PushbackReader(new FileReader(absolutePath));
	}
	
	public static FileWriter getWriter(String absolutePath) throws IOException {
		return getWriter(absolutePath, false);
	}
	public static FileWriter getWriter(String absolutePath, boolean force) throws IOException {
		File file = new File(absolutePath);
		if (!file.createNewFile() && !force) {
			System.out.println("File already exists: " + absolutePath);
			System.out.println("\t Overwrite to overwrite? (y / n)\n");
			String response = ("" + (char) System.in.read()).toLowerCase();
			System.out.println("");
			switch (response) {
			case "y":
				break;
			default:
				System.out.println("Compiler aborted.");
				return null;
			}
		}
		return new FileWriter(file);
	}
}
