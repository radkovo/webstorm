/**
 * BoxNode.java
 *
 * Created on 2.6.2006, 11:39:46 by burgetr
 */
package org.burgetr.segm;

import java.util.*;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;

import org.fit.cssbox.layout.*;

/**
 * A node of a tree of visual blocks.
 * 
 * @author burgetr
 */
public class BoxNode extends TreeNode<Box>
{
    private static final long serialVersionUID = -7148914807204471399L;
    
    /** Overlapping threshold - the corners are considered to overlap if the boxes
     *  share more than OVERLAP pixels */
    private static final int OVERLAP = 2;
    
    /** Which percentage of the box area must be inside of another box in order
     * to consider it as a child box (from 0 to 1) */
    private static final double AREAP = 0.9;
    
    /** Order in the box tree. It is assumed that the box tree obtained from the
     * rendering engine is sorted so that the siblings in the bottom are before the
     * siblings in front (i.e. in the drawing order). Usually, this corresponds
     * to the order in the document code. The order value doesn't correspond
     * to the order value of the source box because the box order values correspond
     * to the box creation order and not to the drawing order. */
    protected int order;
    
    /** The total bounds of the box node. Normally, the bounds are the same
     * as the content bounds. However, the BoxNode may be extended
     * in order to enclose all the overlapping boxes */
    protected Rectangular bounds;
    
    /** The total content bounds of the node. */
    protected Rectangular content;
    
    /** Visual bounds of the node. */
    protected Rectangular visual = null;
    
    /** Is the box separated by background */
    protected boolean backgroundSeparated = false;
    
    /** Efficient background color */
    protected Color efficientBackground = null;
    
    /** Potential nearest parent node in the box tree */
    public BoxNode nearestParent = null;
    
    //===================================================================================
    
    /**
     * Creates a new node containing a box.
     * @param box the contained box
     */
    public BoxNode(Box box)
    {
        super(box);
        
        //copy the bounds from the box
        if (box != null)
        {
	        content = computeContentBounds();
	        bounds = new Rectangular(content); //later, this will be recomputed using recomputeBounds()
        }
    }

    /**
     * @return the order in the display tree
     */
    public int getOrder()
    {
        return order;
    }

    /**
     * @param order the display tree order to set
     */
    public void setOrder(int order)
    {
        this.order = order;
    }

    public boolean isRootNode()
    {
        //System.out.println(this + " => " + this.nearestParent);
        return nearestParent == null;
    }
    
    public String toString()
    {
        Box box = getBox();
        String ret = "";
        if (efficientBackground != null)
            ret += (box != null && isVisuallySeparated()) ? "+" : "-";
        ret += order + ": ";
        if (box == null)
            ret += "- empty -";
        else if (box instanceof Viewport)
            ret += box.toString();
        else if (box instanceof ElementBox)
        {
            ElementBox elem = (ElementBox) box;
            ret += elem.getElement().getTagName();
            ret += " [" + elem.getElement().getAttribute("id") + "]";
            ret += " [" + elem.getElement().getAttribute("class") + "]";
            ret += " B" + getBounds().toString();
            ret += " V" + getVisualBounds().toString();
        }
        else if (box instanceof TextBox)
        {
            ret = ((TextBox) box).getText();
            ret += " (" + box.getAbsoluteBounds().x + ","
            			+ box.getAbsoluteBounds().y + ","
            			+ (box.getAbsoluteBounds().x + box.getAbsoluteBounds().width - 1) + ","
            			+ (box.getAbsoluteBounds().y + box.getAbsoluteBounds().height - 1) + ")";
        }
        else
            ret = "?: " + box.toString();
        
        return ret;
    }
    
    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof BoxNode)
        {
            return ((BoxNode) obj).getBox() == getBox();
        }
        else
            return false;
    }
    
    
    public boolean isBackgroundSeparated()
    {
        return backgroundSeparated;
    }

    public void setBackgroundSeparated(boolean backgroundSeparated)
    {
        this.backgroundSeparated = backgroundSeparated;
    }
    
    /**
     * @return the efficient background color
     */
    public Color getEfficientBackground()
    {
        return efficientBackground;
    }

    /**
     * @param efficientBackground the efficientBackground to set
     */
    public void setEfficientBackground(Color bgcolor)
    {
        this.efficientBackground = bgcolor;
    }
    
    //===================================================================================


    /**
     * @return <code>true</code> if the box is visually separated by a border or
     * a different background color.
     */
    public boolean isVisuallySeparated()
    {
        Box box = getBox();

        //invisible boxes are not separated
        if (!isVisible()) 
            return false;
        //viewport is visually separated
        else if (box instanceof Viewport)
            return true;
        //non-empty text boxes are visually separated
        else if (box instanceof TextBox) 
        {
            if (box.isEmpty())
                return false;
            else
                return true;
        }
        //replaced boxes are visually separated
        else if (box instanceof InlineReplacedBox || box instanceof BlockReplacedBox)
        {
            return true;
        }
        //list item boxes with a bullet
        else if (box instanceof ListItemBox)
        {
            return ((ListItemBox) box).hasVisibleBullet();
        }
        //other element boxes
        else 
        {
            //check if separated by border -- at least one border needed
            if (getBorderCount() >= 1)
                return true;
            //check the background
            else if (isBackgroundSeparated())
                return true;
            return false;
        }
    }
    
    /**
     * Returns the minimal bounds of the box for enclosing all the contained boxes.
     * @return the minimal visual bounds
     */
    private Rectangular getMinimalVisualBounds()
    {
    	//return getBox().getMinimalAbsoluteBounds();
        Box box = getBox();
        if (box instanceof TextBox)
            return new Rectangular(box.getAbsoluteBounds().intersection(box.getClipBlock().getAbsoluteContentBounds()));
        else if (box != null && box.isReplaced())
            return new Rectangular(box.getMinimalAbsoluteBounds().intersection(box.getClipBlock().getAbsoluteContentBounds()));
        else
        {
        	Rectangular ret = null;
            for (int i = 0; i < getChildCount(); i++)
            {
                BoxNode subnode = getChildBox(i); 
                Box sub = subnode.getBox();
                Rectangular sb = subnode.getMinimalVisualBounds();
                if (sub.isDisplayed() && subnode.isVisible() && sb.getWidth() > 0 && sb.getHeight() > 0)
                {
	                if (ret == null)
	                	ret = new Rectangular(sb);
	                else
	                	ret.expandToEnclose(sb);
                }
            }
            //if nothing has been found return the top left corner
            if (ret == null)
            {
                Rectangle b = box.getAbsoluteBounds().intersection(box.getClipBlock().getAbsoluteContentBounds());
            	return new Rectangular(b.x, b.y, b.x, b.y);
            }
            else
            	return ret;
        }
    }
    
    /**
     * Returns the bounds of the box as they visually appear to the user.
     * @return the visual bounds
     */
    public Rectangular getVisualBounds()
    {
        if (visual == null)
            visual = computeVisualBounds();
        return visual;
    }
    
    private Rectangular computeVisualBounds()
    {
        Box box = getBox();
        Rectangular ret = null;
        
        if (box instanceof ElementBox)
        {
            ElementBox elem = (ElementBox) box;
            //one border only -- the box represents the border only
            if (getBorderCount() == 1 && !isBackgroundSeparated())
            {
            	Rectangular b = new Rectangular(elem.getAbsoluteBorderBounds().intersection(elem.getClipBlock().getAbsoluteContentBounds())); //clipped absolute bounds
            	if (hasTopBorder())
            		ret = new Rectangular(b.getX1(), b.getY1(), b.getX2(), b.getY1() + elem.getBorder().top - 1);
            	else if (hasBottomBorder())
            		ret = new Rectangular(b.getX1(), b.getY2() - elem.getBorder().bottom + 1, b.getX2(), b.getY2());
            	else if (hasLeftBorder())
            		ret = new Rectangular(b.getX1(), b.getY1(), b.getX1() + elem.getBorder().left - 1, b.getY2());
            	else if (hasRightBorder())
            		return new Rectangular(b.getX2() - elem.getBorder().right + 1, b.getY1(), b.getX2(), b.getY2());
            }
            //at least two borders or a border and background - take the border bounds
            else if (getBorderCount() >= 2 || (getBorderCount() == 1 && isBackgroundSeparated()))
            {
                ret = new Rectangular(elem.getAbsoluteBorderBounds().intersection(elem.getClipBlock().getAbsoluteContentBounds()));
            }
            //no border but the background is different from the parent
            else if (isBackgroundSeparated())
            {
                ret = new Rectangular(elem.getAbsoluteBackgroundBounds().intersection(elem.getClipBlock().getAbsoluteContentBounds()));
            }
            //no visual separators, consider the contents
            else
            {
                ret = getMinimalVisualBounds();
            }
        }
        else //not an element
            ret = getMinimalVisualBounds();
        
        return ret;
    }
    
    /**
     * Re-computes the visual bounds of the whole subtree.
     */
    public void recomputeVisualBounds()
    {
        for (int i = 0; i < getChildCount(); i++)
            getChildBox(i).recomputeVisualBounds();
        visual = computeVisualBounds();
    }
    
    /**
     * Recomputes the total bounds of the whole subtree. The bounds of each box will
     * correspond to its visual bounds. If the child boxes exceed the parent box,
     * the parent box bounds will be expanded accordingly.
     */
    public void recomputeBounds()
    {
        bounds = new Rectangular(visual);
        for (int i = 0; i < getChildCount(); i++)
        {
            BoxNode child = getChildBox(i);
            child.recomputeBounds();
            expandToEnclose(child);
        }
    }
    
    /**
     * Computes node the content bounds. They correspond to the background bounds
     * however, when a border is present, it is included in the contents. Moreover,
     * the box is clipped by its clipping box.
     */
    private Rectangular computeContentBounds()
    {
        Box box = getBox();
        Rectangular ret = null;
        
        if (box instanceof ElementBox)
        {
            ElementBox elem = (ElementBox) box;
            //at least one border - take the border bounds
            //TODO: when only one border is present, we shouldn't take the whole border box? 
            if (elem.getBorder().top > 0 || elem.getBorder().left > 0 ||
                elem.getBorder().bottom > 0 || elem.getBorder().right > 0)
            {
                ret = new Rectangular(elem.getAbsoluteBorderBounds());
            }
            //no border
            else
            {
                ret = new Rectangular(elem.getAbsoluteBackgroundBounds());
            }
        }
        else //not an element - return the whole box
            ret = new Rectangular(box.getAbsoluteBounds());

        //clip with the clipping bounds
        if (box.getClipBlock() != null)
        {
            Rectangular clip = new Rectangular(box.getClipBlock().getAbsoluteContentBounds());
            ret = ret.intersection(clip);
        }
        
        return ret;
    }
    
    /**
     * @return the number of defined borders for the box
     */
    public int getBorderCount()
    {
        int bcnt = 0;
        if (hasTopBorder()) bcnt++;
        if (hasBottomBorder()) bcnt++;
        if (hasLeftBorder()) bcnt++;
        if (hasRightBorder()) bcnt++;
        return bcnt;
    }
    
    /**
     * @return <code>true</code> if the box has a top border
     */
    public boolean hasTopBorder()
    {
        Box box = getBox();
        if (box instanceof ElementBox && ((ElementBox) box).getBorder().top > 0)
            return true;
        else
            return false;
    }
    
    /**
     * Obtains the top border of the box
     * @return the width of the border or 0 when there is no border
     */
    public int getTopBorder()
    {
        Box box = getBox();
        if (box instanceof ElementBox)
            return ((ElementBox) box).getBorder().top;
        else
            return 0;
    }

    /**
     * @return <code>true</code> if the box has a bottom border
     */
    public boolean hasBottomBorder()
    {
        Box box = getBox();
        if (box instanceof ElementBox && ((ElementBox) box).getBorder().bottom > 0)
            return true;
        else
            return false;
    }
    
    /**
     * Obtains the bottom border of the box
     * @return the width of the border or 0 when there is no border
     */
    public int getBottomBorder()
    {
        Box box = getBox();
        if (box instanceof ElementBox)
            return ((ElementBox) box).getBorder().bottom;
        else
            return 0;
    }

    /**
     * @return <code>true</code> if the box has a left border
     */
    public boolean hasLeftBorder()
    {
        Box box = getBox();
        if (box instanceof ElementBox && ((ElementBox) box).getBorder().left > 0)
            return true;
        else
            return false;
    }
    
    /**
     * Obtains the left border of the box
     * @return the width of the border or 0 when there is no border
     */
    public int getLeftBorder()
    {
        Box box = getBox();
        if (box instanceof ElementBox)
            return ((ElementBox) box).getBorder().left;
        else
            return 0;
    }

    /**
     * @return <code>true</code> if the box has a right border
     */
    public boolean hasRightBorder()
    {
        Box box = getBox();
        if (box instanceof ElementBox && ((ElementBox) box).getBorder().right > 0)
            return true;
        else
            return false;
    }
    
    /**
     * Obtains the right border of the box
     * @return the width of the border or 0 when there is no border
     */
    public int getRightBorder()
    {
        Box box = getBox();
        if (box instanceof ElementBox)
            return ((ElementBox) box).getBorder().right;
        else
            return 0;
    }

    /**
     * Get the effective text color. If the text color is set, it is returned.
     * When the color is not set, the parent boxes are considered.
     * @return the background color string
     */
    public String getEfficientColor()
    {
        Box box = getBox(); 
        do
        {
            if (box instanceof ElementBox)
            {
                String color = ((ElementBox) box).getStylePropertyValue("color");
                if (!color.equals(""))
                    return color;
            }
            box = box.getParent();
        } while (box != null);
        return "";
    }

    /**
     * Obtains the text color of the first box in the area.
     * @return The color.
     */
    public Color getStartColor()
    {
    	return getBox().getVisualContext().getColor();
    }
    
    /**
     * Checks if the box text is visible (it does not contain spaces only). This is not equivalent to isWhitespace() because
     * non-beraking spaces are not whitespace characters but they are not visible.
     * @return <true> if the box text consists of space characters only
     */
    public boolean containsVisibleTextString()
    {
        String s = getText();
        for (int i = 0; i < s.length(); i++)
        {
            char ch = s.charAt(i);
            if (!Character.isSpaceChar(ch))
                return true;
        }
        return false;
    }
    
    /**
     * Checks whether the box is inside of the visible area and the text is visible and its color is different from the background
     * @return <code>true</code> if the box is visible
     */
    public boolean isVisible()
    {
        Box box = getBox();
        if (box == null)
            return false;
        else if (box instanceof TextBox)
            return box.isVisible() && containsVisibleTextString() && !getEfficientBackground().equals(getEfficientColor());
        else
            return box.isVisible();
    }
    
    /**
     * Returns the declared background color of the element or null when transparent.
     */
    public Color getBgcolor()
    {
        if (getBox() instanceof ElementBox)
            return ((ElementBox) getBox()).getBgcolor();
        else
            return null;
    }
    
    //===================================================================================
    
    /**
     * @return the contained box
     */
    public Box getBox()
    {
        return (Box) getUserObject();
    }
    
    /**
     * @return the parent box of this box in the tree
     */
    public BoxNode getParentBox()
    {
        return (BoxNode) getParent();
    }

    /**
     * Returns a specified child box.
     * @param index the child index
     * @return the specified child box
     */
    public BoxNode getChildBox(int index)
    {
        return (BoxNode) getChildAt(index);
    }
    
    /**
	 * @return the total bounds of the node
	 */
	public Rectangular getBounds()
	{
		return bounds;
	}

	/**
	 * @return the content bounds of the node
	 */
	public Rectangular getContentBounds()
	{
		return content;
	}

	/** 
     * Checks if another node is located inside the visual bounds of this box.
     * @param childNode the node to check
     * @return <code>true</code> if the child node is completely inside this node, <code>false</code> otherwise 
     */
    public boolean visuallyEncloses(BoxNode childNode)
    {
        int cx1 = childNode.getVisualBounds().getX1();
        int cy1 = childNode.getVisualBounds().getY1();
        int cx2 = childNode.getVisualBounds().getX2();
        int cy2 = childNode.getVisualBounds().getY2();
        int px1 = getVisualBounds().getX1();
        int py1 = getVisualBounds().getY1();
        int px2 = getVisualBounds().getX2();
        int py2 = getVisualBounds().getY2();
        
        /*if (this.toString().contains("pBody") && childNode.toString().contains("mediawiki"))
            System.out.println(childNode + " inside of " + this);
        if (childNode.toString().contains("www-lupa-cz") && this.toString().contains("[page]"))
            System.out.println(childNode + " inside of " + this);*/
        /*if (this.getOrder() == 70 && childNode.getOrder() == 74)
            System.out.println("jo!");*/
        
        
        //check how many corners of the child are inside enough (with some overlap)
        int ccnt = 0;
        if (cx1 >= px1 + OVERLAP && cx1 <= px2 - OVERLAP &&
            cy1 >= py1 + OVERLAP && cy1 <= py2 - OVERLAP) ccnt++; //top left
        if (cx2 >= px1 + OVERLAP && cx2 <= px2 - OVERLAP &&
            cy1 >= py1 + OVERLAP && cy1 <= py2 - OVERLAP) ccnt++; //top right
        if (cx1 >= px1 + OVERLAP && cx1 <= px2 - OVERLAP &&
            cy2 >= py1 + OVERLAP && cy2 <= py2 - OVERLAP) ccnt++; //bottom left
        if (cx2 >= px1 + OVERLAP && cx2 <= px2 - OVERLAP &&
            cy2 >= py1 + OVERLAP && cy2 <= py2 - OVERLAP) ccnt++; //bottom right
        //check how many corners of the child are inside the parent exactly
        int xcnt = 0;
        if (cx1 >= px1 && cx1 <= px2 &&
            cy1 >= py1 && cy1 <= py2) xcnt++; //top left
        if (cx2 >= px1 && cx2 <= px2 &&
            cy1 >= py1 && cy1 <= py2) xcnt++; //top right
        if (cx1 >= px1 && cx1 <= px2 &&
            cy2 >= py1 && cy2 <= py2) xcnt++; //bottom left
        if (cx2 >= px1 && cx2 <= px2 &&
            cy2 >= py1 && cy2 <= py2) xcnt++; //bottom right
        //and reverse direction - how many corners of the parent are inside of the child
        int rxcnt = 0;
        if (px1 >= cx1 && px1 <= cx2 &&
            py1 >= cy1 && py1 <= cy2) rxcnt++; //top left
        if (px2 >= cx1 && px2 <= cx2 &&
            py1 >= cy1 && py1 <= cy2) rxcnt++; //top right
        if (px1 >= cx1 && px1 <= cx2 &&
            py2 >= cy1 && py2 <= cy2) rxcnt++; //bottom left
        if (px2 >= cx1 && px2 <= cx2 &&
            py2 >= cy1 && py2 <= cy2) rxcnt++; //bottom right
        //shared areas
        int shared = getVisualBounds().intersection(childNode.getVisualBounds()).getArea();
        double sharedperc = (double) shared / childNode.getBounds().getArea();
        
        //no overlap
        if (xcnt == 0)
        	return false;
        //fully overlapping or over a corner - the order decides
        else if ((cx1 == px1 && cy1 == py1 && cx2 == px2 && cy2 == py2) //full overlap
        	     || (ccnt == 1 && xcnt <= 1)) //over a corner
        	return this.getOrder() < childNode.getOrder() && sharedperc >= AREAP;
        //fully inside
        else if (xcnt == 4)
            return true;
        //partly inside (at least two corners)
        else if (xcnt >= 2)
        {
            if (rxcnt == 4) //reverse relation - the child contains the parent
                return false;
            else //child partly inside the parent
                return this.getOrder() < childNode.getOrder() && sharedperc >= AREAP;
        }
        //not inside
        else
            return false;
    }

    /** 
     * Checks if another node is fully located inside the content bounds of this box.
     * @param childNode the node to check
     * @return <code>true</code> if the child node is completely inside this node, <code>false</code> otherwise 
     */
    public boolean visuallyEncloses1(BoxNode childNode)
    {
        int cx1 = childNode.getVisualBounds().getX1();
        int cy1 = childNode.getVisualBounds().getY1();
        int cx2 = childNode.getVisualBounds().getX2();
        int cy2 = childNode.getVisualBounds().getY2();
        int px1 = getVisualBounds().getX1();
        int py1 = getVisualBounds().getY1();
        int px2 = getVisualBounds().getX2();
        int py2 = getVisualBounds().getY2();
        
        //check how many corners of the child are inside the parent exactly
        int xcnt = 0;
        if (cx1 >= px1 && cx1 <= px2 &&
            cy1 >= py1 && cy1 <= py2) xcnt++; //top left
        if (cx2 >= px1 && cx2 <= px2 &&
            cy1 >= py1 && cy1 <= py2) xcnt++; //top right
        if (cx1 >= px1 && cx1 <= px2 &&
            cy2 >= py1 && cy2 <= py2) xcnt++; //bottom left
        if (cx2 >= px1 && cx2 <= px2 &&
            cy2 >= py1 && cy2 <= py2) xcnt++; //bottom right
        
        /*if (childNode.toString().contains("globalWrapper") && this.toString().contains("mediawiki"))
            System.out.println("jo!");*/
        
        if ((cx1 == px1 && cy1 == py1 && cx2 == px2 && cy2 == py2)) //exact overlap
           return this.getOrder() < childNode.getOrder();
        else
            return xcnt == 4;
    }
    
    /** 
     * Checks if another node is fully located inside the content bounds of this box.
     * @param childNode the node to check
     * @return <code>true</code> if the child node is completely inside this node, <code>false</code> otherwise 
     */
    public boolean contentEncloses(BoxNode childNode)
    {
    	//System.out.println(childNode + " => " + childNode.getVisualBounds());
        int cx1 = childNode.getContentBounds().getX1();
        int cy1 = childNode.getContentBounds().getY1();
        int cx2 = childNode.getContentBounds().getX2();
        int cy2 = childNode.getContentBounds().getY2();
        int px1 = getContentBounds().getX1();
        int py1 = getContentBounds().getY1();
        int px2 = getContentBounds().getX2();
        int py2 = getContentBounds().getY2();
        
        //check how many corners of the child are inside the parent exactly
        int xcnt = 0;
        if (cx1 >= px1 && cx1 <= px2 &&
            cy1 >= py1 && cy1 <= py2) xcnt++; //top left
        if (cx2 >= px1 && cx2 <= px2 &&
            cy1 >= py1 && cy1 <= py2) xcnt++; //top right
        if (cx1 >= px1 && cx1 <= px2 &&
            cy2 >= py1 && cy2 <= py2) xcnt++; //bottom left
        if (cx2 >= px1 && cx2 <= px2 &&
            cy2 >= py1 && cy2 <= py2) xcnt++; //bottom right
        
        if ((cx1 == px1 && cy1 == py1 && cx2 == px2 && cy2 == py2)) //exact overlap
           return this.getOrder() < childNode.getOrder();
        else
            return xcnt == 4;
    }
    
    /** 
     * Expands the box node in order to fully enclose another box 
     */
    public void expandToEnclose(BoxNode child)
    {
    	bounds.expandToEnclose(child.getBounds());
    }
    
    /**
     * Takes a list of nodes and selects the nodes that are located directly inside 
     * of this node's box. The nearestParent of the selected boxes is set to this box.
     * @param list the list of nodes to test
     * @param full when set to true, all the nodes within the box content bounds are considered.
     *          Otherwise, only the boxes within the visual bounds are considered.
     * @return the list of nodes from the collection that are located inside of this node's box
     */
    public void markNodesInside(Vector<BoxNode> list, boolean full)
    {
        for (Iterator<BoxNode> it = list.iterator(); it.hasNext();)
        {
            BoxNode node = it.next();
            if (full)
            {
                if (node != this 
                    && this.contentEncloses(node)
                    && (node.isRootNode() || !this.contentEncloses(node.nearestParent))) 
                {
                    node.nearestParent = this;
                }
            }
            else
            {
                if (node != this 
                        && this.visuallyEncloses(node)
                        && (node.isRootNode() || !this.visuallyEncloses(node.nearestParent))) 
                {
                    node.nearestParent = this;
                }
            }
        }
    }
    
    /**
     * Goes through the parent's children, takes all the nodes that are inside of this node
     * and makes them the children of this node. Then, recursively calls the children to take
     * their nodes.
     */
	public void takeChildren(Vector<BoxNode> list)
    {
        for (Iterator<BoxNode> it = list.iterator(); it.hasNext();)
        {
            BoxNode node = it.next();
            if (node.nearestParent.equals(this))    
            {
                add(node);
                it.remove();
            }
        }
        
        //let the children take their children
        for (int i = 0; i < getChildCount(); i++)
            getChildBox(i).takeChildren(list);
    }
    
    /**
     * Removes the node from the tree. Clears the parent and removes all the child
     * node.
     */
    public void removeFromTree()
    {
        nearestParent = null;
        //setParent(null);
        removeAllChildren();
    }
    
    //==================================================================================
    
    /**
     * @return all the text contained in this box and its subboxes.
     * Contents of the individual text boxes are separated by spaces.
     */
    public String getText()
    {
        return recursiveGetText(this);
    }
    
    private String recursiveGetText(BoxNode root)
    {
        Box box = root.getBox();
        if (box instanceof TextBox)
            return ((TextBox) box).getText();
        else
        {
            String ret = "";
            for (int i = 0; i < root.getChildCount(); i++)
            {
                if (ret.trim().length() > 0)
                    ret += " ";
                ret = ret + recursiveGetText(root.getChildBox(i)).trim();
            }
            return ret;
        }
    }
    
    //==================================================================================
    
    public void drawExtent(BrowserCanvas canvas)
    {
        Graphics g = canvas.getImageGraphics();
        //getBox().drawExtent(g);
        
        //draw the visual content box
        g.setColor(java.awt.Color.RED);
        Rectangular r = getBounds();
        g.drawRect(r.getX1(), r.getY1(), r.getWidth() - 1, r.getHeight() - 1);
        
        //draw the visual content box
        g.setColor(java.awt.Color.GREEN);
        r = getVisualBounds();
        g.drawRect(r.getX1(), r.getY1(), r.getWidth() - 1, r.getHeight() - 1);
    }


}
