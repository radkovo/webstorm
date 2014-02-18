/*
 * Processor.java
 *
 * Created on 15. 12. 2013, 11:26:38 by burgetr
 */

package org.burgetr.segm;

import java.util.Vector;

import org.burgetr.segm.areas.AreaTreeOperator;
import org.burgetr.segm.areas.FindLineOperator;
import org.burgetr.segm.areas.LogicalLocalGroupingOperator;
import org.burgetr.segm.areas.ReorderOperator;
import org.burgetr.segm.areas.SuperAreaOperator;
import org.burgetr.segm.areas.LayoutSplitOperator;
import org.burgetr.segm.tagging.TagPredictor;
import org.burgetr.segm.tagging.TreeTagger;
import org.burgetr.segm.tagging.taggers.DateTagger;
import org.burgetr.segm.tagging.taggers.LocationsTagger;
import org.burgetr.segm.tagging.taggers.PersonsTagger;
import org.burgetr.segm.tagging.taggers.SessionTagger;
import org.burgetr.segm.tagging.taggers.Tagger;
import org.burgetr.segm.tagging.taggers.TimeTagger;
import org.burgetr.segm.tagging.taggers.TitleTagger;
import org.fit.cssbox.layout.Viewport;

/**
 * Implementation of the basic processes.
 * @author burgetr
 */
public class Processor
{
    private BoxTree btree;
    private AreaTree atree;
    private LogicalTree ltree;
    private FeatureAnalyzer features;
    private TreeTagger tagger;
    private TagPredictor tpred;
    private double[] weights;


    public Processor()
    {
        weights = null;
    }
    

    /**
     * Segments a rendered page and creates the box tree, area tree and logical tree.
     * @param viewport the viewport of the rendered page
     */
    public void segmentPage(Viewport viewport)
    {
        //box tree
        btree = new BoxTree(viewport);
        
        //area tree
        atree = new AreaTree(btree);
        atree.findBasicAreas();
        
        //apply the area tree operations
        Vector<AreaTreeOperator> operations = new Vector<AreaTreeOperator>();
        operations.add(new FindLineOperator(Config.CONSISTENT_LINE_STYLE, Config.MAX_LINE_EM_SPACE));
        ////operations.add(new HomogeneousLeafOperator());
        ////operations.add(new FindColumnsOperator());
        operations.add(new SuperAreaOperator(1)); //TODO misto pass limit by se hodilo nejake omezeni granularity na zaklade vlastnosti oblasti
        ////operations.add(new CollapseAreasOperator());
        operations.add(new ReorderOperator());
        
        for (AreaTreeOperator op : operations)
        {
            op.apply(atree);
        }
        
        ////atree.findColumns();
        //atree = new VipsAreaTree(btree);
        
        features = new FeatureAnalyzer(atree);
        if (weights != null)
            features.setWeights(weights);
        LayoutAnalyzer layout = new LayoutAnalyzer(atree);
        
        //tagging
        Tagger tTime = new TimeTagger();
        Tagger tDate = new DateTagger();
        Tagger tSession = new SessionTagger();
        Tagger tPersons = new PersonsTagger(1);
        Tagger tLocations = new LocationsTagger(1);
        Tagger tTitle = new TitleTagger();
        
        tagger = new TreeTagger(atree);
        tagger.addTagger(tTime);
        tagger.addTagger(tDate);
        tagger.addTagger(tSession);
        tagger.addTagger(tPersons);
        tagger.addTagger(tLocations);
        tagger.addTagger(tTitle);
        tagger.tagTree();
        //tagger.joinTaggedAreas(atree.getRoot());
        
        //tpred = new TagPredictor(null);
        
        int limit = 10;
        for (int i = 0; i < limit; i++)
        {
            //logical tree
            ltree = new LogicalTreeIndentation(atree, features, layout);
            //ltree.joinTaggedNodes(tpred);
            
            //another round of segmentation
            LogicalLocalGroupingOperator lgop = new LogicalLocalGroupingOperator(ltree, 2);
            lgop.apply(atree);
            if (!lgop.madeChanges())
                break;
            
            //split areas based on their type
            LayoutSplitOperator tsop = new LayoutSplitOperator(layout);
            tsop.apply(atree);
        }
        ltree = new LogicalTreeIndentation(atree, features, layout);
        //ltree.joinTaggedNodes(tpred);

        //refresh the tree display
        treesCompleted();
        
        //XML output
        /*PrintWriter xs = new PrintWriter(new FileOutputStream("test/tree.xml"));
        XMLOutput xo = new XMLOutput(atree, url);
        xo.dumpTo(xs);
        xs.close();*/

        //HTML output
        /*PrintWriter hts = new PrintWriter(new FileOutputStream("test/tree.html"));
        HTMLOutput hto = new HTMLOutput(atree, url);
        hto.dumpTo(hts);
        hts.close();*/
        
        //extract
        //System.out.println("EXTRACTION");
        //PrintStream exs = new PrintStream(new FileOutputStream("test/extract.html"));
        //ex = new ArticleExtractor(atree);
        //ex.extractDescriptions();
        //ex.dumpTo(exs);
        //exs.close();
        
        /*Vector<AreaNode> hdrs = ex.findHeadings();
        System.out.println("HEADINGS:");
        for (AreaNode hdr : hdrs)
        {
            System.out.println("  " + hdr.getArea().getBoxText());
        }*/
        
    }
    
    //====================================================================================
    
    protected void treesCompleted()
    {
        //this is called when the tree creation is finished
    }

    //====================================================================================
    
    public BoxTree getBoxTree()
    {
        return btree;
    }


    public AreaTree getAreaTree()
    {
        return atree;
    }


    public LogicalTree getLogicalTree()
    {
        return ltree;
    }


    public FeatureAnalyzer getFeatures()
    {
        return features;
    }


    public TreeTagger getTagger()
    {
        return tagger;
    }


    public TagPredictor getTagPredictor()
    {
        return tpred;
    }


    public double[] getWeights()
    {
        return weights;
    }


    public void setWeights(double[] weights)
    {
        this.weights = weights;
    }
    
}
