package incl;

import java.util.Objects;
import java.util.Optional;

/**
 * Represents the result of executing a command.
 */
public class CommandResult {
    private final boolean success;
    private final String outputMessage;
    private final String errorMessage;

    /**
     * Constructor to create a new CommandResult.
     *
     * @param success       Whether the command execution was successful.
     * @param outputMessage The standard output of the command.
     * @param errorMessage  The error message if the command failed.
     */
    private CommandResult(boolean success, String outputMessage, String errorMessage) {
        this.success = success;
        this.outputMessage = Optional.ofNullable(outputMessage).orElse("");
        this.errorMessage = Optional.ofNullable(errorMessage).orElse("");
    }

    /**
     * Static method to create a successful CommandResult.
     *
     * @param outputMessage The standard output of the successful command.
     * @return A CommandResult representing a successful command execution.
     */
    public static CommandResult success(String outputMessage) {
        return new CommandResult(true, outputMessage, null);
    }

    /**
     * Static method to create a failed CommandResult.
     *
     * @param errorMessage The error message from the failed command execution.
     * @return A CommandResult representing a failed command execution.
     */
    public static CommandResult failure(String errorMessage) {
        return new CommandResult(false, null, errorMessage);
    }

    /**
     * Returns whether the command execution was successful.
     *
     * @return {@code true} if successful, {@code false} otherwise.
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Returns the standard output message from the command.
     *
     * @return The standard output message.
     */
    public String getOutputMessage() {
        return outputMessage;
    }

    /**
     * Returns the error message from the command execution.
     *
     * @return The error message.
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Provides a string representation of the CommandResult object.
     * Useful for logging and debugging.
     *
     * @return A string describing the CommandResult.
     */
    @Override
    public String toString() {
        return String.format("CommandResult{success=%b, outputMessage='%s', errorMessage='%s'}",
                success, outputMessage, errorMessage);
    }

    /**
     * Compares this CommandResult to another object for equality.
     *
     * @param o The object to compare to.
     * @return {@code true} if the objects are equal, {@code false} otherwise.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CommandResult that = (CommandResult) o;
        return success == that.success &&
                Objects.equals(outputMessage, that.outputMessage) &&
                Objects.equals(errorMessage, that.errorMessage);
    }

    /**
     * Returns a hash code value for this CommandResult.
     *
     * @return The hash code for this object.
     */
    @Override
    public int hashCode() {
        return Objects.hash(success, outputMessage, errorMessage);
    }
}
