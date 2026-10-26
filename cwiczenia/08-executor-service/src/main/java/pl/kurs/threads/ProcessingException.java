package pl.kurs.threads;

/** Failure while processing one of the elements. The cause is the exception thrown by the function. */
public class ProcessingException extends RuntimeException {

    public ProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
