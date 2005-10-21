SRC     = src

BUILD   = build

CLASSES = $(BUILD)/classes

JAVA_FILES = \
	com/zimbra/znative/IO.java \
	com/zimbra/znative/Process.java \
	com/zimbra/znative/Util.java \
	com/zimbra/znative/OperationFailedException.java \
	com/zimbra/znative/tests/HardLinkTest.java \
	com/zimbra/znative/tests/LinkCountTest.java \
	com/zimbra/znative/tests/ProcessTest.java

JAVA_SOURCES = $(patsubst %,$(SRC)/java/%,$(JAVA_FILES))

JAVA_CLASSES = $(patsubst %,$(CLASSES)/%,$(JAVA_FILES:%.java=%.class))


all: FORCE
	$(MAKE) $(BUILD)/zimbra-native.jar
	$(MAKE) $(BUILD)/libzimbra-native.so
	$(MAKE) $(BUILD)/zmtomcatstart

$(BUILD)/zmtomcatstart: $(SRC)/launcher/zmtomcatstart.c
	gcc -Wall -Wmissing-prototypes -o $@ $<

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

$(BUILD)/libzimbra-native.so: $(BUILD)/IO.o $(BUILD)/Process.o $(BUILD)/zjniutil.o
	gcc -shared -o $@ $^

$(BUILD)/%.o: $(SRC)/native/%.c
	gcc -I$(BUILD) -Wall -Wmissing-prototypes -c -o $@ $<

$(BUILD)/Process.o: $(SRC)/native/Process.c $(BUILD)/Process.h $(SRC)/native/zjniutil.h

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

#
# Hack to copy to destination
#
push:
	cp $(BUILD)/zimbra-native.jar ../ZimbraServer/jars
	cp $(BUILD)/libzimbra-native.so ../ZimbraServer/lib

#
# Clean
#
clean:
	$(RM) -r $(BUILD)

.PHONY: all push clean
