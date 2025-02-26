namespace MotiveLines
{
    partial class frmMain
    {
        /// <summary>
        ///  Required designer variable.
        /// </summary>
        private System.ComponentModel.IContainer components = null;

        /// <summary>
        ///  Clean up any resources being used.
        /// </summary>
        /// <param name="disposing">true if managed resources should be disposed; otherwise, false.</param>
        protected override void Dispose(bool disposing)
        {
            if (disposing && (components != null))
            {
                components.Dispose();
            }
            base.Dispose(disposing);
        }

        #region Windows Form Designer generated code

        /// <summary>
        ///  Required method for Designer support - do not modify
        ///  the contents of this method with the code editor.
        /// </summary>
        private void InitializeComponent()
        {
            System.ComponentModel.ComponentResourceManager resources = new System.ComponentModel.ComponentResourceManager(typeof(frmMain));
            this.label1 = new Label();
            this.txtOutput = new TextBox();
            this.btnWrite = new Button();
            this.colorDialog1 = new ColorDialog();
            this.chkTS = new CheckBox();
            this.chkKillpips = new CheckBox();
            this.chkMQ = new CheckBox();
            this.chkTSLines = new CheckBox();
            this.chkMTSLines = new CheckBox();
            this.chkSampleData = new CheckBox();
            this.rtfTraderSmarts = new TextBox();
            this.rtfKillpips = new TextBox();
            this.rtfMenthorQ = new TextBox();
            this.SuspendLayout();
            // 
            // label1
            // 
            this.label1.AutoSize = true;
            this.label1.Location = new Point(22, 21);
            this.label1.Name = "label1";
            this.label1.Size = new Size(85, 20);
            this.label1.TabIndex = 0;
            this.label1.Text = "Output File:";
            // 
            // txtOutput
            // 
            this.txtOutput.Location = new Point(113, 18);
            this.txtOutput.Name = "txtOutput";
            this.txtOutput.Size = new Size(345, 27);
            this.txtOutput.TabIndex = 1;
            this.txtOutput.Text = "c:\\temp\\MotiveLines.csv";
            // 
            // btnWrite
            // 
            this.btnWrite.Location = new Point(475, 17);
            this.btnWrite.Name = "btnWrite";
            this.btnWrite.Size = new Size(94, 29);
            this.btnWrite.TabIndex = 2;
            this.btnWrite.Text = "Write File";
            this.btnWrite.UseVisualStyleBackColor = true;
            this.btnWrite.Click += this.btnWrite_Click;
            // 
            // chkTS
            // 
            this.chkTS.AutoSize = true;
            this.chkTS.Location = new Point(30, 69);
            this.chkTS.Name = "chkTS";
            this.chkTS.Size = new Size(168, 24);
            this.chkTS.TabIndex = 3;
            this.chkTS.Text = "Output TraderSmarts";
            this.chkTS.UseVisualStyleBackColor = true;
            // 
            // chkKillpips
            // 
            this.chkKillpips.AutoSize = true;
            this.chkKillpips.Checked = true;
            this.chkKillpips.CheckState = CheckState.Checked;
            this.chkKillpips.Location = new Point(26, 212);
            this.chkKillpips.Name = "chkKillpips";
            this.chkKillpips.Size = new Size(167, 24);
            this.chkKillpips.TabIndex = 5;
            this.chkKillpips.Text = "Output Killpips Lines";
            this.chkKillpips.UseVisualStyleBackColor = true;
            // 
            // chkMQ
            // 
            this.chkMQ.AutoSize = true;
            this.chkMQ.Checked = true;
            this.chkMQ.CheckState = CheckState.Checked;
            this.chkMQ.Location = new Point(22, 377);
            this.chkMQ.Name = "chkMQ";
            this.chkMQ.Size = new Size(148, 24);
            this.chkMQ.TabIndex = 7;
            this.chkMQ.Text = "Output MenthorQ";
            this.chkMQ.UseVisualStyleBackColor = true;
            // 
            // chkTSLines
            // 
            this.chkTSLines.AutoSize = true;
            this.chkTSLines.Checked = true;
            this.chkTSLines.CheckState = CheckState.Checked;
            this.chkTSLines.Location = new Point(607, 101);
            this.chkTSLines.Name = "chkTSLines";
            this.chkTSLines.Size = new Size(133, 24);
            this.chkTSLines.TabIndex = 9;
            this.chkTSLines.Text = "Include TS lines";
            this.chkTSLines.UseVisualStyleBackColor = true;
            // 
            // chkMTSLines
            // 
            this.chkMTSLines.AutoSize = true;
            this.chkMTSLines.Checked = true;
            this.chkMTSLines.CheckState = CheckState.Checked;
            this.chkMTSLines.Location = new Point(607, 131);
            this.chkMTSLines.Name = "chkMTSLines";
            this.chkMTSLines.Size = new Size(146, 24);
            this.chkMTSLines.TabIndex = 10;
            this.chkMTSLines.Text = "Include MTS lines";
            this.chkMTSLines.UseVisualStyleBackColor = true;
            // 
            // chkSampleData
            // 
            this.chkSampleData.AutoSize = true;
            this.chkSampleData.Location = new Point(607, 20);
            this.chkSampleData.Name = "chkSampleData";
            this.chkSampleData.Size = new Size(175, 24);
            this.chkSampleData.TabIndex = 13;
            this.chkSampleData.Text = "Remove Sample Data";
            this.chkSampleData.UseVisualStyleBackColor = true;
            this.chkSampleData.CheckedChanged += this.chkSampleData_CheckedChanged;
            // 
            // rtfTraderSmarts
            // 
            this.rtfTraderSmarts.Location = new Point(21, 99);
            this.rtfTraderSmarts.Multiline = true;
            this.rtfTraderSmarts.Name = "rtfTraderSmarts";
            this.rtfTraderSmarts.ScrollBars = ScrollBars.Vertical;
            this.rtfTraderSmarts.Size = new Size(564, 91);
            this.rtfTraderSmarts.TabIndex = 17;
            this.rtfTraderSmarts.Text = resources.GetString("rtfTraderSmarts.Text");
            this.rtfTraderSmarts.TextChanged += this.rtfTraderSmarts_TextChanged;
            // 
            // rtfKillpips
            // 
            this.rtfKillpips.Location = new Point(22, 242);
            this.rtfKillpips.Multiline = true;
            this.rtfKillpips.Name = "rtfKillpips";
            this.rtfKillpips.ScrollBars = ScrollBars.Vertical;
            this.rtfKillpips.Size = new Size(564, 113);
            this.rtfKillpips.TabIndex = 18;
            this.rtfKillpips.Text = resources.GetString("rtfKillpips.Text");
            // 
            // rtfMenthorQ
            // 
            this.rtfMenthorQ.Location = new Point(22, 407);
            this.rtfMenthorQ.Multiline = true;
            this.rtfMenthorQ.Name = "rtfMenthorQ";
            this.rtfMenthorQ.ScrollBars = ScrollBars.Vertical;
            this.rtfMenthorQ.Size = new Size(564, 149);
            this.rtfMenthorQ.TabIndex = 19;
            this.rtfMenthorQ.Text = resources.GetString("rtfMenthorQ.Text");
            // 
            // frmMain
            // 
            this.AutoScaleDimensions = new SizeF(8F, 20F);
            this.AutoScaleMode = AutoScaleMode.Font;
            this.ClientSize = new Size(810, 580);
            this.Controls.Add(this.rtfMenthorQ);
            this.Controls.Add(this.rtfKillpips);
            this.Controls.Add(this.rtfTraderSmarts);
            this.Controls.Add(this.chkSampleData);
            this.Controls.Add(this.chkMTSLines);
            this.Controls.Add(this.chkTSLines);
            this.Controls.Add(this.chkMQ);
            this.Controls.Add(this.chkKillpips);
            this.Controls.Add(this.chkTS);
            this.Controls.Add(this.btnWrite);
            this.Controls.Add(this.txtOutput);
            this.Controls.Add(this.label1);
            this.MaximizeBox = false;
            this.Name = "frmMain";
            this.StartPosition = FormStartPosition.CenterScreen;
            this.Text = "MotiveLines - version 1.0";
            this.Load += this.frmMain_Load;
            this.ResumeLayout(false);
            this.PerformLayout();
        }

        #endregion

        private Label label1;
        private TextBox txtOutput;
        private Button btnWrite;
        private ColorDialog colorDialog1;
        private CheckBox chkTS;
        private CheckBox chkKillpips;
        private CheckBox chkMQ;
        private CheckBox chkTSLines;
        private CheckBox chkMTSLines;
        private CheckBox chkSampleData;
        private TextBox rtfTraderSmarts;
        private TextBox rtfKillpips;
        private TextBox rtfMenthorQ;
    }
}
