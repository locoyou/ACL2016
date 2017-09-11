package org.thunlp.lcy.anp.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;

public class ViterbiSearch {
	public double score = 0.0;
	public HashMap<String, ArrayList<String>> index;
	public HashMap<String, Double> idf = new HashMap<String, Double>();
	IBMModel1 s2tModel, t2sModel;
	Dictionary s2tDict, t2sDict;
	double[] powMem = new double[100];
	public ArrayList<String> sourceSentences;

	public void init(ArrayList<String> sourceSentences) {
		this.sourceSentences = sourceSentences;
		index = new HashMap<String, ArrayList<String>>();
		for(int i = 0; i < sourceSentences.size(); i++) {
			String sentence = sourceSentences.get(i);
			String[] tags = sentence.split(" ");
			HashSet<String> tmp = new HashSet<String>();
			for(String tag : tags) {
				if(!tmp.contains(tag)) {
					if(idf.containsKey(tag)) {
						idf.put(tag, idf.get(tag)+1.0);
					}
					else {
						idf.put(tag, 1.0);
					}
					tmp.add(tag);
				}
				ArrayList<String> wordIndex = index.get(tag);
				if(wordIndex == null) {
					wordIndex = new ArrayList<String>();
					index.put(tag, wordIndex);
				}
				wordIndex.add(sentence);
			}
		}
		for(String s : idf.keySet()) {
			idf.put(s, Math.log(sourceSentences.size()/(1.0+idf.get(s))));
		}
	}

	public ViterbiSearch(IBMModel1 s2tModel, IBMModel1 t2sModel,
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
	
	public ArrayList<String> retrievalSentences(String tp) {
		ArrayList<String> candidates = new ArrayList<String>();
		HashMap<String, Double> candidateScore = new HashMap<String, Double>();
		String[] words = tp.split(" ");
		for(String t : words) {
			if (s2tModel.transProbInverse.containsKey(t)) {
				for (String s : s2tModel.transProbInverse.get(t).keySet()) {
					if (s2tModel.transProbInverse.get(t).get(s) > LuceneTrainer.alpha) {
						ArrayList<String> docList = index.get(s);
						if (docList == null)
							continue;
						for(String sentence : docList) {
							if(candidateScore.containsKey(sentence)) {
								candidateScore.put(sentence, candidateScore.get(sentence)+idf.get(s));
							}
							else {
								candidateScore.put(sentence, idf.get(s));
							}
						}
					}
				}
			}
		}
		ArrayList<Entry<String,Double>> l = new ArrayList<Entry<String,Double>>(candidateScore.entrySet());
        Collections.sort(l, new Comparator<Map.Entry<String, Double>>() {
            public int compare(Map.Entry<String, Double> o1, Map.Entry<String, Double> o2) {
                return (o2.getValue() - o1.getValue()) > 0 ? 1 : -1;
            }
        });
        for(int i = 0; i < LuceneTrainer.topN && i < l.size(); i++) {
        	candidates.add(l.get(i).getKey());
        }
		return candidates;
	}

	public String agreeSearch(String tp) {
		try {
			String sp = "";
			double maxScore = -10000;

			ArrayList<String> topDocs = retrievalSentences(tp);
			for (int i = 0; i < topDocs.size(); i++) {
				String sp_ = topDocs.get(i);
				if (sp_.length() == 0)
					continue;
				double score_ = getS2TScore(sp_, tp) + getT2SScore(sp_, tp);
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
			ArrayList<String> topDocs = retrievalSentences(tp);
			for (int i = 0; i < topDocs.size(); i++) {
				String sp_ = topDocs.get(i);
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
