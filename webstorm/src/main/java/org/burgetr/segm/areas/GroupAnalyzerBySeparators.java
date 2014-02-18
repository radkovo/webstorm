/**
 * GroupAnalyzerBySeparators.java
 *
 * Created on 23.1.2007, 14:29:39 by burgetr
 */
package org.burgetr.segm.areas;

import org.burgetr.segm.Area;
import org.burgetr.segm.AreaNode;
import org.burgetr.segm.Config;
import org.burgetr.segm.Rectangular;
import org.burgetr.segm.Separator;
import org.burgetr.segm.SeparatorSet;

import java.util.*;

/**
 * Provides the visual area detection by separators only (VIPS-like approach).
 * 
 * @author burgetr
 */
public class GroupAnalyzerBySeparators extends GroupAnalyzer
{
    private SeparatorSet seps;

    public GroupAnalyzerBySeparators(AreaNode parent)
    {
        super(parent);
    }

    @Override
    public AreaNode findSuperArea(AreaNode sub, Vector<AreaNode> selected)
    {
        //parent.createSeparators();
        seps = parent.getSeparators();

        //determine the minimal separator weight
        int minWeight = 0;
        boolean prefHoriz = false;
        Separator msep = seps.getMostImportantSeparator();
        if (msep != null)
        {
            minWeight = msep.getWeight() - Config.SEPARATOR_WEIGHT_THRESHOLD;
            prefHoriz = msep.isHorizontal();
        }
        //System.out.println("mW: " + minWeight + " prefHoriz:" + prefHoriz);
        
        Vector<AreaNode> group = findChildrenGroup(sub, minWeight, prefHoriz, !prefHoriz);
        /*if (group.size() == parent.getChildCount())
            group = findChildrenGroup(sub, minWeight, true, false);*/
        
        //find all the sub-areas contained in the area found
        Rectangular bounds = new Rectangular(sub.getArea().getBounds());
        selected.removeAllElements();
        for (AreaNode chld : group)
        {
            selected.add(chld);
            bounds.expandToEnclose(chld.getArea().getBounds());
        }
        
        return new AreaNode(new Area(bounds));
    }

    private Vector<AreaNode> findChildrenGroup(AreaNode sub, int minWeight, boolean useHorizontal, boolean useVertical)
    {
        Vector<AreaNode> ret = new Vector<AreaNode>();
        
        //find the area limited by the nearest separators
        Separator[] sp = nearestSeparators(sub, minWeight, useHorizontal, useVertical);
        int ax1 = (sp[3] == null) ? parent.getX() : sp[3].getX2()+1;
        int ax2 = (sp[1] == null) ? parent.getX2() : sp[1].getX1()-1;
        int ay1 = (sp[0] == null) ? parent.getY() : sp[0].getY2()+1;
        int ay2 = (sp[2] == null) ? parent.getY2() : sp[2].getY1()-1;
        Rectangular region = new Rectangular(ax1, ay1, ax2, ay2);
        
        //find all the sub-areas contained in the area found
        for (int i = 0; i < parent.getChildCount(); i++)
        {
            AreaNode chld = parent.getChildArea(i);
            if (region.encloses(chld.getArea().getBounds()))
                ret.add(chld);
        }
        
        return ret;
        
    }
    
    /**
     * Finds the nearest separators from the specified sub area in all
     * four directions
     * @param sub the sub area where the search starts
     * @param minWeight the minimal weight of the separator to be considered
     * @param useHorizontal consider the horizontal separators
     * @param useVertical consider the vertical separators
     * @return the array of the nearest separators that contains the
     * nearest separator on the top, left, bottom, right or null when 
     * the area bounds have been reached
     */
    private Separator[] nearestSeparators(AreaNode sub, int minWeight, boolean useHorizontal, boolean useVertical)
    {
        Separator[] ret = new Separator[4];
        int min[] = new int[4];
        for (int i = 0; i < 4; i++)
        {
            ret[i] = null;
            min[i] = Integer.MAX_VALUE;
        }
        if (useHorizontal)
        {
            for (Iterator<Separator> it = seps.getHorizontal().iterator(); it.hasNext(); )
            {
                Separator sep = it.next();
                if (sep.getWeight() >= minWeight)
                {
                    int dif = sub.getY() - sep.getY2(); 
                    if (dif >= 0 && min[0] > dif)
                    {
                        min[0] = dif;
                        ret[0] = sep;
                    }
                    dif = sep.getY1() - sub.getY2();
                    if (dif >= 0 && min[2] > dif)
                    {
                        min[2] = dif;
                        ret[2] = sep;
                    }
                }
            }
        }
        if (useVertical)
        {
            for (Iterator<Separator> it = seps.getVertical().iterator(); it.hasNext(); )
            {
                Separator sep = it.next();
                if (sep.getWeight() >= minWeight)
                {
                    int dif = sub.getX() - sep.getX2(); 
                    if (dif >= 0 && min[3] > dif)
                    {
                        min[3] = dif;
                        ret[3] = sep;
                    }
                    dif = sep.getX1() - sub.getX2();
                    if (dif >= 0 && min[1] > dif)
                    {
                        min[1] = dif;
                        ret[1] = sep;
                    }
                }
            }
        }
        return ret;
    }
    
}
