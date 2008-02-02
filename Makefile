SRC     = src

BUILD   = build

BUILD_ROOT := $(shell pwd)
BUILD_PLATFORM := $(shell sh $(BUILD_ROOT)/../ZimbraBuild/rpmconf/Build/get_plat_tag.sh)

SHARED := -shared
JAVAINC := -I/usr/local/java/include -I/usr/local/java/include/linux
SHARED_EXT := so
PUSHED_EXT := so.Linux.i386
CF := -fPIC -g

ifeq ($(BUILD_PLATFORM), MACOSX)
JAVAINC := -I/System/Library/Frameworks/JavaVM.framework/Headers
SHARED := -dynamiclib
MACDEF := -DDARWIN
SHARED_EXT := jnilib
LIB_OPTS := -install_name /opt/zimbra/lib/libzimbra-native.$(SHARED_EXT) -framework JavaVM
LIB_OPTS_SETUID := -install_name /opt/zimbra/lib/libsetuid.$(SHARED_EXT) -framework JavaVM
JAVA_BINARY = /usr/bin/java
PUSHED_EXT := jnilib.MacOSX.ppc
endif

ifeq (MACOSXx86,$(findstring MACOSXx86,$(BUILD_PLATFORM)))   
JAVAINC := -I/System/Library/Frameworks/JavaVM.framework/Headers
SHARED := -dynamiclib
MACDEF := -DDARWIN
SHARED_EXT := jnilib
LIB_OPTS := -install_name /opt/zimbra/lib/libzimbra-native.$(SHARED_EXT) -framework JavaVM
LIB_OPTS_SETUID := -install_name /opt/zimbra/lib/libsetuid.$(SHARED_EXT) -framework JavaVM
JAVA_BINARY = /usr/bin/java
PUSHED_EXT := jnilib.MacOSX.i386
endif

all: FORCE
	ant
	$(MAKE) $(BUILD)/libzimbra-native.$(SHARED_EXT)
	$(MAKE) $(BUILD)/libsetuid.$(SHARED_EXT)

FORCE: ;

$(BUILD)/libzimbra-native.$(SHARED_EXT): $(BUILD)/IO.o $(BUILD)/Process.o $(BUILD)/ProcessorUsage.o $(BUILD)/ResourceUsage.o $(BUILD)/Util.o $(BUILD)/zjniutil.o
	gcc $(CF) $(LIB_OPTS) $(SHARED) -o $@ $^

$(BUILD)/libsetuid.$(SHARED_EXT): $(BUILD)/org_mortbay_setuid_SetUID.o
	gcc $(CF) $(LIB_OPTS_SETUID) $(SHARED) -o $@ $^

$(BUILD)/%.o: $(SRC)/native/%.c
	gcc $(CF) $(MACDEF) $(JAVAINC) -I$(BUILD) -Wall -Wmissing-prototypes -c -o $@ $<

$(BUILD)/%.o: $(SRC)/jetty-setuid/%.c
	gcc $(CF) $(MACDEF) $(JAVAINC) -I$(BUILD) -Wall -Wmissing-prototypes -c -o $@ $<

$(BUILD)/Process.o: $(SRC)/native/Process.c $(BUILD)/Process.h $(SRC)/native/zjniutil.h

$(BUILD)/ProcessorUsage.o: $(SRC)/native/ProcessorUsage.c $(BUILD)/ProcessorUsage.h $(SRC)/native/zjniutil.h

$(BUILD)/ResourceUsage.o: $(SRC)/native/ResourceUsage.c $(BUILD)/ResourceUsage.h $(SRC)/native/zjniutil.h

$(BUILD)/Util.o: $(SRC)/native/Util.c $(BUILD)/Util.h $(SRC)/native/zjniutil.h

$(BUILD)/zjniutil.o: $(SRC)/native/zjniutil.c $(SRC)/native/zjniutil.h

$(BUILD)/IO.o: $(SRC)/native/IO.c $(BUILD)/IO.h $(SRC)/native/zjniutil.h

$(BUILD)/org_mortbay_setuid_SetUID.o: $(SRC)/jetty-setuid/org_mortbay_setuid_SetUID.c $(SRC)/jetty-setuid/org_mortbay_setuid_SetUID.h

#
# Hack to copy to destination for use on incremental builds in a linux
# dev environment.
#
push: all
	p4 edit ../ZimbraCommon/jars/zimbra-native.jar
	cp $(BUILD)/zimbra-native.jar ../ZimbraCommon/jars/zimbra-native.jar
	p4 edit ../ZimbraServer/lib/libzimbra-native.$(PUSHED_EXT)
	cp $(BUILD)/libzimbra-native.$(SHARED_EXT) ../ZimbraServer/lib/libzimbra-native.$(PUSHED_EXT)

#
# Clean
#
clean:
	$(RM) -r $(BUILD)

.PHONY: all push clean
