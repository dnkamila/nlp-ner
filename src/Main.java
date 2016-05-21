import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.StringTokenizer;

import cc.mallet.fst.SimpleTagger;
import edu.stanford.nlp.ling.CoreAnnotations.GazetteerAnnotation;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;

public class Main {
	private static String corpusFilename = "data/training_data.txt";
	private static String datasetFilename = "data/dataset.txt";
	private static String unlabeledFilename = "data/testing_data_unannotated.txt";
	private static String unlabeledDatasetFilename = "data/unlabeled_dataset.txt";
	private static String labeledFilename = "data/labeled.txt";

	private static ArrayList<String> corpusToken = new ArrayList<String>();
	private static ArrayList<String> corpusLabel = new ArrayList<String>();
	private static ArrayList<String> corpusPOS = new ArrayList<String>();

	private static int limit = 0;

	private static String prefixPersonFilename = "data/resources/list-prefix-person.txt";
	private static String prefixOrganizationFilename = "data/resources/list-prefix-organization.txt";
	private static String prefixLocationFilename = "data/resources/list-prefix-location.txt";
	private static String suffixPersonFilename = "data/resources/list-suffix-person.txt";
	private static String suffixOrganizationFilename = "data/resources/list-suffix-organization.txt";
	private static String suffixLocationFilename = "data/resources/list-suffix-location.txt";

	private static String gazetteerFootballClubEuropeFilename = "data/gazetteer/football-club-europe.txt";
	private static String gazetteerFootballClubIndonesiaFilename = "data/gazetteer/football-club-europe.txt";
	private static String gazetteerLocationFilename = "data/gazetteer/location.txt";
	private static String gazetteerOrganizationFilename = "data/gazetteer/organization.txt";
	private static String gazetteerPartaiFilename = "data/gazetteer/football-club-europe.txt";
	private static String gazetteerUniversitasFilename = "data/gazetteer/football-club-europe.txt";

	private static String prefixModelFilename = "model/tagger-model";

	private static HashSet<String> setPrefixPerson = new HashSet<String>();
	private static HashSet<String> setPrefixOrganization = new HashSet<String>();
	private static HashSet<String> setPrefixLocation = new HashSet<String>();
	private static HashSet<String> setSuffixPerson = new HashSet<String>();
	private static HashSet<String> setSuffixOrganization = new HashSet<String>();
	private static HashSet<String> setSuffixLocation = new HashSet<String>();
	private static List<String> gazetteerFootballClub = new ArrayList<>();
	private static List<String> gazetteerOrganization = new ArrayList<>();
	private static List<String> gazetteerLocation = new ArrayList<>();
	private static List<String> gazetteerUniversity = new ArrayList<>();

	public static void main(String[] args) throws Exception {

		generateDatasetMaterial(corpusFilename);
		generateDataset(datasetFilename);

		generateDatasetMaterialUnlabeled(unlabeledFilename);
		generateDataset(unlabeledDatasetFilename);

		TrainCRF.run("data/dataset.txt", "model/ner_crf.model");

		PrintStream psOutput = new PrintStream(new FileOutputStream("data/temp.txt"));
		System.setOut(psOutput);

		args = new String[3];

		args[0] = "--model-file";
		args[1] = "model/ner_crf.model";
		args[2] = unlabeledDatasetFilename;

		SimpleTagger.main(args);

		psOutput.close();

		BufferedReader br = new BufferedReader(new FileReader("data/temp.txt"));
		BufferedWriter bw = new BufferedWriter(new FileWriter(labeledFilename));

		for (int ii = 0; ii < corpusToken.size(); ii++) {
			if (!corpusToken.get(ii).equals("\n")) {
				bw.write(corpusToken.get(ii).trim() + " " + br.readLine());
			}
			bw.write("\n");
		}

		br.close();
		bw.close();
	}

	public static void generateDatasetMaterial(String filename) throws Exception {
		clearCorpusData();

		BufferedReader br = new BufferedReader(new FileReader(filename));

		try {
			String s;
			while ((s = br.readLine()) != null && !s.equals("")) {
				String[] temps = s.split("\\t");

				s = temps[0];
				s = s.replace("enamex", "ENAMEX");
				s = s.replace("type", "TYPE");
				s = s.replace("ENAMEX TYPE=", "");
				s = s.replace("/ENAMEX TYPE=", "");
				s = s.replace("ENAMEX ", "ENAMEX>");
				s = s.replaceAll("(?<=\\S)(?:(?<=\\p{Punct})|(?=\\p{Punct}))(?=\\S)", " ");
				s = s.replace("(<", "( <");
				s = s.replace(">)", "> )");
				s = s.replace("< \" ", "<\"");
				s = s.replace("< / ", "</");
				s = s.replace(" \" >", "\">");
				s = s.replace(" >", ">");
				s = s.replaceAll("\\s+", " ");

				StringTokenizer stTagged = new StringTokenizer(tagString(s.replaceAll("<[^>]*>", "")));
				insertGazzetteer(s.replaceAll("<[^>]*>", ""));
				StringTokenizer st = new StringTokenizer(s);
				String type = "NON";

				while (st.hasMoreTokens()) {
					String temp = st.nextToken();
					String tag = "";

					if ("</ENAMEX>".equalsIgnoreCase(temp))
						type = "NON";
					else if ("<\"PERSON\">".equalsIgnoreCase(temp))
						type = "PERSON";
					else if ("<\"LOCATION\">".equalsIgnoreCase(temp))
						type = "LOCATION";
					else if ("<\"ORGANIZATION\">".equalsIgnoreCase(temp))
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
			corpusPOS.remove(corpusPOS.size() - 1);
			corpusLabel.remove(corpusLabel.size() - 1);

			if (corpusToken.size() != corpusLabel.size() && corpusPOS.size() != corpusToken.size())
				throw new Exception();

			limit = corpusToken.size();
		} finally {
			br.close();
		}
	}

	public static void generateDataset(String filename) throws IOException {
		BufferedWriter bw = new BufferedWriter(new FileWriter(filename));
		generateResources();

		String firstToken = " FIRSTOKEN";
		String firstCapitalized = "";
		String allCapitalized = "";
		String prefixPerson = "";
		String prefixOrganization = "";
		String prefixLocation = "";
		String suffixPerson = "";
		String suffixLocation = "";
		String suffixOrganization = "";
		String prevPOS = "START";
		String nextPOS = "";
		String nextTokenRaw = "";
		String nextTokenLower = "";

		for (int ii = 0; ii < limit; ii++) {
			String tokenRaw = corpusToken.get(ii);
			String tokenLower = corpusToken.get(ii).toLowerCase();

			String valPrefixOneChar = tokenRaw.length() < 1 ? " P1=" + tokenRaw : " P1=" + tokenRaw.substring(0, 1);
			String valPrefixTwoChar = tokenRaw.length() < 2 ? " P2=" + tokenRaw : " P2=" + tokenRaw.substring(0, 2);
			String valPrefixThreeChar = tokenRaw.length() < 3 ? " P3=" + tokenRaw : " P3=" + tokenRaw.substring(0, 3);

			if (ii == limit - 1) {
				nextPOS = "END";
				nextTokenRaw = "";
			} else {
				nextPOS = corpusPOS.get(ii + 1);
				nextTokenRaw = corpusToken.get(ii + 1);
			}

			if (nextPOS.equals("\n"))
				nextPOS = "END";

			if (tokenRaw.equals("\n")) {
				firstToken = " FIRSTTOKEN";
				firstCapitalized = "";
				allCapitalized = "";
				prefixPerson = "";
				prefixOrganization = "";
				prefixLocation = "";
				suffixPerson = "";
				suffixOrganization = "";
				suffixLocation = "";
				valPrefixOneChar = "";
				valPrefixTwoChar = "";
				valPrefixThreeChar = "";
				bw.write("\n");
			} else {
				nextTokenLower = nextTokenRaw.toLowerCase();
				suffixPerson = setSuffixPerson.contains(nextTokenLower) ? " SUFFIXPERSON" : "";
				suffixLocation = setSuffixLocation.contains(nextTokenLower) ? " SUFFIXLOCATION" : "";
				suffixOrganization = setSuffixOrganization.contains(nextTokenLower) ? " SUFFIXORGANIZATION" : "";

				firstCapitalized = firstCapitalized(tokenRaw) ? " FIRSTCAPITALIZED" : "";
				allCapitalized = allCapitalized(tokenRaw) ? " ALLCAPITALIZED" : "";

				String toWrite = ((corpusLabel == null || corpusLabel.size() == 0 ? "" : tokenLower) + firstCapitalized
						+ allCapitalized + firstToken + " " + prevPOS + " "
						+ (corpusPOS.get(ii) == null ? "NN" : corpusPOS.get(ii)) + " " + nextPOS + prefixPerson
						+ prefixOrganization + prefixLocation + suffixPerson + suffixLocation + suffixOrganization
						+ valPrefixOneChar + valPrefixTwoChar + valPrefixThreeChar + " "
						+ (corpusLabel == null || corpusLabel.size() == 0 ? tokenLower : corpusLabel.get(ii)));

				toWrite = toWrite.trim();
				bw.write(toWrite + "\n");

				prefixPerson = setPrefixPerson.contains(tokenLower) ? " PREFIXPERSON" : "";
				prefixOrganization = setPrefixOrganization.contains(tokenLower) ? " PREFIXORGANIZATION" : "";
				prefixLocation = setPrefixLocation.contains(tokenLower) ? " PREFIXLOCATION" : "";

				prevPOS = corpusPOS.get(ii);
				firstToken = "";
				if (prevPOS.equals("\n"))
					prevPOS = "START";
			}
		}
		bw.close();
	}

	public static boolean firstCapitalized(String token) {
		if (null == token || token.isEmpty())
			return false;

		return (Character.isUpperCase(token.codePointAt(0)));
	}

	public static boolean allCapitalized(String token) {
		for (int i = 0; i < token.length(); i++) {
			if (!Character.isUpperCase(token.charAt(i)))
				return false;
		}

		return true;
	}

	public static void generateResources() throws IOException {
		loadFileToSet(prefixPersonFilename, setPrefixPerson);
		loadFileToSet(prefixOrganizationFilename, setPrefixOrganization);
		loadFileToSet(prefixLocationFilename, setPrefixLocation);
		loadFileToSet(suffixPersonFilename, setSuffixPerson);
		loadFileToSet(suffixOrganizationFilename, setSuffixOrganization);
		loadFileToSet(suffixLocationFilename, setSuffixLocation);
	}

	private static void loadFileToSet(String filename, HashSet<String> set) throws IOException {
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

	public static void generateDatasetMaterialUnlabeled(String filename) throws Exception {
		clearCorpusData();

		BufferedReader br = new BufferedReader(new FileReader(filename));

		try {
			String s;
			while ((s = br.readLine()) != null && !s.equals("")) {
				String[] temps = s.split("\\t");

				s = temps[0];
				s = s.replaceAll("(?<=\\S)(?:(?<=\\p{Punct})|(?=\\p{Punct}))(?=\\S)", " ");

				StringTokenizer stTagged = new StringTokenizer(tagString(s.replaceAll("<[^>]*>", "")));
				StringTokenizer st = new StringTokenizer(s);

				while (st.hasMoreTokens()) {
					String temp = st.nextToken();
					String tag = "";

					if (stTagged.hasMoreTokens())
						tag = stTagged.nextToken().split("_")[1];

					corpusToken.add(temp);
					corpusPOS.add(tag);
				}

				corpusToken.add("\n");
				corpusPOS.add("\n");
			}
			corpusToken.remove(corpusToken.size() - 1);
			corpusPOS.remove(corpusPOS.size() - 1);

			if (corpusToken.size() != corpusPOS.size())
				throw new Exception();

			limit = corpusToken.size();
		} finally {
			br.close();
		}
	}

	private static void readGazetteer() throws Exception {
		BufferedReader reader = new BufferedReader(new FileReader(gazetteerFootballClubEuropeFilename));
		try {
			String s;
			while ((s = reader.readLine()) != null && !s.equals("")) {
				gazetteerFootballClub.add(s.trim());
			}
			reader.close();

			reader = new BufferedReader(new FileReader(gazetteerFootballClubIndonesiaFilename));

			while ((s = reader.readLine()) != null && !s.equals("")) {
				gazetteerFootballClub.add(s.trim());
			}
			reader.close();

			reader = new BufferedReader(new FileReader(gazetteerLocationFilename));
			while ((s = reader.readLine()) != null && !s.equals("")) {
				gazetteerLocation.add(s.trim());
			}
			reader.close();

			reader = new BufferedReader(new FileReader(gazetteerOrganizationFilename));
			while ((s = reader.readLine()) != null && !s.equals("")) {
				gazetteerOrganization.add(s.trim());
			}
			reader.close();

			reader = new BufferedReader(new FileReader(gazetteerPartaiFilename));
			while ((s = reader.readLine()) != null && !s.equals("")) {
				gazetteerOrganization.add(s.trim());
			}
			reader.close();

			reader = new BufferedReader(new FileReader(gazetteerUniversitasFilename));
			while ((s = reader.readLine()) != null && !s.equals("")) {
				gazetteerUniversity.add(s.trim());
			}
		} finally {
			reader.close();
		}
	}

	private static void insertGazzetteer(String s) {
		List<List<String>> gazetteerResult = new ArrayList<>(4);
		for (int i = 0; i < gazetteerResult.size(); i++)
			gazetteerResult.add(new ArrayList<String>());
		for (String gFootball : gazetteerFootballClub) {
			if (s.toLowerCase().contains(gFootball.toLowerCase())) {
				StringTokenizer gToken = new StringTokenizer(gFootball);  
				List<String> tagFootball = gazetteerResult.get(0);
				  
			}
		}
	}

	private static void clearCorpusData() {
		corpusToken.clear();
		corpusPOS.clear();
		corpusLabel.clear();
	}

	private static void clearCorpusData(ArrayList<String> arr) {
		arr.clear();
	}
}
