/*
 * Created by IntelliJ IDEA.
 * User: administrator
 * Date: Oct 31, 2001
 * Time: 2:11:33 PM
 * To change template for new class use 
 * Code Style | Class Templates options (Tools | IDE Options).
 */
package ASSET.Util.XMLFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.jdom.Element;

import ASSET.Util.RandomGenerator;

public class XMLChoice implements XMLOperation
{
  /***************************************************************
   *  member variables
   ***************************************************************/

  /** the list of values to choose from
   *
   */
  private HashMap<List<Element>, String> _myList;

  /** the element we will use for this permutation
   *
   */
  private List<Element> _currentVal;



  /***************************************************************
   *  constructor
   ***************************************************************/
  @SuppressWarnings("unchecked")
	public XMLChoice(final Element element)
  {
    this();

    // you know, get the stuff

    // have a fish around inside it

    final List vars = element.getChildren();

    final Iterator it = vars.iterator();
    while (it.hasNext())
    {
      final Element thisE = (Element) it.next();
      final String thisName = thisE.getAttribute("name").getValue();
      final List children = thisE.getChildren();

      final List<Element> duplicates = new Vector(0,1);

      if(children != null)
      {
        // keep track of the elements to be detached
        Vector toBeDetached = new Vector(0,1);

        // in this first pass, take copies of the children
        final Iterator iter = children.iterator();
        while (iter.hasNext())
        {
          final Element el = (Element) iter.next();
          duplicates.add((Element)el.clone());
          toBeDetached.add(el);
        }

        // now pass through again and detach all of the children
        Iterator iter2 = toBeDetached.iterator();
        while (iter2.hasNext())
        {
          Element el = (Element) iter2.next();
          el.detach();
        }

        _myList.put(duplicates, thisName);
      }
    }

    // stick in a random variable to start us off
    newPermutation();
  }

  private XMLChoice(XMLChoice other)
  {
    this();
  }

  private XMLChoice()
  {
    _myList = new HashMap<List<Element>, String>();
  }

  /***************************************************************
   *  member methods
   ***************************************************************/
  /** produce a new value for this operation
   *
   */
  public void newPermutation()
  {
    final int index = (int)(RandomGenerator.nextRandom() * _myList.size());

    final Iterator<List<Element>> it = _myList.keySet().iterator();

    for(int i=0;i<=index;i++)
    {
      _currentVal = it.next();
    }

    // done

  };

  /** return the human legible current value of this permutation
   *
   */
  public String getSimpleValue()
  {
    return (String) _myList.get(_currentVal) ;
  }

  /** return the current value of this permutation
   *
   */
  public String getValue()
  {
    String res = "";
    final List<Element> ls = _currentVal;
    final Iterator<Element> it = ls.iterator();
    while (it.hasNext())
    {
      final Object o = (Object) it.next();
      res += o.toString();
    }

    return res;
  };


  /** return our object as a list
   *
   */
  public List<Element> getList()
  {
    return _currentVal;
  }


  /** clone this object
   *
   */
  public Object clone()
  {
    final XMLChoice res = new XMLChoice(this);
    return res;
  }


  /** merge ourselves with the supplied operation
   *
   */
  public void merge(XMLOperation other)
  {
  }


}
