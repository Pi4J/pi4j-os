
open module hellopi4j {
     // Pi4J Modules
    requires com.pi4j;
    requires com.pi4j.library.pigpio;
    requires com.pi4j.plugin.pigpio;
    requires com.pi4j.plugin.raspberrypi;
    requires com.pi4j.plugin.mock;
    uses com.pi4j.extension.Extension;
    uses com.pi4j.provider.Provider;

    requires org.slf4j;

    exports hellopi4j
}
