0. data
	dictionary (see data/phrase/dict.txt as example, we now do not use the probability in that file)
	phrases of both language (see data/phrase/english.txt and data/phrase/chinese.txt as example)

1.make index for both languages
	java -jar makeIndex.jar sourceTextFile sourceIndexDir
	java -jar makeIndex.jar targetTextFile targetIndexDir

2.train model
	java -jar LuceneMTTrainer.jar sourceTextFile targetTextFile sourceIndexDir targetIndexDir [indep|outer]
	or
	java -jar LuceneMTTrainer.jar sourceTextFile targetTextFile sourceIndexDir targetIndexDir agree thresholdOfLogProb(-120) searchDepth(10) searchBeam(3)
