################################################
#  NB: This Makefile is designed to be called  #
#      from the main PRISM Makefile. It won't  #
#      work on its own because it needs        #
#      various options to be passed in         #
################################################

default: all

all: checks memory-measurer

JOL_JAR = jol-core-0.17.jar

# Try and prevent accidental makes (i.e. called manually, not from top-level Makefile)
checks:
	@if [ "$(LIBSUFFIX)" = "" ]; then \
	  (echo "Error: This Makefile is designed to be called from the main PRISM Makefile"; exit 1) \
	fi; 

memory-measurer:
	@(if [ "$(OSTYPE)" = "linux" ]; then \
	  echo "Copying Java Object Layout JAR"; \
	  cp "$(JOL_JAR)" ../../$(PRISM_LIB_DIR)/ ; \
	fi)

clean: checks
	rm -f "../../$(PRISM_LIB_DIR)/$(JOL_JAR)"

celan:	clean


#################################################
