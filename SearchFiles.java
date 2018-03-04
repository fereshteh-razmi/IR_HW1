/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
//package org.apache.lucene.demo;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.search.similarities.LMJelinekMercerSimilarity;
import org.apache.lucene.search.similarities.LMSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

import java.util.HashSet;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.CollectionStatistics;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.TermStatistics;

/** Simple command-line based search demo. */
public class SearchFiles {
		
  private SearchFiles() {}
  private static int vocabulary_size = 0;
  
  /** Simple command-line based search demo. */
  public static void main(String[] args) throws Exception {
	  
    String index_path = "index";
    String query_path = "query.txt";
    String output_path = "output.txt";
    String my_login_id = "frazmim";
    int numofMaxRetreivedDocs = 100;
    String search_type = "BM25";
    String field = "Text";    /////// it's Text not text
    
    //  if ("-index".equals(args[i])) {
    //    index = args[i+1];
    
    IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(index_path)));
    IndexSearcher searcher = new IndexSearcher(reader);
    Analyzer analyzer = new StandardAnalyzer();
    QueryParser parser = new QueryParser(field, analyzer);
    
    vocabulary_size = vocabSize(reader); /// calculate vocabulary size
    
    File file = new File(query_path);
	FileReader fileReader = new FileReader(file);
	BufferedReader bufferedReader = new BufferedReader(fileReader);
	String line;
	while ((line = bufferedReader.readLine()) != null) {
		line = line.trim();
		if (line.length() == 0) 
			break;
		Query query = parser.parse(line);
		System.out.println("Searching for: " + query.toString(field));
		switch (search_type) {
			case "BM25":
				float k1 = 1.2F;
				float b = 0.75F;
				BM25Similarity sim = new BM25Similarity(k1, b);
				searcher.setSimilarity(sim);
				TopDocs results = searcher.search(query, numofMaxRetreivedDocs); /////Search here 1
				ScoreDoc[] hits = results.scoreDocs;
			    	for (int i = 0; i < hits.length; i++) {
			    		Document doc = searcher.doc(hits[i].doc);
			    		String DocId = doc.get("DocId"); 
			        System.out.println((i+1) + ". " + DocId + ", " + hits[i].score);
			        System.out.println(doc.get("Title"));
			    	}
				break;
			case "LMLaplace":
				LaplanceLM sim_ = new LaplanceLM(vocabulary_size);
			    searcher.setSimilarity(sim_);
				TopDocs results_ = searcher.search(query, numofMaxRetreivedDocs); /////Search here 1
				ScoreDoc[] hits_ = results_.scoreDocs;
			    	for (int i = 0; i < hits_.length; i++) {
			    		Document doc = searcher.doc(hits_[i].doc);
			    		String DocId = doc.get("DocId"); 
			        System.out.println((i+1) + ". " + DocId + ", " + hits_[i].score);
			        System.out.println(doc.get("Title"));
			    	}
				break;
			case "RM1":
				float mu1 = 0.2F; ///????DrichletLM interpolation
				RM1 rm1_searcher = new RM1(searcher, reader, 50, numofMaxRetreivedDocs, 20);//topDoc,rankedDoc,#newQueryTerms
				ArrayList<DocStats> ranked_docs = rm1_searcher.search(mu1, query);
				int rank = 1;
				for (DocStats rd : ranked_docs) {
					System.out.println(rank + ". " + rd.DocId + ", " + rd.score);
					System.out.println(searcher.doc(rd.doc_index).get("Title"));
					rank ++;	
				}
				break;
			case "RM3":
				float mu_ = 0.2F; //DrichletLM interpolation
				float lambda = 0.9F; //RM3 interpolation, product of P_RM1
				RM3 rm3_searcher = new RM3(searcher, reader, 50, numofMaxRetreivedDocs, 20);//topDoc,rankedDoc,#newQueryTerms
				ArrayList<DocStats> ranked_docs_ = rm3_searcher.search(lambda, mu_, query);
				int rank_ = 1;
				for (DocStats rd : ranked_docs_) {
					System.out.println(rank_ + ". " + rd.DocId + ", " + rd.score);
					System.out.println(searcher.doc(rd.doc_index).get("Title"));
					rank_ ++;	
				}
				break;
			default:
				throw new IllegalArgumentException("Invalid search type " + search_type);		
		}
			
		
	}
	fileReader.close();

  } 
  
  
  // To calculate Vocabulary Size
	public static int vocabSize(IndexReader reader) throws IOException {
		Fields fds = MultiFields.getFields(reader);
		Terms tms = fds.terms("Text"); 
		TermsEnum itr = tms.iterator();
		BytesRef byteRef = null;
		HashSet<String> set = new HashSet<>();
		while((byteRef = itr.next()) != null) {
			set.add(byteRef.utf8ToString());
		}
		int vocabSize = set.size();
		return vocabSize;
	}
	
}
