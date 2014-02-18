/**
 * AreaFontComparator.java
 *
 * Created on 6.8.2008, 13:44:25 by burgetr
 */
package org.burgetr.segm.areas;

import java.util.Comparator;

import org.burgetr.segm.AreaNode;

/**
 * This comparator comares two AreaNodes. The contained areas are ordered
 * according to the average font size (the greater font first).
 * 
 * @author burgetr
 */
public class AreaFontComparator implements Comparator<AreaNode>
{
    
    public int compare(AreaNode o1, AreaNode o2)
    {
        if (o1.getArea().getAverageFontSize() < o2.getArea().getAverageFontSize())
            return 1;
        else if (o1.getArea().getAverageFontSize() > o2.getArea().getAverageFontSize())
            return -1;
        else
            return 0;
    }

}
