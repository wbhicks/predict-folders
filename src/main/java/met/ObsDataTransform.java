package met;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;

public class ObsDataTransform {

	public static final String REQD_EXTENSION = ".csv";
	public static final String REQD_ROWCODE = "TMAX";
	public static final String MISSING_DATA = "" + Short.MIN_VALUE;
	private static File dir = null;
	private static BufferedWriter bwForRowByRow = null;
	private static CSVPrinter writerForRowByRow = null;
	private static CSVParser parser = null;
	private static int firstYear;
	private static short maxObsToWatch = 1000;
	
	/**
	 * There are two invocation patterns at the moment, chosen via args[0]:
	 * 
	 * (1) To process a raw CSV:
	 * 
	 * java -classpath build/libs/Hib324sp1_t3.jar:lib/commons-lang3-3.1.jar:l
	 * ib/commons-csv-1.0.jar met/ObsDataTransform transform etl 1999.csv 1999
	 * 
	 * (2) To analyze the resulting "flattened" CSV:
	 * 
	 * java -classpath build/libs/Hib324sp1_t3.jar:lib/commons-lang3-3.1.jar:l
	 * ib/commons-csv-1.0.jar met/ObsDataTransform verify etl 1999_TMAX_noCru
	 * ft_byLoc_flat.csv 1999 498
	 * 
	 * In both cases, args[1] is the dir, under this one, to be used; args[2] is
	 * the input file; args[3] is the first (so far, the only) year appearing in
	 * the input file; and args[4] (which is used by the verify subroutine only)
	 * is the maximum value whose frequency should be individually tallied.
	 * 
	 * In the verify subroutine, a frequency count is generated for each 
	 * individual value between (args[4] * -1) and args[4] inclusive.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		dir = new File(args[1]);
		File f = new File(dir, args[2]);
		firstYear = Integer.parseInt(args[3]);
		if (args[0].equalsIgnoreCase("transform")) {
			if (args[2].endsWith(REQD_EXTENSION)) {
				File filteredByRecord = filterWholeRecords(f);
				File filteredByField = filterWithinRecord(filteredByRecord);
				File sortedByLoc = sortByLoc(filteredByField);
				System.out.println("Collapsing ...");
				File collapsedByLoc = collapseEachLoc(sortedByLoc);
			} // else err TODO
		} else if (args[0].equalsIgnoreCase("verify")) {
			maxObsToWatch = Short.parseShort(args[4]);
			showCurve(f);
		}
	}
	
	private static File filterWholeRecords(File rawFile) {
		String beforeExtension = rawFile.getName().substring(
				0, rawFile.getName().length() - REQD_EXTENSION.length());
		File fileNew = new File(dir, beforeExtension + "_" 
				+ REQD_ROWCODE + REQD_EXTENSION);
		try {
			parser = CSVParser.parse(rawFile, Charset.forName("US-ASCII"), 
					CSVFormat.RFC4180);
			bwForRowByRow = new BufferedWriter(new FileWriter(fileNew));
			writerForRowByRow = new CSVPrinter(bwForRowByRow, 
					CSVFormat.RFC4180);
			for (CSVRecord csvRecord : parser) {
				boolean saveThisRecord = false;
				int column = 0;
				for (String s : csvRecord) {
					column++;
					if (column == 3 && s.equals("TMAX")) {
						saveThisRecord = true;
					}
				}
				if (saveThisRecord) {
					writerForRowByRow.printRecord(csvRecord);
				}
			}
		} catch (IOException e) { e.printStackTrace(); }
		try {
			bwForRowByRow.close();
		} catch (IOException e) { e.printStackTrace(); }
		return fileNew;
	}
	
	private static File filterWithinRecord(File in) {
		String beforeExtension = in.getName().substring(
				0, in.getName().length() - REQD_EXTENSION.length());
		File out = new File(dir, beforeExtension + "_" 
				+ "noCruft" + REQD_EXTENSION);
		try {
			parser = CSVParser.parse(in, Charset.forName("US-ASCII"), 
					CSVFormat.RFC4180);
			bwForRowByRow = new BufferedWriter(new FileWriter(out));
			writerForRowByRow = new CSVPrinter(bwForRowByRow, 
					CSVFormat.RFC4180);
			for (CSVRecord csvRecord : parser) {
				int column = 0;
				for (String s : csvRecord) {
					column++;
					if (column == 1 || column == 2 || column == 4) {
						writerForRowByRow.print(s);
					}
				}
				writerForRowByRow.println();
			}
		} catch (IOException e) { e.printStackTrace(); }
		try {
			bwForRowByRow.close();
		} catch (IOException e) { e.printStackTrace(); }
		return out;
	}
	
	private static File sortByLoc(File in) {
		String beforeExtension = in.getName().substring(
				0, in.getName().length() - REQD_EXTENSION.length());
		File out = new File(dir, beforeExtension + "_" 
				+ "byLoc" + REQD_EXTENSION);
		try {
			// using sort's --output option is easier than trying to get a
			// unix redirect (>) to work:
			String cmd = "sort " + in.getPath() + " --output=" + out.getPath();
			System.out.println("About to execute command:\n     " + cmd);
			Process p = Runtime.getRuntime().exec(cmd);
			BufferedReader stdIn = new BufferedReader(new 
					InputStreamReader(p.getInputStream()));
			BufferedReader stdErr = new BufferedReader(new 
	            	InputStreamReader(p.getErrorStream()));
            System.out.print("stdIn: ");
            String s;
            while ((s = stdIn.readLine()) != null) {
                System.out.println(s);
            }
            System.out.print("stdErr: ");
            while ((s = stdErr.readLine()) != null) {
                System.out.println(s);
            }	
		} catch (IOException e) { 
			System.err.println("L107 " + e.getMessage());
			e.printStackTrace(); }
		return out;
	}

	private static File collapseEachLoc(File in) {
		String beforeExtension = in.getName().substring(
				0, in.getName().length() - REQD_EXTENSION.length());
		File fileNew = new File(dir, beforeExtension + "_" 
				+ "flat" + REQD_EXTENSION);
		try {
			parser = CSVParser.parse(in, Charset.forName("US-ASCII"), 
					CSVFormat.RFC4180);
			bwForRowByRow = new BufferedWriter(new FileWriter(fileNew));
			writerForRowByRow = new CSVPrinter(bwForRowByRow, 
					CSVFormat.RFC4180);
			writerForRowByRow.print("LOC");
			String dateForHeader = "" + firstYear + "0101";
			int safeguardForHeader = 0;
			while (safeguardForHeader++ < 370 
					&& !dateForHeader.equals("" + (firstYear + 1) + "0101")) {
				writerForRowByRow.print(dateForHeader);
				dateForHeader = nextDate(dateForHeader);
			}
			String currLoc = "";
			String currDate = "";
			int blocksOfMissingData = 0;
			int cellsOfMissingData = 0;
			int lengthOfChampionBlock = 0;
			int streakOfMissingCells = 0;
			for (CSVRecord csvRecord : parser) {
				boolean isFirstRecordForThisLoc = false;
				int column = 0;
				for (String s : csvRecord) {
					column++;
					if (column == 1 && !s.equals(currLoc)) { // start of new loc
						isFirstRecordForThisLoc = true;
						currLoc = s;
						currDate = "" + (firstYear - 1) + "1231";
						writerForRowByRow.println();
						writerForRowByRow.print(s);
					} else if (column == 2) { // should be next day
						if (areSequentialDates(currDate, s)) {
							currDate = s;
						} else {
							blocksOfMissingData++;
							int safeguard = 0;
							while (!areSequentialDates(currDate, s)) {						
								if (++safeguard > 365) {
									throw new IllegalStateException(
										"Loc " + currLoc + 
										" has been down for " + safeguard +
										" days!");
								}
								currDate = nextDate(currDate);
								cellsOfMissingData++;
								streakOfMissingCells++;
								writerForRowByRow.print(MISSING_DATA);
							}
							if (streakOfMissingCells > lengthOfChampionBlock) {
								lengthOfChampionBlock = streakOfMissingCells;
							}
							streakOfMissingCells = 0;
							currDate = s;
						}
					} else if (column == 3) {
						writerForRowByRow.print(s);
					}
				}
			}
			writerForRowByRow.println();
			System.out.println("Missing " + cellsOfMissingData 
					+ " cells clustered in " + blocksOfMissingData 
					+ " blocks. Longest block: " + lengthOfChampionBlock 
					+ " cells.");
		} catch (IOException e) { 
			System.err.println("L169 " + e.getMessage());
			e.printStackTrace(); 
		}
		try {
			bwForRowByRow.close();
		} catch (IOException e) { 
			System.err.println("L175 " + e.getMessage());
			e.printStackTrace(); 
		}
		return fileNew;
	}
	
	/**
	 * TODO would it be faster to just invoke nextDate and compare to date2?
	 * 
	 * Preconditions: the date representations look like "19710203" for Feb. 3rd
	 * 1971, and all dates are valid (e.g. no 31st of June; no 29th of Feb. in a
	 * non-leap year).
	 * 
	 * @param date1
	 * @param date2
	 * @return
	 */
	private static boolean areSequentialDates(String date1, String date2) {
		if (date1.equals("")) {
			return true; // first record is about to be processed
		}
		boolean result = false;
		int y1 = Integer.parseInt(date1.substring(0, 4));
		int y2 = Integer.parseInt(date2.substring(0, 4));
		int m1 = Integer.parseInt(date1.substring(4, 6));
		int m2 = Integer.parseInt(date2.substring(4, 6));
		int d1 = Integer.parseInt(date1.substring(6, 8));
		int d2 = Integer.parseInt(date2.substring(6, 8));
		if (y1 == y2) {
			if (m1 == m2) {
				result = (d1 + 1 == d2);
			} else if ((m1 + 1 == m2) && d2 == 1) {
				result = isLastDayOfMonth(y1, m1, d1);
			} else {
				result = false;
			}
		} else {
			result = ((y1 + 1 == y2) && m1 == 12 && m2 == 1 && d1 == 31 
					&& d2 == 1);
		}
		return result;
	}
	
	private static String nextDate(String date1) throws IllegalStateException {
		if (date1.equals("")) { // first record is about to be processed
			throw new IllegalStateException("Do not invoke at start");
		}
		int y1 = Integer.parseInt(date1.substring(0, 4));
		int m1 = Integer.parseInt(date1.substring(4, 6));
		int d1 = Integer.parseInt(date1.substring(6, 8));
		if (isLastDayOfMonth(y1, m1, d1)) {
			if (m1 == 12) {
				String y2 = "" + (y1 + 1);
				String m2 = "01";
				String d2 = "01";
				return y2 + m2 + d2;
			} else {
				String y2 = "" + y1;
				String m2 = ((m1 + 1) < 10 ? "0" : "") + (m1 + 1);
				String d2 = "01";
				return y2 + m2 + d2;
			}
		} else {
			String y2 = "" + y1;
			String m2 = (m1 < 10 ? "0" : "") + m1;
			String d2 = ((d1 + 1) < 10 ? "0" : "") + (d1 + 1);
			return y2 + m2 + d2;
		}
	}
	
	private static boolean isLastDayOfMonth(int y1, int m1, int d1) {
		return 
			(d1 == 30 && (m1 == 4 || m1 == 6 || m1 == 9 || m1 == 11)) 
				||
			(d1 == 31 && (m1 == 1 || m1 == 3 || m1 == 5 || m1 == 7 || 
						  m1 == 8 || m1 == 10 || m1 == 12)) 
				||
			(d1 == 28 && m1 == 2 && !isLeapYear(y1)) 
				||
			(d1 == 29 && m1 == 2 && isLeapYear(y1));
	}
	
	private static boolean isLeapYear(int year) {
		return (year % 4 == 0) && (year % 100 != 0) || (year % 400 == 0);
	}
	
	private static File showCurve(File in) {
		int[] freq = new int[maxObsToWatch * 2 + 1];
		boolean lookingAtMissingData = false;
		int blocksOfMissingData = 0;
		int cellsOfMissingData = 0;
		int lengthOfChampionBlock = 0;
		int streakOfMissingCells = 0;
		int numOfExcessives = 0;
		short maxValueSeen = Short.MIN_VALUE;
		short minUsefulValueSeen = Short.MAX_VALUE;
		try {
			parser = CSVParser.parse(in, Charset.forName("US-ASCII"), 
					CSVFormat.RFC4180);
			int record = 0;
			for (CSVRecord csvRecord : parser) {
				record++; // counting from 1 as though a spreadsheet
				if (record > 1) { // skip header row
					int column = 0;
					for (String cell : csvRecord) {
						column++; // counting from 1 as though a spreadsheet
						if (column > 1) { // skip loc name
							short obs = Short.parseShort(cell);
							if (obs == Short.MIN_VALUE) {
								if (!lookingAtMissingData) {
									lookingAtMissingData = true;
									blocksOfMissingData++;
								}
								cellsOfMissingData++;
								streakOfMissingCells++;
							} else {
								if (streakOfMissingCells > lengthOfChampionBlock) {
									lengthOfChampionBlock = streakOfMissingCells;
								}
								streakOfMissingCells = 0;
								lookingAtMissingData = false;
								if (obs > maxValueSeen) {
									maxValueSeen = obs;
								}
								if (obs < minUsefulValueSeen) {
									minUsefulValueSeen = obs;
								}	
								if (obs >= (maxObsToWatch * -1) 
										&& obs <= maxObsToWatch) {
									freq[obs + maxObsToWatch]++;
								} else {
									numOfExcessives++;
								}
							}
						}
					}
				}
			}
		} catch (IOException e) { 
			System.err.println("L265 " + e.getMessage());
			e.printStackTrace(); 
		}
		
		// write it:

		String beforeExtension = in.getName().substring(
				0, in.getName().length() - REQD_EXTENSION.length());
		File fileNew = new File(dir, beforeExtension + "_" 
				+ "stat" + REQD_EXTENSION);
		try {
			bwForRowByRow = new BufferedWriter(new FileWriter(fileNew));
			writerForRowByRow = new CSVPrinter(bwForRowByRow, 
					CSVFormat.RFC4180);
			String summaryForHeaderRow = "Missing " + cellsOfMissingData 
					+ " cells clustered in " + blocksOfMissingData 
					+ " blocks. Longest block: " + lengthOfChampionBlock 
					+ " cells. Observations outside the range below: "
					+ numOfExcessives + ". maxValueSeen: " + maxValueSeen 
					+ ". minUsefulValueSeen: " + minUsefulValueSeen;
			writerForRowByRow.print(summaryForHeaderRow);
			writerForRowByRow.println();
			for (int i = maxObsToWatch * -1; i <= maxObsToWatch; i++) {
				writerForRowByRow.print(i);
			}
			writerForRowByRow.println();
			for (int i = maxObsToWatch * -1; i <= maxObsToWatch; i++) {
				writerForRowByRow.print(freq[i + maxObsToWatch]);
			}
			writerForRowByRow.println();
		} catch (IOException e) { 
			System.err.println("L300 " + e.getMessage());
			e.printStackTrace(); 
		}
		try {
			bwForRowByRow.close();
		} catch (IOException e) { 
			System.err.println("L306 " + e.getMessage());
			e.printStackTrace(); 
		}
		return fileNew;
	}
	
}
