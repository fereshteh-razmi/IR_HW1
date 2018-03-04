/*
 * Licensed to the Apache Software Foundation (ASF) 
 */

import java.io.File;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;


/** Index all text files under a directory.
 * This is a command-line application demonstrating simple Lucene indexing.
 * Run it with no command-line arguments for usage information.
 */
public class IndexFiles {
  
  private static int count_indexedFiles = 0;
  private IndexFiles() {}

  /** Index all text files under a directory. 
 * @throws ParserConfigurationException 
 * @throws SAXException */
  public static void main(String[] args) throws SAXException, ParserConfigurationException {
    String usage = "java org.apache.lucene.demo.IndexFiles"
                 + " [-index INDEX_PATH] [-docs DOCS_PATH] [-update]\n\n"
                 + "This indexes the documents in DOCS_PATH, creating a Lucene index"
                 + "in INDEX_PATH that can be searched with SearchFiles";
    String indexPath = "index";
    String docsPath = null;
    boolean create = true;
    for(int i=0;i<args.length;i++) {
      if ("-index".equals(args[i])) {
        indexPath = args[i+1];
        i++;
      } else if ("-docs".equals(args[i])) {
        docsPath = args[i+1];
        i++;
      } else if ("-update".equals(args[i])) {
        create = false;
      }
    }

    if (docsPath == null) {
      System.err.println("Usage: " + usage);
      System.exit(1);
    }

    final Path docDir = Paths.get(docsPath);
    if (!Files.isReadable(docDir)) {
      System.out.println("Document directory '" +docDir.toAbsolutePath()+ "' does not exist or is not readable, please check the path");
      System.exit(1);
    }
    
    Date start = new Date();
    try {
      System.out.println("Indexing to directory '" + indexPath + "'...");

      Directory dir = FSDirectory.open(Paths.get(indexPath));
      Analyzer analyzer = new StandardAnalyzer();
      IndexWriterConfig iwc = new IndexWriterConfig(analyzer);

      if (create) {
        // Create a new index in the directory, removing any
        // previously indexed documents:
        iwc.setOpenMode(OpenMode.CREATE);
      } else {
        // Add new documents to an existing index:
        iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);
      }

      //extract map for category to real topic
      
      IndexWriter writer = new IndexWriter(dir, iwc);
      indexDocs(writer, docDir);
      writer.close();

      Date end = new Date();
      System.out.println(end.getTime() - start.getTime() + " total milliseconds");

    } catch (IOException e) {
      System.out.println(" caught a " + e.getClass() +
       "\n with message: " + e.getMessage());
    }
  }

  /**
   * Indexes the given file using the given writer, or if a directory is given,
   * recurses over files and directories found under the given directory.
   * 
   * NOTE: This method indexes one document per input file.  This is slow.  For good
   * throughput, put multiple documents into your input file(s).  An example of this is
   * in the benchmark module, which can create "line doc" files, one document per line,
   * using the
   * <a href="../../../../../contrib-benchmark/org/apache/lucene/benchmark/byTask/tasks/WriteLineDocTask.html"
   * >WriteLineDocTask</a>.
   *  
   * @param writer Writer to the index where the given file/dir info will be stored
   * @param path The file to index, or the directory to recurse into to find files to index
   * @throws IOException If there is a low-level I/O error
 * @throws ParserConfigurationException 
 * @throws SAXException 
   */
  static void indexDocs(final IndexWriter writer, Path path) throws IOException {
    if (Files.isDirectory(path)) {
      Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        		indexDoc(writer, file, attrs.lastModifiedTime().toMillis());
        		return FileVisitResult.CONTINUE;
        }
      });
    } else {
      indexDoc(writer, path, Files.getLastModifiedTime(path).toMillis());
    }
  }

  /** Indexes a single document */
  static void indexDoc(IndexWriter writer, Path file, long lastModified) {
    try{
    		
      // make a new, empty document
      Document doc = new Document();
      
      // save file path as a searchable but not tokenizable  word
      Field pathField = new StringField("path", file.toString(), Field.Store.YES);
      doc.add(pathField);
      
      // Map news topic code to real category
		File codefile = new File("topic_codes.txt");
		Scanner input = new Scanner(codefile);
		Map<String, String> map = new HashMap<String, String>();
		int line_number = 0;
		while (input.hasNextLine()) {
			line_number++;
			String curr_line = input.nextLine();
		    String[] tokens = curr_line.split("\\t"); 	    
		    	if(tokens.length > 1 && line_number > 2) {
		    	    map.put(tokens[0], tokens[1]);
			}
		}
		input.close();
		
      
      // Override a SAX handler in order to read XML tages
      File inputFile = new File(file.toString());
      SAXParserFactory factory = SAXParserFactory.newInstance();
      SAXParser saxParser = factory.newSAXParser();
      SAXHandler userhandler = new SAXHandler();
      saxParser.parse(inputFile, userhandler); 

      // change date format to yyyyMMdd so we can search it by range
      SimpleDateFormat formatter=new SimpleDateFormat("yyyy-MM-dd");
      Date date=formatter.parse(userhandler.getDoc().date);
      formatter.applyPattern("yyyyMMdd");
      String newDateString = formatter.format(date);
      
      String category = "";
      if(map.get(userhandler.getDoc().category) == null) {
    	  		category = "";
      }else {
    	  		category = map.get(userhandler.getDoc().category);
      }
      
      // all are tokenized and indexed but text won't be saved
      String title_text = userhandler.getDoc().title + userhandler.getDoc().text;
      //System.out.println(title_text);
      doc.add(new TextField("text", new StringReader(title_text)));
      doc.add(new TextField("title", userhandler.getDoc().title,Field.Store.YES));
      doc.add(new TextField("body", new StringReader(userhandler.getDoc().text)));
      doc.add(new TextField("date", userhandler.getDoc().date,Field.Store.YES));
      doc.add(new TextField("category", category, Field.Store.YES));      
      doc.add(new TextField("date_range", newDateString,Field.Store.YES)); //time:[19970815 TO 19970815]
      
      //every 1000th time, print the file name
	  count_indexedFiles++; 
      if (writer.getConfig().getOpenMode() == OpenMode.CREATE) {
        // New index, so we just add the document (no old document can be there):
	    	  if((count_indexedFiles%1000) == 0)
	   		   System.out.println(Integer.toString(count_indexedFiles/1000) + ", adding " + file);
          writer.addDocument(doc);
      } else {
        // Existing index (an old copy of this document may have been indexed) so 
        // we use updateDocument instead to replace the old one matching the exact 
        // path, if present:
	    	  if((count_indexedFiles%1000) == 0)
	   		   System.out.println(Integer.toString(count_indexedFiles/1000) + ", updating " + file);
           writer.updateDocument(new Term("path", file.toString()), doc);
      }
    }catch (Exception e) {
    		System.err.println(file.toString());
        e.printStackTrace();
    }
  }
}


/******************* XML READER  ****************/


class SAXHandler extends DefaultHandler {
	 
	ReutersDoc r_doc = null;
	String content = null;
	Boolean in_text_tag = false;
	Boolean in_codes_tag = false; //Codes tag related to CATEGORY of the news
	String supercontent = new String();
	
	//getter method for employee list
    public ReutersDoc getDoc() {
        return r_doc;
    }
	
  @Override
  //Triggered when the start of tag is found.
  public void startElement(String uri, String localName, 
                           String qName, Attributes attributes) 
                           throws SAXException {
    supercontent = "";   
    switch(qName){
      //Create a new Employee object when the start tag is found
      case "newsitem":
        r_doc = new ReutersDoc();
        r_doc.date = attributes.getValue("date");
        break;
      case "text":
    	  	in_text_tag = true;
    	  	break;
      case "codes":
    	  	if(attributes.getValue("class").equals("bip:topics:1.0"))
    	  		in_codes_tag = true;
    	  	break;
      case "code":
  	  	if(in_codes_tag)
  	  		r_doc.category = attributes.getValue("code");
  	  		in_codes_tag = false; //If don't do it here, we get the second category (from 2nd code tag)
  	  	break;
    }
  }
 
  @Override
  public void endElement(String uri, String localName, 
                         String qName) throws SAXException {

	  switch(qName){
     case "title":
    	 	r_doc.title = content;
    	 	break;
     case "p":
    	 	if(in_text_tag = true)
    	 		r_doc.text = r_doc.text + "\n" + supercontent;
 	 	break;
     case "text":
    	 	in_text_tag = false;
    	 	break;
     case "category":
    	 	r_doc.category = content;
       break;
     case "codes":
    	 	in_codes_tag = false; 
    	 	break;

   }
  }
 
  @Override
  public void characters(char[] ch, int start, int length) 
          throws SAXException {
    content = String.copyValueOf(ch, start, length).trim();
    supercontent = supercontent + content;
  }
     
}
 
class ReutersDoc {
 
  String title = new String("");
  String text = new String("");
  String date = new String("");
  String category = new String("");
 
  @Override
  public String toString() {
    return "title: " + title + "\ntext: " + text + " \ndate: " + date + " \ncategory: " + category;
  }
}
