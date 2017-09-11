package org.thunlp.lcy.anp.model;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;

import org.thunlp.lcy.anp.preprocess.MatchEvaluation;

public class LuceneTrainer {
	public static String dictFile = "data/dict/dict_tst_1000.txt";
	public static String cPhraseFile = "data/phrase/chinese_40000.txt";
	public static String ePhraseFile = "data/phrase/english_40000.txt";
	public static String cIndex = "data/index/cIndex40000",
			eIndex = "data/index/eIndex40000";
	public static double theta = Math.log(1e-60), alpha = 0.1,
			defaultProb = 1e-7;
	public static int iterNum = 10, topN = 10, queryNum = 3;
	public static int evaNum = 20000;
	
	Dictionary c2eDict, e2cDict;
	ArrayList<String> cPhrases, ePhrases;
	IBMModel1 c2eModel, e2cModel;
	MatchEvaluation evaluation = new MatchEvaluation();

	public static void main(String[] args) {
		String tag = "all";
		if(args.length == 7) {
			System.out.println("run "+ args[0]+" "+args[1]+" "+args[2]+" "+args[3]+" "+args[4]);
			dictFile = args[0];
			cPhraseFile = args[1];
			ePhraseFile = args[2];
			cIndex = args[3];
			eIndex = args[4];
			tag = args[5];
			evaNum = Integer.valueOf(args[6]);
		}
		
		long start = System.currentTimeMillis();
		LuceneTrainer trainer = new LuceneTrainer();
		
		if(tag.equals("all") || tag.equals("indep")) {
			theta = -60;
			trainer.load();
			System.out.println("independentTrain");
			trainer.independentTrain();
			System.out.println("------");
			System.out.println();
		}
		
		if(tag.equals("all") || tag.equals("inver")) {
			theta = -60;
			trainer.load();
			System.out.println("inverseTrain");
			trainer.inverseTrain();
			System.out.println("------");
			System.out.println();
		}
		
		if(tag.equals("all") || tag.equals("outer")) {
			theta = -80;
			trainer.load();
			System.out.println("outerAgreeTrain");
			trainer.outerAgreeTrain();
			System.out.println("------");
			System.out.println();
		}
		
		if(tag.equals("all") || tag.equals("inner")) {
			theta = -150;
			trainer.load();
			System.out.println("agreeTrain");
			trainer.agreeTrain();
			System.out.println("------");
			System.out.println();
		}
		
		System.out.println("time:"+(System.currentTimeMillis()-start));
	}

	public void loadPhrases(ArrayList<String> phrases, String phraseFile) {
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(
					new FileInputStream(phraseFile)));
			String line;
			while ((line = br.readLine()) != null) {
				phrases.add(line);
			}
			br.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void load() {
		c2eModel = new IBMModel1();
		e2cModel = new IBMModel1();

		c2eDict = new Dictionary();
		e2cDict = new Dictionary();
		c2eDict.load(dictFile, true);
		e2cDict.load(dictFile, false);

		c2eModel.initByDict(c2eDict);
		e2cModel.initByDict(e2cDict);

		cPhrases = new ArrayList<String>();
		ePhrases = new ArrayList<String>();
		loadPhrases(cPhrases, cPhraseFile);
		loadPhrases(ePhrases, ePhraseFile);
		System.out.println("load finished");
	}

	public void inverseTrain() {
		LuceneViterbiSearch c2eSearcher = new LuceneViterbiSearch(c2eModel, e2cModel,
				c2eDict, e2cDict);
		c2eSearcher.init(cIndex);

		for (int iter = 0; iter < iterNum; iter++) {
			System.out.println("iter:" + iter);
			ArrayList<String> cTrainPhrase = new ArrayList<String>();
			ArrayList<String> eTrainPhrase = new ArrayList<String>();
			ArrayList<Double> trainScore = new ArrayList<Double>();
			int count = 0;
			for (String ePhrase : ePhrases) {
				count++;
				if(count % 10000 == 0)
					System.out.print(".");
				String cPhrase = c2eSearcher.independentSearch(ePhrase);
				if (c2eSearcher.score > theta) {
					cTrainPhrase.add(cPhrase);
					eTrainPhrase.add(ePhrase);
					trainScore.add(c2eSearcher.score);
				}
			}
			System.out.println();
			c2eModel.independentTrain(cTrainPhrase, eTrainPhrase, c2eDict);
			try {
				PrintWriter pw1 = new PrintWriter(new OutputStreamWriter(
						new FileOutputStream("output/inver" + iter)));
				for (int i = 0; i < cTrainPhrase.size(); i++) {
					pw1.println(cTrainPhrase.get(i)+ " ||| "+eTrainPhrase.get(i)+" ||| "+trainScore.get(i));
				}
				pw1.close();
				evaluation.evaluate("output/inver" + iter);
			} catch (Exception e) {
				e.printStackTrace();
			}
			c2eModel.print("output/inverse.c2e"+iter);
		}
		
	}
	
	public void independentTrain() {
		LuceneViterbiSearch e2cSearcher = new LuceneViterbiSearch(e2cModel, c2eModel,
				e2cDict, c2eDict);
		e2cSearcher.init(eIndex);

		for (int iter = 0; iter < iterNum; iter++) {
			System.out.println("iter:" + iter);
			ArrayList<String> cTrainPhrase = new ArrayList<String>();
			ArrayList<String> eTrainPhrase = new ArrayList<String>();
			ArrayList<Double> trainScore = new ArrayList<Double>();
			int count = 0;
			for (String cPhrase : cPhrases) {
				count++;
				if(count % 10000 == 0)
					System.out.print(".");
				String ePhrase = e2cSearcher.independentSearch(cPhrase);
				if (e2cSearcher.score > theta) {
					cTrainPhrase.add(cPhrase);
					eTrainPhrase.add(ePhrase);
					trainScore.add(e2cSearcher.score);
				}
			}
			System.out.println();
			e2cModel.independentTrain(eTrainPhrase, cTrainPhrase, e2cDict);
			try {
				PrintWriter pw1 = new PrintWriter(new OutputStreamWriter(
						new FileOutputStream("output/indep" + iter)));
				for (int i = 0; i < cTrainPhrase.size(); i++) {
					pw1.println(cTrainPhrase.get(i)+ " ||| "+eTrainPhrase.get(i)+" ||| "+trainScore.get(i));
				}
				pw1.close();
				evaluation.evaluate("output/indep" + iter);
			} catch (Exception e) {
				e.printStackTrace();
			}
			e2cModel.print("output/indep.e2c"+iter);
		}
		
	}
	
	public void outerAgreeTrain() {
		LuceneViterbiSearch e2cSearcher = new LuceneViterbiSearch(e2cModel, c2eModel,
				e2cDict, c2eDict);
		LuceneViterbiSearch c2eSearcher = new LuceneViterbiSearch(c2eModel, e2cModel,
				c2eDict, e2cDict);
		e2cSearcher.init(eIndex);
		c2eSearcher.init(cIndex);

		for (int iter = 0; iter < iterNum; iter++) {
			System.out.println("iter:" + iter);
			//ArrayList<String> cTrainPhrase1 = new ArrayList<String>();
			//ArrayList<String> eTrainPhrase1 = new ArrayList<String>();
			//ArrayList<String> cTrainPhrase2 = new ArrayList<String>();
			//ArrayList<String> eTrainPhrase2 = new ArrayList<String>();
			ArrayList<String> cTrainPhrase = new ArrayList<String>();
			ArrayList<String> eTrainPhrase = new ArrayList<String>();
			ArrayList<Double> trainScore = new ArrayList<Double>();
			int count = 0;
			//int e2cCount = 0, c2eCount = 0, interCount = 0;
			for (String cPhrase : cPhrases) {
				count++;
				if(count % 10000 == 0)
					System.out.print(".");
				if(count % 500000 == 0)
					System.out.println();
				String ePhrase = e2cSearcher.independentSearch(cPhrase);
				if (e2cSearcher.score > theta) {
					String cPhrase_ = c2eSearcher.independentSearch(ePhrase);
					//cTrainPhrase1.add(cPhrase);
					//eTrainPhrase1.add(ePhrase);
					//e2cCount++;
					if (cPhrase_.equals(cPhrase)) {
						//cTrainPhrase1.add(cPhrase);
						//eTrainPhrase1.add(ePhrase);
						//cTrainPhrase2.add(cPhrase);
						//eTrainPhrase2.add(ePhrase);
						cTrainPhrase.add(cPhrase);
						eTrainPhrase.add(ePhrase);
						trainScore.add(e2cSearcher.score);
						//interCount++;
					}
				}
			}
			System.out.println();
			/*
			for (String ePhrase : ePhrases) {
				String cPhrase = c2eSearcher.independentSearch(ePhrase);
				if (c2eSearcher.score > theta) {
					//cTrainPhrase2.add(cPhrase);
					//eTrainPhrase2.add(ePhrase);
					c2eCount++;
				}
			}
			System.out.println(e2cCount+" "+c2eCount+" "+interCount+" "+(2.0*interCount/(e2cCount+c2eCount)));
			*/
			//c2eModel.independentTrain(cTrainPhrase2, eTrainPhrase2, c2eDict);
			//e2cModel.independentTrain(eTrainPhrase1, cTrainPhrase1, e2cDict);
			
			c2eModel.independentTrain(cTrainPhrase, eTrainPhrase, c2eDict);
			e2cModel.independentTrain(eTrainPhrase, cTrainPhrase, e2cDict);
			System.out.println(cTrainPhrase.size());
			try {
				PrintWriter pw1 = new PrintWriter(new OutputStreamWriter(
						new FileOutputStream("output/outer" + iter)));
				for (int i = 0; i < cTrainPhrase.size(); i++) {
					pw1.println(cTrainPhrase.get(i)+ " ||| "+eTrainPhrase.get(i)+" ||| "+trainScore.get(i));
				}
				pw1.close();
				evaluation.evaluate("output/outer" + iter);
			} catch (Exception e) {
				e.printStackTrace();
			}
			e2cModel.print("output/outer.e2c"+iter);
			c2eModel.print("output/outer.c2e"+iter);
		}
		
	}

	public void agreeTrain() {
		LuceneViterbiSearch e2cSearcher = new LuceneViterbiSearch(e2cModel, c2eModel,
				e2cDict, c2eDict);
		LuceneViterbiSearch c2eSearcher = new LuceneViterbiSearch(c2eModel, e2cModel,
				c2eDict, e2cDict);
		e2cSearcher.init(eIndex);
		c2eSearcher.init(cIndex);

		for (int iter = 0; iter < iterNum; iter++) {
			System.out.println("iter:" + iter);
			ArrayList<String> cTrainPhrase = new ArrayList<String>();
			ArrayList<String> eTrainPhrase = new ArrayList<String>();
			ArrayList<Double> trainScore = new ArrayList<Double>();
			int count = 0;
			//int e2cCount = 0, c2eCount = 0, interCount = 0;
			for (String cPhrase : cPhrases) {
				count++;
				if(count % 10000 == 0)
					System.out.print(".");
				String ePhrase = e2cSearcher.agreeSearch(cPhrase);
				if (e2cSearcher.score > theta) {
					//e2cCount++;
					String cPhrase_ = c2eSearcher.agreeSearch(ePhrase);
					if (cPhrase_.equals(cPhrase)) {
						cTrainPhrase.add(cPhrase);
						eTrainPhrase.add(ePhrase);
						trainScore.add(e2cSearcher.score);
						//interCount++;
					}
				}
			}
			System.out.println();
			/*
			for (String ePhrase : ePhrases) {
				String cPhrase = c2eSearcher.agreeSearch(ePhrase);
				if (c2eSearcher.score > theta)
					c2eCount++;
			}
			System.out.println(e2cCount+" "+c2eCount+" "+interCount+" "+(2.0*interCount/(e2cCount+c2eCount)));
			*/
			c2eModel.agreeTrain(cTrainPhrase, eTrainPhrase, c2eDict, e2cModel, e2cDict);
			e2cModel.agreeTrain(eTrainPhrase, cTrainPhrase, e2cDict, c2eModel, c2eDict);
			c2eModel.normalize();
			e2cModel.normalize();
			try {
				PrintWriter pw1 = new PrintWriter(new OutputStreamWriter(
						new FileOutputStream("output/agree" + iter)));
				for (int i = 0; i < cTrainPhrase.size(); i++) {
					pw1.println(cTrainPhrase.get(i)+ " ||| "+eTrainPhrase.get(i)+" ||| "+trainScore.get(i));
				}
				pw1.close();
				evaluation.evaluate("output/agree" + iter);
			} catch (Exception e) {
				e.printStackTrace();
			}
			e2cModel.print("output/agree.e2c"+iter);
			c2eModel.print("output/agree.c2e"+iter);
		}
		
	}
}
