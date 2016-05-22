Compile :
- javac -cp .:../library/* Main.java
- javac ConstructData.java
- javac Validator.java

Run:
- java -cp .:../library/* Main
- java ConstructData data/labeled.txt data/labeled_sentence.txt
- java Validator data/labeled_sentence.txt data/testing_data.txt
