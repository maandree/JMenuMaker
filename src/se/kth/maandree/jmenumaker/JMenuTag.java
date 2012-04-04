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
 * Menu tag, item; an invisible item which handles dynamic menus
 *
 * @version  1.0
 * @author   Mattias Andrée, <a href="mailto:maandree@kth.se">maandree@kth.se</a>
 */
public final class JMenuTag extends JSeparator
{
    /**
     * Desired by {@link Serializable}
     */
    private static final long serialVersionUID = 1;
    
    
    
    /**
     * Weak multiton constructor
     */
    protected JMenuTag()
    {
        this.setVisible(false);
    }
    
    /**
     * Gets, and may create, an instance of the class
     *
     * @param   tag  The instance's unique tag
     * @return       The instance
     */
    public static JMenuTag getInstance(final String tag)
    {
        synchronized (JMenuTag.class)
        {
            WeakReference<JMenuTag> ref = instances.get(tag);
            if ((ref == null) || (ref.get() == null))
            {
                JMenuTag menu;
                instances.put(tag, new WeakReference<JMenuTag>(menu = new JMenuTag()));
                return menu;
            }
            return ref.get();
        }
    }
    
    /**
     * The instances
     */
    private static final HashMap<String, WeakReference<JMenuTag>> instances = new HashMap<String, WeakReference<JMenuTag>>();
    
    
    
    /**
     * The tag's empty indicator item
     */
    private Component emptyIndicator = null;
    
    /**
     * The tag's alive (non-empty) indicator item
     */
    private Component aliveIndicator = null;
    
    /**
     * The tag's items
     */
    private Component[] components = {};        
    
    /**
     * The number of items in the menu handled by the tag
     */
    private int handledItems = 0;
    
    
    
    /**
     * Sets the tag's empty indicator item
     *
     * @param  item  The indicator
     */
    public void setEmptyIndicator(final Component item)
    {
        this.emptyIndicator = item;
        update();
    }
    
    /**
     * Gets the tag's empty indicator item
     *
     * @return  The indicator
     */
    public Component getEmptyIndicator()
    {
        return this.emptyIndicator;
    }
    
    /**
     * Sets the tag's alive (non-empty) indicator item
     *
     * @param  item  The indicator
     */
    public void setAliveIndicator(final Component item)
    {
        this.aliveIndicator = item;
        update();
    }
    
    /**
     * Gets the tag's alive (non-empty) indicator item
     *
     * @return  The indicator
     */
    public Component getAliveIndicator()
    {
        return this.aliveIndicator;
    }
    
    
    /**
     * Sets the tag's items
     *
     * @param  items  The component's items
     */
    public void setItems(final Component... items)
    {
        this.components = items == null ? new Component[0] : items;
        update();
    }
    
    /**
     * Gets the tag's items
     *
     * @return  The component's items
     */
    public Component[] getItems()
    {
        return this.components;
    }
    
    /**
     * Call this method if you have made a component visible or invisible
     */
    public void update()
    {
        final JPopupMenu parent = (JPopupMenu)(this.getParent());
        
        int index = 1;
        for (final Component component : parent.getComponents())
        {
            if (component == this)
                break;
            index++;
        }
        
        for (int i = 0; i < handledItems; i++)
            parent.remove(i);
        
        boolean empty = true;
        for (final Component component : this.components)
        {
            if (empty && component.isVisible())
                empty = false;
            parent.add(component, index++);
        }
        
        this.handledItems = this.components.length;
        
        if (empty && (this.emptyIndicator != null))
        {
            parent.add(this.emptyIndicator, index++);
            handledItems++;
        }
        
        if (this.aliveIndicator != null)
            this.aliveIndicator.setVisible(!empty);
    }
    
}


