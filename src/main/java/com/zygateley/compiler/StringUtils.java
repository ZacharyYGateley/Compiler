package com.zygateley.compiler;

public class StringUtils {
	/**
	 * Performs single unescape on all escape characters
	 * for destination assembly
	 * 
	 * @param input 
	 * @return unescaped string
	 */
	public static String unescapeAssemblyString(String input) {
		if (!(input instanceof String)) {
			return "";
		}
		return input
				.replaceAll("\\\\\"", "\",'\"',\"")
				.replaceAll("\0", "\",0,\"")
				.replaceAll("\\\\n", "\",10,\"")
				.replaceAll("\\\\f", "\",12,\"")
				.replaceAll("\\\\r", "\",13,\"")
				.replaceAll("\"\",", "");
	}
	
	/**
	 * Performs single unescape on all escape characters
	 * for destination Java
	 * 
	 * Courtesy 
	 * https://gist.github.com/uklimaschewski
	 * Found at 
	 * https://gist.github.com/uklimaschewski/6741769
	 * 
	 * @param st
	 * @return unescaped string
	 */
	public static String unescapeJavaString(String st) {
	    StringBuilder sb = new StringBuilder(st.length());

	    for (int i = 0; i < st.length(); i++) {
	        char ch = st.charAt(i);
	        if (ch == '\\') {
	            char nextChar = (i == st.length() - 1) ? '\\' : st
	                    .charAt(i + 1);
	            // Octal escape?
	            if (nextChar >= '0' && nextChar <= '7') {
	                String code = "" + nextChar;
	                i++;
	                if ((i < st.length() - 1) && st.charAt(i + 1) >= '0'
	                        && st.charAt(i + 1) <= '7') {
	                    code += st.charAt(i + 1);
	                    i++;
	                    if ((i < st.length() - 1) && st.charAt(i + 1) >= '0'
	                            && st.charAt(i + 1) <= '7') {
	                        code += st.charAt(i + 1);
	                        i++;
	                    }
	                }
	                sb.append((char) Integer.parseInt(code, 8));
	                continue;
	            }
	            switch (nextChar) {
	            case '\\':
	                ch = '\\';
	                break;
	            case 'b':
	                ch = '\b';
	                break;
	            case 'f':
	                ch = '\f';
	                break;
	            case 'n':
	                ch = '\n';
	                break;
	            case 'r':
	                ch = '\r';
	                break;
	            case 't':
	                ch = '\t';
	                break;
	            case '\"':
	                ch = '\"';
	                break;
	            case '\'':
	                ch = '\'';
	                break;
	            // Hex Unicode: u????
	            case 'u':
	                if (i >= st.length() - 5) {
	                    ch = 'u';
	                    break;
	                }
	                int code = Integer.parseInt(
	                        "" + st.charAt(i + 2) + st.charAt(i + 3)
	                                + st.charAt(i + 4) + st.charAt(i + 5), 16);
	                sb.append(Character.toChars(code));
	                i += 5;
	                continue;
	            }
	            i++;
	        }
	        sb.append(ch);
	    }
	    return sb.toString();
	}
}