package org.debrief.limpet_integration.data;

import info.limpet.ICollection;
import info.limpet.IStoreGroup;
import info.limpet.IStoreItem;
import info.limpet.data.store.InMemoryStore;
import info.limpet.ui.data_provider.data.LimpetWrapper;
import info.limpet.ui.data_provider.data.ReflectivePropertySource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import Debrief.Wrappers.Measurements.SupplementalDataBlock;
import MWC.GUI.CanvasType;
import MWC.GUI.Editable;
import MWC.GUI.Editable2;
import MWC.GUI.Plottable;
import MWC.GenericData.WorldArea;
import MWC.GenericData.WorldLocation;

public class StoreWrapper implements SupplementalDataBlock, Editable2,
    LimpetWrapper
{

  private final InMemoryStore _store;

  public StoreWrapper(InMemoryStore store)
  {
    _store = store;
  }

  @Override
  public void paint(CanvasType dest)
  {
    // TODO Auto-generated method stub

  }

  @Override
  public WorldArea getBounds()
  {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public boolean getVisible()
  {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public void setVisible(boolean val)
  {
    // TODO Auto-generated method stub

  }

  @Override
  public double rangeFrom(WorldLocation other)
  {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public String getName()
  {
    return "Measurements";
  }

  @Override
  public boolean hasEditor()
  {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public EditorType getInfo()
  {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public int compareTo(Plottable o)
  {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public void setWrapper(Object parent)
  {
    // TODO Auto-generated method stub

  }


  private ArrayList<Editable> getElementsFor(IStoreGroup store)
  {
    ArrayList<Editable> res = new ArrayList<Editable>();
    Iterator<IStoreItem> iter = store.iterator();
    while (iter.hasNext())
    {
      IStoreItem storeItem = (IStoreItem) iter.next();
      final Editable thisE;

      if (storeItem instanceof IStoreGroup)
      {
        IStoreGroup group = (IStoreGroup) storeItem;
        thisE = new GroupWrapper(group);
      }
      else if (storeItem instanceof ICollection)
      {
        thisE = new ItemWrapper(storeItem);
      }
      else
      {
        thisE = null;
      }

      res.add(thisE);
    }

    return res;
  }

  protected class GroupWrapper extends ReflectivePropertySource implements
      Editable2, LimpetWrapper
  {

    private IStoreGroup _group;

    public GroupWrapper(IStoreGroup group)
    {
      super(group);
      _group = group;
    }

    @Override
    public String getName()
    {
      return _group.getName();
    }

    @Override
    public Object getSubject()
    {
      return _group;
    }

    @Override
    public LimpetWrapper getParent()
    {
      return null;
    }

    @Override
    public boolean hasEditor()
    {
      // TODO Auto-generated method stub
      return false;
    }

    @Override
    public EditorType getInfo()
    {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public boolean hasChildren()
    {
      return _group.size()>0;
    }

    @Override
    public Collection<Editable> getChildren()
    {
      return getElementsFor(_group);
    }

    @Override
    public Object getValue(Object descriptor)
    {
      return super.getPropertyValue(descriptor);
    }

    @Override
    public void setValue(Object id, Object theValue)
    {
      super.setPropertyValue(id,  theValue);
    }


  }

  protected static class ItemWrapper extends ReflectivePropertySource implements
      Editable2, LimpetWrapper
  {

    private IStoreItem _item;

    public ItemWrapper(IStoreItem storeItem)
    {
      super(storeItem);
      _item = storeItem;
    }

    @Override
    public String getName()
    {
      return _item.getName();
    }

    @Override
    public String toString()
    {
      return getName();
    }

    @Override
    public boolean hasEditor()
    {
      return true;
    }

    @Override
    public EditorType getInfo()
    {
      return null;
    }

    @Override
    public Object getSubject()
    {
      return _item;
    }

    @Override
    public LimpetWrapper getParent()
    {
      // TODO Auto-generated method stub
      return null;
    }
    

    @Override
    public Object getValue(Object descriptor)
    {
      return super.getPropertyValue(descriptor);
    }

    @Override
    public boolean hasChildren()
    {
      // TODO Auto-generated method stub
      return false;
    }

    @Override
    public Collection<Editable> getChildren()
    {
      // TODO Auto-generated method stub
      return null;
    }
    
    @Override
    public void setValue(Object id, Object theValue)
    {
      super.setPropertyValue(id,  theValue);
    }
//

  }

  @Override
  public LimpetWrapper getParent()
  {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Object getSubject()
  {
    return _store;
  }

  @Override
  public boolean hasChildren()
  {
    return _store.size() > 0;
  }

  @Override
  public Collection<Editable> getChildren()
  {
    return getElementsFor(_store);
  }

  @Override
  public Object getValue(Object descriptor)
  {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void setValue(Object id, Object theValue)
  {
    // TODO Auto-generated method stub
    
  }

}
