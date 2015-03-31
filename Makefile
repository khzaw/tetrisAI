SKELETON=Genetic.java

all: compile run

compile:	Genetic.java
	javac -g $(SKELETON)

run:	Genetic.class
	java Genetic

clean:
	rm *.class
