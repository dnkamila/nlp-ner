import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.regex.Pattern;

import cc.mallet.fst.CRF;
import cc.mallet.fst.CRFTrainerByLabelLikelihood;
import cc.mallet.fst.MaxLatticeDefault;
import cc.mallet.fst.PerClassAccuracyEvaluator;
import cc.mallet.fst.TokenAccuracyEvaluator;
import cc.mallet.fst.Transducer;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.pipe.SimpleTaggerSentence2TokenSequence;
import cc.mallet.pipe.TokenSequence2FeatureVectorSequence;
import cc.mallet.pipe.iterator.LineGroupIterator;
import cc.mallet.types.FeatureVector;
import cc.mallet.types.InstanceList;
import cc.mallet.types.Sequence;
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

		InstanceList[] instanceLists = instanceList.split(new Randoms(), new double[] { 0.9, 0.1, 0.0 });
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
		// TrainCRF.run("data/dataset.txt", "model/ner_crf.model");
	}

	public static void label() throws Exception {
		ArrayList<Pipe> pipes = new ArrayList<Pipe>();

		pipes.add(new SimpleTaggerSentence2TokenSequence());
		pipes.add(new TokenSequence2FeatureVectorSequence());

		Pipe pipe = new SerialPipes(pipes);

		ObjectInputStream s = new ObjectInputStream(new FileInputStream("model/ner_crf.model"));
		CRF crf = (CRF) s.readObject();
		s.close();
		
		InstanceList test = new InstanceList(pipe);
		test.addThruPipe(new LineGroupIterator(
				new BufferedReader(new InputStreamReader(new FileInputStream("data/unlabeled_dataset.txt"))),
				Pattern.compile("^\\s*$"), true));
		for (int i = 0; i < test.size(); i++) {
			Sequence input = (Sequence) test.get(i).getData();
			Sequence[] outputs = apply(crf, input, 1);
			int k = outputs.length;
			boolean error = false;
			for (int a = 0; a < k; a++) {
				if (outputs[a].size() != input.size()) {
					error = true;
				}
			}
			if (!error) {
				for (int j = 0; j < input.size(); j++) {
					StringBuffer buf = new StringBuffer();
					for (int a = 0; a < k; a++) {
						buf.append(outputs[a].get(j).toString()).append(" ");
					}
					FeatureVector fv = (FeatureVector) input.get(j);
					buf.append(fv.toString(true));

					System.out.println(input.get(j) + " " + buf.toString());
				}
				System.out.println();
			}
		}
	}

	public static Sequence[] apply(Transducer model, Sequence input, int k) {
		Sequence[] answers;
		if (k == 1) {
			answers = new Sequence[1];
			answers[0] = model.transduce(input);
		} else {
			MaxLatticeDefault lattice = new MaxLatticeDefault(model, input, null, 10000);
			answers = lattice.bestOutputSequences(k).toArray(new Sequence[0]);
		}
		return answers;
	}

}