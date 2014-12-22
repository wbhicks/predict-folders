package met;

import java.io.BufferedWriter;
import java.io.Console;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import util.SkfldOrLogShUtils;

public class ArrSerializer {
	
	private String floatFormat = "%7.4f, ";
	static MatrixFolder workingMF;
	private static List<MatrixFolder> knownMFs = new ArrayList<MatrixFolder>();
	private static MockObservation mo = new MockObservation();
		
	public interface MockAlgo {
		short calculate(short a, short b);
	}
	
	public static void main(String[] args) {
		ArrSerializer arrSer = new ArrSerializer();
		if ("interactive".equalsIgnoreCase(args[0])) {
			Console c = System.console();
			boolean exitRequested = false;
			while (!exitRequested) {
				String menu = "\n" + identifyWorkingMF() 
				+ "\n \n---------- MENU OF COMMANDS ----------\n" + pp("", 
  "SHORTCUTS: [1] work with new mock data; [2] apply an algorithm to the most recent data;\n"	
+ "FULL MENU: E[x]it; [s]can disk and refresh metadata; [g]enerate new matrix folder with mock data;\n"
+ "           Reports: [w]orking matrix folder; [a]ll known matrix folders; [b]riefly;\n"
+ "           The working matrix folder: [c]hange; [l]oad from disk; apply [p]rediction algorithm\n") 
+ "Select: ";
				String selection = c.readLine(menu);
				if ("1".equalsIgnoreCase(selection)) { // ok
					MatrixFolder mf = mo.manageMock(c);
					knownMFs.add(mf);
					workingMF = mf;
					arrSer.manageScanForMatrixFolders(c);
					// c.printf(identifyWorkingMF() + "\n");
				} else if ("2".equalsIgnoreCase(selection)) { // ok
					arrSer.manageScanForMatrixFolders(c);
					MatrixFolder mostRecentFound = null;
					for (MatrixFolder mf : knownMFs) {
						if (mostRecentFound == null || 
								mostRecentFound.getTimeLastModifiedOnDisk()
								< mf.getTimeLastModifiedOnDisk()) {
							mostRecentFound = mf;
						}
					}
					workingMF = mostRecentFound;
					arrSer.manageApplyAlgo(c);
				} else if ("a".equalsIgnoreCase(selection)) { // ok
					arrSer.manageReportKnownMFs(c, true);
				} else if ("b".equalsIgnoreCase(selection)) { // ok
					arrSer.manageReportKnownMFs(c, false);
				} else if ("c".equalsIgnoreCase(selection)) { // ok
					arrSer.manageSelectWorkingMF(c);
				} else if ("g".equalsIgnoreCase(selection)) { // ok
					knownMFs.add(mo.manageMock(c));
				} else if ("l".equalsIgnoreCase(selection)	 // ok
						&& (workingMF != null)) {
					workingMF.refreshFromDisk();
				} else if ("p".equalsIgnoreCase(selection)) {		 // ok
					arrSer.manageApplyAlgo(c);
				} else if ("s".equalsIgnoreCase(selection)) { // ok
					arrSer.manageScanForMatrixFolders(c);
				} else if ("w".equalsIgnoreCase(selection)) { // ok
					arrSer.manageReportWorkingMF(c, true);
				} else if ("x".equalsIgnoreCase(selection)) {
					exitRequested = true;
					// TODO nothing to clean up, for now, so ...
					return; // or System.exit(0);
				}
			}
		}
	}

// ok	
	/**
	 * Creates a new predicted matrix.
	 * @param c
	 */
	public void manageApplyAlgo(Console c) {
		if (workingMF == null) {
			c.printf("   You have not yet selected a working matrix folder.\n");
		} else {
			String s = c.readLine("   Select algorithm # to run in " +
					"working matrix folder, " + workingMF.dirHandle.getName() 
					+ " (default 1). EXPERIMENTAL: Select 42 to run all at once "
					+ "and also generate a battle-of-the-algos matrix: ");
			int algoID = Integer.parseInt(s.equals("") ? "1" : s);
			if (algoID != 42) {
				boolean success = workingMF.generatePredictedMatrix(algoID);
				// TODO extract function:
				c.printf("   The operation " + (success ? "succeeded" : "failed") + ".\n");		
			} else { // EXPERIMENTAL: Select 42 to run all at once
				PredictionAndScoring pas = new PredictionAndScoring(workingMF);
				try {
					pas.predictAndScore();
					String s2 = c.readLine("   Select row to use when"
							+ " generating the all-algorithm comparison"
							+ " matrix (default 0): ");
					int rowForSample = Integer.parseInt(s2.equals("") ? "0" : s2);
					pas.writeSampleAllAlgoMatrix(rowForSample);
				} catch (IOException ioe) {
					c.printf("{L119} " + ioe);
				}
			}
		}
	}

// ok	
	public void manageSelectWorkingMF(Console c) {
		c.printf(identifyWorkingMF());
		String s = c.readLine("Select matrix folder (enter directory name, or "
				+ "press return for no change): ");
		for (MatrixFolder mf : knownMFs) {
			if (mf.dirHandle.getName().equalsIgnoreCase(s)) {
				workingMF = mf;
			}
		}
		c.printf(identifyWorkingMF() + "\n");
	}
	
	private static String identifyWorkingMF() {
		if (workingMF == null) {
			return "   No matrix folder has been selected as the working " +
					"matrix folder. ";
		} else {
			return "   The working matrix folder is " + 
					workingMF.dirHandle.getName() + ". ";
		}
	}

// ok	
	public void manageReportWorkingMF(Console c, boolean verbose) {
		c.printf("---------- WORKING MATRIX FOLDER ----------\n", null);
		if (workingMF == null) {
			c.printf("   No matrix folder has been selected as the working " +
					"matrix folder. ", null);
		} else {
			c.printf(workingMF.toString(verbose), null);
		}
	}
		
// ok	
	public void manageReportKnownMFs(Console c, boolean verbose) {
		String labelForWMF = " (This is the working matrix folder.)";
		c.printf("---------- LIST OF KNOWN MATRIX FOLDERS ----------\n"
				+ "   There are currently %s known matrix folders", 
				knownMFs.size());
		c.printf((knownMFs.size() > 0 ? ":\n \n" : ".\n"), null);
		/* start with 1 not 0, because we're numbering them in a list for human
		 * consumption:
		 */
		int i = 1;
		for (MatrixFolder mf : knownMFs) {
			c.printf("---- Matrix Folder #" + i++, null);
			if (workingMF != null &&
					mf.dirHandle.getAbsolutePath().equalsIgnoreCase(
					workingMF.dirHandle.getAbsolutePath())) {		
				c.printf(labelForWMF, null);
			}
			c.printf(" ----\n", null);
			c.printf(mf.toString(verbose), null);
		}
	}
	
	public void manageScanForMatrixFolders(Console c) {
		int numNewGroupsAdded = 0;
		int numExistingGroupsUpdated = 0;
		List<File> dirHandles = getDirHandles(new File("data/met/in"));
		String result = "---------- RESULTS OF SCAN ----------\n"
				+ "   Found " + dirHandles.size() 
				+ " matrix folders.";
		if (dirHandles.size() > 0) {
			result += " W=The working matrix folder. N=New, U=Updated:\n "
					+ "\n      Time Last Modified  N/U   Directory Name\n" +
					"      ------------------  ---   --------------\n";
			for (File dir : dirHandles) {
				MatrixFolder mf = findKnownMFByDir(dir);
				boolean merelyUpdatedNotNew;
				if (mf != null) {
					mf.refreshFromDisk();
					numExistingGroupsUpdated++;
					merelyUpdatedNotNew = true;
				} else { // must add new MatrixGroup
					knownMFs.add(new MatrixFolder(dir));
					numNewGroupsAdded++;
					merelyUpdatedNotNew = false;
				}
				long timestamp = dir.lastModified();
				result += "    " + ((merelyUpdatedNotNew && workingMF != null &&
						mf.dirHandle.getAbsolutePath().equalsIgnoreCase(
						workingMF.dirHandle.getAbsolutePath())) ? "W " : "  ");
		    	result += SkfldOrLogShUtils.toSpacelessDateString(
		    			timestamp) + "    " + (merelyUpdatedNotNew ? "U" : "N") 
		    			+ "    " + dir.getName() + "\n";
			}
		}
		result += "\n   " + numExistingGroupsUpdated + 
				" existing matrix folders were updated. " + numNewGroupsAdded + 
				" new matrix folders were added. Total: " + 
				(numExistingGroupsUpdated + numNewGroupsAdded) + "\n";
		c.printf(result);
	}

	private MatrixFolder findKnownMFByDir(File dir) {
		MatrixFolder result = null;
		for (MatrixFolder mf : knownMFs) {
			try {
				if ((mf.dirHandle != null) && (dir != null) && 
						mf.dirHandle.getCanonicalPath().equalsIgnoreCase(
						dir.getCanonicalPath())) {
					result = mf;
				}
			} catch (IOException ioe) {}
		}
		return result;
	}
	
//	public void prettyPrintArr1D(float[] arr) {
//		for (int col = 0; col < arr.length; col++) {
//			System.out.print(String.format(floatFormat, arr[col]));
//		}
//	}
	
//	public void read(String filename) throws Exception {
//		File csvData = new File(filename);
//		CSVParser parser = CSVParser.parse(csvData, 
//				Charset.forName("US-ASCII"), CSVFormat.RFC4180);
//		int row = 0;
//		for (CSVRecord csvRecord : parser) {
//			if (!sizeOfFirstRecordIsKnown) {
//				sizeOfFirstRecordIsKnown = true;
//				sizeOfFirstRecord = csvRecord.size();
//				arr2D = new short[numberOfRecords][sizeOfFirstRecord];
//			}
//			arrForCurrentRecord = new short[sizeOfFirstRecord];
//			System.err.println("a" + csvRecord);
//			int col = 0;
//			for (String s : csvRecord) {
//				int tempInt = Math.round(Float.parseFloat(s) * 100);
//				short tempShort;
//				if ((tempInt < Short.MAX_VALUE - 1) && 
//						(tempInt > Short.MIN_VALUE + 1)) {
//					tempShort = (short) tempInt;
//				} else {
//					throw new Exception("huh?");
//				}
//				arrForCurrentRecord[col++] = tempShort;
//				System.err.println("col " + col + ":" + s + "=" + Float.parseFloat(s));
//			}
//			arr2D[row++] = arrForCurrentRecord;
//		}
//		System.err.println("==" + arr2D);
//	}
	
	private List<File> getDirHandles(File inputDir) {
		List<File> result = new ArrayList<File>();
		File[] allChildren = inputDir.listFiles();
		for (File f : allChildren) {
		    if (f.isDirectory()) {
		    	result.add(f);
		    }
		}
		return result;
	}
	
	/**
	 * Convenience wrapper to de-clutter the code. Place in each class where
	 * SkfldOrLogShUtils.indented is used extensively.
	 * @param indenter
	 * @param s
	 * @return
	 */
	private static String pp(String indenter, String s) {
		return SkfldOrLogShUtils.indented(indenter, s);
	}
	
}
