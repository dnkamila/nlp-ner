import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.regex.Pattern;

import cc.mallet.fst.MEMM;
import cc.mallet.fst.MEMMTrainer;
import cc.mallet.fst.MEMMTrainer.MEMMOptimizableByLabelLikelihood;
import cc.mallet.fst.tests.TestMEMM;
import cc.mallet.fst.PerClassAccuracyEvaluator;
import cc.mallet.fst.TokenAccuracyEvaluator;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.pipe.SimpleTaggerSentence2TokenSequence;
import cc.mallet.pipe.TokenSequence2FeatureVectorSequence;
import cc.mallet.pipe.iterator.LineGroupIterator;
import cc.mallet.types.InstanceList;

public class TrainMEMM {

	public TrainMEMM(String trainingFilename, String testingFilename) throws IOException {
		ArrayList<Pipe> pipes = new ArrayList<Pipe>();

		int[][] conjunctions = new int[2][];
		conjunctions[0] = new int[] { -1 };
		conjunctions[1] = new int[] { 1 };

		pipes.add(new SimpleTaggerSentence2TokenSequence());
		pipes.add(new TokenSequence2FeatureVectorSequence());

		Pipe pipe = new SerialPipes(pipes);

		InstanceList trainingInstances = new InstanceList(pipe);
		InstanceList testingInstances = new InstanceList(pipe);

		trainingInstances.addThruPipe(
				new LineGroupIterator(new BufferedReader(new InputStreamReader(new FileInputStream(trainingFilename))),
						Pattern.compile("^\\s*$"), true));
		testingInstances.addThruPipe(
				new LineGroupIterator(new BufferedReader(new InputStreamReader(new FileInputStream(testingFilename))),
						Pattern.compile("^\\s*$"), true));

		MEMM memm = new MEMM(pipe, null);
		// memm.addStatesForLabelsConnectedAsIn(trainingInstances);
		memm.addFullyConnectedStatesForLabels();
		memm.setWeightsDimensionAsIn(trainingInstances);
		memm.addStartState();

		MEMMTrainer trainer = new MEMMTrainer(memm);
		
		trainer.addEvaluator(new PerClassAccuracyEvaluator(testingInstances, "testing"));
		trainer.addEvaluator(new PerClassAccuracyEvaluator(trainingInstances, "training"));
		trainer.addEvaluator(new TokenAccuracyEvaluator(testingInstances, "testing"));
		trainer.addEvaluator(new TokenAccuracyEvaluator(trainingInstances, "training"));
		trainer.train(trainingInstances);
	}
	
	public static void testUsingModel(String s) {
		
	}

	public static void main(String[] args) throws Exception {
		TrainMEMM trainer = new TrainMEMM("data/train.txt", "data/test.txt");
	}
}
