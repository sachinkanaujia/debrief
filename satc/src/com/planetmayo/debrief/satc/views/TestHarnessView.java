package com.planetmayo.debrief.satc.views;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.osgi.framework.Bundle;

import com.planetmayo.debrief.satc.SATC_Activator;
import com.planetmayo.debrief.satc.model.contributions.BearingMeasurementContribution;
import com.planetmayo.debrief.satc.model.contributions.BearingMeasurementContributionTest;
import com.planetmayo.debrief.satc.model.contributions.SpeedForecastContribution;
import com.planetmayo.debrief.satc.model.generator.TrackGenerator;

/**
 * This sample class demonstrates how to plug-in a new workbench view. The view
 * shows data obtained from the model. The sample creates a dummy model on the
 * fly, but a real implementation would connect to the model available either in
 * this or another plug-in (e.g. the workspace). The view is connected to the
 * model using a content provider.
 * <p>
 * The view uses a label provider to define how model objects should be
 * presented in the view. Each view can present the same model objects using
 * different labels and icons, if needed. Alternatively, a single label provider
 * can be shared between views in order to ensure that objects of the same type
 * are presented in the same way everywhere.
 * <p>
 */

public class TestHarnessView extends ViewPart
{

	class NameSorter extends ViewerSorter
	{
	}

	class ViewContentProvider implements IStructuredContentProvider
	{
		@Override
		public void dispose()
		{
		}

		@Override
		public Object[] getElements(Object parent)
		{
			return new String[]
			{ "One", "Two", "Three" };
		}

		@Override
		public void inputChanged(Viewer v, Object oldInput, Object newInput)
		{
		}
	}

	class ViewLabelProvider extends LabelProvider implements ITableLabelProvider
	{
		@Override
		public Image getColumnImage(Object obj, int index)
		{
			return getImage(obj);
		}

		@Override
		public String getColumnText(Object obj, int index)
		{
			return getText(obj);
		}

		@Override
		public Image getImage(Object obj)
		{
			return PlatformUI.getWorkbench().getSharedImages()
					.getImage(ISharedImages.IMG_OBJ_ELEMENT);
		}
	}

	/**
	 * The ID of the view as specified by the extension.
	 */
	public static final String ID = "com.planetmayo.debrief.satc.views.SampleView";
	private TableViewer viewer;

	/*
	 * The content provider class is responsible for providing objects to the
	 * view. It can wrap existing objects in adapters or simply return objects
	 * as-is. These objects may be sensitive to the current input of the view, or
	 * ignore it and always show the same content (like Task List, for example).
	 */

	private Action _restartAction;
	private Action _stepAction;
	private Action _playAction;
	private Action _populateAction;

	private TrackGenerator _generator;

	/**
	 * The constructor.
	 */
	public TestHarnessView()
	{
	}

	private void contributeToActionBars()
	{
		IActionBars bars = getViewSite().getActionBars();
		fillLocalPullDown(bars.getMenuManager());
		fillLocalToolBar(bars.getToolBarManager());
	}

	/**
	 * This is a callback that will allow us to create the viewer and initialize
	 * it.
	 */
	@Override
	public void createPartControl(Composite parent)
	{
		viewer = new TableViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		viewer.setContentProvider(new ViewContentProvider());
		viewer.setLabelProvider(new ViewLabelProvider());
		viewer.setSorter(new NameSorter());
		viewer.setInput(getViewSite());

		// Create the help context id for the viewer's control
		PlatformUI.getWorkbench().getHelpSystem()
				.setHelp(viewer.getControl(), "com.planetmayo.debrief.satc.viewer");
		makeActions();
		hookContextMenu();
		contributeToActionBars();

		// hey, see if there's a track generator to listen to
		_generator = SATC_Activator.getDefault().getMockEngine().getGenerator();

		if (_generator == null)
			SATC_Activator.log(Status.ERROR, "Failed to find generator", null);
		else
			SATC_Activator.log(Status.INFO, "Found generator:", null);

		// did it work?
		if (_generator != null)
		{
			// ok, we can enable our buttons
			_populateAction.setEnabled(true);
			_restartAction.setEnabled(true);
			_stepAction.setEnabled(true);
			_playAction.setEnabled(true);
		}
	}

	private void fillContextMenu(IMenuManager manager)
	{
	}

	private void fillLocalPullDown(IMenuManager manager)
	{
	}

	private void fillLocalToolBar(IToolBarManager manager)
	{
		manager.add(_populateAction);
		manager.add(_restartAction);
		manager.add(_stepAction);
		manager.add(_playAction);
	}

	private void hookContextMenu()
	{
		MenuManager menuMgr = new MenuManager("#PopupMenu");
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener()
		{
			@Override
			public void menuAboutToShow(IMenuManager manager)
			{
				TestHarnessView.this.fillContextMenu(manager);
			}
		});
		Menu menu = menuMgr.createContextMenu(viewer.getControl());
		viewer.getControl().setMenu(menu);
		getSite().registerContextMenu(menuMgr, viewer);
	}

	private void makeActions()
	{
		_populateAction = new Action()
		{
			@Override
			public void run()
			{
				BearingMeasurementContribution bmc = new BearingMeasurementContribution();
				Bundle bundle = Platform.getBundle(SATC_Activator.PLUGIN_ID);
				URL fileURL = bundle
						.getEntry(BearingMeasurementContributionTest.THE_PATH);
				FileInputStream input;
				try
				{
					input = new FileInputStream(new File(FileLocator.resolve(fileURL)
							.toURI()));
					bmc.loadFrom(input);
					_generator.addContribution(bmc);
				}
				catch (Exception e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				SpeedForecastContribution speed = new SpeedForecastContribution();
				speed.setMinSpeed(12);
				speed.setMaxSpeed(43);
				_generator.addContribution(speed);

			}
		};
		_populateAction.setText("Populate");
		_populateAction.setToolTipText("Action 1 tooltip");

		_restartAction = new Action()
		{
			@Override
			public void run()
			{
				_generator.restart();
			}
		};
		_restartAction.setText("Restart");
		_restartAction.setToolTipText("Action 1 tooltip");

		_stepAction = new Action()
		{
			@Override
			public void run()
			{
				_generator.step();
			}
		};
		_stepAction.setText("Step");
		_stepAction.setToolTipText("Action 2 tooltip");

		_playAction = new Action()
		{
			@Override
			public void run()
			{
				_generator.step();
			}
		};
		_playAction.setText("Play");
		_playAction.setToolTipText("Action 2 tooltip");

	}

	/**
	 * Passing the focus request to the viewer's control.
	 */
	@Override
	public void setFocus()
	{
		viewer.getControl().setFocus();
	}

}