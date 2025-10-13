///usr/bin/env jbang "$0" "$@" ; exit $?

//SOURCES incl/*

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import incl.BaseChecker;
import incl.CheckerResult;

import static incl.CommandExecutor.execute;

/**
 * jbang https://github.com/pi4j/pi4j-os/blob/main/iochecks/GPIOChecker.java
 */
public class GPIOChecker extends BaseChecker {

    public static void main(String[] args) {
        System.out.println(detect().logOutput());
    }

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
                    var output = execute(path.toString());
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