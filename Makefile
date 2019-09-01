JAVAC = javac
JFLAGS = -g

BINDIR = ./bin
SRCDIR = ./src
DOCDIR = ./doc

SOURCES = CloudData.java CloudDataParallel.java SumArray.java WriteClouds.java

.SUFFIXES: .java .class


$(BINDIR)/%.class: $(SRCDIR)/%.java
	$(JAVAC) $(JFLAGS) $< -cp $(BINDIR) -d $(BINDIR)

all: $(BINDIR)/CloudData.class $(BINDIR)/CloudDataParallel.class

$(BINDIR)/CloudData.class: $(SRCDIR)/CloudData.java 

$(BINDIR)/CloudDataParallel.class: $(BINDIR)/SumArray.class  $(BINDIR)/WriteClouds.class $(SRCDIR)/CloudDataParallel.java


docs: 
	javadoc -version -author -classpath $(BINDIR) -d $(DOCDIR) $(SRCDIR)/*.java

cleandocs:
	rm -rf $(DOCDIR)/*

clean:
	@rm -f $(BINDIR)/*.class
