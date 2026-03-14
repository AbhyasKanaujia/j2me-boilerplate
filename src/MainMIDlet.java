import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Form;
import javax.microedition.midlet.MIDlet;

public class MainMIDlet extends MIDlet {
    private final Display display;
    private final Form form;

    public MainMIDlet() {
        display = Display.getDisplay(this);
        form = new Form("Hello J2ME");
        form.append("Hello from J2ME!");
    }

    public void startApp() {
        display.setCurrent(form);
    }

    public void pauseApp() {
        // No background behavior for this sample MIDlet.
    }

    public void destroyApp(boolean unconditional) {
        // No resources to release.
    }
}
