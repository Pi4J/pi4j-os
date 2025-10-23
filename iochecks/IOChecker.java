///usr/bin/env jbang "$0" "$@" ; exit $?
//REPOS mavencentral,mavensnapshot=https://central.sonatype.com/repository/maven-snapshots/

//DEPS com.pi4j:pi4j-drivers:0.0.1-SNAPSHOT

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.pi4j.Pi4J;
import com.pi4j.context.Context;
import com.pi4j.drivers.sensor.Sensor;
import com.pi4j.drivers.sensor.SensorDescriptor;
import com.pi4j.drivers.sensor.SensorDetector;

/**
 * Make sure JBang is installed on your system! More info on the
 * <a href="https://www.jbang.dev/">JBang</a>
 * and <a href="https://www.pi4j.com/prepare/install-java/#install-sdkman-maven-and-jbang">Pi4J</a> websites.
 * <br><br>
 * To execute all checks, run:
 * jbang https://github.com/pi4j/pi4j-os/blob/main/iochecks/IOChecker.java
 * <br><br>
 * Individual checks can be executed by passing the check name as argument
 * <ul>
 *     <li>gpio</li>
 *     <li>pwm</li>
 *     <li>i2c</li>
 *     <li>spi</li>
 *     <li>serial</li>
 * </ul>
 * For example:
 * <ul>
 *     <li>Only PWM: `jbang https://github.com/pi4j/pi4j-os/blob/main/iochecks/IOChecker.java pwm`</li>
 *     <li>I2C and SPI: `jbang https://github.com/pi4j/pi4j-os/blob/main/iochecks/IOChecker.java i2c spi`</li>
 * </ul>
 */
public class IOChecker {

    public static void main(String[] args) {
        List<String> argList = Arrays.asList(args);

        if (argList.isEmpty() || argList.contains("gpio")) {
            System.out.println(GPIOChecker.detect().logOutput());
        }

        if (argList.isEmpty() || argList.contains("pwm")) {
            System.out.println(PWMChecker.detect().logOutput());
        }

        if (argList.isEmpty() || argList.contains("i2c")) {
            System.out.println(I2CChecker.detect().logOutput());
        }

        if (argList.isEmpty() || argList.contains("spi")) {
            System.out.println(SPIChecker.detect().logOutput());
        }

        if (argList.isEmpty() || argList.contains("serial")) {
            System.out.println(SerialChecker.detect().logOutput());
        }
    }

    static class CommandExecutor {
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
                return CommandResult.failure(errorMessage);
            }

            return CommandResult.success(outputMessage);
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

    static class CommandResult {
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

    static class HexFormatter {
        private static final HexFormat HEX = HexFormat.of().withUpperCase();
        private static final HexFormat HEX_ARRAY = HexFormat.ofDelimiter(", ").withUpperCase().withPrefix("0x");

        /**
         * Formats a single byte value to hex format, e.g. 0xFF
         *
         * @param value byte value to format
         * @return hex formatted byte value
         */
        public static String format(byte value) {
            return "0x" + HEX.toHexDigits(value);
        }

        /**
         * Formats byte array to hex format, e.g. [0x01, 0xFF]
         *
         * @param value byte array to format
         * @return hex formatted byte array
         */
        public static String format(byte[] value) {
            return HEX_ARRAY.formatHex(value);
        }
    }

    static record CheckerResult(String title, List<Check> results) {
        public void addResult(Check result) {
            results.add(result);
        }

        public record Check(ResultStatus resultStatus, String command, String expected, String result) {
        }

        public enum ResultStatus {
            PASS, FAIL, TO_EVALUATE, UNDEFINED
        }

        public String logOutput() {
            var log = new StringBuilder();
            log.append("Result").append(results.size() > 1 ? "s" : "").append(" from ").append(title).append("\n");
            results.forEach(r -> {
                if (!r.command.isEmpty()) {
                    log.append("\n\t")
                        .append(r.command.trim());
                }
                log.append("\n\t\tStatus: ").append(r.resultStatus);
                log.append("\n\t\tExpected: ")
                    .append("\n\t\t\t").append(r.expected.isEmpty() ? "-" : r.expected.trim());
                log.append("\n\t\tResult: ")
                    .append(r.result.isEmpty() ? "\n\t\t\t-" : Arrays.stream(r.result.trim().split("\n"))
                        .map(l -> "\n\t\t\t" + l)
                        .collect(Collectors.joining()));
                log.append("\n");
            });
            return log.toString();
        }
    }

    static class BaseChecker {
        static CheckerResult.Check detectWithCommand(String command, String expectedOutput) {
            try {
                var output = CommandExecutor.execute(command);
                if (output.isSuccess() && !output.getOutputMessage().trim().isEmpty()) {
                    return new CheckerResult.Check(CheckerResult.ResultStatus.TO_EVALUATE, "Info returned by '" + command + "'",
                            expectedOutput, output.getOutputMessage());
                }
            } catch (Exception e) {
                System.out.printf("Error detecting SPI devices with command '{}': {}", command, e.getMessage());
            }
            return new CheckerResult.Check(CheckerResult.ResultStatus.TO_EVALUATE, "No info returned by '" + command + "'", expectedOutput, "");
        }

        protected static CheckerResult.Check detectConfigSetting(String setting, String interfaceName, String expectedOutput) {
            var result = new StringBuilder();
            String[] configPaths = {"/boot/config.txt", "/boot/firmware/config.txt"};
            boolean foundAny = false;

            for (String configPath : configPaths) {
                try {
                    Path path = Paths.get(configPath);
                    if (Files.exists(path)) {
                        List<String> lines = Files.readAllLines(path);

                        for (String line : lines) {
                            if (line.contains(setting) && !line.startsWith("#")) {
                                result.append("Found in ").append(configPath).append(": ").append(line.trim()).append("\n");
                                foundAny = true;
                            }
                        }
                    }
                } catch (Exception e) {
                    System.out.printf("Could not read config file %s: %s", configPath, e.getMessage());
                }
            }

            var command = "Configuration check for " + interfaceName.toUpperCase() + " in config.txt";
            if (!foundAny) {
                return new CheckerResult.Check(CheckerResult.ResultStatus.FAIL,
                        command, expectedOutput, result.toString());
            }

            return new CheckerResult.Check(CheckerResult.ResultStatus.PASS,
                    command, expectedOutput, result.toString());
        }

        protected static CheckerResult.Check detectInterfaceFromDeviceTree(String interfaceType, String description) {
            var result = new StringBuilder();
            List<String> foundDevices = new ArrayList<>();

            try {
                var socPath = findSocPath();
                if (socPath.isPresent()) {
                    try (var stream = Files.walk(socPath.get(), 2)) {
                        var interfacePaths = stream
                                .filter(Files::isDirectory)
                                .filter(path -> {
                                    String name = path.getFileName().toString();
                                    return name.contains(interfaceType);
                                })
                                .sorted()
                                .toList();

                        for (Path interfacePath : interfacePaths) {
                            String dirName = interfacePath.getFileName().toString();

                            // Check status
                            Path statusPath = interfacePath.resolve("status");
                            String status = "unknown";
                            if (Files.exists(statusPath)) {
                                try {
                                    status = Files.readString(statusPath).trim().replace("\0", "");
                                } catch (Exception e) {
                                    System.out.printf("Could not read status for %s: %s", interfacePath, e.getMessage());
                                }
                            }

                            // Only include devices with okay status
                            if ("okay".equals(status)) {
                                foundDevices.add(dirName);
                                result.append("✓ ").append(dirName).append(" (status: ").append(status).append(")\n");
                            } else {
                                result.append("✗ ").append(dirName).append(" (status: ").append(status).append(")\n");
                            }
                        }
                    }
                } else {
                    result.append("Device-tree path /proc/device-tree/soc not available\n");
                }
            } catch (Exception e) {
                System.out.printf("Error reading device-tree %s info: %s", interfaceType, e.getMessage());
                result.append("Error reading device-tree info: ").append(e.getMessage()).append("\n");
            }

            var command = "Search for " + interfaceType.toUpperCase() + " in /proc/device-tree";
            var expectedOutput = interfaceType + " device-tree entries with status=okay";

            if (foundDevices.isEmpty()) {
                return new CheckerResult.Check(CheckerResult.ResultStatus.FAIL,
                        command, expectedOutput, result.toString());
            }

            return new CheckerResult.Check(CheckerResult.ResultStatus.PASS,
                    command, expectedOutput, result.toString());
        }

        protected static Optional<Path> findSocPath() {
            // Look for any directory in /proc/device-tree that starts with "soc"
            Path dtBasePath = Paths.get("/proc/device-tree");

            try {
                if (Files.exists(dtBasePath)) {
                    try (var stream = Files.list(dtBasePath)) {
                        return stream
                                .filter(Files::isDirectory)
                                .filter(path -> path.getFileName().toString().startsWith("soc"))
                                .findFirst();
                    }
                }
            } catch (IOException e) {
                System.err.printf("Error reading device-tree info: %s", e.getMessage());
            }

            return Optional.empty();
        }
    }

    static class GPIOChecker extends BaseChecker {
        public static CheckerResult detect() {
            return new CheckerResult("GPIO Detection", List.of(
                    detectGpioDevicesWithTools()
            ));
        }

        private static CheckerResult.Check detectGpioDevicesWithTools() {
            var found = new ArrayList<String>();

            try {
                Path[] commonPaths = {
                        Path.of("/usr/bin/gpiodetect"),
                        Path.of("/usr/local/bin/gpiodetect"),
                        Path.of("/bin/gpiodetect")
                };

                for (Path path : commonPaths) {
                    if (Files.exists(path) && Files.isExecutable(path)) {
                        var output = CommandExecutor.execute(path.toString());
                        if (output.isSuccess() && !output.getOutputMessage().trim().isEmpty()) {
                            found.add(output.getOutputMessage());
                        }
                    }
                }
            } catch (Exception e) {
                System.out.printf("Error detecting GPIO devices with gpiodetect: %s", e.getMessage());
            }

            var command = "gpiodetect";
            var expectedOutput = "gpiochip0 [pinctrl-bcm2835] (54 lines) or similar";

            if (found.isEmpty()) {
                return new CheckerResult.Check(CheckerResult.ResultStatus.FAIL,
                        command, expectedOutput, "");
            } else {
                return new CheckerResult.Check(CheckerResult.ResultStatus.PASS,
                        command, expectedOutput,
                        found.stream()
                                .map(String::trim)
                                .distinct()
                                .sorted()
                                .collect(Collectors.joining("\n"))
                );
            }
        }
    }

    static class PWMChecker extends BaseChecker {
        public static CheckerResult detect() {
            return new CheckerResult("PWM Detection", List.of(
                detectConfigSetting("dtoverlay=pwm", "PWM", "dtoverlay=pwm (or dtoverlay=pwm-2chan for 2-channel PWM)"),
                detectPwmChips(),
                detectPwmFromPinctrl()
            ));
        }

        private static CheckerResult.Check detectPwmFromPinctrl() {
            var result = new StringBuilder();

            try {
                ProcessBuilder processBuilder = new ProcessBuilder("pinctrl");
                processBuilder.redirectErrorStream(true);
                Process process = processBuilder.start();

                try (var reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.contains("PWM")) {
                            result.append(line).append("\n");
                        }
                    }
                }

                process.waitFor();
            } catch (Exception e) {
                System.err.printf("Could not execute pinctrl command: %s", e.getMessage());
            }

            var command = "pinctrl | grep PWM";
            var expectedOutput = "GPIO line(s) with PWM function (e.g., GPIO18 = PWM0_CHAN2)";

            if (result.isEmpty()) {
                return new CheckerResult.Check(CheckerResult.ResultStatus.FAIL,
                    command, expectedOutput, "");
            } else {
                return new CheckerResult.Check(CheckerResult.ResultStatus.PASS,
                    command, expectedOutput, result.toString());
            }
        }

        private static CheckerResult.Check detectPwmChips() {
            var result = new StringBuilder();

            try {
                Path pwmPath = Paths.get("/sys/class/pwm");
                if (Files.exists(pwmPath)) {
                    try (var stream = Files.walk(pwmPath, 2)) {
                        stream
                            .map(f -> f.getFileName() + " ")
                            .filter(f -> f.startsWith("pwmchip"))
                            .sorted()
                            .forEach(result::append);
                    }
                }
            } catch (Exception e) {
                System.err.printf("Error detecting PWM chips: %s", e.getMessage());
            }

            var command = "ls -l /sys/class/pwm/";
            var expectedOutput = "One or more pwmchipX (X = number)";

            if (result.isEmpty()) {
                return new CheckerResult.Check(CheckerResult.ResultStatus.FAIL,
                    command, expectedOutput, "");
            } else {
                return new CheckerResult.Check(CheckerResult.ResultStatus.PASS,
                    command, expectedOutput, result.toString());
            }
        }
    }

    static class I2CChecker extends BaseChecker {

        public static CheckerResult detect() {
            var devices = detectI2CDevices();

            var result = new CheckerResult("I2C Detection", new ArrayList<>(List.of(
                detectConfigSetting("dtparam=i2c", "I2C", "dtparam=i2c_arm=on"),
                detectInterfaceFromDeviceTree("i2c", "I2C bus controller"),
                detectI2CDevicesWithCommand(devices)
            )));

            if (!devices.isEmpty()) {
                devices.forEach(d -> {
                    result.addResult(detectI2CUsedAddresses(d));
                    result.addResult(detectSensors(d.getBusNumber()));
                });
            }

            return result;
        }

        private static CheckerResult.Check detectI2CDevicesWithCommand(List<I2CDevice> devices) {
            String expectedOutput = "One or more devices, e.g. 'i2c-1' (I2C bus adapters detected by 'i2cdetect -l')";

            if (devices.isEmpty()) {
                return new CheckerResult.Check(CheckerResult.ResultStatus.FAIL,
                    "i2cdetect -l",
                    expectedOutput, "");
            } else {
                return new CheckerResult.Check(CheckerResult.ResultStatus.PASS,
                    "i2cdetect -l",
                    expectedOutput,
                    devices.stream()
                        .map(I2CDevice::output)
                        .collect(Collectors.joining("\n")));
            }
        }

        private static CheckerResult.Check detectI2CUsedAddresses(I2CDevice device) {
            var result = new StringBuilder();
            String expectedOutput = "One or more I2C used addresses detected on bus " + device.getBusNumber();
            List<Byte> addresses = new ArrayList<>();

            try {
                var output = CommandExecutor.execute("i2cdetect -y " + device.getBusNumber());
                if (output.isSuccess() && !output.getOutputMessage().trim().isEmpty()) {
                    addresses = parseI2CDeviceAddresses(output.getOutputMessage());

                    if (!addresses.isEmpty()) {
                        result.append("Found ").append(addresses.size())
                            .append(" used addres(ses) on bus ").append(device.getBusNumber())
                            .append(": ");
                        result.append(addresses.stream()
                            .map(HexFormatter::format).sorted()
                            .collect(Collectors.joining(", ")));
                    } else {
                        result.append("No used addresses found on bus ").append(device.getBusNumber()).append("\n");
                    }
                }
            } catch (Exception e) {
                System.err.printf("Error detecting I2C addresses on bus %s: %s", device.getBusNumber(), e.getMessage());
                return new CheckerResult.Check(CheckerResult.ResultStatus.FAIL,
                    "Error scanning I2C bus " + device.getBusNumber(),
                    expectedOutput, e.getMessage());
            }

            if (addresses.isEmpty()) {
                return new CheckerResult.Check(CheckerResult.ResultStatus.FAIL,
                    "i2cdetect -y " + device.getBusNumber(),
                    expectedOutput, result.toString());
            } else {
                return new CheckerResult.Check(CheckerResult.ResultStatus.PASS,
                    "i2cdetect -y " + device.getBusNumber(),
                    expectedOutput, result.toString());
            }
        }

        static List<Byte> parseI2CDeviceAddresses(String output) {
            List<Byte> addresses = new ArrayList<>();

            try {
                for (String line : output.split("\n")) {
                    line = line.trim();
                    if (line.startsWith("00: ") || line.startsWith("10: ") ||
                        line.startsWith("20: ") || line.startsWith("30: ") || line.startsWith("40: ") ||
                        line.startsWith("50: ") || line.startsWith("60: ") || line.startsWith("70: ")) {

                        // Skip the row number at the beginning
                        String[] parts = line.substring(3).split("\\s+");

                        // Start from index 1 to skip the row number (00:, 10:, etc.)
                        for (int i = 0; i < parts.length; i++) {
                            String part = parts[i].trim();
                            // Valid addresses are two hex digits (not -- or UU)
                            if (part.matches("[0-9a-fA-F]{2}")) {
                                addresses.add((byte) Integer.parseInt(part.toLowerCase(), 16));
                            }
                        }
                    }
                }
            } catch (Exception e) {
                System.err.printf("Error parsing I2C addresses: %s", e.getMessage());
            }

            return addresses;
        }

        private static List<I2CDevice> detectI2CDevices() {
            try {
                var output = CommandExecutor.execute("i2cdetect -l");
                if (output.isSuccess() && !output.getOutputMessage().trim().isEmpty()) {
                    return parseI2CDetectOutput(output.getOutputMessage());
                }
            } catch (Exception e) {
                System.err.printf("Error detecting I2C devices with i2cdetect command: %s", e.getMessage());
            }

            return new ArrayList<>();
        }

        private static List<I2CDevice> parseI2CDetectOutput(String output) {
            List<I2CDevice> devices = new ArrayList<>();

            for (String line : output.split("\n")) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }

                // Split by whitespace (multiple spaces/tabs)
                String[] parts = line.split("\\s+");

                if (parts.length >= 4) {
                    String bus = parts[0];          // e.g., "i2c-1"
                    String type = parts[1];         // e.g., "i2c"
                    String name = parts[2];         // e.g., "Synopsys" or "107d508200.i2c"

                    // Join remaining parts as description
                    String description = String.join(" ", Arrays.copyOfRange(parts, 3, parts.length));

                    devices.add(new I2CDevice(bus, type, name, description));
                }
            }

            return devices;
        }

        private static CheckerResult.Check detectSensors(int bus) {
            Context pi4j = null;
            
            try {
                pi4j = Pi4J.newAutoContext();
                var sensors = SensorDetector.detectI2cSensors(pi4j, bus);

                if (sensors.isEmpty()) {
                    return new CheckerResult.Check(CheckerResult.ResultStatus.TO_EVALUATE,
                            "Pi4J Drivers SensorDetector",
                            "I2C sensors on bus " + bus, "No sensors could be detected, maybe there are none, or they could not be recognized by the Pi4J Drivers library.");
                }

                return new CheckerResult.Check(CheckerResult.ResultStatus.PASS,
                        "Pi4J Drivers SensorDetector",
                        "One or more I2C sensors on bus " + bus + " were recognized by the Pi4J Drivers library",
                        sensors.stream()
                                .map(s -> s.getClass().getSimpleName() + "\n" + getSensorValues(s))
                                .collect(Collectors.joining("\n")));
            } catch (Exception e) {
                return new CheckerResult.Check(CheckerResult.ResultStatus.FAIL,
                        "Pi4J Drivers SensorDetector",
                        "I2C sensors on bus " + bus, e.getMessage());
            } finally {
                if (pi4j != null) {
                    pi4j.shutdown();
                }
            }
        }

        private static String getSensorValues(Sensor sensor) {
            String rt = "- Addresses: " + sensor.getDescriptor().getI2cAddresses().stream()
                    .map(a -> "0x" + String.format("%02X", a))
                    .collect(Collectors.joining(", "))
                + "\n";

            List<SensorDescriptor.Value> valueDescriptors = sensor.getDescriptor().getValues();

            float[] values = new float[valueDescriptors.size()];
            sensor.readMeasurement(values);

            for (SensorDescriptor.Value valueDescriptor : valueDescriptors) {
                rt += " - " + valueDescriptor.getKind() + ": " + values[valueDescriptor.getIndex()] + "\n";
            }

            return rt;
        }

        private record I2CDevice(String bus, String type, String name, String description) {
            public int getBusNumber() {
                try {
                    return Integer.parseInt(bus.replace("i2c-", ""));
                } catch (NumberFormatException e) {
                    return -1; // or throw exception
                }
            }

            public String output() {
                return getBusNumber() + ": " + name + ", " + description;
            }
        }
    }

    static class SPIChecker extends BaseChecker {

        public static CheckerResult detect() {
            return new CheckerResult("SPI Detection", List.of(
                detectConfigSetting("dtparam=spi", "SPI", "dtparam=spi=on"),
                detectInterfaceFromDeviceTree("spi", "SPI bus controller"),
                detectSpi()
            ));
        }

        private static CheckerResult.Check detectSpi() {
            var result = new StringBuilder();

            try {
                Path pwmPath = Paths.get("/sys/bus/spi/devices");
                if (Files.exists(pwmPath)) {
                    try (var stream = Files.walk(pwmPath, 2)) {
                        stream
                            .map(f -> f.getFileName() + " ")
                            .filter(f -> f.startsWith("spi"))
                            .sorted()
                            .forEach(result::append);
                    }
                }
            } catch (Exception e) {
                System.err.printf("Error detecting SPI: %s", e.getMessage());
            }

            var command = "ls -l /sys/bus/spi/devices";
            var expectedOutput = "One or more spiX.Y (X and Y = numbers)";

            if (result.isEmpty()) {
                return new CheckerResult.Check(CheckerResult.ResultStatus.FAIL,
                    command, expectedOutput, "");
            } else {
                return new CheckerResult.Check(CheckerResult.ResultStatus.PASS,
                    command, expectedOutput, result.toString());
            }
        }
    }

    static class SerialChecker extends BaseChecker {

        public static CheckerResult detect() {
            return new CheckerResult("Serial Detection", List.of(
                detectConfigSetting("dtparam=uart", "UART", "dtparam=uart0=on"),
                detectInterfaceFromDeviceTree("uart", "UART serial controller"),
                detectSerialPortAvailability()
            ));
        }

        private static CheckerResult.Check detectSerialPortAvailability() {
            var result = new StringBuilder();
            String[] serialDevices = {"/dev/ttyS0", "/dev/ttyAMA0", "/dev/ttyUSB0", "/dev/ttyACM0"};
            var found = false;

            for (String devicePath : serialDevices) {
                try {
                    Path device = Paths.get(devicePath);
                    if (Files.exists(device)) {
                        // Check if device is readable/writable
                        boolean readable = Files.isReadable(device);
                        boolean writable = Files.isWritable(device);

                        result.append(devicePath).append(" exists");
                        if (readable || writable) {
                            found = true;
                            result.append(" (");
                            if (readable) result.append("readable");
                            if (readable && writable) result.append(", ");
                            if (writable) result.append("writable");
                            result.append(")");
                        } else {
                            result.append(" (no permissions)");
                        }
                        result.append("\n");
                    } else {
                        result.append(devicePath).append(" not available\n");
                    }
                } catch (Exception e) {
                    result.append(devicePath).append(" - error checking: ").append(e.getMessage()).append("\n");
                }
            }

            var command = "Checking serial device availability";
            var expectedOutput = "/dev/ttyS0 exists (readable, writable)";

            if (!found) {
                return new CheckerResult.Check(CheckerResult.ResultStatus.FAIL,
                    command, expectedOutput, result.toString());
            }

            return new CheckerResult.Check(CheckerResult.ResultStatus.PASS,
                command, expectedOutput, result.toString());
        }
    }
}