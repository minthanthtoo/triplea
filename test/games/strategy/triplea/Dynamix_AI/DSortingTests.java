/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package games.strategy.triplea.Dynamix_AI;

import games.strategy.engine.data.GameData;
import games.strategy.triplea.xml.LoadGameUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import junit.framework.TestCase;

/**
 *
 * @author Stephen
 */
public class DSortingTests extends TestCase
{
    private GameData m_data;

    @Override
    protected void setUp() throws Exception
    {
        m_data = LoadGameUtil.loadGame("Great Lakes War", "Great Lakes War v1.4.xml");
    }

    @Override
    protected void tearDown() throws Exception
    {
        m_data = null;
    }

    public void testDSorting()
    {
        List<String> origItems = Arrays.asList("b", "a", "c", "g", "h", "d", "f", "e");
        List<String> items_Ascending = Arrays.asList("a", "b", "c", "d", "e", "f", "g", "h");
        List<String> items_Descending = Arrays.asList("h", "g", "f", "e", "d", "c", "b", "a");
        List<String> vals;

        //Positive integer testing
        HashMap<String, Number> map = new HashMap<String, Number>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        map.put("d", 4);
        map.put("e", 5);
        map.put("f", 6);
        map.put("g", 7);
        map.put("h", 8);

        vals = new ArrayList<String>(origItems);
        vals = DSorting.SortListByScores_HashMap_A(vals, map);
        assertEquals(vals, items_Ascending);

        vals = new ArrayList<String>(origItems);
        vals = DSorting.SortListByScores_HashMap_D(vals, map);
        assertEquals(vals, items_Descending);



        //Negative integer testing
        map = new HashMap<String, Number>();
        map.put("a", -8);
        map.put("b", -7);
        map.put("c", -6);
        map.put("d", -5);
        map.put("e", -4);
        map.put("f", -3);
        map.put("g", -2);
        map.put("h", -1);

        vals = new ArrayList<String>(origItems);
        vals = DSorting.SortListByScores_HashMap_A(vals, map);
        assertEquals(vals, items_Ascending);

        vals = new ArrayList<String>(origItems);
        vals = DSorting.SortListByScores_HashMap_D(vals, map);
        assertEquals(vals, items_Descending);



        //Assorted integer testing
        map = new HashMap<String, Number>();
        map.put("a", -39);
        map.put("b", -29);
        map.put("c", -15);
        map.put("d", -9);
        map.put("e", -8);
        map.put("f", -7);
        map.put("g", -6);
        map.put("h", 12);

        vals = new ArrayList<String>(origItems);
        vals = DSorting.SortListByScores_HashMap_A(vals, map);
        assertEquals(vals, items_Ascending);

        vals = new ArrayList<String>(origItems);
        vals = DSorting.SortListByScores_HashMap_D(vals, map);
        assertEquals(vals, items_Descending);



        //Assorted double testing
        map = new HashMap<String, Number>();
        map.put("a", -19.23D);
        map.put("b", -19.22D);
        map.put("c", -16.5D);
        map.put("d", -2.45D);
        map.put("e", -0.0D);
        map.put("f", 4.2);
        map.put("g", 9.99);
        map.put("h", 12.8);

        vals = new ArrayList<String>(origItems);
        vals = DSorting.SortListByScores_HashMap_A(vals, map);
        assertEquals(vals, items_Ascending);

        vals = new ArrayList<String>(origItems);
        vals = DSorting.SortListByScores_HashMap_D(vals, map);
        assertEquals(vals, items_Descending);
    }
}