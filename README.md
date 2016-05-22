Compile :
- javac -cp .:../library/* *.java

Run Training:
- java -cp .:../library/* TugasNER train

Run Testing:
- java -cp .:../library/* TugasNER
- java Evaluator data/output.txt data/testing_data.txt
