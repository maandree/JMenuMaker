import se.kth.maandree.jmenumaker.*;

import javax.swing.*;
import java.awt.*;


public class HelloWorld extends javax.swing.JFrame
{
    public static void main(final String... args) throws Exception
    {
        (new HelloWorld()).setVisible(true);
    }

    HelloWorld() throws Exception
    {
        super("HelloWorld.java");
        this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        this.setSize(new Dimension(300, 200));

        JMenuMaker.makeMenu(this, "HelloWorld.jmml", null, null);
    }
}
