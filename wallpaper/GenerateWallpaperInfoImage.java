///usr/bin/env jbang "$0" "$@" ; exit $?

//DEPS com.pi4j:pi4j-core:3.0.3

import com.pi4j.Pi4J;
import com.sun.management.OperatingSystemMXBean;
import java.lang.management.ManagementFactory;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.Enumeration;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * Code to create a wallpaper image with system information.
 *
 * The input image, output image, and width and height for the output image must be provided as args.
 * The Pi4J dependency is used to detect the type of Raspberry Pi board.
 * You can execute this script with JBang:
 * java <input-image-path> <output-image-path> <output-image-width> <output-image-height>
 *
 * Example usage to generate a wallpaper for a 1280x800 screen:
 * cd wallpaper
 * jbang GenerateWallpaperInfoImage.java
 */
public class GenerateWallpaperInfoImage {

    public static void main(String[] args) {
        var width = 500;
        var height = 300;

        var outputFile = generateSystemInfoImage("pi4j-logo.png", "wallpaper.png", width, height);

        if (outputFile == null) {
            System.err.println("No output image could be created...");
        } else {
            System.out.println("Image generated successfully");

            // Update the wallpaper on the screen
            try {
                int exitCode = executeWithExitCode(Arrays.asList("pcmanfm",
                        "--set-wallpaper", outputFile.getCanonicalPath(),
                        "--wallpaper-mode", "center"));

                if (exitCode == 0) {
                    System.out.println("Wallpaper is updated");
                } else {
                    System.err.println("Failed to update wallpaper (exit code: " + exitCode + ")");
                }
            } catch (IOException e) {
                System.err.println("Failed to update wallpaper: " + e.getMessage());
            }
        }
    }

    public static File generateSystemInfoImage(String inputImagePath, String outputImagePath, int width, int height) {
        try {
            // Info box
            int logoSize = 170;
            int padding = 10;
            int lineHeight = 14;
            int infoWidth = 300;
            int networkLineHeight = 24;

            List<String> infoItems = new ArrayList<>();
            infoItems.addAll(getSystemInfo());
            infoItems.addAll(getNetworkInfo());

            // Read the input image
            BufferedImage originalImage = ImageIO.read(new File(inputImagePath));

            // Create a copy of the image
            BufferedImage newImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = newImage.createGraphics();

            // Add semi-transparent background
            g2d.setColor(new Color(100, 150, 150, 180));
            g2d.fillRect(0, 0, width, height);

            // Draw the original image
            int imageWidth = width - infoWidth - (2 * padding);
            g2d.drawImage(originalImage, padding, padding, logoSize, logoSize, null);

            // Configure text rendering
            g2d.setRenderingHint(
                    RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON
            );
            g2d.setFont(new Font("Arial", Font.PLAIN, 12));

            // Add system information text
            g2d.setColor(new Color(255, 255, 255));
            int y = padding + lineHeight;
            int x = width - infoWidth + padding;
            for (String info : infoItems) {
                g2d.drawString(info, x , y);
                y += lineHeight;
            }

            g2d.dispose();

            // Save the new image
            var outputFile = new File(outputImagePath);
            ImageIO.write(newImage, "PNG", outputFile);
            return outputFile;
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
        return null;
    }

    private static List<String> getNetworkInfo() {
        List<String> info = new ArrayList<>();

        // Give the network stack time to update
        try {
            Thread.sleep(3_000);
        } catch (InterruptedException e) {
            // Should not arrive here
        }

        info.add("Network Information");

        // IP Addresses
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (!ni.isLoopback() && ni.isUp()) {
                    Enumeration<InetAddress> addresses = ni.getInetAddresses();
                    while (addresses.hasMoreElements()) {
                        InetAddress addr = addresses.nextElement();
                        if (addr instanceof Inet4Address) {  // Only include IPv4 addresses
                            info.add("  IP (" + ni.getDisplayName() + "): " + addr.getHostAddress());
                            System.out.println("Network found: " + ni.getDisplayName() + " - " + addr.getHostAddress());
                        }
                    }

                }
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }

        return info;
    }

    private static List<String> getSystemInfo() {
        List<String> info = new ArrayList<>();

        // OS Information
        info.add("Operating System");
        info.add("   Name: " + System.getProperty("os.name"));
        info.add("   Arch: " + System.getProperty("os.arch"));
        if (System.getProperty("os.name").toLowerCase().contains("linux")) {
            info.add("   Kernel: " + execute(Arrays.asList("uname", "-r")));
        }
        info.add("   Hostname: " + getHostName());

        // Java Version
        info.add("Java");
        info.add("   Version: " + System.getProperty("java.version"));
        info.add("   Runtime: " + System.getProperty("java.runtime.version"));
        info.add("   Vendor: " + System.getProperty("java.vendor"));

        // Raspberry Pi info
        var pi4j = Pi4J.newAutoContext();
        info.add("Raspberry Pi");
        info.add("   Model: " + pi4j.boardInfo().getBoardModel().getLabel());

        // Overall system memory using OperatingSystemMXBean
        OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
        long totalPhysicalMemorySize = osBean.getTotalPhysicalMemorySize() / (1024 * 1024);
        long freePhysicalMemorySize = osBean.getFreePhysicalMemorySize() / (1024 * 1024);
        info.add("System Memory");
        info.add("   Total: " + totalPhysicalMemorySize + "MB");
        info.add("   Free: " + freePhysicalMemorySize + "MB");

        return info;
    }

    private static String getHostName() {
        String hostname = null;

        // Method 1: Try InetAddress.getLocalHost().getHostName()
        try {
            hostname = InetAddress.getLocalHost().getHostName();
            if (isValidHostname(hostname)) {
                return hostname;
            }
        } catch (UnknownHostException e) {
            // Silently proceed to next method
        }

        // Method 2: Try InetAddress.getLocalHost().getCanonicalHostName()
        try {
            hostname = InetAddress.getLocalHost().getCanonicalHostName();
            if (isValidHostname(hostname)) {
                return hostname;
            }
        } catch (UnknownHostException e) {
            // Silently proceed to next method
        }

        // Method 3: Try system property
        hostname = System.getProperty("hostname");
        if (isValidHostname(hostname)) {
            return hostname;
        }

        // Method 4: Try environment variables
        hostname = System.getenv("COMPUTERNAME"); // Windows
        if (isValidHostname(hostname)) {
            return hostname;
        }

        hostname = System.getenv("HOSTNAME"); // Unix/Linux
        if (isValidHostname(hostname)) {
            return hostname;
        }

        // Method 5: Execute hostname command as last resort
        try {
            Process process;
            if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                process = Runtime.getRuntime().exec("hostname");
            } else {
                process = Runtime.getRuntime().exec(new String[]{"hostname"});
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                hostname = reader.readLine();
                if (isValidHostname(hostname)) {
                    return hostname;
                }
            }
        } catch (IOException e) {
            // Silently proceed to default return
        }

        // If all methods fail, return "unknown-host"
        return "";
    }

    private static boolean isValidHostname(String hostname) {
        return hostname != null && !hostname.isEmpty() && !hostname.trim().equals("localhost");
    }

    private static int executeWithExitCode(List<String> command) throws IOException {
        System.out.println("Executing: " + String.join(" ", command));

        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();

            // Read and display output
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                reader.lines().forEach(System.out::println);
            }

            // Wait for completion with timeout
            if (process.waitFor(1, TimeUnit.MINUTES)) {
                return process.exitValue();
            } else {
                process.destroyForcibly();
                System.err.println("Process timed out");
                return -1;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return -1;
        }
    }

    private static String execute(List<String> command) {
        System.out.println("Excecuting: " + String.join(" ", command));

        var result = new StringBuilder();
        try {
            // Create and configure ProcessBuilder
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);

            // Start process
            Process process = processBuilder.start();

            // Read output using modern try-with-resources
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                reader.lines().forEach(result::append);
            }

            // Wait for completion with timeout
            if (process.waitFor(1, TimeUnit.MINUTES)) {
                System.out.println("Process completed with exit code: " + process.exitValue());
            } else {
                process.destroyForcibly();
                System.err.println("Process timed out");
            }
        } catch (Exception e) {
            System.err.println("Error retrieving output from cmd: " + String.join(" ", command));
        }
        return result.toString();
    }
}

