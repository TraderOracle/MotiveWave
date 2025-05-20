package HelloTOWorld;

//region IMPORTS
import java.awt.*;
import java.awt.Graphics2D;
import java.awt.geom.*;
import java.util.*;
import java.util.List;
import java.io.*;
import java.net.*;
import com.motivewave.platform.sdk.common.*;
import com.motivewave.platform.sdk.common.desc.*;
import com.motivewave.platform.sdk.common.menu.*;
import com.motivewave.platform.sdk.study.*;
import com.motivewave.platform.sdk.draw.*;
//endregion

@StudyHeader(
        namespace="com.HomieSlice",
        id="HelloTOWorld",
        rb="TraderOracle.nls.strings", // locale specific strings are loaded from here
        name="Hello TO World",
        label="HelloTOWorld",
        desc="Hello TO World",
        menu="Custom",
        overlay=true,
        studyOverlay=true,
        signals=true)

public class HelloTOWorld extends Study {
    //region VARIABLES
    enum Signals {KAMA_CROSS}

    private static final Color RED = new Color(255, 0, 0);
    private static final Color GREEN = new Color(0, 255, 0);
    private static final Color WHITE = new Color(255, 255, 255);
    private String msg = "";
    //endregion

    //region COOL EXTRA FUNCTIONS

    @Override
    public void onBarClose(DataContext ctx) {
        // Called after every new bar close
        debug("onBarClose called");
        var series = ctx.getDataSeries();

        this.clearFigures();

        long fStart = series.getVisibleStartTime();
        long fEnd = series.getVisibleEndTime();
        long fMid = fStart + (Math.abs(fEnd - fStart) / 2);
        PathInfo pf = new PathInfo(WHITE, 2, new float[] {4,1,3}, true, true, false, 0, 2);
        Line lk = new Line(new Coordinate(fStart, 20160), new Coordinate(fEnd, 20160), pf);
        lk.setText("VolImb", new Font("Arial", Font.PLAIN, 12));
        this.addFigure(lk);

    }

    @Override
    public void onSettingsUpdated(DataContext ctx) {
        // Called after user adjusts a setting in the config dialog
        debug("onSettingsUpdated called");
    }

    //endregion

    //region INITIALIZE AND MISC

    @Override
    public void initialize(Defaults defaults) {
        var sd = createSD();

        var tab = sd.addTab("Cool Settings");
        var grp = tab.addGroup("Inputs");
        grp.addRow(new BooleanDescriptor("ShowDot", "Show Dot", true));
        grp.addRow(new StringDescriptor("DisplayMe", "Display String", "Hello TO World - screen writing!"));
        grp.addRow(new IntegerDescriptor("KAMAPeriod", "KAMA Period", 9, 1, 9999, 1));
        grp.addRow(new InputDescriptor(Inputs.INPUT, "KAMA Line", Enums.BarInput.CLOSE));

        grp = tab.addGroup("Markers");
        grp.addRow(new MarkerDescriptor("UPMarker", "Up Marker", Enums.MarkerType.TRIANGLE, Enums.Size.SMALL, defaults.getGreen(), defaults.getLineColor(), true, true));
        grp.addRow(new MarkerDescriptor("DOWNMarker", "Down Marker", Enums.MarkerType.TRIANGLE, Enums.Size.SMALL, defaults.getRed(), defaults.getLineColor(), true, true));

        RuntimeDescriptor desc = new RuntimeDescriptor();
        setRuntimeDescriptor(desc);
        desc.declareSignal(Signals.KAMA_CROSS, "Kama Cross");
    }


    @Override
    protected void calculate(int index, DataContext ctx) {
        var series = ctx.getDataSeries();

        int lastCandleIndex = series.size() - 1;
        if (series == null || index < 202 || !series.isBarComplete(index))
            return;

        // region CONFIG SETTINGS
        msg = getSettings().getString("DisplayMe");
        boolean bUseKama = getSettings().getBoolean("KAMAPeriod");
        int iKAMAPeriod = getSettings().getInteger("KAMAPeriod");
        Object input = getSettings().getInput(Inputs.INPUT);
        //endregion

        //region CANDLE CALCS
        double close = series.getClose(index);
        double open = series.getOpen(index);
        double pclose = series.getClose(index - 1);
        double popen = series.getOpen(index - 1);
        double ppclose = series.getClose(index - 2);
        double ppopen = series.getOpen(index - 2);
        double low = series.getLow(index);
        double plow = series.getLow(index - 1);
        double high = series.getHigh(index);
        double phigh = series.getHigh(index - 1);
        boolean c0G = close > series.getOpen(index);
        boolean c1G = series.getClose(index - 1) > series.getOpen(index - 1);
        boolean c2G = series.getClose(index - 2) > series.getOpen(index - 2);
        boolean c0R = close < open;
        boolean c1R = series.getClose(index - 1) < series.getOpen(index - 1);
        boolean c2R = series.getClose(index - 2) < series.getOpen(index - 2);
        double body = Math.abs(open - close);
        double pbody = Math.abs(series.getOpen(index - 1) - series.getClose(index - 1));
        double kama = series.kama(index, iKAMAPeriod, input);
        double pkama = series.kama(index-1, iKAMAPeriod, input);
        boolean bGDoji = c1G && pbody < phigh - pclose && pbody < popen - plow;
        boolean bRDoji = c1R && pbody < phigh - popen && pbody < pclose - plow;
        boolean bDoji = bGDoji || bRDoji;
        //endregion

        // If we cross the KAMA, then add a marker
        if (close > kama && pclose < pkama)
        {
            var marker = getSettings().getMarker("UPMarker");
            Coordinate coords = new Coordinate(series.getStartTime(index), (double) low);
            this.addFigure(new Marker(coords, Enums.Position.BOTTOM, marker, msg));
            ctx.signal(index, Signals.KAMA_CROSS, "KAMA CROSS", close);
        }


        //lk.setExtendRightBounds(false);
        //if (coordsEnd == null)
        //    lk.setExtendRightBounds(true);
        //lk.setText("VolImb", new Font("Arial", Font.PLAIN, 12));
        //if (!al.contains(lk))
        //    al.add(lk);
        //this.addFigure(lk);

        // Draw horizontal line
        LineInfo lf = new LineInfo(20115d, Color.WHITE, 1.0f, new float[]{3, 1, 2});
        RuntimeDescriptor ts = new RuntimeDescriptor();
        setRuntimeDescriptor(ts);
        Plot t = ts.getDefaultPlot();
        //t.addHorizontalLine(lf);

    }
}

