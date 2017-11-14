package com.zimbra.cs.util;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;

public class RetryUtil {

    public static class RequestWithRetry<T> {

        private Command<T> command;
        private ExceptionHandler exceptionHandler;
        private OnFailureAction onFailureAction;


        public RequestWithRetry(Command<T> command, ExceptionHandler exceptionHandler, OnFailureAction onFailure) {
            this.exceptionHandler = exceptionHandler;
            this.command = command;
            this.onFailureAction = onFailure;
        }

        public T execute() throws ServiceException {
            try {
                return command.execute();
            } catch (Exception e) {
                if (exceptionHandler.exceptionMatches(e)) {
                    if (onFailureAction.runOnFailure()) {
                        try {
                            return command.execute();
                        } catch (Exception e2) {
                            String errorMsg = String.format("error running command %s after invoking OnFailureAction", command.getClass().getName());
                            throw ServiceException.FAILURE(errorMsg, e2);
                        }
                    }
                } else {
                    String errorMsg = String.format("unexpected error running command %s", command.getClass().getName());
                    throw ServiceException.FAILURE(errorMsg, e);
                }
            }
            return null;
        }

        public static abstract class OnFailureAction {

            public boolean runOnFailure() {
                try {
                    run();
                    return true;
                } catch (ServiceException e) {
                    ZimbraLog.misc.error("error invoking runOnFailure method of %s", this.getClass().getName());
                    return false;
                }
            }

            protected abstract void run() throws ServiceException;
        }

        public static interface Command<T> {
            public T execute() throws Exception;
        }

        public static interface ExceptionHandler {
            public boolean exceptionMatches(Exception e);
        }
    }
}
