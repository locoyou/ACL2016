package org.thunlp.lcy.anp.preprocess;

import java.io.File;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.thunlp.lcy.anp.model.LuceneTrainer;

public class LuceneTest {
	public Analyzer analyzer;
	public DirectoryReader directoryReader;
	public IndexSearcher searcher;
	
	public static void main(String[] args) {
		LuceneTest t = new LuceneTest(args[0]);
		long start = System.currentTimeMillis();
		t.test(10000, 10);
		System.out.println("time:"+(System.currentTimeMillis()-start)/1000.0);
	}
	
	public LuceneTest(String indexPath) {
		try {
			Directory directory = FSDirectory.open(new File(indexPath));
			directoryReader = DirectoryReader.open(directory);
			searcher = new IndexSearcher(directoryReader);
			analyzer = new WhitespaceAnalyzer(Version.LUCENE_45);
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public int test(int count, int topN) {
		QueryParser queryParser = new QueryParser(Version.LUCENE_45,
				"content", analyzer);
		try {
			int ans = 0;
			for(int i = 0; i < count; i++) {
				Query query = queryParser.parse("Test query for this index");
				TopDocs topDocs = searcher.search(query, LuceneTrainer.topN);
				ans += topDocs.totalHits; 
			}
			return ans;
		}
		catch(Exception e) {
			e.printStackTrace();
			return 0;
		}
	}
}
