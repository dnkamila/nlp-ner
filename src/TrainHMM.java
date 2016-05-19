import java.io.*;
import java.util.*;
import java.util.regex.*;
import java.util.zip.*;

import javax.swing.UnsupportedLookAndFeelException;

import cc.mallet.fst.*;
import cc.mallet.pipe.*;
import cc.mallet.pipe.iterator.*;
import cc.mallet.pipe.tsf.*;
import cc.mallet.types.*;
import cc.mallet.util.*;

public class TrainHMM {
	public TrainHMM(String trainingFilename, String testingFilename) throws IOException {
		ArrayList<Pipe> pipes = new ArrayList<Pipe>();

		pipes.add(new SimpleTaggerSentence2TokenSequence());
		pipes.add(new TokenSequence2FeatureSequence());

		Pipe pipe = new SerialPipes(pipes);

		InstanceList trainingInstances = new InstanceList(pipe);
		InstanceList testingInstances = new InstanceList(pipe);
		// InstanceList unlabeledInstances = new InstanceList(pipe);

		trainingInstances.addThruPipe(
				new LineGroupIterator(new BufferedReader(new InputStreamReader(new FileInputStream(trainingFilename))),
						Pattern.compile("^\\s*$"), true));
		testingInstances.addThruPipe(
				new LineGroupIterator(new BufferedReader(new InputStreamReader(new FileInputStream(testingFilename))),
						Pattern.compile("^\\s*$"), true));
		/*
		 * unlabeledInstances.addThruPipe( new LineGroupIterator(new
		 * BufferedReader(new InputStreamReader(new
		 * FileInputStream("data/unlabeled.txt"))), Pattern.compile("^\\s*$"),
		 * true));
		 */

		HMM hmm = new HMM(pipe, null);
		hmm.addStatesForLabelsConnectedAsIn(trainingInstances);
		// hmm.addStatesForBiLabelsConnectedAsIn(trainingInstances);

		HMMTrainerByLikelihood trainer = new HMMTrainerByLikelihood(hmm);
		TransducerEvaluator trainingEvaluator = new PerClassAccuracyEvaluator(trainingInstances, "training");
		TransducerEvaluator testingEvaluator = new PerClassAccuracyEvaluator(testingInstances, "testing");

		trainer.train(trainingInstances);

		trainingEvaluator.evaluate(trainer);
		testingEvaluator.evaluate(trainer);

		trainingEvaluator = new TokenAccuracyEvaluator(trainingInstances, "training");
		testingEvaluator = new TokenAccuracyEvaluator(testingInstances, "testing");

		trainingEvaluator.evaluate(trainer);
		testingEvaluator.evaluate(trainer);

		FileOutputStream fos = new FileOutputStream("data/ner_hmm.model");
		ObjectOutputStream oos = new ObjectOutputStream(fos);
		oos.writeObject(hmm);
		
		oos.close();
	}

	public static void main(String[] args) throws Exception {
		TrainHMM trainer = new TrainHMM("data/train.txt", "data/test.txt");
		//SimpleTagger.main(args);
	}
}