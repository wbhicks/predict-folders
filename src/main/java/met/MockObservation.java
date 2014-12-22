package met;

import java.io.Console;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import met.ArrSerializer.MockAlgo;
import util.SkfldOrLogShUtils;

public class MockObservation {
	
	private static final float PI2 = (float) (2 * Math.PI);
	
	public MatrixFolder manageMock(Console c) {
		String s = "";
		String defaultRelativeDirPath = "data/met/in/mock_" + 
				SkfldOrLogShUtils.toSpacelessDateString(
						System.currentTimeMillis());
		String relativeDirPath = ((s = c.readLine("New group name (default " +
				defaultRelativeDirPath + "): ")).equals("") 
				? defaultRelativeDirPath : s);
		int numOfRows = Integer.parseInt(((s = c.readLine(
				"numOfRows (default 3): ")).equals("") 
				? "3" : s));
		int numOfColumns = Integer.parseInt(((s = c.readLine(
				"numOfColumns (default 4): ")).equals("") 
				? "4" : s));
		float cycleAnchor1 = Float.parseFloat(((s = c.readLine(
				"cycleAnchor1 (default 1.5): ")).equals("") 
				? "1.5" : s));
		float cycleAnchor2 = Float.parseFloat(((s = c.readLine(
				"cycleAnchor2 (default -2.5): ")).equals("") 
				? "-2.5" : s));
		float cycleLength1 = Float.parseFloat(((s = c.readLine(
				"cycleLength1 (default 12.3): ")).equals("") 
				? "12.3" : s));
		float cycleLength2 = Float.parseFloat(((s = c.readLine(
				"cycleLength2 (default 23.4): ")).equals("") 
				? "23.4" : s));
		float cycleEfficiency1 = Float.parseFloat(((s = c.readLine(
				"cycleEfficiency1 (default 0.33): ")).equals("") 
				? "0.33" : s));
		float cycleEfficiency2 = Float.parseFloat(((s = c.readLine(
				"cycleEfficiency2 (default 0.75): ")).equals("") 
				? "0.75" : s));
		c.printf("Params: %s %s %s %s %s %s %s %s\n", 
				numOfRows, numOfColumns,
				cycleAnchor1, cycleAnchor2,
				cycleLength1, cycleLength2,
				cycleEfficiency1, cycleEfficiency2);
    	MatrixFolder mg = new MatrixFolder(new File(relativeDirPath));
    	generate2CycleMockLevel(
    			mg, numOfRows, numOfColumns,
				cycleAnchor1, cycleAnchor2, 
				cycleLength1, cycleLength2, 
				cycleEfficiency1, cycleEfficiency2);	
    	return mg;
	}

	public void generate2CycleMockLevel(MatrixFolder mg, 
			int numOfRows, int numOfColumns, 
			float cycleAnchor1, float cycleAnchor2, 
			float cycleLength1, float cycleLength2, 
			float cycleEfficiency1, float cycleEfficiency2) {
		final int NUM_OF_DEBUG_ATTRIBS = 7;
		float[][] floatVals = new float[numOfRows][numOfColumns];
		float[][] floatDebugVals = new float[NUM_OF_DEBUG_ATTRIBS
		                                * numOfRows][numOfColumns];
		AdvisingMatrix amf = new AdvisingMatrix(mg, 0);
		AdvisingMatrix amfd = new AdvisingMatrix(mg, 0);
		for (int r = 0; r < numOfRows; r++) {
			float[][] g2cmr = generate2CycleMockRow(numOfColumns, 
					cycleAnchor1, cycleAnchor2, cycleLength1, cycleLength2, 
					cycleEfficiency1, cycleEfficiency2, NUM_OF_DEBUG_ATTRIBS);
			floatVals[r] = g2cmr[0];
//			System.err.print("{L74} g2cmr[0]= << ");
//			for (float f1 : g2cmr[0]) {
//				System.err.print(" " + f1);
//			}
//			System.err.println(" >>\n");
			amf.appendRowF(SkfldOrLogShUtils.toArrayList(g2cmr[0]));
			for (int attrib = 0; attrib < 
					NUM_OF_DEBUG_ATTRIBS; attrib++) {
				floatDebugVals[r * NUM_OF_DEBUG_ATTRIBS
				          + attrib] = g2cmr[attrib];
				amfd.appendRowF(SkfldOrLogShUtils.toArrayList(g2cmr[attrib]));
			}
		}
		File file = new File(mg.dirHandle, "0_mock_float.csv");
		amf.setFile(file);
		file = new File(mg.dirHandle, "0_mock_float_debug.csv");
		amfd.setFile(file);
		AdvisingMatrix ams = amf.floatsToShorts();
		file = new File(mg.dirHandle, "0_mock.csv");
		ams.setFile(file);
		try {
			amf.saveEfficiently();
			amfd.saveEfficiently();
			ams.saveEfficiently();
		} catch (FileNotFoundException | IllegalStateException e) {
			System.err.println("MO L86");
		}

//		System.err.println("ArrSer L346 " 
//				// TODO: salvage use of older method below?
//				+ (SkfldOrLogShUtils.floatsToShorts(floatVals) == null));
//		System.err.println("ArrSer L347 " + (floatVals == null));
//		System.err.println("ArrSer L348 " + (floatDebugVals == null));
	}

	public float[][] generate2CycleMockRow(int numOfColumns, 
			float cycleAnchor1, float cycleAnchor2, 
			float cycleLength1, float cycleLength2, 
			float cycleEfficiency1, float cycleEfficiency2,
			int noda) {
		cycleAnchor1 = (float) (cycleAnchor1 * (Math.random() * 0.2 + 0.9));
		cycleAnchor2 = (float) (cycleAnchor2 * (Math.random() * 0.2 + 0.9));
		cycleLength1 = (float) (cycleLength1 * (Math.random() * 0.2 + 0.9));
		cycleLength2 = (float) (cycleLength2 * (Math.random() * 0.2 + 0.9));
		float f = (float) (5.0 * (Math.random() - 0.5));
		double positionInCycle1 = PI2 * Math.random();
		double positionInCycle2 = PI2 * Math.random();
		float[][] debugVals = new 
				float[noda][numOfColumns];
		for (int d = 0; d < numOfColumns ; d++) {
			positionInCycle1 += PI2 / cycleLength1;
			if (positionInCycle1 >= PI2) {
				positionInCycle1 %= PI2;
			}
			float target1 = (float) (cycleAnchor1 * Math.sin(positionInCycle1));
			float delta1 = (float) (cycleEfficiency1 * (target1 - f));
			f += delta1;
			positionInCycle2 += PI2 / cycleLength2;
			if (positionInCycle2 >= PI2) {
				positionInCycle2 %= PI2;
			}
			float target2 = (float) (cycleAnchor2 * Math.sin(positionInCycle2));
			float delta2 = (float) (cycleEfficiency2 * (target2 - f));
			f += delta2;
//			System.err.println(
//					String.format(floatFormat, positionInCycle1) + 
//					String.format(floatFormat, target1) + 
//					String.format(floatFormat, delta1) + 
//					String.format(floatFormat, positionInCycle2) + 
//					String.format(floatFormat, target2) + 
//					String.format(floatFormat, delta2) + 
//					String.format(floatFormat, f));
			debugVals[0][d] = f;
			debugVals[1][d] = (float) positionInCycle1;
			debugVals[2][d] = target1;
			debugVals[3][d] = delta1;
			debugVals[4][d] = (float) positionInCycle2;
			debugVals[5][d] = target2;
			debugVals[6][d] = delta2;
		}
//		prettyPrintArr1D(result);
		return debugVals;
	}

	private short[][] generateCallbackMockLevel(int numOfRows, int numOfColumns, 
			MockAlgo callbackObj) throws IOException {
		short[][] result = new short[numOfRows][numOfColumns];
		for (short row = 0; row < numOfRows; row++) {
			short[] arr = new short[numOfColumns];
			for (short col = 0; col < numOfColumns; col++) {
				short val = callbackObj.calculate(row, col);
				arr[col] = val;
			}
			result[row] = arr;
		}
		return result;
	}
	
	private short[][] generateSimpleMockLevel(int numOfRows, int numOfColumns, 
			short seed) throws IOException {
		short[][] result = new short[numOfRows][numOfColumns];
		for (short row = 0; row < numOfRows; row++) {
			short[] arr = new short[numOfColumns];
			for (short col = 0; col < numOfColumns; col++) {
				short val = (short)(seed + 10 * row + col);
//				System.err.println("row:" + row + " col:" + col + " val:" + val);
				arr[col] = val;
			}
			result[row] = arr;
		}
//		System.err.println("-->\n" + SkfldOrLogShUtils.prettyPrintArr2D(result)
//				+ "<--");
		return result;
	}

	
}
