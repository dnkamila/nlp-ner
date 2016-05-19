import java.io.*;
import java.util.*;
import java.util.regex.*;
import java.util.zip.*;

import cc.mallet.pipe.*;
import cc.mallet.pipe.iterator.*;
import cc.mallet.types.*;

public class ImportDataDummy {
	public static Pipe generatePipe() {
		ArrayList<Pipe> pipes = new ArrayList<Pipe>();

		pipes.add(new Input2CharSequence("UTF-8"));
		pipes.add(new CharSequence2TokenSequence(Pattern.compile("[\\p{L}\\p{N}_]+")));
		pipes.add(new TokenSequenceLowercase());
		pipes.add(new TokenSequenceRemoveStopwords(false, false));
		pipes.add(new TokenSequence2FeatureSequence());
		pipes.add(new Target2Label());
		pipes.add(new FeatureSequence2FeatureVector());
		pipes.add(new PrintInputAndTarget());

		return new SerialPipes(pipes);
	}

	public static InstanceList generateInstanceList(File[] directories) throws FileNotFoundException, IOException {
		FileIterator iterator = new FileIterator(directories, new TxtFilter(), FileIterator.LAST_DIRECTORY);

		InstanceList instanceList = new InstanceList(generatePipe());
		instanceList.addThruPipe(iterator);

		return instanceList;
	}

	public static void main(String[] args) throws FileNotFoundException, IOException {
		InstanceList instanceList = generateInstanceList(new File[] {new File("data")});
		instanceList.save(new File("data/elizabeth_needham_dummy.txt"));
	}
	
	static class TxtFilter implements FileFilter {
        public boolean accept(File file) {
            return file.toString().endsWith(".txt");
        }
    }
}
