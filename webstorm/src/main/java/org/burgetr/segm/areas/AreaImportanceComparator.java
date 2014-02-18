/**
 * AreaImportanceComparator.java
 *
 * Created on 6.8.2008, 13:59:10 by burgetr
 */
package org.burgetr.segm.areas;

import java.util.Comparator;

import org.burgetr.segm.AreaNode;

/**
 * This comparator comares two AreaNodes. The contained areas are ordered
 * according to their total importance (more important first).
 * 
 * @author burgetr
 */
public class AreaImportanceComparator implements Comparator<AreaNode>
{
    
    public int compare(AreaNode o1, AreaNode o2)
    {
        //double i1 = o1.getTotalImportance();
        //double i2 = o2.getTotalImportance();
        double i1 = o1.getAverageImportance();
        double i2 = o2.getAverageImportance();
        
        if (i1 < i2)
            return 1;
        else if (i1 > i2)
            return -1;
        else
            return 0;
    }

}
