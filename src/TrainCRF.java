import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.regex.Pattern;

import cc.mallet.fst.CRF;
import cc.mallet.fst.CRFTrainerByLabelLikelihood;
import cc.mallet.fst.CRFWriter;
import cc.mallet.fst.PerClassAccuracyEvaluator;
import cc.mallet.fst.TokenAccuracyEvaluator;
import cc.mallet.fst.TransducerTrainer;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.pipe.SimpleTaggerSentence2TokenSequence;
import cc.mallet.pipe.TokenSequence2FeatureVectorSequence;
import cc.mallet.pipe.iterator.LineGroupIterator;
import cc.mallet.types.InstanceList;

public class TrainCRF {
	public TrainCRF(String trainingFilename, String testingFilename) throws IOException {
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

		CRF crf = new CRF(pipe, null);
		// crf.addStatesForLabelsConnectedAsIn(trainingInstances);
		crf.addStatesForThreeQuarterLabelsConnectedAsIn(trainingInstances);
		crf.addStartState();

		CRFTrainerByLabelLikelihood trainer = new CRFTrainerByLabelLikelihood(crf);
		trainer.setGaussianPriorVariance(10.0);

		CRFWriter crfWriter = new CRFWriter("model/ner_crf.model") {
			@Override
			public boolean precondition(TransducerTrainer tt) {
				// save the trained model after training finishes
				return tt.getIteration() % Integer.MAX_VALUE == 0;
			}
		};
		trainer.addEvaluator(crfWriter);
		trainer.addEvaluator(new PerClassAccuracyEvaluator(testingInstances, "testing"));
		trainer.addEvaluator(new PerClassAccuracyEvaluator(trainingInstances, "training"));
		trainer.addEvaluator(new TokenAccuracyEvaluator(testingInstances, "testing"));
		trainer.addEvaluator(new TokenAccuracyEvaluator(trainingInstances, "training"));
		trainer.train(trainingInstances);
	}
	
	public static void testUsingModel(String s) {
		
	}

	public static void main(String[] args) throws Exception {
		Main.generateDatasetMaterial();
		Main.generateDataset();
		TrainCRF trainer = new TrainCRF("data/train.txt", "data/test.txt");
	}
}