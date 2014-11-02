package zhaw;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ComputeMain {

	public static final boolean LDEBUG = true;

	/**
	 * Program arguments:
	 * 	ComputeMain [-t <input_text_file_name> [-f | -p | -i | -e] [-h]] | [-d <file_for_decoding.hencoded>]
	 * 		-t is the input text file
	 * 		-f compute the relative frequencies
	 * 		-p compute the probabilities 
	 * 		-i compute the information content
	 * 		-e compute the entropy
	 * 		-h create the hoffman tree and serialize it to the file. Note: requires probability information
	 * 		-d decode a file based on Hoffman encoding and generate the relevant .decoded file
	 * */
	private static HashMap<String, String> programParams = new HashMap<>();
	
	private static void parsProgramArguments( String[] args) throws UserErrorException {
		String currParamValue = null;
	    for (int i = 0; i < args.length; i++) {
	    	switch (args[i].charAt(0)) {
	        case '-':
	        	Matcher matcher = Pattern.compile("\\-+([\\w_]+)").matcher(args[i]);
	        	if ( ! matcher.find() || matcher.groupCount() <= 0 )
	        		throw new UserErrorException("Ivalid argument name: " + args[i]);
	        	programParams.put( currParamValue =  matcher.group(1), null);
	        	continue;
	        default:
	        	if ( currParamValue == null || ! programParams.containsKey( currParamValue) )
	        		throw new UserErrorException("No argument name specified for argument value: " + args[i]);
	        	programParams.put( currParamValue, args[i]);
	        	currParamValue = null;
	        	continue;
	    	}
	    }
	    
	    if ( LDEBUG ) {
	    	System.out.println("Program parameters:   ");
		    for ( String k: programParams.keySet()) {
		    	System.out.println("\t" + k + " : " + programParams.get(k));
		    }
	    }
	}
	
	public static void main(String[] args) {
		System.out.println( "Starting ComputeMain...");

		try {
			parsProgramArguments( args);
			
			Compute compute = new Compute();
			// check if the input text file is required 
			if ( programParams.containsKey("f") || programParams.containsKey("p") || programParams.containsKey("i") || programParams.containsKey("e") )
				if ( programParams.get("t") == null)
					throw new UserErrorException("Missing input text file for reading.");

			/// compute entropy
			if ( programParams.containsKey("e") ) {
				BigDecimal entropy = compute.ComputeEntropy( programParams.get("t"));
				System.out.println("Entropy : " + entropy);
			}
			/// compute information
			else if ( programParams.containsKey("i") )
				compute.ComputeInformation( programParams.get("t"));
			/// compute probability
			else if ( programParams.containsKey("p") )
				compute.ComputeProbabilities( programParams.get("t"));
			/// compute frequency
			else if ( programParams.containsKey("f") )
				compute.ReadInputTextFileCharacters( programParams.get("t"));
			
			if ( programParams.containsKey("h") )
			{
				HoffmanTree hoffmanTree = compute.CreateHoffmanTree();
				// serialize to the binary Hoffman table file
				hoffmanTree.Serialize( programParams.get("t"));
				// encode the text file on the bases of the hoffmantree object to the binary file
				hoffmanTree.Encode( programParams.get("t"));
			}
			else if ( programParams.containsKey("d") )
			{
				HoffmanTree hoffmanTree = new HoffmanTree();
				// deserialize the Hoffman tree from file
				System.out.println("Deserializing Hoffman tree...   ");
				hoffmanTree.Deserialize( programParams.get("d"));
				// decode the encoded file based on the already deserialized tree
				hoffmanTree.Decode( programParams.get("d"));
			}

			if ( programParams.containsKey("f") || programParams.containsKey("p") || programParams.containsKey("i") || programParams.containsKey("e") )
				compute.PrintOutCharProps();
		} catch ( UserErrorException uex) {
			System.err.println("Error: " + uex.getMessage());
		}
		System.out.println("Program execution completed");
	}

}
