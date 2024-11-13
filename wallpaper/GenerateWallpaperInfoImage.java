import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.Enumeration;
import java.util.List;

/**
 * Code to create a wallpaper image with system information.
 *
 * The input and output image must be provided as args.
 * As no dependencies are used, this can be executed with java instead of jbang:
 * java <input-image-path> <output-image-path>
 *
 * Example usages:
 * java GenerateWallpaperInfoImage.java data/wallpaper-2-1920x1080.jpg wallpaper.png
 */
public class GenerateWallpaperInfoImage {

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: java <input-image-path> <output-image-path>");
            return;
        }

        var outputFile = generateSystemInfoImage(args[0], args[1]);

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

    public static File generateSystemInfoImage(String inputImagePath, String outputImagePath) {
        try {
            // Read the input image
            BufferedImage originalImage = ImageIO.read(new File(inputImagePath));

            // Create a copy of the image
            BufferedImage newImage = new BufferedImage(
                    originalImage.getWidth(),
                    originalImage.getHeight(),
                    BufferedImage.TYPE_INT_ARGB
            );

            // Draw the original image
            Graphics2D g2d = newImage.createGraphics();
            g2d.drawImage(originalImage, 0, 0, null);

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
        info.add("   Java Version: " + System.getProperty("java.runtime.version") + ", " + System.getProperty("java.vendor"));
        info.add("   JavaFX Version: " + System.getProperty("javafx.runtime.version"));

        // Java Version
        info.add("Raspberry Pi");
        info.add("   Board model: " + execute(Arrays.asList("cat", "/proc/cpuinfo", "|", "grep", "'Revision'", "|", "awk", "'{print $3}'")));

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

        // System resources
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory() / (1024 * 1024);
        long totalMemory = runtime.totalMemory() / (1024 * 1024);
        long freeMemory = runtime.freeMemory() / (1024 * 1024);
        info.add("Memory");
        info.add("   Max: " + maxMemory + "MB");
        info.add("   Total: " + totalMemory + "MB");
        info.add("   Free: " + freeMemory + "MB");

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
