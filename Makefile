SKELETON=PlayerSkeleton.java

all: compile run

compile:	PlayerSkeleton.java
	javac $(SKELETON)

run:	PlayerSkeleton.class
	java PlayerSkeleton
