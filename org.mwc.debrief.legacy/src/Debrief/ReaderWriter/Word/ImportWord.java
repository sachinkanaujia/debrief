package Debrief.ReaderWriter.Word;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import junit.framework.TestCase;

import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.usermodel.Paragraph;
import org.apache.poi.hwpf.usermodel.Range;

import Debrief.GUI.Frames.Application;
import Debrief.ReaderWriter.Replay.ImportReplay;
import Debrief.Wrappers.FixWrapper;
import Debrief.Wrappers.NarrativeWrapper;
import Debrief.Wrappers.TrackWrapper;
import MWC.GUI.Editable;
import MWC.GUI.Layer;
import MWC.GUI.Layers;
import MWC.GUI.ToolParent;
import MWC.GUI.Properties.DebriefColors;
import MWC.GenericData.HiResDate;
import MWC.GenericData.TimePeriod;
import MWC.GenericData.Watchable;
import MWC.GenericData.WatchableList;
import MWC.GenericData.WorldDistance;
import MWC.GenericData.WorldLocation;
import MWC.GenericData.WorldSpeed;
import MWC.GenericData.WorldVector;
import MWC.TacticalData.Fix;
import MWC.TacticalData.NarrativeEntry;

public class ImportWord
{

  private static class FCSEntry
  {
    /**
     * extract the track number from the provided string
     * 
     * @param str
     * @return
     */
    private static String parseTrack(final String str)
    {
      final String longTrackId = "[A-Z](\\d{3})";
      final Pattern longPattern = Pattern.compile(longTrackId);

      final Matcher matcher1 = longPattern.matcher(str);
      final String res;
      if (matcher1.find())
      {
        res = matcher1.group(1);
      }
      else
      {
        final String shortTrackId = "(M\\d{2})";
        final Pattern shortPattern = Pattern.compile(shortTrackId);

        final Matcher matcher = shortPattern.matcher(str);
        if (matcher.find())
        {
          res = matcher.group(1);
        }
        else
        {
          res = null;
        }
      }

      return res;
    }

    final double brgDegs;
    final double rangYds;
    final String tgtType;
    private String contact;
    public double crseDegs;
    public double spdKts;

    @SuppressWarnings("unused")
    private final String label;

    public FCSEntry(final NarrEntry thisN, final String msg)
    {

      final int bIndex = msg.indexOf("B-");
      final int rIndex = msg.indexOf("R-");
      final int rEndIndex = msg.indexOf("kyds", rIndex);
      final int cIndex = msg.indexOf("C-");
      final int sIndex = msg.indexOf("S-");
      final int classStrStart = msg.indexOf("Classified");
      final int classStrEnd1 = -1;
      final int classStrEnd2 =
          msg.indexOf(".", classStrStart + "Classified".length() + 2);

      final int classStrEnd;
      if (classStrEnd1 != -1 && classStrEnd2 != -1)
      {
        classStrEnd = Math.min(classStrEnd1, classStrEnd2);
      }
      else if (classStrEnd2 != -1)
      {
        classStrEnd = classStrEnd2;
      }
      else if (classStrEnd1 != -1)
      {
        classStrEnd = classStrEnd1;
      }
      else
      {
        classStrEnd = -1;
        // ok, we can't process it. maybe we need to use the
      }

      // did we have Distance off track?
      final int speedEnd = msg.indexOf(" ", sIndex + 1);

      final String id = msg.substring(0, bIndex).trim();
      String bStr = msg.substring(bIndex + 2, rIndex - 1);
      final String rStr = msg.substring(rIndex + 2, rEndIndex);

      String cStr;
      if (cIndex == -1)
      {
        cStr = "N/A";
      }
      else
      {
        cStr = msg.substring(cIndex + 2, sIndex - 1);

        // just double-check there isn't a degree symbol in there
        cStr = cStr.replace("\u00B0", "");
        cStr = cStr.replace("�", "");

        final int degIndex = cStr.indexOf("�");
        if (degIndex != -1)
        {
          cStr = cStr.substring(0, degIndex);
        }

        this.crseDegs = Double.parseDouble(cStr);
      }

      String sStr;
      if (cIndex == -1)
      {
        sStr = "N/A";
      }
      else
      {
        sStr = msg.substring(sIndex + 2, speedEnd);

        // extract the kts
        final int ktsIndex = sStr.indexOf("kt");
        if (ktsIndex != -1)
        {
          sStr = sStr.substring(0, ktsIndex);
        }
        this.spdKts = Double.parseDouble(sStr);
      }

      final String classStr;
      if (classStrStart != -1 && classStrEnd != -1)
      {
        classStr =
            msg.substring(classStrStart + "Classified".length(), classStrEnd)
                .trim();
      }
      else
      {
        classStr = "N/A";
      }

      tgtType = classStr.trim();

      label = "Class:" + classStr + " " + cStr + "\u00B0 " + sStr;

      // try to extract the track id
      final String trackId = parseTrack(id);
      if (trackId != null)
      {
        // ok. we have an integer.
        contact = trackId;
      }
      else
      {
        contact = id;
      }

      // just check there isn't a bearing value in there
      bStr = bStr.replace("�", "");

      final int degIndex = bStr.indexOf("�");
      if (degIndex != -1)
      {
        bStr = bStr.substring(0, degIndex);
      }

      brgDegs = Double.parseDouble(bStr);
      rangYds = Double.parseDouble(rStr) * 1000;
    }

  }

  private static class NarrEntry
  {
    HiResDate dtg;
    String type;
    String platform;
    String text;

    boolean appendedToPrevious = false;

    // ///////////////////
    // static variables to help handle corrupt/incomplete data.
    // NOTE: any new ones should be included in the "reset() processing
    // ///////////////////

    private static Date lastDtg;
    private static String lastPlatform;
    private static NarrEntry lastEntry;
    /**
     * we've encountered circumstances where copy/paste has ended up with the day being earlier than
     * the current one When we can detect this, we'll use the previous day.
     */
    private static String lastDay;

    static public NarrEntry create(final String msg, final int lineNum)
    {
      NarrEntry res = null;
      try
      {
        res = new NarrEntry(msg);

        if (res.appendedToPrevious && res.text != null)
        {
          // that's ok - we'll let the parent handle it
        }
        else
        {
          // just check it's valid
          final boolean valid =
              (res.dtg != null) && (res.type != null) && (res.platform != null)
                  && (res.text != null);
          if (!valid)
          {
            res = null;
          }
        }
      }
      catch (final ParseException e)
      {
        logThisError("Failed whilst parsing Word Document, at line:" + lineNum,
            e);
      }

      return res;
    }

    /** reset the static variables we use to handle missing, or mangled data
     * 
     */
    public static void reset()
    {
      lastDtg = null;
      lastPlatform = null;
      lastEntry = null;
      lastDay = null;
    }

    @SuppressWarnings("deprecation")
    public NarrEntry(final String entry) throws ParseException
    {
      final String trimmed = entry.trim();
      final String[] parts = trimmed.split(",");
      int ctr = 0;

      final boolean correctLength = parts.length > 5;
      final boolean sixFigDTG =
          correctLength && parts[0].length() == 6
              && parts[0].matches(DATE_MATCH_SIX);
      final boolean fourFigDTG =
          correctLength && parts[0].length() == 4
              && parts[0].matches(DATE_MATCH_FOUR);
      final boolean hasDTG = sixFigDTG || fourFigDTG;

      if (hasDTG)
      {
        final String dtgStr;
        if (fourFigDTG)
        {
          dtgStr = parts[ctr++];
        }
        else
        {
          dtgStr = parts[ctr++].substring(2, 6);
        }

        // ok, sort out the time first
        String dayStr = parts[ctr++];
        final String monStr = parts[ctr++];
        final String yrStr = parts[ctr++];
        platform = parts[ctr++].trim();
        type = parts[ctr++].trim();

        /**
         * special processing, to overcome the previous day being used
         * 
         */
        if (lastDay != null
            && Integer.parseInt(dayStr) < Integer.parseInt(lastDay))
        {
          dayStr = lastDay;
        }
        else
        {
          // it's valid, update the last day
          lastDay = dayStr;
        }

        // hmm, on occasion we don't get the closing comma on the entry type
        if (type.length() > 20)
        {
          final int firstSpace = type.indexOf(" ");
          // note: should actually be looking for non-alphanumeric, since it may be a tab
          type = type.substring(0, firstSpace - 1);
        }

        final int year;
        if (yrStr.length() == 2)
        {
          year = 2000 + Integer.parseInt(yrStr);
        }
        else
        {
          year = Integer.parseInt(yrStr);
        }

        final Date datePart =
            new Date(year - 1900, Integer.parseInt(monStr) - 1, Integer
                .parseInt(dayStr));

        final DateFormat dtgBlock = new SimpleDateFormat("HHmm");
        dtgBlock.setTimeZone(TimeZone.getTimeZone("GMT"));

        final Date timePart = dtgBlock.parse(dtgStr);

        dtg = new HiResDate(new Date(datePart.getTime() + timePart.getTime()));

        // ok, and the message part
        final int ind = entry.indexOf(type);

        text = entry.substring(ind + type.length() + 2).trim();

        // remember what's happening, so we can refer back to previous entries
        lastDtg = new Date(dtg.getDate().getTime());
        lastPlatform = platform;
        lastEntry = this;
      }
      else
      {

        final int firstTab = trimmed.indexOf("\t");
        int blockToUse = 6;
        if (firstTab != -1 && firstTab <= 7)
        {
          blockToUse = firstTab;
        }

        // see if the first few characters are date
        final String dateStr =
            trimmed.substring(0, Math.min(trimmed.length(), blockToUse));

        // is this all numeric
        boolean probIsDate = false;

        try
        {
          if (dateStr.length() == 6 || dateStr.length() == 4)
          {
            @SuppressWarnings("unused")
            final int testInt = Integer.parseInt(dateStr);
            probIsDate = true;
          }
        }
        catch (final NumberFormatException e)
        {
        }

        if (probIsDate)
        {
          // yes, go for it.

          // ooh, do we have some stored data?
          if (lastDtg != null && lastPlatform != null)
          {
            Date timePart = null;
            if (dateStr.length() == 6)
            {
              // first try to parse it
              final DateFormat dtgBlock = new SimpleDateFormat("ddHHmm");
              dtgBlock.setTimeZone(TimeZone.getTimeZone("GMT"));
              timePart = dtgBlock.parse(trimmed);

              // check the date matches
              final int date = timePart.getDate();

              if (date == lastDtg.getDate())
              {
                // ok, we can go for it
                final Date newDate = new Date(lastDtg.getTime());

                newDate.setHours(timePart.getHours());
                newDate.setMinutes(timePart.getMinutes());
                newDate.setSeconds(timePart.getSeconds());

                dtg = new HiResDate(newDate);
              }

            }
            else if (dateStr.length() == 4)
            {
              // first try to parse it
              final DateFormat dtgBlock = new SimpleDateFormat("HHmm");
              dtgBlock.setTimeZone(TimeZone.getTimeZone("GMT"));
              timePart = dtgBlock.parse(trimmed);

              // ok, we can go for it
              final Date newDate = new Date(lastDtg.getTime());

              newDate.setHours(timePart.getHours());
              newDate.setMinutes(timePart.getMinutes());
              newDate.setSeconds(timePart.getSeconds());

              dtg = new HiResDate(newDate);

            }

            // stash the platform
            platform = lastPlatform;

            // and catch the rest of the text
            text = trimmed.substring(dateStr.length()).trim();

            // see if we can recognise the first word as a track number
            final String startOfLine =
                text.substring(0, Math.min(20, text.length() - 1));
            final String trackNum = FCSEntry.parseTrack(startOfLine);
            if (trackNum != null)
            {
              type = "FCS";
            }
            else
            {
              // explain we don't know what type of comment this is
              type = "N/A";
            }

            // try to replace soft returns with hard returns
            text = text.replace("\r", "\n");
          }
        }
        else
        {
          // hmm, see if it's just text. If it is, stick it on the end of the previous one

          // ooh, it may be a next day marker. have a check
          final DateFormat dtgBlock = new SimpleDateFormat("dd MMM yy");
          dtgBlock.setTimeZone(TimeZone.getTimeZone("GMT"));

          boolean hasDate = false;
          try
          {
            @SuppressWarnings("unused")
            final Date scrapDate = dtgBlock.parse(trimmed);
            hasDate = true;
          }
          catch (final ParseException e)
          {
            // it's ok, we can silently fail
          }

          if (hasDate)
          {
            // ok. skip it. it's just a date
          }
          else
          {
            // ooh, do we have a previous one?
            if (lastEntry != null)
            {
              text = trimmed;

              // now flag that we've just added ourselves to the previous one
              appendedToPrevious = true;
            }
          }
        }
      }
    }
  }

  public static class TestImportWord extends TestCase
  {

    private final static String doc_path = "../org.mwc.cmap.combined.feature/root_installs/sample_data/other_formats/narrative.doc";


    public void testImportEmptyLayers() throws FileNotFoundException
    {
      final String testFile = doc_path;
      final File testI = new File(testFile);
      assertTrue(testI.exists());

      final InputStream is = new FileInputStream(testI);

      final Layers tLayers = new Layers();

      final ImportWord importer = new ImportWord(tLayers);
      importer.importThis(testFile, is);

      // hmmm, how many tracks
      assertEquals("got new tracks", 1, tLayers.size());

      final NarrativeWrapper narrLayer =
          (NarrativeWrapper) tLayers.elementAt(0);
      System.out.println("processed:" + narrLayer.size());

    }

    public void testNameHandler()
    {
      final Layers layers = new Layers();
      final TrackWrapper track = new TrackWrapper();
      track.setName("Nelson");
      layers.addThisLayer(track);
      final TrackWrapper track2 = new TrackWrapper();
      track2.setName("Iron Duck");
      layers.addThisLayer(track2);
      final ImportWord iw = new ImportWord(layers);
      String match = iw.trackFor("HMS Boat", "HMS Boat");
      assertNull("not found match", match);
      match = iw.trackFor("HMS Nelson", "HMS Nelson");
      assertNotNull("found match", match);
      match = iw.trackFor("Hms Nelson", "Hms Nelson");
      assertNotNull("found match", match);
      match = iw.trackFor("RNAS Nelson", "RNAS Nelson");
      assertNotNull("found match", match);

      // check we've created new entries
      assertEquals("name matches", 3, iw.nameMatches.size());

      // and the two word name
      match = iw.trackFor("Hms Iron Duck", "Hms Iron Duck");
      assertNotNull("found match", match);

      // check we've created new entries
      assertEquals("name matches", 4, iw.nameMatches.size());

    }

    @SuppressWarnings("deprecation")
    public void testParseDate()
    {

      final String goodDate = "000000";
      assertTrue("date", goodDate.matches(DATE_MATCH_SIX));
      assertFalse("not date", "Notes:".matches(DATE_MATCH_SIX));

      final String testDate1 =
          "160909,16,09,2016,HMS NONSUCH, CAT COMMENT, SOME COMMENT ";

      // ok, get the narrative type
      final NarrEntry thisN1 = NarrEntry.create(testDate1, 1);
      assertEquals("year", 116, thisN1.dtg.getDate().getYear());
      assertEquals("month", 8, thisN1.dtg.getDate().getMonth());
      assertEquals("day", 16, thisN1.dtg.getDate().getDate());
      assertEquals("hour", 9, thisN1.dtg.getDate().getHours());
      assertEquals("min", 9, thisN1.dtg.getDate().getMinutes());
      assertEquals("sec", 0, thisN1.dtg.getDate().getSeconds());
      assertEquals("platform", "HMS NONSUCH", thisN1.platform);
      assertEquals("content", "SOME COMMENT", thisN1.text);

      // ok, now one with mangled (missing) date fields
      final String testDate2 = "161006\tSOME COMMENT 2 ";
      // ok, get the narrative type
      final NarrEntry thisN2 = NarrEntry.create(testDate2, 1);
      assertEquals("year", 116, thisN2.dtg.getDate().getYear());
      assertEquals("month", 8, thisN2.dtg.getDate().getMonth());
      assertEquals("day", 16, thisN2.dtg.getDate().getDate());
      assertEquals("hour", 11, thisN2.dtg.getDate().getHours());
      assertEquals("min", 6, thisN2.dtg.getDate().getMinutes());
      assertEquals("sec", 0, thisN2.dtg.getDate().getSeconds());
      assertEquals("platform", "HMS NONSUCH", thisN2.platform);
      assertEquals("content", "SOME COMMENT 2", thisN2.text);
      assertFalse("flag", thisN2.appendedToPrevious);

      // hey, what if it's just text?
      final String testDate3 = "SOME COMMENT ";
      // ok, get the narrative type
      final NarrEntry thisN3 = NarrEntry.create(testDate3, 1);

      // ok, should just be that text
      assertNull("year", thisN3.dtg);
      assertNull("platform", thisN3.platform);
      assertNotNull("content", thisN3.text);
      assertTrue("flag", thisN3.appendedToPrevious);

    }

    public void testParseFCS() throws ParseException
    {
      final String str1 =
          "160504,16,08,2016,NONSUCH,AAA,   SR023 AAAA AAAA AAA (AAAA) B-123 R-5kyds C-321 S-6kts AAAAAAA. Classified AAAAAA BBBBBB AAAAAA. ";

      final String str2 =
          "160403,16,09,2016,NONSUCH,FCS, M01 1234 Rge B-311� R-12.4kyds. Classified AAAAAA CCCCCC AAAAAA.";

      NarrEntry ne = new NarrEntry(str1);
      final FCSEntry fe1 = new FCSEntry(ne, ne.text);
      assertEquals("got range:", 5000d, fe1.rangYds);
      assertEquals("got brg:", 123d, fe1.brgDegs);
      assertEquals("got contact:", "023", fe1.contact);
      assertEquals("got course:", 321d, fe1.crseDegs);
      assertEquals("got speed:", 6d, fe1.spdKts);
      assertEquals("got name:", "AAAAAA BBBBBB AAAAAA", fe1.tgtType);

      ne = new NarrEntry(str2);
      final FCSEntry fe2 = new FCSEntry(ne, ne.text);
      assertEquals("got range:", 12400d, fe2.rangYds);
      assertEquals("got brg:", 311d, fe2.brgDegs);
      assertEquals("got contact:", "M01", fe2.contact);
      assertEquals("got course:", 0d, fe2.crseDegs);
      assertEquals("got speed:", 0d, fe2.spdKts);
      assertEquals("got name:", "AAAAAA CCCCCC AAAAAA", fe2.tgtType);

    }

    public void testParseTrackNumber()
    {
      final String str1 = "asdfads S000 adf ag a";
      final String str1a = "asdfads S000 adf ag a";
      final String str2 = "asdfads SV000 adf ag a";
      final String str2a = "asdfads M00 adf ag a";
      final String str3 = "asdfads adf ag a";
      final String str5 = "M00 0000";

      assertEquals("right id", "000", FCSEntry.parseTrack(str1));
      assertEquals("right id", "000", FCSEntry.parseTrack(str1a));
      assertEquals("right id", "000", FCSEntry.parseTrack(str2));
      assertEquals("right id", "M00", FCSEntry.parseTrack(str2a));
      assertEquals("right id", "M00", FCSEntry.parseTrack(str5));
      assertNull("right id", FCSEntry.parseTrack(str3));
    }
  }

  private static List<String> SkipNames = null;

  /**
   * match a 6 figure DTG
   * 
   */
  private static final String DATE_MATCH_SIX = "(\\d{6})";

  private static final String DATE_MATCH_FOUR = "(\\d{4})";

  public static void logThisError(final String msg, final Exception e)
  {
    Application.logError2(ToolParent.WARNING, msg, e);
  }

  /**
   * where we write our data
   * 
   */
  private final Layers _layers;

  /**
   * keep track of the last successfully imported narrateive entry if we've just received a plain
   * text block, we'll add it to the previous one *
   */
  private NarrativeEntry _lastEntry;

  Map<String, String> nameMatches = new HashMap<String, String>();

  public ImportWord(final Layers target)
  {
    _layers = target;

    if (SkipNames == null)
    {
      SkipNames = new ArrayList<String>();
      SkipNames.add("HMS");
      SkipNames.add("Hms");
      SkipNames.add("USS");
      SkipNames.add("RNAS");
      SkipNames.add("HNLMS");
    }
  }

  private void addEntry(final NarrEntry thisN)
  {
    final NarrativeWrapper nw = getNarrativeLayer();
    String hisTrack = trackFor(thisN.platform, thisN.platform);

    // did we find a track? Don't worry if we didn't just use the raw text
    if (hisTrack == null)
    {
      hisTrack = thisN.platform;
    }

    final NarrativeEntry ne =
        new NarrativeEntry(hisTrack, thisN.type, new HiResDate(thisN.dtg),
            thisN.text);

    // remember that entry, in case we get incomplete text inthe future
    _lastEntry = ne;

    // try to color the entry
    final Layer host = _layers.findLayer(trackFor(thisN.platform));
    if (host instanceof TrackWrapper)
    {
      final TrackWrapper tw = (TrackWrapper) host;
      ne.setColor(tw.getColor());
    }

    // and store it
    nw.add(ne);
  }

  private void addFCS(final NarrEntry thisN)
  {
    // ok, parse the message
    final FCSEntry fe = new FCSEntry(thisN, thisN.text);

    // find the host
    final TrackWrapper host =
        (TrackWrapper) _layers.findLayer(trackFor(thisN.platform));
    if (host != null)
    {
      // find the fix nearest this time
      final Watchable[] nearest = host.getNearestTo(thisN.dtg);
      if (nearest != null && nearest.length > 0)
      {
        final Watchable fix = nearest[0];
        // apply the offset
        final WorldVector vec =
            new WorldVector(Math.toRadians(fe.brgDegs), new WorldDistance(
                fe.rangYds, WorldDistance.YARDS), new WorldDistance(0,
                WorldDistance.METRES));
        final WorldLocation loc = fix.getLocation().add(vec);

        // find the track for this solution
        TrackWrapper hisTrack = (TrackWrapper) _layers.findLayer(fe.contact);
        if (hisTrack == null)
        {
          hisTrack = new TrackWrapper();
          hisTrack.setName(fe.contact);
          hisTrack.setColor(DebriefColors.RED);
          _layers.addThisLayer(hisTrack);
        }

        // ok, now create the fix
        final WorldSpeed ws = new WorldSpeed(fe.spdKts, WorldSpeed.Kts);
        final double yds_per_sec = ws.getValueIn(WorldSpeed.ft_sec / 3);
        final Fix newF =
            new Fix(thisN.dtg, loc, Math.toRadians(fe.crseDegs), yds_per_sec);
        final FixWrapper newFw = new FixWrapper(newF);

        // and store it
        hisTrack.add(newFw);
      }
      else
      {
        logError("Host fix not present for FCS at:" + thisN.dtg.getDate(), null);
      }
    }
  }

  private NarrativeWrapper getNarrativeLayer()
  {
    NarrativeWrapper nw =
        (NarrativeWrapper) _layers.findLayer(ImportReplay.NARRATIVE_LAYER);

    if (nw == null)
    {
      nw = new NarrativeWrapper(ImportReplay.NARRATIVE_LAYER);
      _layers.addThisLayer(nw);
    }

    return nw;
  }

  public void importThis(final String fName, final InputStream is)
  {
    HWPFDocument doc = null;
    try
    {
      doc = new HWPFDocument(is);
    }
    catch (final IOException e)
    {
      e.printStackTrace();
    }

    if (doc == null)
      return;

    // keep track of if we've added anything
    boolean dataAdded = false;

    // find the outer time period - we only load data into the current time period
    TimePeriod outerPeriod = null;
    final Enumeration<Editable> layers = _layers.elements();
    while (layers.hasMoreElements())
    {
      final Layer thisL = (Layer) layers.nextElement();
      if (thisL instanceof WatchableList)
      {
        final WatchableList wl = (WatchableList) thisL;
        if (wl.getStartDTG() != null && wl.getEndDTG() != null)
        {
          final TimePeriod thisP =
              new TimePeriod.BaseTimePeriod(wl.getStartDTG(), wl.getEndDTG());
          if (outerPeriod == null)
          {
            outerPeriod = thisP;
          }
          else
          {
            outerPeriod.extend(wl.getStartDTG());
            outerPeriod.extend(wl.getEndDTG());
          }
        }
      }
    }

    final Range r = doc.getRange();

    // clear the stored data in the MS Word importer
    NarrEntry.reset();

    final int lenParagraph = r.numParagraphs();
    for (int x = 0; x < lenParagraph; x++)
    {
      final Paragraph p = r.getParagraph(x);
      final String text = p.text();
      if (text.trim().length() == 0)
      {
        continue;
      }

      // ok, get the narrative type
      final NarrEntry thisN = NarrEntry.create(text, x);

      if (thisN == null)
      {
        // logError("Unable to parse line:" + text, null);
        continue;
      }

      // do we know the outer time period?
      if (outerPeriod != null && thisN.dtg != null)
      {
        // check it's in the currently loaded time period
        if (!outerPeriod.contains(thisN.dtg))
        {
          // ok, it's not in our period
          continue;
        }
      }

      // is it just text, that we will appned
      if (thisN.appendedToPrevious)
      {
        // hmm, just check if this is an FCS

        // do we have a previous one?
        if (_lastEntry != null)
        {
          final String newText = thisN.text;

          _lastEntry.setEntry(_lastEntry.getEntry() + "\n" + newText);
        }

        // ok, we can't do any more. carry on
        continue;
      }

      switch (thisN.type)
      {
      case "FCS":
      {
        // add a narrative entry
        addEntry(thisN);

        // create track for this
        try
        {
          addFCS(thisN);
        }
        catch (final StringIndexOutOfBoundsException e)
        {
          // don't worry about panicking, it may not be an FCS after all
        }
        catch (final NumberFormatException e)
        {
          // don't worry about panicking, it may not be an FCS after all
        }

        // ok, take note that we've added something
        dataAdded = true;

        break;
      }
      default:
      {
        // ok, just add a narrative entry for anything not recognised

        // add a narrative entry
        addEntry(thisN);

        // ok, take note that we've added something
        dataAdded = true;

        break;

      }
      }
    }

    if (dataAdded)
    {
      _layers.fireModified(getNarrativeLayer());
    }
  }

  public void logError(final String msg, final Exception e)
  {
    logThisError(msg, e);
  }

  private String trackFor(final String originalName)
  {
    return trackFor(originalName, null);
  }

  private String trackFor(final String originalName, String name)
  {
    if (name == null)
    {
      name = originalName;
    }

    final String platform = name.trim();
    String match = nameMatches.get(platform);
    if (match == null)
    {
      // search the layers
      final Layer theL = _layers.findLayer(platform);
      if (theL != null)
      {
        match = theL.getName();
        nameMatches.put(originalName, match);
      }
      else
      {
        // try skipping then names
        final Iterator<String> nameIter = SkipNames.iterator();
        while (nameIter.hasNext() && match == null)
        {
          final String thisSkip = nameIter.next();
          if (platform.startsWith(thisSkip))
          {
            final String subStr = platform.substring(thisSkip.length()).trim();
            match = trackFor(originalName, subStr);
          }
        }
      }
    }

    return match;
  }
}
