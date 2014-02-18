/**
 * 
 */
package org.burgetr.segm;

import javax.swing.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

import org.fit.cssbox.css.CSSNorm;
import org.fit.cssbox.css.DOMAnalyzer;
import org.fit.cssbox.io.DOMSource;
import org.fit.cssbox.io.DefaultDOMSource;
import org.fit.cssbox.io.DefaultDocumentSource;
import org.fit.cssbox.io.DocumentSource;
import org.fit.cssbox.layout.BrowserCanvas;
import org.fit.cssbox.layout.BrowserConfig;
import org.fit.cssbox.pdf.PdfBrowserCanvas;
import org.apache.pdfbox.exceptions.CryptographyException;
import org.apache.pdfbox.exceptions.InvalidPasswordException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.burgetr.segm.tagging.Tag;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.*;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;

import java.awt.GridBagConstraints;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import java.awt.GridLayout;

import javax.swing.JTree;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.GridBagLayout;

import javax.swing.JList;
import javax.swing.JToggleButton;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.FlowLayout;

import javax.swing.JTextArea;
import javax.swing.JTable;

import java.awt.Insets;
import java.awt.Font;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * @author burgetr
 *
 */
public class BlockBrowser
{
    public static BlockBrowser browser;

    private BrowserConfig config;
    private Processor proc;
    /*private BoxTree btree;
    private AreaTree atree;
    private LogicalTree ltree;
    private FeatureAnalyzer features;
    private ArticleExtractor ex;
    private TreeTagger tagger;
    private SingleClassStyleAnalyzer ssa;
    private AreaTreeStyleAnalyzer msa;
    private ExtractedStyleAnalyzer esa;
    private TagPredictor tpred;
    private Vector<EvalData> evalData;*/
    private File saveDir = null;
    private URL currentUrl = null;
    private String charset = "utf-8";
    private boolean dispFinished = false;
    private boolean areasync = true;
    private boolean logsync = true;

    private Date startDate, layoutDate, styleDate, parseDate, treeDate; //for performance measuring

    private JFrame mainWindow = null;  //  @jve:decl-index=0:visual-constraint="-239,28"
    private JPanel mainPanel = null;
    private JPanel urlPanel = null;
    private JPanel contentPanel = null;
    private JPanel structurePanel = null;
    private JPanel statusPanel = null;
    private JTextField statusText = null;
    private JLabel jLabel = null;
    private JTextField urlText = null;
    private JButton okButton = null;
    private JTabbedPane sidebarPane = null;
    private JPanel boxTreePanel = null;
    private JScrollPane boxTreeScroll = null;
    private JTree boxTree = null;
    private JScrollPane contentScroll = null;
    private JPanel contentCanvas = null;
    private JSplitPane mainSplitter = null;
    private JToolBar showToolBar = null;
    private JButton redrawButton = null;
	private JPanel areaTreePanel = null;
	private JScrollPane areaTreeScroll = null;
	private JTree areaTree = null;
	private JPanel sepListPanel = null;
	private JScrollPane sepScroll = null;
	private JList sepList = null;
    private JToggleButton lookupButton = null;
    private JToggleButton extractButton = null;
    private JToggleButton boxLookupButton = null;
    private JButton showSepButton = null;
    private JButton showBoxButton = null;
    private JButton showAreaButton = null;
    private JToolBar lookupToolBar = null;
    private JPanel toolPanel = null;
    private JToolBar fileToolBar = null;
    private JButton saveButton = null;
    private JButton gridButton = null;
    private JPanel jPanel = null;
    private JScrollPane logicalTreeScroll = null;
    private JTree logicalTree = null;
    private JTextArea consoleText = null;
    private JButton refreshButton = null;
    private JSplitPane infoSplitter = null;
    private JPanel objectInfoPanel = null;
    private JScrollPane objectInfoScroll = null;
    private JTable infoTable = null;
    private JButton showArtAreaButton = null;
    private JButton showColumnsButton = null;
    private JScrollPane featureScroll = null;
    private JTable featureTable = null;
    private JButton saveLogicalButton = null;
    private JTextField markednessText;
    private JButton tagsButton;
    private JPanel pathsPanel;
    private JScrollPane pathListScroll;
    private JScrollPane extractionScroll;
    private JList pathList;
    private JTable extractionTable;
    private JToolBar treeCompToolBar;
    private JButton treeCompButton;
    private JFrame treeCompWindow;
    private JToggleButton sepLookupButton;
    private JScrollPane probabilityScroll;
    private JTable probTable;
    private JButton evaluationButton;
    private JButton saveRDFButton;

    public BlockBrowser()
    {
        config = new BrowserConfig();
        saveDir = new File("/home/burgetr/local/rdf");
    }
    
    //===========================================================================
    
    public void setLocation(String url)
    {
        urlText.setText(url);
        displayURL(url);
    }
    
    public String getLocation()
    {
        return currentUrl.toString();
    }

    public void setLoadImages(boolean b)
    {
        config.setLoadImages(b);
    }
    
    public boolean getLoadImages()
    {
        return config.getLoadImages();
    }
    
    public String getCharset()
    {
        return charset;
    }

    public void setCharset(String charset)
    {
        this.charset = charset;
    }

    public JPanel getToolBar()
    {
        return toolPanel;
    }
    
    /**
     * Print a text string on the console.
     */
    public void consolePrint(String s)
    {
        consoleText.setCaretPosition(consoleText.getText().length());
        consoleText.insert(s, consoleText.getCaretPosition());
    }
    
    public BoxTree getBtree()
    {
        return proc.getBoxTree();
    }
    
    public AreaTree getAtree()
    {
        return proc.getAreaTree();
    }
    
    /**
     * Refresh the trees.
     */
    public void refresh()
    {
        boxTree.setModel(new DefaultTreeModel(proc.getBoxTree().getRoot()));
        areaTree.setModel(new DefaultTreeModel(proc.getAreaTree().getRoot()));
        logicalTree.setModel(new DefaultTreeModel(proc.getLogicalTree().getRoot()));
    }
    
    //=============================================================================================================
    
    public void displayURL(String urlstring)
    {
        dispFinished = false;
        saveButton.setEnabled(false);
        saveLogicalButton.setEnabled(false);
        saveRDFButton.setEnabled(false);
        treeCompButton.setEnabled(false);
        if (treeCompWindow != null)
        {
            treeCompWindow.setVisible(false);
            treeCompWindow.dispose();
            treeCompWindow = null;
        }
        
        try {
            if (!urlstring.startsWith("http:") &&
                !urlstring.startsWith("ftp:") &&
                !urlstring.startsWith("file:"))
                    urlstring = "http://" + urlstring;
            
            DocumentSource src = new DefaultDocumentSource(urlstring);
            currentUrl = src.getURL();
            contentCanvas = createContentCanvas(src);
            
            contentCanvas.addMouseListener(new MouseListener() {
                public void mouseClicked(MouseEvent e)
                {
                    System.out.println("Click: " + e.getX() + ":" + e.getY());
                    canvasClick(e.getX(), e.getY());
                }
                public void mousePressed(MouseEvent e) { }
                public void mouseReleased(MouseEvent e) { }
                public void mouseEntered(MouseEvent e) { }
                public void mouseExited(MouseEvent e) 
                {
                    statusText.setText("");
                }
            });
            contentCanvas.addMouseMotionListener(new MouseMotionListener() {
                public void mouseDragged(MouseEvent e) { }
                public void mouseMoved(MouseEvent e) 
                { 
                    String s = "Absolute: " + e.getX() + ":" + e.getY();
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) areaTree.getLastSelectedPathComponent();
                    if (node != null && node instanceof AreaNode)
                    {
                        AreaNode area = (AreaNode) node;
                        int rx = e.getX() - area.getX();
                        int ry = e.getY() - area.getY();
                        s += "  Relative: " + rx + ":" + ry;
                        if (area.getArea().contains(e.getX(), e.getY()))
                        {
                            AreaGrid grid = area.getGrid();
                            if (grid != null)
                            {
                                int gx = grid.findCellX(e.getX());
                                int gy = grid.findCellY(e.getY());
                                s += "  Grid: " + gx + ":" + gy;
                            }
                        }
                    }
                    statusText.setText(s);
                }
            });
            contentScroll.setViewportView(contentCanvas);
            layoutDate = new Date();
            
            proc = new Processor() {
                protected void treesCompleted()
                {
                    refresh();
                }
            };
            proc.segmentPage(((BrowserCanvas) contentCanvas).getViewport());
            treeDate = new Date();

            /*AreaNode node = atree.getAreaByName("731:");
            if (node != null)
            {
                TreePath select = new TreePath(node.getPath());
                areaTree.setSelectionPath(select);
                areaTree.expandPath(select);
            }*/
            
            //=============================================================================
            
            System.out.println();
            System.out.println("Statistics");
            System.out.println("----------");
            System.out.println("Load&parse: "+ (parseDate.getTime() - startDate.getTime()) + " ms");
            System.out.println("Compute styles: "+ (styleDate.getTime() - parseDate.getTime()) + " ms");
            System.out.println("Layout: "+ (layoutDate.getTime() - styleDate.getTime()) + " ms");
            System.out.println("Segmentation: "+ (treeDate.getTime() - layoutDate.getTime()) + " ms");
            
            dispFinished = true;
            saveButton.setEnabled(true);
            saveLogicalButton.setEnabled(true);
            saveRDFButton.setEnabled(true);
            treeCompButton.setEnabled(true);
            
        } catch (Exception e) {
            System.err.println("*** Error: "+e.getMessage());
            e.printStackTrace();
        }
    }
    
    /** Creates the appropriate canvas based on the file type */
    private BrowserCanvas createContentCanvas(DocumentSource src) throws IOException, SAXException
    {
        InputStream is = src.getInputStream();
        String mime = src.getContentType();
        if (mime == null)
            mime = "text/html";
        int p = mime.indexOf(';');
        if (p != -1)
            mime = mime.substring(0, p).trim();
        System.out.println("File type: " + mime);
        
        startDate = new Date();
        
        if (mime.equals("application/pdf"))
        {
            PDDocument doc = loadPdf(is);
            parseDate = new Date();
            styleDate = new Date();
            return new PdfBrowserCanvas(doc, null, contentScroll.getSize(), src.getURL());
        }
        else
        {
            DOMSource parser = new DefaultDOMSource(src);
            Document doc = parser.parse();
            parseDate = new Date();
            
            DOMAnalyzer da = new DOMAnalyzer(doc, src.getURL());
            da.attributesToStyles();
            da.addStyleSheet(null, CSSNorm.stdStyleSheet(), DOMAnalyzer.Origin.AGENT);
            da.addStyleSheet(null, CSSNorm.userStyleSheet(), DOMAnalyzer.Origin.AGENT);
            da.getStyleSheets();
            styleDate = new Date();
            
            BrowserCanvas ret = new BrowserCanvas(da.getRoot(), da, src.getURL());
            ret.getConfig().setLoadImages(false);
            ret.getConfig().setLoadBackgroundImages(false);
            ret.createLayout(contentScroll.getSize());
            return ret;
        }
    }
    
    private PDDocument loadPdf(InputStream is) throws IOException
    {
        PDDocument document = null;
        document = PDDocument.load(is);
        if( document.isEncrypted() )
        {
            try
            {
                document.decrypt("");
            }
            catch( InvalidPasswordException e )
            {
                System.err.println( "Error: Document is encrypted with a password." );
                System.exit( 1 );
            }
            catch( CryptographyException e )
            {
                System.err.println( "Error: Document is encrypted with a password." );
                System.exit( 1 );
            }
        }
        return document;
    }
    
    
    /** This is called when the browser canvas is clicked */
    private void canvasClick(int x, int y)
    {
        if (lookupButton.isSelected())
        {
            AreaNode node = proc.getAreaTree().getAreaAt(x, y);
            if (node != null)
            {
                showAreaInTree(node);
                showAreaInLogicalTree(node);
            }
            //lookupButton.setSelected(false);
        }
        if (boxLookupButton.isSelected())
        {
            BoxNode node = proc.getBoxTree().getBoxAt(x, y);
            if (node != null)
                showBoxInTree(node);
            //boxLookupButton.setSelected(false);
        }
        if (sepLookupButton.isSelected())
        {
            showSeparatorAt(x, y);
        }
        if (extractButton.isSelected())
        {
        }
    }
    
    private void showBoxInTree(BoxNode node)
    {
        TreePath select = new TreePath(node.getPath());
        boxTree.setSelectionPath(select);
        boxTree.expandPath(select);
    }
    
    private void showAreaInTree(AreaNode node)
    {
        TreePath select = new TreePath(node.getPath());
        areaTree.setSelectionPath(select);
        //areaTree.expandPath(select);
        areaTree.scrollPathToVisible(new TreePath(node.getPath()));
    }
    
    private void showAreaInLogicalTree(AreaNode node)
    {
        LogicalNode lnode = proc.getLogicalTree().findArea(node);
        if (lnode != null)
        {
            TreePath select = new TreePath(lnode.getPath());
            logicalTree.setSelectionPath(select);
            //logicalTree.expandPath(select);
            logicalTree.scrollPathToVisible(new TreePath(lnode.getPath()));
        }
    }
    
    private void displayAreaInfo(AreaNode area)
    {
        Vector<String> cols = infoTableData("Property", "Value");
        
        Vector<Vector <String>> vals = new Vector<Vector <String>>();
        vals.add(infoTableData("Layout", area.getLayoutType().toString()));
        vals.add(infoTableData("GP", area.getGridPosition().toString()));
        vals.add(infoTableData("Tags", tagString(area.getTags())));
        vals.add(infoTableData("Total probs", tagProbabilityString(proc.getTagPredictor() != null ? proc.getTagPredictor().getTagProbabilities(area) : null)));
        vals.add(infoTableData("Importance", String.valueOf(area.getImportance())));
        vals.add(infoTableData("Separated", (area.explicitelySeparated()) ? "true" : "false"));
        vals.add(infoTableData("Atomic", (area.isAtomic()) ? "true" : "false"));
        vals.add(infoTableData("Indent scale", (area.getGrid() != null) ? (area.getGrid().getMinIndent() + " - " + area.getGrid().getMaxIndent()) : "-"));
        vals.add(infoTableData("Indent value", String.valueOf(proc.getFeatures().getIndentation(area))));
        vals.add(infoTableData("Centered", (area.isCentered()) ? "true" : "false"));
        vals.add(infoTableData("Coherent", (area.isCoherent()) ? "true" : "false"));
        vals.add(infoTableData("Parent perc.", String.valueOf(area.getParentPercentage())));
        
        Area a = area.getArea();
        if (a != null)
        {
            vals.add(infoTableData("Name", a.getName()));
            vals.add(infoTableData("Bounds", a.getBounds().toString()));
            vals.add(infoTableData("Content", (a.getContentBounds() == null) ? "" : a.getContentBounds().toString()));
            vals.add(infoTableData("Level", String.valueOf(a.getLevel())));
            vals.add(infoTableData("Borders", borderString(a)));
            vals.add(infoTableData("Bg separated", (a.isBackgroundSeparated()) ? "true" : "false"));
            vals.add(infoTableData("Is hor. sep.", (a.isHorizontalSeparator()) ? "true" : "false"));
            vals.add(infoTableData("Is vert. sep.", (a.isVerticalSeparator()) ? "true" : "false"));
            vals.add(infoTableData("Avg. fsize", String.valueOf(a.getAverageFontSize())));
            vals.add(infoTableData("Avg. fweight", String.valueOf(a.getAverageFontWeight())));
            vals.add(infoTableData("Avg. fstyle", String.valueOf(a.getAverageFontStyle())));
            vals.add(infoTableData("Decl. fsize", String.valueOf(a.getDeclaredFontSize())));
            vals.add(infoTableData("Luminosity", String.valueOf(a.getAverageColorLuminosity())));
            //vals.add(infoTableData("Start color", colorString(a.getBoxes().firstElement().getStartColor())));
            vals.add(infoTableData("Bg color", colorString(a.getBackgroundColor())));
        }
        
        vals.add(infoTableData("Fg color", colorString(area.getFirstNestedBox().getBox().getVisualContext().getColor())));
        
        markednessText.setText(String.format("%.2f", proc.getFeatures().getMarkedness(area)));

        //classification result
        displayProbabilityTable(area);
        
        Vector<Vector <String>> fvals = new Vector<Vector <String>>();
        FeatureVector f = proc.getFeatures().getFeatureVector(area);
        if (f != null)
        {
            Method[] methods = f.getClass().getMethods();
            for (Method m : methods)
            {
                try
                {
                    if (m.getName().startsWith("get") && !m.equals("getClass"))
                    {
                        Object ret = m.invoke(f, (Object []) null);
                        if (ret != null)
                            fvals.add(infoTableData(m.getName().substring(3), ret.toString()));
                    }
                    if (m.getName().startsWith("is"))
                    {
                        Object ret = m.invoke(f, (Object []) null);
                        if (ret != null)
                            fvals.add(infoTableData(m.getName().substring(2), ret.toString()));
                    }
                } catch (Exception e) {}
            }
        }
        
        DefaultTableModel tab = new DefaultTableModel(vals, cols);
        infoTable.setModel(tab);
        DefaultTableModel ftab = new DefaultTableModel(fvals, cols);
        featureTable.setModel(ftab);
    }
    
    private String borderString(Area a)
    {
        String bs = "";
        if (a.hasTopBorder()) bs += "^";
        if (a.hasLeftBorder()) bs += "<";
        if (a.hasRightBorder()) bs += ">";
        if (a.hasBottomBorder()) bs += "_";
        return bs;
    }
    
    private String colorString(java.awt.Color color)
    {
        if (color == null)
            return "- transparent -";
        else
            return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }
    
    private String tagString(Set<Tag> tags)
    {
        String ret = "";
        for (Tag tag : tags)
            ret += tag + " ";
        return ret;
    }
    
    private String tagProbabilityString(Map<Tag, Double> map)
    {
        String ret = "";
        if (map != null)
        {
            for (Map.Entry<Tag, Double> entry : map.entrySet())
            {
                if (entry.getValue() > Config.TAG_PROBABILITY_THRESHOLD)
                    ret += entry.getKey() + " (" + String.format("%1.2f", entry.getValue()) + ") "; 
            }
        }
        return ret;
    }
    
    private Vector<String> infoTableData(String prop, String value)
    {
        Vector<String> cols = new Vector<String>(2);
        cols.add(prop);
        cols.add(value);
        return cols;
    }
    
    private void displayProbabilityTable(AreaNode area)
    {
        List<Tag> tags = proc.getTagger().getAllTags();
        Vector<String> cols = new Vector<String>();
        cols.add("Source");
        for (Tag tag : tags)
            cols.add(tag.toString());
        
        Vector<Vector <String>> vals = new Vector<Vector <String>>();
        //tags
        Vector<String> tprob = new Vector<String>();
        tprob.add("Tag");
        for (Tag tag : tags)
        {
            double p = 0.0;
            if (area.hasTag(tag))
                p = tag.getSource().getRelevance();
            tprob.add(String.format("%1.2f", p));
        }
        vals.add(tprob);
        //classifiers
        if (proc.getTagPredictor() != null)
            vals.add(getProbTableLine("total", tags, proc.getTagPredictor().getTagProbabilities(area)));
                
        probTable.setModel(new DefaultTableModel(vals, cols));
    }
    
    private Vector<String> getProbTableLine(String title, List<Tag> tags, Map<Tag, Double> data)
    {
        Vector<String> ret = new Vector<String>();
        ret.add(title);
        for (Tag tag : tags)
        {
            if (data.containsKey(tag))
                ret.add(String.format("%1.2f", data.get(tag)));
            else
                ret.add("");
        }
        return ret;
    }
    
    /** Displays all separators from the selected area */
    private void showSeparators()
    {
        ListModel ml = sepList.getModel();
        for (int i = 0; i < ml.getSize(); i++)
        {
            Separator sep = (Separator) ml.getElementAt(i);
            sep.drawExtent((BrowserCanvas) contentCanvas);
        }
        contentCanvas.repaint();
    }
    
    private void showSeparatorAt(int x, int y)
    {
        ListModel ml = sepList.getModel();
        for (int i = 0; i < ml.getSize(); i++)
        {
            Separator sep = (Separator) ml.getElementAt(i);
            if (sep.contains(x, y))
            {
                sepList.setSelectedValue(sep, true);
            }
        }
        //contentCanvas.repaint();
    }
    
    private void showArea(AreaNode anode)
    {
        Area sel = anode.getArea();
        sel.drawExtent((BrowserCanvas) contentCanvas);
        //anode.drawGrid((BrowserCanvas) contentCanvas);
        contentCanvas.repaint();
        
        //show the info table
        displayAreaInfo(anode);
        
        //show the separator list
        SeparatorSet sset = Config.createSeparators(anode);
        DefaultListModel ml = new DefaultListModel();
        for (Separator sep : sset.getHorizontal())
            ml.addElement(sep);
        for (Separator sep : sset.getVertical())
            ml.addElement(sep);
        for (Separator sep : sset.getBoxsep())
            ml.addElement(sep);
        sepList.setModel(ml);
        
        //debug joining
        AreaNode p = anode.getParentArea();
        if (p != null)
            p.debugAreas(anode);
    }

    private void showNode(LogicalNode lnode)
    {
    	boolean first = true;
    	for (AreaNode node : lnode.getAreaNodes())
    	{
    		if (first)
    			showArea(node);
    		else
    		{
    	        Area sel = node.getArea();
    	        sel.drawExtent((BrowserCanvas) contentCanvas);
    		}
    		first = false;
    	}
        contentCanvas.repaint();
    }
    
    @SuppressWarnings("unchecked")
    public void showAreas(AreaNode node, String name)
    {
        Enumeration<AreaNode> en = node.postorderEnumeration();
        while (en.hasMoreElements())
        {
            AreaNode child = en.nextElement();
            if (child.getArea() != null && (name == null || child.getArea().toString().contains(name)))
                child.getArea().drawExtent((BrowserCanvas) contentCanvas);
        }
        contentCanvas.repaint();
    }
    
    @SuppressWarnings("unchecked")
    public void colorizeTags(AreaNode node)
    {
        Enumeration<AreaNode> en = node.postorderEnumeration();
        while (en.hasMoreElements())
        {
            AreaNode child = en.nextElement();
            if (child.getArea() != null)
                child.getArea().colorizeByTags((BrowserCanvas) contentCanvas, child.getTags());
        }
        contentCanvas.repaint();
    }
    
    public BrowserCanvas getBrowserCanvas()
    {
        return (BrowserCanvas) contentCanvas;
    }
    
    //===========================================================================
    
    /**
     * This method initializes jFrame	
     * 	
     * @return javax.swing.JFrame	
     */
    private JFrame getMainWindow()
    {
        if (mainWindow == null)
        {
            mainWindow = new JFrame();
            mainWindow.setTitle("Visual Block Browser");
            mainWindow.setVisible(true);
            mainWindow.setBounds(new Rectangle(0, 0, 1489, 256));
            mainWindow.setMinimumSize(new Dimension(1200, 256));
            mainWindow.setContentPane(getMainPanel());
            mainWindow.addWindowListener(new java.awt.event.WindowAdapter()
            {
                public void windowClosing(java.awt.event.WindowEvent e)
                {
                    mainWindow.setVisible(false);
                    System.exit(0);
                }
            });
        }
        return mainWindow;
    }

    /**
     * This method initializes jContentPane	
     * 	
     * @return javax.swing.JPanel	
     */
    private JPanel getMainPanel()
    {
        if (mainPanel == null)
        {
            GridBagConstraints gridBagConstraints2 = new GridBagConstraints();
            gridBagConstraints2.gridy = -1;
            gridBagConstraints2.anchor = GridBagConstraints.WEST;
            gridBagConstraints2.gridx = -1;
            GridBagConstraints gridBagConstraints11 = new GridBagConstraints();
            gridBagConstraints11.fill = java.awt.GridBagConstraints.BOTH;
            gridBagConstraints11.weighty = 1.0;
            gridBagConstraints11.gridx = 0;
            gridBagConstraints11.weightx = 1.0;
            GridBagConstraints gridBagConstraints3 = new GridBagConstraints();
            gridBagConstraints3.gridx = 0;
            gridBagConstraints3.weightx = 1.0;
            gridBagConstraints3.fill = java.awt.GridBagConstraints.HORIZONTAL;
            gridBagConstraints3.gridwidth = 1;
            gridBagConstraints3.gridy = 4;
            GridBagConstraints gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
            gridBagConstraints.weightx = 1.0;
            gridBagConstraints.gridwidth = 1;
            gridBagConstraints.gridy = 1;
            mainPanel = new JPanel();
            mainPanel.setLayout(new GridBagLayout());
            mainPanel.add(getToolPanel(), gridBagConstraints2);
            mainPanel.add(getUrlPanel(), gridBagConstraints);
            mainPanel.add(getMainSplitter(), gridBagConstraints11);
            mainPanel.add(getStatusPanel(), gridBagConstraints3);
        }
        return mainPanel;
    }

    /**
     * This method initializes jPanel	
     * 	
     * @return javax.swing.JPanel	
     */
    private JPanel getUrlPanel()
    {
        if (urlPanel == null)
        {
            GridBagConstraints gridBagConstraints1 = new GridBagConstraints();
            gridBagConstraints1.fill = java.awt.GridBagConstraints.HORIZONTAL;
            gridBagConstraints1.gridy = 0;
            gridBagConstraints1.weightx = 1.0;
            gridBagConstraints1.gridx = 1;
            GridBagConstraints gridBagConstraints7 = new GridBagConstraints();
            gridBagConstraints7.gridx = 3;
            gridBagConstraints7.insets = new java.awt.Insets(4,0,5,7);
            gridBagConstraints7.gridy = 1;
            GridBagConstraints gridBagConstraints6 = new GridBagConstraints();
            gridBagConstraints6.fill = java.awt.GridBagConstraints.HORIZONTAL;
            gridBagConstraints6.gridy = 1;
            gridBagConstraints6.weightx = 1.0;
            gridBagConstraints6.insets = new java.awt.Insets(0,5,0,5);
            GridBagConstraints gridBagConstraints5 = new GridBagConstraints();
            gridBagConstraints5.gridy = 1;
            gridBagConstraints5.anchor = java.awt.GridBagConstraints.CENTER;
            gridBagConstraints5.insets = new java.awt.Insets(0,6,0,0);
            gridBagConstraints5.gridx = 0;
            jLabel = new JLabel();
            jLabel.setText("Location :");
            urlPanel = new JPanel();
            urlPanel.setLayout(new GridBagLayout());
            urlPanel.add(jLabel, gridBagConstraints5);
            urlPanel.add(getUrlText(), gridBagConstraints6);
            urlPanel.add(getOkButton(), gridBagConstraints7);
        }
        return urlPanel;
    }

    /**
     * This method initializes jPanel	
     * 	
     * @return javax.swing.JPanel	
     */
    private JPanel getContentPanel()
    {
        if (contentPanel == null)
        {
            GridLayout gridLayout1 = new GridLayout();
            gridLayout1.setRows(1);
            contentPanel = new JPanel();
            contentPanel.setLayout(gridLayout1);
            contentPanel.add(getContentScroll(), null);
        }
        return contentPanel;
    }

    /**
     * This method initializes jPanel1	
     * 	
     * @return javax.swing.JPanel	
     */
    private JPanel getStructurePanel()
    {
        if (structurePanel == null)
        {
            GridLayout gridLayout = new GridLayout();
            gridLayout.setRows(1);
            structurePanel = new JPanel();
            structurePanel.setPreferredSize(new Dimension(200, 408));
            structurePanel.setLayout(gridLayout);
            structurePanel.add(getSidebarPane(), null);
        }
        return structurePanel;
    }

    /**
     * This method initializes jPanel2	
     * 	
     * @return javax.swing.JPanel	
     */
    private JPanel getStatusPanel()
    {
        if (statusPanel == null)
        {
            GridBagConstraints gridBagConstraints4 = new GridBagConstraints();
            gridBagConstraints4.gridx = 0;
            gridBagConstraints4.fill = java.awt.GridBagConstraints.HORIZONTAL;
            gridBagConstraints4.weightx = 1.0;
            gridBagConstraints4.insets = new java.awt.Insets(0,7,0,0);
            gridBagConstraints4.gridy = 2;
            statusPanel = new JPanel();
            statusPanel.setLayout(new GridBagLayout());
            statusPanel.add(getStatusText(), gridBagConstraints4);
        }
        return statusPanel;
    }

    /**
     * This method initializes jTextField	
     * 	
     * @return javax.swing.JTextField	
     */
    private JTextField getStatusText()
    {
        if (statusText == null)
        {
            statusText = new JTextField();
            statusText.setEditable(false);
            statusText.setText("Browser ready.");
        }
        return statusText;
    }

    /**
     * This method initializes jTextField	
     * 	
     * @return javax.swing.JTextField	
     */
    private JTextField getUrlText()
    {
        if (urlText == null)
        {
            urlText = new JTextField();
            urlText.addActionListener(new java.awt.event.ActionListener()
            {
                public void actionPerformed(java.awt.event.ActionEvent e)
                {
                    displayURL(urlText.getText());
                }
            });
        }
        return urlText;
    }

    /**
     * This method initializes jButton	
     * 	
     * @return javax.swing.JButton	
     */
    private JButton getOkButton()
    {
        if (okButton == null)
        {
            okButton = new JButton();
            okButton.setText("Go!");
            okButton.addActionListener(new java.awt.event.ActionListener()
            {
                public void actionPerformed(java.awt.event.ActionEvent e)
                {
                    displayURL(urlText.getText());
                }
            });
        }
        return okButton;
    }

    /**
     * This method initializes jTabbedPane	
     * 	
     * @return javax.swing.JTabbedPane	
     */
    private JTabbedPane getSidebarPane()
    {
        if (sidebarPane == null)
        {
            sidebarPane = new JTabbedPane();
            sidebarPane.addTab("Area tree", null, getJPanel(), null);
            sidebarPane.addTab("Logical tree", null, getJPanel4(), null);
            sidebarPane.addTab("Box tree", null, getBoxTreePanel(), null);
            sidebarPane.addTab("Separators", null, getJPanel2(), null);
            sidebarPane.addTab("Paths", null, getPathsPanel(), null);
        }
        return sidebarPane;
    }

    /**
     * This method initializes jPanel	
     * 	
     * @return javax.swing.JPanel	
     */
    private JPanel getBoxTreePanel()
    {
        if (boxTreePanel == null)
        {
            GridLayout gridLayout2 = new GridLayout();
            gridLayout2.setRows(1);
            boxTreePanel = new JPanel();
            boxTreePanel.setLayout(gridLayout2);
            boxTreePanel.add(getBoxTreeScroll(), null);
        }
        return boxTreePanel;
    }

    /**
     * This method initializes jScrollPane	
     * 	
     * @return javax.swing.JScrollPane	
     */
    private JScrollPane getBoxTreeScroll()
    {
        if (boxTreeScroll == null)
        {
            boxTreeScroll = new JScrollPane();
            boxTreeScroll.setViewportView(getBoxTree());
        }
        return boxTreeScroll;
    }

    /**
     * This method initializes jTree	
     * 	
     * @return javax.swing.JTree	
     */
    private JTree getBoxTree()
    {
        if (boxTree == null)
        {
            boxTree = new JTree();
            boxTree.addTreeSelectionListener(new javax.swing.event.TreeSelectionListener()
            {
                public void valueChanged(javax.swing.event.TreeSelectionEvent e)
                {
                    BoxNode node = (BoxNode) boxTree.getLastSelectedPathComponent();
                    if (node != null && node.getBox() != null)
                    {
                        System.out.println("Bg:"+node.getEfficientBackground());
                        System.out.println("Fg:"+node.getEfficientColor());
	                    node.drawExtent((BrowserCanvas) contentCanvas);
                        contentCanvas.repaint();
                        boxTree.scrollPathToVisible(new TreePath(node.getPath()));
                    }
                }
            });
        }
        return boxTree;
    }

    /**
     * This method initializes jScrollPane	
     * 	
     * @return javax.swing.JScrollPane	
     */
    private JScrollPane getContentScroll()
    {
        if (contentScroll == null)
        {
            contentScroll = new JScrollPane();
            contentScroll.setViewportView(getContentCanvas());
            contentScroll.getVerticalScrollBar().setUnitIncrement(10);
            contentScroll.addComponentListener(new java.awt.event.ComponentAdapter()
            {
                public void componentResized(java.awt.event.ComponentEvent e)
                {
                    if (contentCanvas != null && contentCanvas instanceof BrowserCanvas)
                    {
                        ((BrowserCanvas) contentCanvas).createLayout(contentScroll.getSize());
                        contentScroll.repaint();
                        BoxTree btree = new BoxTree(((BrowserCanvas) contentCanvas).getViewport());
                        boxTree.setModel(new DefaultTreeModel(btree.getRoot()));
                    }
                }
            });
        }
        return contentScroll;
    }

    /**
     * This method initializes jPanel   
     *  
     * @return javax.swing.JPanel   
     */
    private JPanel getContentCanvas()
    {
        if (contentCanvas == null)
        {
            contentCanvas = new JPanel();
            //contentCanvas.add(getInfoSplitter(), null);
        }
        return contentCanvas;
    }
    
    /**
     * This method initializes jSplitPane	
     * 	
     * @return javax.swing.JSplitPane	
     */
    private JSplitPane getMainSplitter()
    {
        if (mainSplitter == null)
        {
            mainSplitter = new JSplitPane();
            mainSplitter.setDividerLocation(250);
            mainSplitter.setLeftComponent(getStructurePanel());
            mainSplitter.setRightComponent(getInfoSplitter());
        }
        return mainSplitter;
    }

    /**
     * This method initializes jToolBar 
     *  
     * @return javax.swing.JToolBar 
     */
    private JToolBar getShowToolBar()
    {
        if (showToolBar == null)
        {
            showToolBar = new JToolBar();
            showToolBar.add(getRedrawButton());
            showToolBar.add(getShowBoxButton());
            showToolBar.add(getShowAreaButton());
            showToolBar.add(getShowArtAreaButton());
            showToolBar.add(getShowColumnsButton());
            showToolBar.add(getShowSepButton());
            showToolBar.add(getGridButton());
            showToolBar.add(getTagsButton());
        }
        return showToolBar;
    }
    
    
    /**
     * This method initializes jButton	
     * 	
     * @return javax.swing.JButton	
     */
    private JButton getRedrawButton()
    {
        if (redrawButton == null)
        {
            redrawButton = new JButton();
            redrawButton.setText("Clear");
            redrawButton.setMnemonic(KeyEvent.VK_UNDEFINED);
            redrawButton.addActionListener(new java.awt.event.ActionListener()
            {
                public void actionPerformed(java.awt.event.ActionEvent e)
                {
                    ((BrowserCanvas) contentCanvas).redrawBoxes();
                    contentCanvas.repaint();
                }
            });
        }
        return redrawButton;
    }

    /**
	 * This method initializes jPanel	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getJPanel()
	{
		if (areaTreePanel == null)
		{
			GridLayout gridLayout4 = new GridLayout();
			gridLayout4.setRows(1);
			gridLayout4.setColumns(1);
			areaTreePanel = new JPanel();
			areaTreePanel.setLayout(gridLayout4);
			areaTreePanel.add(getJScrollPane(), null);
		}
		return areaTreePanel;
	}

	/**
	 * This method initializes jScrollPane	
	 * 	
	 * @return javax.swing.JScrollPane	
	 */
	private JScrollPane getJScrollPane()
	{
		if (areaTreeScroll == null)
		{
			areaTreeScroll = new JScrollPane();
			areaTreeScroll.setViewportView(getAreaTree());
		}
		return areaTreeScroll;
	}

	/**
	 * This method initializes jTree	
	 * 	
	 * @return javax.swing.JTree	
	 */
	private JTree getAreaTree()
	{
		if (areaTree == null)
		{
			areaTree = new JTree();
			areaTree.addTreeSelectionListener(new javax.swing.event.TreeSelectionListener()
			{
				public void valueChanged(javax.swing.event.TreeSelectionEvent e)
				{
                    if (logsync)
                    {
	                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) areaTree.getLastSelectedPathComponent();
	                    if (node != null && node instanceof AreaNode)
	                    {
	                        showArea((AreaNode) node);
                        	areasync = false;
                        	showAreaInLogicalTree((AreaNode) node);
                        	areasync = true;
	                    }
                    }
				}
			});
		}
		return areaTree;
	}

	/**
	 * This method initializes jPanel	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getJPanel2()
	{
		if (sepListPanel == null)
		{
			GridLayout gridLayout3 = new GridLayout();
			gridLayout3.setRows(1);
			gridLayout3.setColumns(1);
			sepListPanel = new JPanel();
			sepListPanel.setLayout(gridLayout3);
			sepListPanel.add(getJScrollPane2(), null);
		}
		return sepListPanel;
	}

	/**
	 * This method initializes jScrollPane	
	 * 	
	 * @return javax.swing.JScrollPane	
	 */
	private JScrollPane getJScrollPane2()
	{
		if (sepScroll == null)
		{
			sepScroll = new JScrollPane();
			sepScroll.setViewportView(getSepList());
		}
		return sepScroll;
	}

	/**
	 * This method initializes sepList	
	 * 	
	 * @return javax.swing.JList	
	 */
	private JList getSepList()
	{
		if (sepList == null)
		{
			sepList = new JList();
			sepList.addListSelectionListener(new javax.swing.event.ListSelectionListener()
			{
				public void valueChanged(javax.swing.event.ListSelectionEvent e)
				{
                    Separator sep = (Separator) sepList.getSelectedValue();
                    if (sep != null)
                    {
	                    sep.drawExtent((BrowserCanvas) contentCanvas);
	                    contentCanvas.repaint();
                    }
				}
			});
		}
		return sepList;
	}

	/**
     * This method initializes lookupButton	
     * 	
     * @return javax.swing.JToggleButton	
     */
    private JToggleButton getLookupButton()
    {
        if (lookupButton == null)
        {
            lookupButton = new JToggleButton();
            lookupButton.setSelected(true);
            lookupButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent arg0) {
                    if (lookupButton.isSelected())
                    {
                        boxLookupButton.setSelected(false);
                        sepLookupButton.setSelected(false);
                        extractButton.setSelected(false);
                    }
                }
            });
            lookupButton.setText("Area");
            lookupButton.setToolTipText("Find areas");
        }
        return lookupButton;
    }

    private JToggleButton getSepLookupButton() {
        if (sepLookupButton == null) {
            sepLookupButton = new JToggleButton("Sep");
            sepLookupButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent arg0) {
                    lookupButton.setSelected(false);
                    boxLookupButton.setSelected(false);
                    extractButton.setSelected(false);
                }
            });
        }
        return sepLookupButton;
    }
    
    /**
     * This method initializes extractButton	
     * 	
     * @return javax.swing.JToggleButton	
     */
    private JToggleButton getExtractButton()
    {
        if (extractButton == null)
        {
            extractButton = new JToggleButton();
            extractButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (extractButton.isSelected())
                    {
                        boxLookupButton.setSelected(false);
                        sepLookupButton.setSelected(false);
                        lookupButton.setSelected(false);
                    }
                }
            });
            extractButton.setText("Extract");
        }
        return extractButton;
    }

    /**
     * This method initializes boxLookupButton	
     * 	
     * @return javax.swing.JToggleButton	
     */
    private JToggleButton getBoxLookupButton()
    {
        if (boxLookupButton == null)
        {
            boxLookupButton = new JToggleButton();
            boxLookupButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (boxLookupButton.isSelected())
                    {
                        lookupButton.setSelected(false);
                        sepLookupButton.setSelected(false);
                        extractButton.setSelected(false);
                    }
                }
            });
            boxLookupButton.setText("Box");
            boxLookupButton.setToolTipText("Find boxes");
        }
        return boxLookupButton;
    }

    /**
     * This method initializes showSepButton	
     * 	
     * @return javax.swing.JButton	
     */
    private JButton getShowSepButton()
    {
        if (showSepButton == null)
        {
            showSepButton = new JButton();
            showSepButton.setText("Separators");
            showSepButton.addActionListener(new java.awt.event.ActionListener()
            {
                public void actionPerformed(java.awt.event.ActionEvent e)
                {
                    showSeparators();
                }
            });
        }
        return showSepButton;
    }

    /**
     * This method initializes showBoxButton	
     * 	
     * @return javax.swing.JButton	
     */
    private JButton getShowBoxButton()
    {
        if (showBoxButton == null)
        {
            showBoxButton = new JButton();
            showBoxButton.setText("Show boxes");
            showBoxButton.setToolTipText("Show all boxes in the selected tree");
            showBoxButton.addActionListener(new java.awt.event.ActionListener()
            {
				public void actionPerformed(java.awt.event.ActionEvent e)
                {
                    BoxNode node = (BoxNode) boxTree.getLastSelectedPathComponent();
                    if (node != null /*&& node.getBox() != null*/)
                    {
                        Enumeration<?> en = node.postorderEnumeration();
                        while (en.hasMoreElements())
                        {
                            BoxNode child = (BoxNode) en.nextElement();
                            if (child.getBox() != null)
                                child.drawExtent((BrowserCanvas) contentCanvas);
                        }
                        contentCanvas.repaint();
                    }
                }
            });
        }
        return showBoxButton;
    }

    /**
     * This method initializes showAreaButton	
     * 	
     * @return javax.swing.JButton	
     */
    private JButton getShowAreaButton()
    {
        if (showAreaButton == null)
        {
            showAreaButton = new JButton();
            showAreaButton.setText("Show areas");
            showAreaButton.setToolTipText("Show all the areas in the selected area");
            showAreaButton.addActionListener(new java.awt.event.ActionListener()
            {
				public void actionPerformed(java.awt.event.ActionEvent e)
                {
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) areaTree.getLastSelectedPathComponent();
                    if (node != null && node instanceof AreaNode)
                    {
                        showAreas((AreaNode) node, null);
                    }
                }
            });
        }
        return showAreaButton;
    }

    /**
     * This method initializes lookupToolBar	
     * 	
     * @return javax.swing.JToolBar	
     */
    private JToolBar getLookupToolBar()
    {
        if (lookupToolBar == null)
        {
            lookupToolBar = new JToolBar();
            lookupToolBar.add(getBoxLookupButton());
            lookupToolBar.add(getLookupButton());
            lookupToolBar.add(getSepLookupButton());
            lookupToolBar.add(getExtractButton());
        }
        return lookupToolBar;
    }

    /**
     * This method initializes jPanel	
     * 	
     * @return javax.swing.JPanel	
     */
    private JPanel getToolPanel()
    {
        if (toolPanel == null)
        {
            FlowLayout flowLayout = new FlowLayout();
            flowLayout.setAlignment(java.awt.FlowLayout.LEFT);
            toolPanel = new JPanel();
            toolPanel.setLayout(flowLayout);
            toolPanel.add(getShowToolBar(), null);
            toolPanel.add(getLookupToolBar(), null);
            toolPanel.add(getFileToolBar(), null);
            toolPanel.add(getTreeCompToolBar());
        }
        return toolPanel;
    }

    /**
	 * This method initializes fileToolBar	
	 * 	
	 * @return javax.swing.JToolBar	
	 */
	private JToolBar getFileToolBar() {
		if (fileToolBar == null) {
			fileToolBar = new JToolBar();
			fileToolBar.add(getRefreshButton());
			fileToolBar.add(getSaveButton());
			fileToolBar.add(getSaveLogicalButton());
			fileToolBar.add(getSaveRDFButton());
		}
		return fileToolBar;
	}

	/**
	 * This method initializes saveButton	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getSaveButton() {
		if (saveButton == null) {
			saveButton = new JButton();
			saveButton.setText("Save Visual");
			saveButton.setEnabled(false);
			saveButton.addActionListener(new java.awt.event.ActionListener()
			{
				public void actionPerformed(java.awt.event.ActionEvent e)
				{
					if (currentUrl != null && dispFinished)
					{
	            		JFileChooser chooser = new JFileChooser();
	            	    FileNameExtensionFilter filter = new FileNameExtensionFilter("XML files", "xml");
	            	    chooser.setFileFilter(filter);
	            	    chooser.setMultiSelectionEnabled(false);
	            	    chooser.setDialogTitle("Save Area Tree");
	            	    if (saveDir != null)
	            	    	chooser.setCurrentDirectory(saveDir);
	            	    int returnVal = chooser.showSaveDialog(mainWindow);
	            	    if(returnVal == JFileChooser.APPROVE_OPTION)
	            	    {
	            	    	saveDir = chooser.getCurrentDirectory();
	            	    	try {
		            	    	File file = chooser.getSelectedFile();
                                PrintWriter xs = new PrintWriter(file, "utf-8");
		                        XMLOutput xo = new XMLOutput(proc.getAreaTree(), currentUrl);
		                        xo.dumpTo(xs);
		                        xs.close();
	            	    	} catch (FileNotFoundException ex) {
	            	    		System.err.println("Error: " + ex.getMessage());
	            	    	} catch (UnsupportedEncodingException ex) {
                                System.err.println("Error: " + ex.getMessage());
                            }
	            	    }
					}
				}
			});
		}
		return saveButton;
	}

	/**
     * This method initializes gridButton	
     * 	
     * @return javax.swing.JButton	
     */
    private JButton getGridButton()
    {
        if (gridButton == null)
        {
            gridButton = new JButton();
            gridButton.setText("Show grid");
            gridButton.addActionListener(new java.awt.event.ActionListener()
            {
                public void actionPerformed(java.awt.event.ActionEvent e)
                {
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) areaTree.getLastSelectedPathComponent();
                    if (node != null && node instanceof AreaNode && contentCanvas instanceof BrowserCanvas)
                    {
                        ((AreaNode) node).drawGrid((BrowserCanvas) contentCanvas);
                        contentCanvas.repaint();
                    }
                }
            });
        }
        return gridButton;
    }

    /**
     * This method initializes jPanel	
     * 	
     * @return javax.swing.JPanel	
     */
    private JPanel getJPanel4()
    {
        if (jPanel == null)
        {
            GridBagConstraints gridBagConstraints8 = new GridBagConstraints();
            gridBagConstraints8.fill = GridBagConstraints.BOTH;
            gridBagConstraints8.gridy = 0;
            gridBagConstraints8.weightx = 1.0;
            gridBagConstraints8.weighty = 1.0;
            gridBagConstraints8.gridx = 0;
            jPanel = new JPanel();
            jPanel.setLayout(new GridBagLayout());
            jPanel.add(getJScrollPane3(), gridBagConstraints8);
        }
        return jPanel;
    }

    /**
     * This method initializes jScrollPane	
     * 	
     * @return javax.swing.JScrollPane	
     */
    private JScrollPane getJScrollPane3()
    {
        if (logicalTreeScroll == null)
        {
            logicalTreeScroll = new JScrollPane();
            logicalTreeScroll.setViewportView(getLogicalTree());
        }
        return logicalTreeScroll;
    }

    /**
     * This method initializes logicalTree	
     * 	
     * @return javax.swing.JTree	
     */
    private JTree getLogicalTree()
    {
        if (logicalTree == null)
        {
            logicalTree = new JTree();
            logicalTree.addTreeSelectionListener(new javax.swing.event.TreeSelectionListener()
                    {
                        public void valueChanged(javax.swing.event.TreeSelectionEvent e)
                        {
                        	if (areasync)
                        	{
	                            DefaultMutableTreeNode node = (DefaultMutableTreeNode) logicalTree.getLastSelectedPathComponent();
	                            if (node != null && node instanceof LogicalNode)
	                            {
	                            	showNode((LogicalNode) node);
                            		logsync = false;
                            		showAreaInTree(((LogicalNode) node).getFirstAreaNode());
                            		logsync = true;
	                            }
                        	}
                        }
                    });
        }
        return logicalTree;
    }

    /**
     * This method initializes refreshButton	
     * 	
     * @return javax.swing.JButton	
     */
    private JButton getRefreshButton()
    {
        if (refreshButton == null)
        {
            refreshButton = new JButton();
            refreshButton.setText("Refresh");
            refreshButton.setToolTipText("Refresh the tree views");
            refreshButton.addActionListener(new java.awt.event.ActionListener()
            {
                public void actionPerformed(java.awt.event.ActionEvent e)
                {
                    refresh();
                }
            });
        }
        return refreshButton;
    }

    /**
     * This method initializes infoSplitter	
     * 	
     * @return javax.swing.JSplitPane	
     */
    private JSplitPane getInfoSplitter()
    {
        if (infoSplitter == null)
        {
            infoSplitter = new JSplitPane();
            infoSplitter.setDividerLocation(1050);
            infoSplitter.setLeftComponent(getContentPanel());
            infoSplitter.setRightComponent(getObjectInfoPanel());
        }
        return infoSplitter;
    }

    /**
     * This method initializes objectInfoPanel	
     * 	
     * @return javax.swing.JPanel	
     */
    private JPanel getObjectInfoPanel()
    {
        if (objectInfoPanel == null)
        {
            GridBagConstraints gridBagConstraints9 = new GridBagConstraints();
            gridBagConstraints9.insets = new Insets(0, 0, 5, 0);
            gridBagConstraints9.fill = GridBagConstraints.BOTH;
            gridBagConstraints9.weighty = 1.0;
            gridBagConstraints9.weightx = 1.0;
            gridBagConstraints9.gridy = 3;
            gridBagConstraints9.gridx = 0;
            GridBagConstraints gridBagConstraints10 = new GridBagConstraints();
            gridBagConstraints10.insets = new Insets(0, 0, 5, 0);
            gridBagConstraints10.fill = GridBagConstraints.BOTH;
            gridBagConstraints10.gridy = 0;
            gridBagConstraints10.weightx = 1.0;
            gridBagConstraints10.weighty = 1.0;
            gridBagConstraints10.gridx = 0;
            objectInfoPanel = new JPanel();
            GridBagLayout gbl_objectInfoPanel = new GridBagLayout();
            gbl_objectInfoPanel.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0};
            gbl_objectInfoPanel.columnWeights = new double[]{1.0};
            objectInfoPanel.setLayout(gbl_objectInfoPanel);
            objectInfoPanel.add(getJScrollPane4(), gridBagConstraints10);
            GridBagConstraints gbc_probabilityScroll = new GridBagConstraints();
            gbc_probabilityScroll.weighty = 0.25;
            gbc_probabilityScroll.weightx = 1.0;
            gbc_probabilityScroll.insets = new Insets(0, 0, 5, 0);
            gbc_probabilityScroll.fill = GridBagConstraints.BOTH;
            gbc_probabilityScroll.gridx = 0;
            gbc_probabilityScroll.gridy = 1;
            objectInfoPanel.add(getProbabilityScroll(), gbc_probabilityScroll);
            GridBagConstraints gbc_markednessText = new GridBagConstraints();
            gbc_markednessText.insets = new Insets(0, 0, 5, 0);
            gbc_markednessText.fill = GridBagConstraints.HORIZONTAL;
            gbc_markednessText.gridx = 0;
            gbc_markednessText.gridy = 2;
            objectInfoPanel.add(getMarkednessText(), gbc_markednessText);
            objectInfoPanel.add(getJScrollPane5(), gridBagConstraints9);
        }
        return objectInfoPanel;
    }

    /**
     * This method initializes jScrollPane	
     * 	
     * @return javax.swing.JScrollPane	
     */
    private JScrollPane getJScrollPane4()
    {
        if (objectInfoScroll == null)
        {
            objectInfoScroll = new JScrollPane();
            objectInfoScroll.setViewportView(getInfoTable());
        }
        return objectInfoScroll;
    }

    /**
     * This method initializes infoTable	
     * 	
     * @return javax.swing.JTable	
     */
    private JTable getInfoTable()
    {
        if (infoTable == null)
        {
            infoTable = new JTable();
        }
        return infoTable;
    }

    
    private JButton getTagsButton()
    {
        if (tagsButton == null)
        {
            tagsButton = new JButton("Tags");
            tagsButton.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent arg0)
                {
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) areaTree.getLastSelectedPathComponent();
                    if (node != null && node instanceof AreaNode)
                    {
                        colorizeTags((AreaNode) node);
                    }
                }
            });
        }
        return tagsButton;
    }
    
    /**
     * This method initializes showArtAreaButton	
     * 	
     * @return javax.swing.JButton	
     */
    private JButton getShowArtAreaButton()
    {
        if (showArtAreaButton == null)
        {
            showArtAreaButton = new JButton();
            showArtAreaButton.setText("Art. areas");
            showArtAreaButton.setToolTipText("Show artificial areas marked with <area>");       
            showArtAreaButton.addActionListener(new java.awt.event.ActionListener()
            {
                public void actionPerformed(java.awt.event.ActionEvent e)
                {
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) areaTree.getLastSelectedPathComponent();
                    if (node != null && node instanceof AreaNode)
                    {
                        showAreas((AreaNode) node, "<area");
                    }
                }
            });
        }
        return showArtAreaButton;
    }

    /**
     * This method initializes showColumnsButton	
     * 	
     * @return javax.swing.JButton	
     */
    private JButton getShowColumnsButton()
    {
        if (showColumnsButton == null)
        {
          showColumnsButton = new JButton();
          showColumnsButton.setText("Columns");
          showColumnsButton.setToolTipText("Show columns marked with <column>");
          showColumnsButton.addActionListener(new java.awt.event.ActionListener()
          {
              public void actionPerformed(java.awt.event.ActionEvent e)
              {
                  DefaultMutableTreeNode node = (DefaultMutableTreeNode) areaTree.getLastSelectedPathComponent();
                  if (node != null && node instanceof AreaNode)
                  {
                      showAreas((AreaNode) node, "<column>");
                  }
              }
          });
        }
        return showColumnsButton;
    }

    /**
     * This method initializes jScrollPane	
     * 	
     * @return javax.swing.JScrollPane	
     */
    private JScrollPane getJScrollPane5()
    {
        if (featureScroll == null)
        {
            featureScroll = new JScrollPane();
            featureScroll.setViewportView(getFeatureTable());
        }
        return featureScroll;
    }

    /**
     * This method initializes featureTable	
     * 	
     * @return javax.swing.JTable	
     */
    private JTable getFeatureTable()
    {
        if (featureTable == null)
        {
            featureTable = new JTable();
        }
        return featureTable;
    }

    /**
     * This method initializes saveLogicalButton	
     * 	
     * @return javax.swing.JButton	
     */
    private JButton getSaveLogicalButton()
    {
        if (saveLogicalButton == null)
        {
            saveLogicalButton = new JButton();
            saveLogicalButton.setText("Save Logical");
            saveLogicalButton.setEnabled(false);
            saveLogicalButton.setToolTipText("Save logical tree to a file");
            saveLogicalButton.addActionListener(new java.awt.event.ActionListener()
            {
                public void actionPerformed(java.awt.event.ActionEvent e)
                {
                    if (currentUrl != null && dispFinished)
                    {
                        JFileChooser chooser = new JFileChooser();
                        FileNameExtensionFilter filter = new FileNameExtensionFilter("XML files", "xml");
                        chooser.setFileFilter(filter);
                        chooser.setMultiSelectionEnabled(false);
                        chooser.setDialogTitle("Save Logical Tree");
                        if (saveDir != null)
                            chooser.setCurrentDirectory(saveDir);
                        int returnVal = chooser.showSaveDialog(mainWindow);
                        if(returnVal == JFileChooser.APPROVE_OPTION)
                        {
                            saveDir = chooser.getCurrentDirectory();
                            try {
                                File file = chooser.getSelectedFile();
                                PrintWriter xs = new PrintWriter(file, "utf-8");
                                XMLLogicalOutput xo = new XMLLogicalOutput(proc.getLogicalTree(), currentUrl);
                                xo.dumpTo(xs);
                                xs.close();
                            } catch (FileNotFoundException ex) {
                                System.err.println("Error: " + ex.getMessage());
                            } catch (UnsupportedEncodingException ex) {
                                System.err.println("Error: " + ex.getMessage());
                            }
                        }
                    }
                }
            });
        }
        return saveLogicalButton;
    }

    private JButton getSaveRDFButton()
    {
        if (saveRDFButton == null)
        {
            saveRDFButton = new JButton("Save RDF");
            saveRDFButton.setEnabled(false);
            saveRDFButton.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent arg0)
                {
                    if (currentUrl != null && dispFinished)
                    {
                        JFileChooser chooser = new JFileChooser();
                        FileNameExtensionFilter filter = new FileNameExtensionFilter("RDF files", "rdf");
                        chooser.setFileFilter(filter);
                        chooser.setMultiSelectionEnabled(false);
                        chooser.setDialogTitle("Save RDF");
                        if (saveDir != null)
                            chooser.setCurrentDirectory(saveDir);
                        int returnVal = chooser.showSaveDialog(mainWindow);
                        if(returnVal == JFileChooser.APPROVE_OPTION)
                        {
                            saveDir = chooser.getCurrentDirectory();
                            try {
                                File file = chooser.getSelectedFile();
                                PrintWriter xs = new PrintWriter(file, "utf-8");
                                //RDFOutput xo = new RDFOutput(proc.getBoxTree(), proc.getAreaTree(), currentUrl);
                                //xo.dumpTo(xs);
                                xs.close();
                            } catch (FileNotFoundException ex) {
                                System.err.println("Error: " + ex.getMessage());
                            } catch (UnsupportedEncodingException ex) {
                                System.err.println("Error: " + ex.getMessage());
                            }
                        }
                    }
                }
            });
        }
        return saveRDFButton;
    }
    
    private JTextField getMarkednessText() 
    {
        if (markednessText == null) 
        {
            markednessText = new JTextField();
            markednessText.setHorizontalAlignment(SwingConstants.CENTER);
            markednessText.setText("---");
            markednessText.setFont(new Font("Dialog", Font.PLAIN, 18));
            markednessText.setEditable(false);
            markednessText.setColumns(10);
        }
        return markednessText;
    }

    private JPanel getPathsPanel()
    {
        if (pathsPanel == null)
        {
            pathsPanel = new JPanel();
            GridBagLayout gbl_pathsPanel = new GridBagLayout();
            gbl_pathsPanel.columnWidths = new int[] { 0, 0 };
            gbl_pathsPanel.rowHeights = new int[] { 0, 0, 0 };
            gbl_pathsPanel.columnWeights = new double[] { 1.0, Double.MIN_VALUE };
            gbl_pathsPanel.rowWeights = new double[] { 1.0, 1.0,
                    Double.MIN_VALUE };
            pathsPanel.setLayout(gbl_pathsPanel);
            GridBagConstraints gbc_pathListScroll = new GridBagConstraints();
            gbc_pathListScroll.insets = new Insets(0, 0, 5, 0);
            gbc_pathListScroll.fill = GridBagConstraints.BOTH;
            gbc_pathListScroll.gridx = 0;
            gbc_pathListScroll.gridy = 0;
            pathsPanel.add(getPathListScroll(), gbc_pathListScroll);
            GridBagConstraints gbc_extractionScroll = new GridBagConstraints();
            gbc_extractionScroll.fill = GridBagConstraints.BOTH;
            gbc_extractionScroll.gridx = 0;
            gbc_extractionScroll.gridy = 1;
            pathsPanel.add(getExtractionScroll(), gbc_extractionScroll);
        }
        return pathsPanel;
    }

    private JScrollPane getPathListScroll()
    {
        if (pathListScroll == null)
        {
            pathListScroll = new JScrollPane();
            pathListScroll.setViewportView(getPathList());
        }
        return pathListScroll;
    }

    private JScrollPane getExtractionScroll()
    {
        if (extractionScroll == null)
        {
            extractionScroll = new JScrollPane();
            extractionScroll.setViewportView(getExtractionTable());
        }
        return extractionScroll;
    }

    private JList getPathList()
    {
        if (pathList == null)
        {
            pathList = new JList();
            /*pathList.addListSelectionListener(new ListSelectionListener() 
            {
                public void valueChanged(ListSelectionEvent arg0) 
                {
                    TaggedNodePath path = (TaggedNodePath) pathList.getSelectedValue();
                    if (path != null)
                    {
                        for (LogicalNode node : path)
                            node.getFirstAreaNode().getArea().drawExtent((BrowserCanvas) contentCanvas);
                        contentCanvas.repaint();
                        Map<String, LogicalNode> match = pex.getMatchingNodes(path);
                        displayExtractionResult(match);
                    }
                    
                }
            });*/
        }
        return pathList;
    }

    private JScrollPane getProbabilityScroll()
    {
        if (probabilityScroll == null)
        {
            probabilityScroll = new JScrollPane();
            probabilityScroll.setViewportView(getProbTable());
        }
        return probabilityScroll;
    }

    private JTable getProbTable()
    {
        if (probTable == null)
        {
            probTable = new JTable();
            probTable.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer()
            {
                private static final long serialVersionUID = 1L;
                @Override
                public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
                {
                    Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                    try {
                        Double d = Double.parseDouble(value.toString().replace(',', '.'));
                        if (d <= Config.TAG_PROBABILITY_THRESHOLD)
                            c.setForeground(new java.awt.Color(180, 180, 180));
                        else
                            c.setForeground(Color.BLACK);
                    } catch (NumberFormatException e) {
                        c.setForeground(Color.BLACK);
                    }
                    return c;
                }
            });
        }
        return probTable;
    }

    private JTable getExtractionTable()
    {
        if (extractionTable == null)
        {
            extractionTable = new JTable();
        }
        return extractionTable;
    }
    
    private JToolBar getTreeCompToolBar() 
    {
        if (treeCompToolBar == null) 
        {
            treeCompToolBar = new JToolBar();
            treeCompToolBar.add(getEvaluationButton());
            treeCompToolBar.add(getTreeCompButton());
        }
        return treeCompToolBar;
    }
    
    private JButton getEvaluationButton()
    {
        if (evaluationButton == null)
        {
            evaluationButton = new JButton("Evaluation");
            evaluationButton.setEnabled(false);
            evaluationButton.addActionListener(new ActionListener() 
            {
                public void actionPerformed(ActionEvent arg0) 
                {
                }
            });
        }
        return evaluationButton;
    }

    private JButton getTreeCompButton() 
    {
        if (treeCompButton == null) 
        {
            treeCompButton = new JButton("TreeComp");
            treeCompButton.setEnabled(false);
            treeCompButton.addActionListener(new ActionListener() 
            {
                public void actionPerformed(ActionEvent arg0) 
                {
                }
            });
        }
        return treeCompButton;
    }
    
    /**
     * @param args
     */
    public static void main(String[] args)
    {
        browser = new BlockBrowser();
        browser.setLoadImages(false);
        JFrame main = browser.getMainWindow();
        //main.setSize(1000,600);
        //main.setMinimumSize(new Dimension(1200, 600));
        //main.setSize(1500,600);
        main.setSize(1600,1000);
        main.setVisible(true);
        
        try {
            //String localpath = "file:///home/radek/myprog/workspace/Layout";
            //String localpath = "file:///home/burgetr/workspace/Layout";
            //String localpath = "file:/C:\\Documents and Settings\\burgetr\\workspace\\Layout";
            String localpath = "file://" + System.getProperty("user.home");
            localpath += "/git/Layout";
    
            //URL url = new URL("http://www.idnes.cz/");
            //URL url = new URL("http://olomouc.idnes.cz/rad-nemeckych-rytiru-pozadal-v-restitucich-i-o-hrady-bouzov-a-sovinec-12b-/olomouc-zpravy.aspx?c=A131113_115042_olomouc-zpravy_mip");
            //URL url = new URL("http://www.aktualne.cz/");
            
            /* PROGRAMMES */
            
            //URL url = new URL("http://faculty.neu.edu.cn/yangxc/DQIS2011/workshop.html");
            //URL url = new URL("http://dali2011.dia.uniroma3.it/program.html");
            //URL url = new URL("http://www.searchingspeech.org/SSCS2010/SSCS2010.html");
            //URL url = new URL("http://sspnet.eu/2010/04/sspw/");
            //URL url = new URL("http://www.icudl2010.org/icudl_program.htm"); //problemy - prazdne vyrazne oblasti?
            //URL url = new URL("http://iwssps2010.cs.arizona.edu/program.html");
            //URL url = new URL("http://www.cssim.org/sites/cssim.org/files/cssim-timetable-full.html");
            //URL url = new URL("http://liber2009.biu-toulouse.fr/images/stories/documents/conference_programme_toulouse_EN.pdf");
            //URL url = new URL("http://www.ehealthconference.info/ConferenceProgramme/index.php");
            //URL url = new URL("http://www.dexa.org/previous/dexa2011/programme703b.html?cid=189");
            //URL url = new URL("http://www.icdar2011.org/EN/column/column32.shtml");
            //URL url = new URL("http://aktualne.centrum.cz/ekonomika/business-ve-svete/clanek.phtml?id=749291");
            //URL url = new URL("http://clair.si.umich.edu/clair/sigmod-pods06/SIGMOD-program.htm");
            //URL url = new URL("http://edbticdt2011.it.uu.se/workshops_program.html");
            //URL url = new URL("http://www.znalosti.eu/program-konference");
            //URL url = new URL(localpath + "/test/simple.html");
            //URL url = new URL(localpath + "/test/markedness.html");
            //URL url = new URL(localpath + "/test/programmes/dqis2011.html");
            //URL url = new URL(localpath + "/test/programmes/SSCS2010.html");
            //URL url = new URL(localpath + "/test/programmes/icudl2010.html");
            //URL url = new URL(localpath + "/test/programmes/iwssps2010.html");
            //URL url = new URL(localpath + "/test/programmes/dali2011.html");
            //URL url = new URL(localpath + "/test/programmes/RuleML-2010-Programme.pdf");
            //URL url = new URL(localpath + "/test/programmes/cade23-schedule.pdf");
            URL url = new URL(localpath + "/test/programmes/ehealth07.html");
            //URL url = new URL(localpath + "/test/programmes/znalosti2013.html");
            //URL url = new URL(localpath + "/test/programmes2/1/aaa-idea.org/program.shtml");            
            //URL url = new URL(localpath + "/test/programmes2/2/aciids2010.hueuni.edu.vn/index.html");
            //URL url = new URL(localpath + "/test/programmes3/x37/index.html");

            
            /* MENUS */

            //URL url = new URL("http://menu.olomouc.cz/index.php?act=rmenu&rid=32");
            //URL url = new URL("http://www.obedvat.cz/cz/obedove-menu/619-pivni-bar-atrium.html");
            
            
            
            browser.setLocation(url.toString());
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        
    }

}
