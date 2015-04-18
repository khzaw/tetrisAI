all: compile run

PlayerSkeleton.class: PlayerSkeleton.java
	javac -g PlayerSkeleton.java

compile: PlayerSkeleton.class

run: PlayerSkeleton.class
	java PlayerSkeleton

report: report.tex
	pdflatex report.tex

clean:
	rm *.class
