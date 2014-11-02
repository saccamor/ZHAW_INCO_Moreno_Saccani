package zhaw;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Formatter;
import java.util.HashMap;

import zhaw.HoffmanTree.ArcType;
import zhaw.HoffmanTree.Node;

public class Compute {
	public static final boolean LDEBUG = false;

	private HashMap< Integer /*character*/, CharProp> chars = null;
	private double fileCharactersCount = 0;
	
	// log2:  Logarithm base 2
    public static double log2(double d) {
    	return Math.log(d)/Math.log(2.0);
    }
	
    
	public void ReadInputTextFileCharacters( String relativeFilePath) throws UserErrorException 
	{
	    if ( LDEBUG ) {
	    	System.out.println("Current directory is: " + System.getProperty("user.dir"));
		}

		chars = new HashMap<>();
		fileCharactersCount = 0;
		try ( BufferedReader in = new BufferedReader( new FileReader( relativeFilePath)))
		{
			System.out.println( "Reading the input text file " + relativeFilePath + " ...");
			int c;
			while ((c = in.read()) != -1) {
				/* 
				 * ToDo: [1.1] implement computing of the relative frequency of the current character. 
				 * */
				CharProp charProp = new CharProp();
				if (chars.get(c) == null)		//Cerco se nel HashMap c'è c se non c'è é null
					{
					chars.put(c, charProp);
					
					charProp.occurence++;	//Incremento
					}	
				else
				{
					charProp = chars.get(c);	//Estraggo
					charProp.occurence++;		//Incremento
					chars.put(c, charProp);		//Reinserisco
				}
				
				/* 
				 * ToDo: [1.2] count the characters in the file. 
				 * */
				 fileCharactersCount++;
				//throw new NotImplementedException();
			}
			// add EOF too
			chars.put( HoffmanTree.hoffmanContentEOFchar, new CharProp());
			++(chars.get(HoffmanTree.hoffmanContentEOFchar).occurence);
			++fileCharactersCount;
			
		} 
		catch (FileNotFoundException ex)
		{
			throw new UserErrorException( "input file " + relativeFilePath + " does not exists.");
		} catch (IOException e) {
			throw new UserErrorException( "input file " + relativeFilePath + " reading failed.");
		}

		if ( fileCharactersCount <= 0 )
			throw new UserErrorException( "input file " + relativeFilePath + " has nothing inside.");
	}
	
	public void ComputeProbabilities( String relativeFilePath) throws UserErrorException 
	{
		// you have to read the file before computing the probabilities
		if ( chars == null )
			ReadInputTextFileCharacters( relativeFilePath);
		System.out.println( "Computing probabilities...");
		/* 
		 * ToDo: [2] implement computing of the probabilities of the existing characters. 
		 * 			 Use the precision 10 after the comma and the constant RoundingMode.HALF_UP
		 * */		
		for (int key : chars.keySet())		//chars.keySet() ritorna le chiavi del HashMap 
		{
			CharProp charProp = new CharProp();
			charProp = chars.get(key);		//Estraggo i dati dal HashMap
			BigDecimal prob = new BigDecimal(charProp.occurence/fileCharactersCount).setScale(10, RoundingMode.HALF_UP);//Calcolo la probabilità arrotondata
			charProp.probability = prob;
			chars.put(key, charProp);		//Reinserisco i dati nel HashMap (aggiornato)
		}
		//throw new NotImplementedException();
	}

	public void ComputeInformation( String relativeFilePath) throws UserErrorException 
	{
		// you have to read the file before computing the information
		if ( chars == null )
			ComputeProbabilities( relativeFilePath);
		System.out.println( "Computing information...");
		/* 
		 * ToDo: [3] implement computing of the information of the existing characters. 
		 * 			 Use the precision 10 after the comma and the constant RoundingMode.HALF_UP
		 * */		
		double information = 0;
		
		for (int key : chars.keySet())
		{
			CharProp charProp = new CharProp(); //inizializzo charProp
			charProp = chars.get(key);
			BigDecimal logOfCharPropDouble = new BigDecimal(log2(1/charProp.probability.doubleValue())).setScale(10, RoundingMode.HALF_UP);
			charProp.information = logOfCharPropDouble.doubleValue();
			chars.put(key, charProp);
		}		
		//throw new NotImplementedException();
	}

	
	public BigDecimal ComputeEntropy( String relativeFilePath) throws UserErrorException 
	{
		// you have to read the file before computing the entropy
		if ( chars == null )
			ComputeInformation( relativeFilePath);
		System.out.println( "Computing entropy...");
		BigDecimal sum = new BigDecimal( "0.0000000");
		/* 
		 * ToDo: [5] implement computing of the entropy of the existing characters. 
		 * 			 Send the entropy value back as a result.
		 * */
		for (int key : chars.keySet())
		{
			CharProp charProp = new CharProp(); //inizializzo charProp
			charProp = chars.get(key);	//Estraggo i dati dal HashMap
			double logOfCharPropDouble;
			logOfCharPropDouble = log2(charProp.probability.doubleValue());
			BigDecimal logOfCharPropBigDecimal = new BigDecimal(logOfCharPropDouble);
			sum = sum.add(charProp.probability.multiply(logOfCharPropBigDecimal));	//sum = sum+(charProp.probability * log2charProp.probability)
			
		}
		BigDecimal minusOne = new BigDecimal(-1);
		sum = sum.multiply(minusOne);

		return sum;
		//throw new NotImplementedException();
	}
	
	public void PrintOutCharProps() 
	{
		System.out.println("Character types in file: " + chars.size());
		System.out.println("Number of character in file: " + fileCharactersCount);
		for ( int c : chars.keySet() ) {
			String chr = "" + (char) c;
			if ( Character.isWhitespace(c) )
				chr = "(" + c + ")";
			try (Formatter ft = new Formatter())
			{
				System.out.println( ft.format("%1$5s : %2$s", chr,chars.get( c) ).toString());
			}
		}
	}
	
	public HoffmanTree CreateHoffmanTree() throws UserErrorException 
	{
		System.out.println( "Creating HoffmanTree...");
		if ( chars == null )
			throw new UserErrorException("You have to request computation of probabilities before you request creating of Hffman Tree.");
		HoffmanTree res = new HoffmanTree();
		// create the ordered by probabilities hash map for temporal container in order to build the tree,
		// e.g. it always keeps the node probability sorted in ascendent order.
		// the first in the sorted map "sm" is with the lowest probability
		
		CacheTreeMap sm = new CacheTreeMap();
		for ( int currChr: chars.keySet()) {
			CharProp cp = chars.get( currChr);
			try (Formatter ft = new Formatter())
			{
			//sm.put( cp.probability, res.new Node( "" + (char)currChr  /* ft.format("%s", currChr).toString()*/));
			sm.put( cp.probability, res.new Node( Character.toString((char)currChr)  /* ft.format("%s", currChr).toString()*/));
			}
		}
		// do build the HoffmanTree till there is at least two elements in the sorted cache map
	while ( sm.elements() > 1 )
		{
			/*   ToDo: [6] Having the sorted cache map "sm" so that:
			 * 			   Create a parent node which has the the lowest value element as a right child node 
			 * 			   and the second lowest as a left child node.
			 * 			   Also do not forget to add back into the sorted map the parent node.
			 *  */
			Node parent;
			Node a = sm.popNodeWithLowesProbability(ArcType.RIGHT);
			Node b = sm.popNodeWithLowesProbability(ArcType.LEFT);
			
			parent = res.CreateParentForNodes(a, b);
			parent.code = ArcType.NONE;

			//System.out.println(a.value + a.probability);
			//System.out.println(b.value + b.probability);
			
			sm.put(parent);

		}
		/*   ToDo: [7]  Add the last element as the root of the tree.
		 * */
		Node a = sm.popNodeWithLowesProbability(ArcType.NONE);

	//System.out.println(a.value + a.probability);
	
	res.root = a;
	
		return res;
	}

}






