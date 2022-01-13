package hellopi4j;

import java.time.Duration;

import com.pi4j.Pi4J;
import com.pi4j.io.gpio.digital.DigitalInput;
import com.pi4j.io.gpio.digital.PullResistance;
import com.pi4j.library.pigpio.PiGpio;
import com.pi4j.plugin.pigpio.provider.gpio.digital.PiGpioDigitalInputProvider;
import com.pi4j.plugin.raspberrypi.platform.RaspberryPiPlatform;

/**
 * Minimal Example for accessing a Button via Pi4J.
 * <p>
 * The arcade consoles typically have just a number of buttons attached to the GPIO of Raspberry Pi, no LED or LCD displays for example.
 * <p>
 * In a RaspPi terminal the command  'gpio readall' will give you all the pin numbers in the different numbering schemes.
 * <p>
 * In Pi4J BCM numbering scheme is used as default.
 * <p>
 * Slightly modified example provided by Frank Delporte.
 *
 */
public class MinimalPi4J {
    private static final int PIN_BUTTON = 25;  //LEFT-Button on CrowPi,  Button-4 of Picade

    public static void main(String[] args) {
        // ------------------------------------------------------------
        // Initialize the Pi4J Runtime Context
        // ------------------------------------------------------------
        // Before you can use Pi4J you must initialize a new runtime
        // context.
        //
        // The 'Pi4J' static class includes a few helper context
        // creators for the most common use cases.
        //
        // 'newAutoContext()' will automatically load all available Pi4J
        // extensions found in the application's classpath which
        // may include 'Platforms' and 'I/O Providers'

        // to get this default context you can use:

         var pi4j = Pi4J.newAutoContext();

        // Here we will create I/O interfaces for a (GPIO) digital input pin.
        final var buttonConfig = DigitalInput.newConfigBuilder(pi4j)
                                       .id("BCM_" + PIN_BUTTON)
                                       .name("Button")
                                       .address(PIN_BUTTON)
                                       .pull(PullResistance.PULL_UP)
                                       .debounce(3000L)
                                       .provider("pigpio-digital-input")
                                       .build();  //don't forget to build the config


        //create a DigitalInput for the given buttonConfig
        final var button = pi4j.create(buttonConfig);

        // specify some action, that will be triggered whenever the button's state changed
        button.addListener(e -> {
            switch (e.state()) {
                case LOW:
                    System.out.println("Button was depressed!");
                    break;
                case HIGH:
                    System.out.println("Button was pressed!");
                    break;
                case UNKNOWN:
                    System.out.println("Something unknown happened!!");
                    break;
                default:
                    System.out.println("if something else happens, it's a bug in Pi4J, this is the state '" + e.state() + "'");
            }
        });

        System.out.println("Press the button to see it in action!");

        // Wait for 15 seconds while handling events before exiting
        delay(Duration.ofSeconds(15));

        // ------------------------------------------------------------
        // Terminate the Pi4J library
        // ------------------------------------------------------------
        // We are all done and want to exit our application, we must
        // call the 'shutdown()' on the Pi4J static helper class.
        // This will ensure that all I/O instances are
        // released by the the system and shutdown in the appropriate
        // manner. It will also ensure that any background
        // threads/processes are cleanly shutdown and any used memory
        // is returned to the system.

        pi4j.shutdown();
    }

    private static void delay(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
