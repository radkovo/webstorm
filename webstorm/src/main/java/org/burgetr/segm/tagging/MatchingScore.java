/**
 * MatchingScore.java
 *
 * Created on 15.11.2011, 9:35:19 by burgetr
 */
package org.burgetr.segm.tagging;

import java.util.Locale;

/**
 * A result of the tag pattern matching. It assigns a tag string to its matching score and the number of occurences.
 * @author burgetr
 */
public class MatchingScore<T> implements Comparable<MatchingScore<T>>
{
    private T pattern;
    private int count;
    private double score;
    
    public MatchingScore(T pattern, int count, double score)
    {
        this.pattern = pattern;
        this.count = count;
        this.score = score;
    }

    public T getPattern()
    {
        return pattern;
    }

    public void setPattern(T pattern)
    {
        this.pattern = pattern;
    }

    public int getCount()
    {
        return count;
    }

    public void setCount(int count)
    {
        this.count = count;
    }

    public double getScore()
    {
        return score;
    }

    public void setScore(double score)
    {
        this.score = score;
    }
    
    public int compareTo(MatchingScore<T> other)
    {
        /*if (score == other.score)
            return count - other.count;
        else if (score < other.score)
            return -1;
        else
            return 1;*/
        if (count == other.count)
        {
            if (score == other.score)
                return 0;
            else if (score < other.score)
                return -1;
            else
                return 1;
        }
        else
            return count - other.count;
    }

    @Override
    public String toString()
    {
        return String.format(Locale.US, "%.2f:%dx%s", score, count, pattern.toString());
    }

}
