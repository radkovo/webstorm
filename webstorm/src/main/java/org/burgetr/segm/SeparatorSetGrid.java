/**
 * 
 */
package org.burgetr.segm;

import java.util.*;

/**
 * @author radek
 * A separator set created by splitting the horizontal and vertical
 * separators independently.
 */
public class SeparatorSetGrid extends SeparatorSet
{
	/**
	 * Creates a new separator set with one horizontal and one vertical separator.
	 */
	public SeparatorSetGrid(AreaNode root)
	{
		super(root);
	}

    /**
     * Creates a new separator set with one horizontal and one vertical separator.
     */
    public SeparatorSetGrid(AreaNode root, Area filter)
    {
    	super(root, filter);
    }
    
    //=====================================================================================
    
    /**
     * Finds the horizontal and vertical list of separators
     * @param area the root area
     * @param filter ignored in this implementation
     */
    protected void findSeparators(AreaNode area, Area filter)
    {
    	//the grid is required in the area
    	if (area.getGrid() == null)
    		area.createGrid();
    	
        hsep = new Vector<Separator>();
        vsep = new Vector<Separator>();

        //find the empty columns
        Vector<Rectangular> columns = findEmptyColumns(area);
        
        //connect neighbouring separators
        joinColumns(columns);
        
        //convert columns to separators
        for (Rectangular rect : columns)
        {
        	Rectangular abspos = area.getGrid().getAreaBoundsAbsolute(rect);
            short type = abspos.getWidth() > abspos.getHeight() ? Separator.HORIZONTAL : Separator.VERTICAL;
        	Separator newsep = new Separator(type, abspos.getX1(), abspos.getY1(), abspos.getX2(), abspos.getY2());
        	if (type == Separator.HORIZONTAL)
        		hsep.add(newsep);
        	else
        		vsep.add(newsep);
        }
        
        applyRegularFilters();
    }
    
    /**
     * Finds the longest connected columns of empty cells in the area grid.
     * @param area the area to be processed
     * @return the list of rectangles representing the columns (all the rectangles
     * will be one column wide) 
     */
    private Vector<Rectangular> findEmptyColumns(AreaNode area)
    {
    	Vector<Rectangular> ret = new Vector<Rectangular>();
    	AreaGrid grid = area.getGrid();
    	
    	Rectangular current = null;
    	
    	for (int x = 0; x < grid.getWidth(); x++)
    	{
    		for (int y = 0; y < grid.getHeight(); y++)
    		{
    			if (grid.cellEmpty(x, y))
    			{
    				if (current == null)
    					current = new Rectangular(x, y, x, y);
    				else
    					current.setY2(y);
    			}
    			else
    			{
    				if (current != null)
    				{
    					ret.add(current);
    					current = null;
    				}
    			}
    		}
			if (current != null)
			{
				ret.add(current);
				current = null;
			}
    	}
    	
    	return ret;
    }
    
    private Vector<Rectangular> joinColumns(Vector<Rectangular> input)
    {
    	Vector<Rectangular> ret = input;
    	Collections.sort(ret, new XYComparator());
    	Rectangular current = null;
    	for (Iterator<Rectangular> it = ret.iterator(); it.hasNext(); )
    	{
    		Rectangular r = it.next();
    		if (current == null)
    			current = r;
    		else
    		{
    			if (r.getY1() == current.getY1() && r.getY2() == current.getY2()
    					&& r.getX1() == current.getX2()+1)
    			{
    				current.setX2(r.getX1());
    				it.remove();
    			}
    			else
    				current = r;
    		}
    	}
    	return ret;
    }
    
}

class XYComparator implements Comparator<Rectangular>
{
	public int compare(Rectangular o1, Rectangular o2) 
	{
		if (o1.getY1() == o2.getY2())
			return o1.getX1() - o2.getX1();
		else
			return o1.getY1() - o2.getY1();
	}
}