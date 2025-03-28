package TraderOracle;

import java.awt.*;
import java.awt.Graphics2D;
import java.awt.geom.*;
import java.util.*;
import java.util.List;
import java.io.*;
import java.net.*;
import org.json.*;
import javax.net.ssl.SSLContext;
import javax.net.ssl.HttpsURLConnection;
import com.motivewave.platform.sdk.common.*;
import com.motivewave.platform.sdk.common.desc.*;
import com.motivewave.platform.sdk.common.menu.*;
import com.motivewave.platform.sdk.study.*;
import com.motivewave.platform.sdk.draw.*;

@StudyHeader(
        namespace="com.Mycompany",
        id="TONews",
        rb="TraderOracle.nls.strings", // locale specific strings are loaded from here
        name="Forex Factory News",
        label="Forex Factory News",
        desc="Forex Factory News",
        menu="TraderOracle",
        overlay=true,
        studyOverlay=true,
        signals=true)

public class TONews extends Study
{
    //region VARIABLES
    enum Values {UP, DOWN, TREND}

    private static final Color RED = new Color(255, 0, 0);
    private static final Color GREEN = new Color(0, 255, 0);
    private static final Color WHITE = new Color(255, 255, 255);
    private static final Color YELLOW = new Color(255, 0, 0);
    private boolean bDrawn = false;
    private Coordinate cS;
    private Coordinate cE;
    private int currIndex = 0;
    private String sMsg = "";
    //endregion

    public static String generateRandomString(int length) {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder randomString = new StringBuilder();
        Random random = new Random();

        for (int i = 0; i < length; i++) {
            int index = random.nextInt(characters.length());
            randomString.append(characters.charAt(index));
        }

        return randomString.toString();
    }

    private class Box extends Figure
    {
        @Override
        public void draw(Graphics2D gc, DrawContext ctx)
        {
            try {
                Rectangle rect = gc.getClipBounds();
                Font font = new Font("Dialog", Font.PLAIN, 14);
                gc.setFont(font);
                gc.setColor(WHITE);
                gc.drawString(sMsg, 150, 200);
                gc.drawLine(100, 100, 500, 500);
                gc.drawString("Rectangle = " + cS + ", " + cE, 450, 30);
            } catch (java.lang.Exception e) {
                debug("Error: " + e);
                bDrawn = false;
            }
        }
    }

    //region HTTP
    public String getHTML(String urlToRead) throws Exception {
        StringBuilder result = new StringBuilder();
        URL url = new URL(urlToRead);
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
        conn.addRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:221.0) Gecko/20100101 Firefox/31.0");
        conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9");
        conn.setRequestProperty("Accept-Encoding", "gzip,deflate,sdch");
        conn.setConnectTimeout(100000);
        conn.setInstanceFollowRedirects(true);
        conn.setReadTimeout(100000);
        conn.setRequestMethod("GET");
        conn.connect();
        int responseCode = conn.getResponseCode();
        debug("Response Code: " + responseCode);
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream()))) {
            for (String line; (line = reader.readLine()) != null; ) {
                result.append(line);
            }
            reader.close();
        }
        conn.disconnect();
        return result.toString();
    }
    //endregion

    public void FetchTheNews()
    {
        debug("FetchTheNews");
        try{
            String res = getHTML("https://www.forexfactory.com/calendar?day=today");
            debug("10");
            int iJSONStart = res.indexOf("window.calendarComponentStates[1] = ");
            int iJSONEnd = res.indexOf("\"}]}],", iJSONStart);
            String jsonString = res.substring(iJSONStart, iJSONEnd - iJSONStart);
            jsonString = jsonString.replace("window.calendarComponentStates[1] = ", "");
            jsonString += "\"}]}]}";

            debug("jsonString: " + jsonString);

            JSONObject obj = new JSONObject(jsonString);
            JSONArray arr = obj.getJSONArray("days"); // notice that `"posts": [...]`
            for (int i = 0; i < arr.length(); i++)
            {
                String name = arr.getJSONObject(i).getString("name");
                String impactTitle = arr.getJSONObject(i).getString("impactTitle");
                String timeLabel = arr.getJSONObject(i).getString("timeLabel");
                String actual = arr.getJSONObject(i).getString("actual");
                debug("Name: " + name + ", Title: " + impactTitle);
            }

        } catch (Exception e) {}
    }

    @Override
    public void onSettingsUpdated(DataContext ctx)
    {
        bDrawn = false;
        //FetchTheNews();
    }

    @Override
    public void onBarClose(DataContext ctx)
    {
        TripleSuperTrend(ctx.getDataSeries(), 11, 2);
        sMsg = generateRandomString(50);
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

    private void TripleSuperTrend(DataSeries series, int period, double mult)
    {
        int index = series.getEndIndex();

        Double atr = series.atr(index, period);
        if (atr != null) {
            float hl2 = (series.getHigh(index) + series.getLow(index)) / 2.0F;
            double up = (double)hl2 - mult * atr;
            double down = (double)hl2 + mult * atr;
            Double vUp = series.getDouble(index - 1, Values.UP);
            Double vDown = series.getDouble(index - 1, Values.DOWN);
            Double vTrend = series.getDouble(index - 1, Values.TREND);
            float close = series.getClose(index);
            float pclose = series.getClose(index - 1);
            if (vUp != null && (double)pclose > vUp) {
                up = Util.max(up, vUp);
            }

            if (vDown != null && (double)pclose < vDown) {
                down = Util.min(new double[]{down, vDown});
            }

            double trend = (double)1.0F;
            if (vDown != null && (double)close > vDown) {
                trend = (double)1.0F;
            } else if (vUp != null && (double)close < vUp) {
                trend = (double)-1.0F;
            } else if (vTrend != null) {
                trend = vTrend;
            }

            series.setDouble(index, Values.TREND, trend);
            series.setDouble(index, Values.UP, up);
            series.setDouble(index, Values.DOWN, down);
            boolean upNewTrend = vTrend != null && trend != vTrend;
            boolean lastBar = series.isBarComplete(index);
            if (trend > (double)0.0F) {
                //debug("mb");
                if (upNewTrend)
                    debug("BUY");
            } else {
                //debug("ms");
                if (upNewTrend)
                    debug("SELL");
            }
        }
    }

    @Override
    protected void calculate(int index, DataContext ctx)
    {
        var series = ctx.getDataSeries();

        cS = new Coordinate(series.getVisibleStartTime(), 5700d);
        cE = new Coordinate(series.getVisibleEndTime(), 5700d);
        if (bDrawn == false)
        {
            Box bx = new Box();
            addFigure(bx);
            bDrawn = true;
        }
    }

}