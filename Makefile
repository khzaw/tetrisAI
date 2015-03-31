SKELETON=PlayerSkeleton.java

all: compile run

Genetic.class: $(SKELETON) Genetic.java
	javac -g $(SKELETON)
	javac -g Genetic.java

compile: Genetic.class

run:	Genetic.class
	java Genetic

clean:
	rm *.class
