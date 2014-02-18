/**
 * TagPath.java
 *
 * Created on 12.11.2011, 17:50:49 by radek
 */
package org.burgetr.segm.tagging;

import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import javax.swing.tree.TreeNode;

import org.burgetr.segm.LogicalNode;

/**
 * A path in the tree consisting of the tagged nodes. 
 * @author radek
 */
public class TaggedNodePath extends Vector<LogicalNode>
{
    private static final long serialVersionUID = -4312314989352311083L;
    
    private Vector<Set<Tag>> tagSets;
    private TagString[] tagStrings;
	
	/**
	 * Creates a path of tagged nodes from tree root to the leaf. The resulting path
	 * may be empty when there are no tagged nodes on the path.
	 * @param leaf the leaf node where the path ends
	 */
	public TaggedNodePath(LogicalNode leaf)
	{
	    tagSets = new Vector<Set<Tag>>();
	    tagStrings = null;
		int cnt = 1;
		//Find the tagged nodes in the path to leaf
		TreeNode[] path = leaf.getPath();
		for (TreeNode tnode : path)
		{
			LogicalNode lnode = (LogicalNode) tnode;
			Set<Tag> tags = lnode.getTags();
			if (!tags.isEmpty())
			{
				add(lnode);
				tagSets.add(tags);
				cnt = cnt * tags.size();
			}
		}
		//Construct all combinations of the tags for the path
		if (!isEmpty())
		{
			tagStrings = new TagString[cnt];
			for (int i = 0; i < cnt; i++)
				tagStrings[i] = new TagString(size());
			int step = 1;
			for (int n = 0; n < size(); n++)
			{
				int t = 0;
				Set<Tag> curtags = tagSets.elementAt(n);
				int tagcnt = curtags.size();
				for (Iterator<Tag> it = curtags.iterator(); it.hasNext(); )
				{
					Tag tag = it.next();
					for (int pos = step * t; pos < cnt; pos += step * tagcnt)
						for (int i = 0; i < step; i++)
							tagStrings[pos + i].add(tag); 
					t++;
				}
				step = step * tagcnt;
			}
		}
	}
	
	@Override
    public String toString()
    {
	    StringBuilder ret = new StringBuilder();
	    ret.append('(');
	    boolean fn = true;
	    for (Set<Tag> tags : tagSets)
	    {
	        if (!fn) ret.append(", ");
	        fn = false;
	        boolean ft = true;
	        for (Tag tag : tags)
	        {
	            if (!ft) ret.append('&');
	            ft = false;
	            ret.append(tag.toString());
	        }
	    }
	    ret.append(')');
	    return ret.toString();
    }
	
    @Override
    public int hashCode()
    {
        return ((tagSets == null) ? 0 : tagSets.hashCode());
    }

    /**
     * Checks whether the node path contains exactly the same tags on the same positions.
     * @param other the node path to compare
     * @return <code>true</code> if the paths consist of the same tags
     */
    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (getClass() != obj.getClass()) return false;
        TaggedNodePath other = (TaggedNodePath) obj;
        if (tagSets == null)
        {
            if (other.tagSets != null) return false;
        }
        else if (!tagSets.equals(other.tagSets)) return false;
        return true;
    }
	
    /**
     * Checks whether the node path is a generalization of another path, i.e. it contains at least the same tags on the same positions (and possibly some more).
     * @param other the node path to compare
     * @return <code>true</code> if the paths consist of the same tags
     */
    public boolean isGeneralizationOf(TaggedNodePath other)
    {
        if (size() == other.size())
        {
            for (int i = 0; i < size(); i++)
            {
                Set<Tag> s1 = elementAt(i).getTags();
                Set<Tag> s2 = other.elementAt(i).getTags();
                if (!s1.containsAll(s2))
                    return false;
            }
            return true;
        }
        else
            return false;
    }
    
    /**
     * Checks whether a tag string is applicable for this node path.
     * @param tags the tag string to compare
     * @return <code>true</code> if all the tags in the tag string correspond to the appropriate nodes in the path.
     */
    public boolean allowsTagString(TagString tags)
    {
        if (size() == tags.size())
        {
            for (int i = 0; i < size(); i++)
            {
                Set<Tag> s = elementAt(i).getTags();
                Tag tag = tags.elementAt(i);
                if (!s.contains(tag))
                    return false;
            }
            return true;
        }
        else
            return false;
    }
    
    public boolean containsTag(String tag)
    {
    	Tag t = new Tag(tag, null);
        for (Set<Tag> ts : tagSets)
        {
            if (ts.contains(t))
                return true;
        }
        return false;
    }
    
    public int indexOf(Tag tag, int start)
    {
        for (int i = start; i < size(); i++)
        {
            if (elementAt(i).getTags().contains(tag))
                return i;
        }
        return -1;
    }
    
    public int indexOf(Tag tag)
    {
        return indexOf(tag, 0);
    }
    
	/**
	 * Obtains all the combinations of the tags on the path.
	 * @return an array of the size [combination_count][path_length] or <code>null</code> if the path is empty.
	 */
	public TagString[] getTagStrings()
	{
		return tagStrings;
	}
	
}
