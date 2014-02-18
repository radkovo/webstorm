/**
 * BoxTree.java
 * 
 * 
 */
package org.burgetr.segm;

import java.awt.Color;
import java.util.*;

import org.fit.cssbox.layout.*;

/**
 * This class represents a tree of visual page blocks.
 * 
 * @author burgetr
 */
public class BoxTree
{
    /** The root box from which the tree has been created */
    private ElementBox rootbox;
    
    /** The root node of the resulting tree */
    private BoxNode root;
    
    /** The list of all nodes in the source box tree */
    private Vector<BoxNode> boxlist;
    
    /** a counter for assigning the box order */
    private int order_counter;
    
    /**
     * Creates a new visual block tree from a tree of positioned boxes. 
     * @param rbox the root box of the tree (usually <code>body</code>)
     */
    public BoxTree(Viewport rbox)
    {
        rootbox = rbox;

        //an artificial root node
        root = new BoxNode(rootbox);
        root.setOrder(0);
        
        //create the working list of nodes
        //System.out.println("LIST");
        boxlist = new Vector<BoxNode>();
        order_counter = 1;
        createBoxList(rootbox, boxlist);
        
        //create the tree
        //System.out.println("A1");
        createBoxTree(true); //create a nesting tree based on the content bounds
        //System.out.println("A2");
        Color bg = rbox.getBgcolor();
        if (bg == null) bg = Color.WHITE;
        computeBackgrounds(root, bg); //compute the efficient background colors
        //System.out.println("A2.5");
        root.recomputeVisualBounds(); //compute the visual bounds for the whole tree
        //System.out.println("A3");
        createBoxTree(false); //create the nesting tree based on the visual bounds
        root.recomputeBounds(); //compute the real bounds of each node
        //System.out.println("A4");
    }
    
    /**
     * @return the root node of the tree
     */
    public BoxNode getRoot()
    {
        return root;
    }
    
    /**
     * @return the source root box from which the tree has been created
     */
    public ElementBox getRootBox()
    {
        return rootbox;
    }
    
    //========================================================================

    
    /**
     * Recursively creates a list of all the visible boxes in a box subtree. The nodes are 
     * added to the end of a specified list. The previous content of the list 
     * remains unchanged. The 'viewport' box is ignored.
     * @param root the source root box
     * @param list the list that will be filled with the nodes
     */
    private void createBoxList(Box root, Vector<BoxNode> list)
    {
        if (root.isDisplayed())
        {
        	if (!(root instanceof Viewport)
        	    && root.isVisible()
        		&& root.getWidth() > 0 && root.getHeight() > 0)
            {
        	    BoxNode newnode = new BoxNode(root);
	    	    newnode.setOrder(order_counter++);
	    		list.add(newnode);
            }
            if (root instanceof ElementBox)
            {
                ElementBox elem = (ElementBox) root;
                for (int i = elem.getStartChild(); i < elem.getEndChild(); i++)
                    createBoxList(elem.getSubBox(i), list);
            }
        }
    }

    /**
     * Creates a tree of box nesting based on the content bounds of the boxes.
     * This tree is only used for determining the backgrounds.
     * @param full when set to true, the tree is build according to the content bounds
     * of each box. Otherwise, only the visual bounds are used.
     */
    private void createBoxTree(boolean full)
    {
        //a working copy of the box list
        Vector<BoxNode> list = new Vector<BoxNode>(boxlist);

        //destroy any old trees
        root.removeAllChildren();
        for (BoxNode node : list)
            node.removeFromTree();
        
        //when working with visual bounds, remove the boxes that are not visually separated
        if (!full)
        {
            for (Iterator<BoxNode> it = list.iterator(); it.hasNext(); )
            {
                BoxNode node = it.next();
                if (!node.isVisuallySeparated() || !node.isVisible())
                    it.remove();
            }
        }
        
        //let each node choose it's children - find the roots and parents
        for (BoxNode node : list)
            node.markNodesInside(list, full);
        
        //choose the roots
        for (Iterator<BoxNode> it = list.iterator(); it.hasNext();)
        {
            BoxNode node = it.next();
            
            /*if (!full) //DEBUG
            {
               if (node.toString().contains("mediawiki") || node.toString().contains("globalWrapper"))
                    System.out.println(node + " => " + node.nearestParent);
            }*/
            
            if (node.isRootNode())
            {
                root.add(node);
                it.remove();
            }
        }
        
        //recursively choose the children
        for (int i = 0; i < root.getChildCount(); i++)
            root.getChildBox(i).takeChildren(list);
    }
    
    /**
     * Computes efficient background color for all the nodes in the tree
     */
    private void computeBackgrounds(BoxNode root, Color currentbg)
    {
        //if (root.getBox() != null && root.getBox().toString().contains("body"))
        //    System.out.println("jo!");
        Color newbg = root.getBgcolor();
        if (newbg == null)
            newbg = currentbg;
        root.setEfficientBackground(newbg);
        root.setBackgroundSeparated(!newbg.equals(currentbg));
        
        for (int i = 0; i < root.getChildCount(); i++)
            computeBackgrounds(root.getChildBox(i), newbg);
    }
    
    
    //========================================================================
    
    public BoxNode getBoxAt(int x, int y)
    {
        return recursiveGetBoxAt(root, x, y);
    }
    
    private BoxNode recursiveGetBoxAt(BoxNode root, int x, int y)
    {
        if (root.getBox() == null || root.getBounds().contains(x, y))
        {
            for (int i = 0; i < root.getChildCount(); i++)
            {
                BoxNode ret = recursiveGetBoxAt(root.getChildBox(i), x, y);
                if (ret != null)
                    return ret;
            }
            return root;
        }
        else
            return null;
    }

    //========================================================================
    
    public Vector<BoxNode> getBoxesInRegion(Rectangular r)
    {
        Vector<BoxNode> ret = new Vector<BoxNode>();
        recursiveGetBoxesInRegion(root, r, ret);
        return ret;
    }
    
    private void recursiveGetBoxesInRegion(BoxNode root, Rectangular r, Vector<BoxNode> result)
    {
        if (root.getBox() != null && r.intersects(root.getBounds()))
        {
            result.add(root);
        }
        else
        {
            for (int i = 0; i < root.getChildCount(); i++)
                recursiveGetBoxesInRegion(root.getChildBox(i), r, result);
        }
    }
    
    //========================================================================
    
    public BoxNode getNodeContaining(Box box)
    {
        return recursiveGetNodeContaining(root, box);
    }
    
    private BoxNode recursiveGetNodeContaining(BoxNode root, Box box)
    {
        if (root.getBox() == box)
            return root;
        else
        {
            for (int i = 0; i < root.getChildCount(); i++)
            {
                BoxNode ret = recursiveGetNodeContaining(root.getChildBox(i), box);
                if (ret != null)
                    return ret;
            }
            return null;
        }
    }
    
    
}
