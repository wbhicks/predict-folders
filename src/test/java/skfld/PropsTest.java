package skfld;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.junit.Rule;

public class PropsTest {
	
	@Rule
	public ExpectedException thrown = ExpectedException.none();
	
	@Rule
    public TemporaryFolder tmpDir = new TemporaryFolder();
	private String tmpDirPath = "";
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		/* For now, we're using only tests that are not destructive of the
		 * properties files. Therefore, we don't need to re-load the properties
		 * before each unit test; instead, we can load them once here.
		 * 
		 * TODO Move most of this to setUp() once we start writing tests that
		 * will require the properties to be re-loaded.
		 * 
		 * TODO Create and use a test version of the 2nd file too
		 */
// 		Props.load("etc/test_default.properties", "etc/test_app.properties");
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {}

	@Before
	public void setUp() throws Exception {
		Props.load("etc/test_default.properties", "etc/test_app.properties");
		File f = tmpDir.getRoot();
		// typically something like /tmp/junit5118499105463717648
		tmpDirPath = f.getCanonicalPath();
	}

	@After
	public void tearDown() throws Exception {}
	
	@Test
	public void testLoadException() throws FileNotFoundException, IOException {
		thrown.expect(FileNotFoundException.class);
//		thrown.expectMessage("????");
		Props.load(tmpDirPath + "/DOES_NOT_EXIST.properties", 
				"etc/test_app.properties");
	}
	
	// don't test for now - don't risk corrupting the real custom store
//	@Test
//	public void testStore() {
//		Props.store();
//	}
	
	@Test
	public void testStoreString() {
		try {
			Props.store(tmpDirPath + "/testStoreString.properties");
		} catch (IOException e) {
			fail("Should not have gotten any exception");
		}
	}

	@Test
	public void testLoadChangeStoreRoundtrip() throws FileNotFoundException, 
			IOException {
		assertEquals("bar", Props.get("foo"));
		Props.set("foo", "No longer bar.");
		assertEquals("No longer bar.", Props.get("foo"));		
		try {
			Props.store(tmpDirPath + "/testLCSR.properties");
		} catch (IOException e) {
			fail("Should not have gotten any exception");
		}
		Props.set("foo", "Once again bar.");
		assertEquals("Once again bar.", Props.get("foo"));		
		Props.load("etc/test_default.properties", 
				tmpDirPath + "/testLCSR.properties");
		assertEquals("No longer bar.", Props.get("foo"));	
	}

	@Test
	public void testAliasesRoundtrip() {
		/* The alias mechanism makes it hard to test the following expectation
		 * directly, but 1 of the 2 test properties files loaded in setUp() 
		 * should include these 2 lines:
		 * 
		 * apple $alias banana
		 * carrot $alias date
		 * 
		 * And the other test file should include these 2 lines:
		 * 
		 * banana $alias carrot
		 * date endive
		 */
		assertEquals("endive", Props.get("apple"));
		/*
		 * Florida $alias Fla
		 * Fla $alias FL
		 * 
		 * CityOfMiami $alias Miami
		 * Miami $alias MIA
		 * 
		 * FL.MIA Michaelson
		 */
		assertEquals("Michaelson", Props.get("Florida.CityOfMiami"));
		assertEquals("Michaelson", Props.get("Fla.CityOfMiami"));
		assertEquals("Michaelson", Props.get("FL.CityOfMiami"));
		assertEquals("Michaelson", Props.get("Florida.Miami"));
		assertEquals("Michaelson", Props.get("Fla.Miami"));
		assertEquals("Michaelson", Props.get("FL.Miami"));
		assertEquals("Michaelson", Props.get("Florida.MIA"));
		assertEquals("Michaelson", Props.get("Fla.MIA"));
		assertEquals("Michaelson", Props.get("FL.MIA"));

		assertEquals("Palo Alto", Props.get("us.co.hp"));
		assertEquals("Nonesuch", Props.get("us.co.hp.ai"));
		assertEquals("Watson", Props.get("us.co.ibm.ai"));
		assertEquals("Mountain View", Props.get("us.co.sun"));
		assertEquals("Mountain View", Props.get("us.co.oracle"));
		assertEquals("Mountain View", Props.get("com.sun"));
		assertEquals("Mountain View", Props.get("com.oracle"));
		assertEquals("Sparc", Props.get("us.co.sun.hw"));
		assertEquals("Yacht", Props.get("us.co.oracle.hw"));
		assertEquals("Sparc", Props.get("com.sun.hw"));
		assertEquals("Yacht", Props.get("com.oracle.hw"));

		assertEquals("Manhattan", Props.get("us.co.nbc"));
		assertEquals("Manhattan", Props.get("us.co.rca"));
		assertEquals("Manhattan", Props.get("us.co.ge"));
		assertEquals("", Props.get("com.nbc"));
		assertEquals("Manhattan", Props.get("com.rca"));
		assertEquals("Manhattan", Props.get("com.ge"));
		assertEquals("Welch", Props.get("us.co.nbc.ceo"));
		assertEquals("Welch", Props.get("us.co.rca.ceo"));
		assertEquals("Edison", Props.get("us.co.ge.ceo"));
		assertEquals("", Props.get("com.nbc.ceo"));
		assertEquals("Welch", Props.get("com.rca.ceo"));
		assertEquals("Welch", Props.get("com.ge.ceo"));
		
		assertEquals("NewYorkCity", Props.get("Manhattan"));
		assertEquals("WWII", Props.get("projects.Manhattan"));
		assertEquals("beverage", Props.get("drinks.Manhattan"));
		assertEquals("Allen", Props.get("films.Manhattan"));
		assertEquals("Scorsese", Props.get("movies.Manhattan"));
		

	}

	@Test
	public void testGetString() {
		String shouldBeEmpty = Props.get("doesNotExist");
		assertEquals("", shouldBeEmpty);
		
		assertEquals("bar", Props.get("foo"));
		assertEquals("stock", Props.get("tall"));
		assertEquals("chance", Props.get("thin"));
		assertEquals("at heart", Props.get("old"));
		assertEquals("house", Props.get("alpha"));

		assertEquals("tres", Props.get("style.Paris"));
		assertEquals("la dolce vita", Props.get("style.Rome"));
		assertEquals("Athens", Props.get("style.ancient.Greece"));

		assertEquals("10 20 30", Props.get("style.Paris.color.fg"));
		assertEquals("110 120 130", Props.get("style.Rome.color.fg"));
		assertEquals("210 220 230", Props.get("style.ancient.Greece.color.fg"));
	}
	
	@Test
	public void testGetStringString() {
		String shouldBeNull = Props.get("doesNotExist", null);
		assertEquals(null, shouldBeNull);
		String shouldBeEmpty = Props.get("doesNotExist", "");
		assertEquals("", shouldBeEmpty);
		String shouldBeXYZ = Props.get("doesNotExist", "XYZ");
		assertEquals("XYZ", shouldBeXYZ);
		String s = Props.get("foo", "XYZ");
		assertEquals("bar", s);
	}
	
}
