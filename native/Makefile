SRC     = src

BUILD   = build

BUILD_ROOT := $(shell pwd)

ifneq (${JAVA_HOME},)
JAVA_HOME_ = ${JAVA_HOME}
else
JAVA_HOME_ = $(subst /bin/javac,,$(realpath $(shell which javac)))
endif

SHARED := -shared
JAVAINC := -I$(JAVA_HOME_)/include -I$(JAVA_HOME_)/include/linux
SHARED_EXT := so
PUSHED_EXT := so.Linux.i386
CF := -fPIC -g
PROXY_INFO := DefaultProxyInfo
CC := gcc

all: FORCE
	$(MAKE) $(BUILD)/libzimbra-native.$(SHARED_EXT)
	$(MAKE) $(BUILD)/libsetuid.$(SHARED_EXT)

FORCE: ;

$(BUILD)/libzimbra-native.$(SHARED_EXT): $(BUILD)/IO.o $(BUILD)/Process.o $(BUILD)/ProcessorUsage.o $(BUILD)/ResourceUsage.o $(BUILD)/Util.o $(BUILD)/zjniutil.o $(BUILD)/$(PROXY_INFO).o
	$(CC) $(CF) $(LIB_OPTS) $(SHARED) -o $@ $^

$(BUILD)/libsetuid.$(SHARED_EXT): $(BUILD)/org_mortbay_setuid_SetUID.o
	$(CC) $(CF) $(LIB_OPTS_SETUID) $(SHARED) -o $@ $^

$(BUILD)/%.o: $(SRC)/native/%.c
	$(CC) $(CF) $(MACDEF) $(JAVAINC) -I$(BUILD) -Wall -Wmissing-prototypes -c -o $@ $<

$(BUILD)/%.o: $(SRC)/jetty-setuid/%.c
	$(CC) $(CF) $(MACDEF) $(JAVAINC) -I$(BUILD) -Wall -Wmissing-prototypes -c -o $@ $<

$(BUILD)/Process.o: $(SRC)/native/Process.c $(BUILD)/com_zimbra_znative_Process.h $(SRC)/native/zjniutil.h

$(BUILD)/ProcessorUsage.o: $(SRC)/native/ProcessorUsage.c $(BUILD)/com_zimbra_znative_ProcessorUsage.h $(SRC)/native/zjniutil.h

$(BUILD)/ResourceUsage.o: $(SRC)/native/ResourceUsage.c $(BUILD)/com_zimbra_znative_ResourceUsage.h $(SRC)/native/zjniutil.h

$(BUILD)/Util.o: $(SRC)/native/Util.c $(BUILD)/com_zimbra_znative_Util.h $(SRC)/native/zjniutil.h

$(BUILD)/zjniutil.o: $(SRC)/native/zjniutil.c $(SRC)/native/zjniutil.h

$(BUILD)/IO.o: $(SRC)/native/IO.c $(BUILD)/com_zimbra_znative_IO.h $(SRC)/native/zjniutil.h

$(BUILD)/org_mortbay_setuid_SetUID.o: $(SRC)/jetty-setuid/org_mortbay_setuid_SetUID.c $(SRC)/jetty-setuid/org_mortbay_setuid_SetUID.h

$(BUILD)/$(PROXY_INFO).o: $(SRC)/native/$(PROXY_INFO).c $(BUILD)/com_zimbra_znative_ProxyInfo.h $(SRC)/native/zjniutil.h

#
# Hack to copy to destination for use on incremental builds on linux / mac dev environments.
#
#push: all
#	p4 edit ../ZimbraCommon/jars/zimbra-native.jar
#	cp $(BUILD)/zimbra-native.jar ../ZimbraCommon/jars/zimbra-native.jar
#	p4 edit ../ZimbraServer/lib/libzimbra-native.$(PUSHED_EXT)
#	cp $(BUILD)/libzimbra-native.$(SHARED_EXT) ../ZimbraServer/lib/libzimbra-native.$(PUSHED_EXT)

#
# Clean
#
clean:
	$(RM) -r $(BUILD)

.PHONY: all push clean
