all: compile run

PlayerSkeleton.class: PlayerSkeleton.java Genetic.java
	javac -g PlayerSkeleton.java

compile: PlayerSkeleton.class

run: PlayerSkeleton.class
	java PlayerSkeleton

clean:
	rm *.class
