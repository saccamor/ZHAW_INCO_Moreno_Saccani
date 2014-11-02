package zhaw;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.security.InvalidParameterException;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Map.Entry;
import java.io.FileWriter;

public class HoffmanTree 
{
	public static final boolean LDEBUG = false;

	private static final String FILE_HOFFOMANTREE_EXTENTION = ".htable";
	private static final String FILE_HOFFOMANENCODED_EXTENTION = ".hencoded";
	private static final String FILE_HOFFOMANDECODED_EXTENTION = ".hdecoded";
	public static final int hoffmanContentEOFchar = 0x3;   ///ascii is end of text
	
	/** the code is used for the arc and has a value 0 or 1 
	 * 		0 is for the right node which has to have lower or equal value to the left.
	 * 		1 is for the left node which has to have greater or equal value to the left.
	 * 		-1 is the root node.
	 * */
	public enum ArcType {
	    RIGHT(0), LEFT(1), NONE(-1);
	    private final int value;

	    private ArcType(int value) {
	        this.value = value;
	    }

	    public int getValue() {
	        return value;
	    }
	}
	
	public class Node {

		public ArcType code = null;
		public String value;
		public BigDecimal probability = new BigDecimal(0);
		public Node childLeft;
		public Node childRight;
		public Node( String val)
		{
			this.value = val;
		}
		public Node( String val, ArcType code)
		{
			this.value = val;
			this.code = code;
		}
		
		public void Serialize( BufferedWriter outBuffWritter) throws IOException
		{
			// traverse the tree till you do not have left nodes for traversing
			// you have to traverse the tree with priority ArcType.Right
			if (this.childRight != null)
				this.childRight.Serialize( outBuffWritter);
			if (this.childLeft != null)
				this.childLeft.Serialize( outBuffWritter);
			if (this.code == ArcType.NONE)
				outBuffWritter.write( this.value);
			else {
				outBuffWritter.write( this.value + (char)this.code.getValue()); /** <node_value><is_it_left_or_right_child>*/
				if (LDEBUG) System.out.println("   S: " + this.value + "(" + (int)this.code.getValue() + ")");
			}
		}
	
		/**
		 * This function builds based on the current hoffman tree
		 * the cache of <characters> -> <codes> for all the leaves of the tree.
		 * 
		 * @param hcc - code cache having as a key the character and as a value the hoffman code
		 * @param currCode - the current code which was passed from the parent.
		 */
		public void BuildCodesCache( HashMap< Integer /*character*/, Code /*Hoffman code*/> hcc, Code currCode)
		{
			if ( currCode == null )
				throw new InvalidParameterException("Code as a parameter has to be provided. ");
			if ( this.childRight != null )
					this.childRight.BuildCodesCache( hcc, new Code( currCode, this.childRight.code));  // create a right child code
			if ( this.childLeft != null )
					this.childLeft.BuildCodesCache( hcc, new Code( currCode, this.childLeft.code));  // create a left child code
			// the current node is a leaf and we can put the code into the cache
			if ( this.childRight == null && this.childLeft == null)
			{
				hcc.put( new Integer((char)this.value.charAt( 0)), currCode);
				if (LDEBUG) System.out.println( "   CC:   " +  (char)this.value.charAt( 0) + "   ->   " + currCode.value);
			}
		}
	}
	
	private class Code {    /// streams hoffman codes form character into the string with binary numbers
		private String value;   /// it will always be taken the complete 8 bytes, e.g. 8 characters form the stream
		public Code()
		{
			value = "";
		}

		/**
		 *   Concatenate the current character hoffman code with the rest of the code left from the last
		 * */
		public Code( Code currCode, ArcType arcType) 
		{
			value = currCode.value + arcType.getValue();
		}
		public void add( Code code) 
		{
			value += code.value;
		}
		private boolean hasFullByte()
		{
			return value.length() >= 8;
		}
		public void writeInto( DataOutputStream out) throws IOException
		{
			// has information for writing in the file
			while ( hasFullByte() )
				out.writeByte( popFullByte());
		}
		public void flashAllLeftBites( DataOutputStream out) throws IOException
		{
			if (value.length() < 8)
				value += "00000000".substring(0, 8 - value.length());
			// has information for writing in the file
			while ( hasFullByte() )
				out.writeByte( popFullByte());
		}
		/**
		 * Pop the first byte from the string (stream of concatenated hoffman codes) back as a result. 
		 */
		private byte popFullByte()
		{
			byte res = 0;
			String str = value.substring( 0, 8);
			value = value.substring( 8);
			for ( int i=0; i<8; ++i)
			{
				char ch = str.charAt( 7 - i);
				if (ch == '1')
					res += 1 << i;
			}
			return res;
		}
	}
	
	/** The root of the tree */
	public Node root;
	private HashMap< Integer /*character*/, Code /*Hoffman code*/> hoffmanCodesCache;
	
	
	public Node CreateParentForNodes( Node a, Node b) 
	{
		Node parent;
		if (a.code == ArcType.LEFT)
		{
			parent = new Node( a.value + b.value);
			parent.childLeft = a;
			parent.childRight = b;
		} else {
			parent = new Node( b.value + a.value);
			parent.childLeft = b;
			parent.childRight = a;
		}
		parent.probability = a.probability.add( b.probability);
		return parent;
	}
	
	/** Generate a text based file having the serialization version of the Hoffman tree which is used in the decoding process.
	 *  => <text_file_name.txt> => <text_based_hofman_tree>.htree 
	 *  Example: "oringes.txt" => "oringes.txt.htree"
	 * @throws UserErrorException 
	 * */
	public void Serialize( String relativeTextFilePath) throws UserErrorException
	{
		System.out.println( "Serializing Hoffman tree into file...");
		try ( BufferedWriter out = new BufferedWriter( new FileWriter( relativeTextFilePath + FILE_HOFFOMANTREE_EXTENTION)))
		{
			root.Serialize( out);
		} catch (FileNotFoundException ex)
		{
			throw new UserErrorException( "output file " + relativeTextFilePath + FILE_HOFFOMANTREE_EXTENTION + " can not be created.");
		} catch (IOException e) {
			throw new UserErrorException( "output file " + relativeTextFilePath + FILE_HOFFOMANTREE_EXTENTION + " writting failed.");
		}

	}
	
	public void Deserialize( String relativeTextFilePath) throws UserErrorException
	{
		String filePath = "";
		if ( relativeTextFilePath.endsWith(FILE_HOFFOMANENCODED_EXTENTION) )
			filePath = relativeTextFilePath.substring( 0, relativeTextFilePath.length() - FILE_HOFFOMANENCODED_EXTENTION.length());
		filePath += FILE_HOFFOMANTREE_EXTENTION;
		try ( BufferedReader in = new BufferedReader(new FileReader( filePath)) )
		{	    
			int c;
			String nodeValue = "";
			// create a cache with the nodes by value
			HashMap<String /*node value*/, Node> cacheLeft = new HashMap<>();
			HashMap<String /*node value*/, Node> cacheRight = new HashMap<>();
			while ((c = in.read()) != -1) 
			{
				if ( c != 0 && c != 1)
				{
					nodeValue += (char)c; 
					continue;
				}
				Node childRight = null, childLeft = null;
				// find the two pairs in case the current node is a composite node
				if ( nodeValue.length() > 1 )
				{
					for (int i=0; i<nodeValue.length()-1; ++i)
					{
						String left = nodeValue.substring(0,i+1);
						if ( ! cacheLeft.containsKey( left))
							continue;
						String right = nodeValue.substring(i+1);
						if ( ! cacheRight.containsKey( right))
							continue;
						// we found it
						childLeft = cacheLeft.get( left);
						cacheLeft.remove( left);
						childRight = cacheRight.get( right);
						cacheRight.remove( right);
					}
				}
				Node newNode = null;;
				if ( c== 1 )
					cacheLeft.put( nodeValue, newNode = new Node( nodeValue, ArcType.LEFT));
				else
					cacheRight.put( nodeValue, newNode = new Node( nodeValue, ArcType.RIGHT));
				if ( childLeft != null )
				{
					newNode.childLeft = childLeft;
					newNode.childRight = childRight;
				}
				nodeValue = ""; 
			}
			// the root node is the node with the value nodeValue
			root = new Node( nodeValue, ArcType.NONE);
			root.childLeft = cacheLeft.entrySet().iterator().next().getValue();
			root.childRight = cacheRight.entrySet().iterator().next().getValue();
		} catch (FileNotFoundException ex)
		{
			throw new UserErrorException( "input file " + filePath + " can not be find.");
		} catch (IOException e) {
			throw new UserErrorException( "file related to " + filePath + " reading failed.");
		}
		
	}

	public void Decode( String relativeTextFilePath) throws UserErrorException
	{
		String decodedFilePath = "";
		if ( relativeTextFilePath.endsWith(FILE_HOFFOMANENCODED_EXTENTION) )
			decodedFilePath = relativeTextFilePath.substring( 0, relativeTextFilePath.length() - FILE_HOFFOMANENCODED_EXTENTION.length()) + FILE_HOFFOMANDECODED_EXTENTION;
		else
		{
			decodedFilePath = relativeTextFilePath + FILE_HOFFOMANDECODED_EXTENTION;
			relativeTextFilePath += FILE_HOFFOMANENCODED_EXTENTION;
		}
		// build the code cache and then reverse it.
		BuildHoffmanCodesCache();
		HashMap< String /*Hoffman code*/, Integer /*character*/> revHCCache = new HashMap<>();
		if (LDEBUG) System.out.println("Reverse character->code cache table:");
		for ( Entry<Integer, Code> e : this.hoffmanCodesCache.entrySet())
		{
			revHCCache.put( e.getValue().value ,e.getKey());
			if (LDEBUG) 
				try (Formatter ft = new Formatter())
				{   System.out.println( ft.format("%1$12s -> %2$s", e.getValue().value, (char)(int)e.getKey()));   }
		}

		// read the encoded file and generate the decoded file
		try ( 	DataInputStream in = new DataInputStream(new FileInputStream(relativeTextFilePath));
				BufferedWriter out = new BufferedWriter(new FileWriter( decodedFilePath))
				)
		{	 
			System.out.println( "Decoding file " + relativeTextFilePath + "...");
			if (LDEBUG) System.out.println("Found tokens during reading (Ts):");
			byte c;
			String currStr = "";
			reading:
			while (true) 
			{
				c = in.readByte();
				// concatenate the input to the current string
				for ( int i=7; i>=0; --i)
				{
					if ( (c & (1 << i)) > 0 )
					   currStr += "1";
					else
						currStr += "0";
				}
				if (LDEBUG) System.out.println( "read() and current string is jet: " + currStr);
				// try to find as much as possible encoded letters
				for ( int i=1; i<=currStr.length(); ++i)
				{
					String token = currStr.substring(0, i);
					if ( ! revHCCache.containsKey( token))
						continue;
					// we found a hoffman code in the input stream 
					if (LDEBUG) System.out.println( "\t  T: " + token);
					// check for end of file
					if ( hoffmanContentEOFchar == revHCCache.get(token))
					{
						System.out.println("Decoding successfully finished.");
						break reading;
					}
					out.write( revHCCache.get(token));
					currStr = currStr.substring(i);
					i=0;
				}
			}
		} catch ( EOFException ex)
		{
			/** hoffmanContentEOFchar is the mark token for end of content and you forgot to encode it at the end. */
			System.out.println("Reading readed end of file. Decoding probably is unsuccessfull!");
		} catch (FileNotFoundException ex)
		{
			throw new UserErrorException( "input file " + relativeTextFilePath + " can not be find.");
		} catch (IOException e) {
			throw new UserErrorException( "file related to " + relativeTextFilePath + " reading failed.");
		}

	}
	
	/** get the text file name, open it, read it and encode it to a new binary file basing the encoding of the current Hoffman tree. 
	 *  => <text_file_name.txt> => <binary_text_encoded>.hencoded 
	 *  Example: "oringes.txt" => "oringes.txt.hencoded"
	 * @throws UserErrorException 
	 * */
	public void Encode( String relativeTextFilePath) throws UserErrorException 
	{
		try ( 	BufferedReader in = new BufferedReader( new FileReader( relativeTextFilePath));
				DataOutputStream out = new DataOutputStream(new FileOutputStream(relativeTextFilePath + FILE_HOFFOMANENCODED_EXTENTION))
				)
		{	    
			if ( LDEBUG ) System.out.println("Building Hoffman cache");
			BuildHoffmanCodesCache();

			if ( LDEBUG ) System.out.println("Starting encoding " + relativeTextFilePath + "...");
			Encode( in, out);
		} catch (FileNotFoundException ex)
		{
			throw new UserErrorException( "input file " + relativeTextFilePath + " can not be find.");
		} catch (IOException e) {
			throw new UserErrorException( "file related to " + relativeTextFilePath + " writting failed.");
		}

	}

	private void Encode( BufferedReader in, DataOutputStream out) throws IOException 
	{
		/**    ToDo: [10] Please fill after the commented lines with todo 10.1 and 10.2 the necessary code in
		 *           order after you run the  the program with parameters for serializing and encoding: -t deutsch.txt -p -h
		 *           to get the two files which with running second time the program with parameters: -d deutsch.txt.hencoded
		 *           you will get the decoded text file. Then you need to compare the original with the decoded with file hash sum
		 *           or some tool telling the difference between files.
		 *           If you implemented successfully the task 10 then you have to have no differences.
		 * */
		if ( hoffmanCodesCache == null )
			throw new RuntimeException( "Hoffman code cache is a prerequisite for this method. Do build it before.");
		int c;
		Code codeLeftForWritting = new Code();
		while ((c = in.read()) != -1)   //   reads a single character from the input file
		{
			if ( ! hoffmanCodesCache.containsKey(c) )
				throw new RuntimeException( "Hoffman code was not found in the cache. The cache is invalid.");
			/** [10.1] - needs 2 lines of code using the instance "codeLeftForWritting" 
			 * */
			codeLeftForWritting.add(hoffmanCodesCache.get(c));
			codeLeftForWritting.writeInto(out);
//			throw new NotImplementedException();
			
			if (LDEBUG) System.out.println("   W:   " + (char)c + " = " + hoffmanCodesCache.get( c).value);
		}
		/** [10.3] - needs 3 lines of code using the instance "codeLeftForWritting" 
		 * */
		codeLeftForWritting.add(hoffmanCodesCache.get(hoffmanContentEOFchar));
		codeLeftForWritting.writeInto(out);
		codeLeftForWritting.flashAllLeftBites(out);
//		throw new NotImplementedException();
	}

	private void BuildHoffmanCodesCache()
	{
		if ( hoffmanCodesCache != null )
			return;
		hoffmanCodesCache = new HashMap<>();

		// traverse the Hoffman tree and create the cache with character as a key
		root.BuildCodesCache( hoffmanCodesCache, new Code());
	}
	
}













