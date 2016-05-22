import cc.mallet.fst.SimpleTagger;

import java.io.*;

public class TugasNER {
  public static String LABELED_FILE_NAME = "data/labeled.txt";
  public static String LABELED_SENTENCE_FILE_NAME = "data/output.txt";
  public static String CORPUS_FILE_NAME = "data/training_data.txt";
  public static String DATASET_FILE_NAME = "data/dataset.txt";
  public static String MODEL_FILE_NAME = "model/ner_crf.model";
  public static String UNLABELED_FILE_NAME = "data/testing_data_unannotated.txt";
  public static String UNLABELED_DATASET_FILE_NAME = "data/unlabeled_dataset.txt";

  public TugasNER() {}

  private void run(String[] args) throws Exception {
    if (args.length == 0)
      System.out.print("write file input name and feature number! [data/input.txt 3] ");
    else
      System.out.print("write file input name and feature number! [data/training_data.txt 3] ");
    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
    String input = br.readLine();
    String[] sp = input.split("\\s");

    String fileInput = null;
    String feature = null;
    if (sp.length == 2) {
      fileInput = sp[0];
      feature = sp[1];
    }

    if (feature == null) {
      feature = "3";
    }

    if (args.length == 1) {
      if ("train".equalsIgnoreCase(args[0])) {
        if (fileInput == null) {
          fileInput = CORPUS_FILE_NAME;
        }
        trainCRF(fileInput, Integer.parseInt(feature));
      }
    } else {
      if (fileInput == null) {
        fileInput = UNLABELED_FILE_NAME;
      }
      testingData(fileInput, Integer.parseInt(feature));
    }
  }

  private void trainCRF(String fileInput, int featureNumber) throws Exception {
    DataGenerator dataGenerator = new DataGenerator();
    dataGenerator.generateDatasetMaterial(CORPUS_FILE_NAME);
    dataGenerator.generateDataset(DATASET_FILE_NAME, featureNumber);

    String[] args = new String[5];

    args[0] = "--train";
    args[1] = "true";
    args[2] = "--model-file";
    args[3] = MODEL_FILE_NAME;
    args[4] = DATASET_FILE_NAME;

    SimpleTagger.main(args);
  }

  private void testingData(String unlabeledFileName, int featureNumber) throws Exception {
    DataGenerator dataGenerator = new DataGenerator();
    dataGenerator.generateGazetteer();
    dataGenerator.generateDatasetMaterialUnlabeled(unlabeledFileName);
    dataGenerator.generateDataset(UNLABELED_DATASET_FILE_NAME, featureNumber);

    PrintStream psOutput = new PrintStream(new FileOutputStream(DataGenerator.TEMP_FILE_NAME));
    System.setOut(psOutput);

    String[] args = new String[3];

    args[0] = "--model-file";
    args[1] = MODEL_FILE_NAME;
    args[2] = UNLABELED_DATASET_FILE_NAME;

    SimpleTagger.main(args);

    psOutput.close();

    dataGenerator.printLabeledData();

    ConstructData.main(new String[] {LABELED_FILE_NAME, LABELED_SENTENCE_FILE_NAME});
  }

  public static void main(String[] args) throws Exception {
    TugasNER tugasNER = new TugasNER();
    tugasNER.run(args);
  }
}
