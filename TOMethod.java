package study_examples;

import com.motivewave.platform.sdk.common.desc.SignalDescriptor;
import com.motivewave.platform.sdk.study.RuntimeDescriptor;
import com.motivewave.platform.sdk.common.DataSeries;
import com.motivewave.platform.sdk.common.DataContext;
import com.motivewave.platform.sdk.common.Defaults;
import com.motivewave.platform.sdk.common.Enums;
import com.motivewave.platform.sdk.common.Inputs;
import com.motivewave.platform.sdk.common.desc.InputDescriptor;
import com.motivewave.platform.sdk.common.desc.IntegerDescriptor;
import com.motivewave.platform.sdk.common.desc.PathDescriptor;
import com.motivewave.platform.sdk.common.desc.ValueDescriptor;
import com.motivewave.platform.sdk.study.Study;
import com.motivewave.platform.sdk.study.StudyHeader;
import com.motivewave.platform.sdk.common.BaseInfo;
import com.motivewave.platform.sdk.common.LineInfo;
import com.motivewave.platform.sdk.common.desc.BooleanDescriptor;
import com.motivewave.platform.sdk.common.desc.PathDescriptor;
import com.motivewave.platform.sdk.common.menu.MenuDescriptor;
import com.motivewave.platform.sdk.common.menu.MenuItem;
import com.motivewave.platform.sdk.common.menu.MenuSeparator;
import com.motivewave.platform.sdk.draw.Figure;
import com.motivewave.platform.sdk.draw.ResizePoint;
import com.motivewave.platform.sdk.study.StudyHeader;
import com.motivewave.platform.sdk.study.Plot;

import java.awt.*;

@StudyHeader(
        namespace="com.mycompany",
        id="TOMethod",
        rb="study_examples.nls.strings", // locale specific strings are loaded from here
        name="TraderOracle Method",
        label="TOMethod",
        desc="TraderOracle Method",
        menu="MENU_EXAMPLES",
        overlay=true,
        studyOverlay=true,
        signals=true)
public class TOMethod extends Study
{
    enum Values { MA, MOMENTUM, BB_KC_DIFF }
    enum Signals { ENG_BB, BOINK }

    private static final Color DEFAULT_COLOR = new Color(255, 0, 0);
    private int bodyToTailRatio = 1; // Customize this value to adjust the sensitivity

    @Override
    public void initialize(Defaults defaults)
    {
        var desc = createRD();
        // Signals
        desc.declareSignal(Signals.BOINK, "BOINK");
        desc.declareSignal(Signals.ENG_BB, "Engulfing candle off BB");

        // Describe the settings that may be configured by the user.
        // Settings may be organized using a combination of tabs and groups.
        var sd = createSD();
        var tab = sd.addTab(get("TAB_GENERAL"));

        var grp = tab.addGroup(get("LBL_INPUTS"));
        // Declare the inputs that are used to calculate the moving average.
        // Note: the 'Inputs' class defines several common input keys.
        // You can use any alpha-numeric string that you like.
        grp.addRow(new InputDescriptor(Inputs.INPUT, get("Input"), Enums.BarInput.CLOSE));
        grp.addRow(new IntegerDescriptor(Inputs.PERIOD, get("Period"), 20, 1, 9999, 1));

        grp = tab.addGroup(get("TAB_DISPLAY"));
        // Allow the user to change the settings for the path that will
        // draw the moving average on the graph.  In this case, we are going
        // to use the input key Inputs.PATH
        grp.addRow(new PathDescriptor(Inputs.PATH, get("Path"), null, 1.0f, null, true, true, false));

        // Describe the runtime settings using a 'RuntimeDescriptor'

        // Describe how to create the label.  The label uses the
        // 'label' attribute in the StudyHeader (see above) and adds the input values
        // defined below to generate a label.
        desc.setLabelSettings(Inputs.INPUT, Inputs.PERIOD);
        // Exported values can be used to display cursor data
        // as well as provide input parameters for other studies,
        // generate alerts or scan for study patterns (see study scanner).
        desc.exportValue(new ValueDescriptor(Values.MA, get("My MA"),
                new String[] {Inputs.INPUT, Inputs.PERIOD}));
        // MotiveWave will automatically draw a path using the path settings
        // (described above with the key 'Inputs.LINE')  In this case
        // it will use the values generated in the 'calculate' method
        // and stored in the data series using the key 'Values.MA'
        desc.declarePath(Values.MA, Inputs.PATH);
    }

    @Override
    protected void calculate(int index, DataContext ctx) {
        var series = ctx.getDataSeries();
        int last = series.size() - 1;
        Object input = getSettings().getInput(Inputs.INPUT);
        if (series == null) return;
        if (index < 202) return;

        //series.setPriceBarColor(index, Color.white);
        series.setDouble(index, Values.MA, 19680d);
        if (index == series.size() - 1)
            series.setComplete(index);

        double clow = series.getLow(index);
        double plow = series.getLow(index - 1);
        double high = series.getHigh(index);
        double phigh = series.getHigh(index - 1);
        boolean c0G = series.getClose(index) > series.getOpen(index);
        boolean c1G = series.getClose(index - 1) > series.getOpen(index - 1);
        boolean c0R = series.getClose(index) < series.getOpen(index);
        boolean c1R = series.getClose(index - 1) < series.getOpen(index - 1);
        double body = Math.abs(series.getOpen(index) - series.getClose(index));
        double pbody = Math.abs(series.getOpen(index - 1) - series.getClose(index - 1));

        //double bodyHeight = Math.abs(open - close);
        //double tailHeight = Math.min(Math.abs(high - open), Math.abs(high - close));
        //double bodyToTailHeightRatio = bodyHeight / tailHeight;

        double kama = series.kama(index, 9, input);
        double ema200 = series.ema(index, 200, input);

        // Calculate Bollinger Bands
        double middleBB = series.sma(index , 20, input);
        double stdDev = series.std(index, 20, input);
        double upperBB = middleBB + (2 * stdDev);
        double lowerBB = middleBB - (2 * stdDev);
        double pmiddleBB = series.sma(index-1 , 20, input);
        double pstdDev = series.std(index-1, 20, input);
        double pupperBB = middleBB + (2 * stdDev);
        double plowerBB = middleBB - (2 * stdDev);

        // Calculate Keltner Channels
        double middleKC = series.ema(index, 20, input);
        double range = series.atr(index, 20);
        double upperKC = middleKC + (1.5d * range);
        double lowerKC = middleKC - (1.5d * range);
        double pmiddleKC = series.ema(index-1, 20, input);
        double prange = series.atr(index-1, 20);
        double pupperKC = middleKC + (1.5d * range);
        double plowerKC = middleKC - (1.5d * range);

        if (c0R && high > kama && series.getOpen(index) < kama && phigh < kama)
        {
            series.setPriceBarColor(index, Color.yellow);
            ctx.signal(index, Signals.BOINK, "BOINK", series.getClose(index));
        }

        if (c0G && clow < kama && series.getOpen(index) > kama && plow > kama)
        {
            series.setPriceBarColor(index, Color.yellow);
            ctx.signal(index, Signals.BOINK, "BOINK", series.getClose(index));
        }

        if ((clow < lowerBB || plow < lowerBB) && body > pbody && c1R && c0G)
        {
            ctx.signal(index, Signals.ENG_BB, "ENG_BB", series.getClose(index));
            series.setPriceBarColor(index, Color.yellow);
            return;
        }

        if ((high > upperBB || phigh > upperBB) && body > pbody && c1G && c0R)
        {
            ctx.signal(index, Signals.ENG_BB, "ENG_BB", series.getClose(index));
            series.setPriceBarColor(index, Color.yellow);
            return;
        }

        //debug("upperBB = " + upperBB);
        //debug("lowerBB = " + lowerBB);
        // Check for squeeze (when BB is inside KC)
        boolean squeeze = (lowerBB > lowerKC) && (upperBB < upperKC);
        boolean psqueeze = (plowerBB > plowerKC) && (pupperBB < pupperKC);

        if (!squeeze && psqueeze) {
            series.setPriceBarColor(index, Color.white);
        }
        else {
            //series.setPriceBarColor(index, Color.white);
        }

/*
        for (int i = 1; i <= series.size() - 1; i++)
        {
            double open = series.getOpen(i);
            double high = series.getHigh(i);
            double low = series.getLow(i);
            double close = series.getClose(i);

            double bodyHeight = Math.abs(open - close);
            double tailHeight = Math.min(Math.abs(high - open), Math.abs(high - close));

            double bodyToTailHeightRatio = bodyHeight / tailHeight;

            if (bodyHeight > 0 && tailHeight > 0 && bodyToTailHeightRatio >= bodyToTailRatio)
            {
                series.setPriceBarColor(i, Color.white);
                //pinbarAlert.setValue(new Object());
                return;
            }
        }

        series.setDouble(index, Values.MA, 19680d);
        if (index == series.size() - 1)
            series.setComplete(index);
            */

    }

}
