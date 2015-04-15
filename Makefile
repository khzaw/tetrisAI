all: compile run

PlayerSkeleton.class: PlayerSkeleton.java SquaredError.java
	javac -g PlayerSkeleton.java SquaredError.java

compile: PlayerSkeleton.class

run: PlayerSkeleton.class
	java PlayerSkeleton

clean:
	rm *.class
