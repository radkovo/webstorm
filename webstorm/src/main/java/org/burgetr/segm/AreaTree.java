/**
 * VisualAreaTree.java
 *
 * Created on 28.6.2006, 15:10:11 by burgetr
 */
package org.burgetr.segm;

import java.util.HashSet;
import java.util.Set;

import org.burgetr.segm.tagging.Tag;

/**
 * A tree of visual areas created from a box tree.
 * 
 * @author burgetr
 */
public class AreaTree implements GenericAreaTree
{
    /** The tree root */
    protected AreaNode root;
    
    /** The source tree */
    protected BoxTree boxtree;
    
    /** The root node area */
    protected Area rootarea;
    
    //=================================================================================
    
    /**
     * Create a new tree of areas by the analysis of a box tree
     * @param srctree the source box tree
     */
    public AreaTree(BoxTree srctree)
    {
        boxtree = srctree;
        rootarea = new Area(0, 0, 0, 0);
        root = new AreaNode(rootarea);
    }
    
    /**
     * @return the root node of the tree of areas
     */
    public AreaNode getRoot()
    {
        return root;
    }
    
    /**
     * Creates the area tree skeleton - selects the visible boxes and converts
     * them to areas 
     */
    public AreaNode findBasicAreas()
    {
        rootarea = new Area(0, 0, 0, 0);
        root = new AreaNode(rootarea);
        for (int i = 0; i < boxtree.getRoot().getChildCount(); i++)
        {
            AreaNode sub;
            sub = new AreaNode(new Area(boxtree.getRoot().getChildBox(i)));
            if (sub.getWidth() > 1 || sub.getHeight() > 1)
            {
                findStandaloneAreas(boxtree.getRoot().getChildBox(i), sub);
                root.addArea(sub);
            }
        }
        createGrids(root);
        return root;
    }
    
    //=================================================================================
    
    /**
     * Goes through a box tree and tries to identify the boxes that form standalone
     * visual areas. From these boxes, new areas are created, which are added to the
     * area tree. Other boxes are ignored.
     * @param boxroot the root of the box tree
     * @param arearoot the root node of the new area tree 
     */ 
    private void findStandaloneAreas(BoxNode boxroot, AreaNode arearoot)
    {
        if (boxroot.isVisible())
        {
            for (int i = 0; i < boxroot.getChildCount(); i++)
            {
                BoxNode child = boxroot.getChildBox(i);
		        if (child.isVisible())
		        {
	                if (child.isVisuallySeparated())
	                {
	                    AreaNode newnode = new AreaNode(new Area(child));
	                    if (newnode.getWidth() > 1 || newnode.getHeight() > 1)
	                    {
                            findStandaloneAreas(child, newnode);
	                    	arearoot.addArea(newnode);
	                    }
	                }
	                else
	                    findStandaloneAreas(child, arearoot);
		        }
            }
        }
    }
    
    /**
     * Goes through all the areas in the tree and creates the grids in these areas
     * @param root the root node of the tree of areas
     */
    protected void createGrids(AreaNode root)
    {
        root.createGrid();
        for (int i = 0; i < root.getChildCount(); i++)
            createGrids(root.getChildArea(i));
    }

    
    //=================================================================================
    
    public AreaNode getAreaAt(int x, int y)
    {
        return recursiveGetAreaAt(root, x, y);
    }
    
    private AreaNode recursiveGetAreaAt(AreaNode root, int x, int y)
    {
        if (root.getArea().contains(x, y))
        {
            for (int i = 0; i < root.getChildCount(); i++)
            {
                AreaNode ret = recursiveGetAreaAt(root.getChildArea(i), x, y);
                if (ret != null)
                    return ret;
            }
            return root;
        }
        else
            return null;
    }
    
    public AreaNode getAreaByName(String name)
    {
        return recursiveGetAreaByName(root, name);
    }
    
    private AreaNode recursiveGetAreaByName(AreaNode root, String name)
    {
        if (root.toString().indexOf(name) != -1)
            return root;
        else
        {
            for (int i = 0; i < root.getChildCount(); i++)
            {
                AreaNode ret = recursiveGetAreaByName(root.getChildArea(i), name);
                if (ret != null)
                    return ret;
            }
            return null;
        }
    }

    //=================================================================================
    
    /**
     * Obtains all the tags that are really used in the tree.
     * @return A set of used tags.
     */
    public Set<Tag> getUsedTags()
    {
        Set<Tag> ret = new HashSet<Tag>();
        recursiveGetTags(getRoot(), ret);
        return ret;
    }
    
    private void recursiveGetTags(AreaNode root, Set<Tag> dest)
    {
        dest.addAll(root.getTags());
        for (int i = 0; i < root.getChildCount(); i++)
            recursiveGetTags(root.getChildArea(i), dest);
    }
    
}
