///usr/bin/env jbang "$0" "$@" ; exit $?

//SOURCES incl/*

import incl.BaseChecker;
import incl.CheckerResult;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class SPIChecker extends BaseChecker {

    public static void main(String[] args) {
        System.out.println(detect().logOutput());
    }

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