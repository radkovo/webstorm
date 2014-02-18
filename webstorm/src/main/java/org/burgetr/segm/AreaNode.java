/**
 * VisualAreaNode.java
 *
 * Created on 28.6.2006, 15:13:48 by burgetr
 */
package org.burgetr.segm;

import java.awt.Color;
import java.awt.Graphics;
import java.util.*;

import org.burgetr.segm.areas.GroupAnalyzer;
import org.burgetr.segm.tagging.Tag;
import org.fit.cssbox.layout.BrowserCanvas;

/**
 * A node in the area tree. The nested areas are laid out in a grid.
 * 
 * @author burgetr
 */
public class AreaNode extends TreeNode<Area>
{
    private static final long serialVersionUID = 3931378515695196845L;

    /** Estimated layout type */
    private LayoutType layoutType;
    
    /** Position in the grid */
    private Rectangular gp;
    
    /** A grid of inserted elements */
    private AreaGrid grid;
    
    /** Set of separators */
    private SeparatorSet seps;
    
    /** Analyzer for finding the super areas */
    private GroupAnalyzer groups;
    
    /** Explicitely separated area */
    private boolean separated;
    
    /** Node importance computed from headings */
    private double importance = 0;
    
    /** Area markedness computed from multiple features */
    private double markedness = 0;
    
    /** True if the markedness should not be recomputed anymore (it's fixed) */
    private boolean fixedMarkedness = false;
    
    /** True if the node should not be reordered inside */
    private boolean atomic = false;
    
    /** Previous box on the same line */
    private AreaNode previousOnLine = null;
    
    /** Next box on the same line */
    private AreaNode nextOnLine = null;
    
    /** Assigned tags */
    private Set<Tag> tags;
    
    /** The level of the most probable assigned tag (-1 means not computed yet) */
    private int taglevel = -1;
    

    //====================================================================================

    /**
     * Creates an area node from an area.
     * @param area The area to be contained in this node
     */
    public AreaNode(Area area)
    {
        super(area);
        layoutType = LayoutType.NORMAL;
        grid = null;
        gp = new Rectangular();
        tags = new HashSet<Tag>();
    }

    public String toString()
    {
    	if (getArea() != null)
    	{
    		String ret = gp.toString();
    		/*if (grid != null)
    			ret += " {" + grid.getWidth() + "x" + grid.getHeight() + "}";
            if (backgroundSeparated())
                ret += "B";*/
    		//ret += "{" + getAverageImportance() + "/" + getTotalImportance() + "}";
    		//ret += "{" + getArea().getExpressiveness() + "} ";
    		return ret + getArea().toString();
    	}
    	else
    		return "(area)";
    }

    //====================================================================================
    
    public LayoutType getLayoutType()
    {
        return layoutType;
    }

    public void setLayoutType(LayoutType layoutType)
    {
        this.layoutType = layoutType;
    }

    /**
     * Creates the grid of areas from the child areas.
     */
    public void createGrid()
    {
        grid = new AreaGrid(this);
    }
    
    /**
     * @return The grid of areas
     */
    public AreaGrid getGrid()
    {
        return grid;
    }
    
    /**
     * Creates a set of the horizontal and vertical separators
     */
    public void createSeparators()
    {
    	seps = Config.createSeparators(this);
    }
    
    /**
     * @return the set of separators in this area
     */
    public SeparatorSet getSeparators()
    {
    	return seps;
    }
    
    /**
     * When set to true, the area is considered to be separated from other
     * areas explicitely, i.e. independently on its real borders or background.
     * This is usually used for some new superareas.
     * @return <code>true</code>, if the area is explicitely separated
     */
    public boolean explicitelySeparated()
    {
        return separated;
    }

    /**
     * When set to true, the area is considered to be separated from other
     * areas explicitely, i.e. independently on its real borders or background.
     * This is usually used for some new superareas.
     * @param separated <code>true</code>, if the area should be explicitely separated
     */
    public void setSeparated(boolean separated)
    {
        this.separated = separated;
    }

    /**
     * Obtains the overall style of the area.
     * @return the area style
     */
    public AreaStyle getStyle()
    {
        return new AreaStyle(this);
    }
    
    /**
     * @return the importance
     */
    public double getImportance()
    {
        return importance;
    }

    /**
     * @param importance the importance to set
     */
    public void setImportance(double importance)
    {
        this.importance = importance;
    }

    /**
     * @return the markedness
     */
    public double getMarkedness()
    {
        return markedness;
    }

    /**
     * @param markedness the markedness to set
     */
    public void setMarkedness(double markedness)
    {
        this.markedness = markedness;
    }

    public void setFixedMarkedness(double markedness)
    {
        this.markedness = markedness;
        this.fixedMarkedness = true;
    }
    
    public boolean hasFixedMarkedness()
    {
        return fixedMarkedness;
    }
    
    /**
     * Computes the sum of the importance of all the descendant areas
     */
    public double getTotalImportance()
    {
        double ret = importance;
        for (int i = 0; i < getChildCount(); i++)
            ret += getChildArea(i).getTotalImportance();
        return ret;
    }
    
    /**
     * Computes the average importance of all the descendant areas
     */
    public double getAverageImportance()
    {
        double ret = 0;
        int cnt = 0;
        for (int i = 0; i < getChildCount(); i++)
        {
            double imp = getChildArea(i).getAverageImportance();
            if (imp > 0)
            {
                ret += imp;
                cnt++;
            }
        }
        if (cnt > 0) ret = ret / cnt;
        return importance + ret;
    }
    
    /**
     * @return true, if the node is atomic and it should not be reordered
     */
    public boolean isAtomic()
    {
        return atomic;
    }

    /**
     * @param atomic when set to true, the node is marked as atomic and it won't be reordered
     */
    public void setAtomic(boolean atomic)
    {
        this.atomic = atomic;
    }

    /** Obtains all the text from the area and its child areas */
    public String getText()
    {
        String ret = "";
        if (isLeaf())
            ret = getArea().getBoxText();
        else
            for (int i = 0; i < getChildCount(); i++)
                ret += getChildArea(i).getText();
        return ret;
    }

    /**
     * Obtains the first box nested in this area or any subarea
     * @return the box node or <code>null</code> if there are no nested boxes
     */
    public BoxNode getFirstNestedBox()
    {
        if (isLeaf())
        {
            Vector<BoxNode> boxes = getArea().getBoxes();
            if (!boxes.isEmpty())
                return boxes.firstElement();
            else
                return null;
        }
        else
            return getChildArea(0).getFirstNestedBox();
    }
    
    /** Computes the efficient background color by considering the parents if necessary */
    public Color getEffectiveBackgroundColor()
    {
        if (getArea().getBackgroundColor() != null)
            return getArea().getBackgroundColor();
        else
        {
            if (getParentArea() != null)
                return getParentArea().getEffectiveBackgroundColor();
            else
                return Color.WHITE; //use white as the default root color
        }
    }

    public AreaNode getPreviousOnLine()
	{
		return previousOnLine;
	}

	public void setPreviousOnLine(AreaNode previousOnLine)
	{
		this.previousOnLine = previousOnLine;
	}

	public AreaNode getNextOnLine()
	{
		return nextOnLine;
	}

	public void setNextOnLine(AreaNode nextOnLine)
	{
		this.nextOnLine = nextOnLine;
	}

	/**
     * Adds a tag to this area.
     * @param tag the tag to be added.
     */
    public void addTag(Tag tag)
    {
        tags.add(tag);
    }
    
    /**
     * Tests whether the area has this tag.
     * @param tag the tag to be tested.
     * @return <code>true</code> if the area has this tag
     */
    public boolean hasTag(Tag tag)
    {
        return tags.contains(tag);
    }
    
    public void removeAllTags(Collection<Tag> c)
    {
        tags.removeAll(c);
    }
    
    /**
     * Tests whether the area or any of its direct child areas have the given tag.
     * @param tag the tag to be tested.
     * @return <code>true</code> if the area or its direct child areas have the given tag
     */
    public boolean containsTag(Tag tag)
    {
        if (hasTag(tag))
            return true;
        else
            return false;
        /*{
            for (int i = 0; i < getChildCount(); i++)
                if (getChildArea(i).hasTag(tag))
                    return true;
            return false;
        }*/
    }
    
    /**
     * Obtains the set of tags assigned to the area.
     * @return a set of tags
     */
    public Set<Tag> getTags()
    {
        return tags;
    }
    
    /**
     * Obtains the level of the most probable tag assigned to the area. This value is computed from outsied,
     * usually by the AreaTree from the tag predictor and search tree.
     * @return
     */
    public int getTagLevel()
    {
        return taglevel;
    }

    /**
     * Sets the level of the most probable tag assigned to the area.
     * @param taglevel
     */
    public void setTagLevel(int taglevel)
    {
        this.taglevel = taglevel;
    }

    /**
     * Looks for the most important leaf child (with the greatest markedness) of this area which is tagged with the given tag.
     * @param tag The required tag.
     * @return The most important leaf child area with that tag or <code>null</code> if there are no children with this tag.
     */
    public AreaNode getMostImportantLeaf(Tag tag)
    {
        Enumeration<?> e = depthFirstEnumeration();
        AreaNode best = null;
        double bestMarkedness = -1;
        while (e.hasMoreElements())
        {
            AreaNode node = (AreaNode) e.nextElement();
            if (node.isLeaf() && node.hasTag(tag) && node.getMarkedness() > bestMarkedness)
            {
                bestMarkedness = node.getMarkedness();
                best = node;
            }
        }
        return best;
    }
    
    //====================================================================================
    
    /**
     * Join this area with another area and update the layout in the grid to the given values.
     * @param other The area to be joined to this area
     * @param pos The position of the result in the grid
     * @param horizontal Horizontal or vertical join?
     */
	@SuppressWarnings({ "rawtypes", "unchecked" })
    public void joinArea(AreaNode other, Rectangular pos, boolean horizontal)
    {
    	gp = pos;
    	if (other.children != null)
    	{
	    	Vector adopt = new Vector(other.children);
	    	for (Iterator it = adopt.iterator(); it.hasNext();)
	    		add((AreaNode) it.next());
    	}
    	getArea().join(other.getArea(), horizontal);
    	tags.addAll(other.getTags());
    }
    
    //====================================================================================
    
    /**
     * Creates a new subarea from a specified region of the area and moves the selected child
     * nodes to the new area.
     * @param gp the subarea bounds
     * @param selected nodes to be moved to the new area
     * @param name the name (identification) of the new area
     * @return the new AreaNode created in the tree or null, if nothing was created
     */ 
    public AreaNode createSuperArea(Rectangular gp, Vector<AreaNode> selected, String name)
    {
        if (getChildCount() > 1 && selected.size() > 1 && selected.size() != getChildCount())
        {
            //create the new area
	        Area area = new Area(getArea().getX1() + grid.getColOfs(gp.getX1()),
	                             getArea().getY1() + grid.getRowOfs(gp.getY1()),
	                             getArea().getX1() + grid.getColOfs(gp.getX2()+1) - 1,
	                             getArea().getY1() + grid.getRowOfs(gp.getY2()+1) - 1);
	        area.setName(name);
        	AreaNode grp = new AreaNode(area);
        	int index = getIndex(selected.firstElement());
            insert(grp, index);
        	grp.addAll(selected);
            grp.createGrid();
            createGrid();
            return grp;
        }
        else
            return null;
    }
    
    @SuppressWarnings("unused")
    public void debugAreas(AreaNode sub)
    {
        if (Config.DEBUG_AREAS && sub.getChildCount() == 0)
        {
            if (groups == null)
                groups = Config.createGroupAnalyzer(this);
            Vector<AreaNode> inside = new Vector<AreaNode>();
            createSeparators();
            groups.findSuperArea(sub, inside);
        }
        //joinAreas();
    }
    
    //====================================================================================
    
    public void collapseSubtree()
    {
        //System.out.println("Collapsing: " + toString());
        recursiveCollapseSubtree(this);
        removeAllChildren();
        //System.out.println("Result: " + getText());
    }
    
    private void recursiveCollapseSubtree(AreaNode dest)
    {
        for (int i = 0; i < getChildCount(); i++)
        {
            AreaNode child = getChildArea(i);
            child.recursiveCollapseSubtree(dest);
            dest.getArea().joinChild(child.getArea());
        }
    }
    
    //====================================================================================
    
    /**
     * Removes simple separators from current separator set. A simple separator
     * has only one or zero visual areas at each side
     */
    public void removeSimpleSeparators()
    {
    	/*Vector<Separator> hs = seps.getHorizontal();
    	if (hs.size() > 0)
    	{
    		Separator sep = hs.firstElement();
    		System.out.println("filter: " + sep);
    		System.out.println("GX=" + grid.findCellX(sep.getX1()+1));
    		System.out.println("GY=" + grid.findCellY(sep.getY1()+1));
    		System.out.println("A=" + countAreasAbove(sep));
    	}*/
    	removeSimpleSeparators(seps.getHorizontal());
    	removeSimpleSeparators(seps.getVertical());
    	removeSimpleSeparators(seps.getBoxsep());
    }
    
    /**
     * Removes simple separators from a vector of separators. A simple separator
     * has only one or zero visual areas at each side
     */
    private void removeSimpleSeparators(Vector<Separator> v)
    {
        //System.out.println("Rem: this="+this);
    	for (Iterator<Separator> it = v.iterator(); it.hasNext();)
		{
			Separator sep = it.next();
			if (sep.getType() == Separator.HORIZONTAL || sep.getType() == Separator.BOXH)
			{
				int a = countAreasAbove(sep);
				int b = countAreasBelow(sep);
				if (a <= 1 && b <= 1)
					it.remove();
			}
			else
			{
				int a = countAreasLeft(sep);
				int b = countAreasRight(sep);
				if (a <= 1 && b <= 1)
					it.remove();
			}
		}
    }
    
    /**
     * @return the number of the areas directly above the separator
     */
    private int countAreasAbove(Separator sep)
    {
    	int gx1 = grid.findCellX(sep.getX1());
    	int gx2 = grid.findCellX(sep.getX2());
    	int gy = grid.findCellY(sep.getY1() - 1);
    	int ret = 0;
    	if (gx1 >= 0 && gx2 >= 0 && gy >= 0)
    	{
    		int i = gx1;
    		while (i <= gx2)
    		{
    			AreaNode node = grid.getNodeAt(i, gy);
    			//System.out.println("Search: " + i + ":" + gy + " = " + node);
    			if (node != null)
    			{
    				ret++;
    				i += node.getGridWidth();
    			}
    			else
    				i++;
    		}
    	}
    	return ret;
    }
    
    /**
     * @return the number of the areas directly below the separator
     */
    private int countAreasBelow(Separator sep)
    {
    	int gx1 = grid.findCellX(sep.getX1());
    	int gx2 = grid.findCellX(sep.getX2());
    	int gy = grid.findCellY(sep.getY2() + 1);
    	int ret = 0;
    	if (gx1 >= 0 && gx2 >= 0 && gy >= 0)
    	{
    		int i = gx1;
    		while (i <= gx2)
    		{
    			AreaNode node = grid.getNodeAt(i, gy);
    			//System.out.println("Search: " + i + ":" + gy + " = " + node);
    			if (node != null)
    			{
    				ret++;
    				i += node.getGridWidth();
    			}
    			else
    				i++;
    		}
    	}
    	return ret;
    }
    
    /**
     * @return the number of the areas directly on the left of the separator
     */
    private int countAreasLeft(Separator sep)
    {
    	int gy1 = grid.findCellY(sep.getY1());
    	int gy2 = grid.findCellY(sep.getY2());
    	int gx = grid.findCellX(sep.getX1() - 1);
    	int ret = 0;
    	if (gy1 >= 0 && gy2 >= 0 && gx >= 0)
    	{
    		int i = gy1;
    		while (i <= gy2)
    		{
    			AreaNode node = grid.getNodeAt(gx, i);
    			if (node != null)
    			{
    				ret++;
    				i += node.getGridWidth();
    			}
    			else
    				i++;
    		}
    	}
    	return ret;
    }
    
    /**
     * @return the number of the areas directly on the left of the separator
     */
    private int countAreasRight(Separator sep)
    {
    	int gy1 = grid.findCellY(sep.getY1());
    	int gy2 = grid.findCellY(sep.getY2());
    	int gx = grid.findCellX(sep.getX2() + 1);
    	int ret = 0;
    	if (gy1 >= 0 && gy2 >= 0 && gx >= 0)
    	{
    		int i = gy1;
    		while (i <= gy2)
    		{
    			AreaNode node = grid.getNodeAt(gx, i);
    			if (node != null)
    			{
    				ret++;
    				i += node.getGridWidth();
    			}
    			else
    				i++;
    		}
    	}
    	return ret;
    }
    
    /**
     * Looks for the nearest text box area placed above the separator. If there are more
     * such areas in the same distance, the leftmost one is returned.
     * @param sep the separator 
     * @return the leaf area containing the box or <code>null</code> if there is nothing above the separator
     */
    public AreaNode findContentAbove(Separator sep)
    {
        return recursiveFindAreaAbove(sep.getX1(), sep.getX2(), 0, sep.getY1());
    }
    
    private AreaNode recursiveFindAreaAbove(int x1, int x2, int y1, int y2)
    {
        AreaNode ret = null;
        int maxx = x2;
        int miny = y1;
        Vector <BoxNode> boxes = getArea().getBoxes();
        for (BoxNode box : boxes)
        {
            int bx = box.getBounds().x1; 
            int by = box.getBounds().y2; 
            if ((bx >= x1 && bx <= x2 && by < y2) &&  //is placed above
                    (by > miny ||
                     (by == miny && bx < maxx)))
            {
                ret = this; //found in our boxes
                if (bx < maxx) maxx = bx;
                if (by > miny) miny = by;
            }
        }

        for (int i = 0; i < getChildCount(); i++)
        {
            AreaNode child = getChildArea(i);
            AreaNode area = child.recursiveFindAreaAbove(x1, x2, miny, y2);
            if (area != null)
            {   
                int bx = area.getX(); 
                int by = area.getY2();
                int len = area.getText().length();
                if ((len > 0) && //we require some text in the area
                        (by > miny ||
                         (by == miny && bx < maxx)))
                {
                    ret = area;
                    if (bx < maxx) maxx = bx;
                    if (by > miny) miny = by;
                }
            }
        }
        
        return ret;
    }

    //====================================================================================
    
    /**
     * @return Returns the height of the area in the grid height in rows
     */
    public int getGridHeight()
    {
        return gp.getHeight();
    }

    /**
     * @return Returns the width of the area in the grid in rows
     */
    public int getGridWidth()
    {
        return gp.getWidth();
    }

    /**
     * @return Returns the gridX.
     */
    public int getGridX()
    {
        return gp.getX1();
    }

    /**
     * @param gridX The gridX to set.
     */
    public void setGridX(int gridX)
    {
        gp.setX1(gridX);
    }

    /**
     * @return Returns the gridY.
     */
    public int getGridY()
    {
        return gp.getY1();
    }

    /**
     * @param gridY The gridY to set.
     */
    public void setGridY(int gridY)
    {
        gp.setY1(gridY);
    }
    
    /**
     * @return the position of this area in the grid of its parent area
     */
    public Rectangular getGridPosition()
    {
    	return gp;
    }
    
    /**
     * Sets the position in the parent area grid for this area
     * @param pos the position
     */
    public void setGridPosition(Rectangular pos)
    {
        gp = new Rectangular(pos);
    }
    
    //====================================================================================
    
    public int getX()
    {
        return getArea().getX1();
    }
    
    public int getY()
    {
        return getArea().getY1();
    }
    
    public int getX2()
    {
        return getArea().getX2();
    }
    
    public int getY2()
    {
        return getArea().getY2();
    }
    
    public int getWidth()
    {
        return getArea().getWidth();
    }
    
    public int getHeight()
    {
        return getArea().getHeight();
    }
    
    //====================================================================================

    /**
     * Adds a new area as a subarea. Updates the area bounds if necessary.
     * @param area the area node to be added
     */
    public void addArea(AreaNode sub)
    {
    	add(sub);
    	getArea().getBounds().expandToEnclose(sub.getArea().getBounds());
    	getArea().updateAverages(sub.getArea());
    }
    
    /**
     * Adds all the areas in the vector as the subareas. It the areas
     * have already had another parent, they are removed from the old
     * parent.
     * @param areas the collection of subareas
     */
    public void addAll(Collection<AreaNode> areas)
    {
    	for (Iterator<AreaNode> it = areas.iterator(); it.hasNext(); )
    		addArea(it.next());
    }
    
    /**
     * @return the contained area
     */
    public Area getArea()
    {
        return getUserObject();
    }
    
    /**
     * @return the parent area node of this area in the tree
     */
    public AreaNode getParentArea()
    {
        return (AreaNode) getParent();
    }

    /**
     * Returns a specified child area node.
     * @param index the child index
     * @return the specified child box
     */
    public AreaNode getChildArea(int index)
    {
        return (AreaNode) getChildAt(index);
    }

    /**
     * @return a vector of the child areas 
     */
    @SuppressWarnings("unchecked")
    public Vector<AreaNode> getChildAreas()
    {
        return this.children;
    }
    
    /**
     * Returns the child area at the specified grid position or null, if there is no
     * child area at this position.
     */
    public AreaNode getChildAtGridPos(int x, int y)
    {
        for (int i = 0; i < getChildCount(); i++)
        {
            AreaNode child = getChildArea(i);
            if (child.getGridPosition().contains(x, y))
                return child;
        }
        return null;
    }
    
    /**
     * Returns the child areas whose absolute coordinates intersect with the specified rectangle.
     */
    public Vector<AreaNode> getChildNodesInside(Rectangular r)
    {
        Vector<AreaNode> ret = new Vector<AreaNode>();
        for (int i = 0; i < getChildCount(); i++)
        {
            AreaNode child = getChildArea(i);
            if (child.getArea().getBounds().intersects(r))
                ret.add(child);
        }
        return ret;
    }
    
    /**
     * Check if there are some children in the given subarea of the area.
     */
    public boolean isAreaEmpty(Rectangular r)
    {
        for (int i = 0; i < getChildCount(); i++)
        {
            AreaNode child = getChildArea(i);
            if (child.getArea().getBounds().intersects(r))
                return false;
        }
        return true;
    }
    
    /**
     * Computes the total square area occupied by the area contents.
     * @return the total square area of the contents in pixels
     */
    public int getContentSquareArea()
    {
        int ret = 0;
        for (int i = 0; i < getChildCount(); i++)
            ret += getChildArea(i).getArea().getSquareArea();
        return ret;
    }
    
    /**
     * Computes the percentage of the contents of the parent area occupied by this area.
     * @return the percentage of this area in the parent area's contents (0..1)
     */
    public double getParentPercentage()
    {
        AreaNode parent = getParentArea();
        if (parent != null)
            return (double) getArea().getSquareArea() / parent.getContentSquareArea();
        else
            return 0;
    }
    
    /**
     * Checks whether all the child nodes have a coherent style.
     * @return <code>true</code> if none of the children has much different style from the average.
     */
    public boolean isCoherent()
    {
        double afs = getArea().getAverageFontSize();
        double aw = getArea().getAverageFontWeight();
        double as = getArea().getAverageFontStyle();
        for (int i = 0; i < getChildCount(); i++)
        {
            Area child = getChildArea(i).getArea();
            double fsdif = Math.abs(child.getAverageFontSize() - afs) * 2; //allow 1/2 difference only (counting with averages)
            double wdif = Math.abs(child.getAverageFontWeight() - aw) * 2;
            double sdif = Math.abs(child.getAverageFontStyle() - as) * 2;
            if (fsdif > Config.FONT_SIZE_THRESHOLD 
                || wdif > Config.FONT_WEIGHT_THRESHOLD
                || sdif > Config.FONT_STYLE_THRESHOLD)
                return false;
            //TODO: doplnit barvy, viz hasSameStyle
        }
        return true;
    }
    
    /**
     * Compares two areas and decides whether they have the same style. The thresholds of the style are taken from the {@link Config}.
     * @param other the other area to be compared
     * @return <code>true</code> if the areas are considered to have the same style
     */
    public boolean hasSameStyle(AreaNode other)
    {
        return getStyle().isSameStyle(other.getStyle());
    }
    
    /**
     * @return <code>true</code> if the area is visually separated from its parent by
     * a different background color
     */
    public boolean isBackgroundSeparated()
    {
        return getArea().isBackgroundSeparated();
    }

    /**
     * @return <code>true<code> if the area is separated from the areas below it
     */
    public boolean separatedDown()
    {
        return getArea().hasBottomBorder() || isBackgroundSeparated();
    }
    
    /**
     * @return <code>true<code> if the area is separated from the areas above it
     */
    public boolean separatedUp()
    {
        return getArea().hasTopBorder() || isBackgroundSeparated();
    }
    
    /**
     * @return <code>true<code> if the area is separated from the areas on the left
     */
    public boolean separatedLeft()
    {
        return getArea().hasLeftBorder() || isBackgroundSeparated();
    }
    
    /**
     * @return <code>true<code> if the area is separated from the areas on the right
     */
    public boolean separatedRight()
    {
        return getArea().hasRightBorder() || isBackgroundSeparated();
    }

    /**
     * Checks whether the area is horizontally centered within its parent area
     * @return <code>true</code> if the area is centered
     */
    public boolean isCentered()
    {
        return isCentered(true, true) == 1;
    }
    
    /**
     * Tries to guess whether the area is horizontally centered within its parent area 
     * @param askBefore may we compare the alignment with the preceding siblings?
     * @param askAfter may we compare the alignment with the following siblings?
     * @return 0 when certailny not centered, 1 when certainly centered, 2 when not sure (nothing to compare with and no margins around)
     */
    private int isCentered(boolean askBefore, boolean askAfter)
    {
        AreaNode parent = getParentArea();
        if (parent != null)
        {
            int left = getX() - parent.getX();
            int right = parent.getX2() - getX2();
            int limit = (int) (((left + right) / 2.0) * Config.CENTERING_THRESHOLD);
            if (limit == 0) limit = 1; //we always allow +-1px
            //System.out.println(this + " left=" + left + " right=" + right + " limit=" + limit);
            boolean middle = Math.abs(left - right) <= limit; //first guess - check if it is placed in the middle
            boolean fullwidth = left == 0 && right == 0; //centered because of full width
            
            if (!middle && !fullwidth) //not full width and certainly not in the middle
            {
                return 0; 
            }
            else //may be centered - check the alignment
            {
                //compare the alignent with the previous and/or the next child
                AreaNode prev = null;
                AreaNode next = null;
                int pc = 2; //previous centered?
                int nc = 2; //next cenrered?
                if (askBefore || askAfter)
                {
                    if (askBefore)
                    {
                        prev = (AreaNode) getPreviousSibling();
                        while (prev != null && (pc = prev.isCentered(true, false)) == 2)
                            prev = (AreaNode) prev.getPreviousSibling();
                    }
                    if (askAfter)
                    {
                        next = (AreaNode) getNextSibling();
                        while (next != null && (nc = next.isCentered(false, true)) == 2)
                            next = (AreaNode) next.getNextSibling();
                    }
                }
                
                if (pc != 2 || nc != 2) //we have something for comparison
                {
                    if (fullwidth) //cannot guess, compare with others
                    {
                        if (pc != 0 && nc != 0) //something around is centered - probably centered
                            return 1;
                        else
                            return 0;
                    }
                    else //probably centered, if it is not left- or right-aligned with something around
                    {
                        if (prev != null && lrAligned(this, prev) == 1 ||
                            next != null && lrAligned(this, next) == 1)
                            return 0; //aligned, not centered
                        else
                            return 1; //probably centered
                    }
                }
                else //nothing to compare, just guess
                {
                    if (fullwidth)
                        return 2; //cannot guess from anything
                    else
                        return (middle ? 1 : 0); //nothing to compare with - guess from the position
                }
            }
        }
        else
            return 2; //no parent - we don't know
    }
    
    /**
     * Checks if the areas are left- or right-aligned.
     * @return 0 if not, 1 if yes, 2 if both left and right
     */
    private int lrAligned(AreaNode a1, AreaNode a2)
    {
        if (a1.getX() == a2.getX())
            return (a1.getX2() == a2.getX2()) ? 2 : 1;
        else if (a1.getX2() == a2.getX2())
            return 1;
        else
            return 0;
    }
    
    //======================================================================================
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
	public void sortChildren(Comparator<AreaNode> comparator)
    {
    	for (Enumeration e = children(); e.hasMoreElements(); )
    	{
    		AreaNode chld = (AreaNode) e.nextElement();
    		if (!chld.isAtomic())
    		    chld.sortChildren(comparator);
    	}
    	if (children != null)
    		java.util.Collections.sort(children, comparator);
    }
    
    //======================================================================================
    
    public void drawGrid(BrowserCanvas canvas)
    {
        Graphics ig = canvas.getImageGraphics();
        Color c = ig.getColor();
        ig.setColor(Color.BLUE);
        int xo = getArea().getX1();
        for (int i = 1; i <= grid.getWidth(); i++)
        {
            xo += grid.getCols()[i-1];
            /*System.out.println(i + " : " + xo);
            if (i == 42) ig.setColor(Color.GREEN);
            else if (i == 47) ig.setColor(Color.RED);
            else ig.setColor(Color.BLUE);*/
            ig.drawLine(xo, getArea().getY1(), xo, getArea().getY2());
        }
        int yo = getArea().getY1();
        for (int i = 0; i < grid.getHeight(); i++)
        {
            yo += grid.getRows()[i];
            ig.drawLine(getArea().getX1(), yo, getArea().getX2(), yo);
        }
        ig.setColor(c);
    }

    //====================================================================================
    
    public enum LayoutType 
    { 
        /** Normal flow */
        NORMAL("normal"),
        /** Tabular layout */
        TABLE("table"),
        /** A simple list */
        LIST("list");
        
        private String name;
        
        private LayoutType(String name)
        {
            this.name = name;
        }
        
        public String toString()
        {
            return name;
        }
    } 
    
}
