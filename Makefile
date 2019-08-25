JAVAC = javac
JFLAGS = -g

BINDIR = ./bin
SRCDIR = ./src
DOCDIR = ./doc

SOURCES = CloudData.java

.SUFFIXES: .java .class


$(BINDIR)/%.class: $(SRCDIR)/%.java
	$(JAVAC) $(JFLAGS) $< -cp $(BINDIR) -d $(BINDIR)

all: $(BINDIR)/CloudData.class

$(BINDIR)/CloudData.class: $(SRCDIR)/CloudData.java 


docs: 
	javadoc -version -author -classpath $(BINDIR) -d $(DOCDIR) $(SRCDIR)/*.java

cleandocs:
	rm -rf $(DOCDIR)/*

clean:
	@rm -f $(BINDIR)/*.class
