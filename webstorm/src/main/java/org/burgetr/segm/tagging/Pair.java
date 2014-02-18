/**
 * Pair.java
 *
 * Created on 26.6.2012, 14:42:54 by burgetr
 */
package org.burgetr.segm.tagging;

/**
 * @author burgetr
 *
 */
public class Pair<T, V> 
{
    protected T first;
    protected V second;

    public Pair(T first, V second)
    {
        this.first = first;
        this.second = second;
    }

    public T getFirst()
    {
        return first;
    }

    public V getSecond()
    {
        return second;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((first == null) ? 0 : first.hashCode());
        result = prime * result + ((second == null) ? 0 : second.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        Pair<?,?> other = (Pair<?,?>) obj;
        if (first == null)
        {
            if (other.first != null) return false;
        }
        else if (!first.equals(other.first)) return false;
        if (second == null)
        {
            if (other.second != null) return false;
        }
        else if (!second.equals(other.second)) return false;
        return true;
    }

    @Override
    public String toString()
    {
        return "(" + first + ", " + second + ")";
    }

    
    
}
