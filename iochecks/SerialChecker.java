///usr/bin/env jbang "$0" "$@" ; exit $?

//SOURCES incl/*

import incl.BaseChecker;
import incl.CheckerResult;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class SerialChecker extends BaseChecker {

    public static void main(String[] args) {
        System.out.println(detect().logOutput());
    }

    public static CheckerResult detect() {
        return new CheckerResult("Serial Detection", List.of(
            detectConfigSetting("enable_uart", "UART", "enable_uart=1"),
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