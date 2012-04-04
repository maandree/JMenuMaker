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
import java.awt.*;
import java.util.*;
import java.io.*;
import java.lang.ref.*;


/**
 * Menp (MENu Processing) interpreter
 *
 * @version  1.0
 * @author   Mattias Andrée, <a href="mailto:maandree@kth.se">maandree@kth.se</a>
 */
public class Menp
{
    /**
     * Constructor
     *
     * @param  file  The file with the Memp code
     *
     * @throws  FileNotFoundException  If the file does not exist or cannot be accessed
     * @throws  IOException            On IO exception
     */
    public Menp(final String file) throws FileNotFoundException, IOException
    {
        {
            int size;
            final InputStream is = new FileInputStream(file);
            final byte[] bs = new byte[size = is.available()];
            int ptr = 0;
            int z = size;
            
            while (z > 0)
            {
                int m = is.read(bs, ptr, z);
                z -= m;
                ptr += m;
            }
            
            is.close();
            
            this.code = (new String(bs, "UTF-8")).replace('\f', '\n').replace('\r', '\n').replace('\t', ' ');
        }
        
        final ArrayList<Integer> stack = new ArrayList<Integer>();
        
        boolean str = true;
        for (int i = 0, n = this.code.length(); i < n;)
        {
            final char c = this.code.charAt(i++);
            if (str)
            {
                if (c == '"')
                    str = false;
            }
            else if (c == '(')
                stack.add(0, new Integer(i));
            else if (c == ')')
                this.wormholes.put(stack.remove(0), Integer.valueOf(i));
            else if (c == '@')
            {
                final int j = this.code.indexOf("\n", 0);
                
                int size;
                final InputStream is = new FileInputStream(this.code.substring(i, j));
                final byte[] bs = new byte[size = is.available()];
                int ptr = 0;
                int z = size;
                n += size;
                
                while (z > 0)
                {
                    int m = is.read(bs, ptr, z);
                    z -= m;
                    ptr += m;
                }
                
                final String iCode = new String(bs, "UTF-8").replace("\f", "\n").replace("\r\n", "\n").replace("\r", "\n").replace("\t", " ");
                
                final StringBuilder sb = new StringBuilder();
                sb.append(this.code.substring(0, i));
                sb.append(iCode);
                sb.append(this.code.substring(j));
                this.code = sb.toString();
            }
            else if (c == '#')
            {
                final int j = this.code.indexOf("\n", 0);
                this.code = this.code.substring(0, i) + this.code.substring(j);
            }
            else if (c == '"')
                str = true;
        }
        
        final char[] codeBuf = new char[this.code.length()];
        int ptr = 0;
        str = false;
        for (int i = 0, len = this.code.length(); i < len; i++)
        {
            final char c = this.code.charAt(i);
            if (str)
            {
                if ((codeBuf[ptr++] = c) == '"')
                    str = false;
            }
            else if ((c != ' ') && (c != '\n'))
            {
                codeBuf[ptr++] = c;
                str = c == '"';
            }
        }
        this.code = new String(codeBuf, 0, ptr);
        
        char c;
        StringBuilder buf = new StringBuilder();
        for (int i = 0, n = this.code.length(); i < n; i++)
            if ((c = this.code.charAt(i)) == '(')
                i = this.wormholes.get(new Integer(i));
            else if (c == ':')
            {
                methods.put(buf.toString(), new Integer(i + 1));
                buf = new StringBuilder();
            }
            else if (c != ' ')
                buf.append(c);
    }
    
    
    
    /**
     * The Menp code
     */
    private String code;
    
    /**
     * Map from opening brackets ['('] to closing brackets [')']
     */
    private final HashMap<Integer, Integer> wormholes = new HashMap<Integer, Integer>();
    
    /**
     * Method mapping
     */
    private final HashMap<String, Integer> methods = new HashMap<String, Integer>();
    
    /**
     * Variables
     */
    private final HashMap<String, Object> variables = new HashMap<String, Object>();
    
    
    
    /**
     * Invokes a method in the Menp code
     *
     * @param   method      The method to invoke
     * @param   menuItems   The menu items
     * @param   parameters  Parameters
     * @return              The returned value
     */
    public Object[] invoke(final String method, final HashMap<String, WeakReference<Component>> menuItems, final String... parameters)
    {
        int i = this.methods.get(method).intValue() + 1;
        int end = this.wormholes.get(new Integer(i)).intValue();
        
        final Object[] ret = exec(this.code.substring(i, end), menuItems, parameters);
        if ((ret != null) && (ret.length > 0) && (ret[0] == null))
        {
            final Object[] rc = new Object[ret.length - 1];
            System.arraycopy(rc, 0, ret, 1, rc.length);
            return rc;
        }
        return null;
    }
    
    /**
     * Parses and executes a segment of a Menp code
     *
     * @param   code        The code to execute
     * @param   menuItems   The menu items
     * @param   parameters  Parameters
     * @return              The returned value
     */
    private Object[] exec(final String code, final HashMap<String, WeakReference<Component>> menuItems, final String... parameters)
    {
        int i = 0;
        while (this.code.charAt(i) == '(')
        {
            final int j = this.wormholes.get(new Integer(i)).intValue();
            Object[] ret = exec(code.substring(i + 1, j), menuItems, parameters);
            if ((ret != null) && (ret.length > 0) && (ret[0] == null))
                return ret;
            i = j + 1;
        }

        if (i >= this.code.length())
            return null;
        
        final ArrayList<String> list;
        StringBuilder buf = new StringBuilder();
        switch (this.code.charAt(i++))
        {
            case '>': //call
            {
                list = list(this.code.substring(i));
                return invoke(list.remove(0), menuItems, (String[])(list.toArray()));
            }
            
            case '<': //return
            {
                list = list(this.code.substring(i));
                final Object[] rc = new Object[list.size() + 1];
                int j = 0;
                rc[j++] = null;
                for (final String item : list)
                    rc[j++] = parseItem(item, menuItems, parameters);
                return rc;
            }
            
            case '~': //undeclare
            {
                list = list(this.code.substring(i));
                for (final String item : list)
                    if (item.startsWith("%"))
                        variables.remove(item.substring(1));
                break;
            }
            
            case '.': //expand
            {
                list = list(this.code.substring(i));
                final ArrayList<Object> items = new ArrayList<Object>();
                for (final String sitem : list)
                {
                    final Object item = parseItem(sitem, menuItems, parameters);
                    if (item instanceof ArrayList)
                        for (final Object iitem : (ArrayList)item)
                            items.add(iitem);
                    else
                        items.add(item);
                }
                return items.toArray();
            }
            
            case '*': //not
            {
                list = list(this.code.substring(i));
                final Object[] rc = new Object[list.size()];
                int j = 0;
                for (final String sitem : list)
                {
                    final Object item = parseItem(sitem, menuItems, parameters);
                    if (item instanceof Boolean)
                        rc[j++] = Boolean.valueOf(((Boolean)item).booleanValue() == false);
                    else
                        rc[j++] = item;
                }
                return rc;
            }
            
            case '=': //same
            {
                list = list(this.code.substring(i));
                final ArrayList<Object> items = new ArrayList<Object>();
                for (final String sitem : list)
                    items.add(parseItem(sitem, menuItems, parameters));
                final Object left = items.remove(0);
                if (left instanceof ArrayList)
                {
                    final Object[] lefts = ((ArrayList<?>)left).toArray();
                    if (lefts.length != items.size())
                        return new Object[] { Boolean.FALSE };
                    final HashSet<Object> set = new HashSet<Object>();
                    for (final Object item : items)
                        if (set.contains(item))
                            set.remove(item);
                        else
                            return new Object[] { Boolean.FALSE };
                    return new Object[] { Boolean.TRUE };
                }
                return new Object[] { Boolean.valueOf((items.size() == 1) && (items.get(0).equals(left))) };
            }
            
            case '|': //or / union
            {
                list = list(this.code.substring(i));
                Boolean bool = null;
                final HashSet<Object> set = new HashSet<Object>();
                final ArrayList<Object> items = new ArrayList<Object>();
                for (final String sitem : list)
                {
                    final Object item = items.add(parseItem(sitem, menuItems, parameters));
                    if (item instanceof Boolean)
                        bool = bool == null ? (Boolean)item : Boolean.valueOf(bool.booleanValue() | ((Boolean)item).booleanValue());
                    else if (item instanceof ArrayList)
                    {
                        for (final Object iitem : (ArrayList)item)
                            if (set.contains(iitem) == false)
                            {
                                set.add(iitem);
                                items.add(iitem);
                            }
                    }
                    else
                        if (set.contains(item) == false)
                        {
                            set.add(item);
                            items.add(item);
                        }
                }
                if (bool != null)
                    items.add(bool);
                return items.toArray();
            }
            
            case '&': //and / intersection
            {
                list = list(this.code.substring(i));
                Boolean bool = null, boolic = false, first = true;
                HashSet<Object> set = new HashSet<Object>();
                ArrayList<Object> items = new ArrayList<Object>();
                for (final String sitem : list)
                {
                    final Object item = items.add(parseItem(sitem, menuItems, parameters));
                    if (first)
                    {
                        if (item instanceof Boolean)
                            bool = bool == null ? (Boolean)item : Boolean.valueOf(bool.booleanValue() & ((Boolean)item).booleanValue());
                        else if (item instanceof ArrayList)
                        {
                            for (final Object iitem : (ArrayList)item)
                                if (set.contains(iitem) == false)
                                {
                                    set.add(iitem);
                                    items.add(iitem);
                                }
                        }
                        else
                        {
                            set.add(item);
                            items.add(item);
                        }
                        first = false;
                    }
                    else
                        if (item instanceof Boolean)
                            bool = bool == null ? (Boolean)item : Boolean.valueOf(bool.booleanValue() & ((Boolean)item).booleanValue());
                        else if (item instanceof ArrayList)
                        {
                            final ArrayList<Object> litems = items;
                            final HashSet<Object> rset = new HashSet<Object>();
                            
                            set = new HashSet<Object>();
                            items = new ArrayList<Object>();
                            
                            for (final Object iitem : (ArrayList)item)
                                if (rset.contains(iitem) == false)
                                    rset.add(iitem);
                            
                            for (final Object litem : litems)
                                if (rset.contains(litem))
                                {
                                    set.add(litem);
                                    items.add(litem);
                                }
                        }
                        else
                            if (set.contains(item) == false)
                                boolic = true;
                            else
                            {
                                set = new HashSet<Object>();
                                items = new ArrayList<Object>();
                                set.add(item);
                                items.add(item);
                            }
                }
                if (boolic)
                    items = new ArrayList<Object>();
                if (bool != null)
                    items.add(bool);
                return items.toArray();
            }
            
            case '^': //parity
            {
                list = list(this.code.substring(i));
                Boolean bool = null;
                final HashSet<Object> set = new HashSet<Object>();
                final HashSet<Object> parity = new HashSet<Object>();
                final ArrayList<Object> items = new ArrayList<Object>();
                for (final String sitem : list)
                {
                    final Object item = items.add(parseItem(sitem, menuItems, parameters));
                    if (item instanceof Boolean)
                        bool = bool == null ? (Boolean)item : Boolean.valueOf(bool.booleanValue() ^ ((Boolean)item).booleanValue());
                    else if (item instanceof ArrayList)
                        for (final Object iitem : (ArrayList)item)
                        {
                            if (set.contains(iitem) == false)
                                items.add(iitem);
                            set.add(iitem);
                            if (parity.contains(iitem))
                                parity.remove(iitem);
                            else
                                parity.add(iitem);
                        }
                    else
                    {
                        if (set.contains(item) == false)
                            items.add(item);
                        set.add(item);
                        if (parity.contains(item))
                            parity.remove(item);
                        else
                            parity.add(item);
                    }
                }
                final ArrayList<Object> pitems = new ArrayList<Object>();
                for (final Object item : items)
                    if (parity.contains(item))
                        pitems.add(item);
                if (bool != null)
                    pitems.add(bool);
                return pitems.toArray();
            }
            
            case ',': //if
            {
                list = list(this.code.substring(i));
                boolean cond = false;
                Object item;
                for (final String sitem : list)
                    if ((item = parseItem(sitem, menuItems, parameters)) instanceof Boolean)
                    {
                        cond = ((Boolean)item).booleanValue();
                        break;
                    } 
                return new Object[] {parseItem(list.get(cond ? 1 : 2), menuItems, parameters)};
            }
            
            case ':': //assign
            {
                final String rc;
                list = list(this.code.substring(i));
                variables.put(list.get(0), parseItem(rc = list.get(1), menuItems, parameters));
                return new Object[] { rc };
            }
            
            case '!': //set
            {
                list = list(this.code.substring(i));
                Object item;
                if (list.get(0).startsWith("accelerator="))
                {
                    final String setting = list.remove(0);
                    for (final String sitem : list)
                        if ((item = parseItem(sitem, menuItems, parameters)) instanceof String)
                        {
                            final WeakReference<Component> ref = menuItems.get((String)item);
                            if ((ref != null) && (ref.get() != null) && (ref.get() instanceof JMenuItem))
                                ((JMenuItem)(ref.get())).setAccelerator(JMenuMaker.parseKeyStroke(setting.substring("accelerator=".length())));
                        }
                }
                else
                {
                    final int setting = getSetting(list.remove(0));
                    for (final String sitem : list)
                        if ((item = parseItem(sitem, menuItems, parameters)) instanceof String)
                        {
                            final WeakReference<Component> ref = menuItems.get((String)item);
                            if ((ref != null) && (ref.get() != null))
                                applySetting(ref.get(), setting);
                        }
                }
                break;
            }
            
            case '?': //query
            {
                Object item;
                if (this.code.charAt(i) == '?')
                {
                    if (this.code.charAt(++i) == '?') //query parity
                    {
                        i++;
                        int p = 0;
                        list = list(this.code.substring(i));
                        final int setting = getSetting(list.remove(0));
                        for (final String sitem : list)
                            if ((item = parseItem(sitem, menuItems, parameters)) instanceof String)
                            {
                                final WeakReference<Component> ref = menuItems.get((String)item);
                                if ((ref != null) && (ref.get() != null))
                                    if (querySetting(ref.get(), setting) == false)
                                        p++;
                            }
                        return new Object[] { Boolean.valueOf((p & 1) == 1) };
                    }
                    //query all
                    {
                        list = list(this.code.substring(i));
                        final int setting = getSetting(list.remove(0));
                        for (final String sitem : list)
                            if ((item = parseItem(sitem, menuItems, parameters)) instanceof String)
                            {
                                final WeakReference<Component> ref = menuItems.get((String)item);
                                if ((ref != null) && (ref.get() != null))
                                    if (querySetting(ref.get(), setting) == false)
                                        return new Object[] { Boolean.FALSE };
                            }
                        return new Object[] { Boolean.TRUE };
                    }
                }
                //query any
                {
                    list = list(this.code.substring(i));
                    final int setting = getSetting(list.remove(0));
                    for (final String sitem : list)
                        if ((item = parseItem(sitem, menuItems, parameters)) instanceof String)
                        {
                            final WeakReference<Component> ref = menuItems.get((String)item);
                            if ((ref != null) && (ref.get() != null))
                                if (querySetting(ref.get(), setting))
                                    return new Object[] { Boolean.TRUE };
                        }
                    return new Object[] { Boolean.FALSE };
                }
            }
        }
        
        return null;
    }
    
    /**
     * Converts a setting string to an integer
     *
     * @param   setting  The setting as a string
     * @return           The setting as an integer
     */
    private int getSetting(final String setting)
    {
        if (setting.equals( "visible"))        return -1;
        if (setting.equals( "visible=true"))   return -1;
        if (setting.equals( "visible=false"))  return  1;
        if (setting.equals(  "hidden"))        return  1;
        if (setting.equals(  "hidden=true"))   return  1;
        if (setting.equals(  "hidden=false"))  return -1;
        if (setting.equals( "enabled"))        return -2;
        if (setting.equals( "enabled=true"))   return -2;
        if (setting.equals( "enabled=false"))  return  2;
        if (setting.equals("disabled"))        return  2;
        if (setting.equals("disabled=true"))   return  2;
        if (setting.equals("disabled=false"))  return -2;
        if (setting.equals("selected"))        return  3;
        if (setting.equals("selected=true"))   return  3;
        if (setting.equals("selected=false"))  return -3;
        return 0;
    }
    
    /**
     * Applys a setting to a component
     *
     * @param  component  The component
     * @param  setting    The setting
     */
    private void applySetting(final Component component, final int setting)
    {
        switch (setting)
        {
            case  1:  component.setVisible(false);  break;
            case -1:  component.setVisible(true);   break;
            case  2:  component.setEnabled(false);  break;
            case -2:  component.setEnabled(true);   break;
        }
    }
    
    /**
     * Querys a setting from a component
     *
     * @param  component  The component
     * @param  setting    The setting
     */
    private boolean querySetting(final Component component, final int setting)
    {
        switch (setting)
        {
            case  1:  return !component.isVisible();
            case -1:  return  component.isVisible();
            case  2:  return !component.isEnabled();
            case -2:  return  component.isEnabled();
            case  3:  return component instanceof AbstractButton && !((AbstractButton)component).isSelected();
            case -3:  return component instanceof AbstractButton &&  ((AbstractButton)component).isSelected();
        }
        return false;
    }
    
    /**
     * Parses an item
     *
     * @param   item        The item to parse
     * @param   menuItems   The menu items
     * @param   parameters  Invocation parameters
     * @return              The item parsed
     */
    private Object parseItem(final String item, final HashMap<String, WeakReference<Component>> menuItems, final String... parameters)
    {
        String s = item;
        if (s.startsWith("("))
            return exec(s.substring(1, s.length() - 1), menuItems, parameters);
        if (s.startsWith("$"))
        {
            if (s.equals("$$"))
            {
                final ArrayList<Object> rc = new ArrayList<Object>();
                for (final String parameter : parameters)
                    rc.add(parseItem(parameter, menuItems, parameters));
                return rc;
            }
            
            s = parameters[Integer.parseInt(s.substring(1))];
        }
        else if (s.startsWith("%"))
            return this.variables.get(s.substring(1));
        
        if (s.startsWith("\"") && s.endsWith("\""))
            return s.substring(1, s.length() - 1).replace("\"\"", "\"").replace("\\\\", "\\");
        return (s.equals("true") || s.equals("1")) ? Boolean.TRUE : Boolean.FALSE;
    }
    
    /**
     * Creates a list of a partial code
     *
     * @param   lcode  The partial code
     * @return         The list items
     */
    private ArrayList<String> list(final String lcode)
    {
        final ArrayList<String> rc = new ArrayList<String>();
        final String p = lcode.endsWith(";") ? lcode : (lcode + ';');
        
        int start = 0;
        boolean str = false;
        int brackets = 0;
        for (int i = 0, n = p.length(); i < n; i++)
        {
            final char c = p.charAt(i);
            if (str)
            {
                if (c == '"')
                    str = false;
            }
            else if (c == '(')
                brackets++;
            else if (c == ')')
                brackets--;
            else if ((c == ';') && (brackets == 0))
            {
                start = i + 1;
                rc.add(p.substring(start, i - 1));
            }
        }
        
        return rc;
    }
    
}


