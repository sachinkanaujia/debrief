package org.mwc.debrief.satc_interface;

import org.eclipse.jface.resource.ImageDescriptor;
import org.mwc.cmap.core.ui_support.CoreViewLabelProvider.ViewLabelImageHelper;
import org.mwc.debrief.satc_interface.data.SATC_Solution;
import org.mwc.debrief.satc_interface.data.wrappers.BMC_Wrapper;
import org.mwc.debrief.satc_interface.data.wrappers.ContributionWrapper;
import org.mwc.debrief.satc_interface.data.wrappers.StraightLegWrapper;

import MWC.GUI.Editable;

import com.planetmayo.debrief.satc.model.contributions.BaseContribution;
import com.planetmayo.debrief.satc.model.contributions.CourseForecastContribution;
import com.planetmayo.debrief.satc.model.contributions.RangeForecastContribution;
import com.planetmayo.debrief.satc.model.contributions.SpeedForecastContribution;

public class SATC_ImageHelper implements ViewLabelImageHelper
{

	public ImageDescriptor getImageFor(final Editable editable)
	{
		ImageDescriptor res = null;

		if (editable instanceof SATC_Solution)
			res = SATC_Interface_Activator.getImageDescriptor("icons/calculator.gif");
		else if (editable instanceof StraightLegWrapper)
			res = SATC_Interface_Activator.getImageDescriptor("icons/leg.png");
		else if (editable instanceof BMC_Wrapper.MeasurementEditable)
			res = SATC_Interface_Activator.getImageDescriptor("icons/bearings.gif");
		else if (editable instanceof StraightLegWrapper)
			res = SATC_Interface_Activator.getImageDescriptor("icons/leg.png");
		else if (editable instanceof ContributionWrapper)
		{
			ContributionWrapper cw = (ContributionWrapper) editable;
			BaseContribution cont = cw.getContribution();
			if (cont instanceof CourseForecastContribution)
				res = SATC_Interface_Activator
						.getImageDescriptor("icons/direction.png");
			else if (cont instanceof SpeedForecastContribution)
				res = SATC_Interface_Activator.getImageDescriptor("icons/speed.png");
			else if (cont instanceof RangeForecastContribution)
				res = SATC_Interface_Activator.getImageDescriptor("icons/range.png");
		}
		return res;
	}

}