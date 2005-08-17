SRC     = src
BUILD   = build
CLASSES = $(BUILD)/classes

all: $(BUILD)/liquidos.jar  $(BUILD)/libliquidos.so

#
# Build the jar file
#
$(BUILD)/liquidos.jar: $(CLASSES)/com/liquidsys/os/IO.class
	$(RM) $@
	jar c0vf $@ -C $(CLASSES) com

JAVA_SOURCES =  $(SRC)/java/com/liquidsys/os/IO.java \
		$(SRC)/java/com/liquidsys/os/tests/HardLinkTest.java \
		$(SRC)/java/com/liquidsys/os/tests/LinkCountTest.java

$(CLASSES)/com/liquidsys/os/IO.class: $(JAVA_SOURCES)
	mkdir -p $(CLASSES)
	javac -d $(CLASSES) $?

$(BUILD)/libliquidos.so: $(BUILD)/IO.o
	gcc -shared -o $@ $<

$(BUILD)/IO.o: $(SRC)/native/IO.c
	gcc -I$(BUILD) -Wall -Wmissing-prototypes -c -o $@ $<

$(SRC)/native/IO.c: $(BUILD)/IO.h

$(BUILD)/IO.h: $(CLASSES)/com/liquidsys/os/IO.class
	mkdir -p $(@D)
	$(RM) $@
	javah -o $@ -classpath $(CLASSES) com.zimbra.native.IO

$(OUTDIR):
	mkdir -p $(OUTDIR)

#
# Hack to copy to destination
#
push:
	cp $(BUILD)/liquidos.jar ../LiquidArchive/jars
	cp $(BUILD)/libliquidos.so ../LiquidArchive/lib

#
# Clean
#
clean:
	$(RM) -r $(BUILD)

.PHONY: all push clean
