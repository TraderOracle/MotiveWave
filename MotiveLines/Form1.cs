using static System.Windows.Forms.VisualStyles.VisualStyleElement.TrayNotify;
using System.Diagnostics;
using System.Drawing;
using System;
using System.Security.Policy;
using System.Runtime.InteropServices.JavaScript;

namespace MotiveLines
{
    public partial class frmMain : Form
    {
        private List<String> l = new List<string>();

        private String AddArrows(String s)
        {
            s = s.Replace(" FTD", "");

            if (s.Contains("daily max"))
                s = "🖤🖤  Value Area HIGH 🖤🖤  .";
            if (s.Contains("daily min"))
                s = "🖤🖤  Value Area LOW  🖤🖤  .";

            if (s.Contains("daily max"))
                s = "🖐🏿🖐🏿  Daily Max  🖐🏿🖐🏿  .";
            if (s.Contains("daily min"))
                s = "🖐🏿🖐🏿  Daily Max  🖐🏿🖐🏿  .";
            if (s.Contains("1DexpMAX"))
                s = "🕷️🐈‍  1 Day Max  🐈‍🕷️  .";
            if (s.Contains("1DexpMIN"))
                s = "🕷️🐈‍  1 Day Min  🐈‍🕷️  .";

            if (s.Contains("vix r2"))
                s = "🔥🔥  OVERBOUGHT Level 2  🔥🔥  .";
            if (s.Contains("vix r1"))
                s = "🔥  OVERBOUGHT Level 1  🔥  .";

            if (s.Contains("vix s2"))
                s = "🔥🔥  OVERSOLD Level 2  🔥🔥  .";
            if (s.Contains("vix s1"))
                s = "🔥  OVERSOLD Level 1  🔥  .";

            if (s.Contains("in the Sand") || s.Contains("HV"))
                s = "❓❓  LINE IN THE SAND  ❓❓  .";

            else if (s.Contains("SD0"))
                s = "🡇  SHORT level 1  🡇  .";
            else if (s.Contains("SD1"))
                s = "🡇🡇  SHORT level 2  🡇🡇  .";
            else if (s.Contains("SD2"))
                s = "🡇🡇🡇  SHORT level 3  🡇🡇🡇 (long if fails)   .";

            else if (s.Contains("Range Short"))
                s = "🡇  Range Short  🡇  .";
            else if (s.Contains("me Short"))
                s = "🡇🡇🡇  EXTREME Short  🡇🡇🡇  .";
            else if (s.Contains("Odds Short"))
                s = "🡇🡇  Highest Odds Short  🡇🡇  .";

            else if (s.Contains("RD0"))
                s = "🡅  LONG level 1  🡅  .";
            else if (s.Contains("RD1"))
                s = "🡅🡅  LONG level 2  🡅🡅  .";
            else if (s.Contains("RD2"))
                s = "🡅🡅🡅  LONG level 3  🡅🡅🡅 (short if fails)   .";

            else if (s.Contains("Range Long"))
                s = "🡅  Range Long  🡅  .";
            else if (s.Contains("me Long"))
                s = "🡅🡅🡅  EXTREME Long  🡅🡅🡅  .";
            else if (s.Contains("Odds Long"))
                s = "🡅🡅  Highest Odds Long  🡅🡅  .";

            return s;
        }

        public frmMain()
        {
            InitializeComponent();
        }

        private void chkSampleData_CheckedChanged(object sender, EventArgs e)
        {
            rtfKillpips.Clear();
            rtfMenthorQ.Clear();
            rtfTraderSmarts.Clear();
            rtfMancini.Clear();
        }

        private static String ToHex(System.Drawing.Color c) => $"#{c.R:X2}{c.G:X2}{c.B:X2}";

        // NQ,22556.75,Extreme Short,#ffffff,#99190e,4
        private void Addline(String sTicker, String sDesc, String sPrice, Color bc, Color c, int width)
        {
            sDesc = AddArrows(sDesc);
            c = Color.White; // default
            if (sDesc.Contains("RD0") || sDesc.Contains("RD1") || sDesc.Contains("RD2"))
            {
                bc = Color.FromArgb(255, 103, 163, 18);
                c = Color.Red;
            }
            else if (sDesc.Contains("HV"))
            {
                bc = Color.FromArgb(255, 93, 176, 51);
                c = Color.LightGoldenrodYellow;
            }
            else if (sDesc.ToLower().Contains("ange shor"))
            {
                bc = Color.FromArgb(255, 173, 76, 28);
                c = Color.Red;
            }
            else if (sDesc.ToLower().Contains("treme shor"))
            {
                bc = Color.FromArgb(255, 255, 200, 0);
                width = 2;
                c = Color.Red;
            }
            else if (sDesc.ToLower().Contains("est odds short"))
            {
                bc = Color.FromArgb(255, 247, 131, 15);
                c = Color.Red;
            }

            else if (sDesc.ToLower().Contains("ne in the san"))
            {
                bc = Color.FromArgb(255, 224, 192, 31);
                c = Color.FromArgb(255, 38, 248, 255);
                width = 2;
            }

            else if (sDesc.ToLower().Contains("hest odds long"))
            {
                bc = Color.FromArgb(255, 38, 248, 255);
                c = Color.Lime;
                width = 1;
            }
            else if (sDesc.ToLower().Contains("ange lon"))
            {
                bc = Color.FromArgb(255, 71, 192, 196);
                c = Color.Lime;
            }

            else if (sDesc.Contains("LONG lev") || sDesc.Contains("OVERBOUGHT"))
            {
                bc = Color.FromArgb(255, 122, 175, 255);
                c = Color.FromArgb(255, 122, 175, 255);
                width = 2;
            }
            else if (sDesc.Contains("SHORT lev") || sDesc.Contains("OVERSOLD"))
            {
                bc = Color.FromArgb(255, 255, 98, 0);
                c = Color.FromArgb(255, 255, 98, 0);
                width = 2;
            }

            else if (sDesc.ToLower().Contains("reme lon"))
            {
                bc = Color.FromArgb(255, 128, 255, 0);
                c = Color.Lime;
                width = 2;
            }
            else if (sDesc.ToLower().Contains("min") || sDesc.Contains("val"))
            {
                bc = Color.FromArgb(255, 93, 176, 51);
            }
            else if (sDesc.Contains("SD"))
            {
                bc = Color.DarkRed;
                c = Color.Red;
            }
            else if (sDesc.ToLower().Contains("support"))
            {
                bc = Color.FromArgb(255, 93, 176, 51);
            }
            else if (sDesc.ToLower().Contains("max") || sDesc.Contains("vah") || sDesc.ToLower().Contains("resist"))
            {
                bc = Color.DarkRed;
                c = Color.Red;
            }
            else if (sDesc.ToLower().Contains("gex"))
            {
                bc = Color.DodgerBlue;
                c = Color.FromArgb(255, 209, 209, 209);
            }
            else if (sDesc.ToLower().Contains("vix r"))
            {
                c = Color.Lime;
                bc = Color.FromArgb(255, 11, 155, 184);
                c = Color.FromArgb(255, 235, 237, 237);  // grey
            }
            else if (sDesc.ToLower().Contains("vix s"))
            {
                c = Color.Red;
                bc = Color.FromArgb(255, 212, 90, 15);
                c = Color.FromArgb(255, 235, 237, 237);  // grey
            }

            if (sPrice.Contains("-"))
            {
                l.Add(sTicker + "," + sPrice.Split('-')[0] + "," + sDesc + "," +
                    ToHex(c) + "," + ToHex(bc) + "," + width.ToString());
                l.Add(sTicker + "," + sPrice.Split('-')[1] + "," + sDesc + "," +
                    ToHex(c) + "," + ToHex(bc) + "," + width.ToString());
            }
            else
            {
                l.Add(sTicker + "," + sPrice + "," + sDesc + "," +
                    ToHex(c) + "," + ToHex(bc) + "," + width.ToString());
            }
        }

        private void ParseMQKP(TextBox rf)
        {
            String sTicker = String.Empty;
            String sL = String.Empty;
            int iC = 0;

            foreach (String s in rf.Lines)
            {
                if (s.Equals(""))
                    continue;

                sTicker = s.Split(':')[0].Replace("!", "").Replace("1", "").Replace("$", "").Trim();
                sL = s.Split(':')[1].Trim();
                // $NQ1!: VAL, 20812, range daily max, 21521, range daily min, 20776 setting 76k - 644k
                int i = sL.IndexOf(" setting ");
                if (i > 0)
                    sL = sL.Substring(0, i).Trim();
                else
                    sL = sL.Trim();

                string[] sb = sL.Split(", ");
                string price = string.Empty, desc = string.Empty;
                foreach (string sr in sb)
                {
                    if (iC % 2 != 0)
                        price = sr;
                    else
                        desc = sr;

                    if (!string.IsNullOrEmpty(price) && !string.IsNullOrEmpty(desc))
                    {
                        Addline(sTicker, desc, price, Color.BlueViolet, Color.White, 1);
                        price = string.Empty;
                        desc = string.Empty;
                    }
                    iC++;
                }
            }
        }

        private void ParseTraderSmarts()
        {
            String sTicker = String.Empty;
            String sDesc = String.Empty;
            int iLine = 0;

            foreach (String s in rtfTraderSmarts.Lines)
            {
                int commas = s.Split(',').Length - 1;

                // ​TS TradePlan for ES - Monday February 24, 2025
                if (s.Contains("TradePlan for "))
                {
                    int i = s.IndexOf('-');
                    sTicker = s.Substring(0, i).Replace("TS TradePlan for ", "").Trim();
                }

                if (sTicker != String.Empty)
                {
                    // 6226.00, 6199.50, 6186.50, 6178.75, 6170.25, 6169.50, 6167.00, 6159.75
                    if (chkTSLines.Checked && commas > 50 &&
                        (iLine == 2 || iLine == 3 || iLine == 4))
                        foreach (String t in s.Split(", "))
                            Addline(sTicker, "TS", t, Color.BlueViolet, Color.White, 1);

                    // 6178.75 - 6170.25 Extreme Short
                    if (s.Contains("Extreme Short") || s.Contains("Highest Odds Short") || s.Contains("Range Short"))
                    {
                        int ix = s.Replace(" - ", "-").IndexOf(' ');
                        sDesc = s.Substring(ix + 2, s.Length - ix - 2).Trim();
                        Addline(sTicker, sDesc, s.Replace(" - ", "-").Split(' ')[0], Color.DarkRed, Color.White, 1);
                    }
                    if (s.Contains("Extreme Long") || s.Contains("Highest Odds Long") || s.Contains("Range Long"))
                    {
                        int ix = s.Replace(" - ", "-").IndexOf(' ');
                        sDesc = s.Substring(ix + 2, s.Length - ix - 2).Trim();
                        if (sDesc.Equals("ange Long")) sDesc = "Range Long"; // lol don't hate
                        if (sDesc.Equals("xtreme Long")) sDesc = "Extreme Long"; // lol don't hate
                        Addline(sTicker, sDesc, s.Replace(" - ", "-").Split(' ')[0], Color.DarkSeaGreen, Color.White, 1);
                    }
                    if (s.Contains("Line in the Sand"))
                    {
                        int ix = s.Replace(" - ", "-").IndexOf(' ');
                        sDesc = s.Substring(ix + 1, s.Length - ix - 1).Trim();
                        Addline(sTicker, sDesc, s.Replace(" - ", "-").Split(' ')[0], Color.DeepSkyBlue, Color.White, 1);
                    }

                    // ES MTS Numbers: 6311.50, 6250.75, 6199.75, 5999.00, 5896.00, 5762.50
                    if (s.Contains("MTS Numbers: ") && chkMTSLines.Checked)
                    {
                        String yu = s.Replace(" ", "");
                        String uu = yu.Split(':')[1].Replace(" ", "").Trim();
                        foreach (String t in uu.Split(','))
                            Addline(sTicker, "MTS", t, Color.BlueViolet, Color.White, 1);
                    }
                }
                iLine++;
            }
        }

        private async void ParseMancini()
        {
            String sL = String.Empty;
            Color cc = Color.Green;

            foreach (String s in rtfMancini.Lines)
                try
                {
                    sL = s.Replace("Supports are: ", "").Replace("Resistances are: ", "").Replace(".", "").Trim();

                    // Supports are: 5528 - 33(major), 5519(major), 5511, 5508, 5502
                    if (s.ToLower().Contains("resist")) cc = Color.DarkRed;
                    string[] sb = sL.Split(", ");
                    foreach (string sr in sb)
                    {
                        string pr = sr.Replace(" ", "").Replace("(major)", "").Trim().Substring(0, 4);
                        if (pr.Contains("-"))
                            if (pr.Split("-")[1].Trim().Length == 2)
                                pr = pr.Substring(0, 2) + pr.Split("-")[1].Trim();
                            else
                                pr = pr.Split("-")[1].Trim();

                        if (sr.Contains("(major)"))
                            Addline("ES", "Mancini (major)", pr, cc, Color.White, 1);
                        else
                            Addline("ES", "Mancini", pr, cc, Color.White, 1);
                    }
                }
                catch (Exception)
                {

                }
        }

        private void btnWrite_Click(object sender, EventArgs e)
        {
            l.Clear();
            l.Add("Symbol,Price Level,Note,Foreground Color,Background Color,Diameter");

            if (chkTS.Checked)
                ParseTraderSmarts();

            if (chkKillpips.Checked)
                ParseMQKP(rtfKillpips);

            if (chkMQ.Checked)
                ParseMQKP(rtfMenthorQ);

            if (chkMancini.Checked)
                ParseMancini();

            TextWriter tw = new StreamWriter(txtOutput.Text);
            foreach (String s in l)
                tw.WriteLine(s);
            tw.Close();
        }

    }
}
