/**
 * JMenuMaker — Easy, fast, free and flexiable menu system builder for Java.
 *
 * Copyright © 2011  Mattias Andrée (maandree@kth.se)
 *
 * This library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this library.  If not, see <http://www.gnu.org/licenses/>.
 */
package se.kth.maandree.jmenumaker;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.lang.ref.*;


/**
 * This is the menu builder class
 *
 * @version  1.0
 * @author   Mattias Andrée, <a href="mailto:maandree@kth.se">maandree@kth.se</a>
 */
public class JMenuMaker
{
    /**
     * Hidden constructor
     */
    private JMenuMaker()
    {
        //Nullify default constructor
    }
    
    
    
    /**
     * Whether to print rendering information (verbose)
     */
    public static boolean errOutput = true;
    
    
    
    /**
     * Decorates the window
     * 
     * @param   window    Windows do decorate, must be {@link JFrame}, {@link JDialog} or {@link JInternalFrame}
     * @param   file      The file containing the menu configurations
     * @param   listener  Update listener for the menu items
     * @param   menp      Menp instance for auto invocation
     * @return            A {@link HashMap} with all menus, with an ID, added by the menu maker
     * 
     * @throws  IOException  If the configuration file cannot be read
     */
    public static HashMap<String, WeakReference<Component>> makeMenu(final Container window, final String file, final UpdateListener listener, final Menp menp) throws IOException
    {
        boolean quite = false;
        final InputStream conf = new BufferedInputStream(new FileInputStream(file));
        final ArrayList<InputStream> fileStack = new ArrayList<InputStream>();
        fileStack.add(conf);
        try
        {
            final JMenuBar menu = new JMenuBar();
            
            if (window instanceof JFrame)
                ((JFrame)window).setJMenuBar(menu);
            else if (window instanceof JDialog)
                ((JDialog)window).setJMenuBar(menu);
            else if (window instanceof JInternalFrame)
                ((JInternalFrame)window).setJMenuBar(menu);
            else
                throw new RuntimeException(window instanceof JWindow ? "JWindow:s can not have JMenuBar:s"
                                                                     : "window is not JFrame, JDialog or JInternalFrame.");
            
            final HashMap<String, WeakReference<Component>> menuItems = new HashMap<String, WeakReference<Component>>();
            final WeakHashMap<Component, String> reverseId = new WeakHashMap<Component, String>();
            final HashSet<KeyStroke> accelerators = new HashSet<KeyStroke>();
            final ArrayList<String> aliveTargets = new ArrayList<String>();
            final ArrayList<Component> aliveIndicators = new ArrayList<Component>();
            final HashMap<String, String> invokes = new HashMap<String, String>();
            
            final ActionListener actionListener = new ActionListener()
                    {
                        private static final long serialVersionUID = 1;
                        
                        public void actionPerformed(final ActionEvent event)
                        {
                            String id = event.getActionCommand();
                            if (menp != null)
                            {
                                String invoke;
                                if ((invoke = invokes.get(id)) != null)
                                    menp.invoke(invoke, menuItems, id);
                            }
                            
                            if (listener == null)
                                return;
                            
                            final boolean not = id.startsWith("\0");
                            if (not)
                                id = id.substring(1);
                            
                            Object item;
                            if ((item = event.getSource()) != null)
                                if (item instanceof JCheckBoxMenuItem)
                                    listener.valueUpdated(id, ((JCheckBoxMenuItem)item).isSelected() ^ not);
                                else if (item instanceof JRadioButtonMenuItem)
                                    listener.valueUpdated(id, ((JRadioButtonMenuItem)item).isSelected() ^ not);
                                else
                                    listener.itemClicked(id);
                        }
                    };
            
            final ChangeListener changeListener = new ChangeListener()
                    {
                        private static final long serialVersionUID = 1;
                        
                        public void stateChanged(final ChangeEvent event)
                        {
                            if (listener == null)
                                return;
                            
                            String id;
                            Object item;
                            if ((id = reverseId.get(item = event.getSource())) != null)
                                if (item instanceof JSlider)
                                    listener.valueUpdated(id, ((JSlider)item).getValue());
                        }
                    };
            
            final ArrayList<JComponent> stack = new ArrayList<JComponent>();
            final ArrayList<ButtonGroup> groupStack = new ArrayList<ButtonGroup>();
            final ArrayList<Scanner> scanStack = new ArrayList<Scanner>();
            final ArrayList<String> fileNameStack = new ArrayList<String>();
            final ArrayList<int[]> lineStack = new ArrayList<int[]>();
            Scanner scan = new Scanner(conf, "UTF-8");
            
            lineStack.add(new int[] {0});
            scanStack.add(scan);
            fileNameStack.add(file);
            stack.add(menu);
            JComponent menuItem;
            
            String line;
            for (;;)
            {
                final Scanner sc = scanStack.get(0);
                if (sc.hasNextLine() == false)
                {
                    fileNameStack.remove(0);
                    lineStack.remove(0);
                    fileStack.remove(0).close();
                    scanStack.remove(0);
                    break;
                }
                
                int lineIndex = lineStack.get(0)[0] + 1;
                line = sc.nextLine();
                if (line.startsWith("&quite!") == false)
                    errprintln("\033[35mAt line " + lineIndex + " in " + fileNameStack.get(0) + ":  " + line + "\033[m");
                lineStack.remove(0);
                lineStack.add(0, new int[] {lineIndex});
                int at = -1;
                for (int i = 0, n = line.length(); i < n;)
                {
                    final char c = line.charAt(i++);
                    if (c == '@')
                    {
                        final String atFile = line.substring(i);
                        fileNameStack.add(0, atFile);
                        lineStack.add(0, new int[] {0});
                        
                        final InputStream atConf = new BufferedInputStream(new FileInputStream(atFile));
                        final Scanner atScan = new Scanner(atConf);
                        fileStack.add(0, atConf);
                        scanStack.add(0, atScan);
                        
                        break;
                    }
                    if ((c != ' ') && (c != '\t'))
                        break;
                }
                while (line.endsWith("\\") && sc.hasNextLine())
                {
                    final String appline = sc.nextLine();
                    if (line.startsWith("&quite!") == false)
                        errprintln("\033[35mAppending line " + ++lineIndex + ":  " + line + "\033[m");
                    line = line.substring(0, line.length() - 1) + appline;
                }
                lineStack.remove(0);
                lineStack.add(0, new int[] {lineIndex});
                
                if (line.replace(" ", "").replace("\t", "").isEmpty())
                    continue;
                
                final char[] token = new char[line.length()];
                int ptr = 0;
                
                boolean str = false, par = false, bang = false, group = false, ungroup = false, enabled = false;
                boolean push = false, pop = false, weak = false, hard = false, disabled = false, hidden = false;
                boolean nullgroup = false, rich = false;
                String caption = null, special = null;
                final ArrayList<String> settings = new ArrayList<String>();
                
                line = line + ' ';
                for (int i = 0, n = line.length(); i < n;)
                {
                    final char c = line.charAt(i++);
                    
                    if ((ptr == 0) && (c == '!'))
                        bang = true;
                    else if ((ptr == 0) && (c == '$'))
                        rich = true;
                    else if (str)
                    {
                        if ((token[ptr++] = c) == '"')
                            str = false;
                    }
                    else if (par)
                    {
                        if ((token[ptr++] = c) == ')')
                            par = false;
                        else if (c == '"')
                            str = !str;
                    }
                    else if ((c != ' ') && (c != '\t'))
                    {
                        if ((token[ptr++] = c) == '(')
                            par = true;
                        else if (c == '"')
                            str = true;
                    }
                    else
                    {
                        if (ptr != 0)
                        {
                            if (token[0] == '#')
                                break;
                            
                            final String item = new String(token, 0, ptr);
                            
                            if (item.equals(">"))               push      = true;
                            else if (item.equals("<"))          pop       = true;
                            else if (item.equals("{"))          group     = true;
                            else if (item.equals("}"))          ungroup   = true;
                            else if (item.equals("{~"))         nullgroup = true;
                            else if (item.equals("~}"))         ungroup   = true;
                            else if (item.equals("-"))          weak      = true;
                            else if (item.equals("--"))         hard      = true;
                            else if (item.equals("disabled"))   disabled  = true;
                            else if (item.equals("enabled"))    enabled   = true;
                            else if (item.equals("hidden"))     hidden    = true;
                            else if (item.equals("&quite!"))    errOutput = !(quite = true);
                            else if (item.equals("&verbose!"))  errOutput = !(quite = false);
                            else if (item.startsWith("\""))
                                caption = item.substring(1, item.length() - 1).replace("\"\"", "\"").replace("\\\\", "\\");
                            else if (item.startsWith("("))
                                special = item.substring(1, item.length() - 1);
                            else
                                settings.add(item);
                        }
                        ptr = 0;
                    }
                }
                
                
                int iBang = 0;
                if (caption != null)
                {
                    if (bang)
                    {
                        iBang = caption.indexOf("!");
                        caption = caption.substring(0, iBang) + caption.substring(iBang + 1);
                    }
                    if (rich)
                        caption = "<html>" + caption + "</html>";
                }
                
                
                menuItem = null;
                
                if (pop)
                    stack.remove(0);
                if (ungroup)
                    groupStack.remove(0);
                
                if (hard)
                    stack.get(0).add(menuItem = new JSeparator());
                else if (weak)
                    stack.get(0).add(menuItem = new JWeakSeparator(menu));
                else
                    if (caption != null)
                    {
                        boolean selected = false, check = false, radio = false;
                        
                        for (final String setting : settings)
                            if (setting.equals("type=check"))                  check = true;
                            else if (setting.equals("type=CHECK"))  selected = check = true;
                            else if (setting.equals("type=radio"))             radio = true;
                            else if (setting.equals("type=RADIO"))  selected = radio = true;
                        
                        if (push)        menuItem = new JMenu(caption);
                        else if (check)  menuItem = new JCheckBoxMenuItem(caption, selected);
                        else if (radio)  menuItem = new JRadioButtonMenuItem(caption, selected);
                        else             menuItem = new JMenuItem(caption);
                        
                        stack.get(0).add(menuItem);
                    }
                    else if (special != null)
                        if (special.equals(" "))
                            stack.get(0).add(menuItem = new JMenuSpacer());
                        else if (special.startsWith("!") && (special.startsWith("!\"") == false))
                        {
                            if (special.equals("!slider"))
                            {
                                boolean vertical = false;
                                int min = 0, max = 0, value = 0, extent = 0;
                                
                                for (final String setting : settings)
                                    if (setting.equals("vertical"))          vertical = true;
                                    else if (setting.startsWith("min="))     min    = Integer.parseInt(setting.substring(4));
                                    else if (setting.startsWith("max="))     max    = Integer.parseInt(setting.substring(4));
                                    else if (setting.startsWith("value="))   value  = Integer.parseInt(setting.substring(6));
                                    else if (setting.startsWith("extent="))  extent = Integer.parseInt(setting.substring(7));
                                
                                stack.get(0).add(menuItem = new JSlider(vertical ? JSlider.VERTICAL : JSlider.HORIZONTAL, min, max, value));
                                ((JSlider)menuItem).setExtent(extent);
                            }
                        }
                        else
                        {
                            final String parse = special + ' ';
                            final char[] stoken = new char[special.length()];
                            ptr = 0;
                            
                            String text = null, tag = null;
                            int iText = 0, iTag = 0, tokenIndex = 0;
                            boolean delimiter = false;
                            rich = bang = str = false;
                            
                            for (int i = 0, n = parse.length(); i < n;)
                            {
                                final char c = parse.charAt(i++);
                                
                                if ((ptr == 0) && (c == '!'))
                                    bang = true;
                                else if ((ptr == 0) && (c == '$'))
                                    rich = true;
                                else if (str)
                                {
                                    if ((stoken[ptr++] = c) == '"')
                                        str = false;
                                }
                                else if ((c != ' ') && (c != '\t'))
                                {
                                    if ((stoken[ptr++] = c) == '"')
                                        str = true;
                                }
                                else
                                {
                                    if (ptr != 0)
                                    {
                                        final String item = new String(stoken, 0, ptr);
                                        
                                        if (item.equals("?"))
                                            delimiter = true;
                                        else if (item.startsWith("\""))
                                        {
                                            text = item.substring(1, item.length() - 1).replace("\"\"", "\"").replace("\\\\", "\\");
                                            iText = tokenIndex;
                                        }
                                        else
                                        {
                                            tag = item;
                                            iTag = tokenIndex;
                                        }
                                        
                                        tokenIndex++;
                                    }
                                    ptr = 0;
                                }
                            }

                            if ((text != null))
                            {
                                if (bang)
                                {
                                    iBang = text.indexOf("!");
                                    text = text.substring(0, iBang) + text.substring(iBang + 1);
                                }
                                if (rich)
                                    text = "<html>" + text + "</html>";
                            }
                            
                            if (tag != null)
                                if (delimiter)
                                    if (iTag < iText)
                                    {
                                        final JMenuTag item;
                                        stack.get(0).add(item = JMenuTag.getInstance(tag));
                                        item.setEmptyIndicator(menuItem = new JMenuItem(text));
                                        disabled = !enabled;
                                    }
                                    else
                                    {
                                        final JMenuTag item;
                                        boolean selected = false, check = false, radio = false;
                                        
                                        for (final String setting : settings)
                                            if (setting.equals("type=check"))                  check = true;
                                            else if (setting.equals("type=CHECK"))  selected = check = true;
                                            else if (setting.equals("type=radio"))             radio = true;
                                            else if (setting.equals("type=RADIO"))  selected = radio = true;
                                        
                                        if (push)        menuItem = new JMenu(text);
                                        else if (check)  menuItem = new JCheckBoxMenuItem(text, selected);
                                        else if (radio)  menuItem = new JRadioButtonMenuItem(text, selected);
                                        else             menuItem = new JMenuItem(text);
                                        
                                        stack.get(0).add(menuItem);
                                        menuItem.setVisible(false);
                                        
                                        aliveTargets.add(tag);
                                        aliveIndicators.add(menuItem);
                                    }
                                else
                                    stack.get(0).add(JMenuTag.getInstance(tag));
                        }
                
                if (menuItem != null)
                {
                    String id = null;
                    
                    for (final String setting : settings)
                    {
                        if (setting.startsWith("id="))
                            id = getStringValue(setting);
                        else if (setting.startsWith("~id="))
                            id = '\0' + getStringValue(setting);
                        
                        if (id != null)
                        {
                            if (menuItems.containsKey(id))
                                throw new RuntimeException("Reuse of unique ID: " + id.replace("\0", "~"));
                            
                            menuItems.put(id, new WeakReference<Component>(menuItem));
                            if (menuItem instanceof AbstractButton)
                            {
                                ((AbstractButton)menuItem).setActionCommand(id);
                                ((AbstractButton)menuItem).addActionListener(actionListener);
                            }
                            else if (menuItem instanceof JSlider)
                            {
                                reverseId.put(menuItem, id);
                                ((JSlider)menuItem).addChangeListener(changeListener);
                            }
                            break;
                        }
                    }
                    
                    if (menuItem instanceof AbstractButton)
                    {
                        final AbstractButton button = (AbstractButton)menuItem;
                        
                        if (groupStack.isEmpty() == false)
                            if (groupStack.get(0) != null)
                                groupStack.get(0).add(button);
                        
                        final ArrayList<String> setAttributes = new ArrayList<String>();
                        final ArrayList<String> setValues     = new ArrayList<String>();
                        final ArrayList<String> targetIds     = new ArrayList<String>();
                        
                        for (final String setting : settings)
                            if      (setting.startsWith("invoke="))                  invokes.put(id, setting.substring("invoke=".length()));
                            else if (setting.startsWith("icon="))                    button.setIcon                (new ImageIcon(getStringValue(setting)));
                            else if (setting.startsWith("icon@disabled="))           button.setDisabledIcon        (new ImageIcon(getStringValue(setting)));
                            else if (setting.startsWith("icon@disabled&selected="))  button.setDisabledSelectedIcon(new ImageIcon(getStringValue(setting)));
                            else if (setting.startsWith("icon@selected&disabled="))  button.setDisabledSelectedIcon(new ImageIcon(getStringValue(setting)));
                            else if (setting.startsWith("icon@pressed="))            button.setPressedIcon         (new ImageIcon(getStringValue(setting)));
                            else if (setting.startsWith("icon@rollover="))           button.setRolloverIcon        (new ImageIcon(getStringValue(setting)));
                            else if (setting.startsWith("icon@rollover&selected="))  button.setRolloverSelectedIcon(new ImageIcon(getStringValue(setting)));
                            else if (setting.startsWith("icon@selected&rollover="))  button.setRolloverSelectedIcon(new ImageIcon(getStringValue(setting)));
                            else if (setting.startsWith("icon@selected="))           button.setSelectedIcon        (new ImageIcon(getStringValue(setting)));
                            else if (setting.equals("rolloverable"))                 button.setRolloverEnabled(true);
                            else if (setting.equals("rolloverable=true"))            button.setRolloverEnabled(true);
                            else if (setting.equals("rolloverable=false"))           button.setRolloverEnabled(false);
                            else if (setting.startsWith("mnemonic="))                button.setMnemonic(parseKeyCode(setting.substring("mnemonic=".length())));
                            else if (setting.startsWith("mnemonicIndex="))
                            {
                                final int index = Integer.parseInt(setting.substring("mnemonicIndex=".length()));
                                button.setMnemonic((int)(caption.charAt(index)));
                                button.setDisplayedMnemonicIndex(index);
                            }
                            else if (setting.startsWith("accelerator="))
                            {
                                if (menuItem instanceof JMenuItem)
                                {
                                    final KeyStroke keyStroke = parseKeyStroke(setting.substring("accelerator=".length()));
                                    if (accelerators.contains(keyStroke))
                                        throw new RuntimeException("Reuse of accelerator: " + setting.substring("accelerator=".length()));
                                    accelerators.add(keyStroke);
                                    ((JMenuItem)menuItem).setAccelerator(keyStroke);
                                }
                            }
                            else if (setting.startsWith("setAttribute="))
                                for (final String part : setting.substring("setAttribute=".length()).split(";"))
                                    setAttributes.add(part);
                            else if (setting.startsWith("setValue="))
                                setValues.addAll(getStringValues(setting));
                            else if (setting.startsWith("targetId="))
                                targetIds.addAll(getStringValues(setting));
                        
                        if (bang && (caption != null))
                        {
                            button.setMnemonic((int)(caption.charAt(iBang)));
                            button.setDisplayedMnemonicIndex(iBang);
                        }
                        
                        if (setAttributes.isEmpty() == false)
                            if ((setAttributes.size() == setValues.size()) && (setValues.size() == targetIds.size()))
                                button.addActionListener(new ActionListener()
                                        {
                                            /**
                                             * {@inheritDoc}
                                             */
                                            public void actionPerformed(final ActionEvent event)
                                            {
                                                for (int i = 0, n = targetIds.size(); i < n; i++)
                                                {
                                                    final String setAttribute = setAttributes.get(i);
                                                          String setValue     = setValues.    get(i);
                                                    final String targetId     = targetIds.    get(i);
                                                    
                                                    final WeakReference<Component> refTarget = menuItems.get(targetId);
                                                    final Component rawTarget;
                                                    
                                                    if ((refTarget == null) || ((rawTarget = refTarget.get()) == null))
                                                        continue;
                                                    
                                                    boolean plus = setValue.endsWith("+");
                                                    boolean minus = setValue.endsWith("-");
                                                    boolean minus2 = setValue.endsWith("--");
                                                    if (minus2)
                                                        setValue = setValue.substring(0, setValue.length() - 2);
                                                    else if (plus || minus)
                                                        setValue = setValue.substring(0, setValue.length() - 1);
                                                    
                                                    int val = Integer.parseInt(setValue);
                                                    
                                                    if (rawTarget instanceof JSlider)
                                                    {
                                                        final JSlider target = (JSlider)rawTarget;
                                                        
                                                        final int min = target.getMinimum();
                                                        final int max = target.getMaximum();
                                                        final int extent = target.getExtent();
                                                        final int value = target.getValue();
                                                        
                                                        if (setAttribute.equals("min"))
                                                        {
                                                            if (plus)         val += min;
                                                            else if (minus2)  val -= min;
                                                            else if (minus)   val = min - val;
                                                            
                                                            if (val > max)    val = max;
                                                            if (value < val)  target.setValue(val);
                                                            target.setMinimum(val);
                                                        }
                                                        if (setAttribute.equals("max"))
                                                        {
                                                            if (plus)         val += max;
                                                            else if (minus2)  val -= max;
                                                            else if (minus)   val = max - val;
                                                            
                                                            if (val < min)    val = min;
                                                            if (value > val)  target.setValue(val);
                                                            target.setMaximum(val);
                                                        }
                                                        if (setAttribute.equals("extent"))
                                                        {
                                                            if (plus)             val += extent;
                                                            else if (minus2)      val -= extent;
                                                            else if (minus)       val = extent - val;
                                                            
                                                            if (val > max - min)  val = max - min;
                                                            
                                                            target.setExtent(val);
                                                        }
                                                        if (setAttribute.equals("value"))
                                                        {
                                                            if (plus)         val += value;
                                                            else if (minus2)  val -= value;
                                                            else if (minus)   val = value - val;
                                                            
                                                            if (val < min)  val = min;
                                                            if (val > max)  val = max;
                                                            
                                                            target.setValue(val);
                                                        }
                                                    }
                                                }
                                            }
                                        });
                    }
                    
                    if (disabled)
                        menuItem.setEnabled(false);
                    
                    if (hidden)
                        menuItem.setVisible(false);
                    
                    if (push)
                        stack.add(0, menuItem);
                }
                
                if (group)
                    groupStack.add(0, new ButtonGroup());
                
                if (nullgroup)
                    groupStack.add(0, null);
            }
            
            for (int i = 0, n = aliveTargets.size(); i < n; i++)
            {
                final String aliveTarget = aliveTargets.get(i);
                final Component aliveIndicator = aliveIndicators.get(i);
                
                JMenuTag.getInstance(aliveTarget).setAliveIndicator(aliveIndicator);
            }
            
            JWeakSeparator.update(menu);
            
            if (menp != null)
                menp.invoke("main", menuItems);
            
            return menuItems;
        }
        finally
        {
            if (quite)
                errOutput = true;
                
            for (final InputStream stream : fileStack)
                stream.close();
        }
    }
    
    
    
    /**
     * Prints text to stderr without any unhandled escape sequences
     *
     * @param  text  The text
     */
    public static void errprint(final String text)
    {
        if (errOutput == false)
            return;
        
        final char[] out = new char[text.length()];
        int ptr = 0;
        
        char c;
        boolean esc = false;
        for (int i = 0, n = text.length(); i < n;)
        {
            c = text.charAt(i++);
            
            if (esc == false)
                if (c == '\033')
                    esc = true;
                else
                    out[ptr++] = c;
            else
                if (c == '[')
                    while (i < n)
                        if (((c = text.charAt(i++)) != '?') && (c != ';') && ((('a' <= c) && (c <= 'z')) || (('A' <= c) && (c <= 'Z'))))
                        {
                            esc = false;
                            break;
                        }
                else if (c == ']')
                    switch (text.charAt(i++))
                    {
                        case 'P':
                            i += 7;
                            esc = false;
                            break;
                        case '0':
                        case '1':
                        case '2':
                            if (text.charAt(i++) == ';')
                                while (i < n)
                                    if (text.charAt(i++) == '\007')
                                        break;
                            esc = false;
                            break;
                        default:
                            esc = false;
                            break;
                    }
                else
                    esc = false;
        }
        
        System.err.print(new String(out, 0, ptr));
    }
    
    /**
     * Prints text to stderr with an extra LN in the end but without any unhandled escape sequences
     *
     * @param  text  The text
     */
    public static void errprintln(final String text)
    {
        if (errOutput == false)
            return;
        
        errprint(text);
        System.err.println();
    }
    
    
    /**
     * Parses a key stoke
     * 
     * @param  code  The code to parse
     */
    static KeyStroke parseKeyStroke(final String code)
    {
        final char[] buf = new char[code.length()];
        int ptr = 0;
        
        boolean a = false, g = false, s = false, m = false, c = false, r = false;
        
        for (int i = 0, n = code.length(); i < n; i++)
        {
            final char chr;
            buf[ptr++] = chr = code.charAt(i);
            
            if (chr == '>')
            {
                final String key = (new String(buf, 1, ptr - 2)).toUpperCase().replace("-", "").replace(" ", "").replace("_", "");
                ptr = 0;
                
                a |= (key.equals("A") || key.equals("ALT") || key.equals("ALTERNATIVE"));
                g |= (key.equals("G") || key.equals("ALTGR") || key.equals("ALTGRAPH") || key.equals("GRAPH"));
                s |= (key.equals("S") || key.equals("SHF") || key.equals("SFT") || key.equals("SHFT") || key.equals("SHIFT"));
                m |= (key.equals("M") || key.equals("MT") || key.equals("META"));
                c |= (key.equals("C") || key.equals("CTR") || key.equals("CTRL") || key.equals("CNTRL") || key.equals("CONTROL"));
                r |= (key.equals("R") || key.equals("RLS") || key.equals("RELEASE"));
            }
        }
        
        if (ptr == 0)
            return null;
        
        int mod;
        mod  = !a ? 0 : InputEvent.ALT_DOWN_MASK;
        mod |= !g ? 0 : InputEvent.ALT_GRAPH_DOWN_MASK;
        mod |= !s ? 0 : InputEvent.SHIFT_DOWN_MASK;
        mod |= !m ? 0 : InputEvent.META_DOWN_MASK;
        mod |= !c ? 0 : InputEvent.CTRL_DOWN_MASK;
        
        return KeyStroke.getKeyStroke(parseKeyCode(new String(buf, 0, ptr)), mod, r);
    }
    
    /**
     * Parses a key code using reflection on {@link KeyEvent}
     *
     * @param   code  The code to parse
     * @return        The value of the code
     */
    private static int parseKeyCode(final String code)
    {
        try
        {
            int value = 0;
            
            String vk = "VK_" + code.toUpperCase().replace("-", "_").replace(" ", "_");
            value += KeyEvent.class.getField(vk).getInt(null);
            
            return value;
        }
        catch (final Throwable err)
        {
            errprintln("\033[31mInvalid key: " + code + "\033[m");
            return 0;
        }
    }
    
    
    
    /**
     * Gets the string value of an attribute
     *
     * @param   attribute  The attribute
     * @return             The value of the attribute
     */
    private static String getStringValue(final String attribute)
    {
        String rc = attribute.substring(attribute.indexOf("=") + 1);
        if (rc.startsWith("\"") && rc.endsWith("\""))
            rc = rc.substring(1, rc.length() - 1);
        return rc.replace("\"\"", "\"");
    }
    
    /**
     * Gets the string values of an attribute
     *
     * @param   attribute  The attribute
     * @return             The values of the attribute
     */
    private static ArrayList<String> getStringValues(final String attribute)
    {
        final ArrayList<String> rc = new ArrayList<String>();
        final String parts = attribute.substring(attribute.indexOf("=") + 1).replace("\"\"", "\0") + ';';
        final char[] token = new char[parts.length()];
        int ptr = 0;
        
        boolean str = false, not = false;
        for (int i = 0, n = parts.length(); i < n;)
        {
            final char c = parts.charAt(i++);
            
            if (str)
            {
                if (c == '"')
                    str = false;
                else if ((token[ptr++] = c) == '\0')
                    token[ptr - 1] = '"';
            }
            else if (c == '~')
                not = true;
            else if (c == '"')
                str = true;
            else if (c == '\0')
                token[ptr++] = '"';
            else if (c != ';')
                token[ptr++] = c;
            else
            { 
                String s = new String(token, 0, ptr);
                if (not && s.startsWith("\""))
                    s = "\"\0" + s.substring(1);
                rc.add(s);
                ptr = 0;
            }
        }
        
        return rc;
    }
    
}


