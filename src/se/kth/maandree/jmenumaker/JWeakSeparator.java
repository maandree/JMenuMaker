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
import java.lang.ref.*;
import java.io.Serializable;


/**
 * Weak (or shy) {@link JSeparator}
 *
 * @version  1.0
 * @author   Mattias Andrée, <a href="mailto:maandree@kth.se">maandree@kth.se</a>
 */
public class JWeakSeparator extends JSeparator
{
    /**
     * Desired by {@link Serializable}
     */
    private static final long serialVersionUID = 1;
    
    
    
    /**
     * Constructor
     *
     * @param  menuBar  The menu bar containing the separator
     */
    public JWeakSeparator(final JMenuBar menuBar)
    {
        ArrayList<WeakReference<JWeakSeparator>> list = instances.get(menuBar);
        if (list == null)
        {
            list = new ArrayList<WeakReference<JWeakSeparator>>();
            instances.put(menuBar, list);
        }
        
        list.add(new WeakReference<JWeakSeparator>(this));
    }
    
    
    
    /**
     * Hash table of lists of instances
     */
    private static WeakHashMap<JMenuBar, ArrayList<WeakReference<JWeakSeparator>>> instances =
               new WeakHashMap<JMenuBar, ArrayList<WeakReference<JWeakSeparator>>>();
    
    
    
    /**
     * Call this method when you suspect the menu-system's content has been changed,
     * so the visibility of the weak separators can be modified.
     *
     * @param  menuBar  The menu bar which content has been changed
     */
    public static void update(final JMenuBar menuBar)
    {
        final ArrayList<WeakReference<JWeakSeparator>> list = instances.get(menuBar);
        JWeakSeparator separator;
        
        final HashSet<JPopupMenu> hashPopups = new HashSet<JPopupMenu>();
        final ArrayList<JPopupMenu> listPopups = new ArrayList<JPopupMenu>();
        
        if (list != null)
            for (int i = 0; i < list.size(); i++)
                if ((separator = list.get(i).get()) == null)
                    list.remove(i--);
                else
                    if (hashPopups.contains((JPopupMenu)(separator.getParent())) == false)
                    {
                        hashPopups.add((JPopupMenu)(separator.getParent()));
                        listPopups.add((JPopupMenu)(separator.getParent()));
                    }
        
        for (final JPopupMenu parent : listPopups)
        {
            boolean needed = false;
            Component last = null;
            
            for (final Component child : parent.getComponents())
            {
                if (child.isVisible() == false)
                    continue;
                
                if (child instanceof JWeakSeparator)
                {
                    child.setVisible(needed);
                    needed = false;
                }
                else
                    needed = (child instanceof JSeparator) == false;
                
                if (child.isVisible())
                    last = child;
            }
            
            if (last != null)
                if (last instanceof JWeakSeparator)
                    last.setVisible(false);
        }
    }
    
}


