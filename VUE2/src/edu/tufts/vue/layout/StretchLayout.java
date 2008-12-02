/*
 * StretchLayout.java
 *
 * Created on November 24, 2008, 12:21 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

/**
 *
 * @author akumar03
 */


package edu.tufts.vue.layout;

import java.io.*;
import java.net.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import tufts.vue.*;
import edu.tufts.vue.dataset.*;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Point2D;
import java.awt.geom.Line2D;


public class StretchLayout extends Layout {
    
    /** Creates a new instance of StretchLayout */
    public StretchLayout() {
    }
    
    public void layout(LWSelection selection) {
        // compute the bounding rectangle of selection
        int X_PADDING = 40; // applied only on top and left
        int Y_PADDING = 20;
        int MAX_X_MOVE =80;
        int MAX_Y_MOVE = 40;
        double MAX_SHIFT = 1.2;
        double SHIFT_RANGE  = 1.5;
        double EXPAND = 1.25; // expands the ellipse by a factor of 1.1
        Rectangle2D boundingRect = selection.getBounds();
        double maxX = boundingRect.getMaxX();
        double maxY = boundingRect.getMaxY();
        double minX = boundingRect.getMinX();
        double minY = boundingRect.getMinY();
        final double centerX = boundingRect.getCenterX();
        final double centerY = boundingRect.getCenterY();
        double xRange =  Math.abs(maxX-minX)/2;
        double yRange =  Math.abs(maxY-minY)/2;
        // determine the axis a & b of the ellipse that bounds the rectangle
        double angleTopLeft = Math.atan((centerY-minY)/(centerX-minX));
        double a =  xRange;
        double b = yRange;
        double ptEllipseX = centerX - a*Math.cos(angleTopLeft);
        double ptEllipseY = centerY - b*Math.sin(angleTopLeft);
        double distPt = Point2D.distance(ptEllipseX,ptEllipseY,centerX,centerY);
        double distTopLeft = Point2D.distance(minX,minY,centerX,centerY);
        double ratio = distTopLeft/distPt;
        double outerA = a*ratio*EXPAND;
        double outerB = b*ratio*EXPAND;
        
        System.out.println("ELLIPSE: a:"+a+" b: "+b+" xRrange:" + xRange+" yRange:"+yRange+" ratio:"+ratio);
        System.out.printf("center: %.2f,%.2f\n",centerX,centerY);
        Point2D topLeft =  new Point();
        topLeft.setLocation(minX,minY );
        Point2D bottomLeft =  new Point();
        bottomLeft.setLocation(minX ,maxY);
        Point2D topRight =  new Point();
        topRight.setLocation(maxX,minY);
        Point2D bottomRight =  new Point();
        bottomRight.setLocation( maxX ,maxY );
        Point2D center = new Point();
        center.setLocation(boundingRect.getCenterX(),boundingRect.getCenterY());
        Line2D slopeDown = new Line2D.Double(topLeft,bottomRight);
        Line2D slopeUp = new Line2D.Double(bottomLeft,topRight);
        Line2D top = new Line2D.Double(topLeft,topRight);
        Line2D left = new Line2D.Double(topLeft,bottomLeft);
        Line2D right = new Line2D.Double(topRight,bottomRight);
        Line2D bottom = new Line2D.Double(bottomLeft,bottomRight);
        
        
        // move all nodes around the selection
        Iterator<LWComponent> i = VUE.getActiveMap().getAllDescendents(LWContainer.ChildKind.PROPER).iterator();
        while (i.hasNext()) {
            LWComponent c = i.next();
            if(!selection.contains(c) && c instanceof LWNode) {
                // if node is within the selection move it to the bounding box
                
                Point2D location = c.getLocation();
                double newX = location.getX();
                double newY = location.getY();
                double y = location.getY();
                double x =location.getX();
                double angle = Math.atan2(centerY-y,x-centerX);
                
                if(boundingRect.contains(location)) {
                    newX = centerX-X_PADDING+outerA*Math.cos(angle);
                    newY = centerY-Y_PADDING-outerB*Math.sin(angle);
//                    System.out.println("Setting new location for: "+c.getLabel()+" angle:"+Math.toDegrees(angle));
//                      System.out.printf("x:%3.2f,y:%3.2f,newX:%3.2f,newY:%3.2f\n",x,y,newX,newY);
                } else {
                    double xInnerEllipse = centerX+a*Math.cos(angle);
                    double yInnerEllipse = centerY-b*Math.sin(angle);
                    double xOuterEllipse = centerX+outerA*Math.cos(angle);
                    double yOuterEllipse = centerY-outerB*Math.sin(angle);
                    
                    double distInner = Point2D.distance(centerX,centerY,xInnerEllipse,yInnerEllipse);
                    double distOuter = Point2D.distance(centerX,centerY,xOuterEllipse,yOuterEllipse);
                    double distPoint = Point2D.distance(centerX,centerY,x,y);
                    if(distPoint < SHIFT_RANGE*distOuter) {
                        double factor = MAX_SHIFT+ (distPoint-distInner)*(SHIFT_RANGE-MAX_SHIFT)/(SHIFT_RANGE*distOuter-distInner);
                        double newA = outerA*factor;
                        double newB = outerB*factor;
                        newX = centerX-X_PADDING+newA*Math.cos(angle);
                        newY = centerY-Y_PADDING-newB*Math.sin(angle);
                        
                    }
                    
                    /*
                    double distance = Point2D.distance(x,y,centerX,centerY);
                    double xInnerEllipse = centerX+a*Math.cos(angle);
                    double yInnerEllipse = centerY+b*Math.sin(angle);
                    double distEllipse = Point2D.distance(x,y,xInnerEllipse,yInnerEllipse);
                    double distPoint = Point2D.distance(xInnerEllipse,yInnerEllipse,centerX,centerY);
                    System.out.printf("distPoint:%.2f,distEllipse:%.2f,%s\n",distPoint,distEllipse,c.getLabel());
                    if(distEllipse<SHIFT_RANGE_PCT*distPoint) {
                        double factor =1+ (SHIFT_RANGE_PCT -distEllipse/distPoint)*MAX_SHIFT_PCT/SHIFT_RANGE_PCT;
                        newX = centerX+outerA*factor*Math.cos(angle);
                       newY = centerY+outerB*factor*Math.sin(angle);
                       System.out.println("Shifting node: "+c.getLabel());
                       System.out.printf("factor: %3.2f, x:%3.2f,y:%3.2f,newX:%3.2f,newY:%3.2f ",factor,x,y,newX,newY);
                    }
                     */
                }
                c.setLocation(newX,newY);
            }
        }
        
    }
    
    public   LWMap createMap(Dataset ds,String mapName) throws Exception {
        LWMap map = new LWMap(mapName);
        return map;
    }
}
