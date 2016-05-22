import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.regex.Pattern;

import cc.mallet.fst.HMM;
import cc.mallet.fst.HMMTrainerByLikelihood;
import cc.mallet.fst.PerClassAccuracyEvaluator;
import cc.mallet.fst.TokenAccuracyEvaluator;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.pipe.SimpleTaggerSentence2TokenSequence;
import cc.mallet.pipe.TokenSequence2FeatureSequence;
import cc.mallet.pipe.iterator.LineGroupIterator;
import cc.mallet.types.InstanceList;
import cc.mallet.util.Randoms;

public class TrainHMM {
	private static String corpusFilename = "data/training_data.txt";
	private static String datasetFilename = "data/dataset.txt";
	
	private static String modelFilename = "model/ner_crf.model";
	
	public static void run(String datasetFilename, String modelFilename) throws IOException {
		ArrayList<Pipe> pipes = new ArrayList<Pipe>();

		pipes.add(new SimpleTaggerSentence2TokenSequence());
		pipes.add(new TokenSequence2FeatureSequence());

		Pipe pipe = new SerialPipes(pipes);

		InstanceList instanceList = new InstanceList(pipe);

		instanceList.addThruPipe(
				new LineGroupIterator(new BufferedReader(new InputStreamReader(new FileInputStream(datasetFilename))),
						Pattern.compile("^\\s*$"), true));
		
		InstanceList[] instanceLists = instanceList.split(new Randoms(), new double[] {0.9, 0.1, 0.0});
		InstanceList trainingInstances = instanceLists[0];
		InstanceList testingInstances = instanceLists[1];

		HMM hmm = new HMM(pipe, null);
		hmm.addStatesForLabelsConnectedAsIn(trainingInstances);
		// hmm.addStatesForBiLabelsConnectedAsIn(trainingInstances);

		HMMTrainerByLikelihood trainer = new HMMTrainerByLikelihood(hmm);
		trainer.addEvaluator(new PerClassAccuracyEvaluator(trainingInstances, "training"));
		trainer.addEvaluator(new TokenAccuracyEvaluator(trainingInstances, "training"));
		
		trainer.addEvaluator(new PerClassAccuracyEvaluator(testingInstances, "testing"));
		trainer.addEvaluator(new TokenAccuracyEvaluator(testingInstances, "testing"));
		
		trainer.train(trainingInstances);

		FileOutputStream fos = new FileOutputStream(modelFilename);
		ObjectOutputStream oos = new ObjectOutputStream(fos);
		oos.writeObject(hmm);
		
		oos.close();
	}

	public TrainHMM() throws Exception {
		DataGenerator dataGenerator = new DataGenerator();
		dataGenerator.generateGazetteer();
		dataGenerator.generateDatasetMaterial(corpusFilename);
		dataGenerator.generateDataset(datasetFilename, 3);

		run(datasetFilename, modelFilename);
	}

	public static void main(String[] args) throws Exception {
		new TrainHMM();
	}
}