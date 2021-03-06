/*
 *    Debrief - the Open Source Maritime Analysis Application
 *    http://debrief.info
 *
 *    (C) 2000-2014, PlanetMayo Ltd
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the Eclipse Public License v1.0
 *    (http://www.eclipse.org/legal/epl-v10.html)
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
 */
package ASSET.Util.XML.Sensors.Lookup;

import ASSET.Models.Sensor.Lookup.LookupSensor;
import ASSET.Models.Sensor.Lookup.RadarLookupSensor;
import MWC.GenericData.Duration;

/**
 * Created by IntelliJ IDEA.
 * User: Ian
 * Date: 03-Feb-2004
 * Time: 14:31:24
 * To change this template use Options | File Templates.
 */
abstract public class RadarLookupSensorHandler extends CoreLookupSensorHandler
{
  private final static String type = "RadarLookupSensor";
  private final static String K = "K";

  double _k;


  public RadarLookupSensorHandler()
  {
    super(type);

    super.addAttributeHandler(new HandleDoubleAttribute(K)
    {
      public void setValue(String name, final double val)
      {
        _k = val;
      }
    });
  }

  protected LookupSensor createLookupSensor(int id,
                                            double VDR, long TBDO, double MRF, double CRF, Duration CTP, double IRF,
                                            Duration ITP)
  {
    return new RadarLookupSensor(id, VDR, TBDO, MRF, CRF, CTP, IRF, ITP, _k);
  }

}