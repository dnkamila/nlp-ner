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
import edu.stanford.nlp.tagger.maxent.MaxentTagger;

public class Main {
	private static String corpusFilename = "data/training_data.txt";
	private static String datasetFilename = "data/dataset.txt";
	
	private static String unlabeledFilename = "data/testing_data_unannotated.txt";
	private static String unlabeledDatasetFilename = "data/unlabeled_dataset.txt";
	private static String labeledFilename = "data/labeled.txt";
	private static String modelFilename = "model/ner_crf.model";
	
	private static String tempFilename = "data/temp.txt";
	private static ArrayList<String> corpusToken = new ArrayList<String>();
	private static ArrayList<String> corpusLabel = new ArrayList<String>();
	private static ArrayList<String> corpusPOS = new ArrayList<String>();
	private static ArrayList<String> corpusGazetteer = new ArrayList<String>();

	private static int limit = 0;

	private static String prefixPersonFilename = "data/resources/list-prefix-person.txt";
	private static String prefixOrganizationFilename = "data/resources/list-prefix-organization.txt";
	private static String prefixLocationFilename = "data/resources/list-prefix-location.txt";
	private static String suffixPersonFilename = "data/resources/list-suffix-person.txt";
	private static String suffixOrganizationFilename = "data/resources/list-suffix-organization.txt";
	private static String suffixLocationFilename = "data/resources/list-suffix-location.txt";

	private static String gazetteerFootballClubEuropeFilename = "data/gazetteer/football-club-europe.txt";
	private static String gazetteerFootballClubIndonesiaFilename = "data/gazetteer/football-club-indonesia.txt";
	private static String gazetteerLocationFilename = "data/gazetteer/location.txt";
	private static String gazetteerOrganizationFilename = "data/gazetteer/organization.txt";
	private static String gazetteerPartaiFilename = "data/gazetteer/partai.txt";
	private static String gazetteerUniversitasFilename = "data/gazetteer/universitas.txt";

	private static String prefixModelFilename = "model/tagger-model";

	private static HashSet<String> setPrefixPerson = new HashSet<String>();
	private static HashSet<String> setPrefixOrganization = new HashSet<String>();
	private static HashSet<String> setPrefixLocation = new HashSet<String>();
	private static HashSet<String> setSuffixPerson = new HashSet<String>();
	private static HashSet<String> setSuffixOrganization = new HashSet<String>();
	private static HashSet<String> setSuffixLocation = new HashSet<String>();
	private static HashSet<String> gazetteerFootballClub = new HashSet<>();
	private static HashSet<String> gazetteerOrganization = new HashSet<>();
	private static HashSet<String> gazetteerLocation = new HashSet<>();
	private static HashSet<String> gazetteerUniversity = new HashSet<>();
	private static MaxentTagger tagger = new MaxentTagger(prefixModelFilename + ".tagger");

	public static void main(String[] args) throws Exception {
		/*generateDatasetMaterial(corpusFilename);
		generateDataset(datasetFilename);

		args = new String[5];

		args[0] = "--train";
		args[1] = "true";
		args[2] = "--model-file";
		args[3] = modelFilename;
		args[4] = datasetFilename;

		SimpleTagger.main(args);*/
		generateGazetteer();
		generateDatasetMaterialUnlabeled(unlabeledFilename);
		generateDataset(unlabeledDatasetFilename);

		PrintStream psOutput = new PrintStream(new FileOutputStream(tempFilename));
		System.setOut(psOutput);

		args = new String[3];

		args[0] = "--model-file";
		args[1] = modelFilename;
		args[2] = unlabeledDatasetFilename;

		SimpleTagger.main(args);

		psOutput.close();

		BufferedReader br = new BufferedReader(new FileReader(tempFilename));
		BufferedWriter bw = new BufferedWriter(new FileWriter(labeledFilename));

		for (int ii = 0; ii < corpusToken.size(); ii++) {
			if (!corpusToken.get(ii).equals("\n")) {
				bw.write(corpusToken.get(ii).trim() + " " + br.readLine());
			}
			else {
				br.readLine();
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
				corpusGazetteer.addAll(getGazetteerLabel(s.replaceAll("<[^>]*>", "")));
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
				corpusGazetteer.add("\n");
			}
			corpusToken.remove(corpusToken.size() - 1);
			corpusPOS.remove(corpusPOS.size() - 1);
			corpusLabel.remove(corpusLabel.size() - 1);
			corpusGazetteer.remove(corpusGazetteer.size() - 1);

			if (corpusToken.size() != corpusLabel.size() && corpusPOS.size() != corpusToken.size() && corpusGazetteer.size() != corpusToken.size())
				throw new Exception();

			limit = corpusToken.size();
		} finally {
			br.close();
		}
	}
	
	public static void generateDatasetMaterialUnlabeled(String filename) throws Exception {
		clearCorpusData();

		BufferedReader br = new BufferedReader(new FileReader(filename));
		int i = 0;
		try {
			String s;
			while ((s = br.readLine()) != null && !s.equals("")) {
				i++;
				String[] temps = s.split("\\t");

				s = temps[0];
				s = s.replaceAll("(?<=\\S)(?:(?<=\\p{Punct})|(?=\\p{Punct}))(?=\\S)", " ");

				StringTokenizer stTagged = new StringTokenizer(tagString(s.replaceAll("<[^>]*>", "")));
				StringTokenizer st = new StringTokenizer(s.replaceAll("\\s+", " ").trim());
				corpusGazetteer.addAll(getGazetteerLabel(s.replaceAll("\\s+", " ")));
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
				corpusGazetteer.add("\n");
			}
			corpusToken.remove(corpusToken.size() - 1);
			corpusPOS.remove(corpusPOS.size() - 1);
			corpusGazetteer.remove(corpusGazetteer.size()-1);
			if (corpusToken.size() != corpusPOS.size() && corpusGazetteer.size() != corpusToken.size())
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
						+ allCapitalized + firstToken + " " + prevPOS + " " + corpusPOS.get(ii) + " " + (corpusGazetteer.get(ii).equals("") ? "" : corpusGazetteer.get(ii) + " ") + nextPOS
						+ prefixPerson + prefixOrganization + prefixLocation + suffixPerson + suffixLocation
						+ suffixOrganization + valPrefixOneChar + valPrefixTwoChar + valPrefixThreeChar + " "
						+ (corpusLabel == null || corpusLabel.size() == 0 ? tokenLower : corpusLabel.get(ii)) + "\n");
				// toWrite = toWrite.trim();
				toWrite = toWrite.replaceAll("^\\s+", "");
				bw.write(toWrite);

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
	
	public static void generateGazetteer() throws IOException {
		loadFileToSet(gazetteerFootballClubEuropeFilename,gazetteerFootballClub);
		loadFileToSet(gazetteerFootballClubIndonesiaFilename,gazetteerFootballClub);
		loadFileToSet(gazetteerLocationFilename,gazetteerLocation);
		loadFileToSet(gazetteerOrganizationFilename,gazetteerOrganization);
		loadFileToSet(gazetteerPartaiFilename,gazetteerOrganization);
		loadFileToSet(gazetteerUniversitasFilename,gazetteerUniversity);
	}

	private static void loadFileToSet(String filename, HashSet<String> set) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(filename));

		String s;
		while ((s = br.readLine()) != null && !s.equals(""))
			set.add(s);

		br.close();
	}

	private static String tagString(String stringToBeTagged) throws Exception {
		String tagged = tagger.tagString(stringToBeTagged);
		return tagged.trim();
	}

	private static void clearCorpusData() {
		corpusToken.clear();
		corpusPOS.clear();
		corpusLabel.clear();
		corpusGazetteer.clear();
	}

	private static void clearCorpusData(ArrayList<String> arr) {
		arr.clear();
	}

	private static List<String> getGazetteerLabel(String s) {
		String[] splittedS = s.replaceAll("\\s+", " ").trim().split(" ");
		List<String> joinedLabel = new ArrayList<>();
		List<String> cleanedWord = new ArrayList<>();
		for (int i = 0; i < splittedS.length; i++) {
			if (!splittedS[i].equals("")) {
				joinedLabel.add("");
				cleanedWord.add(splittedS[i]);
			}
		}
		List<List<String>> notJoinedLabel = new ArrayList<>();
		// TODO tentuin siapa yg boleh nimpa kalo ada problem
		// misal universitas indonesia bakal di tag di univ dan di location
		// yg harusnya diambil di letakin paling bawah (rulenya nimpa tag yg lama)
		notJoinedLabel.add(getListLabel(gazetteerLocation, s.replaceAll("\\s+", " "), cleanedWord, "GLOC"));
		notJoinedLabel.add(getListLabel(gazetteerFootballClub, s.replaceAll("\\s+", " "), cleanedWord, "GORG"));
		notJoinedLabel.add(getListLabel(gazetteerOrganization, s.replaceAll("\\s+", " "), cleanedWord, "GORG"));
		notJoinedLabel.add(getListLabel(gazetteerUniversity, s.replaceAll("\\s+", " "), cleanedWord, "GUNIV"));
		for (int i = 0; i < notJoinedLabel.size(); i++) {
			for (int j = 0; j < cleanedWord.size(); j++) {
				if (joinedLabel.get(i) == null || joinedLabel.get(i).equals(""))
					joinedLabel.set(j, notJoinedLabel.get(i).get(j));
			}
		}
		return joinedLabel;
	}
	
	private static List<String> getListLabel(HashSet<String> gazetteerSet, String s, List<String> cleanedWord, String label) {
		List<String> currentGLabel = new ArrayList<>();
		for (int i = 0; i < cleanedWord.size(); i++)
			currentGLabel.add("");
		for (String g : gazetteerSet) {
			if (s.toLowerCase().contains(g.toLowerCase())) {
				String[] splittedG = g.split(" ");
				for (int i = 0; i < cleanedWord.size(); i++) {
					if (splittedG[0].toLowerCase().equals(cleanedWord.get(i).toLowerCase())) {
						for (int j = 0; j < splittedG.length; j++) {
							if (i+j < currentGLabel.size())
								currentGLabel.set(i+j, label);
						}
					}
				}
			}
		}
		return currentGLabel;
	}
}
