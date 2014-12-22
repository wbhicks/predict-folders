package skfld;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import org.apache.commons.lang3.StringUtils;

public class Props {
	private static Properties appProps;

	public static void load() {
		try {
			load("etc/default.properties", "etc/app.properties");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
		
	/**
	 * Must be called before any properties are used. May safely be called 
	 * more than once. The default file must exist or else a 
	 * FileNotFoundException will be thrown. The custom file (the application
	 * properties file) does not need to exist. If it does, its properties
	 * will override those taken from the default file.
	 * 
	 * The following code is from http://docs.oracle.com/jav
	 * ase/tutorial/essential/environment/properties.html
	 */
	public static void load(String defaultPropsFilename, String 
			customPropsFilename) throws FileNotFoundException, IOException {
		Properties defaultProps = new Properties();
		FileInputStream in = new FileInputStream(defaultPropsFilename);
		defaultProps.load(in);
		in.close();

		// create application properties with default
		appProps = new Properties(defaultProps);

		// now load properties from last invocation
		try {
			in = new FileInputStream(customPropsFilename);
			appProps.load(in);
			in.close();
		} catch (Exception e) {}
	}

	public static void store() {
		try {
			store("etc/app.properties");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void store(String customPropsFilename) throws IOException {
		FileOutputStream out = new FileOutputStream(customPropsFilename);
		appProps.store(out, " --- Custom Properties ---");
		out.close();
	}
	
	private static String deAliasOneLevel(String nominalKey) {
		String effectiveKey = nominalKey;
		String value = appProps.getProperty(effectiveKey);
		System.out.println("[49 " + value + "]");
//		while ((value = appProps.getProperty(effectiveKey)) 
//				!= null && value.startsWith("$alias")) {
//			System.out.println("[51 " + value + "]");
//			effectiveKey = value.substring(7);
//		}
		while ((value = appProps.getProperty(effectiveKey)) 
				!= null && value.startsWith("$alias")) {
			System.out.println("[51 " + value + "]");
			effectiveKey = value.substring(7);
		}
//		if (value != null) {
//			effectiveKey = value;
//		}
		return effectiveKey;
	}
	
	private static String resolveAsWord(String effectiveKey) {
		String value;
		while ((value = appProps.getProperty(effectiveKey)) 
				!= null && value.startsWith("$alias")) {
			effectiveKey = value.substring(7);
		}
		return effectiveKey;
	}
	
	private static String resolveWordByWord(String key) {
		String[] sArrIn = StringUtils.split(key, '.');
		String[] sArrOut = new String[sArrIn.length];
		for (int i = 0; i < sArrIn.length; i++) {
			sArrOut[i] = resolveAsWord(sArrIn[i]);
		}
		return StringUtils.join(sArrOut, '.');
	}
	
	private static String resolveByTruncating(String key) {
		System.out.println("!!!" + key + "???");
		if (key.equals("")) {
			return key;
		}
		if (key.equals(resolveAsWord(key))
				&& !key.contains(".")) {
			return key;
		}
		String value = appProps.getProperty(key);
		if (value != null && !value.startsWith("$alias")) {
			return key;
		}
		String left = "", right = key;
		boolean broke = false;
		while (right.length() > 0) {
			String[] rightAsArr = StringUtils.split(right, '.');
			for (int len = rightAsArr.length; len > 0; len--) {
				String current = StringUtils.join(rightAsArr, '.', 0, len);
				if ((!current.equals(resolveAsWord(current)))
						|| !current.contains(".")) {
					left = (left.equals("") ? "" : (left + "."))
							+ resolveAsWord(current);
					right = StringUtils.join(rightAsArr, '.', len, rightAsArr.length);
					broke = true;
					break;
				}
			}
		}
		return (left.equals(key) ? left : resolveByTruncating(left));
	}
	
	private static String deAlias(String key) {
		String left = "." + key;
		String right = "";
		do {
			System.out.println("[61 " + left + "]");
			left = "." + deAliasOneLevel(left.substring(1));
			System.out.println("[63 " + left + "]");
			int pos = left.lastIndexOf("."); // -1 iff DNE
			right = left.substring(pos + 1) + "." + right;
			left = (pos < 1 ? "" : left.substring(0, pos));
			System.out.println("[67 " + pos + "," + right 
					+ "," + left + "]");
		} while (left.contains("."));
		return right.substring(0, right.length() - 1);
	}
	
	public static String get(String key) {
		return get(key, "");
	}
	
	public static String get(String key, String defaultValue) {
//		return appProps.getProperty(deAlias(key), defaultValue);
		return appProps.getProperty(resolveByTruncating(key), defaultValue);
	}
	
	public static String set(String key, String value) {
		Object previousValue = appProps.setProperty(deAlias(key), value);
		String result = "";
		if (previousValue == null) {
			// leave result as ""
		} else if (previousValue instanceof String) {
			result = (String) previousValue;
		} else {
			/* Shouldn't occur, from my reading of the Javadocs. But the entry
			 * specifically for setProperty doesn't explicitly rule it out. See
			 * http://docs.oracle.com/javase/7/docs/api/java/util/Propert
			 * ies.html#setProperty%28java.lang.String,%20java.lang.String%29
			 */
			result = previousValue.toString();
		}
		return result;
	}
	
	
}
