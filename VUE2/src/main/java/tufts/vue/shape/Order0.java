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
import java.util.Vector;

final class Order0 extends Curve {
    private double x;
    private double y;

    public Order0(double x, double y) {
	super(INCREASING);
	this.x = x;
	this.y = y;
    }

    public int getOrder() {
	return 0;
    }

    public double getXTop() {
	return x;
    }

    public double getYTop() {
	return y;
    }

    public double getXBot() {
	return x;
    }

    public double getYBot() {
	return y;
    }

    public double getXMin() {
	return x;
    }

    public double getXMax() {
	return x;
    }

    public double getX0() {
	return x;
    }

    public double getY0() {
	return y;
    }

    public double getX1() {
	return x;
    }

    public double getY1() {
	return y;
    }

    public double XforY(double y) {
	return y;
    }

    public double TforY(double y) {
	return 0;
    }

    public double XforT(double t) {
	return x;
    }

    public double YforT(double t) {
	return y;
    }

    public double dXforT(double t, int deriv) {
	return 0;
    }

    public double dYforT(double t, int deriv) {
	return 0;
    }

    public double nextVertical(double t0, double t1) {
	return t1;
    }

    public int crossingsFor(double x, double y) {
	return 0;
    }

    public boolean accumulateCrossings(Crossings c) {
	return (x > c.getXLo() &&
		x < c.getXHi() &&
		y > c.getYLo() &&
		y < c.getYHi());
    }

    public void enlarge(Rectangle2D r) {
	r.add(x, y);
    }

    public Curve getSubCurve(double ystart, double yend, int dir) {
	return this;
    }

    public Curve getReversedCurve() {
	return this;
    }

    public int getSegment(double coords[]) {
	coords[0] = x;
	coords[1] = y;
	return PathIterator.SEG_MOVETO;
    }
}
