# You can set:
# 
#  EXECUTABLE_NAME - the path to write the resulting executable to; defaults
#                    to the name of the current directory, in the current
#                    directory.

# ----------------------------------------------------------------------------
# 
# ----------------------------------------------------------------------------

CURRENT_DIRECTORY := $(shell pwd)
EXECUTABLE_NAME ?= $(shell basename $(CURRENT_DIRECTORY))

# ----------------------------------------------------------------------------
# Rules for compiling Objective C and Objective C++ source.
# ----------------------------------------------------------------------------

COMPILE.m = $(COMPILE.c)
.SUFFIXES: .o .m
%.o: %.m
	$(COMPILE.m) $(OUTPUT_OPTION) $<

COMPILE.mm = $(COMPILE.cpp)
.SUFFIXES: .o .mm
%.o: %.mm
	$(COMPILE.mm) $(OUTPUT_OPTION) $<

# ----------------------------------------------------------------------------
# Sensible compiler/linker flags.
# ----------------------------------------------------------------------------

# Hack around include paths by putting all directories on the path.
SOURCE_DIRECTORIES=$(shell find `pwd`/src '(' -type d -name '.svn' -prune ')' -or -type d)
SOURCE_DIRECTORIES:=$(patsubst %/.svn,,$(SOURCE_DIRECTORIES))
C_AND_CXX_FLAGS += $(patsubst %,-I%,$(SOURCE_DIRECTORIES))

C_AND_CXX_FLAGS += -g -W -Wall #-Werror
CFLAGS += $(C_AND_CXX_FLAGS)
CXXFLAGS += $(C_AND_CXX_FLAGS)

CFLAGS += -std=c99
LDFLAGS += -framework Cocoa

# ----------------------------------------------------------------------------
# Find the source and work out what we need to build.
# ----------------------------------------------------------------------------

SOURCE_EXTENSIONS += c
SOURCE_EXTENSIONS += cpp
SOURCE_EXTENSIONS += m
SOURCE_EXTENSIONS += mm

# FIXME: this belongs in "boilerplate.make".
tail = $(wordlist 2,$(words $(1)),$(1))

FIND_EXPRESSION := $(call tail,$(foreach EXTENSION,$(SOURCE_EXTENSIONS),-or -name "*.$(EXTENSION)"))
SOURCE_FILES = $(shell find `pwd`/src -type f '(' $(FIND_EXPRESSION) ')')
OBJECT_FILES = $(foreach EXTENSION,$(SOURCE_EXTENSIONS),$(patsubst %.$(EXTENSION),%.o,$(filter %.$(EXTENSION),$(SOURCE_FILES))))

# ----------------------------------------------------------------------------
# Our targets; the executable (the default target), and "clean".
# ----------------------------------------------------------------------------

$(EXECUTABLE_NAME): $(OBJECT_FILES)
	$(CXX) -o $(EXECUTABLE_NAME) $(LDFLAGS) $^

.PHONY: clean
clean:
	rm -f $(EXECUTABLE_NAME) $(OBJECT_FILES)
