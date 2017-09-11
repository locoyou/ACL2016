package org.thunlp.lcy.anp.model;

import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class IBMModel1 {
	HashMap<String, HashMap<String, Double>> transProb = new HashMap<String, HashMap<String, Double>>();
	HashMap<String, HashMap<String, Double>> transProbInverse = new HashMap<String, HashMap<String, Double>>();
	HashMap<Integer, HashMap<Integer, Double>> lengthProb = new HashMap<Integer, HashMap<Integer, Double>>();
	HashMap<String, HashMap<String, Double>> transCountCache;
	HashMap<Integer, HashMap<Integer, Double>> lengthCountCache;
	boolean inited = false;
	
	public double getLengthProb(int sl, int tl) {
		if (!inited)
			return 1;
		if (lengthProb.containsKey(sl) && lengthProb.get(sl).containsKey(tl)
				&& lengthProb.get(sl).get(tl) > LuceneTrainer.defaultProb) {
			return lengthProb.get(sl).get(tl);
		} else {
			return LuceneTrainer.defaultProb;
		}
	}

	public double getProb(String s, String t) {
		if (transProb.containsKey(s) && transProb.get(s).containsKey(t)
				&& transProb.get(s).get(t) > LuceneTrainer.defaultProb) {
			return transProb.get(s).get(t);
		} else {
			return LuceneTrainer.defaultProb;
		}
	}

	public void initByDict(Dictionary dict) {
		HashMap<String, HashMap<String, Double>> transCount = new HashMap<String, HashMap<String, Double>>();
		HashMap<Integer, HashMap<Integer, Double>> lengthCount = new HashMap<Integer, HashMap<Integer, Double>>();
		for (String sWord : dict.dict.keySet()) {
			for (String tWord : dict.dict.get(sWord)) {
				HashMap<String, Double> count = transCount.get(sWord);
				if (count == null) {
					count = new HashMap<String, Double>();
					transCount.put(sWord, count);
				}
				if (count.containsKey(tWord)) {
					count.put(tWord, count.get(tWord) + 1.0);
				} else {
					count.put(tWord, 1.0);
				}
			}
		}
		normalize(transCount, lengthCount);
	}

	public void independentTrain(ArrayList<String> sourceSentences,
			ArrayList<String> targetSentences, Dictionary dict) {
		HashMap<String, HashMap<String, Double>> transCount = new HashMap<String, HashMap<String, Double>>();
		HashMap<Integer, HashMap<Integer, Double>> lengthCount = new HashMap<Integer, HashMap<Integer, Double>>();
		for (int i = 0; i < sourceSentences.size(); i++) {
			String[] sWords = (sourceSentences.get(i) + " #null").split(" ");
			String[] tWords = targetSentences.get(i).split(" ");
			int sl = sWords.length - 1, tl = tWords.length;
			HashMap<Integer, Double> lp = lengthCount.get(sl);
			if(lp == null) {
				lp = new HashMap<Integer, Double>();
				lengthCount.put(sl, lp);
			}
			if(lp.containsKey(tl)) {
				lp.put(tl, lp.get(tl)+1);
			}
			else {
				lp.put(tl, 1.0);
			}
			HashMap<String, Double> total = new HashMap<String, Double>();
			HashMap<String, Integer> sFreq = new HashMap<String, Integer>(), tFreq = new HashMap<String, Integer>();
			for (String sWord : sWords) {
				if (sFreq.containsKey(sWord)) {
					sFreq.put(sWord, sFreq.get(sWord) + 1);
				} else {
					sFreq.put(sWord, 1);
				}
			}
			for (String tWord : tWords) {
				if (tFreq.containsKey(tWord)) {
					tFreq.put(tWord, tFreq.get(tWord) + 1);
				} else {
					tFreq.put(tWord, 1);
				}
			}
			for (String tWord : tFreq.keySet()) {
				total.put(tWord, 0.0);
				for (String sWord : sWords) {
					total.put(tWord, total.get(tWord) + getProb(sWord, tWord));
				}
			}
			for (String sWord : sFreq.keySet()) {
				for (String tWord : tFreq.keySet()) {
					double c = sFreq.get(sWord) * tFreq.get(tWord)
							* getProb(sWord, tWord) / total.get(tWord);
					if (dict.dict.containsKey(sWord)
							&& dict.dict.get(sWord).contains(tWord)) {
						c += 1.0;
					}
					HashMap<String, Double> count = transCount.get(sWord);
					if (count == null) {
						count = new HashMap<String, Double>();
						transCount.put(sWord, count);
					}
					if (count.containsKey(tWord)) {
						count.put(tWord, count.get(tWord) + c);
					} else {
						count.put(tWord, c);
					}
				}
			}

		}
		normalize(transCount, lengthCount);
		inited = true;
	}
	/*
	public void agreeTrain(ArrayList<String> sourceSentences,
			ArrayList<String> targetSentences, Dictionary dict,
			IBMModel1 inverseModel, Dictionary inverseDict) {
		HashMap<String, HashMap<String, Double>> transCount = new HashMap<String, HashMap<String, Double>>();
		HashMap<String, HashMap<String, Double>> inverseTransCount = new HashMap<String, HashMap<String, Double>>();
		HashMap<Integer, HashMap<Integer, Double>> lengthCount = new HashMap<Integer, HashMap<Integer, Double>>();
		HashMap<Integer, HashMap<Integer, Double>> inverseLengthCount = new HashMap<Integer, HashMap<Integer, Double>>();
		for (int i = 0; i < sourceSentences.size(); i++) {
			String[] sWords = (sourceSentences.get(i) + " #null").split(" ");
			String[] tWords = (targetSentences.get(i) + " #null").split(" ");
			int sl = sWords.length - 1, tl = tWords.length - 1;
			HashMap<Integer, Double> lp = lengthCount.get(sl);
			if(lp == null) {
				lp = new HashMap<Integer, Double>();
				lengthCount.put(sl, lp);
			}
			if(lp.containsKey(tl)) {
				lp.put(tl, lp.get(tl)+1);
			}
			else {
				lp.put(tl, 1.0);
			}
			
			lp = inverseLengthCount.get(tl);
			if(lp == null) {
				lp = new HashMap<Integer, Double>();
				inverseLengthCount.put(tl, lp);
			}
			if(lp.containsKey(sl)) {
				lp.put(sl, lp.get(sl)+1);
			}
			else {
				lp.put(sl, 1.0);
			}
			
			HashMap<String, Double> total = new HashMap<String, Double>(), inverseTotal = new HashMap<String, Double>();
			HashMap<String, Integer> sFreq = new HashMap<String, Integer>(), tFreq = new HashMap<String, Integer>();
			for (String sWord : sWords) {
				if (sFreq.containsKey(sWord)) {
					sFreq.put(sWord, sFreq.get(sWord) + 1);
				} else {
					sFreq.put(sWord, 1);
				}
			}
			for (String tWord : tWords) {
				if (tFreq.containsKey(tWord)) {
					tFreq.put(tWord, tFreq.get(tWord) + 1);
				} else {
					tFreq.put(tWord, 1);
				}
			}
			for (String tWord : tFreq.keySet()) {
				if (tWord.equals("#null"))
					continue;
				total.put(tWord, 0.0);
				for (String sWord : sWords) {
					total.put(tWord, total.get(tWord) + getProb(sWord, tWord));
				}
			}
			for (String sWord : sFreq.keySet()) {
				if (sWord.equals("#null"))
					continue;
				inverseTotal.put(sWord, 0.0);
				for (String tWord : tWords) {
					inverseTotal.put(sWord, inverseTotal.get(sWord)
							+ inverseModel.getProb(tWord, sWord));
				}
			}

			HashMap<String, HashMap<String, Double>> tmpTransCount = new HashMap<String, HashMap<String, Double>>();
			HashMap<String, HashMap<String, Double>> tmpInverseTransCount = new HashMap<String, HashMap<String, Double>>();

			for (String sWord : sFreq.keySet()) {
				for (String tWord : tFreq.keySet()) {
					if (tWord.equals("#null"))
						continue;
					double c = sFreq.get(sWord) * tFreq.get(tWord)
							* getProb(sWord, tWord) / total.get(tWord);
					HashMap<String, Double> count = tmpTransCount.get(sWord);
					if (count == null) {
						count = new HashMap<String, Double>();
						tmpTransCount.put(sWord, count);
					}
					if (count.containsKey(tWord)) {
						count.put(tWord, count.get(tWord) + c);
					} else {
						count.put(tWord, c);
					}
				}
			}

			for (String tWord : tFreq.keySet()) {
				for (String sWord : sFreq.keySet()) {
					if (sWord.equals("#null"))
						continue;
					double c = sFreq.get(sWord) * tFreq.get(tWord)
							* inverseModel.getProb(tWord, sWord)
							/ inverseTotal.get(sWord);
					HashMap<String, Double> count = tmpInverseTransCount
							.get(tWord);
					if (count == null) {
						count = new HashMap<String, Double>();
						tmpInverseTransCount.put(tWord, count);
					}
					if (count.containsKey(sWord)) {
						count.put(sWord, count.get(sWord) + c);
					} else {
						count.put(sWord, c);
					}
				}
			}

			for (String sWord : tmpTransCount.keySet()) {
				for (String tWord : tmpTransCount.get(sWord).keySet()) {
					double c = tmpTransCount.get(sWord).get(tWord);
					if (!sWord.equals("#null")) {
						c *= tmpInverseTransCount.get(tWord).get(sWord);
						HashMap<String, Double> count = inverseTransCount
								.get(tWord);
						if (count == null) {
							count = new HashMap<String, Double>();
							inverseTransCount.put(tWord, count);
						}
						if (count.containsKey(sWord)) {
							count.put(sWord, count.get(sWord) + c);
						} else {
							count.put(sWord, c);
						}
					}
					HashMap<String, Double> count = transCount.get(sWord);
					if (count == null) {
						count = new HashMap<String, Double>();
						transCount.put(sWord, count);
					}
					if (count.containsKey(tWord)) {
						count.put(tWord, count.get(tWord) + c);
					} else {
						count.put(tWord, c);
					}
				}
			}
			for (String sWord : tmpInverseTransCount.get("#null").keySet()) {
				String tWord = "#null";
				HashMap<String, Double> count = inverseTransCount.get(tWord);
				double c = tmpInverseTransCount.get(tWord).get(sWord);
				if (count == null) {
					count = new HashMap<String, Double>();
					inverseTransCount.put(tWord, count);
				}
				if (count.containsKey(sWord)) {
					count.put(sWord, count.get(sWord) + c);
				} else {
					count.put(sWord, c);
				}
			}
		}
		normalize(transCount, lengthCount);
		inverseModel.normalize(inverseTransCount, inverseLengthCount);
		inited = true;
	}
	*/
	public void agreeTrain(ArrayList<String> sourceSentences,
			ArrayList<String> targetSentences, Dictionary dict,
			IBMModel1 inverseModel, Dictionary inverseDict) {
		HashMap<String, HashMap<String, Double>> transCount = new HashMap<String, HashMap<String, Double>>();
		HashMap<Integer, HashMap<Integer, Double>> lengthCount = new HashMap<Integer, HashMap<Integer, Double>>();
		for (int i = 0; i < sourceSentences.size(); i++) {
			String[] sWords = (sourceSentences.get(i) + " #null").split(" ");
			String[] tWords = targetSentences.get(i).split(" ");
			int sl = sWords.length - 1, tl = tWords.length;
			HashMap<Integer, Double> lp = lengthCount.get(sl);
			if(lp == null) {
				lp = new HashMap<Integer, Double>();
				lengthCount.put(sl, lp);
			}
			if(lp.containsKey(tl)) {
				lp.put(tl, lp.get(tl)+1);
			}
			else {
				lp.put(tl, 1.0);
			}
			HashMap<String, Double> total = new HashMap<String, Double>();
			HashMap<String, Integer> sFreq = new HashMap<String, Integer>(), tFreq = new HashMap<String, Integer>();
			for (String sWord : sWords) {
				if (sFreq.containsKey(sWord)) {
					sFreq.put(sWord, sFreq.get(sWord) + 1);
				} else {
					sFreq.put(sWord, 1);
				}
			}
			for (String tWord : tWords) {
				if (tFreq.containsKey(tWord)) {
					tFreq.put(tWord, tFreq.get(tWord) + 1);
				} else {
					tFreq.put(tWord, 1);
				}
			}
			for (String tWord : tFreq.keySet()) {
				total.put(tWord, 0.0);
				for (String sWord : sWords) {
					total.put(tWord, total.get(tWord) + getProb(sWord, tWord)*inverseModel.getProb(tWord, sWord));
				}
			}
			for (String sWord : sFreq.keySet()) {
				for (String tWord : tFreq.keySet()) {
					double c = sFreq.get(sWord) * tFreq.get(tWord) *
							getProb(sWord, tWord)*inverseModel.getProb(tWord, sWord) / total.get(tWord);
					if (dict.dict.containsKey(sWord)
							&& dict.dict.get(sWord).contains(tWord)) {
						c += 1.0;
					}
					HashMap<String, Double> count = transCount.get(sWord);
					if (count == null) {
						count = new HashMap<String, Double>();
						transCount.put(sWord, count);
					}
					if (count.containsKey(tWord)) {
						count.put(tWord, count.get(tWord) + c);
					} else {
						count.put(tWord, c);
					}
				}
			}

		}
		transCountCache = transCount;
		lengthCountCache = lengthCount;
		//normalize(transCount, lengthCount);
	}
	
	public void normalize() {
		normalize(transCountCache, lengthCountCache);
		inited = true;
	}

	public void normalize(HashMap<String, HashMap<String, Double>> transCount,
			HashMap<Integer, HashMap<Integer, Double>> lengthCount) {
		transProb = new HashMap<String, HashMap<String, Double>>();
		transProbInverse = new HashMap<String, HashMap<String, Double>>();
		lengthProb = new HashMap<Integer, HashMap<Integer, Double>>();
		for (int sl : lengthCount.keySet()) {
			HashMap<Integer, Double> count = lengthCount.get(sl);
			double total = 0;
			for (Double lp : count.values()) {
				total += lp;
			}
			HashMap<Integer, Double> prob = new HashMap<Integer, Double>();
			for (int tl : count.keySet()) {
				double p = count.get(tl)/total;
				if (p > LuceneTrainer.defaultProb) {
					prob.put(tl, p);
				}
			}
			lengthProb.put(sl, prob);
		}
			
		for (String s : transCount.keySet()) {
			HashMap<String, Double> count = transCount.get(s);
			double total = 0;
			for (Double tp : count.values()) {
				total += tp;
			}
			HashMap<String, Double> prob = new HashMap<String, Double>();
			for (String t : count.keySet()) {
				double p = count.get(t) / total;
				if (p > LuceneTrainer.defaultProb) {
					prob.put(t, p);
				}
				if (p > LuceneTrainer.alpha) {
					HashMap<String, Double> probInverse = transProbInverse
							.get(t);
					if (probInverse == null) {
						probInverse = new HashMap<String, Double>();
						transProbInverse.put(t, probInverse);
					}
					probInverse.put(s, p);
					if (probInverse.size() > LuceneTrainer.queryNum) {
						String del = "";
						double minScore = 10000;
						for (String ss : probInverse.keySet()) {
							if (probInverse.get(ss) < minScore) {
								del = ss;
								minScore = probInverse.get(ss);
							}
						}
						probInverse.remove(del);
					}
				}
			}
			transProb.put(s, prob);
		}
		inited = true;
	}

	public void print(String fileName) {
		try {
			PrintWriter pw = new PrintWriter(new OutputStreamWriter(
					new FileOutputStream(fileName)));
			for (String s : transProb.keySet()) {
				HashMap<String, Double> x = transProb.get(s);
				ArrayList<Entry<String, Double>> l = new ArrayList<Entry<String, Double>>(x.entrySet());
				Collections.sort(l, new Comparator<Map.Entry<String, Double>>() {
					public int compare(Map.Entry<String, Double> l1, Map.Entry<String, Double> l2) {
						return (l2.getValue() - l1.getValue()) > 0 ? 1:-1;
					}
				});
				int count = 0;
				for(Entry<String, Double> k : l) {
					pw.println(s+" "+k.getKey()+" "+k.getValue());
					count++;
					if(count == 10)
						break;
				}
			}
			pw.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
