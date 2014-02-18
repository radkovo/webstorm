/**
 * TreeNode.java
 *
 * Created on 11.5.2011, 11:05:58 by burgetr
 */
package org.burgetr.segm;

import javax.swing.tree.DefaultMutableTreeNode;

/**
 * This represents a generic tree node carrying object of the defined type. 
 * 
 * @author burgetr
 */
abstract public class TreeNode<T> extends DefaultMutableTreeNode
{
    private static final long serialVersionUID = -6010398708316661671L;
    
    public TreeNode(T obj)
    {
        super(obj);
    }
    
    @SuppressWarnings("unchecked")
    public T getUserObject()
    {
        return (T) super.getUserObject();
    }
    
    public void addNode(TreeNode<T> node)
    {
        super.add(node);
    }
    
    @SuppressWarnings("unchecked")
    public TreeNode<T> getParentNode()
    {
        return (TreeNode<T>) super.getParent();
    }
    
}
