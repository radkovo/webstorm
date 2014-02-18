/**
 * SuperAreaOperator.java
 *
 * Created on 24. 10. 2013, 14:40:09 by burgetr
 */
package org.burgetr.segm.areas;

import java.util.Vector;

import org.burgetr.segm.AreaNode;
import org.burgetr.segm.AreaTree;
import org.burgetr.segm.Config;

/**
 * Detects the larger visual areas and creates the artificial area nodes.
 * 
 * @author burgetr
 */
public class SuperAreaOperator implements AreaTreeOperator
{
    /** Recursion depth limit while detecting the sub-areas */
    protected int depthLimit;

    /**
     * Creates the operator.
     * @param depthLimit Recursion depth limit while detecting the sub-areas
     */
    public SuperAreaOperator(int depthLimit)
    {
        this.depthLimit = depthLimit;
    }
    
    @Override
    public void apply(AreaTree atree)
    {
        recursiveFindSuperAreas(atree.getRoot());
    }

    @Override
    public void apply(AreaTree atree, AreaNode root)
    {
        recursiveFindSuperAreas(root);
    }

    //==============================================================================

    protected GroupAnalyzer createGroupAnalyzer(AreaNode root)
    {
        return Config.createGroupAnalyzer(root);
    }
    
    //==============================================================================
    
    /**
     * Goes through all the areas in the tree and tries to join their sub-areas into single
     * areas.
     */
    private void recursiveFindSuperAreas(AreaNode root)
    {
        for (int i = 0; i < root.getChildCount(); i++)
            recursiveFindSuperAreas(root.getChildArea(i));
        findSuperAreas(root, depthLimit);
    }
    
    /**
     * Creates syntetic super areas by grouping the subareas of the given area.
     * @param the root area to be processed
     * @param passlimit the maximal number of passes while some changes occur 
     */ 
    public void findSuperAreas(AreaNode root, int passlimit)
    {
        if (root.getChildCount() > 0)
        {
            boolean changed = true;
            int pass = 0;
            root.createSeparators();
            while (changed && pass < passlimit)
            {
                changed = false;
                
                GroupAnalyzer groups = createGroupAnalyzer(root);
                
                Vector<AreaNode> chld = new Vector<AreaNode>(root.getChildAreas());
                while (chld.size() > 1) //we're not going to group a single element
                {
                    //get the super area
                    Vector<AreaNode> selected = new Vector<AreaNode>();
                    int index = root.getIndex(chld.firstElement());
                    AreaNode grp = null;
                    if (chld.firstElement().isLeaf())
                        grp = groups.findSuperArea(chld.firstElement(), selected);
                    if (selected.size() == root.getChildCount())
                    {
                        //everything grouped into one group - it makes no sense to create a new one
                        //System.out.println("(contains all)");
                        break;
                    }
                    else
                    {
                        //add a new area
                        if (selected.size() > 1)
                        {
                            root.insert(grp, index);
                            //add(grp); //add the new group to the end of children (so that it is processed again later)
                            grp.addAll(selected);
                            chld.removeAll(selected);
                            grp.createGrid();
                            findSuperAreas(grp, passlimit - 1); //in the next level, we use smaller pass limit to stop the recursion
                            changed = true;
                        }
                        else
                        {
                             //couldn't group the first element -- remove it and go on
                            chld.removeElementAt(0);
                        }
                    }
                }
                root.createGrid();
                root.removeSimpleSeparators();
                //System.out.println("Pass: " + pass + " changed: " + changed);
                pass++;
            }
        }
    }

}
