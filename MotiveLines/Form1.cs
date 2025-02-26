using static System.Windows.Forms.VisualStyles.VisualStyleElement.TrayNotify;
using System.Diagnostics;
using System.Drawing;
using System;

namespace MotiveLines
{
    public partial class frmMain : Form
    {
        private List<String> l = new List<string>();

        public frmMain()
        {
            InitializeComponent();
        }

        private void chkSampleData_CheckedChanged(object sender, EventArgs e)
        {
            rtfKillpips.Clear();
            rtfMenthorQ.Clear();
            rtfTraderSmarts.Clear();
        }

        private static String ToHex(System.Drawing.Color c) => $"#{c.R:X2}{c.G:X2}{c.B:X2}";

        // NQ,22556.75,Extreme Short,#ffffff,#99190e,4
        private void Addline(String sTicker, String sDesc, String sPrice, Color bc, Color c, int width)
        {
            c = Color.White; // default
            if (sDesc.Contains("range") || sDesc.Contains("HV"))
            {
                bc = Color.LawnGreen; c = Color.Black;
            }
            if (sDesc.ToLower().Contains("min") || sDesc.Contains("VAL"))
            {
                bc = Color.Lime; c = Color.Black;
            }
            else if (sDesc.Contains("RD"))
            {
                bc = Color.DodgerBlue;
            }
            else if (sDesc.Contains("SD"))
            {
                bc = Color.Indigo;
            }
            else if (sDesc.ToLower().Contains("support"))
            {
                bc = Color.Lime; c = Color.Black;
            }
            else if (sDesc.ToLower().Contains("max") || sDesc.Contains("VAH") || sDesc.ToLower().Contains("resist"))
            {
                bc = Color.DarkRed;
            }
            else if (sDesc.ToLower().Contains("vix") || sDesc.ToLower().Contains("gex"))
            {
                bc = Color.DodgerBlue;
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
                        Addline(sTicker, desc, price, Color.BlueViolet, Color.White, 3);
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
                            Addline(sTicker, "TS", t, Color.BlueViolet, Color.White, 3);

                    // 6178.75 - 6170.25 Extreme Short
                    if (s.Contains("Extreme Short") || s.Contains("Highest Odds Short") || s.Contains("Range Short"))
                    {
                        int ix = s.Replace(" - ", "-").IndexOf(' ');
                        sDesc = s.Substring(ix + 2, s.Length - ix - 2).Trim();
                        Addline(sTicker, sDesc, s.Replace(" - ", "-").Split(' ')[0], Color.DarkRed, Color.White, 3);
                    }
                    if (s.Contains("Extreme Long") || s.Contains("Highest Odds Long") || s.Contains("Range Long"))
                    {
                        int ix = s.Replace(" - ", "-").IndexOf(' ');
                        sDesc = s.Substring(ix + 2, s.Length - ix - 2).Trim();
                        Addline(sTicker, sDesc, s.Replace(" - ", "-").Split(' ')[0], Color.DarkSeaGreen, Color.White, 3);
                    }

                    // ES MTS Numbers: 6311.50, 6250.75, 6199.75, 5999.00, 5896.00, 5762.50
                    if (s.Contains("MTS Numbers: ") && chkMTSLines.Checked)
                        foreach (String t in s.Replace("MTS Numbers: ", "").Split(", "))
                            Addline(sTicker, "MTS", t, Color.CadetBlue, Color.White, 3);
                }
                iLine++;
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

            TextWriter tw = new StreamWriter(txtOutput.Text);
            foreach (String s in l)
                tw.WriteLine(s);
            tw.Close();

            MessageBox.Show("Export complete!");
        }

        private void frmMain_Load(object sender, EventArgs e)
        {

        }

        private void rtfTraderSmarts_TextChanged(object sender, EventArgs e)
        {

        }
    }
}
