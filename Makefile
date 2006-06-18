SRC     = src

BUILD   = build

BUILD_ROOT := $(shell pwd)
BUILD_PLATFORM := $(shell sh $(BUILD_ROOT)/../ZimbraBuild/rpmconf/Build/get_plat_tag.sh)

SHARED := -shared
JAVAINC := -I/usr/local/java/include -I/usr/local/java/include/linux
SHARED_EXT := so
PUSHED_EXT := so.Linux.i386

ifeq ($(BUILD_PLATFORM), MACOSX)
JAVAINC := -I/System/Library/Frameworks/JavaVM.framework/Headers
SHARED := -dynamiclib
MACDEF := -DDARWIN
SHARED_EXT := jnilib
LIB_OPTS := -install_name /opt/zimbra/lib/libzimbra-native.$(SHARED_EXT) -framework JavaVM
JAVA_BINARY = /usr/bin/java
PUSHED_EXT := so.MacOSX.ppc
endif

ifeq ($(BUILD_PLATFORM), MACOSXx86)
JAVAINC := -I/System/Library/Frameworks/JavaVM.framework/Headers
SHARED := -dynamiclib
MACDEF := -DDARWIN
SHARED_EXT := jnilib
LIB_OPTS := -install_name /opt/zimbra/lib/libzimbra-native.$(SHARED_EXT) -framework JavaVM
JAVA_BINARY = /usr/bin/java
PUSHED_EXT := so.MacOSX.i386
endif

CLASSES = $(BUILD)/classes

JAVA_FILES = \
	com/zimbra/znative/IO.java \
	com/zimbra/znative/Process.java \
	com/zimbra/znative/ProcessorUsage.java \
	com/zimbra/znative/ResourceUsage.java \
	com/zimbra/znative/Util.java \
	com/zimbra/znative/OperationFailedException.java \
	com/zimbra/znative/tests/HardLinkTest.java \
	com/zimbra/znative/tests/LinkCountTest.java \
	com/zimbra/znative/tests/ProcessTest.java \
	com/zimbra/znative/tests/UsageTest.java

JAVA_SOURCES = $(patsubst %,$(SRC)/java/%,$(JAVA_FILES))

JAVA_CLASSES = $(patsubst %,$(CLASSES)/%,$(JAVA_FILES:%.java=%.class))


all: FORCE
	$(MAKE) $(BUILD)/zimbra-native.jar
	$(MAKE) $(BUILD)/libzimbra-native.$(SHARED_EXT)

#
# Build jar file that wraps native code and it's shared library.
#
$(BUILD)/zimbra-native.jar: remove_classes_list $(JAVA_CLASSES) 
	mkdir -p $(CLASSES)
	@CLASSES_LIST="$(shell cat $(BUILD)/.classes.list)"; \
	    if [ -n "$$CLASSES_LIST" ]; then \
	        echo javac -source 1.4 -target 1.4 -d $(CLASSES) \
	            -sourcepath $(SRC)/java -classpath $(CLASSES) \
		    $(shell cat $(BUILD)/.classes.list); \
	        javac -source 1.4 -target 1.4 -d $(CLASSES) \
	            -sourcepath $(SRC)/java -classpath $(CLASSES) \
		    $(shell cat $(BUILD)/.classes.list) || exit 1; \
		$(RM) $@; \
	    fi
	jar c0vf $@ -C $(CLASSES) com;

$(CLASSES)/%.class: $(SRC)/java/%.java
	@echo $< >> $(BUILD)/.classes.list

remove_classes_list: FORCE
	$(RM) $(BUILD)/.classes.list
	mkdir -p $(BUILD)
	touch $(BUILD)/.classes.list

FORCE: ;

$(BUILD)/libzimbra-native.$(SHARED_EXT): $(BUILD)/IO.o $(BUILD)/Process.o $(BUILD)/ProcessorUsage.o $(BUILD)/ResourceUsage.o $(BUILD)/Util.o $(BUILD)/zjniutil.o
	gcc $(LIB_OPTS) $(SHARED) -o $@ $^

$(BUILD)/%.o: $(SRC)/native/%.c
	gcc $(MACDEF) $(JAVAINC) -I$(BUILD) -Wall -Wmissing-prototypes -c -o $@ $<

$(BUILD)/Process.o: $(SRC)/native/Process.c $(BUILD)/Process.h $(SRC)/native/zjniutil.h

$(BUILD)/ProcessorUsage.o: $(SRC)/native/ProcessorUsage.c $(BUILD)/ProcessorUsage.h $(SRC)/native/zjniutil.h

$(BUILD)/ResourceUsage.o: $(SRC)/native/ResourceUsage.c $(BUILD)/ResourceUsage.h $(SRC)/native/zjniutil.h

$(BUILD)/Util.o: $(SRC)/native/Util.c $(BUILD)/Util.h $(SRC)/native/zjniutil.h

$(BUILD)/zjniutil.o: $(SRC)/native/zjniutil.c $(SRC)/native/zjniutil.h

$(BUILD)/IO.o: $(SRC)/native/IO.c $(BUILD)/IO.h $(SRC)/native/zjniutil.h

$(BUILD)/IO.h: $(CLASSES)/com/zimbra/znative/IO.class
	mkdir -p $(@D)
	$(RM) $@
	javah -o $@ -classpath $(CLASSES) com.zimbra.znative.IO

$(BUILD)/Process.h: $(CLASSES)/com/zimbra/znative/Process.class
	mkdir -p $(@D)
	$(RM) $@
	javah -o $@ -classpath $(CLASSES) com.zimbra.znative.Process

$(BUILD)/ProcessorUsage.h: $(CLASSES)/com/zimbra/znative/ProcessorUsage.class
	mkdir -p $(@D)
	$(RM) $@
	javah -o $@ -classpath $(CLASSES) com.zimbra.znative.ProcessorUsage

$(BUILD)/ResourceUsage.h: $(CLASSES)/com/zimbra/znative/ResourceUsage.class
	mkdir -p $(@D)
	$(RM) $@
	javah -o $@ -classpath $(CLASSES) com.zimbra.znative.ResourceUsage

$(BUILD)/Util.h: $(CLASSES)/com/zimbra/znative/Util.class
	mkdir -p $(@D)
	$(RM) $@
	javah -o $@ -classpath $(CLASSES) com.zimbra.znative.Util

#
# Hack to copy to destination for use on incremental builds in a linux
# dev environment.
#
push: all
	p4 edit ../ZimbraServer/jars/zimbra-native.jar
	cp $(BUILD)/zimbra-native.jar ../ZimbraServer/jars
	p4 edit ../ZimbraServer/lib/libzimbra-native.$(PUSHED_EXT)
	cp $(BUILD)/libzimbra-native.$(SHARED_EXT) ../ZimbraServer/lib/libzimbra-native.$(PUSHED_EXT)

#
# Clean
#
clean:
	$(RM) -r $(BUILD)

.PHONY: all push clean
