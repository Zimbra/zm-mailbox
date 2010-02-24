SRC     = src

BUILD   = build

BUILD_ROOT := $(shell pwd)
BUILD_PLATFORM := $(shell sh $(BUILD_ROOT)/../ZimbraBuild/rpmconf/Build/get_plat_tag.sh)

SHARED := -shared
JAVAINC := -I/usr/local/java/include -I/usr/local/java/include/linux
SHARED_EXT := so
PUSHED_EXT := so.Linux.i386
CF := -fPIC -g
PROXY_INFO := DefaultProxyInfo

ifeq ($(BUILD_PLATFORM), MACOSX)
# Build system is OS/X 10.4 PPC
JAVAINC := -I/System/Library/Frameworks/JavaVM.framework/Headers
SHARED := -dynamiclib
MACDEF := -DDARWIN
SHARED_EXT := jnilib
CF := -fPIC -g -O2 -force_cpusubtype_ALL -mmacosx-version-min=10.4 -arch i386 -arch ppc -arch ppc64 -arch x86_64
LIB_OPTS := -install_name /opt/zimbra/lib/libzimbra-native.$(SHARED_EXT) -framework JavaVM -framework CoreServices
LIB_OPTS_SETUID := -install_name /opt/zimbra/lib/libsetuid.$(SHARED_EXT) -framework JavaVM
JAVA_BINARY = /usr/bin/java
PUSHED_EXT := jnilib.MacOSX
PROXY_INFO := DefaultProxyInfo
endif

ifeq (MACOSXx86,$(findstring MACOSXx86,$(BUILD_PLATFORM)))   
# Build system is OS/X 10.5 and above, x86
JAVAINC := -I/System/Library/Frameworks/JavaVM.framework/Headers
SHARED := -dynamiclib
MACDEF := -DDARWIN
CF := -fPIC -g -O2 -force_cpusubtype_ALL -mmacosx-version-min=10.5 -arch i386 -arch ppc -arch x86_64
SHARED_EXT := jnilib
LIB_OPTS := -install_name /opt/zimbra/lib/libzimbra-native.$(SHARED_EXT) -framework JavaVM -framework CoreServices
LIB_OPTS_SETUID := -install_name /opt/zimbra/lib/libsetuid.$(SHARED_EXT) -framework JavaVM
JAVA_BINARY = /usr/bin/java
PUSHED_EXT := jnilib.MacOSX
PROXY_INFO := MacProxyInfo
endif

ifeq (MACOSXx86,$(BUILD_PLATFORM))
# Build system is OS/X 10.4 x86
CF :=-fPIC -g -O2 -force_cpusubtype_ALL -mmacosx-version-min=10.4 -arch i386 -arch ppc
PROXY_INFO := DefaultProxyInfo
endif

all: FORCE
	ant
	$(MAKE) $(BUILD)/libzimbra-native.$(SHARED_EXT)
	$(MAKE) $(BUILD)/libsetuid.$(SHARED_EXT)

FORCE: ;

$(BUILD)/libzimbra-native.$(SHARED_EXT): $(BUILD)/IO.o $(BUILD)/Process.o $(BUILD)/ProcessorUsage.o $(BUILD)/ResourceUsage.o $(BUILD)/Util.o $(BUILD)/zjniutil.o $(BUILD)/$(PROXY_INFO).o
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

$(BUILD)/$(PROXY_INFO).o: $(SRC)/native/$(PROXY_INFO).c $(BUILD)/ProxyInfo.h $(SRC)/native/zjniutil.h

#
# Hack to copy to destination for use on incremental builds on linux / mac dev environments.
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
