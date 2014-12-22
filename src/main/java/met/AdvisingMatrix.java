package met;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.ArrayUtils;

import util.SkfldOrLogShUtils;

public class AdvisingMatrix/*<T extends Number>*/ {

	/**
	 * no longer private, to allow unit testing
	 */
	enum Modified { MEM_NEWER, IN_SYNC, DISK_NEWER, UNKNOWN }

	private Method copyMethod;
	private int safeNumOfCols;
	private long arrayModifiedTime;
	private Modified mod;
	private File file;
	private boolean isSaveableRowByRow = false;
	private BufferedWriter bwForRowByRow = null;
	private CSVPrinter writerForRowByRow = null;
//	private ArrayList<ArrayList<T>> mat; // should never be null
	private ArrayList<ArrayList<Short>> matS; // should never be null
	private ArrayList<ArrayList<Float>> matF; // should never be null
	// since removal of MatrixTriple:
	private MatrixFolder parent;
	private int algoCode;
	private String algoDesc;
	private ArrayList<Double> aldForRowScores = new ArrayList<Double>();
	private double avgInaccuracy = 0.0;

	public AdvisingMatrix(MatrixFolder parent, int algoCode) {
		this.parent = parent;
		this.algoCode = algoCode;
		safeNumOfCols = 0;
		arrayModifiedTime = System.currentTimeMillis();
		mod = Modified.UNKNOWN;
//		mat = new ArrayList<ArrayList<T>>();
		matF = new ArrayList<ArrayList<Float>>();
		matS = new ArrayList<ArrayList<Short>>();
		Class clazz = Short.class;
		try {
			// notice the lowercase short:
			copyMethod = clazz.getDeclaredMethod("valueOf", short.class);
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public AdvisingMatrix(MatrixFolder parent, int algoCode, String algoDesc) {
		this(parent, algoCode);
		this.algoDesc = algoDesc;
	}
	
	public AdvisingMatrix(AdvisingMatrix other) {
		parent = other.parent;
		algoCode = other.algoCode;
		update(other);
	}

	public void update(AdvisingMatrix other) {
		if (parent == other.parent && algoCode == other.algoCode) {
			copyMethod = other.copyMethod;
			safeNumOfCols = other.safeNumOfCols;
			arrayModifiedTime = other.arrayModifiedTime;
			mod = other.mod;
			file = other.file;
			if (other.matF == null) {
				matF = new ArrayList<ArrayList<Float>>();
			} else {
				matF = new ArrayList<ArrayList<Float>>(other.matF.size());
				for (ArrayList<Float> row : 
					(ArrayList<ArrayList<Float>>) (other.matF)) {
					ArrayList<Float> newRow = new ArrayList<Float>(row.size());
					for (Float num : row) {
						try {
							Float copyOfNum = 
									(Float) copyMethod.invoke(null, num);
							newRow.add(copyOfNum);
						} catch (Exception e) {} // TODO
					}
					matF.add(newRow);
				}
			}
			if (other.matS == null) {
				matS = new ArrayList<ArrayList<Short>>();
			} else {
				matS = new ArrayList<ArrayList<Short>>(other.matS.size());
				for (ArrayList<Short> row : 
					(ArrayList<ArrayList<Short>>) (other.matS)) {
					ArrayList<Short> newRow = new ArrayList<Short>(row.size());
					for (Short num : row) {
						try {
							Short copyOfNum = 
									(Short) copyMethod.invoke(null, num);
							newRow.add(copyOfNum);
						} catch (Exception e) {} // TODO
					}
					matS.add(newRow);
				}
			}
		} else {
			System.err.println("L34 ERROR");
		}
	}

	public AdvisingMatrix floatsToShorts() {
		AdvisingMatrix result = new AdvisingMatrix(
				parent, algoCode);
		if (matF == null) { // TODO err
			result.matS = new ArrayList<ArrayList<Short>>();
		} else {
			result.matS = new ArrayList<ArrayList<Short>>(matF.size());
			for (ArrayList<Float> al : matF) {
				ArrayList<Short> newAL = new ArrayList<Short>(al.size());
				for (Object objUnk : al) {
					Short newObj = Short.MIN_VALUE; // must overwrite
					try {
						// rounds to the right in the case of a tie:
						Float objFl = (Float) objUnk;
						int currentAsInt = Math.round(objFl * 100);
						if ((currentAsInt < Short.MAX_VALUE - 1)
								&& (currentAsInt > Short.MIN_VALUE + 1)) {
							newObj = Short.valueOf((short) currentAsInt);
						} // TODO else error
						newAL.add(newObj);
					} catch (Exception e) {} // TODO
				}
				result.matS.add(newAL);
			}
			// copyOf suffices only because these are primitives:
			// result[i] = Arrays.copyOf(a[i], a[i].length);
		}
		return result;
	}

// covered	
	public Float getF(int rowNum, int colNum) {
		Float result = null;
		if (matF.size() > 0 && matF.size() > rowNum 
				&& rowNum >= 0 && colNum >= 0) {
			ArrayList<Float> row = matF.get(rowNum);
			if (row != null && row.size() > 0 && row.size() > colNum) {
				result = row.get(colNum);
			}
		}
		return result;
	}

	public Short getS(int rowNum, int colNum) {
		Short result = null;
		if (matS.size() > 0 && matS.size() > rowNum 
				&& rowNum >= 0 && colNum >= 0) {
			ArrayList<Short> row = matS.get(rowNum);
			if (row != null && row.size() > 0 && row.size() > colNum) {
				result = row.get(colNum);
			}
		}
		return result;
	}

	private ArrayList<Float> getF(int row) {
		return matF.get(row);
	}

	public ArrayList<Short> getS(int row) {
		return matS.get(row);
	}

	public ArrayList<ArrayList<Float>> getF() { return matF; }
	public ArrayList<ArrayList<Short>> getS() { return matS; }

	public MatrixFolder getParent() { return parent; }
	public int getAlgoCode() { return algoCode; }
	public boolean isInMemF() { return matF.size() > 0; }
	public boolean isInMemS() { return matS.size() > 0; }

	public int getNumOfRowsF() { return matF.size(); }
	public int getNumOfRowsS() { return matS.size(); }
	
	public double calcAvgInaccuracy() {
		System.out.println("L203!!!");
		int index = 0;
		double sumOfRowScores = 0.0; // TODO wrong unless all rows are same length
		for (double d : aldForRowScores) {
			sumOfRowScores += d;
			index++;
		}
		avgInaccuracy = sumOfRowScores / (double) index; 
		System.out.println("L211 " + avgInaccuracy + " " + index 
				+ getAvgInaccuracy());
		return avgInaccuracy;
	}

	public double getAvgInaccuracy() {
		return avgInaccuracy;
	}
	
	public long getLastMemModified() {
		return arrayModifiedTime;
	}

	public Modified getModified() {
		return mod;
	}

	public File getFile() {
		return file;
	}
	
	public void setFile(File file) {
		this.file = file;
		long lm = file.lastModified();
		if (file != null && file.exists() && !file.isDirectory()) {
			if (lm > arrayModifiedTime) {
				mod = Modified.DISK_NEWER;
			} else if (lm != 0L && lm < arrayModifiedTime) {
				System.err.println("L218 " + file.getName() + " " + lm + " " + arrayModifiedTime);
				mod = Modified.MEM_NEWER;
			} else { // unlikely? Except for 0L ...
				mod = Modified.UNKNOWN; // IN_SYNC;
			}
		} else { // TODO ERROR
			mod = Modified.UNKNOWN;
		}
	}

	/**
	 * @return a number guaranteed to be >= the length of the longest row
	 */
	public int getSafeNumOfCols() {
		return safeNumOfCols;
	}

// covered
	/**
	 * Has no effect if t is null.
	 * 
	 * @param t
	 */
	public void appendToLastRowF(Float t) {
		if (t != null) {
			ArrayList<Float> lastRow;
			if (matF.size() > 0) {
				lastRow = matF.get(matF.size() - 1);
				lastRow.add(t);
			} else {
				lastRow = new ArrayList<Float>();
				lastRow.add(t);
				matF.add(lastRow);
			}
			if (lastRow.size() > safeNumOfCols) {
				safeNumOfCols = lastRow.size();
			}
		}
	}

	public void appendToLastRowS(Short t) {
		if (t != null) {
			ArrayList<Short> lastRow;
			if (matS.size() > 0) {
				lastRow = matS.get(matS.size() - 1);
				lastRow.add(t);
			} else {
				lastRow = new ArrayList<Short>();
				lastRow.add(t);
				matS.add(lastRow);
			}
			if (lastRow.size() > safeNumOfCols) {
				safeNumOfCols = lastRow.size();
			}
		}
	}

	/**
	 * Has no effect if newRow is null or of length 0.
	 * 
	 * @param newRow
	 */
	public void appendRowF(ArrayList<Float> newRow) {
		if (newRow != null && newRow.size() > 0) {
//			System.err.print("{L266} newRow= << ");
			for (Object f1 : newRow) {
//				System.err.print(" " + f1);
			}
//			System.err.println(" >>\n");
			matF.add(newRow);
			if (newRow.size() > safeNumOfCols) {
				safeNumOfCols = newRow.size();
			}
		}
	}

	/**
	 * Has no effect if newRow is null or of length 0.
	 * 
	 * @param newRow
	 */
	public void appendRowS(ArrayList<Short> newRow) {
		if (newRow != null && newRow.size() > 0) {
			matS.add(newRow);
			if (newRow.size() > safeNumOfCols) {
				safeNumOfCols = newRow.size();
			}
		}
	}

	public boolean makeSaveableRowByRow() throws FileNotFoundException {
		boolean success = true;
//		System.err.println("L309 " + matS.size());
		if (file == null) {
			success = false;
			throw new FileNotFoundException("No file specified! {L312}");
		}
//		if (matF.size() == 0 && matS.size() == 0) {
//			success = false;
//			throw new IllegalStateException(
//					"matF and matS are both zero-length! {L316}");
//		}
		if (isSaveableRowByRow) {
			success = false;
			throw new IllegalStateException("is already saveable RowByRow! {L319}");
		}
		try {
			bwForRowByRow = new BufferedWriter(new FileWriter(file));
			writerForRowByRow = new CSVPrinter(bwForRowByRow, CSVFormat.RFC4180);
		} catch (IOException ioe) {
			success = false;
//			System.err.println("L209d");
		} // TODO
		if (success) {
			isSaveableRowByRow = true;
		}
		return success;
	}

	public boolean saveLastRow() throws IOException {
		boolean success = true;
		if (file.getName().contains("float")) {
			writerForRowByRow.printRecord(matF.get(matF.size() - 1));
		} else { // are Shorts
			writerForRowByRow.printRecord(matS.get(matS.size() - 1));
		}
		return success;
	}
	
	public boolean unmakeSaveableRowByRow() {
		boolean success = true;
		if (!isSaveableRowByRow) {
			success = false;
			throw new IllegalStateException("is not saveable RowByRow! {L344}");
		}
		try {
			bwForRowByRow.close();
		} catch (IOException ioe) {
			success = false;
//			System.err.println("L209d");
		} // TODO
		if (success) {
			isSaveableRowByRow = false;
		}
		return success;
	}
	
	public boolean saveEfficiently() throws FileNotFoundException {
		boolean result = true;
//		System.err.println("L309 " + matS.size());
		if (file == null) {
			throw new FileNotFoundException("No file specified! {L191}");
		}
		if (matF.size() == 0 && matS.size() == 0) {
			throw new IllegalStateException(
					"matF and matS are both zero-length! {L193}");
		}
		if (isSaveableRowByRow) {
			throw new IllegalStateException("must not invoke "
					+ "saveEfficiently() when isSaveableRowByRow! {L195}");
		}
		if (file.lastModified() > arrayModifiedTime) {
			mod = Modified.DISK_NEWER;
		}
		if (mod == Modified.MEM_NEWER || mod == Modified.UNKNOWN) {
			try {
				BufferedWriter bw = new BufferedWriter(new FileWriter(file));
				CSVPrinter writer = new CSVPrinter(bw, CSVFormat.RFC4180);
				if (file.getName().contains("float")) {
					for (ArrayList<Float> row : matF) {
						writer.printRecord(row);
					}
				} else { // are Shorts
					for (ArrayList<Short> row : matS) {
						writer.printRecord(row);
					}
				}
				bw.close();
//					System.err.println("L209c");
				mod = Modified.IN_SYNC;
			} catch (IOException ioe) {
				result = false;
//				System.err.println("L209d");
			} // TODO
		} else {
			result = false;
		}
		return result;
	}

// covered
	public boolean loadEfficiently() {
//		System.err.println("L232 " + mod + " " + file.getAbsolutePath());
//		System.err.println("L233 " + file.exists());
		boolean result = true;
		if (file != null && file.exists() && !file.isDirectory()) {
			System.err.println("L360 " + file.getName() + " " + mod);
			if (file.lastModified() > arrayModifiedTime) {
				mod = Modified.DISK_NEWER;
			}
//			if (mod == Modified.DISK_NEWER || mod == Modified.UNKNOWN) {
				//			System.err.println("L223 " + mod);
				int numberOfRecords = 0;
				CSVParser parser = null;
				try {
					LineNumberReader recordCounter = new LineNumberReader(
							new FileReader(file));
					//				System.err.println("L226");
					recordCounter.skip(Integer.MAX_VALUE);
					//				System.err.println("L228");
					numberOfRecords = recordCounter.getLineNumber();
					//				System.err.println("L230" + numberOfRecords);
					recordCounter.close();
					//				System.err.println("L232");
					parser = CSVParser.parse(file,
							Charset.forName("US-ASCII"), CSVFormat.RFC4180);
					//				System.err.println("L235");
				} catch (IOException e) {}
				safeNumOfCols = 0;
				if (file.getName().contains("float")) {
					matF = new ArrayList<ArrayList<Float>>(numberOfRecords);
					//				System.err.println("L237");
					for (CSVRecord csvRecord : parser) {
						ArrayList<Float> currRow = 
								new ArrayList<Float>(csvRecord.size());
						for (String s : csvRecord) {
							currRow.add(Float.valueOf(s));
						}
						matF.add(currRow);
						safeNumOfCols = ((currRow.size() > safeNumOfCols) ?
								currRow.size() : safeNumOfCols);
					}
				} else { // are Shorts
					matS = new ArrayList<ArrayList<Short>>(numberOfRecords);
					//				System.err.println("L237");
					for (CSVRecord csvRecord : parser) {
						ArrayList<Short> currRow = 
								new ArrayList<Short>(csvRecord.size());
						for (String s : csvRecord) {
							currRow.add(Short.valueOf(s));
							// arrForRecordAsShorts[column++] = Short.parseShort(s);
						}
						matS.add(currRow);
						safeNumOfCols = ((currRow.size() > safeNumOfCols) ?
								currRow.size() : safeNumOfCols);
					}
				}
				//			System.err.println("L240 " + arrayModifiedTime);
				try {
					Thread.sleep(42); // millis
				} catch (InterruptedException e) {}
				arrayModifiedTime = System.currentTimeMillis();
				//			System.err.println("L242 " + arrayModifiedTime);
				mod = Modified.IN_SYNC;
//			} else { // no need to load it
//				result = false;
//			}
		} else { // TODO ERROR - this file shouldn't have been chosen
			result = false;
		}
		return result;
	}

	public void oldScore() {
		int numOfPredictions = 0;
		int aggregateInaccuracy = 0;
		AdvisingMatrix obs = parent.getObservedMatrixS();		
		for (int rowNum = 0; rowNum < matS.size(); rowNum++) {
			for (int colNum = 0; colNum < matS.get(rowNum).size(); colNum++) {
				short currPrediction = getS(rowNum, colNum);
				if ((currPrediction > Short.MIN_VALUE + 1) &&
						(currPrediction < Short.MAX_VALUE - 1)) {
					int inaccuracy = Math.abs(
							currPrediction - obs.getS(rowNum, colNum));
					aggregateInaccuracy += inaccuracy;
					numOfPredictions++;
				}
			}
		}
		if (numOfPredictions > 0) {
			float avg = aggregateInaccuracy / numOfPredictions;
//			avgInaccuracy = avg;
		}
	}
	
	public void refineScore(ArrayList<Short> observedRow, 
			ArrayList<Short> predictedRow) {
		int sumOfCalculableDiscrepanciesForRow = 0;
		int numOfCalculableDiscrepancies = 0;
		int index = 0;
		for (short prediction : predictedRow) {
			if (prediction != Short.MIN_VALUE) {
				int discrepancy = Math.abs(prediction - observedRow.get(index));
				sumOfCalculableDiscrepanciesForRow += discrepancy;
				numOfCalculableDiscrepancies++;
			}
			index++;
		}
		double scoreForThisRow = (float) sumOfCalculableDiscrepanciesForRow / 
				(float) numOfCalculableDiscrepancies;
		aldForRowScores.add(scoreForThisRow);
	}
	
	/*
	 * TODO where do we do this - i.e. use non-mock floats:
	 * int tempInt = Math.round(Float.parseFloat(s) * 100);
	 * if ((tempInt < Short.MAX_VALUE - 1) && 
	 *						(tempInt > Short.MIN_VALUE + 1)) {
	 * tempShort = (short) tempInt;
	 */
	
	private String previewOnDisk() {
		String result = "";
		if (file != null && file.exists() && !file.isDirectory()) {
			try {
				LineNumberReader recordCounter = new LineNumberReader(
						new FileReader(file));
				recordCounter.skip(Integer.MAX_VALUE);
				int numberOfRecords = recordCounter.getLineNumber();
				recordCounter.close();
				CSVParser parser = CSVParser.parse(file,
						Charset.forName("US-ASCII"), CSVFormat.RFC4180);
				int numOfRecordsSeen = 0,
						minNumOfColumnsSeen = Integer.MAX_VALUE,
						maxNumOfColumnsSeen = Integer.MIN_VALUE,
						minUsefulValueSeen = Integer.MAX_VALUE, 
						maxValueSeen = Integer.MIN_VALUE,
						minValForType = Short.MIN_VALUE;
				boolean hasMinValForType = false;
				for (CSVRecord csvRecord : parser) {
					numOfRecordsSeen++;
					int column = 0;
					for (String s : csvRecord) {
						column++;
						int tempInt;
						if (file.getName().contains("float")) {
							Float t = Float.valueOf(s);
							tempInt = Math.round(Float.parseFloat(s) * 100);
						} else { // are Shorts
							Short t = Short.valueOf(s);
							tempInt = t;
						}
						if (tempInt > maxValueSeen) {
							maxValueSeen = tempInt;
						}
						if (tempInt < minUsefulValueSeen) {
							if (file.getName().contains("float")) {
								minUsefulValueSeen = tempInt;
							} else { // are Shorts
								if (tempInt == minValForType) {
									hasMinValForType = true;
								} else {
									minUsefulValueSeen = tempInt;
								}								
							}
						}
					}
					if (column > maxNumOfColumnsSeen) {
						maxNumOfColumnsSeen = column;
					}
					if (column < minNumOfColumnsSeen) {
						minNumOfColumnsSeen = column;
					}
				}
				result += ("" + numOfRecordsSeen + " records, " + minNumOfColumnsSeen
						+ " to " + maxNumOfColumnsSeen + " columns each, range "
						+ minUsefulValueSeen + " to " + maxValueSeen 
						+ ". (The MIN_VALUE for this Java type was "
						+ (hasMinValForType ? "also " : "not ")
						+ "seen.)");
			} catch (Exception e) {} // TODO
		}
		return result;
	}
	
	@Override
	public String toString() {
		String result = "\nAlgo #" + algoCode + 
				(algoCode != 0 ? ("     Score: " + avgInaccuracy) : "") + 
				"     Filename: " + file.getName() + "     Parent (dir): " + 
				parent.dirHandle.getName() + 
				"\nTime last modified on disk:   " + 
				SkfldOrLogShUtils.toSpacelessDateString(file.lastModified()) +
				"\nTime last modified in memory: " + 
				SkfldOrLogShUtils.toSpacelessDateString(getLastMemModified()) +
				"     Mod status: " + mod + ".     This matrix is " + 
				((isInMemF() || isInMemS()) ? "" : "not ") + 
				"in memory. " +
				"\nNot more than " + safeNumOfCols + " columns. " +
				previewOnDisk() + ".\n";
		return pp("     ", result);
	}
	
	/**
	 * Convenience wrapper to de-clutter the code. Place in each class where
	 * SkfldOrLogShUtils.indented is used extensively.
	 * @param indenter
	 * @param s
	 * @return
	 */
	private String pp(String indenter, String s) {
		return SkfldOrLogShUtils.indented(indenter, s);
	}
	
	public short predictCell(ArrayList<Short> als, int curr) {
		return 42;
	}
	
	public ArrayList<Short> predictRow(ArrayList<Short> observedRow) {
		ArrayList<Short> predictedRow = new ArrayList<Short>(observedRow.size());
		for (int curr = 0; curr < observedRow.size(); curr++) {
			short predictedCurr = predictCell(observedRow, curr);
			predictedRow.add(predictedCurr);
		}
		return predictedRow;
	}
	
}
