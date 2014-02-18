/**
 * TagString.java
 *
 * Created on 13.11.2011, 17:38:40 by radek
 */
package org.burgetr.segm.tagging;

import java.util.Vector;

/**
 * A sequence of tags obtained from a tagged node path.
 * @author radek
 */
public class TagString extends Vector<Tag>
{
	private static final long serialVersionUID = 8861294399625542964L;

	public TagString(int capacity)
	{
		super(capacity);
	}
	
	@Override
	public String toString()
	{
		StringBuilder ret = new StringBuilder("(");
		for (int i = 0; i < size(); i++)
		{
			if (i > 0)
				ret.append(", ");
			ret.append(elementAt(i));
		}
		ret.append(")");
		return ret.toString();
	}
	
}
