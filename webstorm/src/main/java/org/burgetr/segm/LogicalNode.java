/**
 * LogicalNode.java
 *
 * Created on 17.5.2011, 14:24:57 by burgetr
 */
package org.burgetr.segm;

import java.util.HashSet;
import java.util.Vector;
import java.util.Set;

import org.burgetr.segm.tagging.Tag;
import org.burgetr.segm.tagging.taggers.Tagger;

/**
 * A node in the logical tree.
 * 
 * @author burgetr
 */
public class LogicalNode extends TreeNode<Vector<AreaNode>>
{
    private static final long serialVersionUID = -6388940197719941526L;
    private static int next_id = 0;
    
    /** Node serial number */
    private int id;
    
    /** Content subtree for collapsed areas */
    private LogicalNode contentTree;
    
    
    public LogicalNode()
    {
    	super(new Vector<AreaNode>());
    	id = next_id++;
    	contentTree = null;
    }
    
    public LogicalNode(AreaNode node)
    {
    	super(new Vector<AreaNode>());
        id = next_id++;
        addAreaNode(node);
        contentTree = null;
    }
    
    public int getId()
    {
        return id;
    }

    public LogicalNode getContentTree()
    {
        return contentTree;
    }

    public void setContentTree(LogicalNode contentTree)
    {
        this.contentTree = contentTree;
    }

    public String toString()
    {
        if (contentTree != null)
        {
            return contentTree.getStructuredText();
        }
        else if (!getAreaNodes().isEmpty())
    	{
    		StringBuilder ret = new StringBuilder();
    		for (AreaNode node : getAreaNodes())
    		{
	    		if (node.isLeaf())
	    			ret.append(node.getText());
	    		else
	    			ret.append(node.toString());
	    		ret.append(' ');
    		}
    		return ret.toString().trim();
    	}
    	else
    		return "-empty-";
    }
    
    public String getText()
    {
		StringBuilder ret = new StringBuilder();
		for (AreaNode node : getAreaNodes())
		{
   			ret.append(node.getText());
    		ret.append(' ');
		}
		return ret.toString().trim();
    }
    
    /**
     * Obtains the text from the leaf areas only.
     * @return the leaf area text
     */
    public String getLeafText()
    {
        StringBuilder ret = new StringBuilder();
        for (AreaNode node : getAreaNodes())
        {
            if (node.isLeaf())
            {
                ret.append(node.getText());
                ret.append(' ');
            }
        }
        return ret.toString().trim();
    }
    
    /**
     * Returns the text of the whole logical subtree structured by parentheses.
     * @return
     */
    public String getStructuredText()
    {
        StringBuilder ret = new StringBuilder();
        ret.append(getText());
        if (getChildCount() > 0)
        {
            ret.append(" (");
            for (int i = 0; i < getChildCount(); i++)
            {
                if (i != 0)
                    ret.append(' ');
                ret.append(getChildNode(i).getStructuredText());
            }
            ret.append(")");
        }
        return ret.toString();
    }
    
    /**
     * Obtains all the tags assigned to the contained areas.
     * @return Set of tags
     */
    public Set<Tag> getTags()
    {
    	Set<Tag> ret = new HashSet<Tag>();
    	for (AreaNode node : getAreaNodes())
    		ret.addAll(node.getTags());
    	return ret;
    }
    
    /**
     * Checks whether the first area is marked with the given tag.
     * @param tag the tag to check
     * @return <code>true</code> if the first area of the logical node has the given tag.
     */
    public boolean hasTag(Tag tag)
    {
        return getFirstAreaNode().hasTag(tag);
    }
    
    /**
     * Checks whether any contained area or its subareas are marked with the given tag.
     * @param tag the tag to check
     * @return <code>true</code> there is any area marked with the given tag in this logical node.
     */
    public boolean containsTag(Tag tag)
    {
        for (AreaNode node : getAreaNodes())
            if (node.containsTag(tag))
                return true;
        return false;
    }
    
    public void addAreaNode(AreaNode node)
    {
    	getUserObject().add(node);
    }
    
    public Vector<AreaNode> getAreaNodes()
    {
        return getUserObject();
    }
    
    public AreaNode getFirstAreaNode()
    {
        return getUserObject().firstElement();
    }
    
    public AreaNode getLastAreaNode()
    {
        return getUserObject().lastElement();
    }
    
    public void addNode(LogicalNode node)
    {
        super.addNode(node);
    }
    
    public LogicalNode getParentNode()
    {
        return (LogicalNode) super.getParentNode();
    }
    
    public LogicalNode getChildNode(int index)
    {
        return (LogicalNode) super.getChildAt(index);
    }
    
    public LogicalNode findArea(AreaNode area)
    {
        if (getAreaNodes().contains(area))
            return this; //in our area nodes
        else if (getContentTree() != null && getContentTree().findArea(area) != null)
            return this; //in our content tree
        else //in the subtree
        {
            LogicalNode ret = null;
            for (int i = 0; i < getChildCount(); i++)
            {
                ret = getChildNode(i).findArea(area);
                if (ret != null)
                    return ret;
            }
            return null;
        }
    }
    
    public Vector<String> extract(Tag tag)
    {
        if (getFirstAreaNode().isLeaf())
        {
            //at the leaf area level - use all the text
            Tagger tagger = tag.getSource();
            if (tagger != null)
                return tagger.extract(getText());
            else
                return null;
        }
        else
        {
            //we are not at the leaf level - use only the most important leaf area
            return extractBestLeaf(tag);
        }
    }
    
    /**
     * Finds the most visually important leaf area and uses it for extracting the data.
     */
    public Vector<String> extractBestLeaf(Tag tag)
    {
        //find the most important leaf of the areas
        AreaNode best = null;
        double bestMarkedness = -1;
        for (AreaNode node : getAreaNodes())
        {
            AreaNode leaf = node.getMostImportantLeaf(tag);
            if (leaf != null && leaf.getMarkedness() > bestMarkedness)
            {
                best = leaf;
                bestMarkedness = leaf.getMarkedness();
            }
        }
        //extract from the leaf
        if (best != null)
        {
            Tagger tagger = tag.getSource();
            if (tagger != null)
                return tagger.extract(best.getText());
        }
        //no suitable leaf found -- return an empty vector
        return new Vector<String>();
    }
    
}
