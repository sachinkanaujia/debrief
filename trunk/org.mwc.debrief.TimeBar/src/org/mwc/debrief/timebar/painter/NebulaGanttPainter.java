package org.mwc.debrief.timebar.painter;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.jface.util.SafeRunnable;
import org.eclipse.nebula.widgets.ganttchart.AdvancedTooltip;
import org.eclipse.nebula.widgets.ganttchart.DefaultSettings;
import org.eclipse.nebula.widgets.ganttchart.GanttChart;
import org.eclipse.nebula.widgets.ganttchart.GanttCheckpoint;
import org.eclipse.nebula.widgets.ganttchart.GanttComposite;
import org.eclipse.nebula.widgets.ganttchart.GanttEvent;
import org.eclipse.nebula.widgets.ganttchart.GanttEventListenerAdapter;
import org.eclipse.nebula.widgets.ganttchart.GanttGroup;
import org.eclipse.nebula.widgets.ganttchart.GanttImage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.mwc.debrief.timebar.Activator;
import org.mwc.debrief.timebar.model.IEventEntry;

import MWC.GUI.Editable;



public class NebulaGanttPainter implements ITimeBarsPainter, PropertyChangeListener
{
	GanttChart _chart;
	Map<GanttEvent, IEventEntry> _eventEntries = new HashMap<GanttEvent, IEventEntry>();	
	List<ITimeBarsPainterListener> _listeners = new ArrayList<ITimeBarsPainterListener>();
	GanttEvent _earliestEvent = null;
	GanttEvent _latestEvent = null;
	
	public NebulaGanttPainter(Composite parent)
	{
		_chart = new GanttChart(parent, SWT.MULTI, new GanttChartSettings());		
		
		_chart.getGanttComposite().addMouseListener(new MouseListener() {
			
			@Override
			public void mouseUp(MouseEvent e) {}
			
			@Override
			public void mouseDown(MouseEvent e) {}
			
			@Override
			public void mouseDoubleClick(MouseEvent e) 
			{
				//TODO: do not do anything if an event was double-clicked
				Date clickedAt = _chart.getGanttComposite().getDateAt(e.x).getTime();
				if (_earliestEvent.getStartDate().getTime().compareTo(clickedAt) > 0)
				{
					// it is too early
					return;
				}
				if (_latestEvent.getEndDate().getTime().compareTo(clickedAt) < 0)
				{
					//it is too late
					return;
				}
					
				chartDoubleClicked(clickedAt);				
			}
		});
		
		_chart.addGanttEventListener(new GanttEventListenerAdapter()
    	{
    		@Override
    		@SuppressWarnings("rawtypes")
    		public void eventSelected(GanttEvent event, List allSelectedEvents,
    				MouseEvent me) 
    		{
    			super.eventSelected(event, allSelectedEvents, me);
    			IEventEntry eventEntry = findEventEntry(event);
    			if (eventEntry != null)
    				eventIsSelected(eventEntry.getSource());
    		}  
    		
    		@Override
    		public void eventDoubleClicked(GanttEvent event, MouseEvent me) 
    		{    		
    			super.eventDoubleClicked(event, me);
    			IEventEntry eventEntry = findEventEntry(event);
    			if (eventEntry != null)
    				eventIsDoubleClicked(eventEntry.getSource());	
    		}
    	});
	
	}
	
	@Override
	public void drawBar(IEventEntry modelEntry) 
	{
		if (!modelEntry.isVisible())
			return;
			
		GanttEvent evt = new GanttEvent(_chart, modelEntry.getName(), 
				modelEntry.getStart(), modelEntry.getEnd(), 0);
		if (modelEntry.getColor() !=null)
			evt.setStatusColor(modelEntry.getColor());
		if (modelEntry.isBoldText())
			evt.setTextFont(new Font(null, "Arial", 12, SWT.BOLD ));
		
		if (modelEntry.getChildren().size() > 0)
		{
			evt.setScope(true);
			GanttGroup group = new GanttGroup(_chart);
			for (IEventEntry entry: modelEntry.getChildren())
			{
				GanttImage childEvt = new GanttImage(_chart, "", entry.getStart(), 
						Activator.getImageDescriptor("icons/dot.gif").createImage());
				childEvt.setAdvancedTooltip(new AdvancedTooltip("", entry.getName()));
				evt.addScopeEvent(childEvt);
				group.addEvent(childEvt);
				addEvent(childEvt, entry);
			}
		}
		
		addEvent(evt, modelEntry);		
	}

	@Override
	public void drawSpot(IEventEntry modelEntry) 
	{		
		if (!modelEntry.isVisible())
			return;
		
		GanttEvent evt = new GanttCheckpoint(_chart, modelEntry.getName(), modelEntry.getStart());	
		if (modelEntry.getColor() !=null)
			evt.setStatusColor(modelEntry.getColor());
		addEvent(evt, modelEntry);
	}
	
	private void addEvent(GanttEvent evt, IEventEntry modelEntry)
	{
		_eventEntries.put(evt, modelEntry);
		if (_earliestEvent == null)
		{
			_earliestEvent = evt;
		}
		else
		{
			if (evt.getStartDate().compareTo(_earliestEvent.getStartDate()) < 0)
			{
				_earliestEvent = evt;
			}
		}
		if(_latestEvent == null)
		{
			_latestEvent = evt;
		}
		else
		{
			if (evt.getEndDate().compareTo(_latestEvent.getEndDate()) > 0)
			{
				_latestEvent = evt;
			}
		}
	}

	@Override
	public void jumpToBegin() 
	{
		_chart.getGanttComposite().jumpToEarliestEvent();		
	}

	@Override
	public void clear() 
	{
		_chart.getGanttComposite().clearChart();		
	}

	@Override
	public void selectTimeBar(Editable editable) 
	{
		for (Map.Entry<GanttEvent, IEventEntry> entry: _eventEntries.entrySet())
		{
			if (entry.getValue().getSource().equals(editable))
			{
				_chart.getGanttComposite().jumpToEvent(entry.getKey(), true, SWT.LEFT);
				_chart.getGanttComposite().setSelection(entry.getKey());
				return;
			}
		}	
		
	}

	@Override
	public void setFocus() 
	{
		_chart.setFocus();		
	}
	
	public void zoomIn()
	{
		_chart.getGanttComposite().zoomIn();
	}
	
	@Override
	public void zoomOut()
	{
		_chart.getGanttComposite().zoomOut();
	}

	@Override
	public void drawDebriefTime(Date oldTime, Date currTime) 
	{
		final GanttComposite parent = _chart.getGanttComposite();
		
		final int curX = parent.getXForDate(currTime);
	    if (curX == -1) 
	    	return;     
	
	    eraseDebriefTime(oldTime);
	    
	    GC gc = new GC(parent);
		gc.setLineStyle(ITimeBarsPainter.TIME_LINE_STYLE);
		gc.setLineWidth(ITimeBarsPainter.TIME_LINE_WIDTH);
		gc.setForeground(ITimeBarsPainter.TIME_LINE_COLOR);
		    
		gc.drawRectangle(curX, 0, 1, 
		    		parent.getClientArea().height);
		gc.dispose();    
	   
	}
	
	private void eraseDebriefTime(Date timeVal)
	{
		if (timeVal == null)
			return;
		
		final GanttComposite parent = _chart.getGanttComposite();
		final int oldX = parent.getXForDate(timeVal);	   
	    
	    Display.getDefault().syncExec(new Runnable()
		{
			public void run()
			{				 	
				 parent.redraw(oldX-1, 0, 1,
			    			parent.getClientArea().height, false);				 		  
			}
		});
	}

	@Override
	public void addListener(ITimeBarsPainterListener listener) 
	{
		if (!_listeners.contains(listener))		
			_listeners.add(listener);		
	}

	@Override
	public void removeListener(ITimeBarsPainterListener listener) 
	{
		_listeners.remove(listener);		
	}	
	
	public void chartDoubleClicked(final Date clickedAt)
	{
		_chart.getGanttComposite().redraw();
		for (final ITimeBarsPainterListener l: _listeners) {
            SafeRunner.run(new SafeRunnable() {
                public void run() {
                    l.chartDoubleClicked(clickedAt);
                }
            });
		}
	}
	
	public void eventIsDoubleClicked(final Object eventEntry)
	{
		for (final ITimeBarsPainterListener l: _listeners) {
            SafeRunner.run(new SafeRunnable() {
                public void run() {
                    l.eventDoubleClicked(eventEntry);
                }
            });
		}
	}
	
	public void eventIsSelected(final Object eventEntry)
	{
		for (final ITimeBarsPainterListener l: _listeners) {
            SafeRunner.run(new SafeRunnable() {
                public void run() {
                    l.eventSelected(eventEntry);
                }
            });
		}
	}
	
	private IEventEntry findEventEntry(GanttEvent event)
	{
		for (Map.Entry<GanttEvent, IEventEntry> entry: _eventEntries.entrySet())
		{
			if (entry.getKey().equals(event))
			{
				return entry.getValue();
			}
		}
		return null;
	}
	
	private GanttEvent findGanttEvent(Object source)
	{
		for (Map.Entry<GanttEvent, IEventEntry> entry: _eventEntries.entrySet())
		{
			if (entry.getValue().getSource().equals(source))
			{
				return entry.getKey();
			}
		}
		return null;
	}
	

	@Override
	public void propertyChange(PropertyChangeEvent evt) 
	{		
		GanttEvent event = findGanttEvent(evt.getSource());
		if (event != null)
		{
			if (new Boolean(true).equals(evt.getNewValue()))
			{
				_chart.getGanttComposite().addEvent(event);
			}
			else
			{
				_chart.getGanttComposite().removeEvent(event);				
			}
		}		
	}

	@Override
	public void fitToWindow() 
	{
		final GanttComposite composite = _chart.getGanttComposite();
		Rectangle screenBounds = composite.getClientArea();
		Rectangle leftBounds = _earliestEvent.getActualBounds();
		Rectangle rightBounds = _latestEvent.getActualBounds();
		
		composite.jumpToEarliestEvent();
		//TODO: implement size fitting
//		if (rightBounds.x  > screenBounds.width)
//		{
//			
//			{
//				composite.zoomOut();
//				rightBounds = _latestEvent.getActualBounds();
//			}
//		}
		
	}

	@Override
	public void fitToSize() {
		// TODO Auto-generated method stub
		
	}	
	

}



class GanttChartSettings extends DefaultSettings
{
	@Override
	public boolean allowArrowKeysToScrollChart() 
	{
		return true;
	}
	
	@Override
	public boolean allowInfiniteHorizontalScrollBar() 
	{
		return false;
	}
	
	@Override
	public boolean drawEventsDownToTheHourAndMinute() 
	{	
		return true;
	}
	
	@Override
	public boolean drawFullPercentageBar() 
	{
		return false;
	}
	
	@Override
	public int getInitialZoomLevel() 
	{
		return ZOOM_HOURS_NORMAL;
	}
	
	@Override
	public String getTextDisplayFormat() 
	{
		return "#name#";
	}
	
	@Override
	public boolean showToolTips() 
	{
		return false;
	}
	
}	