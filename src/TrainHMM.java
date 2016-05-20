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

	public static void main(String[] args) throws Exception {
		/*Main.generateDatasetMaterial();
		Main.generateDataset();*/
		TrainHMM.run("data/dataset.txt", "model/ner_hmm.model");
	}
}