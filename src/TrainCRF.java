import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
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
import cc.mallet.util.Randoms;

public class TrainCRF {
	public static void run(String datasetFilename, String modelFilename) throws IOException {
		ArrayList<Pipe> pipes = new ArrayList<Pipe>();

		int[][] conjunctions = new int[2][];
		conjunctions[0] = new int[] { -1 };
		conjunctions[1] = new int[] { 1 };

		pipes.add(new SimpleTaggerSentence2TokenSequence());
		pipes.add(new TokenSequence2FeatureVectorSequence());

		Pipe pipe = new SerialPipes(pipes);

		InstanceList instanceList = new InstanceList(pipe);

		instanceList.addThruPipe(
				new LineGroupIterator(new BufferedReader(new InputStreamReader(new FileInputStream(datasetFilename))),
						Pattern.compile("^\\s*$"), true));
		
		InstanceList[] instanceLists = instanceList.split(new Randoms(), new double[] {0.9, 0.1, 0.0});
		InstanceList trainingInstances = instanceLists[0];
		InstanceList testingInstances = instanceLists[1];

		CRF crf = new CRF(pipe, null);
		// crf.addStatesForLabelsConnectedAsIn(trainingInstances);
		crf.addStatesForThreeQuarterLabelsConnectedAsIn(trainingInstances);
		crf.addStartState();

		CRFTrainerByLabelLikelihood trainer = new CRFTrainerByLabelLikelihood(crf);
		trainer.setGaussianPriorVariance(10.0);

		FileOutputStream fos = new FileOutputStream(modelFilename);
		ObjectOutputStream oos = new ObjectOutputStream(fos);
		oos.writeObject(crf);
		
		oos.close();
		
		trainer.addEvaluator(new PerClassAccuracyEvaluator(trainingInstances, "training"));
		trainer.addEvaluator(new TokenAccuracyEvaluator(trainingInstances, "training"));
		
		trainer.addEvaluator(new PerClassAccuracyEvaluator(testingInstances, "testing"));
		trainer.addEvaluator(new TokenAccuracyEvaluator(testingInstances, "testing"));
		
		trainer.train(trainingInstances);
	}

	public static void main(String[] args) throws Exception {
		/*Main.generateDatasetMaterial();
		Main.generateDataset();*/
		TrainCRF.run("data/dataset.txt", "model/ner_crf.model");
	}
}