package com.zimbra.cs.util;

import com.zimbra.common.service.ServiceException;

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
                    try {
                        onFailureAction.run();
                    } catch (Exception e2) {
                        String errorMsg = String.format("error invoking OnFailureAction %s", command.getClass().getName());
                        throw ServiceException.FAILURE(errorMsg, e2);
                    }
                    try {
                        return command.execute();
                    } catch (Exception e2) {
                        String errorMsg = String.format("error running command %s after invoking OnFailureAction", command.getClass().getName());
                        throw ServiceException.FAILURE(errorMsg, e2);
                    }
                } else {
                    String errorMsg = String.format("unexpected error running command %s", command.getClass().getName());
                    throw ServiceException.FAILURE(errorMsg, e);
                }
            }
        }

        public static interface OnFailureAction {
            public void run() throws Exception;
        }

        public static interface Command<T> {
            public T execute() throws Exception;
        }

        public static interface ExceptionHandler {
            public boolean exceptionMatches(Exception e);
        }
    }
}
