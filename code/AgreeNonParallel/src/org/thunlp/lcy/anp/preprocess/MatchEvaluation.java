package org.thunlp.lcy.anp.preprocess;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.HashMap;

import org.thunlp.lcy.anp.model.LuceneTrainer;

public class MatchEvaluation {
	HashMap<String, String> match;
	
	public static void main(String[] args) {
		MatchEvaluation eva = new MatchEvaluation();
		eva.evaluate("output/tmp0");
	}
	
	public MatchEvaluation() {
		try {
			match = new HashMap<String, String>();
			BufferedReader br1 = new BufferedReader(new InputStreamReader(new FileInputStream(LuceneTrainer.cPhraseFile)));
			BufferedReader br2 = new BufferedReader(new InputStreamReader(new FileInputStream(LuceneTrainer.ePhraseFile)));
			String cline, eline;
			int count = 0;
			while((cline = br1.readLine()) != null && count < LuceneTrainer.evaNum) {
				eline = br2.readLine();
				match.put(cline, eline);
				count++;
			}
			br1.close();
			br2.close();
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public void evaluate(String output) {
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(output)));
			String line;
			int matchItem = 0, totalItem = 0;
			while((line = br.readLine()) != null) {
				String[] tags = line.split(" \\|\\|\\| ");
				totalItem++;
				if(match.containsKey(tags[0]) && match.get(tags[0]).equals(tags[1])) {
					matchItem++;
				}
				/*
				else {
					System.out.println(line);
					System.out.println(match.get(tags[0]));
					System.out.println("----");
				}
				*/
			}
			System.out.println(matchItem+" "+totalItem);
			double precision = (double)matchItem/totalItem, recall = (double)matchItem/match.size();
			System.out.println("Accuracy:"+recall);
			System.out.println("Precision:"+precision);
			System.out.println("F1:"+ 2*precision*recall/(precision+recall));
			br.close();
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}
}
