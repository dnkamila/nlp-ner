import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.StringTokenizer;

public class Main {
	private static String corpusFilename = "data/training_data.txt";
	private static String datasetFilename = "data/dataset.txt";

	private static ArrayList<String> corpusToken = new ArrayList<String>();
	private static ArrayList<String> corpusLabel = new ArrayList<String>();
	private static ArrayList<String> corpusPOS = new ArrayList<String>();
	
	private static int limit = 0;
	
	private static String prefixPersonFilename = "data/resources/list-prefix-person.txt";
	private static String prefixOrganizationFilename = "data/resources/list-prefix-organization.txt";
	private static String prefixLocationFilename = "data/resources/list-prefix-location.txt";
	
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
				//TODO : POS-Tagging sentence(s)
				
				String[] temps = s.split("\\t");
				
				s = temps[0];
				s = s.replace("ENAMEX TYPE=", "");
				s = s.replaceAll("(?<=\\S)(?:(?<=\\p{Punct})|(?=\\p{Punct}))(?=\\S)", " ");
				s = s.replace("< \" ", "<\"");
				s = s.replace("< / ", "</");
				s = s.replace(" \" >", "\">");
				s = s.replace(" >", ">");
				s = s.replaceAll("\\s+", " ");

				StringTokenizer st = new StringTokenizer(s);
				String type = "NON";

				//TODO : Match token with its POS
				
				while (st.hasMoreTokens()) {
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
						corpusToken.add(temp);
						corpusLabel.add(type);
					}
				}
				
				corpusToken.add("\n");
				corpusLabel.add("\n");
			}
			corpusToken.remove(corpusToken.size()-1);
			corpusLabel.remove(corpusLabel.size()-1);

			if(corpusToken.size() != corpusLabel.size()) //ADD corpusPOS.size()
				throw new Exception();
			
			limit = corpusToken.size();
		}
		finally {
			br.close();
		}
	}
	
	public static void generateDataset() throws IOException {
		generateResources();
		
		String prefixPerson = "";
		String prefixOrganization = "";
		String prefixLocation = "";
		for(int ii = 0; ii < limit; ii++) {
			String token = corpusToken.get(ii).toLowerCase();
			
			if(token.equals("\n")) {
				prefixPerson = "";
				prefixOrganization = "";
				prefixLocation = "";
			}
			
			System.out.println(token + prefixPerson + prefixOrganization + prefixLocation + " " + corpusLabel.get(ii));
			
			prefixPerson = setPrefixPerson.contains(token) ? " PREFIXPERSON" : "";
			prefixOrganization = setPrefixOrganization.contains(token) ? " PREFIXORGANIZATION" : "";
			prefixLocation = setPrefixLocation.contains(token) ? " PREFIXLOCATION" : "";
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
		while((s = br.readLine()) != null && !s.equals(""))
			set.add(s);
		
		br.close();
	}
}
