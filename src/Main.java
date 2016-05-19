import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.StringTokenizer;

import edu.stanford.nlp.tagger.maxent.MaxentTagger;

public class Main {
	private static String corpusFilename = "data/training_data.txt";
	private static String datasetFilename = "data/dataset.txt";

	private static ArrayList<String> corpusToken = new ArrayList<String>();
	private static ArrayList<String> corpusLabel = new ArrayList<String>();
	private static ArrayList<String> corpusPOS = new ArrayList<String>();
	private static ArrayList<String> corpusPrevPOS = new ArrayList<String>();
	private static ArrayList<String> corpusNextPOS = new ArrayList<String>();
	private static ArrayList<String> templateArgs = new ArrayList<String>();

	private static int limit = 0;

	private static String prefixPersonFilename = "data/resources/list-prefix-person.txt";
	private static String prefixOrganizationFilename = "data/resources/list-prefix-organization.txt";
	private static String prefixLocationFilename = "data/resources/list-prefix-location.txt";
	private static String prefixModelFilename = "model/tagger-model";
	private static String filenameFormat = "%s.txt";

	private static HashSet<String> setPrefixPerson = new HashSet<String>();
	private static HashSet<String> setPrefixOrganization = new HashSet<String>();
	private static HashSet<String> setPrefixLocation = new HashSet<String>();

	public static void main(String[] args) throws Exception {
		generateDatasetMaterial();
		generateDataset();
	}

	public static void generateDatasetMaterial() throws Exception {
		BufferedReader br = new BufferedReader(new FileReader("data/resources/dummy.txt"));

		try {
			String s;
			while ((s = br.readLine()) != null && !s.equals("")) {
				// TODO : POS-Tagging sentence(s)

				String[] temps = s.split("\\t");

				s = temps[0];
				s = s.replace("ENAMEX TYPE=", "");
				s = s.replaceAll("(?<=\\S)(?:(?<=\\p{Punct})|(?=\\p{Punct}))(?=\\S)", " ");
				s = s.replace("< \" ", "<\"");
				s = s.replace("< / ", "</");
				s = s.replace(" \" >", "\">");
				s = s.replace(" >", ">");
				s = s.replaceAll("\\s+", " ");

				StringTokenizer stTagged = new StringTokenizer(tagString(s.replaceAll("<[^>]*>", "")));
				StringTokenizer st = new StringTokenizer(s);
				String type = "NON";
				// TODO : Match token with its POS
				while (st.hasMoreTokens()) {
					String temp = st.nextToken();
					String tag = "";

					if ("</ENAMEX>".equals(temp))
						type = "NON";
					else if ("<\"PERSON\">".equals(temp))
						type = "PERSON";
					else if ("<\"LOCATION\">".equals(temp))
						type = "LOCATION";
					else if ("<\"ORGANIZATION\">".equals(temp))
						type = "ORGANIZATION";
					else {
						if (stTagged.hasMoreTokens())
							tag = stTagged.nextToken().split("_")[1];
						corpusToken.add(temp);
						corpusPOS.add(tag);
						corpusLabel.add(type);
					}
				}

				corpusToken.add("\n");
				corpusPOS.add("\n");
				corpusLabel.add("\n");
			}
			corpusToken.remove(corpusToken.size() - 1);
			corpusLabel.remove(corpusLabel.size() - 1);
			corpusPOS.remove(corpusPOS.size() - 1);

			if (corpusToken.size() != corpusLabel.size() && corpusPOS.size() != corpusToken.size()) // ADD
																									// corpusPOS.size()
				throw new Exception();

			limit = corpusToken.size();
		} finally {
			br.close();
		}
	}

	public static void generateDataset() throws IOException {
		generateResources();

		String prefixPerson = "";
		String prefixOrganization = "";
		String prefixLocation = "";
		String prevPOS = "START";
		String nextPOS = "";
		for (int ii = 0; ii < limit; ii++) {
			String token = corpusToken.get(ii).toLowerCase();
			if (ii < limit-1)
				nextPOS = corpusPOS.get(ii+1);
			else if (ii == limit-1 || nextPOS.equals("\n"))
				nextPOS = "END";
			if (token.equals("\n")) {
				prefixPerson = "";
				prefixOrganization = "";
				prefixLocation = "";
				System.out.print("\n");
			} else {
				System.out.println(token + " " + prevPOS + " " + corpusPOS.get(ii) + " " + nextPOS + prefixPerson + prefixOrganization
					+ prefixLocation + " " + corpusLabel.get(ii));

				prefixPerson = setPrefixPerson.contains(token) ? " PREFIXPERSON" : "";
				prefixOrganization = setPrefixOrganization.contains(token) ? " PREFIXORGANIZATION" : "";
				prefixLocation = setPrefixLocation.contains(token) ? " PREFIXLOCATION" : "";
				prevPOS = corpusPOS.get(ii);
				if (prevPOS.equals("\n"))
					prevPOS = "START";
			}
		}
	}

	public static void generateResources() throws IOException {
		loadFileToSet(prefixPersonFilename, setPrefixPerson);
		loadFileToSet(prefixOrganizationFilename, setPrefixOrganization);
		loadFileToSet(prefixLocationFilename, setPrefixLocation);
	}

	public static void loadFileToSet(String filename, HashSet<String> set) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(filename));

		String s;
		while ((s = br.readLine()) != null && !s.equals(""))
			set.add(s);

		br.close();
	}

	private static String tagString(String stringToBeTagged) throws Exception {
		MaxentTagger tagger = new MaxentTagger(prefixModelFilename + ".tagger");
		String tagged = tagger.tagString(stringToBeTagged);
		return tagged.trim();
	}

	public static ArrayList<String> initializeDataset(String filename) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(new File(filename)));
		ArrayList<String> datasetSentences = new ArrayList<String>();

		String line;
		while ((line = br.readLine()) != null) {
			if (line.matches("<kalimat id=\\w{1,}>")) {
				String sentence = "";
				while (!(line = br.readLine()).equals("</kalimat>")) {
					String[] temp = line.split("\\s+");
					for (int ii = 0; ii < temp.length - 1; ii++)
						sentence += temp[ii] + "_" + temp[temp.length - 1] + " ";
				}
				if (!sentence.equals(""))
					datasetSentences.add(sentence);
			}
		}
		br.close();

		return datasetSentences;
	}

	public static void trainDataset(ArrayList<String> templateArgs, String trainFilename) throws Exception {
		templateArgs.add("-trainFile");
		templateArgs.add(trainFilename);

		run(templateArgs);
	}

	public static void run(ArrayList<String> templateArgs) throws Exception {
		String[] args = new String[templateArgs.size()];
		args = templateArgs.toArray(args);
		MaxentTagger.main(args);
	}

}
