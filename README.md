Written using JDK 8

To compile

```javac -cp .:commons-validator-1.6.jar:javafx.base-8.60.11.jar:stanford-corenlp-3.9.2.jar docIndex.java```

To run

```java -cp .:commons-validator-1.6.jar:javafx.base-8.60.11.jar:stanford-corenlp-3.9.2.jar docIndex```

To check precision/recall for a given query

```perl data/trec_eval.pl -q data/qrels.txt results/xx_rankings.txt```

where ```xx``` is a query number.

This program determines the term frequency, inverse document frequency, and the TF-IDF of a given set of terms in a collection of given documents.
It first reads in any stopwords provided by the user in a stoplist.txt file. It then reads in the document collection, applying stemming, and builds two inverted indexes from the given collection. One being a term index and the other being a document index.

The program allows a user to search any number of terms present in the term index. It will then use the two indexes to perform all necessary calculations to determine the term weighting, term frequency, inverse document frequency, and the TF-IDF. The search is very efficient as it uses a hash map to perform the query on the index, resulting in O(1) lookup time.

The program will then calculate the cosine similarity measure between the given query and each returned relevant documents.

The program firstly calculates the CosSim measure for the queries in data/query_list.txt. It then writes the results to a results/ folder.

Following that, it will take in user input.
