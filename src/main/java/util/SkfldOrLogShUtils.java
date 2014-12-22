package util;

import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import met.ArrSerializer;

import org.apache.commons.lang3.StringUtils;

public class SkfldOrLogShUtils {

	private static CharsetEncoder encoderForASCII = 
		      Charset.forName("US-ASCII").newEncoder();
	
	public static String[] trimValues(String[] a) {
		if (a == null || a.length == 0) {
			return a;
		}
		String[] result = new String[a.length];
		for (int i = 0; i < a.length; i++) {
			if (a[i] != null) {
				result[i] = StringUtils.trimToEmpty(
						StringUtils.normalizeSpace(a[i]));
			}
		}
		return result;
	}	
	public static boolean isLineTerminator(char c) {
		return (c == '\r' || c == '\n'); // kludge
	}
	public static boolean isEscape(char c) {
		return 27 == (int) c;
	}
	public static boolean isPrintableOrSpaceInASCII(char c) {
		return isPrintableInASCII(c) || isSpaceInASCII(c);
	}
	public static boolean isPrintableInASCII(char c) {
		return (((int) c) >= 33) && (((int) c) <= 126);
	}
	public static boolean isSpaceInASCII(char c) {
		return ((int) c) == 32;
	}
	public static boolean isOnlyASCII(String s) {
		return encoderForASCII.canEncode(s);
	}
	public static boolean isOnlyASCII(char c) {
		return encoderForASCII.canEncode(String.valueOf(c));
	}
	public static String toLegible(char c) {
		String result;
		if (isPrintableOrSpaceInASCII(c)) {
			result =  String.valueOf(c);
		} else {
			result = "[" + ((int) c) + "=0x" 
					+ Integer.toHexString((int) c) + "]";
		}
		return result;
	}
	
	public static short[][] floatsToShorts(float[][] floats) {
		short[][] result = new short[floats.length][];
		for (int i = 0; i < floats.length; i++) {
			result[i] = floatsToShorts(floats[i]);
		}
		return result;
	}
	
	public static short[] floatsToShorts(float[] floats) {
		short[] result = new short[floats.length];
		for (int i = 0; i < floats.length; i++) {
			// rounds to the right in the case of a tie:
			int currentAsInt = Math.round(floats[i] * 100);
			if ((currentAsInt < Short.MAX_VALUE - 1) && 
					(currentAsInt > Short.MIN_VALUE + 1)) {
				result[i] = (short) currentAsInt;
			} // TODO else error
//			System.err.println(">" + i + "," + floats[i] + "," 
//					+ currentAsInt + "," + result[i]);
		}
		return result;
	}

	public static float[][] deepCopy2DArray(float[][] a) {
		if (a == null) {
			return null;
		}
		float[][] result = new float[a.length][];
		for (int i = 0; i < a.length; i++) {
			// copyOf suffices only because these are primitives:
			result[i] = Arrays.copyOf(a[i], a[i].length);
		}
		return result;
	}

	public static ArrayList<Short> toArrayList(short[] arr) {
		ArrayList<Short> result = new ArrayList<Short>();
		if (arr != null && arr.length > 0) {
			List list = Arrays.asList(arr);
			result = new ArrayList<Short>(list);
		}
		return result;
	}
	
	public static ArrayList<Float> toArrayList(float[] arr) {
		ArrayList<Float> result = new ArrayList<Float>();
		if (arr != null && arr.length > 0) {
//			System.err.print("{L113} arr= << ");
//			for (float f1 : arr) {
//				System.err.print(" " + f1);
//			}
//			System.err.println(" >>\n");
			List list = Arrays.asList(arr);
//			System.err.print("{L119} list= << ");
//			for (Object f1 : list) {
//				System.err.print(" " + f1);
//			}
//			System.err.println(" >>\n");
			result = new ArrayList<Float>(); //list);
			for (float f1 : arr) {
				result.add(Float.valueOf(f1));
			}
		}
//		System.err.print("{L126} result= << ");
		for (Float f1 : result) {
//			System.err.print(" " + f1 + "((");
		}
//		System.err.println(" )) >>\n");
		return result;
	}
	
	public static String toSpacelessDateString(long millisSinceEpoch) {
		SimpleDateFormat sdf = new SimpleDateFormat(
				"yyyy-MM-dd-HHmmss", Locale.US);
		Date t = new Date(millisSinceEpoch);
		return sdf.format(t);
	}
	
	public static String prettyPrintArr2D(short[][] arr) {
		String result = "";
		for (int row = 0; row < arr.length; row++) {
			for (int col = 0; col < arr[0].length; col++) {
				result += String.format("%6s", arr[row][col]);
			}
			result += "\n";
		}
		return result;
	}

	/**
	 * This method doesn't honor consecutive \n's unless they're separated.
	 * So, use "\n \n" instead of "\n\n" to get a blank line.
	 * @param indenter
	 * @param s
	 * @return
	 */
	public static String indented(String indenter, String s) {
		String[] sArr = s.split("\n");
		String result = "";
		for (String currLine : sArr) {
			result += indenter + currLine + "\n";
		}
		return result;
	}

	
	
	
	
	
	
	
	
}
