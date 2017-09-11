package org.thunlp.lcy.anp.preprocess;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

public class IndexCreate {
	public Directory mDirectory;
	public IndexWriter mWriter;
	public Analyzer mAnalyzer;
	private String mIndexPath;// 表示索引目录位置

	public void InintialIndex(String indexPath) throws Exception {
		mIndexPath = indexPath;
		mDirectory = FSDirectory.open(new File(mIndexPath));
		// mAnalyzer = new SimpleAnalyzer(Version.LUCENE_45);
		mAnalyzer = new WhitespaceAnalyzer(Version.LUCENE_45);

		IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_45,
				mAnalyzer);
		mWriter = new IndexWriter(mDirectory, config);
	}

	public void CloseIndex() {
		try {
			if (mWriter != null) {
				mWriter.close();
				mDirectory.close();
			}
		} catch (Exception ex) {

		}
	}

	// sourceDir:要建立索引的文件所在目录 indexDir：索引所在目录 type：索引的类型“chinese”，“english”
	public void CreateIndex(String sourcePath, String indexPath)
			throws IOException {
		BufferedReader textReader = new BufferedReader(new InputStreamReader(new FileInputStream(sourcePath)));
		String line = textReader.readLine();
		int count = 1;

		while (line != null) {
			Document doc = new Document();
			doc.add(new Field("content", line, TextField.TYPE_STORED));
			try {
				mWriter.addDocument(doc);
			} catch (Exception ex) {
				System.out.println("write into index error");
			}
			line = textReader.readLine();

			// 显示进度
			if (count % 100000 == 0) {
				System.out.print(".");
			}
			count++;
		}
		System.out.println("Create index ok!");
		textReader.close();
	}

	public static void main(String[] args) {
		System.out.println(args[0]);
		System.out.println(args[1]);
		if (args.length < 2) {
			System.out.println("java -jar indexCreate.jar sourcePath destDir");
			return;
		}
		// String sourcePath="./data/poem.txt";
		// String indexPath= "./data/index";

		String sourcePath = args[0];
		String indexPath = args[1];
		IndexCreate indexCreate = new IndexCreate();
		try {
			indexCreate.InintialIndex(indexPath);
			indexCreate.CreateIndex(sourcePath, indexPath);
		} catch (Exception ex) {
			System.out.println(ex.toString());
		} finally {
			indexCreate.CloseIndex();
		}
	}

}
