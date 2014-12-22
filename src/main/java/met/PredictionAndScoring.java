package met;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import util.SkfldOrLogShUtils;

public class PredictionAndScoring {

	MatrixFolder mf;
	ArrayList<AdvisingMatrix> alam = new ArrayList<AdvisingMatrix>();

	public PredictionAndScoring(MatrixFolder mf) {
		this.mf = mf;
		initAlam();
	}
	
	private void initAlam() {
		AdvisingMatrix am = new AdvisingMatrix(mf, 1, "sameAsYest") {
			public short predictCell(ArrayList<Short> als, int curr) {
				short result = Short.MIN_VALUE;
				if (0 < curr && curr <= als.size()) {
					result = als.get(curr - 1);
				}
				return result;
			}			
		};
		alam.add(am);
		am = new AdvisingMatrix(mf, 2, "avg2Days") {
			public short predictCell(ArrayList<Short> als, int curr) {
				short result = Short.MIN_VALUE;
				if (1 < curr && curr <= als.size()) {
					float sum = als.get(curr - 2) + als.get(curr - 1);
					result = (short) (sum / 2.0);
				}
				return result;
			}
		};
		alam.add(am);
		am = new AdvisingMatrix(mf, 3, "extrapolation2Days") {
			public short predictCell(ArrayList<Short> als, int curr) {
				short result = Short.MIN_VALUE;
				if (1 < curr && curr <= als.size()) {
					short yest = als.get(curr - 1);
					short dayb4 = als.get(curr - 2);
					result = (short) (yest + yest - dayb4);
				}
				return result;
			}
		};
		alam.add(am);
		am = new AdvisingMatrix(mf, 4, "avg3Days") {
			public short predictCell(ArrayList<Short> als, int curr) {
				short result = Short.MIN_VALUE;
				if (2 < curr && curr <= als.size()) {
					float sum = als.get(curr - 3) + als.get(curr - 2) + als.get(curr - 1);
					result = (short) (sum / 3.0);
				}
				return result;
			}
		};
		alam.add(am);
		am = new AdvisingMatrix(mf, 5, "extrapolationOfDiff2Days") {
			public short predictCell(ArrayList<Short> als, int curr) {
				short result = Short.MIN_VALUE;
				if (2 < curr && curr <= als.size()) {
					short minus1 = als.get(curr - 1);
					short minus2 = als.get(curr - 2);
					short minus3 = als.get(curr - 3);
					int diffOfYest = minus1 - minus2;
					int diffOfDayb4 = minus2 - minus3;
					result = (short) (diffOfYest + diffOfYest - diffOfDayb4);
				}
				return result;
			}
		};
		alam.add(am);
	}
	
	public void predictAndScore() throws IOException {
		for (AdvisingMatrix am : alam) {
			am.setFile(new File(mf.dirHandle, "" + am.getAlgoCode() + "_"
				+ SkfldOrLogShUtils.toSpacelessDateString(
						System.currentTimeMillis()) + ".csv"));
			am.makeSaveableRowByRow();
		}
		AdvisingMatrix observedAMS = mf.getObservedMatrixS();
		for (ArrayList<Short> observedRow : observedAMS.getS()) {
			for (AdvisingMatrix am : alam) {
				ArrayList<Short> predictedRow = am.predictRow(observedRow);
				am.appendRowS(predictedRow);
				am.saveLastRow();
				am.refineScore(observedRow, predictedRow);
			}
		}
		for (AdvisingMatrix am : alam) {
			am.unmakeSaveableRowByRow();
			am.calcAvgInaccuracy();
			mf.addPredictedMatrix(am);
		}
	}

	public void writeSampleAllAlgoMatrix(int rowForSample) throws IOException {
		AdvisingMatrix sampler = new AdvisingMatrix(mf, 1000, "SAMPLER");
		sampler.setFile(new File(mf.dirHandle, "" + sampler.getAlgoCode() 
				+ "_SAMPLER_R" + rowForSample + "_" 
				+ SkfldOrLogShUtils.toSpacelessDateString(
						System.currentTimeMillis()) + ".csv"));
		sampler.makeSaveableRowByRow();
		ArrayList<Short> alsObserved = mf.getObservedMatrixS().getS(rowForSample);
		// spreadsheet charting is easier when top row is the nonneg ints:
		ArrayList<Short> guideRow = new ArrayList<Short>();
		for (short col = 0; col < alsObserved.size(); col++) {
			guideRow.add(col);
		}
		sampler.appendRowS(guideRow);
		sampler.saveLastRow();
		sampler.appendRowS(alsObserved);
		sampler.saveLastRow();
		for (AdvisingMatrix am : alam) {
			ArrayList<Short> als = am.getS(rowForSample);
			sampler.appendRowS(als);
			sampler.saveLastRow();
		}
		sampler.unmakeSaveableRowByRow();
		mf.addSamplerMatrix(sampler);
	}
	
}
