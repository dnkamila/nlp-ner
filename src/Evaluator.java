import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Evaluator {
  public static final String PERSON = "PERSON";
  public static final String LOCATION = "LOCATION";
  public static final String ORGANIZATION = "ORGANIZATION";

  public static BufferedReader brProgram;
  public static BufferedReader brTesting;
  public static List<Result> resultExactMatch = new ArrayList<>();
  public static List<Result> resultMUC = new ArrayList<>();
  private static Result exactMatchResult;
  private static Result mucResult;

  public static void main(String[] args) throws IOException {
    String labeledByProgram = args[0];
    String labeledTesting = args[1];

    setup(labeledByProgram, labeledTesting);
    doValidation(brProgram, brTesting);
    average();
    output();
  }

  private static void average() {
    double recall = 0.0;
    double precision = 0.0;
    double fmeasure = 0.0;
    for (Result result : resultExactMatch) {
      recall += result.getRecall();
      precision += result.getPrecision();
      fmeasure += result.getFmeasure();
    }
    recall /= resultExactMatch.size();
    precision /= resultExactMatch.size();
    fmeasure /= resultExactMatch.size();
    exactMatchResult = new Result(recall, precision, fmeasure);

    recall = 0.0;
    precision = 0.0;
    fmeasure = 0.0;
    for (Result result : resultMUC) {
      recall += result.getRecall();
      precision += result.getPrecision();
      fmeasure += result.getFmeasure();
    }
    recall /= resultMUC.size();
    precision /= resultMUC.size();
    fmeasure /= resultMUC.size();
    mucResult = new Result(recall, precision, fmeasure);
  }

  private static void output() {
    System.out.println("=== Exact Match ===");
    System.out.println(exactMatchResult);
    System.out.println();
    System.out.println("=== MUC ===");
    System.out.println(mucResult);
  }

  private static void doValidation(BufferedReader brProgram, BufferedReader brTesting) throws IOException {
    while(true) {
      String programSentence = brProgram.readLine();
      String testingSentence = brTesting.readLine();
      if (programSentence == null || testingSentence == null) {
        break;
      }

      Map<String, List<String>> extractedProgram = extractLabel(programSentence);
      Map<String, List<String>> extractedTesting = extractLabel(testingSentence);

      doExactMatch(extractedProgram, extractedTesting, resultExactMatch);
      doMUC(extractedProgram, extractedTesting, resultMUC);
    }

  }

  private static void doMUC(Map<String, List<String>> extractedProgram,
                            Map<String, List<String>> extractedTesting,
                            List<Result> results) {
    int textCor = 0;
    int typeCor = 0;

    // PERSON
    List<String> programPerList = extractedProgram.get(PERSON);
    List<String> testingPerList = extractedTesting.get(PERSON);
    for (String ps : programPerList) {
      boolean found = false;
      for (String ts : testingPerList) {
        if (ts.contains(ps)) {
          found = true;
          break;
        }
      }
      if (found)
        typeCor++;
    }

    // LOCATION
    List<String> programLocList = extractedProgram.get(LOCATION);
    List<String> testingLocList = extractedTesting.get(LOCATION);
    for (String ps : programLocList) {
      boolean found = false;
      for (String ts : testingLocList) {
        if (ts.contains(ps)) {
          found = true;
          break;
        }
      }
      if (found)
        typeCor++;
    }
    // ORGANIZATION
    List<String> programOrgList = extractedProgram.get(ORGANIZATION);
    List<String> testingOrgList = extractedTesting.get(ORGANIZATION);
    for (String ps : programOrgList) {
      boolean found = false;
      for (String ts : testingOrgList) {
        if (ts.contains(ps)) {
          found = true;
          break;
        }
      }
      if (found)
        typeCor++;
    }

    List<String> programList = new ArrayList<>();
    programList.addAll(programPerList);
    programList.addAll(programLocList);
    programList.addAll(programOrgList);
    List<String> testingList = new ArrayList<>();
    testingList.addAll(testingPerList);
    testingList.addAll(testingLocList);
    testingList.addAll(testingOrgList);

    // TEXT
    for (String s : programList) {
      if (testingList.contains(s))
        textCor++;
    }

    int cor = textCor + typeCor;
    int act = 2*programList.size();
    int pos = 2*testingList.size();

    Result result = new Result(cor, act, pos);
    results.add(result);
  }

  private static void doExactMatch(Map<String, List<String>> extractedProgram,
                                   Map<String, List<String>> extractedTesting,
                                   List<Result> results) {
    // PERSON
    List<String> programList = extractedProgram.get(PERSON);
    List<String> testingList = extractedTesting.get(PERSON);
    Metrix resultPer = counting(programList, testingList);
    // LOCATION
    programList = extractedProgram.get(LOCATION);
    testingList = extractedTesting.get(LOCATION);
    Metrix resultLoc = counting(programList, testingList);
    // ORGANIZATION
    programList = extractedProgram.get(ORGANIZATION);
    testingList = extractedTesting.get(ORGANIZATION);
    Metrix resultOrg= counting(programList, testingList);

    Result result = new Result(resultPer, resultLoc, resultOrg);
    results.add(result);
  }

  private static Metrix counting(List<String> programList, List<String> testingList) {
    int cor = 0;
    int act = 0;
    int pos = 0;

    for (String s : programList) {
      if (testingList.contains(s)) {
        cor++;
      }
    }
    pos = testingList.size();
    act = programList.size();
    Metrix metrix = new Metrix(cor, act, pos);
    return metrix;
  }


  private static Map<String, List<String>> extractLabel(String sentence) {
    String perRegex = "<ENAMEX TYPE=\"PERSON\">(.*?)</ENAMEX>";
    String locRegex = "<ENAMEX TYPE=\"LOCATION\">(.*?)</ENAMEX>";
    String orgRegex = "<ENAMEX TYPE=\"ORGANIZATION\">(.*?)</ENAMEX>";
    Pattern perPattern = Pattern.compile(perRegex);
    Pattern locPattern = Pattern.compile(locRegex);
    Pattern orgPattern = Pattern.compile(orgRegex);
    Matcher perMatcher = perPattern.matcher(sentence);
    Matcher locMatcher = locPattern.matcher(sentence);
    Matcher orgMatcher = orgPattern.matcher(sentence);

    Map<String, List<String>> map = new LinkedHashMap<>();
    map.put("PERSON", new ArrayList<>());
    map.put("LOCATION", new ArrayList<>());
    map.put("ORGANIZATION", new ArrayList<>());

    while(perMatcher.find()) {
      map.get("PERSON").add(perMatcher.group(1));
    }
    while(locMatcher.find()) {
      map.get("LOCATION").add(locMatcher.group(1));
    }
    while(orgMatcher.find()) {
      map.get("ORGANIZATION").add(orgMatcher.group(1));
    }
    return map;
  }

  private static void setup(String labeledByProgram, String labeledTesting) throws FileNotFoundException {
    brProgram = new BufferedReader(new FileReader(labeledByProgram));
    brTesting = new BufferedReader(new FileReader(labeledTesting));
  }

  public static class Metrix {
    int pos,act,cor;

    public Metrix(int cor, int act, int pos) {
      this.cor = cor;
      this.act = act;
      this.pos = pos;
    }
  }
  public static class Result {
    private double recall;
    private double precision;
    private double fmeasure;

    public Result() {
    }

    public Result(Metrix per, Metrix loc, Metrix org) {
      int cor = per.cor + loc.cor + org.cor;
      int act = per.act + loc.act + org.act;
      int pos = per.pos + loc.pos + org.pos;
      if (pos == 0)
        recall = 0.0;
      else
        recall = (double) cor / pos;
      if (act == 0)
        precision = 0.0;
      else
        precision = (double) cor / act;
      if (recall+precision < 1e-6)
        fmeasure = 0.0;
      else
        fmeasure = (double) 2*recall*precision/ (recall+precision);
    }

    public Result(int cor, int act, int pos) {
      if (pos == 0)
        recall = 0.0;
      else
        recall = (double) cor / pos;
      if (act == 0)
        precision = 0.0;
      else
        precision = (double) cor / act;
      if (recall+precision < 1e-6)
        fmeasure = 0.0;
      else
        fmeasure = (double) 2*recall*precision/ (recall+precision);
    }

    public Result(double recall, double precision, double fmeasure) {
      this.recall = recall;
      this.precision = precision;
      this.fmeasure = fmeasure;
    }

    public double getFmeasure() {
      return fmeasure;
    }

    public void setFmeasure(double fmeasure) {
      this.fmeasure = fmeasure;
    }

    public double getPrecision() {
      return precision;
    }

    public void setPrecision(double precision) {
      this.precision = precision;
    }

    public double getRecall() {
      return recall;
    }

    public void setRecall(double recall) {
      this.recall = recall;
    }

    public String toString() {
      return "recall: " + recall + "\n"
        + "precision: " + precision + "\n"
        + "fmeasure: " + fmeasure;
    }
  }
}
