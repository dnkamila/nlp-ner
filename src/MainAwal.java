import java.io.*;
import java.util.StringTokenizer;

/**
 * How to compile:
 * javac Main.java
 *
 * How to run:
 * java Main fileinput fileoutput
 */
public class MainAwal {

  public static void main(String[] args) throws IOException {
    BufferedReader br = new BufferedReader(new FileReader(args[0]));
    BufferedWriter bw = new BufferedWriter(new FileWriter(args[1]));

    String s;
    while ((s = br.readLine()) != null) {
      String sp[] = s.split("\\t");
      s = sp[0];
      s = s.replace("ENAMEX TYPE=", "");
      s = s.replaceAll("(?<=\\S)(?:(?<=\\p{Punct})|(?=\\p{Punct}))(?=\\S)", " ");
      s = s.replace("< \" ", "<\"");
      s = s.replace("< / ", "</");
      s = s.replace(" \" >", "\">");
      s = s.replace(" >", ">");
      s = s.replaceAll("\\s+", " ");

      StringTokenizer st = new StringTokenizer(s);
      String type = "NON";
      // 0 = NON
      // 1 = PERSON
      // 2 = LOCATION
      // 3 = ORGANIZATION
      while(st.hasMoreTokens()) {
        String temp = st.nextToken();
        if ("</ENAMEX>".equals(temp))
          type = "NON";
        else if ("<\"PERSON\">".equals(temp))
          type = "PERSON";
        else if ("<\"LOCATION\">".equals(temp))
          type = "LOCATION";
        else if ("<\"ORGANIZATION\">".equals(temp))
          type = "ORGANIZATION";
        else {
          bw.write(temp + " " + type);
          bw.newLine();
          bw.flush();
        }
      }
      bw.newLine();
      bw.flush();
      System.out.println(sp[1] + " done");
    }
  }
}
