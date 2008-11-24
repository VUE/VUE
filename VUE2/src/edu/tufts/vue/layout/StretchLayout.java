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
        int X_PADDING = 80; // applied only on top and left
        int Y_PADDING = 40;
        int MAX_X_MOVE =80;
        int MAX_Y_MOVE = 40;
        Rectangle2D boundingRect = selection.getBounds();
        double maxX = boundingRect.getMaxX();
        double maxY = boundingRect.getMaxY();
        double minX = boundingRect.getMinX();
        double minY = boundingRect.getMinY();
        double xRange =  Math.abs(maxX-minX);
        double yRange =  Math.abs(maxY-minY);
        
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
                
                if(boundingRect.contains(location)) {
                    if(slopeDown.relativeCCW(location)>0) {
                        if(slopeUp.relativeCCW(location)>0)  {
                            newY = minY -Y_PADDING;
                        } else {
                            newX = maxX;
                        }
                    } else {
                        if(slopeUp.relativeCCW(location)>0) {
                            newX = minX-X_PADDING;
                        } else{
                            newY = maxY;
                        }
                    }
                    
                    System.out.println("Setting new location for: "+c.getLabel());
                } else {
                    double y = location.getY();
                    double x =location.getX();
                    double topDistance = Math.abs(y-minY);
                    double bottomDistance = Math.abs(y-maxY);
                    double leftDistance = Math.abs(x-minX);
                    double rightDistance = Math.abs(x-maxX);
                    if(topDistance<bottomDistance && topDistance< yRange && x> minX && x < maxX) {
                        double dY = (yRange - topDistance)*MAX_Y_MOVE/yRange;
                        newY  -= dY;
                        System.out.println("Moving top: "+c.getLabel()+"y= "+y+" newY "+newY+" dY"+dY);
                    } 
                    if (bottomDistance<topDistance && bottomDistance<yRange && x>minX &&  x<maxX) {
                        double dY = (yRange - bottomDistance)*MAX_Y_MOVE/yRange;
                        newY += dY;
                        System.out.println("Moving bottom: "+c.getLabel()+"y= "+y+" newY "+newY+" dY"+dY);  
                    } 
                    if(leftDistance<rightDistance&& leftDistance<xRange && x<minX  ){
                        double dX = (xRange-leftDistance)* MAX_X_MOVE/xRange;
                        newX -= dX;
                    } 
                    if(rightDistance<leftDistance && rightDistance<xRange && y>minY && y<maxY) {
                        double dX = (xRange-rightDistance)* MAX_X_MOVE/xRange;
                        newX += dX;
                    }
                    
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
