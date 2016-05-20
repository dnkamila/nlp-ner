import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;

public class ConstructData {

    public static BufferedReader br;
    public static BufferedWriter bw;

    public static String END_ENAMEX = "</ENAMEX>";
    public static String START_PERSON_ENAMEX = "<ENAMEX TYPE=\"PERSON\">";
    public static String START_ORGANIZATION_ENAMEX = "<ENAMEX TYPE=\"ORGANIZATION\">";
    public static String START_LOCATION_ENAMEX = "<ENAMEX TYPE=\"LOCATION\">";

    public static void main(String[] args) throws Exception {
        prepare(args);
        construct();
    }
    public static void prepare(String[] args) throws Exception {
        br = new BufferedReader(new FileReader(args[0]));
        bw = new BufferedWriter(new FileWriter(args[1]));
    }
    public static void construct() throws Exception {
        String tanda = ".,'\"";
        String s;
        String lastLabel = "NON";
        String curLabel = "";
        String space = "";
        while((s=br.readLine()) != null) {
            String[] sp = s.split("\\s+");
            if (sp.length != 2) {
                bw.write("\n");
                space = "";
                continue;
            }
            curLabel = sp[1];
            if (!curLabel.equalsIgnoreCase(lastLabel)) {
                if (!"NON".equalsIgnoreCase(lastLabel)) {
                    bw.write(END_ENAMEX + space);
                }
                if ("PERSON".equalsIgnoreCase(curLabel)) {
                    bw.write(space + START_PERSON_ENAMEX);
                }
                if ("ORGANIZATION".equalsIgnoreCase(curLabel)) {
                    bw.write(space + START_ORGANIZATION_ENAMEX);
                }
                if ("LOCATION".equalsIgnoreCase(curLabel)) {
                    bw.write(space + START_LOCATION_ENAMEX);
                }
            } else {
                   bw.write(space);
            }
            lastLabel = curLabel;
            bw.write(sp[0]);
            bw.flush();
            space = " ";
        }
    }
}
