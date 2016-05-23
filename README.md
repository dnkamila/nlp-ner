Jalankan semua command dibawah ini dari dalam directory *src*

Compile:
- [ubuntu]	javac -cp .:library/* *.javac
- [windows]	javac -cp .;library/* *.java

Run Training:
- [ubuntu]	java -cp .:library/* TugasNER train
- [windows]	java -cp .;library/* TugasNER train

Run Testing:
- [ubuntu]	java -cp .:library/* TugasNER
- [windows]	java -cp .;library/* TugasNER

Run Evaluator:
- java Evaluator data/output.txt data/testing_data.txt

===========================================================================

Struktur Source Code
- src
	- data
		- gazetteer
			- football-club-europe.txt
			- football-club-indonesia.txt
			- location.txt
			- organization.txt
			- partai.txt
			- universitas.txt
		- resources
			- list-prefix-person.txt
			- list-prefix-organization.txt
			- list-prefix-location.txt
			- list-suffix-person.txt
			- list-suffix-organization.txt
			- list-suffix-location.txt
		- dataset.txt					= dataset dari training data
		- input.txt						= testing_data_unannotated.txt
		- labeled.txt
		- output.txt					= hasil labeling testing data
		- temp.txt
		- testing_data.txt				= gold standard
		- testing_data_unannotated.txt	= testing data untuk diuji dengan gold standard	
		- training_data.txt				= training data
		- unlabeled_dataset.txt			= dataset dari testing data
	- library
		- mallet.jar
		- mallet-deps.jar
		- slf4j.jar
		- stanford-parser.jar
		- stanford-postagger.jar
	- model
		- tagger-model.tagger	= model tagger pada Tugas 1
		- ner_crf.model			= model crf yang dihasilkan dari Run Training
	- Construct.java
	- DataGenerator.java
	- Evaluator.java
	- TrainCRF.java
	- TrainHMM.java
	- TugasNER.java