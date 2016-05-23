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
