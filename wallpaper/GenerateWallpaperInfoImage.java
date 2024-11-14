///usr/bin/env jbang "$0" "$@" ; exit $?

//DEPS com.pi4j:pi4j-core:2.7.0

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
 * The input and output image must be provided as args.
 * As no dependencies are used, this can be executed with java instead of jbang:
 * java <input-image-path> <output-image-path>
 *
 * Example usages:
 * cd wallpaper
 * jbang GenerateWallpaperInfoImage.java wallpaper-2-1920x1080.png wallpaper-out.png 1280 800
 */
public class GenerateWallpaperInfoImage {

    public static void main(String[] args) {
        if (args.length != 4) {
            System.out.println("Usage: java <input-image-path> <output-image-path> <width> <height>");
            return;
        }

        var width = 0;
        var height = 0;

        try {
            width = Integer.parseInt(args[2]);
            height = Integer.parseInt(args[3]);
        } catch (Exception e) {
            System.err.println("Could not parse the width and/or height");
        }

        var outputFile = generateSystemInfoImage(args[0], args[1], width, height);

        if (outputFile == null) {
            System.err.println("No output image could be created...");
        } else {
            System.out.println("Image generated successfully");

            // Update the wallpaper on the screen
            try {
                execute(Arrays.asList("pcmanfm", "--set-wallpaper", outputFile.getCanonicalPath()));
                System.out.println("Wallpaper is updated");
            } catch (IOException e) {
                System.err.println("Failed to update wallpaper");
            }
        }
    }

    public static File generateSystemInfoImage(String inputImagePath, String outputImagePath, int width, int height) {
        try {
            // Read the input image
            BufferedImage originalImage = ImageIO.read(new File(inputImagePath));

            // Create a copy of the image
            BufferedImage newImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

            // Draw the original image
            Graphics2D g2d = newImage.createGraphics();
            g2d.drawImage(originalImage, 0, 0, width, height, null);

            // Configure text rendering
            g2d.setRenderingHint(
                    RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON
            );
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 16));

            // Info box
            int padding = 10;
            int lineHeight = 20;
            List<String> systemInfo = getSystemInfo();
            int infoHeight = systemInfo.size() * lineHeight + (3 * padding);

            // Add semi-transparent background for text
            int startBoxY = newImage.getHeight() - infoHeight - 20;
            g2d.setColor(new Color(0, 0, 0, 180));
            g2d.fillRect(20, startBoxY, (newImage.getWidth() / 2) - 20, infoHeight);

            // Add system information text
            g2d.setColor(Color.WHITE);
            int y = startBoxY + padding + lineHeight;
            for (String info : systemInfo) {
                g2d.drawString(info, padding + 20, y);
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

    private static List<String> getSystemInfo() {
        List<String> info = new ArrayList<>();

        // OS Information
        info.add("Operating System");
        info.add("   Name: " + System.getProperty("os.name"));
        info.add("   Version: " + System.getProperty("os.version"));
        info.add("   Arch: " + System.getProperty("os.arch"));
        if (System.getProperty("os.name").toLowerCase().contains("linux")) {
            info.add("   Kernel: " + execute(Arrays.asList("uname", "-r")));
        }

        // Java Version
        info.add("Java");
        info.add("   Version: " + System.getProperty("java.version"));
        info.add("   Runtime: " + System.getProperty("java.runtime.version"));
        info.add("   Vendor: " + System.getProperty("java.vendor"));

        // Raspberry Pi info
        var pi4j = Pi4J.newAutoContext();
        info.add("Raspberry Pi");
        info.add("   Board model: " + pi4j.boardInfo().getBoardModel().getLabel());

        // IP Addresses
        info.add("Network");
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (!ni.isLoopback() && ni.isUp()) {
                    Enumeration<InetAddress> addresses = ni.getInetAddresses();
                    while (addresses.hasMoreElements()) {
                        InetAddress addr = addresses.nextElement();
                        if (addr instanceof Inet4Address) {  // Only include IPv4 addresses
                            info.add("   IP (" + ni.getDisplayName() + "): " + addr.getHostAddress());
                        }
                    }
                }
            }
        } catch (Exception e) {
            info.add("Error retrieving network interfaces: " + e.getMessage());
        }

        // Overall system memory using OperatingSystemMXBean
        OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
        long totalPhysicalMemorySize = osBean.getTotalPhysicalMemorySize() / (1024 * 1024);
        long freePhysicalMemorySize = osBean.getFreePhysicalMemorySize() / (1024 * 1024);
        info.add("System Memory");
        info.add("   Total: " + totalPhysicalMemorySize + "MB");
        info.add("   Free: " + freePhysicalMemorySize + "MB");

        // Timestap
        info.add("Generated on " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        return info;
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
