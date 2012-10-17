package com.planetmayo.debrief.satc_rcp.ui.contributions;

import java.beans.PropertyChangeListener;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.BeansObservables;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.swt.widgets.Composite;

import com.planetmayo.debrief.satc.model.contributions.BaseContribution;
import com.planetmayo.debrief.satc.model.contributions.CourseForecastContribution;
import com.planetmayo.debrief.satc_rcp.ui.PrefixSuffixLabelConverter;
import com.planetmayo.debrief.satc_rcp.ui.UIUtils;

public class CourseContributionView extends AnalystContributionView
{

	private BaseContribution contribution;
	private DataBindingContext context;
	private PropertyChangeListener titleChangeListener;

	public CourseContributionView(Composite parent, BaseContribution contribution)
	{
		super(parent);
		this.contribution = contribution;
		initUI();
	}

	// don't use inheritance here, because of different nature and although code
	// looks very similar it may be headache in future
	@Override
	protected void bindValues()
	{
		context = new DataBindingContext();

		PrefixSuffixLabelConverter labelsConverter = new PrefixSuffixLabelConverter(
				Object.class, " degs");
		bindCommonHeaderWidgets(context, contribution, labelsConverter);
		bindCommonDates(context, contribution);

		IObservableValue estimateValue = BeansObservables.observeValue(
				contribution, BaseContribution.ESTIMATE);
		IObservableValue estimateLabel = WidgetProperties.text().observe(
				this.estimateLabel);
		context.bindValue(estimateLabel, estimateValue, null,
				UIUtils.converterStrategy(labelsConverter));

		IObservableValue minCourseValue = BeansObservables.observeValue(
				contribution, CourseForecastContribution.MIN_COURSE);
		IObservableValue minCourseSlider = WidgetProperties.selection().observe(
				minSlider);
		IObservableValue minCourseLabel = WidgetProperties.text().observe(minLabel);
		IObservableValue esimateSliderMin = WidgetProperties.minimum().observe(
				estimateSlider);
		IObservableValue maxSliderMin = WidgetProperties.minimum().observe(
				maxSlider);
		context.bindValue(minCourseSlider, minCourseValue);
		context.bindValue(esimateSliderMin, minCourseValue);
		context.bindValue(maxSliderMin, minCourseValue);
		context.bindValue(minCourseLabel, minCourseValue, null, UIUtils
				.converterStrategy(new PrefixSuffixLabelConverter(int.class, "min: ",
						" degs")));

		IObservableValue maxCourseValue = BeansObservables.observeValue(
				contribution, CourseForecastContribution.MAX_COURSE);
		IObservableValue maxCourseSlider = WidgetProperties.selection().observe(
				maxSlider);
		IObservableValue maxCourseLabel = WidgetProperties.text().observe(maxLabel);
		IObservableValue esimateSliderMax = WidgetProperties.maximum().observe(
				estimateSlider);
		IObservableValue minSliderMax = WidgetProperties.maximum().observe(
				minSlider);
		context.bindValue(maxCourseSlider, maxCourseValue);
		context.bindValue(esimateSliderMax, maxCourseValue);
		context.bindValue(minSliderMax, maxCourseValue);
		context.bindValue(maxCourseLabel, maxCourseValue, null, UIUtils
				.converterStrategy(new PrefixSuffixLabelConverter(int.class, "max: ",
						" degs")));

		IObservableValue estimateSliderValue = WidgetProperties.selection()
				.observe(estimateSlider);
		IObservableValue estimateCourseDetailsLabel = WidgetProperties.text()
				.observe(estimateDetailsLabel);
		context.bindValue(estimateSliderValue, estimateValue);
		context.bindValue(estimateCourseDetailsLabel, estimateValue, null, UIUtils
				.converterStrategy(new PrefixSuffixLabelConverter(int.class,
						"Estimate: ", " degs")));

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
				"Course Forecast - ");

		minSlider.setMinimum(0);
		minSlider.setMaximum(360);
		maxSlider.setMinimum(0);
		maxSlider.setMaximum(360);
		estimateSlider.setMinimum(0);
		estimateSlider.setMaximum(360);
	}
}
