///usr/bin/env jbang "$0" "$@" ; exit $?

//SOURCES incl/*

import incl.BaseChecker;
import incl.CheckerResult;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class PWMChecker extends BaseChecker {

    public static void main(String[] args) {
        System.out.println(detect().logOutput());
    }

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