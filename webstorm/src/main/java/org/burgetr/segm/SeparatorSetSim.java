/**
 * 
 */
package org.burgetr.segm;

import java.util.*;

/**
 * @author radek
 * A separator set created by splitting the horizontal and vertical
 * separators simultaneously.
 */
public class SeparatorSetSim extends SeparatorSet
{
	/**
	 * Creates a new separator set with one horizontal and one vertical separator.
	 */
	public SeparatorSetSim(AreaNode root)
	{
		super(root);
	}

    /**
     * Creates a new separator set with one horizontal and one vertical separator.
     */
    public SeparatorSetSim(AreaNode root, Area filter)
    {
    	super(root, filter);
    }
    
    //=====================================================================================
    
    /**
     * Finds the horizontal and vertical list of separators
     * @param area the root area
     * @param filter if not null, only the sub areas enclosed in the filter area
     * 	are considered
     */
    protected void findSeparators(AreaNode area, Area filter)
    {
        hsep = new Vector<Separator>();
        vsep = new Vector<Separator>();
        
        Area base = (filter == null) ? area.getArea() : filter;
        Separator hinit = new Separator(Separator.HORIZONTAL,
                                        base.getX1(),
                                        base.getY1(),
                                        base.getX2(),
                                        base.getY2());
        hsep.add(hinit);
        
        /*Separator vinit = new Separator(Separator.VERTICAL,
                                        base.getX1(),
                                        base.getY1(),
                                        base.getX2(),
                                        base.getY2());
        vsep.add(vinit);*/
        
        if (considerSubareas(this.root, filter) > 1)
        {
            Vector<Area> areas = createAreas(filter);
            if (areas.size() > 1)
            {
                for (Iterator<Area> it = areas.iterator(); it.hasNext(); )
                {
                    Area a = it.next();
                    //dispRect(a.getBounds());
                    /*SeparatorSet aset = new SeparatorSetSim(area, a);
                    hsep.addAll(aset.getHorizontal());
                    vsep.addAll(aset.getVertical());*/
                }
            }
        }
        else
        {
            hsep.removeAllElements();
            vsep.removeAllElements();
        }
        
        applyRegularFilters();
    }
    
	/**
	 * Consider a new area -- updates the separators according to this new area
	 * @param area The new area node to be considered
	 */
	public void considerArea(AreaNode area)
	{
		//area coordinates
		int ax1 = area.getX();
		int ay1 = area.getY();
		int ax2 = area.getX2();
		int ay2 = area.getY2();
		
		//go through horizontal separators
		Vector<Separator> newseps = new Vector<Separator>();
		for (Iterator<Separator> it = hsep.iterator(); it.hasNext();)
		{
			boolean remove = false;
			Separator orig = it.next();
			
			//------- Vertical view --------
			Separator sep = new Separator(orig);
			sep.setType(Separator.HORIZONTAL);
			int sy1 = sep.getY1();
			int sy2 = sep.getY2();
			//the box covers the separator -- remove the separator 
			if (ay1 <= sy1 && ay2 >= sy2)
			{
				remove = true;
			}
			//box entirely inside -- split the separator 
			else if (ay1 > sy1 && ay2 < sy2)
			{
				remove = true;
				Separator newsep = new Separator(Separator.HORIZONTAL,
                                                 sep.getX1(), ay2 + 1,
												 sep.getX2(), sep.getY2());
				newseps.add(newsep);
				sep.setY2(ay1 - 1);
				newseps.add(sep);
			}
			//box partially covers the separator -- update the separator
			else if ((ay1 > sy1 && ay1 < sy2) && ay2 >= sy2)
			{
				remove = true;
				sep.setY2(ay1 - 1);
				newseps.add(sep);
			}
			//box partially covers the separator -- update the separator
			else if (ay1 <= sy1 && (ay2 > sy1 && ay2 < sy2))
			{
				remove = true;
				sep.setY1(ay2 + 1);
				newseps.add(sep);
			}

			if (remove)
			{
				orig.setY1(Math.max(orig.getY1(), ay1));
				orig.setY2(Math.min(orig.getY2(), ay2));
			}
			
			//------- Horizontal view --------
			sep = new Separator(orig);
			sep.setType(Separator.VERTICAL);
			
			int sx1 = sep.getX1();
			int sx2 = sep.getX2();
			//the box covers the separator -- remove the separator 
			if (ax1 <= sx1 && ax2 >= sx2)
			{
				remove = true;
			}
			//box entirely inside -- split the separator 
			else if (ax1 > sx1 && ax2 < sx2)
			{
				remove = true;
				Separator newsep = new Separator(Separator.VERTICAL,
                                                 ax2 + 1, sep.getY1(),
												 sep.getX2(), sep.getY2());
				newseps.add(newsep);
				sep.setX2(ax1 - 1);
				newseps.add(sep);
			}
			//box partially covers the separator -- update the separator
			else if ((ax1 > sx1 && ax1 < sx2) && ax2 >= sx2)
			{
				remove = true;
				sep.setX2(ax1 - 1);
				newseps.add(sep);
			}
			//box partially covers the separator -- update the separator
			else if (ax1 <= sx1 && (ax2 > sx1 && ax2 < sx2))
			{
				remove = true;
				sep.setX1(ax2 + 1);
				newseps.add(sep);
			}
			
			if (remove)
				it.remove();
		}
		hsep.addAll(newseps);
	}
    
    /**
     * Recursively considers all the sub areas and updates the list of
     * separators.
     * @param area the root area
     * @param filter if not null, only the sub areas enclosed in the filter area are considered
     * @return the number of processed subareas
     */
    private int considerSubareas(AreaNode area, Area filter)
    {
        int ret = 0;
        for (int i = 0; i < area.getChildCount(); i++)
        {
            AreaNode sub = area.getChildArea(i);
            if (filter == null || filter.encloses(sub.getArea()))
            {
                if (sub.getArea().isHorizontalSeparator())
                {
                }
                else if (sub.getArea().isVerticalSeparator())
                {
                }
                else
                {
                	//System.out.println("Consider: " + sub);
                    considerArea(sub);
                    //dispSeparators(); wait(1000);
                    //considerSubareas(sub, filter);
                    ret++;
                }
            }
            break;
        }
        return ret;
    }
    
    
    //=====================================================================================

    /**
     * Add a separator and split or update the areas if necessary.
     */
    private void considerSeparator(Vector<Area> areas, Separator sep, boolean horizontal)
    {
        Vector<Area> newareas = new Vector<Area>();
        if (horizontal)
        {
            int sy1 = sep.getY1();
            int sy2 = sep.getY2();
            for (Iterator<Area> it = areas.iterator(); it.hasNext();)
            {
                Area area = it.next();
                int ay1 = area.getY1();
                int ay2 = area.getY2();
                //the separator covers the area -- remove the area 
                if (sy1 <= ay1 && sy2 >= ay2)
                {
                    it.remove();
                }
                //separator entirely inside -- split the area 
                else if (sy1 > ay1 && sy2 < ay2)
                {
                    Area newarea = new Area(area.getX1(), sy2 + 1,
                                                        area.getX2(), area.getY2());
                    newareas.add(newarea);
                    area.getBounds().setY2(sy1 - 1);
                }
                //separator partially covers the area -- update the area
                else if ((sy1 > ay1 && sy1 < ay2) && sy2 >= ay2)
                {
                    area.getBounds().setY2(sy1 - 1);
                }
                //separator partially covers the area -- update the area
                else if (sy1 <= ay1 && (sy2 > ay1 && sy2 < ay2))
                {
                    area.getBounds().setY1(sy2 + 1);
                }
            }
        }
        else
        {
            int sx1 = sep.getX1();
            int sx2 = sep.getX2();
            for (Iterator<Area> it = areas.iterator(); it.hasNext();)
            {
                Area area = it.next();
                int ax1 = area.getX1();
                int ax2 = area.getX2();
                //the separator covers the area -- remove the area 
                if (sx1 <= ax1 && sx2 >= ax2)
                {
                    it.remove();
                }
                //separator entirely inside -- split the area 
                else if (sx1 > ax1 && sx2 < ax2)
                {
                    Area newarea = new Area(sx2 + 1, area.getY1(),
                                                        area.getX2(), area.getY2());
                    newareas.add(newarea);
                    area.getBounds().setX2(sx1 - 1);
                }
                //separator partially covers the area -- update the area
                else if ((sx1 > ax1 && sx1 < ax2) && sx2 >= ax2)
                {
                    area.getBounds().setX2(sx1 - 1);
                }
                //separator partially covers the area -- update the area
                else if (sx1 <= ax1 && (sx2 > ax1 && sx2 < ax2))
                {
                    area.getBounds().setX1(sx2 + 1);
                }
            }
        }
        areas.addAll(newareas);
    }
    
    
    /**
     * Creates a vector of visual areas.
     */
    private Vector<Area> createAreas(Area filter)
    {
        Area base = (filter == null) ? root.getArea() : filter;
        
        Vector<Area> areas = new Vector<Area>();
        Area init = new Area(base.getX1(), base.getY1(), base.getX2(), base.getY2());
        areas.add(init);
        for (Iterator<Separator> it = hsep.iterator(); it.hasNext();)
            considerSeparator(areas, it.next(), true);
        for (Iterator<Separator> it = vsep.iterator(); it.hasNext();)
            considerSeparator(areas, it.next(), false);
        return areas;
    }
        
}
