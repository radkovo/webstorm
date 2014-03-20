/**
 * NKStoreBolt.java
 *
 * Created on 6. 3. 2014, 13:54:38 by burgetr
 */
package org.fit.burgetr.webstorm.bolts;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.IRichBolt;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.tuple.Tuple;

/**
 * A bolt that receives the name - keyword relationships and builds statistics.
 * Accepts: (name, keyword, base_url)
 * 
 * @author burgetr
 */
public class NKStoreBolt implements IRichBolt
{
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(NKStoreBolt.class);
    
    private OutputCollector collector;
    private PreparedStatement insert;
    private Connection db;
    private long nextid = 0;
    
    public NKStoreBolt()
    {
    }

    @SuppressWarnings("rawtypes")
    public void prepare(Map stormConf, TopologyContext context, OutputCollector collector)
    {
        try
        {
            initDB();
        } catch (ClassNotFoundException e)
        {
            e.printStackTrace();
        } catch (SQLException e)
        {
            e.printStackTrace();
        }
        this.collector = collector;
    }

    public void execute(Tuple input)
    {
        String name = input.getString(0);
        String keyword = input.getString(1);

        try
        {
            storeOccurence(name, keyword);
            collector.ack(input);
        }
        catch (SQLException e)
        {
            e.printStackTrace();
            collector.fail(input);
        }
        
    }

    public void cleanup()
    {
        try
        {
            db.close();
        } 
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    public void declareOutputFields(OutputFieldsDeclarer declarer)
    {
    }

    public Map<String, Object> getComponentConfiguration()
    {
        return null;
    }

    //================================================================================================
 
    private void initDB() throws ClassNotFoundException, SQLException
    {
        Class.forName("org.h2.Driver");
        db = DriverManager.getConnection("jdbc:h2:mem:mytest", "sa", "");
        //Connection con = DriverManager.getConnection("jdbc:h2:tcp://localhost/~/test",  "sa", "");
        
        Statement stat = db.createStatement();
        stat.execute("CREATE TABLE entries (id INTEGER, atime datetime, name VARCHAR(32), keyword VARCHAR(32), PRIMARY KEY (id))");
        
        //prepared statement
        insert = db.prepareStatement("INSERT INTO entries (id, atime, name, keyword) VALUES (?,?,?,?)");
    }
    
    private void storeOccurence(String name, String keyword) throws SQLException
    {
        insert.setLong(0, nextid++);
        insert.setTimestamp(1, new Timestamp((new Date()).getTime()));
        insert.setString(2, name);
        insert.setString(3, keyword);
        insert.execute();
        log.debug("Stored " + name + ":" + keyword);
    }
    
}
