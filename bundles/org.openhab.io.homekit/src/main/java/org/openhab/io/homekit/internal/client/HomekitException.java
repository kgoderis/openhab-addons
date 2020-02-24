package org.openhab.io.homekit.internal.client;

public class HomekitException extends Exception {

    private static final long serialVersionUID = -9188483500104140469L;

    /**
     * Constructs an {@code HomekitException} with no detail message.
     * The cause is not initialized, and may subsequently be
     * initialized by a call to {@link #initCause(Throwable) initCause}.
     */
    protected HomekitException() {
    }

    /**
     * Constructs an {@code HomekitException} with the specified detail
     * message. The cause is not initialized, and may subsequently be
     * initialized by a call to {@link #initCause(Throwable) initCause}.
     *
     * @param message the detail message
     */
    protected HomekitException(String message) {
        super(message);
    }

    /**
     * Constructs an {@code HomekitException} with the specified detail
     * message and cause.
     *
     * @param message the detail message
     * @param cause the cause (which is saved for later retrieval by the
     *            {@link #getCause()} method)
     */
    public HomekitException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs an {@code HomekitException} with the specified cause.
     * The detail message is set to {@code (cause == null ? null :
     * cause.toString())} (which typically contains the class and
     * detail message of {@code cause}).
     *
     * @param cause the cause (which is saved for later retrieval by the
     *            {@link #getCause()} method)
     */
    public HomekitException(Throwable cause) {
        super(cause);
    }
}
