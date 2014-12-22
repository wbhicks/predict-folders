package met;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;

import util.SkfldOrLogShUtils;

public class MatrixFolder {
	public File dirHandle;
	private AdvisingMatrix observedMatrixS;
	private AdvisingMatrix observedMatrixF;
	private AdvisingMatrix observedMatrixFD;
	private List<AdvisingMatrix> predictedMatrices = 
			new ArrayList<AdvisingMatrix>();
	private List<AdvisingMatrix> samplerMatrices = 
			new ArrayList<AdvisingMatrix>();
	private List<File> auxiliaryFiles = new ArrayList<File>();
	private long timeLastModifiedOnDisk;
	private boolean modifiedAfterLastSave = false;
	
	/**
	 * For unit tests only.
	 */
	public MatrixFolder() {}
	
	public MatrixFolder(File putativeDirHandle) {
		if (!putativeDirHandle.exists()) {
			putativeDirHandle.mkdir();
		} else if (!putativeDirHandle.isDirectory()) {
			// TODO ERROR
		}
		dirHandle = putativeDirHandle;
		refreshFromDisk();
	}
	
	public void refreshFromDisk() {
		timeLastModifiedOnDisk = dirHandle.lastModified();
		auxiliaryFiles = new ArrayList<File>();
		samplerMatrices = new ArrayList<AdvisingMatrix>();
		AdvisingMatrix amfd = null;
		AdvisingMatrix amf = null;
		AdvisingMatrix ams = null;
		boolean needToLoad = false;
		File[] fileArr = dirHandle.listFiles();
		for (File f : fileArr) {
			if (f.isFile()) {
				if (f.getName().toLowerCase().endsWith(".csv")) {
					if (f.getName().startsWith("0_")) { // is observed
						needToLoad = true;
						if (f.getName().toLowerCase().endsWith(
								"_float_debug.csv")) {
							amfd = new AdvisingMatrix(this, 0);
							amfd.setFile(f);
						} else if (f.getName().toLowerCase().endsWith(
								"_float.csv")) {
							amf = new AdvisingMatrix(this, 0);
							amf.setFile(f);
						} else { // is shorts
							ams = new AdvisingMatrix(this, 0);
							ams.setFile(f);
						}
					} else if (f.getName().startsWith("1000_")) { // is a sampler
						AdvisingMatrix newSampler = new AdvisingMatrix(this, 1000);
						newSampler.setFile(f);
						addSamplerMatrix(newSampler);
					} else { // is neither an observed nor a sampler matrix
						Integer code = null;
						if ((code = indicatesPredictedMatrix(
								f.getName())) != null) {
							AdvisingMatrix existingPM = findPredictedMatrix(f);
							if (existingPM != null) {
								existingPM.loadEfficiently();
							} else { // is new
								AdvisingMatrix newPM = new AdvisingMatrix(
										this, code);
								newPM.setFile(f);
								newPM.loadEfficiently();
								addPredictedMatrix(newPM);
							}
						} else { // has strange prefix
							auxiliaryFiles.add(f);
						}
					}						
				} else { // is not .csv
					auxiliaryFiles.add(f);
				}
			} else { // is dir
				auxiliaryFiles.add(f);
			}
		}
		if (needToLoad) {
			observedMatrixS = ams;
			observedMatrixF = amf;
			observedMatrixFD = amfd;
			if (observedMatrixS != null) {
				observedMatrixS.loadEfficiently();
			}
			if (observedMatrixF != null) {
				observedMatrixF.loadEfficiently();
			}
			if (observedMatrixFD != null) {
				observedMatrixFD.loadEfficiently();
			}
		}
	}
		
//	private AdvisingMatrix findPredictedMatrix(
//			AdvisingMatrix matcher) {
//		AdvisingMatrix result = null;
//		for (AdvisingMatrix pm : predictedMatrices) {
//			if (pm.getAlgoCode() == matcher.getAlgoCode()) {
//				result = new AdvisingMatrix(pm);
//			}
//		}
//		return result;
//	}
	
	private AdvisingMatrix findPredictedMatrix(File f) {
		AdvisingMatrix result = null;
		for (AdvisingMatrix pm : predictedMatrices) {
			if (pm.getFile() == f) {
				result = pm;
			}
		}
		return result;
	}
	
	public AdvisingMatrix getObservedMatrixS() {
		return observedMatrixS;
	}
	
	public void addPredictedMatrix(AdvisingMatrix ams) {
		predictedMatrices.add(ams);
	}
	
	public void addSamplerMatrix(AdvisingMatrix ams) {
		samplerMatrices.add(ams);
	}
	
	public boolean generatePredictedMatrix(int algoID) {
		int numOfUnguessableInitialCols = ((algoID < 4) ? algoID : 3);
		AdvisingMatrix ams = new AdvisingMatrix(this, algoID);
		ams.setFile(new File(dirHandle, "" + algoID + "_"
				+ SkfldOrLogShUtils.toSpacelessDateString(
						System.currentTimeMillis()) + ".csv"));
		for (ArrayList<Short> als : observedMatrixS.getS()) {
			ArrayList<Short> newRow = new ArrayList<Short>();
			for (int c = 0; c < als.size(); c++) {
				if (c >= numOfUnguessableInitialCols) {
					
					if (algoID < 4) {
					
						int sum = 0;
						for (int x = 0; x < algoID; x++) {
							int offset = x * -1 - 1;
							sum += als.get(c + offset);
						}
						float avg = sum / algoID;
						newRow.add((short) Math.round(avg));
					
					} else if (algoID == 4) {

						short yest = als.get(c - 1);
						short dayb4 = als.get(c - 2);
						short guess = (short)(yest + yest - dayb4);
						newRow.add(guess);
						
					}
					
					
				} else {
					newRow.add(Short.MIN_VALUE);
				}
			}
			ams.appendRowS(newRow);
		}
		return addSaveAndScorePredictedMatrix(ams);
	}
		
	private boolean addSaveAndScorePredictedMatrix(AdvisingMatrix arr) {
		boolean result = true;
		try {
			arr.saveEfficiently();
			arr.oldScore();
			addPredictedMatrix(arr);
		} catch (FileNotFoundException fnfe) { 
			result = false; 
		} catch (IllegalStateException ise) { 
			System.err.println("L158 " + ise.getMessage());
			result = false;
		}
		return result;
	}
	
	public List<AdvisingMatrix> getPredictedMatrices() {
		List<AdvisingMatrix> result = 
				new ArrayList<AdvisingMatrix>();
		for (AdvisingMatrix pm : predictedMatrices) {
			result.add(new AdvisingMatrix(pm));
		}
		return result;
	}
		
//		public void readPermanentArray(int numOfLevels) throws IOException {
//			workingMatrices = new short[numOfLevels][][];
//			for (byte level = 0; level < numOfLevels; level++) {
//				short[][] arrForLevel = readLevel("data/sample_" + level + ".csv",
//						Expect.SHORT);
//				workingMatrices[level] = arrForLevel;
//			}
//		}
//		
//		public void writePermanentArray(int numOfLevels) throws IOException {
//			for (byte level = 0; level < numOfLevels; level++) {
//				writeLevel(workingMatrices[level], "tmp/sample_" + level + "_out.csv");
//			}
//		}
		
	Integer indicatesPredictedMatrix(String filename) {
		Integer num = null;
		if (filename.endsWith(".csv")) {
			String beforeValidPrefix = filename.substring(
					0, filename.length() - 4);
			String beforeUnderscore = StringUtils.substringBefore(
					beforeValidPrefix, "_"); // returns all if no _
			if (StringUtils.containsOnly(beforeUnderscore, "0123456789")) {
				num = Integer.parseInt(beforeUnderscore);
			} // else return null
		} // else return null
		return num;
	}
	
	public long getTimeLastModifiedOnDisk() {
		return timeLastModifiedOnDisk;
	}

	public String toString(boolean verbose) {
		String result = "Name (dir): " + dirHandle.getName() 
				+ "     Time dir last modified: " 
				+ SkfldOrLogShUtils.toSpacelessDateString(timeLastModifiedOnDisk) 
				+ " (" + (modifiedAfterLastSave ? "HAS " : "Has NOT ")
				+ "been modified in memory since then.) ";
		if (verbose) {
			result += "\n \nobservedMatrixS: " + observedMatrixS
					+ "\nobservedMatrixF: " + observedMatrixF
					+ "\nobservedMatrixFD: " + observedMatrixFD;
			result += "\n" + predictedMatrices.size() + " predicted matrices" 
					+ (predictedMatrices.size() > 0 ? ":\n" : ".\n");
			for (AdvisingMatrix ams : predictedMatrices) {
				result += ams;
			}
			result += " \n" + samplerMatrices.size() + " sampler matrices" 
					+ (samplerMatrices.size() > 0 ? ":\n" : ".\n");
			for (AdvisingMatrix am : samplerMatrices) {
				result += am.getFile().getName() + "\n";
			}
			result += " \n" + auxiliaryFiles.size() + " auxiliary files" 
					+ (auxiliaryFiles.size() > 0 ? ":\n" : ".\n");
			for (File f : auxiliaryFiles) {
				result += f.getName() + "\n";
			}
		} else {
			result += "\nFiles for observed matrix: " 
					+ "   S? " + (observedMatrixS == null ? "none" : "" + 
							observedMatrixS.getModified())
					+ "   F? " + (observedMatrixF == null ? "none" : "" + 
							observedMatrixF.getModified())
					+ "   FD? " + (observedMatrixFD == null ? "none" : "" + 
							observedMatrixFD.getModified());
			result += "\n" + predictedMatrices.size() + " predicted matrices."
					+ " " + samplerMatrices.size() + " sampler matrices."
					+ " " + auxiliaryFiles.size() + " auxiliary files.\n";
		}
		return pp("     ", result + " \n");
	}
	
	@Override
	public String toString() {
		return toString(false);
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
		
}