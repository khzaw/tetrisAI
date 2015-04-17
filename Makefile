all: compile run

PlayerSkeleton.class: PlayerSkeleton.java PSO.java
	javac -g PlayerSkeleton.java PSO.java

compile: PlayerSkeleton.class

run: PlayerSkeleton.class
	java PlayerSkeleton

clean:
	rm *.class
