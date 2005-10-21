#include <unistd.h>
#include <stdio.h>
#include <string.h>
#include <malloc.h>
#include <stdarg.h>
#include <stdlib.h>

/* We pass through only a limited set of environment variables.  By
 * comparison, sudo strips only LD_LIBRARY_PATH.  We are being overly
 * cautious - intentionally - JDK implementation is very sensitive to
 * JAVA_HOME in particular, but there are a lot of VM/JDK back door
 * environment variables.
 */
static const char *AllowedEnv[] = {
  "HOSTNAME",
  "DISPLAY",
  "HOME",
  "LANG",
  "PATH",
  "LOGNAME",
  "USER"
};

extern char **environ;

static int
IsAllowedEnv(const char *env)
{
  int alsize =  sizeof(AllowedEnv) / sizeof(char *);
  int i;
  for (i = 0; i < alsize; i++) {
    int compareLen = strlen(AllowedEnv[i]);
    if (strncmp(env, AllowedEnv[i], compareLen) == 0) {
      if (env[compareLen] == '=') {
        return 1;
      }
    }
  }
  return 0;
}

static void
StripEnv()
{
  int i, currentEnvSize = 0;
  char **newEnv, **iter;

  iter = environ;
  while (*iter != NULL) {
    currentEnvSize++;
    iter++;
  }

  /* +1 for terminator NULL */
  newEnv = (char **)calloc(currentEnvSize+1, sizeof(*newEnv));
  if (newEnv == NULL) {
    environ = NULL;
    return;
  }

  int newEnvNextLocation = 0;
  for (i = 0; i < currentEnvSize; i++) {
    if (IsAllowedEnv(environ[i])) {
      newEnv[newEnvNextLocation++] = environ[i];
    }
  }

  environ = newEnv;
}

/* Allow only known safe VM options.  This might to too conservative -
 * we could just disallow the potentially harmful ones such as
 * -classpath and -Xrun, ie anything that causes code to be loaded
 * from specified path.  For now maintain an allowed list, which is
 * the safer choice.
 */
static const char *AllowedJVMArgs[] = {
  "-XX:+AggressiveHeap",
  "-XX:+AllowUserSignalHandlers"
  "-XX:+Print",
  "-XX:NewRatio",
  "-Xcomp",
  "-Xconcgc",
  "-Xincgc",
  "-Xloggc",
  "-Xmn",
  "-Xms",
  "-Xmx",
  "-Xrs",
  "-Xrunhprof",
  "-Xss",
  "-client",
  "-d32",
  "-d64",
  "-da",
  "-dsa",
  "-ea",
  "-esa",
  "-fullversion",
  "-server",
  "-showversion",
  "-verbose",
  "-version",
};

static char **newArgv;
static int newArgCount = 0;
static int newArgCapacity = 0;

static int
IsAllowedJVMArg(const char *arg) 
{
  int alsize =  sizeof(AllowedJVMArgs) / sizeof(char *);
  int i;
  for (i = 0; i < alsize; i++) {
    int compareLen = strlen(AllowedJVMArgs[i]);
    if (strncmp(arg, AllowedJVMArgs[i], compareLen) == 0) {
        return 1;
    }
  }
  return 0;
}

static void
NewArgEnsureCapacity(int thisManyMore)
{
  if (newArgCapacity == 0) {
    newArgCapacity = 32;
    newArgv = (char **)malloc(sizeof(*newArgv) * newArgCapacity);
  }
  
  /* the -1 here is so we have room to stick a NULL to terminate newArgv */
  if ((newArgCapacity - 1) <= (newArgCount + thisManyMore)) {
    newArgCapacity = newArgCapacity * 2;
    newArgv = (char **)realloc(newArgv, newArgCapacity);
  }
}

static void 
AddArgInternal(char *arg)
{
  NewArgEnsureCapacity(1);
  newArgv[newArgCount++] = arg;
  newArgv[newArgCount] = NULL;
}

static void
AddArg(const char *fmt, ...)
#ifdef __GNUC__
  __attribute__((format(printf, 1, 2)))
#endif
  ;

static void
AddArg(const char *fmt, ...)
{
  char buf[1024];
  va_list ap;
  va_start(ap, fmt);
  vsnprintf(buf, sizeof(buf), fmt, ap);
  va_end(ap);
  AddArgInternal(strdup(buf));
}

static void
ShowNewArgs()
{
  char **e = newArgv;
  int i = 0;
  printf("---- New args ----\n");
  while (*e != NULL) {
    printf("[%2d] %s\n", i, *e);
    i++;
    e++;
  }
}

static void
ShowNewEnv()
{
  char **e = environ;
  printf("---- New env ----\n");
  while (*e != NULL) {
    printf("%s\n", *e);
    e++;
  }
}

int
main(int argc, char *argv[])
{
  int i;
  int debug_launcher = 0;

  if (getenv("ZIMBRA_TOMCAT_START_DEBUG") != NULL) {
    debug_launcher = 1;
  }

  StripEnv();

  /* TODO: warn if files are not owned by root */

  /* first argument must be name of binary */
  AddArg(JAVA_BINARY);

  for (i = 1; i < argc; i++) {
    if (IsAllowedJVMArg(argv[i])) {
      AddArg(argv[i]);
    } else {
      fprintf(stderr, "zmtomcatstart: option not allowed: %s\n", argv[i]);
      exit(1);
    }
  }

  /* REMIND: Do we need this?  Seems applicable only when -jar option
   * is present?
   * AddArg("-jre-no-restrict-search");
   */
  
  AddArg("-Dcatalina.base=%s", TOMCAT_HOME);
  AddArg("-Dcatalina.home=%s", TOMCAT_HOME);
  AddArg("-Djava.io.tmpdir=%s/temp", TOMCAT_HOME); 
  AddArg("-Djava.library.path=%s", ZIMBRA_LIB);
  AddArg("-Djava.endorsed.dirs=%s/common/endorsed", TOMCAT_HOME);
  AddArg("-classpath");
  AddArg("%s/bin/bootstrap.jar:%s/bin/commons-logging-api.jar", 
         TOMCAT_HOME, TOMCAT_HOME);
  AddArg("org.apache.catalina.startup.Bootstrap");
  AddArg("start");
  
  if (debug_launcher) {
    ShowNewEnv();
    ShowNewArgs();
  }

  execv(JAVA_BINARY, newArgv);
  return 0;
}
