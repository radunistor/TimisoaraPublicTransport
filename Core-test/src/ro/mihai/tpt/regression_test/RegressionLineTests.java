package ro.mihai.tpt.regression_test;

import java.util.LinkedHashSet;

import ro.mihai.tpt.model.City;
import ro.mihai.tpt.model.Line;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import static ro.mihai.tpt.regression_test.BlackListed.*;

/**
 * This file's sole purpose is to identify changes 
 * within the RATT web site: line/station renaming or 
 * changing IDs or adding new/removing old lines/stations.
 * @author mihai
 *
 */
public class RegressionLineTests extends TestCase {
	private City cActual = null, cExpected = null;
	private String lineName;
	
	public RegressionLineTests(String testMethod, City cActual, City cExpected, String lineName) {
		super(testMethod);
		this.cActual = cActual;
		this.cExpected = cExpected;
		this.lineName = lineName;
	}
	
	public void disabled_testLine_StationCount() {
		Line expected = cExpected.getLine(lineName); 
		Line actual = cActual.getLine(lineName);
		
		assertEquals("",
			expected.getStations().size(), 
			actual.getStations().size()
		);
	}
	
	public void testLine_Stations() {
		Line expected = cExpected.getLine(lineName); 
		Line actual = cActual.getLine(lineName);
		
		assertEquals("",
			RegressionTests.diffEntities(expected.getStations(), actual.getStations(),
					blExpectedLineStations(lineName), blActualLineStations(lineName)),"");
	}
	
	@Override
	public String getName() {
		return super.getName().replaceFirst("Line_", "_"+lineName+"_");
	}

	
	public static void addTests(TestSuite suite, City cActual, City cExpected) {
		LinkedHashSet<String> lineNames = new LinkedHashSet<String>();
		for(Line l : cExpected.getLines()) 
			if (!isIdListed(l, blExpectedLines))
				lineNames.add(l.getName());
		for(Line l : cActual.getLines()) 
			if (!isIdListed(l, blActualLines))
				lineNames.add(l.getName());
		
		for(String lineName : lineNames) {
			for(String t : RegressionTests.testMethods(RegressionLineTests.class))
				suite.addTest(new RegressionLineTests(t, cActual, cExpected, lineName));
		}
	}
}
