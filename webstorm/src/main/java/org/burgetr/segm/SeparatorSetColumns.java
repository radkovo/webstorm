/**
 * SeparatorSetHVS.java
 *
 * Created on 17.1.2012, 20:29:33 by burgetr
 */
package org.burgetr.segm;

import java.util.Iterator;
import java.util.Vector;

/**
 * A separator set algorithm that prefers the columns in the page.
 * 
 * @author radek
 */
public class SeparatorSetColumns extends SeparatorSet
{
    /**
     * Creates a new separator set with one horizontal and one vertical separator.
     */
    public SeparatorSetColumns(AreaNode root)
    {
        super(root);
    }

    /**
     * Creates a new separator set with one horizontal and one vertical separator.
     */
    public SeparatorSetColumns(AreaNode root, Area filter)
    {
        super(root, filter);
    }
    
    //=====================================================================================
    
    /**
     * Finds the horizontal and vertical list of separators
     * @param area the root area
     * @param filter if not null, only the sub areas enclosed in the filter area
     *  are considered
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
        
        Separator vinit = new Separator(Separator.VERTICAL,
                                        base.getX1(),
                                        base.getY1(),
                                        base.getX2(),
                                        base.getY2());
        vsep.add(vinit);
        
        if (considerSubareas(this.root, filter) > 1)
        {
            if (vsep.size() == 0) //no vertical separators - split horizontally and try again
            {
                //System.out.println("Filter: " + filter);
                Vector<Area> areas = createAreas(filter);
                //System.out.println("Start: " + areas.size() + " areas"); wait(5000);
                if (areas.size() > 1)
                {
                    for (Iterator<Area> it = areas.iterator(); it.hasNext(); )
                    {
                        Area a = it.next();
                        //System.out.println("Area: " + a);
                        //dispRect(a.getBounds(), java.awt.Color.RED); wait(100);
                        SeparatorSet aset = new SeparatorSetColumns(area, a);
                        hsep.addAll(aset.getHorizontal());
                        vsep.addAll(aset.getVertical());
                    }
                }
            }
            else
                hsep.removeAllElements();
        }
        else
        {
            hsep.removeAllElements();
            vsep.removeAllElements();
        }
        applyRegularFilters(base);
    }
    
    /**
     * Consider a new area -- updates the separators according to this new area
     * @param area The new area node to be considered
     */
    private void considerArea(AreaNode area)
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
            Separator sep = it.next();
            int sy1 = sep.getY1();
            int sy2 = sep.getY2();
            //the box covers the separator -- remove the separator 
            if (ay1 <= sy1 && ay2 >= sy2)
            {
                    it.remove();
            }
            //box entirely inside -- split the separator 
            else if (ay1 > sy1 && ay2 < sy2)
            {
                Separator newsep = new Separator(Separator.HORIZONTAL,
                                                 sep.getX1(), ay2 + 1,
                                                 sep.getX2(), sep.getY2());
                newseps.add(newsep);
                sep.setY2(ay1 - 1);
            }
            //box partially covers the separator -- update the separator
            else if ((ay1 > sy1 && ay1 <= sy2) && ay2 >= sy2)
            {
                sep.setY2(ay1 - 1);
            }
            //box partially covers the separator -- update the separator
            else if (ay1 <= sy1 && (ay2 >= sy1 && ay2 < sy2))
            {
                sep.setY1(ay2 + 1);
            }
        }
        hsep.addAll(newseps);
        
        //go through vertical separators
        newseps = new Vector<Separator>();
        for (Iterator<Separator> it = vsep.iterator(); it.hasNext();)
        {
            Separator sep = it.next();
            int sx1 = sep.getX1();
            int sx2 = sep.getX2();
            //the box covers the separator -- remove the separator 
            if (ax1 <= sx1 && ax2 >= sx2)
            {
                it.remove();
            }
            //box entirely inside -- split the separator 
            else if (ax1 > sx1 && ax2 < sx2)
            {
                Separator newsep = new Separator(Separator.VERTICAL,
                                                 ax2 + 1, sep.getY1(),
                                                 sep.getX2(), sep.getY2());
                newseps.add(newsep);
                sep.setX2(ax1 - 1);
            }
            //box partially covers the separator -- update the separator
            else if ((ax1 > sx1 && ax1 <= sx2) && ax2 >= sx2)
            {
                sep.setX2(ax1 - 1);
            }
            //box partially covers the separator -- update the separator
            else if (ax1 <= sx1 && (ax2 >= sx1 && ax2 < sx2))
            {
                sep.setX1(ax2 + 1);
            }
        }
        vsep.addAll(newseps);
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
                    //dispSeparators();
                    //System.out.println("Consider: " + sub);
                    //dispRect(sub.getArea().getBounds(), java.awt.Color.GREEN); wait(200);
                    //if (sub.toString().contains("MediaEval"))
                    //    System.out.println("jo!");
                    considerArea(sub);
                    ret++;
                }
            }
        }
        Area base = (filter == null) ? area.getArea() : filter;
        applyRegularFilters(base);
        return ret;
    }
    
    
    //=====================================================================================

    /**
     * Add a separator and split or update the areas if necessary.
     */
    private void considerSeparator(Vector<Area> areas, Separator sep, boolean horizontal)
    {
        //dispRect(sep, java.awt.Color.GREEN); wait(1000);

        Vector<Area> newareas = new Vector<Area>();
        if (horizontal) //horizontal separator
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
                else if ((sy1 > ay1 && sy1 <= ay2) && sy2 >= ay2)
                {
                    area.getBounds().setY2(sy1 - 1);
                }
                //separator partially covers the area -- update the area
                else if (sy1 <= ay1 && (sy2 >= ay1 && sy2 < ay2))
                {
                    area.getBounds().setY1(sy2 + 1);
                }
            }
        }
        else //vertical separator
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
                else if ((sx1 > ax1 && sx1 <= ax2) && sx2 >= ax2)
                {
                    area.getBounds().setX2(sx1 - 1);
                }
                //separator partially covers the area -- update the area
                else if (sx1 <= ax1 && (sx2 >= ax1 && sx2 < ax2))
                {
                    area.getBounds().setX1(sx2 + 1);
                }
            }
        }
        areas.addAll(newareas);
    }
    
    /**
     * Add a separator but do not consider its width (zero-width separator). Split or update the areas if necessary.
     * Thin separators are considered only when they span for the whole width/height of the processed area.
     */
    private void considerThinSeparator(Vector<Area> areas, Separator sep, boolean horizontal)
    {
        Vector<Area> newareas = new Vector<Area>();
        if (horizontal) //horizontal separator
        {
            int sy1 = sep.getY1();
            int sy2 = sep.getY2();
            for (Iterator<Area> it = areas.iterator(); it.hasNext();)
            {
                Area area = it.next();
                //the separator width must cover the whole area
                if (sep.getX1() <= area.getX1() && sep.getX2() >= area.getX2())
                {
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
                        Area newarea = new Area(area.getX1(), sy1,
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
                        area.getBounds().setY1(sy1);
                    }
                }
            }
        }
        else //vertical separator
        {
            int sx1 = sep.getX1();
            int sx2 = sep.getX2();
            for (Iterator<Area> it = areas.iterator(); it.hasNext();)
            {
                Area area = it.next();
                //the separator height must cover the whole area
                if (sep.getY1() <= area.getY1() && sep.getY2() >= area.getY2())
                {
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
                        Area newarea = new Area(sx1, area.getY1(),
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
                        area.getBounds().setX1(sx1);
                    }
                }
            }
        }
        areas.addAll(newareas);
    }
    
    
    /**
     * Creates new "virtual" visual areas based on detected separators. These areas are further used for detecting more separators.
     * @param an optional filtering area; only the new sub-areas within this area are considered
     * @return a vector of created visual areas
     */
    private Vector<Area> createAreas(Area filter)
    {
        Area base = (filter == null) ? root.getArea() : filter;
        
        Vector<Area> areas = new Vector<Area>();
        Area init = new Area(base.getX1(), base.getY1(), base.getX2(), base.getY2());
        areas.add(init);
        for (Separator sep : hsep)
            considerSeparator(areas, sep, true);
        for (Separator sep : vsep)
            considerSeparator(areas, sep, false);
        for (Separator sep : bsep)
            considerThinSeparator(areas, sep, sep.getType() == Separator.BOXH);
        return areas;
    }

    protected void applyRegularFilters(Area base)
    {
        joinVertical();
        
        //super.applyRegularFilters();
        
        //remove the separators that touch the borders of the base area
        int by1 = base.getY1();
        int by2 = base.getY2();
        for (Iterator<Separator> it = hsep.iterator(); it.hasNext(); )
        {
            Separator sep = it.next();
            if (sep.getY1() <= by1 || sep.getY2() >= by2)
                it.remove();
        }
        int bx1 = base.getX1();
        int bx2 = base.getX2();
        for (Iterator<Separator> it = vsep.iterator(); it.hasNext(); )
        {
            Separator sep = it.next();
            if (sep.getX1() <= bx1 || sep.getX2() >= bx2)
                it.remove();
        }
    }
    
    @Override
    protected void applyFinalFilters()
    {
        //joinVertical();
        super.applyFinalFilters();
    }

    protected void joinVertical()
    {
        if (vsep.size() > 1)
        {
            boolean change = true;
            int start = 0;
            while (change)
            {
                change = false;
                Separator cur = new Separator(vsep.elementAt(start));
                for (int i = 0; i < vsep.size(); i++)
                {
                    Separator next = vsep.elementAt(i);
                    if (i != start && canJoin(cur, next))
                    {
                        //expand current separator
                        Separator exp = expandVerticalSeparator(cur, next);
                        vsep.add(exp);
                        //split the neighbor
                        splitAndAddSeparator(cur, next, vsep);
                        //remove the old pair
                        vsep.removeElement(cur);
                        vsep.removeElement(next);
                        //start over
                        start = 0;
                        change = true;
                        break;
                    }
                }
                if (!change) //nothing can be changed
                {
                    start++; //try another start
                    if (start < vsep.size()) //unless we have tried all the starts
                        change = true; 
                }
            }
        }
    }
    
    private boolean canJoin(Separator base, Separator ext)
    {
        if ((base.getX1() >= ext.getX1() && base.getX2() <= ext.getX2())) //the width of the whole base fits
        {
            if (base.getY2() <= ext.getY1()) //base above extension
            {
                Rectangular between = new Rectangular(base.getX1(), base.getY2()+1, base.getX2(), ext.getY1()-1);
                return root.isAreaEmpty(between);
            }
            else if (base.getY1() >= ext.getY2()) //base below extension
            {
                Rectangular between = new Rectangular(base.getX1(), ext.getY2()+1, base.getX2(), base.getY1()-1);
                return root.isAreaEmpty(between);
            }
            else //overlapping, always join
                return true;
        }
        else
            return false;
    }
    
    private Separator expandVerticalSeparator(Separator base, Separator ext)
    {
        Separator ret = new Separator(base);
        if (ext.getY1() < base.getY1())
            ret.setY1(ext.getY1());
        if (ext.getY2() > base.getY2())
            ret.setY2(ext.getY2());
        return ret;
    }
    
    private void splitAndAddSeparator(Separator base, Separator ext, Vector<Separator> sep)
    {
        int wl = base.getX1() - ext.getX1();
        int wr = ext.getX2() - base.getX2();
        if (wl > 0)
        {
            Separator extl = new Separator(ext);
            extl.setX2(base.getX1() - 1);
            sep.add(extl);
        }
        if (wr > 0)
        {
            Separator extr = new Separator(ext);
            extr.setX1(base.getX2() + 1);
            sep.add(extr);
        }
    }
    
}
