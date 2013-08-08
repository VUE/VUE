package tufts.vue;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.awt.geom.Point2D;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Test;

public class ToolboxTest {

	@Test
	public void testCalcAnglePrimary() {
		Point2D origin = new Point2D.Double(0,0);
		
		assertEquals(Math.PI/4, Toolbox.angleBetween2PointsCoord(origin, new Point2D.Double(5,5), false), 0.00001);
		assertEquals(Math.PI*3/4, Toolbox.angleBetween2PointsCoord(origin, new Point2D.Double(-5,5), false), 0.00001);
		assertEquals(Math.PI*5/4, Toolbox.angleBetween2PointsCoord(origin, new Point2D.Double(-5,-5), false), 0.00001);
		assertEquals(Math.PI*7/4, Toolbox.angleBetween2PointsCoord(origin, new Point2D.Double(5,-5), false), 0.00001);
		assertEquals(Math.PI*3/2, Toolbox.angleBetween2PointsCoord(origin, new Point2D.Double(0,-5), false), 0.00001);
		assertEquals(Math.PI/2, Toolbox.angleBetween2PointsCoord(origin, new Point2D.Double(0,5), false), 0.00001);
		assertEquals(0,Toolbox.angleBetween2PointsCoord(origin, new Point2D.Double(5,0), false), 0.00001);
		assertEquals(Math.PI,Toolbox.angleBetween2PointsCoord(origin, new Point2D.Double(-5,0), false), 0.00001);
	}
	
	@Test
	public void testCalcAngleSecondary() {
		Point2D origin = new Point2D.Double(0,0);
		
		assertEquals(Math.PI*1/6,Toolbox.angleBetween2PointsCoord(origin, new Point2D.Double(Math.sqrt(3.0)/2,0.5), false), 0.00001);
		assertEquals(Math.PI*2/6,Toolbox.angleBetween2PointsCoord(origin, new Point2D.Double(0.5, Math.sqrt(3)/2), false), 0.00001);
		assertEquals(Math.PI*4/6,Toolbox.angleBetween2PointsCoord(origin, new Point2D.Double(-0.5,Math.sqrt(3)/2), false), 0.00001);
		assertEquals(Math.PI*5/6,Toolbox.angleBetween2PointsCoord(origin, new Point2D.Double(-Math.sqrt(3)/2,0.5), false), 0.00001);
		assertEquals(Math.PI*7/6,Toolbox.angleBetween2PointsCoord(origin, new Point2D.Double(-Math.sqrt(3)/2,-0.5), false), 0.00001);
		assertEquals(Math.PI*8/6,Toolbox.angleBetween2PointsCoord(origin, new Point2D.Double(-0.5,-Math.sqrt(3)/2), false), 0.00001);
		assertEquals(Math.PI*10/6,Toolbox.angleBetween2PointsCoord(origin, new Point2D.Double(0.5,-Math.sqrt(3)/2), false), 0.00001);
		assertEquals(Math.PI*11/6,Toolbox.angleBetween2PointsCoord(origin, new Point2D.Double(Math.sqrt(3)/2,-0.5), false), 0.00001);
	}
	
	@Test
	public void testMinAlphaDifference() {
		Set<Double> s = new TreeSet<Double>();
		
		s.add(0.0);
		s.add(Math.PI/2);
		s.add(Math.PI);
		s.add(3*Math.PI/2);
		
		assertEquals(0.0, Toolbox.minAlphaDifference(s, 0.0), 0.0001);
		assertEquals(Math.PI/2, Toolbox.minAlphaDifference(s, Math.PI/2), 0.0001);
		assertEquals(Math.PI, Toolbox.minAlphaDifference(s, Math.PI), 0.0001);
		assertEquals(Math.PI/2*3, Toolbox.minAlphaDifference(s, Math.PI/2*3), 0.0001);
		assertEquals(0.0, Toolbox.minAlphaDifference(s, Math.PI*2), 0.0001);
		
		assertEquals(0, Toolbox.minAlphaDifference(s, Math.PI/5), 0.0001);
		assertEquals(0, Toolbox.minAlphaDifference(s, Math.PI/5*9), 0.0001);
		
		assertEquals(Math.PI/2, Toolbox.minAlphaDifference(s, Math.PI/5*2), 0.0001);
		assertEquals(Math.PI/2, Toolbox.minAlphaDifference(s, Math.PI/5*3), 0.0001);
		
		assertEquals(Math.PI, Toolbox.minAlphaDifference(s, Math.PI/5*4), 0.0001);
		assertEquals(Math.PI, Toolbox.minAlphaDifference(s, Math.PI/5*6), 0.0001);
		
		assertEquals(Math.PI/2*3, Toolbox.minAlphaDifference(s, Math.PI/5*7), 0.0001);
		assertEquals(Math.PI/2*3, Toolbox.minAlphaDifference(s, Math.PI/5*8), 0.0001);
	}
	
	@Test
	public void testCompareAngles(){
		
		assertEquals(0.0, Toolbox.angleDifference(0,  0), 0.0001);
		assertEquals(Math.PI/2, Toolbox.angleDifference(0,  Math.PI/2), 0.0001);
		assertEquals(Math.PI, Toolbox.angleDifference(0,  Math.PI), 0.0001);
		assertEquals(Math.PI/2, Toolbox.angleDifference(0,  3*Math.PI/2), 0.0001);
		assertEquals(0, Toolbox.angleDifference(0,  2*Math.PI), 0.0001);
		
		assertEquals(Math.PI/2, Toolbox.angleDifference(Math.PI/2,  0), 0.0001);
		assertEquals(0, Toolbox.angleDifference(Math.PI/2,  Math.PI/2), 0.0001);
		assertEquals(Math.PI/2, Toolbox.angleDifference(Math.PI/2,  Math.PI), 0.0001);
		assertEquals(Math.PI, Toolbox.angleDifference(Math.PI/2,  3*Math.PI/2), 0.0001);
		assertEquals(Math.PI/2, Toolbox.angleDifference(Math.PI/2,  2*Math.PI), 0.0001);
		
		assertEquals(Math.PI, Toolbox.angleDifference(Math.PI,  0), 0.0001);
		assertEquals(Math.PI/2, Toolbox.angleDifference(Math.PI,  Math.PI/2), 0.0001);
		assertEquals(0, Toolbox.angleDifference(Math.PI,  Math.PI), 0.0001);
		assertEquals(Math.PI/2, Toolbox.angleDifference(Math.PI,  3*Math.PI/2), 0.0001);
		assertEquals(Math.PI, Toolbox.angleDifference(Math.PI,  2*Math.PI), 0.0001);

		assertEquals(Math.PI/2, Toolbox.angleDifference(3*Math.PI/2,  0), 0.0001);
		assertEquals(Math.PI, Toolbox.angleDifference(3*Math.PI/2,  Math.PI/2), 0.0001);
		assertEquals(Math.PI/2, Toolbox.angleDifference(3*Math.PI/2,  Math.PI), 0.0001);
		assertEquals(0, Toolbox.angleDifference(3*Math.PI/2,  3*Math.PI/2), 0.0001);
		assertEquals(Math.PI/2, Toolbox.angleDifference(3*Math.PI/2,  2*Math.PI), 0.0001);
	}
	


}
