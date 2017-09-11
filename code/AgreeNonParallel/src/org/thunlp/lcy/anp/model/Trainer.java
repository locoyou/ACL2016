package org.thunlp.lcy.anp.model;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;

import org.thunlp.lcy.anp.preprocess.MatchEvaluation;

public class Trainer {
	public static String dictFile = "data/dict/dict_tst_1000.txt";
	public static String cPhraseFile = "data/phrase/chinese_20000.txt";
	public static String ePhraseFile = "data/phrase/english_40000.txt";
	public static String cIndex = "data/index/cIndex",
			eIndex = "data/index/eIndex";
	public static double theta = Math.log(1e-60), alpha = 0.1,
			defaultProb = 1e-7;
	public static int iterNum = 10, topN = 10, queryNum = 3;

	Dictionary c2eDict, e2cDict;
	ArrayList<String> cPhrases, ePhrases;
	IBMModel1 c2eModel, e2cModel;
	MatchEvaluation evaluation = new MatchEvaluation();

	public static void main(String[] args) {
		if(args.length == 5) {
			System.out.println("run "+ args[0]+" "+args[1]+" "+args[2]+" "+args[3]+" "+args[4]);
			dictFile = args[0];
			cPhraseFile = args[1];
			ePhraseFile = args[2];
			cIndex = args[3];
			eIndex = args[4];
		}
		
		long start = System.currentTimeMillis();
		Trainer trainer = new Trainer();
		/*
		trainer.load();
		System.out.println("outerAgreeTrain");
		trainer.outerAgreeTrain();
		System.out.println("------");
		System.out.println();

		trainer.load();
		System.out.println("agreeTrain");
		trainer.agreeTrain();
		System.out.println("------");
		System.out.println();
		*/
		trainer.load();
		System.out.println("independentTrain");
		trainer.independentTrain();	
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

	public void outerAgreeTrain() {
		ViterbiSearch e2cSearcher = new ViterbiSearch(e2cModel, c2eModel,
				e2cDict, c2eDict);
		ViterbiSearch c2eSearcher = new ViterbiSearch(c2eModel, e2cModel,
				c2eDict, e2cDict);
		e2cSearcher.init(ePhrases);
		c2eSearcher.init(cPhrases);

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
				if(count % 5000000 == 0)
					System.out.println();
				String ePhrase = e2cSearcher.independentSearch(cPhrase);
				if (e2cSearcher.score > theta) {
					String cPhrase_ = c2eSearcher.independentSearch(ePhrase);
					if (cPhrase_.equals(cPhrase)) {
						cTrainPhrase.add(cPhrase);
						eTrainPhrase.add(ePhrase);
						trainScore.add(e2cSearcher.score);
					}
				}
			}
			System.out.println();
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
		}
	}

	public void independentTrain() {
		ViterbiSearch e2cSearcher = new ViterbiSearch(e2cModel, c2eModel,
				e2cDict, c2eDict);
		e2cSearcher.init(ePhrases);

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
				if(count % 5000000 == 0)
					System.out.println();
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
		}
	}
	
	public void agreeTrain() {
		ViterbiSearch e2cSearcher = new ViterbiSearch(e2cModel, c2eModel,
				e2cDict, c2eDict);
		ViterbiSearch c2eSearcher = new ViterbiSearch(c2eModel, e2cModel,
				c2eDict, e2cDict);
		e2cSearcher.init(ePhrases);
		c2eSearcher.init(cPhrases);

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
				if(count % 500000 == 0)
					System.out.println();
				String ePhrase = e2cSearcher.independentSearch(cPhrase);
				if (e2cSearcher.score > theta) {
					String cPhrase_ = c2eSearcher.independentSearch(ePhrase);
					if (cPhrase_.equals(cPhrase)) {
						cTrainPhrase.add(cPhrase);
						eTrainPhrase.add(ePhrase);
						trainScore.add(e2cSearcher.score);
					}
				}
			}
			System.out.println();
			c2eModel.agreeTrain(cTrainPhrase, eTrainPhrase, c2eDict, e2cModel, e2cDict);
			
			//e2cModel.agreeTrain(eTrainPhrase, cTrainPhrase, e2cDict, c2eModel);
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
		}
	}
}
