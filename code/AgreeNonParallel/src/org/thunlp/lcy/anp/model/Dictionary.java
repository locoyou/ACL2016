package org.thunlp.lcy.anp.model;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;

public class Dictionary {
	HashMap<String, HashSet<String>> dict = new HashMap<String, HashSet<String>>();
	
	HashSet<String> punc = new HashSet<String>();
	String[] puncs = {",",";","‘","’","'","\"","、","。","(",")","&quot;","and",":"};
	
	public void load(String dictFile, boolean sDict) {
		if(dictFile == null || dictFile.length() == 0)
			return;
		try {
			//for(String s : puncs)
			//	punc.add(s);
			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(dictFile)));
			String line;
			while((line = br.readLine()) != null) {
				String[] tags = line.trim().split(" ");
				String s = "", t = "";
				if(sDict) {
					s = tags[0];
					t = tags[1];
				}
				else {
					s = tags[1];
					t = tags[0];
				}
				if(!punc.contains(s) && !punc.contains(t)) {
					if(dict.containsKey(s)) {
						dict.get(s).add(t);
					}
					else {
						HashSet<String> set = new HashSet<String>();
						set.add(t);
						dict.put(s, set);
					}
				}
			}
			br.close();
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}
}
