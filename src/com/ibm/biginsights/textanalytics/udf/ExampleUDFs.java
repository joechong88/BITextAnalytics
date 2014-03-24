/**
 * IBM Confidential 
 * OCO Source Materials 
 * 5725-C09
 * (C) Copyright IBM Corp. 2011 
 * The source code for this program is not published or otherwise divested of its trade secrets, 
 * irrespective of what has been deposited with the U.S. Copyright Office.
 */
package com.ibm.biginsights.textanalytics.udf;

import com.ibm.avatar.algebra.datamodel.Span;

/**
 * 
 * Sample BigInsights Text Analytics AQL user-defined functions.
 * 
 * To use the UDF functions defined in this class in your BigInsights Text Analytics project:
 * 1) Export this class as a jar
 * 2) Copy the jar in a directory on the data path of your project
 * 3) Invoke the desired UDFs from your AQL code
 * 
 * A pre-built jar generated from this class is in TextAnalytics/aql/udfjars/exampleUDFs.jar
 * An example of how to invoke UDFs from your AQL code is in TextAnalytics/aql/main.aql
 * 
 * Related information: Chapter "User-Defined Functions" in Annotation Query Language (AQL) Reference
 *
 */
public class ExampleUDFs {
	
  /**
   * Example UDF that returns the all lower-case version of the input string.
   * @param s input string
   * @return
   */
	public String toLowerCase(String s)  {
		return s.toLowerCase();
	}

	/**
   * Example UDF that returns the all upper-case version of the text of the input span.
   * Demonstrates the API for accessing the text content of a <code>Span</code> object.
   * @param s input span
   * @return
   */
	public String toUpperCase(Span s) {
		return s.getText().toUpperCase();
	}
	
	/**
	 * Example UDF that returns a formatted string representation of the input parameters.
	 * @param p1
	 * @param p2
	 * @param p3
	 * @param p4
	 * @return
	 */
	public String f1(Integer p1, Integer p2, String p3, String p4) {
		return String.format("%s_%s_%s_%s", p1, p2, p3, p4);
	}
	
	/**
	 * Example UDF with no arguments. Always returns the same string. 
	 * @return
	 */
	public String f1() {
		return "F1";
	}
	
	/**
	 * UDF version of the AQL scalar built-in function <code>CombineSpans(span, span)</code>.
	 * Demonstrates the APIs for accessing the offsets of a <code>Span</code> object,
	 * as well as constructing new <code>Span</code> objects.
	 * @param firstSpan
	 * @param secondSpan
	 * @return a new span that starts at the beginning of the first input spans, 
	 *         and ends at the end of the second span
	 */
	public Span combineSpans(Span firstSpan, Span secondSpan) {
	  
	  // Obtain the begin offset of the first span
		int begin = firstSpan.getBegin();
		
		// Obtain the end offset of the first span
		int end = secondSpan.getEnd();

		if (begin > end) {
			throw new RuntimeException(String.format(
					"Arguments to CombineSpans must be in left-to-right order"
							+ " (Input spans were %s and %s, respectively)",
					firstSpan, secondSpan));
		}

		// Construct a new span over the underlying text object of the input span
		Span ret = Span.makeBaseSpan(firstSpan, begin, end);

		// Propagate cached token offsets, if available.
		ret.setBeginTok(firstSpan.getBeginTok());
		ret.setEndTok(secondSpan.getEndTok());

		return ret; 
	}
	
	/**
	 * Example UDF that returns <code>true</code> if the span is greater than the specified size.
	 * @param span
	 * @param size
	 * @return
	 */
	 public Boolean spanGreaterThan(Span span, Integer size) {
	    if ((span.getEnd() - span.getBegin()) > size)
	      return true;
	    return false;
	  }

}
