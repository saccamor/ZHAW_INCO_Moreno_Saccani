package zhaw;

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;

public class CacheTreeMap extends TreeMap< BigDecimal /*probability*/, List< HoffmanTree.Node>>{
	private static final long serialVersionUID = 123L;
	private int elementsInTheContainer = 0;
	
	public void put( BigDecimal probability, HoffmanTree.Node node)
	{
		node.probability = probability;
		this.put( node);
	}
	
	public void put( HoffmanTree.Node node)
	{
		if ( ! this.containsKey( node.probability) )
			this.put( node.probability, new LinkedList< HoffmanTree.Node>());
		this.get( node.probability).add( node);
		++elementsInTheContainer;
	}
	
	public HoffmanTree.Node popNodeWithLowesProbability( HoffmanTree.ArcType withArcType) 
	{
		/*   
		 *  */
		BigDecimal lowestProbability = this.firstKey();
		List< HoffmanTree.Node> lowestPNodeList = this.get( lowestProbability);
		HoffmanTree.Node firstNode = lowestPNodeList.get( 0); // get the first node from the list with the same probabilities
		lowestPNodeList.remove( 0); //remove the first element from the list of the last key in the sorted map
		// if the list is empty then remove the element from the sorted map
		if ( lowestPNodeList.isEmpty() )
			this.remove( lowestProbability); //remove the first key from the sorted map which has the lowest probability
		--elementsInTheContainer;
		firstNode.code = withArcType;
		return firstNode;
	}

	public int elements() 
	{
		return elementsInTheContainer;
	}
};


