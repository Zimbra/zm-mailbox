/* -*- Mode: c; c-basic-offset: 4 -*- */
/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s):
 * 
 * ***** END LICENSE BLOCK *****
 */

#include <sys/types.h>
#include <sys/stat.h>
#include <sys/ioctl.h>
#include <errno.h>
#include <fcntl.h>
#include <signal.h>
#include <stdarg.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#ifdef DARWIN
#include <malloc/malloc.h>
#else
#include <malloc.h>
#endif

/* Call it zm so there are no conflicts. :-) */
typedef enum { ZM_FALSE, ZM_TRUE } zmboolean;

/* Set by main() */
static const char *progname;
#define PROGNAME "zmtomcatstart";

/* True if environment variable ZIMBRA_TOMCAT_START_DEBUG is set */
static zmboolean debug_launcher = ZM_FALSE;

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
  "-XX:+AllowUserSignalHandlers",
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

static void
CheckForRunningInstance()
{
  FILE *fp = NULL;
  struct stat sb;
  pid_t pid;

  if (debug_launcher) {
      printf("---- GetRunningInstance ----\n");
      printf("PIDFILE: %s\n", TOMCAT_PIDFILE);
  }

  if (stat(TOMCAT_PIDFILE, &sb) < 0) {
    if (debug_launcher) {
      printf("stat failed: %s\n", strerror(errno));
    }

    if (errno == ENOENT) {
      goto NO_INSTANCE;
    }
    
    /* If there are any permission problems or some such, crap out now. */
    fprintf(stderr, "%s: error: stat failed on pid file path: %s: %s\n",
            progname, TOMCAT_PIDFILE, strerror(errno));
    exit(1);
  }

  if (!S_ISREG(sb.st_mode)) {
    fprintf(stderr, "%s: error: pid file is not a regular file: %s: %s\n",
            progname, TOMCAT_PIDFILE, strerror(errno));
    exit(1);
  }

  fp = fopen(TOMCAT_PIDFILE, "r");
  if (fp == NULL) {
    fprintf(stderr, "%s: warning: fopen failed for pid file: %s: %s\n",
            progname, TOMCAT_PIDFILE, strerror(errno));
    goto NO_INSTANCE;
  }

  if (fscanf(fp, "%d", &pid) < 0) {
    if (debug_launcher) {
      printf("did not find a number in pid file\n");
    }
    goto NO_INSTANCE;
  }

  if (kill(pid, 0) < 0) {
    fprintf(stderr, "%s: info: stale pid %d in pid file: %s\n", 
            progname, pid, strerror(errno));
    goto NO_INSTANCE;
  }

  /* PID found, and there is an associated process. */
  if (fp != NULL) {
    fclose(fp);
  }
  
  fprintf(stderr, "%s: error: another instance with pid %d is running\n", 
          progname, pid);
  exit(1);
  return;

 NO_INSTANCE:
  if (fp != NULL) {
    fclose(fp);
  }
  if (debug_launcher) {
    printf("assuming no process running\n");
  }
  return;
}

static int pidfd = -1;

/* Create the pid file in parent process before forking so that 
 * if we can't create the file for some reason we don't fork. */

static void
CreatePidFile()
{
  /* Reset the mask so the pid file has the exact permissions we say
   * it should have. */
  umask(0);
  
  pidfd = creat(TOMCAT_PIDFILE, S_IRUSR | S_IWUSR | S_IRGRP | S_IROTH);
  if (pidfd < 0) {
    fprintf(stderr, "%s: error: could not create pid file: %s: %s\n",
            progname, TOMCAT_PIDFILE, strerror(errno));
    exit(1);
  }
}

static void
RecordPidFromParent(pid_t pid)
{
  char buf[64];
  int len, wrote;

  len = snprintf(buf, sizeof(buf), "%d\n", pid);
  wrote = write(pidfd, buf, len);
  if (wrote != len) {
    fprintf(stderr, "%s: error: wrote only %d of %d to pid file: %s: %s\n",
            progname, wrote, len, TOMCAT_PIDFILE, strerror(errno));
    exit(1);
  }

  if (debug_launcher) {
    printf("---- New child created ----\n");
    printf("PIDFILE=%s\n", TOMCAT_PIDFILE);
    printf("PID=%s\n", buf);
  }

  close(pidfd);
}

static void
StartTomcat()
{
  pid_t pid;

  CreatePidFile();

  /* Thank you, Mr. Stevens. */

  if ((pid = fork()) != 0) {
    RecordPidFromParent(pid);
    exit(0); /* parent process */
  }

  /* Now, we are in the child process. */

  close(pidfd);

  fclose(stdin);
  
  umask(0);
  
  chdir(TOMCAT_HOME);
  
#ifdef DARWIN
  {
    int tfd;
    setpgrp(0, getpid());
    if ((tfd = open("/dev/tty", O_RDWR)) >= 0) {
      ioctl(tfd, TIOCNOTTY, (char *)0); /* lose control tty */
      close(tfd);
    }
  }
#else
  setpgrp();
#endif
   
  execv(JAVA_BINARY, newArgv);
}

static void
CheckJavaBinaryExists()
{
  struct stat sb;
  if (stat(JAVA_BINARY, &sb) < 0) {
    fprintf(stderr, "%s: error: stat failed for java binary: %s: %s\n",
            progname, JAVA_BINARY, strerror(errno));
    exit(1);
  }

  if (sb.st_uid == getuid()) {
    if (!(sb.st_mode & S_IXUSR)) {
      fprintf(stderr, "%s: error: java binary is not executable by user: %s\n",
              progname, JAVA_BINARY);
      exit(1);
    }
  } else {
    if (!(sb.st_mode & S_IXOTH)) {
      fprintf(stderr, "%s: error: java binary is not executable by other: %s\n",
              progname, JAVA_BINARY);
      exit(1);
    }
  }
}

int
main(int argc, char *argv[])
{
  int i;

  progname = (argc > 0 && argv[0] != NULL) ? argv[0] : PROGNAME;
  if (strrchr(progname, '/') > 0) {
    progname = strrchr(progname, '/') + 1;
    if (*progname == '\0') {
      progname = PROGNAME;
    }
  }

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
      fprintf(stderr, "%s: error: JVM option: %s: not allowed\n", progname, argv[i]);
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

  CheckJavaBinaryExists();

  CheckForRunningInstance();

  StartTomcat();

  return 0;
}
