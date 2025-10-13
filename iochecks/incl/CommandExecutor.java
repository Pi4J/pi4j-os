package incl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

import static incl.CommandResult.failure;
import static incl.CommandResult.success;

/**
 * A utility class for executing system commands and capturing their output.
 *
 * <p>Please be careful when using this method. Since we decided to remove the
 * 'sh -c' part, this method no longer supports shell-specific features such as pipes,
 * redirection, or complex shell commands. It is intended to execute simple commands directly.</p>
 */
public class CommandExecutor {
    private static final int COMMAND_TIMEOUT_SECONDS = 30;

    /**
     * Executes a given command and captures its output and error streams.
     *
     * <p>Please be careful when using this method. Since we decided to remove the
     * 'sh -c' part, this method no longer supports pipes, redirection, or other
     * shell-specific features. Only simple, direct commands should be executed.</p>
     *
     * @param command The command to execute (must be simple, direct command without shell features).
     * @return A {@link CommandResult} containing:
     *         - {@code success}: true if the command executed successfully within the timeout.
     *         - {@code outputMessage}: the standard output from the command.
     *         - {@code errorMessage}: the error output or any exception message.
     */
    public static CommandResult execute(String command) {
        boolean finished = false;
        String outputMessage = "";
        String errorMessage = "";

        // Configure the process builder with the command (no shell involved).
        ProcessBuilder builder = new ProcessBuilder(command.split(" "));

        try {
            Process process = builder.start();

            outputMessage = readStream(process.getInputStream());
            errorMessage = readStream(process.getErrorStream());

            finished = process.waitFor(COMMAND_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                errorMessage = "Process timeout after " + COMMAND_TIMEOUT_SECONDS + " seconds.";
            }

        } catch (IOException ex) {
            errorMessage = "IOException while executing command: " + ex.getMessage();
            System.err.printf("IOException during command execution '%s': %s", command, ex.getMessage(), ex);
        } catch (InterruptedException ex) {
            errorMessage = "InterruptedException during command execution: " + ex.getMessage();
            Thread.currentThread().interrupt(); // Restore the interrupted status.
            System.err.printf("InterruptedException during command execution '%s': %s", command, ex.getMessage(), ex);
        }

        if (!finished || !errorMessage.isEmpty()) {
            System.err.printf("Failed to execute command '%s': %s", command, errorMessage);
            return failure(errorMessage);
        }

        return success(outputMessage);
    }

    /**
     * Reads the content of an InputStream and returns it as a string.
     *
     * @param inputStream The InputStream to read.
     * @return The content of the InputStream as a string.
     */
    private static String readStream(InputStream inputStream) {
        StringBuilder content = new StringBuilder();
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                content.append(line).append(System.lineSeparator());
            }
        } catch (IOException ex) {
            content.append("ERROR: ").append(ex.getMessage());
            System.err.printf("Error reading stream: %s", ex.getMessage(), ex);
        }
        return content.toString().trim(); // Trim trailing line separator.
    }
}
