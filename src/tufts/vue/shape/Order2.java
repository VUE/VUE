/*
* Copyright 2003-2010 Tufts University  Licensed under the
 * Educational Community License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 * 
 * http://www.osedu.org/licenses/ECL-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package tufts.vue.shape;

import java.awt.geom.Rectangle2D;
import java.awt.geom.PathIterator;
import java.awt.geom.QuadCurve2D;
import java.util.Vector;

final class Order2 extends Curve {
    private double x0;
    private double y0;
    private double cx0;
    private double cy0;
    private double x1;
    private double y1;
    private double xmin;
    private double xmax;

    private double xcoeff0;
    private double xcoeff1;
    private double xcoeff2;
    private double ycoeff0;
    private double ycoeff1;
    private double ycoeff2;

    public static void insert(Vector curves, double tmp[],
			      double x0, double y0,
			      double cx0, double cy0,
			      double x1, double y1,
			      int direction)
    {
	int numparams = getHorizontalParams(y0, cy0, y1, tmp);
	if (numparams == 0) {
	    curves.add(new Order2(x0, y0, cx0, cy0, x1, y1, direction));
	    return;
	}
	// assert(numparams == 1);
	double t = tmp[0];
	tmp[0] = x0;  tmp[1] = y0;
	tmp[2] = cx0; tmp[3] = cy0;
	tmp[4] = x1;  tmp[5] = y1;
	split(tmp, 0, t);
	Order2 c1 = getInstance(x0, y0,
				tmp[2], tmp[3],
				tmp[4], tmp[5],
				direction);
	Order2 c2 = getInstance(tmp[4], tmp[5],
				tmp[6], tmp[7],
				x1, y1,
				direction);
	if (direction == INCREASING) {
	    curves.add(c1);
	    curves.add(c2);
	} else {
	    curves.add(c2);
	    curves.add(c1);
	}
    }

    public static Order2 getInstance(double x0, double y0,
				     double cx0, double cy0,
				     double x1, double y1,
				     int direction) {
	if (y0 > y1) {
	    return new Order2(x1, y1, cx0, cy0, x0, y0, -direction);
	} else {
	    return new Order2(x0, y0, cx0, cy0, x1, y1, direction);
	}
    }

    /*
     * Fill an array with the coefficients of the parametric equation
     * in t, ready for solving with solveQuadratic.
     * We currently have:
     *     Py(t) = C0*(1-t)^2 + 2*CP*t*(1-t) + C1*t^2
     *           = C0 - 2*C0*t + C0*t^2 + 2*CP*t - 2*CP*t^2 + C1*t^2
     *           = C0 + (2*CP - 2*C0)*t + (C0 - 2*CP + C1)*t^2
     *     Py(t) = (C0) + (2*CP - 2*C0)*t + (C0 - 2*CP + C1)*t^2
     *     Py(t) = C + Bt + At^2
     *     C = C0
     *     B = 2*CP - 2*C0
     *     A = C0 - 2*CP + C1
     */
    public static void getEqn(double eqn[], double c0, double cp, double c1) {
	eqn[0] = c0;
	eqn[1] = cp + cp - c0 - c0;
	eqn[2] = c0 - cp - cp + c1;
    }

    /*
     * Return the count of the number of horizontal sections of the
     * specified quadratic Bezier curve.  Put the parameters for the
     * horizontal sections into the specified <code>ret</code> array.
     * <p>
     * If we examine the parametric equation in t from the getEqn method
     * and take the derivative, we get:
     *     Py(t) = At^2 + Bt + C
     *     dPy(t) = 2At + B = 0
     *     2*(C0 - 2*CP + C1)t + 2*(CP - C0) = 0
     *     2*(C0 - 2*CP + C1)t = 2*(C0 - CP)
     *     t = 2*(C0 - CP) / 2*(C0 - 2*CP + C1)
     *     t = (C0 - CP) / (C0 - CP + C1 - CP)
     * Note that this method will return 0 if the equation is a line,
     * which is either always horizontal or never horizontal.
     * Completely horizontal curves need to be eliminated by other
     * means outside of this method.
     */
    public static int getHorizontalParams(double c0, double cp, double c1,
					  double ret[]) {
	if (c0 <= cp && cp <= c1) {
	    return 0;
	}
	c0 -= cp;
	c1 -= cp;
	double denom = c0 + c1;
	// If denom == 0 then cp == (c0+c1)/2 and we have a line.
	if (denom == 0) {
	    return 0;
	}
	double t = c0 / denom;
	// No splits at t==0 and t==1
	if (t <= 0 || t >= 1) {
	    return 0;
	}
	ret[0] = t;
	return 1;
    }

    /*
     * Split the quadratic Bezier stored at coords[pos...pos+5] representing
     * the paramtric range [0..1] into two subcurves representing the
     * parametric subranges [0..t] and [t..1].  Store the results back
     * into the array at coords[pos...pos+5] and coords[pos+4...pos+9].
     */
    public static void split(double coords[], int pos, double t) {
	double x0, y0, cx, cy, x1, y1;
	coords[pos+8] = x1 = coords[pos+4];
	coords[pos+9] = y1 = coords[pos+5];
	cx = coords[pos+2];
	cy = coords[pos+3];
	x1 = cx + (x1 - cx) * t;
	y1 = cy + (y1 - cy) * t;
	x0 = coords[pos+0];
	y0 = coords[pos+1];
	x0 = x0 + (cx - x0) * t;
	y0 = y0 + (cy - y0) * t;
	cx = x0 + (x1 - x0) * t;
	cy = y0 + (y1 - y0) * t;
	coords[pos+2] = x0;
	coords[pos+3] = y0;
	coords[pos+4] = cx;
	coords[pos+5] = cy;
	coords[pos+6] = x1;
	coords[pos+7] = y1;
    }

    public Order2(double x0, double y0,
		  double cx0, double cy0,
		  double x1, double y1,
		  int direction)
    {
	super(direction);
	// REMIND: Better accuracy in the root finding methods would
	//  ensure that cy0 is in range.  As it stands, it is never
	//  more than "1 mantissa bit" out of range...
	if (cy0 < y0) {
	    cy0 = y0;
	} else if (cy0 > y1) {
	    cy0 = y1;
	}
	this.x0 = x0;
	this.y0 = y0;
	this.cx0 = cx0;
	this.cy0 = cy0;
	this.x1 = x1;
	this.y1 = y1;
	xmin = Math.min(Math.min(x0, x1), cx0);
	xmax = Math.max(Math.max(x0, x1), cx0);
	xcoeff0 = x0;
	xcoeff1 = cx0 + cx0 - x0 - x0;
	xcoeff2 = x0 - cx0 - cx0 + x1;
	ycoeff0 = y0;
	ycoeff1 = cy0 + cy0 - y0 - y0;
	ycoeff2 = y0 - cy0 - cy0 + y1;
    }

    public int getOrder() {
	return 2;
    }

    public double getXTop() {
	return x0;
    }

    public double getYTop() {
	return y0;
    }

    public double getXBot() {
	return x1;
    }

    public double getYBot() {
	return y1;
    }

    public double getXMin() {
	return xmin;
    }

    public double getXMax() {
	return xmax;
    }

    public double getX0() {
	return (direction == INCREASING) ? x0 : x1;
    }

    public double getY0() {
	return (direction == INCREASING) ? y0 : y1;
    }

    public double getCX0() {
	return cx0;
    }

    public double getCY0() {
	return cy0;
    }

    public double getX1() {
	return (direction == DECREASING) ? x0 : x1;
    }

    public double getY1() {
	return (direction == DECREASING) ? y0 : y1;
    }

    public double XforY(double y) {
	if (y == y0) {
	    return x0;
	}
	if (y == y1) {
	    return x1;
	}
	return XforT(TforY(y));
    }

    public double TforY(double y) {
	double eqn[] = new double[3];
	getEqn(eqn, y0, cy0, y1);
	eqn[0] -= y;
	int numroots = QuadCurve2D.solveQuadratic(eqn, eqn);
	return firstValidRoot(eqn, numroots);
    }

    public double XforT(double t) {
	return (xcoeff2 * t + xcoeff1) * t + xcoeff0;
    }

    public double YforT(double t) {
	return (ycoeff2 * t + ycoeff1) * t + ycoeff0;
    }

    public double dXforT(double t, int deriv) {
	switch (deriv) {
	case 0:
	    return (xcoeff2 * t + xcoeff1) * t + xcoeff0;
	case 1:
	    return 2 * xcoeff2 * t + xcoeff1;
	case 2:
	    return 2 * xcoeff2;
	default:
	    return 0;
	}
    }

    public double dYforT(double t, int deriv) {
	switch (deriv) {
	case 0:
	    return (ycoeff2 * t + ycoeff1) * t + ycoeff0;
	case 1:
	    return 2 * ycoeff2 * t + ycoeff1;
	case 2:
	    return 2 * ycoeff2;
	default:
	    return 0;
	}
    }

    public double nextVertical(double t0, double t1) {
	double t = -ycoeff1 / (2 * ycoeff2);
	if (t > t0 && t < t1) {
	    return t;
	}
	return t1;
    }

    public void enlarge(Rectangle2D r) {
	r.add(x0, y0);
	double t = -xcoeff1 / (2 * xcoeff2);
	if (t > 0 && t < 1) {
	    r.add(XforT(t), YforT(t));
	}
	r.add(x1, y1);
    }

    public Curve getSubCurve(double ystart, double yend, int dir) {
	if (ystart == y0 && yend == y1) {
	    return getWithDirection(dir);
	}
	double eqn[] = new double[10];
	double t0, t1;
	if (ystart == y0) {
	    t0 = 0;
	} else {
	    getEqn(eqn, y0, cy0, y1);
	    eqn[0] -= ystart;
	    int numroots = QuadCurve2D.solveQuadratic(eqn, eqn);
	    t0 = firstValidRoot(eqn, numroots);
	}
	if (yend == y1) {
	    t1 = 1;
	} else {
	    getEqn(eqn, y0, cy0, y1);
	    eqn[0] -= yend;
	    int numroots = QuadCurve2D.solveQuadratic(eqn, eqn);
	    t1 = firstValidRoot(eqn, numroots);
	}
	eqn[0] = x0;
	eqn[1] = y0;
	eqn[2] = cx0;
	eqn[3] = cy0;
	eqn[4] = x1;
	eqn[5] = y1;
	if (t1 < 1) {
	    split(eqn, 0, t1);
	}
	if (t0 <= 0) {
	    return new Order2(eqn[0], ystart,
			      eqn[2], eqn[3],
			      eqn[4], yend,
			      dir);
	} else {
	    split(eqn, 0, t0 / t1);
	    return new Order2(eqn[4], ystart,
			      eqn[6], eqn[7],
			      eqn[8], yend,
			      dir);
	}
    }

    public Curve getReversedCurve() {
	return new Order2(x0, y0, cx0, cy0, x1, y1, -direction);
    }

    public int getSegment(double coords[]) {
	coords[0] = cx0;
	coords[1] = cy0;
	if (direction == INCREASING) {
	    coords[2] = x1;
	    coords[3] = y1;
	} else {
	    coords[2] = x0;
	    coords[3] = y0;
	}
	return PathIterator.SEG_QUADTO;
    }

    public String controlPointString() {
	return ("("+round(cx0)+", "+round(cy0)+"), ");
    }
}
