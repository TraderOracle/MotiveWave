package TraderOracle;

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

@StudyHeader(
        namespace="com.DickInTheSpleen",
        id="FairValueGap",
        rb="TraderOracle.nls.strings", // locale specific strings are loaded from here
        name="Fair Value Gap",
        label="Fair Value Gap",
        desc="Fair Value Gap",
        menu="TraderOracle",
        overlay=true,
        studyOverlay=true)

public class FairValueGap extends Study
{
    //region VARIABLES
    private static final Color RED = new Color(255, 0, 0);
    private static final Color GREEN = new Color(0, 255, 0);
    private static final Color WHITE = new Color(255, 255, 255);
    private static final Color YELLOW = new Color(255, 0, 0);

    private List<Figure> al = new ArrayList<>();
    private int pIndex = 0;
    //endregion

    //region INITIALIZE AND MISC

    private void DrawLines(DataSeries s)
    {
        PathInfo pf = new PathInfo(
                new Color(166, 200, 255, 250), 2, new float[]{3, 2, 5}, true, true, false, 0, 2);

        for (int iq = 0; iq < s.size() - 1; iq ++) {
            int index = iq;

            double low = s.getLow(index);
            double pphigh = s.getHigh(index - 2);

            // =-=-=-=-=   VOLUME IMBALANCES GREEN   =-=-=-=-=
            if (low > pphigh)
            {
                boolean bFoundEnd = false;
                Coordinate cS = new Coordinate(s.getEndTime(index-2), (float) s.getHigh(index-2));
                for (int i = index + 1; i < s.size() - 1; i++) {
                    if (s.getLow(i) < s.getLow(index)) {
                        debug("index " + index + ", lindex " + i);
                        Coordinate cE = new Coordinate(s.getStartTime(i), (float) s.getLow(index));
                        Box lx = new Box(cS, cE);
                        lx.setUnderlay(true);
                        lx.setFillColor(new Color(50, 153, 34, 148));
                        lx.setLineColor(new Color(50, 153, 34, 148));
                        this.addFigure(lx);
                        bFoundEnd = true;
                        break;
                    }
                }
                if (!bFoundEnd) {
                    int iStart = s.getStartIndex();
                    int iEnd = s.getEndIndex();
                    var cE = new Coordinate(s.getEndTime(iEnd) + 1000, (double) s.getLow(index));
                    Box lx = new Box(cS, cE, pf);
                    lx.setUnderlay(true);
                    lx.setFillColor(new Color(50, 153, 34, 158));
                    lx.setLineColor(new Color(50, 153, 34, 158));
                    //lk.setText("VolImb", new Font("Arial", Font.PLAIN, 12));
                    this.addFigure(lx);
                    bFoundEnd = true;
                }
            }
        }
    }

    @Override
    public void onSettingsUpdated(DataContext ctx)
    {
        this.clearFigures();
        DrawLines(ctx.getDataSeries());
    }

    @Override
    public void onBarClose(DataContext ctx)
    {
        this.clearFigures();
        DrawLines(ctx.getDataSeries());
    }

    @Override
    public void initialize(Defaults defaults)
    {
        var sd = createSD();
        var tab2 = sd.addTab("Candle Over Candle BB");
        var grp2 = tab2.addGroup("Inputs");
        grp2.addRow(new BooleanDescriptor("CANDLEBB", "Standard Signal", true));
    }

    //endregion

    @Override
    protected void calculate(int index, DataContext ctx)
    {
        var s = ctx.getDataSeries();
        int last = s.size() - 1;
        Object input = getSettings().getInput(Inputs.INPUT);
        if (s == null || index < 2 || !s.isBarComplete(index))
            return;

    }
}