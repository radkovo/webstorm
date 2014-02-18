/**
 * AreaComparator.java
 *
 * Created on 3.1.2007, 18:51:02 by radek
 */
package org.burgetr.segm.areas;

import java.util.Comparator;

import org.burgetr.segm.Area;
import org.burgetr.segm.AreaNode;

/**
 * This comparator compares two AreaNodes. The contained areas are ordered
 * from the top-left corner to the bottom-right corner.
 * 
 * @author radek
 */
public class AreaPositionComparator implements Comparator<AreaNode>
{

	public int compare(AreaNode o1, AreaNode o2)
	{
		Area a1 = o1.getArea();
		Area a2 = o2.getArea();
		
		if (a1.getY2() <= a2.getY1()) //a1 is fully above a2
		    return -1;
		else if (a1.getY1() >= a2.getY2()) //a1 is fully below a2
		    return 1;
		else  //vertical overlap
		{
		    if (a1.getX2() <= a2.getX1())
		        return -1;
		    else if (a1.getX1() >= a2.getX2())
		        return 1;
		    else
		        return 0;
		}
		
	}

}
