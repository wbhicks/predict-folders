package met;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import met.ArrSerializer;
import met.ArrSerializer.MockAlgo;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.Rule;

import util.SkfldOrLogShUtils;

public class ArrSerializerTest {
	private ArrSerializer arrSer;
	private MockObservation mo;
	
	@Rule
    public TemporaryFolder tmpDir = new TemporaryFolder();
	private String tmpDirPath = "";
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
		arrSer = new ArrSerializer();
		mo = new MockObservation();
		File f = tmpDir.getRoot();
		// typically something like /tmp/junit5118499105463717648
		tmpDirPath = f.getCanonicalPath();
	}

	@After
	public void tearDown() throws Exception {
	}
/*
	@Test
	public void testCallbackMockLevelRoundtrip() {
		MockAlgo ma = new MockAlgo() {
			public short calculate(short r, short c) {
				short result = -99;
				if (r == 1) {
					if (c == 1) {
						result = -42;
					} else {
						result = 7;
					}
				} else {
					if (c == 1) {
						result = 6;
					} else {
						result = -1;
					}
				}
				return result;
			}
		};
		for (int numOfRows = 1; numOfRows < 4; numOfRows++) {
			for (int numOfColumns = 1; numOfColumns < 4; numOfColumns++) {
				String filename = tmpDirPath + "/" + numOfRows + "_" 
						+ numOfColumns + ".csv";
				try {
					short[][] arrWrittenOut = arrSer.generateCallbackMockLevel(
							numOfRows, numOfColumns, ma);
					AdvisingMatrix.writeLevel(arrWrittenOut, filename);
					short[][] arrReadIn = new AdvisingMatrix().readLevel(
							filename, Expect.SHORT);
					if (arrReadIn.length > 1 && arrReadIn[0].length > 1) {
						assertEquals(arrReadIn[1][1], -42);
					}
//					for (int i = 1; i < arrReadIn.length; i++) {
//						for (int j = 1; j < arrReadIn[0].length; j++) {
//							assertEquals(1234, arrReadIn[i][j]);
//						}
//					}
					assertTrue(Arrays.deepEquals(arrWrittenOut, arrReadIn));
//					System.out.println("numOfRows=" + numOfRows 
//							+ ", numOfColumns=" + numOfColumns + "\n"
//							+ arrSer.prettyPrintArr2D(arrWrittenOut)
//							+ "\n   ... should match ...\n"
//							+ arrSer.prettyPrintArr2D(arrReadIn));
					arrReadIn[numOfRows - 1][numOfColumns - 1] = -99;
					assertFalse(Arrays.deepEquals(arrWrittenOut, arrReadIn));
				} catch (IOException e) {
					fail("Should not have gotten any exception");
				}
			}
		}
	}

	@Test
	public void testSimpleMockLevelRoundtrip() {
		for (int numOfRows = 1; numOfRows < 4; numOfRows++) {
			for (int numOfColumns = 1; numOfColumns < 4; numOfColumns++) {
				String filename = tmpDirPath + "/" + numOfRows + "_" 
						+ numOfColumns + ".csv";
				try {
					short[][] arrWrittenOut = arrSer.generateSimpleMockLevel(
							numOfRows, numOfColumns, 
							(short)(1000 * numOfRows + 100 * numOfColumns));
					AdvisingMatrix.writeLevel(arrWrittenOut, filename);
					short[][] arrReadIn = new AdvisingMatrix().readLevel(
							filename, Expect.SHORT);
					for (int i = 1; i < arrReadIn.length; i++) {
						for (int j = 1; j < arrReadIn[0].length; j++) {
							assertEquals((short)(
									1000 * arrReadIn.length + 
									100 * arrReadIn[0].length + 
									10 * i + 
									j), 
									arrReadIn[i][j]);
						}
					}
					assertTrue(Arrays.deepEquals(arrWrittenOut, arrReadIn));
//					System.out.println("numOfRows=" + numOfRows 
//							+ ", numOfColumns=" + numOfColumns + "\n"
//							+ arrSer.prettyPrintArr2D(arrWrittenOut)
//							+ "\n   ... should match ...\n"
//							+ arrSer.prettyPrintArr2D(arrReadIn));
					arrReadIn[numOfRows - 1][numOfColumns - 1] = -99;
					assertFalse(Arrays.deepEquals(arrWrittenOut, arrReadIn));
				} catch (IOException e) {
					fail("Should not have gotten any exception");
				}
			}
		}
	}

	@Test
	public void testLevelRoundtrip() {
		String filename = tmpDirPath + "/testLevelRoundtrip.someextension";
		try {
			AdvisingMatrix.writeLevel(new short[][] {{-7, 8, -9}, {17, -18, 19}}, 
					filename);
			short[][] result = new AdvisingMatrix().readLevel(
					filename, Expect.SHORT);
			assertEquals(2, result.length);
			assertEquals(3, result[0].length);
			assertEquals(3, result[1].length);
			assertEquals( -7, result[0][0]);
			assertEquals(  8, result[0][1]);
			assertEquals( -9, result[0][2]);
			assertEquals( 17, result[1][0]);
			assertEquals(-18, result[1][1]);
			assertEquals( 19, result[1][2]);
		} catch (IOException e) {
			fail("Should not have gotten any exception");
		}
	}
*/
	
//	@Test
//	public void testGenerate2CycleMockLevel() {
//		float cycleAnchor1 = 1.1F;
//		float cycleAnchor2 = -1.7F;
//		float cycleLength1 = 11.3F;
//		float cycleLength2 = 17.2F;
//		float cycleEfficiency1 = 0.7F;
//		float cycleEfficiency2 = 0.1F;
//		for (int i = 1; i < 4; i++) {
//			for (int j = 1; j < 4; j++) {
//				short[][] result = SkfldOrLogShUtils.floatsToShorts(
//						arrSer.generate2CycleMockLevel(i, j,
//						cycleAnchor1, cycleAnchor2, cycleLength1, cycleLength2, 
//						cycleEfficiency1, cycleEfficiency2).floatMatrix);
//				assertEquals(i, result.length);
//				assertEquals(j, result[0].length);
//			}
//		}
//	}

	@Test
	public void testGenerate2CycleMockRow() {
		int numOfColumns = 23;
		float cycleAnchor1 = 1.1F;
		float cycleAnchor2 = -1.7F;
		float cycleLength1 = 11.3F;
		float cycleLength2 = 17.2F;
		float cycleEfficiency1 = 0.7F;
		float cycleEfficiency2 = 0.1F;
		float[][] result = mo.generate2CycleMockRow(numOfColumns, 
				cycleAnchor1, cycleAnchor2, cycleLength1, cycleLength2, 
				cycleEfficiency1, cycleEfficiency2, 7);
		assertEquals(numOfColumns, result[0].length);
	}

	/**
	 * TODO move to right class
	 * 
	 * The method under test uses Math.round() in the Java 7 API, which rounds 
	 * to the right in the case of a tie.
	 * 
	 * Both F and f indicate a float literal; there is no difference btwn them.
	 */
	@Test
	public void testFloatsToShorts() {
		final int ARR_LENGTH = 48;
		float[] floats = { 
			-300.0F, -100.0F, -10.0F, -1.0F, -0.1F, -0.01F, -0.001F, 0.0F, 
			0.001F, 0.01F, 0.1F, 1.0F, 10.0F, 100.0F, 300.0F,
			
			-1.23444F, -1.23445F, -1.23446F, -1.23456F, -1.23499F, -1.23501F, 
			1.23444F, 1.23445F, 1.23446F, 1.23456F, 1.23499F, 1.23501F,

		/* The input value 2.235F is omitted because (at least in Java SE
		 * 1.7.0_65-b33) multiplying 2.235F by 100 evaluates to 223.49998
		 * 
		 * See also various bug reports such as 
		 * https://bugs.openjdk.java.net/browse/JDK-8029896
		 */
			
			-0.235F, 0.235F, -1.235F, 1.235F, -2.235F, /* 2.235F, */ -3.235F, 
			3.235F, 
			
			0.005f, 0.015f, 0.025f, 0.035f, 0.045f, 0.055f, 0.065f,
			-0.005f, -0.015f, -0.025f, -0.035f, -0.045f, -0.055f, -0.065f			
			};
		assertEquals(ARR_LENGTH, floats.length);
		short[] expectedShorts = { 
			-30000, -10000, -1000, -100, -10, -1, 0, 0, 0, 1, 10, 100, 1000, 
			10000, 30000,

			-123, -123, -123, -123, -123, -124, 123, 123, 123, 123, 123, 124,
			
			-23, 24, -123, 124, -223, /* 224, */ -323, 324,
			
			1, 2, 3, 4, 5, 6, 7, 0, -1, -2, -3, -4, -5, -6
		};
		assertEquals(ARR_LENGTH, expectedShorts.length);
		short[] resultShorts = SkfldOrLogShUtils.floatsToShorts(floats);
		assertEquals(ARR_LENGTH, resultShorts.length);
		for (int i = 0; i < ARR_LENGTH; i++) {
			assertEquals(expectedShorts[i], resultShorts[i]);
		}
		float[] zeroLengthArr = new float[0];
		assertEquals(0, SkfldOrLogShUtils.floatsToShorts(zeroLengthArr).length);
	}

}
