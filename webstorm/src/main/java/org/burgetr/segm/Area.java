/**
 * VisualArea.java
 *
 * Created on 3-�en-06, 10:58:23  by radek
 */
package org.burgetr.segm;

import java.util.*;
import java.awt.*;

import org.burgetr.segm.tagging.Tag;
import org.fit.cssbox.layout.BlockReplacedBox;
import org.fit.cssbox.layout.Box;
import org.fit.cssbox.layout.BrowserCanvas;
import org.fit.cssbox.layout.ElementBox;
import org.fit.cssbox.layout.InlineReplacedBox;
import org.fit.cssbox.layout.ReplacedContent;
import org.fit.cssbox.layout.TextBox;

/**
 * An area containing several visual boxes.
 * 
 * @author radek
 */
public class Area
{
    private static int nextid = 1;
    
    private int id;
    
	/**
	 * The visual boxes that form this area.
	 */
	private Vector<BoxNode> boxes;
	
	/**
	 * Declared bounds of the area.
	 */
	private Rectangular bounds;
	
	/**
	 * Effective bounds of the area content.
	 */
	private Rectangular contentBounds;

	/**
     * Area description
     */
    private String name;
    
    /**
     * Area level. 0 corresponds to the areas formed by boxes, greater numbers represent
     * greater level of grouping
     */
    private int level = 0;
    
    /**
     * Borders present?
     */
    private boolean btop, bleft, bbottom, bright;
    
    /**
     * Background color of the first box in the area.
     */
    private Color bgcolor;
    
    /**
     * Is the first box in the area separated by background?
     */
    private boolean backgroundSeparated;
    
    /**
     * Sum for computing the average font size
     */
    private int fontSizeSum = 0;
    
    /**
     * Counter for computing the average font size
     */
    private int fontSizeCnt = 0;
    
    private int fontWeightSum = 0;
    private int fontWeightCnt = 0;
    private int fontStyleSum = 0;
    private int fontStyleCnt = 0;
    
    
	//================================================================================
	
    /** 
     * Creates an empty area of a given size
     */
    public Area(int x1, int y1, int x2, int y2)
	{
        id = nextid++;
		boxes = new Vector<BoxNode>();
		bounds = new Rectangular(x1, y1, x2, y2);
        name = null;
        btop = false;
        bleft = false;
        bright = false;
        bbottom = false;
        bgcolor = null;
	}
    
    /** 
     * Creates an empty area of a given size
     */
    public Area(Rectangular r)
    {
        id = nextid++;
        boxes = new Vector<BoxNode>();
        bounds = new Rectangular(r);
        name = null;
        btop = false;
        bleft = false;
        bright = false;
        bbottom = false;
        bgcolor = null;
    }
    
    /** 
     * Creates an area from a single box. Update the area bounds and name accordingly.
     * @param box The source box that will be contained in this area
     */
    public Area(BoxNode box)
    {
        id = nextid++;
        boxes = new Vector<BoxNode>();
        addBox(box); //expands the content bounds appropriately
        bounds = new Rectangular(contentBounds);
        this.name = box.toString();
        btop = box.hasTopBorder();
        bleft = box.hasLeftBorder();
        bright = box.hasRightBorder();
        bbottom = box.hasBottomBorder();
        bgcolor = box.getBgcolor();
        backgroundSeparated = box.isBackgroundSeparated();
    }
    
    /** 
     * Creates an area from a a list of boxes. Update the area bounds and name accordingly.
     * @param boxes The source boxes that will be contained in this area
     */
    public Area(Vector<BoxNode> boxlist)
    {
        id = nextid++;
        boxes = new Vector<BoxNode>(boxlist.size());
        for (BoxNode box : boxlist)
            addBox(box); //expands the content bounds appropriately
        BoxNode box = boxlist.firstElement();
        bounds = new Rectangular(contentBounds);
        this.name = box.toString();
        btop = box.hasTopBorder();
        bleft = box.hasLeftBorder();
        bright = box.hasRightBorder();
        bbottom = box.hasBottomBorder();
        bgcolor = box.getBgcolor();
        backgroundSeparated = box.isBackgroundSeparated();
    }
    
    /** 
     * Creates a copy of another area.
     * @param area The source area
     */
    public Area(Area src)
    {
        id = nextid++;
        boxes = new Vector<BoxNode>(src.getBoxes());
        contentBounds = (src.contentBounds == null) ? null : new Rectangular(src.contentBounds);
        bounds = new Rectangular(src.bounds);
        name = (src.name == null) ? null : new String(src.name);
        btop = src.btop;
        bleft = src.bleft;
        bright = src.bright;
        bbottom = src.bbottom;
        bgcolor = (src.bgcolor == null) ? null : new Color(src.bgcolor.getRed(), src.bgcolor.getGreen(), src.bgcolor.getBlue());
        backgroundSeparated = src.backgroundSeparated;
        level = src.level;
        fontSizeSum = src.fontSizeSum;
        fontSizeCnt = src.fontStyleCnt;
        fontStyleSum = src.fontStyleSum;
        fontStyleCnt = src.fontStyleCnt;
        fontWeightSum = src.fontWeightSum;
        fontWeightCnt = src.fontWeightCnt;
    }
    
    public int getId()
    {
        return id;
    }
    
    /**
     * Joins another area to this area. Update the bounds and the name accordingly.
     * @param other The area to be joined to this area.
     * @param horizontal If true, the areas are joined horizontally.
     * This influences the resulting area borders. If false, the areas are joined vertically.
     */
    public void join(Area other, boolean horizontal)
    {
    	bounds.expandToEnclose(other.bounds);
    	name = name + " . " + other.name;
        //update border information according to the mutual area positions
        if (horizontal)
        {
            if (getX1() <= other.getX1())
            {
                if (other.hasRightBorder()) bright = true;
            }
            else
            {
                if (other.hasLeftBorder()) bleft = true;
            }
        }
        else
        {
            if (getY1() <= other.getY1())
            {
                if (other.hasBottomBorder()) bbottom = true;
            }
            else
            {
                if (other.hasTopBorder()) btop = true;
            }
        }
        //add all the contained boxes
        boxes.addAll(other.boxes);
        updateAverages(other);
        //just a test
        if (!this.hasSameBackground(other))
        	System.err.println("Area: Warning: joining areas " + name + " and " + other.name + 
        	        " of different background colors " + this.bgcolor + " x " + other.bgcolor); 
    }
    
    /**
     * Joins a child area to this area. Updates the bounds and the name accordingly.
     * @param other The child area to be joined to this area.
     */
    public void joinChild(Area other)
    {
        //TODO obsah se neimportuje?
        bounds.expandToEnclose(other.bounds);
        name = name + " . " + other.name;
    }
    
    /**
     * Sets the name of the area. The name is used when the area information is displayed
     * using <code>toString()</code>
     * @param The new area name
     */
    public void setName(String name)
    {
        this.name = name;
    }
    
    public String getName()
    {
    	return name;
    }
    
	public int getLevel()
	{
		return level;
	}

	public void setLevel(int level)
	{
		this.level = level;
	}

	public String toString()
    {
        String bs = "";
        //bs += "{" + getAverageFontSize() + "=" + fontSizeSum + "/" + fontSizeCnt + "}"; 
        /*    + ":" + getAverageFontWeight() 
            + ":" + getAverageFontStyle() + "}";*/
        
        if (hasTopBorder()) bs += "^";
        if (hasLeftBorder()) bs += "<";
        if (hasRightBorder()) bs += ">";
        if (hasBottomBorder()) bs += "_";
        if (isBackgroundSeparated()) bs += "*";
        
        /*if (isHorizontalSeparator()) bs += "H";
        if (isVerticalSeparator()) bs += "I";*/
        
        /*if (bgcolor != null)
            bs += "\"" + String.format("#%02x%02x%02x", bgcolor.getRed(), bgcolor.getGreen(), bgcolor.getBlue()) + "\"";*/
        
        if (name != null)
            return bs + " " + name + " " + bounds.toString();
        else
            return bs + " " + "<area> " + bounds.toString();
          
    }
    
    /**
     * Add the box node to the area if its bounds are inside of the area bounds.
     * @param node The box node to be added
     */
    public void chooseBox(BoxNode node)
    {
    	if (bounds.encloses(node.getVisualBounds()))
    		addBox(node);
    }
    
    /**
     * Returns a vector of boxes that are inside of this area
     * @return A vector containing the {@link org.burgetr.segm.BoxNode BoxNode} objects
     */
    public Vector<BoxNode> getBoxes()
    {
    	return boxes;
    }
    
    /**
     * Set the borders around
     */
    public void setBorders(boolean top, boolean left, boolean bottom, boolean right)
    {
    	btop = top;
    	bleft = left;
    	bbottom = bottom;
    	bright = right;
    }

	//=================================================================================
	
    public Rectangular getBounds()
    {
    	return bounds;
    }
    
    public void setBounds(Rectangular bounds)
    {
        this.bounds = bounds;
    }
    
    public Rectangular getContentBounds()
    {
        return contentBounds;
    }
	
    public int getX1()
    {
    	return bounds.getX1();
    }
    
    public int getY1()
    {
    	return bounds.getY1();
    }
    
    public int getX2()
    {
    	return bounds.getX2();
    }
    
    public int getY2()
    {
    	return bounds.getY2();
    }
    
    public int getWidth()
    {
    	return bounds.getWidth();
    }
    
    public int getHeight()
    {
    	return bounds.getHeight();
    }
    
    /**
     * Computes the square area occupied by this visual area.
     * @return the square area in pixels
     */
    public int getSquareArea()
    {
        return bounds.getArea();
    }
    
    public boolean hasTopBorder()
    {
        return btop;
    }
    
    public boolean hasLeftBorder()
    {
        return bleft;
    }
    
    public boolean hasRightBorder()
    {
        return bright;
    }
    
    public boolean hasBottomBorder()
    {
        return bbottom;
    }
    
    public Color getBackgroundColor()
    {
    	return bgcolor;
    }
    
    public boolean isBackgroundSeparated()
    {
        return backgroundSeparated;
    }
    
    /**
     * Checks if this area has the same background color as another area
     * @param other the other area
     * @return true if the areas are both transparent or they have the same
     * background color declared
     */
    public boolean hasSameBackground(Area other)
    {
        return (bgcolor == null && other.bgcolor == null) || 
               (bgcolor != null && other.bgcolor != null && bgcolor.equals(other.bgcolor));
    }
    
    public boolean encloses(Area other)
    {
    	return bounds.encloses(other.bounds);
    }
    
    public boolean contains(int x, int y)
    {
    	return bounds.contains(x, y);
    }
    
    public boolean hasContent()
    {
        return !boxes.isEmpty();
    }
    
    //======================================================================================
    
    /**
     * @return true if the area contains any text
     */
    public boolean containsText()
    {
        boolean ret = false;
        for (BoxNode root : boxes)
        {
            //scan all the leaf nodes and seek for text
            Enumeration<?> sub = root.depthFirstEnumeration();
            while (sub.hasMoreElements())
            {
                BoxNode node = (BoxNode) sub.nextElement();
                if (node.isLeaf() && node.getBox() != null && node.getBox() instanceof TextBox)
                {
                    if (((TextBox) node.getBox()).getText().trim().length() > 0)
                        ret = true;
                }
            }
        }
        return ret;
    }
    
    /**
     * @return true if the area contains replaced boxes only
     */
    public boolean isReplaced()
    {
        boolean empty = true;
        for (BoxNode root : boxes)
        {
            if (root.getBox() != null)
            {
                empty = false;
                if (!root.getBox().isReplaced())
                    return false;
            }
        }
        return !empty;
    }
    
    /**
     * Returns the text string represented by a concatenation of all
     * the boxes contained directly in this area.
     */
    public String getBoxText()
    {
        StringBuilder ret = new StringBuilder();
        boolean start = true;
        for (Iterator<BoxNode> it = boxes.iterator(); it.hasNext(); )
        {
            if (!start) ret.append(' ');
            else start = false;
            ret.append(it.next().getText());
        }
        return ret.toString();
    }
    
    /**
     * Returns the text string represented by a concatenation of all
     * the boxes contained directly in this area.
     */
    public int getTextLength()
    {
        int ret = 0;
        for (BoxNode box : boxes)
        {
            ret += box.getText().length();
        }
        return ret;
    }
    
    /**
     * @return true if the area contains any text
     */
	public ReplacedContent getReplacedContent()
    {
        ReplacedContent ret = null;
        for (Iterator<BoxNode> it = boxes.iterator(); it.hasNext(); )
        {
            BoxNode root = it.next();
            //scan all the leaf nodes and seek for text
            Enumeration<?> sub = root.depthFirstEnumeration();
            while (sub.hasMoreElements())
            {
                BoxNode node = (BoxNode) sub.nextElement();
                if (node.isLeaf() && node.getBox() != null)
                {
	                if 	(node.getBox() instanceof InlineReplacedBox)
	                {
	                	ret = ((InlineReplacedBox) node.getBox()).getContentObj();
	                	if (ret != null)
	                		break;
	                }
	                if 	(node.getBox() instanceof BlockReplacedBox)
	                {
	                	ret = ((BlockReplacedBox) node.getBox()).getContentObj();
	                	if (ret != null)
	                		break;
	                }
                }
            }
        }
        return ret;
    }
    
    /**
     * Tries to guess if this area acts as a horizontal separator. The criteria are:
     * <ul>
     * <li>It doesn't contain any text</li>
     * <li>It is visible</li>
     * <li>It is low and wide</li>
     * </ul>
     * @return true if the area can be used as a horizontal separator
     */
    public boolean isHorizontalSeparator()
    {
        return !containsText() && 
               bounds.getHeight() < 10 &&
               bounds.getWidth() > 20 * bounds.getHeight();
    }
    
    /**
     * Tries to guess if this area acts as a vertical separator. The criteria are the same
     * as for the horizontal one.
     * @return true if the area can be used as a vertical separator
     */
    public boolean isVerticalSeparator()
    {
        return !containsText() && 
               bounds.getWidth() < 10 &&
               bounds.getHeight() > 20 * bounds.getWidth();
    }
    
    /**
     * Tries to guess if this area acts as any kind of separator.
     * See the {@link #isVerticalSeparator()} and {@link #isHorizontalSeparator()} methods for more explanation.
     * @return true if the area can be used as a separator
     */
    public boolean isSeparator()
    {
        return isHorizontalSeparator() || isVerticalSeparator();
    }
    
    /**
     * Returns the size height declared for the corresponding box. If there are multiple boxes,
     * the first one is used. If there are no boxes (an artificial area), 0 is returned.
     * @return 
     */
    public double getDeclaredFontSize()
    {
        if (boxes.size() > 0)
            return boxes.firstElement().getBox().getVisualContext().getFont().getSize2D();
        else
            return 0;
    }
    
    /**
     * Computes the average font size of the boxes in the area
     * @return the font size
     */
    public double getAverageFontSize()
    {
        if (fontSizeCnt == 0)
            return 0;
        else
            return (double) fontSizeSum / fontSizeCnt;
    }
    
    /**
     * Computes the average font weight of the boxes in the area
     * @return the font size
     */
    public double getAverageFontWeight()
    {
        if (fontWeightCnt == 0)
            return 0;
        else
            return (double) fontWeightSum / fontWeightCnt;
    }
    
    /**
     * Computes the average font style of the boxes in the area
     * @return the font style
     */
    public double getAverageFontStyle()
    {
        if (fontStyleCnt == 0)
            return 0;
        else
            return (double) fontStyleSum / fontStyleCnt;
    }
    
    /**
     * Computes the average luminosity of the boxes in the area
     * @return the font size
     */
    public double getAverageColorLuminosity()
    {
        if (boxes.isEmpty())
            return 0;
        else
        {
            double sum = 0;
            int len = 0;
            for (BoxNode box : boxes)
            {
                int l = box.getText().length(); 
                sum += FeatureAnalyzer.colorLuminosity(box.getBox().getVisualContext().getColor()) * l;
                len += l;
            }
            return sum / len;
        }
    }
    
    /**
     * Updates the average values when a new area is added or joined
     * @param other the other area
     */
    public void updateAverages(Area other)
    {
        fontSizeCnt += other.fontSizeCnt;
        fontSizeSum += other.fontSizeSum;
        fontWeightCnt += other.fontWeightCnt;
        fontWeightSum += other.fontWeightSum;
        fontStyleCnt += other.fontStyleCnt;
        fontStyleSum += other.fontStyleSum;
    }
    
    //=================================================================================
    
    public void drawExtent(BrowserCanvas canvas)
    {
        Graphics ig = canvas.getImageGraphics();
        Color c = ig.getColor();
        ig.setColor(Color.MAGENTA);
        ig.drawRect(bounds.getX1(), bounds.getY1(), bounds.getWidth() - 1, bounds.getHeight() - 1);
        ig.setColor(c);
    }
    
    public void colorizeByString(BrowserCanvas canvas, String s)
    {
        Graphics ig = canvas.getImageGraphics();
        Color c = ig.getColor();
        ig.setColor(stringColor(s));
        ig.fillRect(bounds.getX1(), bounds.getY1(), bounds.getWidth() - 1, bounds.getHeight() - 1);
        ig.setColor(c);
    }
    
    public void colorizeByTags(BrowserCanvas canvas, Set<Tag> s)
    {
        if (!s.isEmpty())
        {
            Graphics ig = canvas.getImageGraphics();
            Color c = ig.getColor();
            float step = (float) bounds.getHeight() / s.size();
            float y = bounds.getY1();
            for (Iterator<Tag> it = s.iterator(); it.hasNext();)
            {
                Tag tag = it.next();
                ig.setColor(stringColor(tag.getValue()));
                ig.fillRect(bounds.getX1(), (int) y, bounds.getWidth(), (int) (step+0.5));
                y += step;
            }
            ig.setColor(c);
        }
    }
    
    private Color stringColor(String cname)                                 
    {                                                                            
            if (cname == null || cname.equals(""))       
                    return Color.WHITE;                                                 
                                                                                 
            String s = new String(cname);                                        
            while (s.length() < 6) s = s + s;                                    
            int r = (int) s.charAt(0) *  (int) s.charAt(1);                      
            int g = (int) s.charAt(2) *  (int) s.charAt(3);                      
            int b = (int) s.charAt(4) *  (int) s.charAt(5);                      
            Color ret = new Color(100 + (r % 150), 100 + (g % 150), 100 + (b % 150), 128);              
            //System.out.println(cname + " => " + ret.toString());               
            return ret;                                                          
    } 
    
	//=================================================================================

	/**
	 * Adds a new box to the area and updates the area bounds.
	 * @param box the new box to add
	 */
	private void addBox(BoxNode box)
	{
		boxes.add(box);

        Rectangular sb = box.getVisualBounds();
        if (contentBounds == null)
        	contentBounds = new Rectangular(sb);
        else if (sb.getWidth() > 0 && sb.getHeight() > 0)
        	contentBounds.expandToEnclose(sb);
        
        if (box.getBox() instanceof TextBox)
        {
            int len = box.getBox().getText().trim().length();
            if (len > 0)
            {
               	fontSizeSum += getAverageBoxFontSize(box.getBox()) * len;
                fontSizeCnt += len;
               	fontWeightSum += getAverageBoxFontWeight(box.getBox()) * len;
                fontWeightCnt += len;
               	fontStyleSum += getAverageBoxFontStyle(box.getBox()) * len;
                fontStyleCnt += len;
            }
        }
	}
	
	private double getAverageBoxFontSize(Box box)
	{
		if (box instanceof TextBox)
			return box.getVisualContext().getFont().getSize();
		else if (box.isReplaced())
			return 0;
		else if (box instanceof ElementBox)
		{
			ElementBox el = (ElementBox) box;
			double sum = 0;
			int cnt = 0;
			for (int i = el.getStartChild(); i < el.getEndChild(); i++)
			{
				Box child = el.getSubBox(i);
				String text = child.getText().trim();
				cnt += text.length();
				sum += getAverageBoxFontSize(child);
			}
			if (cnt > 0)
				return sum / cnt;
			else
				return 0;
		}
		else
			return 0;
	}
	
	private double getAverageBoxFontWeight(Box box)
	{
		if (box instanceof TextBox)
			return box.getVisualContext().getFont().isBold() ? 1.0 : 0.0;
		else if (box.isReplaced())
			return 0;
		else if (box instanceof ElementBox)
		{
			ElementBox el = (ElementBox) box;
			double sum = 0;
			int cnt = 0;
			for (int i = el.getStartChild(); i < el.getEndChild(); i++)
			{
				Box child = el.getSubBox(i);
				String text = child.getText().trim();
				cnt += text.length();
				sum += getAverageBoxFontWeight(child);
			}
			if (cnt > 0)
				return sum / cnt;
			else
				return 0;
		}
		else
			return 0;
	}
	
	private double getAverageBoxFontStyle(Box box)
	{
		if (box instanceof TextBox)
			return box.getVisualContext().getFont().isItalic() ? 1.0 : 0;
		else if (box.isReplaced())
			return 0;
		else if (box instanceof ElementBox)
		{
			ElementBox el = (ElementBox) box;
			double sum = 0;
			int cnt = 0;
			for (int i = el.getStartChild(); i < el.getEndChild(); i++)
			{
				Box child = el.getSubBox(i);
				String text = child.getText().trim();
				cnt += text.length();
				sum += getAverageBoxFontStyle(child);
			}
			if (cnt > 0)
				return sum / cnt;
			else
				return 0;
		}
		else
			return 0;
	}
}
