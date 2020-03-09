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
                RetryExceptionAction action = exceptionHandler.handleException(e);
                if (action.doRetry()) {
                    try {
                        onFailureAction.run(e);
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
                    throw ServiceException.FAILURE(errorMsg, action.getExceptionToThrow());
                }
            }
        }

        public static interface OnFailureAction {
            public void run(Exception e) throws Exception;
        }

        public static interface Command<T> {
            public T execute() throws Exception;
        }

        public static interface ExceptionHandler {
            public RetryExceptionAction handleException(Exception e);
        }

        public static class RetryExceptionAction {

            private boolean shouldRetry;
            private Throwable toThrow;

            public static final RetryExceptionAction SHOULD_RETRY = new RetryExceptionAction(true, null);

            private RetryExceptionAction(boolean shouldRetry, Throwable toThrow) {
                this.shouldRetry = shouldRetry;
                this.toThrow = toThrow;
            }

            public static RetryExceptionAction reThrow(Exception toThrow) {
                return new RetryExceptionAction(false, toThrow);
            }

            public boolean doRetry() {
                return shouldRetry;
            }

            public Throwable getExceptionToThrow() {
                return toThrow;
            }
        }
    }
}
