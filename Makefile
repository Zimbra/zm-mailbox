SRC     = src
BUILD   = build
CLASSES = $(BUILD)/classes

all: $(BUILD)/zimbra-native.jar  $(BUILD)/libzimbra-native.so

#
# Build the jar file
#
$(BUILD)/zimbra-native.jar: $(CLASSES)/com/zimbra/znative/IO.class
	$(RM) $@
	jar c0vf $@ -C $(CLASSES) com

JAVA_SOURCES =  $(SRC)/java/com/zimbra/znative/IO.java \
		$(SRC)/java/com/zimbra/znative/tests/HardLinkTest.java \
		$(SRC)/java/com/zimbra/znative/tests/LinkCountTest.java

$(CLASSES)/com/zimbra/znative/IO.class: $(JAVA_SOURCES)
	mkdir -p $(CLASSES)
	javac -source 1.4 -target 1.4 -d $(CLASSES) $?

$(BUILD)/libzimbra-native.so: $(BUILD)/IO.o
	gcc -shared -o $@ $<

$(BUILD)/IO.o: $(SRC)/native/IO.c
	gcc -I$(BUILD) -Wall -Wmissing-prototypes -c -o $@ $<

$(SRC)/native/IO.c: $(BUILD)/IO.h

$(BUILD)/IO.h: $(CLASSES)/com/zimbra/znative/IO.class
	mkdir -p $(@D)
	$(RM) $@
	javah -o $@ -classpath $(CLASSES) com.zimbra.znative.IO

$(OUTDIR):
	mkdir -p $(OUTDIR)

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
