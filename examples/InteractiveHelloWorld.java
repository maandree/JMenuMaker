import se.kth.maandree.jmenumaker.*;

import javax.swing.*;
import java.awt.*;


public class InteractiveHelloWorld extends javax.swing.JFrame implements UpdateListener
{
    public static void main(final String... args) throws Exception
    {
        (new InteractiveHelloWorld()).setVisible(true);
    }

    InteractiveHelloWorld() throws Exception
    {
        super("InteractiveHelloWorld.java");
        this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        this.setSize(new Dimension(300, 200));

        JMenuMaker.makeMenu(this, "InteractiveHelloWorld.jmml", this, null);
    }
    
    public void itemClicked(final String id)
    {
        System.out.println(id);
    }
    
    public void valueUpdated(final String id, final String value)  { /*Not used*/}
    public void valueUpdated(final String id, final double value)  { /*Not used*/}
    public void valueUpdated(final String id, final long value)    { /*Not used*/}
    public void valueUpdated(final String id, final int value)     { /*Not used*/}
    public void valueUpdated(final String id, final boolean value) { /*Not used*/}
}
