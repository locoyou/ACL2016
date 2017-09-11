package org.thunlp.lcy.anp.model;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;

public class MTTrainer {
	public static String dictFile = "";
	public static String cParallel = "", eParallel = "";
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
	//MatchEvaluation evaluation = new MatchEvaluation();
	ArrayList<String> cParallelPhrases, eParallelPhrases;

	public static void main(String[] args) {
		String model = "all";
		if(args.length == 6 || args.length == 7) {
			System.out.println("run "+ args[0]+" "+args[1]+" "+args[2]+" "+args[3]+" "+args[4]+" "+args[5]);
			cParallel = args[0];
			eParallel = args[1];
			cPhraseFile = args[2];
			ePhraseFile = args[3];
			cIndex = args[4];
			eIndex = args[5];
			if(args.length == 7) {
				model = args[6];
			}
		}
		
		MTTrainer trainer = new MTTrainer();
		
		if(model.equals("all") || model.equals("outer")) {
			trainer.load();
			trainer.trainWithParallel();
			System.out.println("outerAgreeTrain");
			trainer.outerAgreeTrain();
			System.out.println("------");
			System.out.println();
		}
		
		if(model.equals("all") || model.equals("agree")) {
			trainer.load();
			trainer.trainWithParallel();
			System.out.println("agreeTrain");
			trainer.agreeTrain();
			System.out.println("------");
			System.out.println();
		}

		if(model.equals("all") || model.equals("indep")) {
			trainer.load();
			trainer.trainWithParallel();
			System.out.println("independentTrain");
			trainer.independentTrain();
			System.out.println("------");
			System.out.println();
		}
	}

	public void trainWithParallel() {
		for(int i = 0; i < 5; i++) {
			c2eModel.independentTrain(cParallelPhrases, eParallelPhrases, c2eDict);
			e2cModel.independentTrain(eParallelPhrases, cParallelPhrases, e2cDict);
		}
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
		cParallelPhrases = new ArrayList<String>();
		eParallelPhrases = new ArrayList<String>();
		loadPhrases(cPhrases, cPhraseFile);
		loadPhrases(ePhrases, ePhraseFile);
		loadPhrases(cParallelPhrases, cParallel);
		loadPhrases(eParallelPhrases, eParallel);
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
			long start = System.currentTimeMillis();
			for (String cPhrase : cPhrases) {
				count++;
				if(count % 10000 == 0) {
					System.out.println("iter"+iter+" "+count + " " +(System.currentTimeMillis()-start)/1000.0);
				}
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
						new FileOutputStream("output/outer" + iter+".chi")));
				PrintWriter pw2 = new PrintWriter(new OutputStreamWriter(
						new FileOutputStream("output/outer" + iter+".eng")));
				for (int i = 0; i < cParallelPhrases.size(); i++) {
					pw1.println(cParallelPhrases.get(i));
					pw2.println(eParallelPhrases.get(i));
				}
				for (int i = 0; i < cTrainPhrase.size(); i++) {
					pw1.println(cTrainPhrase.get(i));
					pw2.println(eTrainPhrase.get(i));
				}
				pw1.close();
				pw2.close();
				//evaluation.evaluate("output/outer" + iter);
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
						new FileOutputStream("output/indep" + iter+".chi")));
				PrintWriter pw2 = new PrintWriter(new OutputStreamWriter(
						new FileOutputStream("output/indep" + iter+".eng")));
				for (int i = 0; i < cParallelPhrases.size(); i++) {
					pw1.println(cParallelPhrases.get(i));
					pw2.println(eParallelPhrases.get(i));
				}
				for (int i = 0; i < cTrainPhrase.size(); i++) {
					pw1.println(cTrainPhrase.get(i));
					pw2.println(eTrainPhrase.get(i));
				}
				pw1.close();
				pw2.close();
				//evaluation.evaluate("output/indep" + iter);
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
			c2eModel.agreeTrain(cTrainPhrase, eTrainPhrase, c2eDict, e2cModel, e2cDict);
			
			//e2cModel.agreeTrain(eTrainPhrase, cTrainPhrase, e2cDict, c2eModel);
			try {
				PrintWriter pw1 = new PrintWriter(new OutputStreamWriter(
						new FileOutputStream("output/agree" + iter+".chi")));
				PrintWriter pw2 = new PrintWriter(new OutputStreamWriter(
						new FileOutputStream("output/agree" + iter+".eng")));
				for (int i = 0; i < cParallelPhrases.size(); i++) {
					pw1.println(cParallelPhrases.get(i));
					pw2.println(eParallelPhrases.get(i));
				}
				for (int i = 0; i < cTrainPhrase.size(); i++) {
					pw1.println(cTrainPhrase.get(i));
					pw2.println(eTrainPhrase.get(i));
				}
				pw1.close();
				pw2.close();
				//evaluation.evaluate("output/agree" + iter);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
