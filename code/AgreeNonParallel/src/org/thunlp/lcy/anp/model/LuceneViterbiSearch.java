package org.thunlp.lcy.anp.model;

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

public class LuceneViterbiSearch {
	public double score = 0.0;
	public Analyzer analyzer;
	public DirectoryReader directoryReader;
	public IndexSearcher searcher;
	IBMModel1 s2tModel, t2sModel;
	Dictionary s2tDict, t2sDict;
	double[] powMem = new double[100];

	public void init(String indexPath) {
		try {
			Directory directory = FSDirectory.open(new File(indexPath));
			directoryReader = DirectoryReader.open(directory);
			searcher = new IndexSearcher(directoryReader);
			analyzer = new WhitespaceAnalyzer(Version.LUCENE_45);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public LuceneViterbiSearch(IBMModel1 s2tModel, IBMModel1 t2sModel,
			Dictionary s2tDict, Dictionary t2sDict) {
		this.s2tModel = s2tModel;
		this.t2sModel = t2sModel;
		this.s2tDict = s2tDict;
		this.t2sDict = t2sDict;
		for(int i = 0; i < 10; i++) {
			for(int j = 0; j < 10; j++) {
				powMem[i*10+j] = Math.log(Math.pow(i+1, j));
			}
		}
	}
	
	public double getPow(int i, int j) {
		return powMem[i*10+j];
	}

	public String getQuery(String tp) {
		String sp = "";
		String[] tags = tp.split(" ");
		for (String t : tags) {
			if (s2tModel.transProbInverse.containsKey(t)) {
				for (String s : s2tModel.transProbInverse.get(t).keySet()) {
					if (s2tModel.transProbInverse.get(t).get(s) > LuceneTrainer.alpha) {
						sp += s + " ";
					}
				}
			}
		}
		return sp;
	}

	public double getS2TScore(String sp, String tp) {
		double scoreIBM = 0.0;
		String[] sWords = (sp + " #null").split(" ");
		String[] tWords = tp.split(" ");
		int sl = sWords.length - 1, tl = tWords.length;
		for (String tWord : tWords) {
			double tmp = 0.0;
			for (String sWord : sWords) {
				if (s2tModel.transProb.containsKey(sWord)
						&& s2tModel.transProb.get(sWord).containsKey(tWord)) {
					tmp += s2tModel.transProb.get(sWord).get(tWord);
				} else {
						tmp += LuceneTrainer.defaultProb;
				}
			}
			scoreIBM += Math.log(tmp);
		}
		scoreIBM += Math.log(s2tModel.getLengthProb(sl, tl)) - getPow(sl, tl);
		return scoreIBM;
	}

	public double getT2SScore(String sp, String tp) {
		double scoreIBM = 0.0;
		String[] sWords = sp.split(" ");
		String[] tWords = (tp + " #null").split(" ");
		int sl = sWords.length, tl = tWords.length - 1;
		for (String sWord : sWords) {
			double tmp = 0.0;
			for (String tWord : tWords) {
				if (t2sModel.transProb.containsKey(tWord)
						&& t2sModel.transProb.get(tWord).containsKey(sWord)) {
					tmp += t2sModel.transProb.get(tWord).get(sWord);
				} else {
						tmp += LuceneTrainer.defaultProb;
				}
			}
			scoreIBM += Math.log(tmp);
		}
		scoreIBM += Math.log(t2sModel.getLengthProb(tl, sl)) - getPow(tl, sl);
		return scoreIBM;
	}
	
	public double getAlignScore(String sp, String tp) {
		double scoreAlign = 0.0;
		String[] sWords = (sp + " #null").split(" ");
		String[] tWords = (tp + " #null").split(" ");
		int sl = sWords.length - 1, tl = tWords.length - 1;
		for (String tWord : tWords) {
			if(tWord.equals("#null"))
				continue;
			double tmp1 = LuceneTrainer.defaultProb, tmp2 = LuceneTrainer.defaultProb;
			//double tmp = tmp1 * tmp2;
			double tmp = 0.0;
			for (String sWord : sWords) {
				if (s2tModel.transProb.containsKey(sWord)
						&& s2tModel.transProb.get(sWord).containsKey(tWord)) {
						tmp1 = s2tModel.transProb.get(sWord).get(tWord);
				}
				else {
					tmp1 = LuceneTrainer.defaultProb;
				}
				if (t2sModel.transProb.containsKey(tWord)
						&& t2sModel.transProb.get(tWord).containsKey(sWord)) {
					tmp2 = t2sModel.transProb.get(tWord).get(sWord);
				}
				else {
					tmp2 = LuceneTrainer.defaultProb;
				}
				/*
				if(tmp1*tmp2 > tmp) {
					tmp = tmp1*tmp2;
				}
				*/
				tmp += tmp1*tmp2;
			}
			scoreAlign += Math.log(tmp);
		}
		
		//scoreAlign += Math.log(s2tModel.getLengthProb(sl, tl)) + Math.log(t2sModel.getLengthProb(tl, sl));
		scoreAlign += Math.log(t2sModel.getLengthProb(tl, sl)) - getPow(tl, sl);
		return scoreAlign;
	}

	public String agreeSearch(String tp) {
		try {
			String sp = "";
			double maxScore = -10000;
			QueryParser queryParser = new QueryParser(Version.LUCENE_45,
					"content", analyzer);
			String queryStr = getQuery(tp).trim();
			if (queryStr.length() == 0) {
				score = -10000;
				return "";
			}
			queryStr = QueryParser.escape(queryStr + " " + tp).trim();
			Query query = queryParser.parse(queryStr);
			TopDocs topDocs = searcher.search(query, LuceneTrainer.topN);
			for (int i = 0; i < topDocs.scoreDocs.length; i++) {
				int docId = topDocs.scoreDocs[i].doc;
				String sp_ = searcher.doc(docId).getValues("content")[0];
				if (sp_.length() == 0)
					continue;
				//double score_ = getS2TScore(sp_, tp) + getT2SScore(sp_, tp);
				double score_ = getAlignScore(sp_, tp);
				if (score_ > maxScore) {
					sp = sp_;
					maxScore = score_;
				}
			}
			score = maxScore;
			return sp;
		} catch (Exception e) {
			e.printStackTrace();
			return "";
		}
	}

	public String independentSearch(String tp) {
		try {
			String sp = "";
			double maxScore = -10000;
			score = -10000;
			QueryParser queryParser = new QueryParser(Version.LUCENE_45,
					"content", analyzer);
			String queryStr = getQuery(tp).trim();
			if (queryStr.length() == 0) {
				score = -10000;
				return "";
			}
			queryStr = QueryParser.escape(queryStr + " " + tp).trim();
			Query query = queryParser.parse(queryStr);
			TopDocs topDocs = searcher.search(query, LuceneTrainer.topN);
			
			for (int i = 0; i < topDocs.scoreDocs.length; i++) {
				int docId = topDocs.scoreDocs[i].doc;
				String sp_ = searcher.doc(docId).getValues("content")[0];
				if (sp_.length() == 0)
					continue;
				double score_ = getS2TScore(sp_, tp);
				if (score_ > maxScore) {
					sp = sp_;
					maxScore = score_;
				}
			}
			
			score = maxScore;
			return sp;
		} catch (Exception e) {
			e.printStackTrace();
			return "";
		}
	}
}
