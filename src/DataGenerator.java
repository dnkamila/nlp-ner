import edu.stanford.nlp.tagger.maxent.MaxentTagger;

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.StringTokenizer;

public class DataGenerator {
  private ArrayList<String> corpusToken = new ArrayList<String>();
  private ArrayList<String> corpusLabel = new ArrayList<String>();
  private ArrayList<String> corpusPOS = new ArrayList<String>();
  private ArrayList<String> corpusGazetteer = new ArrayList<String>();

  public static String TEMP_FILE_NAME = "data/temp.txt";
  private String prefixPersonFilename = "data/resources/list-prefix-person.txt";
  private String prefixOrganizationFilename = "data/resources/list-prefix-organization.txt";
  private String prefixLocationFilename = "data/resources/list-prefix-location.txt";
  private String suffixPersonFilename = "data/resources/list-suffix-person.txt";
  private String suffixOrganizationFilename = "data/resources/list-suffix-organization.txt";
  private String suffixLocationFilename = "data/resources/list-suffix-location.txt";

  private String gazetteerFootballClubEuropeFilename = "data/gazetteer/football-club-europe.txt";
  private String gazetteerFootballClubIndonesiaFilename = "data/gazetteer/football-club-indonesia.txt";
  private String gazetteerLocationFilename = "data/gazetteer/location.txt";
  private String gazetteerOrganizationFilename = "data/gazetteer/organization.txt";
  private String gazetteerPartaiFilename = "data/gazetteer/partai.txt";
  private String gazetteerUniversitasFilename = "data/gazetteer/universitas.txt";

  private int limit = 0;

  private HashSet<String> setPrefixPerson = new HashSet<String>();
  private HashSet<String> setPrefixOrganization = new HashSet<String>();
  private HashSet<String> setPrefixLocation = new HashSet<String>();
  private HashSet<String> setSuffixPerson = new HashSet<String>();
  private HashSet<String> setSuffixOrganization = new HashSet<String>();
  private HashSet<String> setSuffixLocation = new HashSet<String>();
  private HashSet<String> gazetteerFootballClub = new HashSet<>();
  private HashSet<String> gazetteerOrganization = new HashSet<>();
  private HashSet<String> gazetteerLocation = new HashSet<>();
  private HashSet<String> gazetteerUniversity = new HashSet<>();
  private String prefixModelFilename = "model/tagger-model";
  private MaxentTagger tagger = new MaxentTagger(prefixModelFilename + ".tagger");


  public DataGenerator() {}

  private void clearCorpusData() {
    corpusToken.clear();
    corpusPOS.clear();
    corpusLabel.clear();
    corpusGazetteer.clear();
  }

  public void generateDataset(String outputFileName, int featureNumber) throws IOException {
    BufferedWriter bw = new BufferedWriter(new FileWriter(outputFileName));
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

        String toWrite = "";
        switch (featureNumber) {
          case 1:
            toWrite = ((corpusLabel == null || corpusLabel.size() == 0 ? "" : tokenLower) + firstCapitalized
              + allCapitalized + firstToken + valPrefixOneChar + valPrefixTwoChar + valPrefixThreeChar + " "
              + (corpusLabel == null || corpusLabel.size() == 0 ? tokenLower : corpusLabel.get(ii)));
            break;
          case 2:
            toWrite = ((corpusLabel == null || corpusLabel.size() == 0 ? "" : tokenLower) + firstCapitalized
              + allCapitalized + firstToken + " " + corpusPOS.get(ii) + " " + (corpusGazetteer.get(ii).equals("") ? "" : (corpusGazetteer.get(ii) + " "))
              + valPrefixOneChar + valPrefixTwoChar + valPrefixThreeChar + " "
              + (corpusLabel == null || corpusLabel.size() == 0 ? tokenLower : corpusLabel.get(ii)));
            break;
          case 3:
            toWrite = ((corpusLabel == null || corpusLabel.size() == 0 ? "" : tokenLower) + firstCapitalized
              + allCapitalized + firstToken + " " + prevPOS + " " + corpusPOS.get(ii) + " " + (corpusGazetteer.get(ii).equals("") ? "" : (corpusGazetteer.get(ii) + " ")) + nextPOS
              + prefixPerson + prefixOrganization + prefixLocation + suffixPerson + suffixLocation
              + suffixOrganization + valPrefixOneChar + valPrefixTwoChar + valPrefixThreeChar + " "
              + (corpusLabel == null || corpusLabel.size() == 0 ? tokenLower : corpusLabel.get(ii)));
            break;
        }

        toWrite = toWrite.replaceAll("\\s+", " ") + "\n";
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

  public void generateDatasetMaterialUnlabeled(String filename) throws Exception {
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

  public void generateDatasetMaterial(String filename) throws Exception {
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
        corpusGazetteer.addAll(getGazetteerLabel(s.replaceAll("\\s+", " ")));
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

  private String tagString(String stringToBeTagged) throws Exception {
    String tagged = tagger.tagString(stringToBeTagged);
    return tagged.trim();
  }

  private List<String> getGazetteerLabel(String s) {
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
  private List<String> getListLabel(HashSet<String> gazetteerSet, String s, List<String> cleanedWord, String label) {
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

  private void loadFileToSet(String filename, HashSet<String> set) throws IOException {
    BufferedReader br = new BufferedReader(new FileReader(filename));

    String s;
    while ((s = br.readLine()) != null && !s.equals(""))
      set.add(s);

    br.close();
  }

  public void generateGazetteer() throws IOException {
    loadFileToSet(gazetteerFootballClubEuropeFilename,gazetteerFootballClub);
    loadFileToSet(gazetteerFootballClubIndonesiaFilename,gazetteerFootballClub);
    loadFileToSet(gazetteerLocationFilename,gazetteerLocation);
    loadFileToSet(gazetteerOrganizationFilename,gazetteerOrganization);
    loadFileToSet(gazetteerPartaiFilename,gazetteerOrganization);
    loadFileToSet(gazetteerUniversitasFilename,gazetteerUniversity);
  }

  public void generateResources() throws IOException {
    loadFileToSet(prefixPersonFilename, setPrefixPerson);
    loadFileToSet(prefixOrganizationFilename, setPrefixOrganization);
    loadFileToSet(prefixLocationFilename, setPrefixLocation);
    loadFileToSet(suffixPersonFilename, setSuffixPerson);
    loadFileToSet(suffixOrganizationFilename, setSuffixOrganization);
    loadFileToSet(suffixLocationFilename, setSuffixLocation);
  }

  public boolean firstCapitalized(String token) {
    if (null == token || token.isEmpty())
      return false;
    return (Character.isUpperCase(token.codePointAt(0)));
  }

  public boolean allCapitalized(String token) {
    for (int i = 0; i < token.length(); i++) {
      if (!Character.isUpperCase(token.charAt(i)))
        return false;
    }
    return true;
  }

  public void printLabeledData() throws Exception {
    BufferedReader br = new BufferedReader(new FileReader(TEMP_FILE_NAME));
    BufferedWriter bw = new BufferedWriter(new FileWriter(TugasNER.LABELED_FILE_NAME));

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
}
