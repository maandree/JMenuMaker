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


/**
 * Menu item value update listener
 *
 * @version  1.0
 * @author   Mattias Andrée, <a href="mailto:maandree@kth.se">maandree@kth.se</a>
 */
public interface UpdateListener
{
    /**
     * Invoked when a valueless item is clicked
     *
     * @param  id  The item's ID
     */
    void itemClicked(String id);
    
    /**
     * Invoked on value update
     *
     * @param  id     The item's ID
     * @param  value  The item's new value
     */
    void valueUpdated(String id, String value);
    
    /**
     * Invoked on value update
     *
     * @param  id     The item's ID
     * @param  value  The item's new value
     */
    void valueUpdated(String id, double value);
    
    /**
     * Invoked on value update
     *
     * @param  id     The item's ID
     * @param  value  The item's new value
     */
    void valueUpdated(String id, long value);
    
    /**
     * Invoked on value update
     *
     * @param  id     The item's ID
     * @param  value  The item's new value
     */
    void valueUpdated(String id, int value);
    
    /**
     * Invoked on value update
     *
     * @param  id     The item's ID
     * @param  value  The item's new value
     */
    void valueUpdated(String id, boolean value);
    
}


