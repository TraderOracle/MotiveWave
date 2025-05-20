package TraderOracle;

import java.awt.*;
import java.awt.Graphics2D;
import java.awt.geom.*;
import java.util.*;
import java.util.List;
import java.io.*;
import java.net.*;
import org.json.*;
import com.motivewave.platform.sdk.common.*;
import com.motivewave.platform.sdk.common.desc.*;
import com.motivewave.platform.sdk.common.menu.*;
import com.motivewave.platform.sdk.study.*;
import com.motivewave.platform.sdk.draw.*;

@StudyHeader(
        namespace="com.DickInTheSpleen",
        id="TripleSuperTrend",
        rb="TraderOracle.nls.strings", // locale specific strings are loaded from here
        name="TripleSuperTrend",
        label="TripleSuperTrend",
        desc="TripleSuperTrend",
        menu="TraderOracle",
        overlay=true,
        studyOverlay=true,
        signals=true)

public class TripleSuperTrend extends Study
{
    private static final Color RED = new Color(255, 0, 0);
    private static final Color GREEN = new Color(0, 255, 0);
    private static final Color WHITE = new Color(255, 255, 255);
    private static final Color YELLOW = new Color(255, 0, 0);
    private boolean bDrawn = false;
    enum Values { UP, DOWN, TREND };

    @Override
    public void onSettingsUpdated(DataContext ctx)
    {
    }

    @Override
    public void onBarClose(DataContext ctx)
    {
        bDrawn = false;
    }

    @Override
    public void initialize(Defaults defaults)
    {
        var sd = createSD();
        var tab = sd.addTab("BOINK");
        var grp = tab.addGroup("Inputs");
        grp.addRow(new BooleanDescriptor("ShowBOINK", "Show BOINKs", true));
        grp.addRow(new BooleanDescriptor("UseKAMA", "Use KAMA", true));
        grp.addRow(new IntegerDescriptor("KAMAPeriod", "KAMA Period", 9, 1, 9999, 1));
        grp.addRow(new BooleanDescriptor("StdEMA", "Use Standard EMA", false));
        grp.addRow(new IntegerDescriptor("EMAPeriod", "EMA Period", 21, 1, 9999, 1));
        grp.addRow(new BooleanDescriptor("BigEMA", "Use Larger Ema", true));
        grp.addRow(new IntegerDescriptor("LargerEMAPeriod", "Larger EMA Period", 200, 1, 9999, 1));

        grp = tab.addGroup("Markers");
        grp.addRow(new MarkerDescriptor("UPBOINKMarker", "Up Marker", Enums.MarkerType.TRIANGLE, Enums.Size.SMALL, defaults.getGreen(), defaults.getLineColor(), true, true));
        grp.addRow(new MarkerDescriptor("DOWNBOINKMarker", "Down Marker", Enums.MarkerType.TRIANGLE, Enums.Size.SMALL, defaults.getRed(), defaults.getLineColor(), true, true));

        var tab1=sd.addTab("Engulfing BB");
        var grp1=tab1.addGroup("Inputs");
        grp1.addRow(new BooleanDescriptor("ShowEngBB", "Show Engulfing off Bollinger Band", true));
        grp1.addRow(new BooleanDescriptor("ColorCandle", "Color candle white", true));

        grp1 = tab1.addGroup("Markers");
        grp1.addRow(new MarkerDescriptor("UPEngBBMarker", "Up Marker",
                Enums.MarkerType.CIRCLE, Enums.Size.SMALL,
                defaults.getGreen(), defaults.getLineColor(), true, true));
        grp1.addRow(new MarkerDescriptor("DOWNEngBBMarker", "Down Marker",
                Enums.MarkerType.CIRCLE, Enums.Size.SMALL,
                defaults.getRed(), defaults.getLineColor(), true, true));

        RuntimeDescriptor desc = new RuntimeDescriptor();
        setRuntimeDescriptor(desc);
    }

    @Override
    protected void calculate(int index, DataContext ctx) {
        DataSeries series = ctx.getDataSeries();
        Double var7 = series.atr(index, var5);
        if (var7 != null) {
            float var8 = (series.getHigh(index) + series.getLow(index)) / 2.0F;
            double var9 = (double) var8 - var3 * var7;
            double index1 = (double) var8 + var3 * var7;
            Double index3 = series.getDouble(index - 1, Values.UP);
            Double index4 = series.getDouble(index - 1, Values.DOWN);
            Double index5 = series.getDouble(index - 1, Values.TREND);
            float index6 = series.getClose(index);
            float index7 = series.getClose(index - 1);
            if (index3 != null && (double) index7 > index3) {
                var9 = Util.max(var9, index3);
            }

            if (index4 != null && (double) index7 < index4) {
                index1 = Util.min(new double[]{index1, index4});
            }

            double index8 = (double) 1.0F;
            if (index4 != null && (double) index6 > index4) {
                index8 = (double) 1.0F;
            } else if (index3 != null && (double) index6 < index3) {
                index8 = (double) -1.0F;
            } else if (index5 != null) {
                index8 = index5;
            }

            series.setDouble(index, Values.TREND, index8);
            series.setDouble(index, Values.UP, var9);
            series.setDouble(index, Values.DOWN, index1);
            boolean var20 = index5 != null && index8 != index5;
            boolean var21 = series.isBarComplete(index);
            if (index8 > (double) 0.0F) {
                //this.addFigure(new Marker(new Coordinate(series.getStartTime(index), (double) series.getLow(index)),Position.BOTTOM, var22, var23));

            } else {
               //series.setDouble(index, SuperTrend$Values.TSL, index1);
                //series.setPathColor(index, SuperTrend$Values.TSL, this.getSettings().getColor("downColor",var2.getDefaults().getRedLine()));
                if (var20) {
                    //this.addFigure(new Marker(new Coordinate(series.getStartTime(index), (double) series.getHigh
                    // (index)), Position.TOP, var24, var25));
                    }
                }

            series.setComplete(index, var21);
        }
    }
}