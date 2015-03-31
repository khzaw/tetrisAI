SKELETON=PlayerSkeleton.java

all: compile run

compile:	PlayerSkeleton.java
	javac -g $(SKELETON)

run:	PlayerSkeleton.class
	java PlayerSkeleton

clean:
	rm *.class