package org.thunlp.lcy.anp.preprocess;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.HashSet;

public class NGramExtractor {
	public static void main(String[] args) {
		NGramExtractor extractor = new NGramExtractor();
		extractor.extract(args[0], args[1], Integer.valueOf(args[2]));
	}
	
	public void extract(String inputFile, String outputFile, int order) {
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(inputFile)));
			PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(outputFile)));
			HashSet<String> ngrams = new HashSet<String>();
			String line;
			while((line = br.readLine()) != null) {
				String[] tags = line.split(" ");
				for(int i = 0; i < tags.length; i++) {
					String ngram = tags[i];
					ngrams.add(ngram);
					for(int j = i+1; j < tags.length && j < i + order; j++) {
						ngram += " " + tags[j];
						ngrams.add(ngram);
					}
				}
			}
			br.close();
			for(String ngram : ngrams) {
				pw.println(ngram);
			}
			pw.close();
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}
}
