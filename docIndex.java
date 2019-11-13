import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.lang.Math;

import org.apache.commons.validator.routines.UrlValidator;
import javafx.util.Pair;
import edu.stanford.nlp.process.Stemmer;

public class docIndex {

	//static array list to store stop words in
	private static ArrayList<String> stopWords = new ArrayList<String>();

	//get the stop words and store them in stopWords(ArrayList)
	private static void readStopWords(String fileName){
		try(BufferedReader br = new BufferedReader(new FileReader(fileName))) {
    		for(String line; (line = br.readLine()) != null;){
        		stopWords.add(line);
    		}
		}catch(Exception e){
			System.out.println(e);
		}
	}

	//check if a given word is a stop word
	private static boolean isStopWord(String word){
		word = word.toLowerCase();
		if(stopWords.contains(word)){
			return true;
		}
		return false;
	}

	//list of documents without stop words (docID, document)
	private static ArrayList<Pair<String, List<String>>> documents = new ArrayList<Pair<String, List<String>>>();

	//read in the documents, clean them, and store them in a list of lists
	private static void readDocuments(String fileName){
		File file = new File(fileName);

		String docID = "";
		List<String> tempDocument = new ArrayList<String>();

		//to check whether the words are URLs, we don't want to include URLs
		UrlValidator validator = new UrlValidator(){
	        @Override
	        public boolean isValid(String value) {
	            return super.isValid(value) || super.isValid("http://" + value);
	        }
		};

  		if(file != null){

			//apply stemming
			Stemmer s = new Stemmer();

			//iterate through the collection
			try(BufferedReader br = new BufferedReader(new FileReader(file))) {

				//bool to know whether or not we're reading the document contents
				boolean readingText = false;

				//iterate through the document one line at a time
				for(String line; (line = br.readLine()) != null;){

					//remove the zero width space character "\u200B"
					line = line.replaceAll("[\\p{C}]", "");

					//start of a document's content
					if(line.contains("<TEXT>")){
						readingText = true;
						continue;
					}
					//end of a document's content
					else if(line.contains("</TEXT>")){
						readingText = false;
						continue;
					}
					//start of a new document
					else if(line.contains("<DOC>")){
						//instantiate a new temp document
						tempDocument = new ArrayList<String>();
						continue;
					}
					//end of the current document
					else if(line.contains("</DOC>")){
						//add the pair (docID, tempDocument) to documents
						Pair<String,List<String>> docIDDocPair = new Pair<String, List<String>>(docID, tempDocument);
						documents.add(docIDDocPair);
						continue;
					}
					//get the document ID
					else if(line.contains("<DOCNO>")){
						String[] currLine = line.split("\\s+");
						docID = currLine[1];
						continue;
					}

					//remove miscellaneous punctuation marks
					line = line.replaceAll(",", " ");
					line = line.replaceAll("\\.", "");
					line = line.replaceAll("_", " ");
					line = line.replaceAll(";", " ");
					line = line.replaceAll(":", " ");
					line = line.replaceAll("'", "");
					line = line.replaceAll("\"", " ");
					line = line.replaceAll("`", " ");

					//split hyphenated and slashed words
					line = line.replaceAll("/", " ");
					line = line.replaceAll("-", " ");

					//split the current line using whitespace as a delimiter
					String[] currLine = line.split("\\s+");

					//iterate through the current line and add any valid words to docIDDocPair(docID, tempDocument)
					for(int i = 0; i < currLine.length; ++i){

						if(!readingText){
							continue;
						}

						currLine[i] = currLine[i].toLowerCase();
						//don't include URLs
						if(validator.isValid(currLine[i])){
							continue;
						}

						//check that the current word isn't a stop word
						if(!isStopWord(currLine[i])){
							tempDocument.add(s.stem(currLine[i]));
						}
					}
	    		}
			}catch(Exception e){
				System.out.println(e);
			}
  		}
		else{
			System.out.println("ERROR: " + file.toString() + " does not exist");
  		}
	}

	//key: docID, value: # terms in document
	public static HashMap<String, Integer> docIndex = new HashMap<String, Integer>();

	public static void createDocIndex(ArrayList<Pair<String, List<String>>> documents){
		int numTerms;
		//iterate through the documents
		for(int i = 0; i < documents.size(); ++i){
			//get the number of terms in each document
			numTerms = documents.get(i).getValue().size();
			//put the docID and number of terms into the index
			docIndex.put(documents.get(i).getKey(), numTerms);
		}
	}

	//key: term, value: # documents with a point to a linked list of postings with docID/frequency
	public static HashMap<String, Pair<Integer, LinkedList<Pair<String,Integer>>>> termIndex = new HashMap<String, Pair<Integer, LinkedList<Pair<String,Integer>>>>();

	public static void createTermIndex(List<Pair<String, List<String>>> documents){
		for(int i = 0; i < documents.size(); ++i){
			for(int j = 0; j < documents.get(i).getValue().size(); ++j){

				String term = documents.get(i).getValue().get(j);

				//check whether the term is already in the term index
				Pair<Integer, LinkedList<Pair<String, Integer>>> hit = termIndex.get(term);

				//term is not in the index
				if(hit == null){
					//instantiate a new linked list and add the docID and docFreq pair
					String docID = documents.get(i).getKey();
					int docFreq = 1; //since this is the first time the term is hitting, docFreq is one
					Pair<String, Integer> docIDFreqPair = new Pair<>(docID, docFreq);
					LinkedList<Pair<String, Integer>> postings = new LinkedList<Pair<String, Integer>>();
					postings.add(docIDFreqPair);

					int numDocs = 1; //since this is the first time the term is hitting the doc, numDocs is one
					//instantiate a new pair with # docs and the linked list postings
					hit = new Pair<Integer, LinkedList<Pair<String, Integer>>>(numDocs, postings);
					//place the term and index into the term index
					termIndex.put(term, hit);
				}

				//term is in the index
				else{
					String currDocID = documents.get(i).getKey();

					boolean inPostings = false;
					LinkedList<Pair<String, Integer>> postings = hit.getValue();

					//iterate through the postings and check if we already have the document
					for(int k = 0; k < postings.size(); ++k){
    					String postingID = postings.get(k).getKey();
						//the term appears again in the same document
						if(currDocID == postingID){
							inPostings = true;
							//increase the document frequency by one
							int docFreq = postings.get(k).getValue() + 1;
							Pair<String, Integer> docIDFreqPair = new Pair<>(postingID, docFreq);
							postings.set(k, docIDFreqPair);
						}
					}
					//if the document is not apart of postings
					if(!inPostings){
						int numDocs = hit.getKey() + 1; //increase the number of docs the term is in
						int docFreq = 1; //since this is the first time the term is hitting, docFreq is one
						Pair<String, Integer> docIDFreqPair = new Pair<>(currDocID, docFreq);
						postings.add(docIDFreqPair);
						//update the number of documents
						Pair<Integer, LinkedList<Pair<String, Integer>>> hitUpdate = new Pair<Integer, LinkedList<Pair<String, Integer>>>(numDocs, postings);
						termIndex.put(term, hitUpdate);
					}
				}
			}
		}
	}

    public static void main(String[] args){

		//get the stop words
		readStopWords("stoplist.txt");

		//get the documents from the collection
		readDocuments("data/ap89_collection");

		//create a term index
		createTermIndex(documents);

		//create a document index
		createDocIndex(documents);

		//test our indexes
		Evaluate evaluate = new Evaluate();
		evaluate.startSearch(termIndex, docIndex, documents);
    }
}

class Evaluate {

	private static HashMap<String, Pair<Integer, LinkedList<Pair<String,Integer>>>> termIndex;
	private static HashMap<String, Integer> docIndex;

	public static double computeCosSim(ArrayList<Double> queryVec, ArrayList<Double> docVec){
		double numerator = 0.0;
		double denominator = 0.0;

		double docMagnitude = 0.0;
		double queryMagnitude = 0.0;

		//find the inner product
		for(int i = 0; i < queryVec.size(); ++i){
			numerator += queryVec.get(i) * docVec.get(i);
		}

		//to avoid divide by zero errors
		//in the event that none of the terms in the query are in the index
		//in the event that the idf is zero
		if(numerator == 0.0){
			return 0.0;
		}

		//compute the magnitude of queryVec
		for(int i = 0; i < queryVec.size(); ++i){
			queryMagnitude += queryVec.get(i) * queryVec.get(i);
		}
		queryMagnitude = Math.sqrt(queryMagnitude);

		//compute the magnitude of docVec
		for(int i = 0; i < docVec.size(); ++i){
			docMagnitude += docVec.get(i) * docVec.get(i);
		}
		docMagnitude = Math.sqrt(docMagnitude);

		denominator = queryMagnitude * docMagnitude;
		return numerator / denominator;
	}

	public static double cosSimHelper(String[] query, String docID){

		ArrayList<Double> queryVec = new ArrayList<Double>();

		//the dimension of the docVec will be the same as the number of terms in the query
		ArrayList<Double> docVec = new ArrayList<Double>();

		//to check if a query term is repeated
		ArrayList<String> repeat = new ArrayList<String>();

		//create the document vector which holds the term weightings
		for(int i = 0; i < query.length; ++i){

			repeat.add(query[i]);

			double tf_idf = 0.0;

			Pair<Integer, LinkedList<Pair<String, Integer>>> hit = termIndex.get(query[i]);
			//check if the term is in the index
			if(hit != null){
				LinkedList<Pair<String,Integer>> docs = termIndex.get(query[i]).getValue();
				for(int j = 0; j < docs.size(); ++j){
					if((docs.get(j).getKey()).equals(docID)){
						int docFreq = docs.get(j).getValue();
						int docLeng = docIndex.get(docID);
						double tf = computeTF(docFreq, docLeng);

						int totalNumDocs = docIndex.size();
						int numDocsWithTerm = docs.size();
						double idf = computeIDF(numDocsWithTerm, totalNumDocs);

						tf_idf = computeTF_IDF(tf, idf);
						break;
					}
				}
			}
			docVec.add(tf_idf);
		}

		//create the query vector which holds the term weightings
		for(int i = 0; i < query.length; ++i){
			if(Collections.frequency(repeat, query[i]) > 1){
				queryVec.add( ((double) Collections.frequency(repeat, query[i])) / ((double) (query.length)));
			}
			else{
				queryVec.add(1.0 / ((double) (query.length)));
			}
		}

		return computeCosSim(queryVec, docVec);
	}

	public static double computeTF(int numTerms, int totalNumTerms){
			double tf = (double) numTerms / (double) totalNumTerms;
			return tf;
	}

	public static double computeIDF(int numDocs, int totalNumDocs){
		//take the log of (total number of docs / number of docs with term 't')
		double idf = Math.log10( (double) totalNumDocs / (double) numDocs);
		return idf;
	}

	public static double computeTF_IDF(double tf, double idf){
		return tf * idf;
	}

	public static void startSearch(HashMap<String, Pair<Integer, LinkedList<Pair<String,Integer>>>> trmIdx, HashMap<String, Integer> dcIdx, ArrayList<Pair<String, List<String>>> documents){

		termIndex = trmIdx;
		docIndex = dcIdx;

		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		String query;

		Stemmer s = new Stemmer();

		//read in the queries
		File file = new File("data/query_list.txt");

		if(file != null){
			try(BufferedReader br = new BufferedReader(new FileReader(file))) {

				//iterate through the queries one by one
				for(String line; (line = br.readLine()) != null;){

					String[] temp = line.split("\\s+");
					String q = "";
					for(int i = 1; i < temp.length; ++i){
						q += temp[i] + " ";
					}

					String qNum = temp[0].replaceAll("\\.", "");

					//file to write the results to
					PrintWriter writer = new PrintWriter("results/" + qNum + "_rankings.txt", "UTF-8");

					//split hyphenated and slashed words
					q = q.replaceAll("/", " ");
					q = q.replaceAll("-", " ");

					//remove miscellaneous punctuation marks
					q = q.replaceAll(",", " ");
					q = q.replaceAll("\\.", "");
					q = q.replaceAll("_", " ");
					q = q.replaceAll(";", " ");
					q = q.replaceAll(":", " ");
					q = q.replaceAll("'", "");
					q = q.replaceAll("\"", " ");
					q = q.replaceAll("`", " ");

					q = q.toLowerCase();

					q = s.stem(q);

					//split the query using whitespace as a delimiter
					String[] terms = q.split("\\s+");

					//for holding all of the scores for a given query
					ArrayList<Pair<String, Double>> scoreList = new ArrayList<Pair<String, Double>>();

					//perform the queries on the collection
					for(int i = 0; i < documents.size(); ++i){
							Pair<String, Double> scores = new Pair<String, Double>(qNum + " Q0 " + documents.get(i).getKey() + " rank " + cosSimHelper(terms, documents.get(i).getKey()) + " Exp", cosSimHelper(terms, documents.get(i).getKey()));
							if(scores.getValue() != 0.0){
								scoreList.add(scores);
							}
					}

					//sort by CosSim score
					Collections.sort(scoreList, Comparator.comparing(p -> -p.getValue()));

					//format the scores
					for(int i = 0; i < scoreList.size(); ++i){
						String format = scoreList.get(i).getKey();
						format = format.replaceAll("rank", Integer.toString(i+1));

						//write the scores to their respective result file
						writer.println(format);
					}
					writer.close();
				}
			}catch(Exception e){
				System.out.println(e);
			}
		}

		while(true){
			try{
				System.out.print("Enter a search term(\"QUIT\" to quit): ");
				query = in.readLine();
				if(query.equals("QUIT")){
					break;
				}

				//split hyphenated and slashed words
				query = query.replaceAll("/", " ");
				query = query.replaceAll("-", " ");

				//remove miscellaneous punctuation marks
				query = query.replaceAll(",", " ");
				query = query.replaceAll("\\.", "");
				query = query.replaceAll("_", " ");
				query = query.replaceAll(";", " ");
				query = query.replaceAll(":", " ");
				query = query.replaceAll("'", "");
				query = query.replaceAll("\"", " ");
				query = query.replaceAll("`", " ");

				query = query.toLowerCase();

				query = s.stem(query);

				//split the query using whitespace as a delimiter
				String[] terms = query.split("\\s+");

				for(int i = 0; i < documents.size(); ++i){
					double ans = cosSimHelper(terms, documents.get(i).getKey());
					if(ans != 0.0){
						System.out.println(documents.get(i).getKey() + " CosSim: " + ans);
					}
				}

			}catch(Exception e){
				e.printStackTrace();
				continue;
			}
		}
	}
}
