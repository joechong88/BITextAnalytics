/*
 * IBM Confidential
 * OCO Source Materials
 * (C) Copyright IBM Corp. 2010, 2011, 2012
 * The source code for this program is not published or otherwise divested of its trade secrets, irrespective of what has been deposited with the U.S. Copyright Office.
 */

package com.ibm.biginsights.textanalytics.example;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.fs.Path;

import com.ibm.avatar.algebra.datamodel.AbstractTupleSchema;
import com.ibm.avatar.algebra.datamodel.FieldGetter;
import com.ibm.avatar.algebra.datamodel.FieldType;
import com.ibm.avatar.algebra.datamodel.Pair;
import com.ibm.avatar.algebra.datamodel.Span;
import com.ibm.avatar.algebra.datamodel.TLIter;
import com.ibm.avatar.algebra.datamodel.Tuple;
import com.ibm.avatar.algebra.datamodel.TupleList;
import com.ibm.avatar.algebra.datamodel.TupleSchema;
import com.ibm.avatar.algebra.util.tokenize.TokenizerConfig;
import com.ibm.avatar.api.CompileAQL;
import com.ibm.avatar.api.CompileAQLParams;
import com.ibm.avatar.api.DocReader;
import com.ibm.avatar.api.ExplainModule;
import com.ibm.avatar.api.ExternalTypeInfo;
import com.ibm.avatar.api.ExternalTypeInfoFactory;
import com.ibm.avatar.api.OperatorGraph;
import com.ibm.avatar.api.exceptions.TextAnalyticsException;
import com.ibm.avatar.api.tam.ModuleMetadata;
import com.ibm.avatar.api.tam.ModuleMetadataFactory;

/*
 * We recommend that the reader of this tutorial carefully examine every usage of our runtime APIs and also perform a Ctrl+Space [content assist]
 * on each object used whose native type is defined by some class in our runtime. This would help the reader explore further methods to learn and utilize.
 * We also recommend the reader to visit our InfoCenter documentation section that describes our runtime APIs in greater detail.
 * 
 * Resources for this tutorial [including external dictionary file] can be found under the 'resources' folder of this project.
 * 
 * Data set necessary for this tutorial can be found under the folder 'data' of this project.
 * 
 * @author IBM
 * @version 2.0
 */

public class TextAnalyticsAPITutorial {
	
	private final Log log = LogFactory.getLog(getClass());
	
	// Path to source of text analytics modules inside this project
	private String MODULES_SRC_PATH = new File("textAnalytics/src").getAbsolutePath();
	
	// Path to compiled modules inside this project
	private String COMPILED_MODULES_PATH = new File("textAnalytics/bin").toURI().toString();
	
	// Path to parent directory of possible input data collection(s) to use, in executing the text analytics modules inside this project
	// Assumption is the every directory inside INPUT_DATA_COLLECTIONS_ROOT would each be an input data collection on its own.
	private static String INPUT_DATA_COLLECTION = new File("data/ibmQuarterlyReports").getAbsolutePath();
	
	private static String INPUT_DATA_EXTERNAL_VIEW = new File("data/externalViewData/externalViewData.json").getAbsolutePath();
	
	private String EXPLAIN_MODULE_OUTPUT_DIRECTORY = new File("resources/tutorialOutput").getAbsolutePath();
	
	/*
	 * Fetch the list of children under the denoted text analytics source folder of this project.
	 * 
	 * @return String[] - array of directory names under the text analytics source directory. Each directory returned is the root of a module. If 
	 * 					  the text analytics source location either doesn't exist or isn't a directory, a null is returned.
	 */
	
	private String[] getTextAnalyticsModulesInThisProject() {
	
		File modulesSourceLocation = new File(MODULES_SRC_PATH);
		
		if(modulesSourceLocation.exists() && modulesSourceLocation.isDirectory()) {
			MODULES_SRC_PATH = new File(MODULES_SRC_PATH).toURI().toString();
			return modulesSourceLocation.list();
		}
		
		return null;
		
	}
	
	// An array of text analytics modules to use in this sample project, that are found under MODULES_SRC_PATH
	private String[] TEXTANALYTICS_MODULES = getTextAnalyticsModulesInThisProject();
	
	// Output of any text analytics execution, performed using our eclipse tools can be found as time-stamped folders
	// under the project's 'result' folder. Right click on any such result folder and select 'Show result in Annotation Explorer View' to
	// examine the results from your extractor.
	
	/*
	Each module requires a certain kind of tokenization configuration, based on the mode of tokenization of the dictionaries it consumes, 
	both during compile-time and load-time. Please note that when the TokenizerConfig object for a given module at compile-time and load-time
	do not match, a runtime exception would be thrown with the appropriate error information. There two such modes - STANDARD and MULTILINGUAL.
	For sake of simplicity of this sample project, 
	let's persist with the Standard Tokenizer configuration. For more information about using our Multilingual tokenizer configuration, 
	please visit our InfoCenter documentation on Tokenization, under the Text Analytics section.
	*/
	
	private TokenizerConfig STANDARD_TOKENIZER = new TokenizerConfig.Standard();
	
	
	// The OperatorGraph object is an internal graphical representation of the text analytics extractor yielded when all the specified modules 
	// are combined together. 
	private OperatorGraph extractor = null;
	

	/*
	 * Receive user input of a path to an external type element - 'q' to quit the prompt
	 * 
	 * @param isRequired - a flag whose value if 'true' indicates that a user input is mandatory. 'else' optional.
	 * @param externalType - either a 'dictionary' or a 'table'
	 * @param externalTypeName - name of the external type, as specified in the metadata of the module
	 * @return String - the fully qualified URI String of the user specified location to the external type element
	 */
	
	private String getUserInputFor (boolean isRequired, String externalType, String externalTypeName) throws Exception {
		
		String userInput = "";
		
		do {
			
			if(isRequired)
				System.out.println("\n\t Please enter location of external file for external "+externalType+" : "+externalTypeName+ "*, 'i' to ignore, 'q' to quit : ");
			else
				System.out.println("\n\t Please enter location of external file for external "+externalType+" : "+externalTypeName+ ", 'i' to ignore, 'q' to quit : ");
			
			BufferedReader userInputReader = new BufferedReader(new InputStreamReader (System.in));
		
			try {
		
				userInput = userInputReader.readLine(); 
				if(!userInput.toLowerCase().equals("q")) {
					if(!userInput.toLowerCase().equals("i")) {
						isRequired = false; 
						userInput = new File(userInput).toURI().toString();
						return userInput;
					}
					else if(!isRequired)
						return null;
				}
				else
					throw new Exception("Required external file path not provided. Please re-try running this tutorial with the required input -- Refer to this project's README.txt for additional help.");
		
			} catch (IOException exception) {
				log.error(exception);
			}
			
		} while(isRequired);
		
		return null;
	}
	
	private boolean validateModules () throws TextAnalyticsException {

		/*
		 * The OperatorGraph.validateOG method is a useful utility that can be leveraged to test the validity of a set of text analytics modules 
		 * before they are to be used to construct the required Operator graph. It is encouraged that this API be utilized prior to any other 
		 * operations on modules at run-time, so as to catch any discrepancy. Following are some of the checks it performs: 
		 * 
		 * a. Ensure compliance of the Tokenizer configuration used on every module between that used during its compilation and that being used during its loading.
		 * b. Ensure compliance of the Tokenizer configuration used across all modules being operated upon. An OperatorGraph can be constructed only on those modules,
		 * all of whom share a similar Tokenizer configuration mode.
		 * c. Ensure compliance on the input document schemata used by each individual module - All of them must use a similar input document schema.
		 * d. Ensure that there are no two modules present on either the module path or the project's system class path, with similar names.
		 * e. Ensure that all dependencies of each module [in terms of other modules] are all on either the module path or the project's system class path.
		 */
		
		return OperatorGraph.validateOG(TEXTANALYTICS_MODULES, COMPILED_MODULES_PATH, STANDARD_TOKENIZER);
		
	}
	
	/*
	 * Receive inputs from user for each required and optional external dictionary and table for each module
	 * in our text analytics source location
	 * 
	 * @return ExternalTypeInfo - the object containing external type information for all modules together, which need to be stitched together into one operator graph
	 *							  If no such external type info is required, a null would be returned.
	 */
	
	private ExternalTypeInfo getExternalTypesForModules () throws TextAnalyticsException, Exception {
		
		ModuleMetadata[] modulesMetadata = ModuleMetadataFactory.readMetaData(TEXTANALYTICS_MODULES, COMPILED_MODULES_PATH);
		
		ExternalTypeInfo externalTypeInfo = ExternalTypeInfoFactory.createInstance();
		
		System.out.println("Please enter locations to required external dictionaries and tables for each module below. \n"+
							" Required paths would be marked with an * besides the element's name");
		
		for(int metadataIndex = 0; metadataIndex < modulesMetadata.length; ++metadataIndex) {
			
			ModuleMetadata metadata = modulesMetadata[metadataIndex];
			
			// Get list of URI Strings for all external dictionaries in this module
			String[] externalDictionaries = metadata.getExternalDictionaries();
			for(int dictionaryIndex = 0; dictionaryIndex < externalDictionaries.length; ++dictionaryIndex) {
			
				String externalDictionaryName = externalDictionaries[dictionaryIndex];
				boolean isRequired = ! (metadata.getDictionaryMetadata(externalDictionaryName).isAllowEmpty());
				String dictionaryPath = getUserInputFor(isRequired, "dictionary", externalDictionaryName);
				externalTypeInfo.addDictionary(externalDictionaryName, dictionaryPath);
				
			}
			
			// Get list of URI Strings for all external tables in this module
			String[] externalTables = metadata.getExternalTables();
			for(int tableIndex = 0; tableIndex < externalTables.length; ++tableIndex) {
			
				String externalTableName = externalTables[tableIndex];
				boolean isRequired = ! (metadata.getTableMetadata(externalTableName).isAllowEmpty());
				String tablePath = getUserInputFor(isRequired, "table", externalTableName);
				externalTypeInfo.addTable(externalTableName, tablePath);
				
			}
			
		}
		
		if (externalTypeInfo.getDictionaryNames().isEmpty() && externalTypeInfo.getTableNames().isEmpty())
			return null;
		else
			return externalTypeInfo;
	}
	
	
	/*
	 * Construct and return the operator graph of the extractor, constituting the modules inside this project
	 * This method shall also take care of any external type information needed by the text analytics runtime 
	 * [ex. external dictionaries, tables] in constructing this extractor's operator graph.
	 * 
	 *  @return OperatorGraph
	 */
	
	private OperatorGraph getOperatorGraph() throws TextAnalyticsException, Exception {
		
		return OperatorGraph.createOG(TEXTANALYTICS_MODULES, COMPILED_MODULES_PATH, getExternalTypesForModules(), STANDARD_TOKENIZER);
		
	}
	
	
	/*
	 * Auxiliary method to display a schema - either input document or output view schema
	 * 
	 * @param textAnalyticsSchema - the schema that needs to be displayed
	 */
	private void displayTextAnalyticsSchema (TupleSchema textAnalyticsSchema) {
		
		String[] fieldNames = textAnalyticsSchema.getFieldNames ();
	    FieldType[] fieldTypes = textAnalyticsSchema.getFieldTypes ();
		
	    for(int fieldIndex = 0; fieldIndex < fieldNames.length; ++fieldIndex)
	    	System.out.println("\t Field name : "+fieldNames[fieldIndex]+"\tField type : "+fieldTypes[fieldIndex] );
	    
	}
	
	/*
	 * Auxiliary method to clear the directory containing compiled modules [or .tam files]
	 */
	
	private void clearCompiledModulesDirectory () throws URISyntaxException, IOException {
		
		File compiledModulesPath = new File(new URI(COMPILED_MODULES_PATH).getPath());
		if(compiledModulesPath.isDirectory() && compiledModulesPath.list().length > 0) {

			for (String compiledModulePath : compiledModulesPath.list())
				new File(compiledModulePath).delete();
			
		}

	}
	
	/*
	 * Compile the source of a list of input modules from the MODULES_SRC_PATH of this project 
	 */
	@SuppressWarnings("unused")
	private void compileTextAnalyticsModules() throws TextAnalyticsException, Exception {
		
		// Clear the compiled modules folder, to ensure we write fresh copies of the compiled modules into it
		clearCompiledModulesDirectory();
		
		CompileAQLParams moduleCompilationParams = new CompileAQLParams();
		
		String[] INPUT_MODULE_SOURCE = new String[TEXTANALYTICS_MODULES.length];
		
		for(int moduleIndex = 0; moduleIndex < TEXTANALYTICS_MODULES.length; ++moduleIndex)
			INPUT_MODULE_SOURCE [moduleIndex] = MODULES_SRC_PATH + Path.SEPARATOR + TEXTANALYTICS_MODULES [moduleIndex];
		
		moduleCompilationParams.setInputModules(INPUT_MODULE_SOURCE);
		moduleCompilationParams.setModulePath(MODULES_SRC_PATH);
		moduleCompilationParams.setOutputURI(COMPILED_MODULES_PATH);
		moduleCompilationParams.setTokenizerConfig(STANDARD_TOKENIZER);
		CompileAQL.compile(moduleCompilationParams);
		
	}
	
	/*
	 * Auxiliary method to explain every module generated, into a text file for each such module inside $EXPLAIN_MODULE_OUTPUT_DIRECTORY
	 */
	
	@SuppressWarnings("unused")
	private void explainTextAnalyticsModules() throws TextAnalyticsException, Exception {
		
		for(File compiledModuleFile : new File(new URI(COMPILED_MODULES_PATH).getPath()).listFiles()) 
			ExplainModule.explain(compiledModuleFile, new File(EXPLAIN_MODULE_OUTPUT_DIRECTORY + "/" + compiledModuleFile.getName()+".txt"));
		
	}
	
	/*
	 * Auxiliary method to print extraction results
	 */
	
	@SuppressWarnings("unused")
	private void printExtractionResults (Map<String, TupleList> extractions)
	  {
	    for (String viewName : extractions.keySet ()) {

	      // All tuples of the output view
	      TupleList tups = extractions.get (viewName);

	      System.out.printf ("Output View %s:\n", viewName);

	      // The schema of the output view
	      AbstractTupleSchema schema = tups.getSchema ();

	      // Iterate through the tuples of the output view
	      TLIter itr = tups.iterator ();
	      while (itr.hasNext ()) {
	        Tuple tup = itr.next ();
	        System.out.printf ("    %s\n", tup);

	        // Iterate through the fields of the tuple, to show how to do it.
	        // Method #1: Direct access to fields by index
	        
	        for (int fieldIx = 0; fieldIx < schema.size (); fieldIx++) {
	          String fieldName = schema.getFieldNameByIx (fieldIx);
	          Object fieldVal = schema.getCol (tup, fieldIx);
	        }

	        // Method #2: Create and use accessor objects
	        // Creating the accessors -- do this ONCE, ahead of time.
	        // The accessor objects should be created ONCE, ahead of time. The accessors can be
	        // reused subsequently to access values of all tuples of this output view, across all input documents
	        
	        for (int fieldIx = 0; fieldIx < schema.size (); fieldIx++) {

	          // Obtain the field name from the view schema
	          String fieldName = schema.getFieldNameByIx (fieldIx);

	          // Use a Span accessor to access fields of type Span.
	          if (schema.getFieldTypeByIx (fieldIx).getIsSpan ()) {
	            FieldGetter<Span> accessor = schema.spanAcc (fieldName);

	            // Using the accessor to get the field value
	            Span span = accessor.getVal (tup);
	            System.out.printf ("    %s: %s\n", fieldName, span.getText ());
	          }
	          // Use an Integer accessor to access fields of type Integer
	          else if (schema.getFieldTypeByIx (fieldIx).getIsIntegerType ()) {
	            FieldGetter<Integer> accessor = schema.intAcc (fieldName);

	            // Using the accessor to get the field value
	            int intVal = accessor.getVal (tup);
	            System.out.printf ("    %s: %d\n", fieldName, intVal);
	          }
	          
	          // Similarly, we have accessors for other scalar data types in AQL, such as Text, Float and ScalarList

	        }
	        System.out.println ();
	      }
	    }
	  }
	
	private void loadAndExecuteTextAnalyticsModules() throws TextAnalyticsException, Exception {
		
		// Now that the modules are compiled from their source in to their .tam equivalents:

		// a. Validate the set of modules to be used further 
		if(validateModules()) {

			// b. Construct the Operator Graph for the set of text analytics modules inside this project.
			extractor = getOperatorGraph();

			/*
			 * Get the required input schema for this extractor.
			 * If there was no 'required document with columns' statement in any module constituting
			 * this operator graph, the default input schema would be [label of type Text, text of type Text] 
			 */

			// c. Inspect the required input document schema for this extractor
			TupleSchema inputDocumentSchema = extractor.getDocumentSchema();

			System.out.println("\n Displaying input document schema of the constructed extractor : ");
			displayTextAnalyticsSchema (inputDocumentSchema);


			/*
			 *  Display the output schema for every output view type in this extractor
			 */

			// d. Examine the output schema of every output type in this extractor
			Map<String, TupleSchema> outputTypesSchema = extractor.getOutputTypeNamesAndSchema();

			for(String outputType : outputTypesSchema.keySet()) {

				System.out.println("Output schema for output view type : " + outputType);
				displayTextAnalyticsSchema(outputTypesSchema.get(outputType));

			}

			// e. Let's execute this extractor atop the specified input data collection, for every output type in this extractor
			DocReader inputDataReader = new DocReader (new File(INPUT_DATA_COLLECTION));

			// Read every tuple in from the docReader object, each of which represents an input document to the extractor to extract from
			while(inputDataReader.hasNext()) {
				Tuple inputTuple = inputDataReader.next();

				// Place the results from each extraction in to the global map, per tuple being processed
				System.out.println(inputTuple.toString());
				printExtractionResults(extractor.execute(inputTuple, null, null)); 
			}
		}
	}
	
	/*
	 * Method to illustrate the DocReader APIs to read in tuples of an external view from an external file in JSON format.
	 * The APIs create an association between each input document tuple, and its corresponding external view tuples. This
	 * association is then passed on to the text analytics runtime during execution of the extractor, to ensure the 
	 * necessary external view defined inside our AQL that pertains to this external name [as in the JSON file] is well
	 * populated prior to being utilized internally, inside the extractor.   
	 * 
	 */
	
	public void illustrateExternalViewsViaDocReader() throws TextAnalyticsException, Exception {
		
		// Retrieve this extractor's required input document schema
		TupleSchema extractorInputDocumentSchema = extractor.getDocumentSchema();
		
		// Retrive the view schema of the external view defined in this extractor. Note that the external view name is explicitly fully qualified with the module name
		TupleSchema externalViewSchema = extractor.getExternalViewSchema("metricsIndicator_externalTypes.EmailMetadata");

		// Persist the schema of the external view along with its external name, for the runtime to comprehend  
		Map<Pair<String, String>, TupleSchema> externalViewNameVsSchema = new HashMap<Pair<String, String>, TupleSchema>();
		externalViewNameVsSchema.put(new Pair<String, String>("metricsIndicator_externalTypes.EmailMetadata", "EmailMetadataSrc"), externalViewSchema);
		
		// Receive the iterator across each document tuple, that would fetch in turn the list of all the external view tuples associated with that document tuple
		// Note the use of the static method 'makeDocandExternalPairsItr' - this is a new and powerful utility added in 2.0 to support reading in external view tuples 
		// from input data files, of JSON format.
		Iterator<Pair<Tuple, Map<String, TupleList>>> documentTuplesWithExternalViews = DocReader.makeDocandExternalPairsItr(INPUT_DATA_EXTERNAL_VIEW, extractorInputDocumentSchema, externalViewNameVsSchema);
		while(documentTuplesWithExternalViews.hasNext()) {
			
			Pair<Tuple, Map<String, TupleList>> documentTupleWithExternalViews = documentTuplesWithExternalViews.next();
			
			Tuple inputDocumentTuple = documentTupleWithExternalViews.first;
			Map<String, TupleList> externalViewTuples = documentTupleWithExternalViews.second;
			
			// Execute the extractor to yield all its output views, however, also passing in the external view's tuples, for the current input document tuple
			printExtractionResults(extractor.execute(inputDocumentTuple, null, externalViewTuples));
			
		}
		
	}
	
	public static void main(String[] args) {
		
		TextAnalyticsAPITutorial textAnalytics = new TextAnalyticsAPITutorial(); 
		
		try {
			
			// NOTE : Commented out method invocations exist for a reason. They can be uncommented as desired by the reader. More importantly, the reader is expected to also visit
			// such commented out methods to understand more about our runtime's APIs.
			
			// Compile each text analytics module inside this project
			//textAnalytics.compileTextAnalyticsModules();
			
			// Explain each compiled text analytics module
			//textAnalytics.explainTextAnalyticsModules();
			
			// Load and execute each compiled text analytics module across an input data set
			textAnalytics.loadAndExecuteTextAnalyticsModules();
			
			// Illustrate External views concept using our runtime APIs [DocReader's new utility methods]
			textAnalytics.illustrateExternalViewsViaDocReader();
			
		} catch (TextAnalyticsException textAnalyticsException) {
		  System.out.println(textAnalyticsException.getMessage());
		  textAnalyticsException.printStackTrace();
		} catch (Exception exception) {
			System.out.println(exception.getMessage());
			exception.printStackTrace();
		}

	}

}
