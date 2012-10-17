package com.planetmayo.debrief.satc_rcp.ui.contributions;

import java.beans.PropertyChangeListener;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.BeansObservables;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.swt.widgets.Composite;

import com.planetmayo.debrief.satc.model.contributions.BaseContribution;
import com.planetmayo.debrief.satc.model.contributions.SpeedForecastContribution;
import com.planetmayo.debrief.satc_rcp.ui.PrefixSuffixLabelConverter;
import com.planetmayo.debrief.satc_rcp.ui.UIUtils;

public class SpeedContributionView extends AnalystContributionView
{

	private BaseContribution contribution;
	private DataBindingContext context;
	private PropertyChangeListener titleChangeListener;

	public SpeedContributionView(Composite parent, BaseContribution contribution)
	{
		super(parent);
		this.contribution = contribution;
		initUI();
	}

	@Override
	protected void bindValues()
	{
		context = new DataBindingContext();

		PrefixSuffixLabelConverter labelsConverter = new PrefixSuffixLabelConverter(
				Object.class, " kts");
		bindCommonHeaderWidgets(context, contribution, labelsConverter);
		bindCommonDates(context, contribution);

		IObservableValue estimateValue = BeansObservables.observeValue(
				contribution, BaseContribution.ESTIMATE);
		IObservableValue estimateLabel = WidgetProperties.text().observe(
				this.estimateLabel);
		context.bindValue(estimateLabel, estimateValue, null,
				UIUtils.converterStrategy(labelsConverter));

		IObservableValue minSpeedValue = BeansObservables.observeValue(
				contribution, SpeedForecastContribution.MIN_SPEED);
		IObservableValue minSpeedSlider = WidgetProperties.selection().observe(
				minSlider);
		IObservableValue minSpeedLabel = WidgetProperties.text().observe(minLabel);
		IObservableValue esimateSliderMin = WidgetProperties.minimum().observe(
				estimateSlider);
		IObservableValue maxSliderMin = WidgetProperties.minimum().observe(
				maxSlider);
		context.bindValue(minSpeedSlider, minSpeedValue);
		context.bindValue(esimateSliderMin, minSpeedValue);
		context.bindValue(maxSliderMin, minSpeedValue);
		context.bindValue(minSpeedLabel, minSpeedValue, null, UIUtils
				.converterStrategy(new PrefixSuffixLabelConverter(double.class,
						"min: ", " kts")));

		IObservableValue maxSpeedValue = BeansObservables.observeValue(
				contribution, SpeedForecastContribution.MAX_SPEED);
		IObservableValue maxSpeedSlider = WidgetProperties.selection().observe(
				maxSlider);
		IObservableValue maxSpeedLabel = WidgetProperties.text().observe(maxLabel);
		IObservableValue esimateSliderMax = WidgetProperties.maximum().observe(
				estimateSlider);
		IObservableValue minSliderMax = WidgetProperties.maximum().observe(
				minSlider);
		context.bindValue(maxSpeedSlider, maxSpeedValue);
		context.bindValue(esimateSliderMax, maxSpeedValue);
		context.bindValue(minSliderMax, maxSpeedValue);
		context.bindValue(maxSpeedLabel, maxSpeedValue, null, UIUtils
				.converterStrategy(new PrefixSuffixLabelConverter(double.class,
						"max: ", " kts")));

		IObservableValue estimateSliderValue = WidgetProperties.selection()
				.observe(estimateSlider);
		IObservableValue estimateSpeedDetailsLabel = WidgetProperties.text()
				.observe(estimateDetailsLabel);
		context.bindValue(estimateSliderValue, estimateValue);
		context.bindValue(estimateSpeedDetailsLabel, estimateValue, null, UIUtils
				.converterStrategy(new PrefixSuffixLabelConverter(double.class,
						"Estimate: ", " kts")));
	}

	@Override
	public void dispose()
	{
		super.dispose();
		contribution.removePropertyChangeListener(BaseContribution.NAME,
				titleChangeListener);
		context.dispose();
	}

	@Override
	protected void initializeWidgets()
	{
		titleChangeListener = attachTitleChangeListener(contribution,
				"Speed Forecast - ");
	}
}
