import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.print.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

class TourFinSys extends JFrame {

    // ===================== DATA MODEL =====================
    static class Student {
        String id, name, section, discountType, orNo = "", payDate = "", payMode = "";
        double netFee, paid;
        Student(String id, String name, String section, String discountType, double netFee) {
            this.id = id; this.name = name; this.section = section;
            this.discountType = discountType; this.netFee = netFee;
        }
        String status() { return paid >= netFee ? "Paid" : "Unpaid"; }
        double balance() { return Math.max(0, netFee - paid); }
        @Override public String toString() { return id + " - " + name + " (" + section + ") [" + status() + "]"; }
    }

    static class Payment {
        LocalDate date; double amount;
        Payment(LocalDate d, double a) { date = d; amount = a; }
    }

    static final String[] SECTIONS = {"BSCS 3A", "BSCS 3B", "BSCS 3C", "BSIT 3A", "BSIT 3B"};
    static final String[] DISCOUNT_LABELS = {
            "Standard (0% Discount)", "Class Officer Subsidy (20% Off)",
            "Academic Scholar (50% Off)", "Full Subsidized (100% Free)"};
    static final double[] DISCOUNT_RATES = {0, 0.20, 0.50, 1.00};

    static final Color DARK = new Color(0x0f172a), BLUE = new Color(0x2563eb), GREEN = new Color(0x059669),
            RED = new Color(0xe11d48), SLATE = new Color(0x64748b), BG = new Color(0xf8fafc),
            LINE = new Color(0xe2e8f0);

    String tourTitle = "Educational Field Trip 2026";
    String tourDate = "Nov 25, 2026";
    double standardFee = 2500;
    int orCounter = 5;

    final List<Student> students = new ArrayList<>();
    final List<Payment> payments = new ArrayList<>();

    // ===================== UI REFERENCES =====================
    CardLayout cards = new CardLayout();
    JPanel content = new JPanel(cards);
    Map<String, JButton> navButtons = new LinkedHashMap<>();
    boolean updating = false;

    // header / sidebar
    JLabel headerTour = new JLabel(), sidebarPercent = new JLabel("0%"),
            sidebarPaid = new JLabel("0 Paid"), sidebarUnpaid = new JLabel("0 Unpaid");
    JProgressBar sidebarBar = new JProgressBar(0, 100);

    // roster
    JLabel dispTitle = new JLabel(), dispDate = new JLabel(), dispFee = new JLabel();
    JTextField idF = new JTextField(), nameF = new JTextField(), netFeeF = new JTextField();
    JComboBox<String> sectionCb = new JComboBox<>(SECTIONS), discountCb = new JComboBox<>(DISCOUNT_LABELS);
    JTextField searchF = new JTextField(14);
    JComboBox<String> statusFilter = new JComboBox<>(new String[]{"All Statuses", "Paid", "Unpaid"});
    DefaultTableModel rosterModel = nonEditable("Student ID", "Student Name", "Section", "Tour Fee", "Paid Amount", "Status");
    JTable rosterTable = new JTable(rosterModel);
    JLabel rosterCount = new JLabel();

    // payments
    JComboBox<Object> payStudentCb = new JComboBox<>();
    JLabel paySumNet = new JLabel("₱0.00"), paySumBal = new JLabel("₱0.00");
    JTextField payOr = new JTextField(), payDateF = new JTextField(LocalDate.now().toString()),
            payAmount = new JTextField(), payRemarks = new JTextField();
    JComboBox<String> payMode = new JComboBox<>(new String[]{"Cash", "GCash", "Bank Transfer"});
    JPanel receiptPanel;
    JLabel rcOr = bold(""), rcDate = bold(""), rcName = bold(""), rcIdSec = bold(""), rcMode = bold(""),
            rcDesc = bold(""), rcRemarks = new JLabel(""), rcAmount = bold(""), rcWords = bold(""), rcSign = bold("Payer Signature");

    // analytics
    JLabel kCollected = kpiValue(), kOutstanding = kpiValue(), kRatio = kpiValue(), kPercent = kpiValue(),
            kRatioSub = small(""), kTarget = small("");
    JPanel sectionBars = new JPanel();
    ChartPanel chartPanel = new ChartPanel();

    // reports
    JComboBox<String> reportType = new JComboBox<>(new String[]{"All Students", "Paid", "Unpaid"});
    JComboBox<String> reportSection = new JComboBox<>();
    DefaultTableModel reportModel = nonEditable("Student", "Section", "Status", "Balance");
    JTable reportTable = new JTable(reportModel);
    JLabel reportTitle = bold("Report Summary Preview");
    JComboBox<Object> clearCb = new JComboBox<>();
    JPanel clearancePanel;
    JLabel clrBadge = new JLabel("STATUS: UNKNOWN"), clrName = new JLabel("--"), clrIdSec = new JLabel("--"),
            clrBalance = new JLabel("₱0.00");

    // ===================== MAIN =====================
    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> new TourFinSys().setVisible(true));
    }

    TourFinSys() {
        super("Department Tour Financial & Receipting System");
        seedData();
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1280, 820);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        getContentPane().setBackground(BG);

        add(buildHeader(), BorderLayout.NORTH);
        add(buildSidebar(), BorderLayout.WEST);

        content.setBackground(BG);
        content.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        content.add(buildRosterScreen(), "roster");
        content.add(buildPaymentScreen(), "payments");
        content.add(buildAnalyticsScreen(), "analytics");
        content.add(buildReportsScreen(), "reports");
        add(content, BorderLayout.CENTER);

        refreshAll();
        switchTab("roster");
    }

    void seedData() {
        addSeed("2026-CS-0101", "Natalie D. Briones", "BSCS 3B", 0, 1, "2026-10-08", "Cash", true);
        addSeed("2026-CS-0102", "Mrincess Allisha Sombrano", "BSCS 3B", 1, 2, "2026-10-09", "GCash", true);
        addSeed("2026-CS-0103", "Maria Santos", "BSCS 3A", 0, 0, "", "", false);
        addSeed("2026-CS-0104", "Arvin Reyes", "BSCS 3A", 2, 3, "2026-10-10", "Cash", true);
        addSeed("2026-CS-0105", "Sophia Lopez", "BSCS 3C", 0, 0, "", "", false);
        addSeed("2026-IT-0201", "Christian Vance", "BSIT 3A", 0, 4, "2026-10-11", "Bank Transfer", true);
    }

    void addSeed(String id, String name, String sec, int disc, int orNum, String date, String mode, boolean paid) {
        String[] keys = {"none", "officer", "scholar", "full"};
        Student s = new Student(id, name, sec, keys[disc], standardFee * (1 - DISCOUNT_RATES[disc]));
        if (paid) {
            s.paid = s.netFee;
            s.orNo = String.format("OR-TOUR-2026-%04d", orNum);
            s.payDate = date; s.payMode = mode;
            payments.add(new Payment(LocalDate.parse(date), s.paid));
        }
        students.add(s);
    }

    // ===================== HEADER / SIDEBAR =====================
    JComponent buildHeader() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(DARK);
        p.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        JLabel brand = new JLabel("<html><span style='font-size:15px;color:white'><b>🚌 TourFinSys</b></span><br>"
                + "<span style='font-size:9px;color:#94a3b8'>Department Tour Financial System</span></html>");
        headerTour.setForeground(new Color(0xe2e8f0));
        JLabel user = new JLabel("<html><div style='text-align:right;color:#e2e8f0'><b>Finance Staff</b><br>"
                + "<span style='font-size:9px;color:#94a3b8'>Computer Science Dept</span></div></html>");
        p.add(brand, BorderLayout.WEST);
        p.add(headerTour, BorderLayout.CENTER);
        headerTour.setHorizontalAlignment(SwingConstants.CENTER);
        p.add(user, BorderLayout.EAST);
        return p;
    }

    JComponent buildSidebar() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 1, LINE), BorderFactory.createEmptyBorder(12, 10, 12, 10)));
        p.setPreferredSize(new Dimension(220, 0));

        JLabel t = new JLabel("SYSTEM MODULES");
        t.setFont(t.getFont().deriveFont(Font.BOLD, 10f));
        t.setForeground(SLATE);
        t.setAlignmentX(0);
        p.add(t);
        p.add(Box.createVerticalStrut(8));

        addNav(p, "roster", "🚌  Tour Setup & Roster");
        addNav(p, "payments", "🧾  Record Payment & Receipt");
        addNav(p, "analytics", "📊  Financial Analytics");
        addNav(p, "reports", "📋  Reports & Clearance");
        p.add(Box.createVerticalStrut(20));

        JPanel q = new JPanel(new GridLayout(0, 1, 0, 4));
        q.setBackground(DARK);
        q.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        JLabel ql = new JLabel("Quick Collection:"); ql.setForeground(new Color(0x94a3b8));
        sidebarPercent.setForeground(new Color(0x34d399));
        row.add(ql, BorderLayout.WEST); row.add(sidebarPercent, BorderLayout.EAST);
        sidebarBar.setForeground(new Color(0x10b981));
        JPanel cnt = new JPanel(new BorderLayout());
        cnt.setOpaque(false);
        sidebarPaid.setForeground(new Color(0xcbd5e1)); sidebarUnpaid.setForeground(new Color(0xcbd5e1));
        cnt.add(sidebarPaid, BorderLayout.WEST); cnt.add(sidebarUnpaid, BorderLayout.EAST);
        q.add(row); q.add(sidebarBar); q.add(cnt);
        q.setAlignmentX(0);
        q.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
        p.add(q);
        p.add(Box.createVerticalGlue());
        return p;
    }

    void addNav(JPanel p, String key, String text) {
        JButton b = new JButton(text);
        b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setContentAreaFilled(true);
        b.setBackground(Color.WHITE);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        b.setAlignmentX(0);
        b.addActionListener(e -> switchTab(key));
        navButtons.put(key, b);
        p.add(b);
        p.add(Box.createVerticalStrut(4));
    }

    void switchTab(String key) {
        cards.show(content, key);
        navButtons.forEach((k, b) -> {
            boolean on = k.equals(key);
            b.setBackground(on ? new Color(0xeff6ff) : Color.WHITE);
            b.setForeground(on ? new Color(0x1d4ed8) : new Color(0x475569));
            b.setFont(b.getFont().deriveFont(on ? Font.BOLD : Font.PLAIN));
        });
        if (key.equals("analytics")) refreshAnalytics();
    }

    // ===================== SCREEN 1: ROSTER =====================
    JComponent buildRosterScreen() {
        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setOpaque(false);

        // tour info
        JPanel info = card(new BorderLayout(12, 0));
        dispTitle.setFont(dispTitle.getFont().deriveFont(Font.BOLD, 20f));
        JPanel left = new JPanel(new GridLayout(0, 1, 0, 3));
        left.setOpaque(false);

        left.add(dispTitle);
        left.add(dispDate);
        left.add(new JLabel("Destination: Subic Tech Center & Clark Hub"));
        JPanel right = new JPanel(new GridLayout(0, 1, 0, 3));
        right.setOpaque(false);
        right.add(small("Standard Tour Fee"));
        dispFee.setFont(dispFee.getFont().deriveFont(Font.BOLD, 22f));
        dispFee.setForeground(new Color(0x1d4ed8));
        right.add(dispFee);
        JButton edit = new JButton("Edit Event");
        edit.addActionListener(e -> editTourDialog());
        right.add(edit);
        info.add(left, BorderLayout.CENTER);
        info.add(right, BorderLayout.EAST);

        // form
        JPanel form = card(new BorderLayout(0, 8));
        form.add(bold("Register Student for Tour"), BorderLayout.NORTH);
        JPanel grid = new JPanel(new GridLayout(2, 6, 8, 2));
        grid.setOpaque(false);
        for (String s : new String[]{"Student ID *", "Full Name *", "Section / Year *", "Discount Category", "Net Tour Fee (₱)", ""})
            grid.add(small(s));
        netFeeF.setEditable(false);
        netFeeF.setFont(netFeeF.getFont().deriveFont(Font.BOLD));
        JPanel btns = new JPanel(new GridLayout(1, 2, 4, 0));
        btns.setOpaque(false);
        JButton reg = new JButton("Register");
        reg.addActionListener(e -> addStudent());
        JButton clr = new JButton("Clear");
        clr.addActionListener(e -> resetStudentForm());
        btns.add(reg); btns.add(clr);
        grid.add(idF); grid.add(nameF); grid.add(sectionCb); grid.add(discountCb); grid.add(netFeeF); grid.add(btns);
        discountCb.addActionListener(e -> calcNetFee());
        sectionCb.setSelectedIndex(1);
        form.add(grid, BorderLayout.CENTER);

        JPanel top = new JPanel(new BorderLayout(0, 12));
        top.setOpaque(false);
        top.add(info, BorderLayout.NORTH);
        top.add(form, BorderLayout.CENTER);
        root.add(top, BorderLayout.NORTH);

        // table
        JPanel tablePanel = card(new BorderLayout(0, 8));
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        bar.setOpaque(false);
        bar.add(bold("Tour Student Roster"));
        bar.add(Box.createHorizontalStrut(20));
        bar.add(new JLabel("Search:")); bar.add(searchF); bar.add(statusFilter);
        JButton pay = new JButton("Record Payment");
        pay.addActionListener(e -> { Student s = selectedRosterStudent(); if (s != null) openPayment(s); });
        JButton rec = new JButton("View Receipt");
        rec.addActionListener(e -> { Student s = selectedRosterStudent(); if (s != null) openPayment(s); });
        bar.add(pay); bar.add(rec);
        searchF.getDocument().addDocumentListener(new SimpleDocListener(this::refreshRoster));
        statusFilter.addActionListener(e -> refreshRoster());
        styleTable(rosterTable);
        tablePanel.add(bar, BorderLayout.NORTH);
        tablePanel.add(new JScrollPane(rosterTable), BorderLayout.CENTER);
        tablePanel.add(rosterCount, BorderLayout.SOUTH);
        root.add(tablePanel, BorderLayout.CENTER);
        return root;
    }

    void calcNetFee() {
        int i = Math.max(0, discountCb.getSelectedIndex());
        netFeeF.setText(String.format("%.0f", standardFee * (1 - DISCOUNT_RATES[i])));
    }

    void addStudent() {
        String id = idF.getText().trim(), name = nameF.getText().trim();
        if (id.isEmpty() || name.isEmpty()) { warn("Student ID and Full Name are required."); return; }
        if (students.stream().anyMatch(s -> s.id.equalsIgnoreCase(id))) {
            warn("Student ID already exists in the tour roster!"); return;
        }
        int d = discountCb.getSelectedIndex();
        String[] keys = {"none", "officer", "scholar", "full"};
        students.add(new Student(id, name, (String) sectionCb.getSelectedItem(), keys[d],
                standardFee * (1 - DISCOUNT_RATES[d])));
        resetStudentForm();
        refreshAll();
        JOptionPane.showMessageDialog(this, "Student successfully registered!");
    }

    void resetStudentForm() {
        idF.setText(""); nameF.setText("");
        sectionCb.setSelectedIndex(1); discountCb.setSelectedIndex(0);
        calcNetFee();
    }

    Student selectedRosterStudent() {
        int r = rosterTable.getSelectedRow();
        if (r < 0) { warn("Select a student in the table first."); return null; }
        String id = (String) rosterModel.getValueAt(rosterTable.convertRowIndexToModel(r), 0);
        return students.stream().filter(s -> s.id.equals(id)).findFirst().orElse(null);
    }

    void openPayment(Student s) {
        switchTab("payments");
        payStudentCb.setSelectedItem(s);
        loadPaymentDetails();
    }

    void refreshRoster() {
        String q = searchF.getText().toLowerCase().trim();
        String f = (String) statusFilter.getSelectedItem();
        rosterModel.setRowCount(0);
        int shown = 0;
        for (Student s : students) {
            boolean m = s.name.toLowerCase().contains(q) || s.id.toLowerCase().contains(q);
            boolean fm = f.startsWith("All") || s.status().equals(f);
            if (m && fm) {
                rosterModel.addRow(new Object[]{s.id, s.name, s.section, peso(s.netFee), peso(s.paid), s.status()});
                shown++;
            }
        }
        rosterCount.setText("Showing " + shown + " of " + students.size() + " registered students");
    }

    void editTourDialog() {
        JTextField t = new JTextField(tourTitle), d = new JTextField(tourDate),
                f = new JTextField(String.format("%.0f", standardFee));
        JPanel p = new JPanel(new GridLayout(0, 1, 0, 4));
        p.add(new JLabel("Tour Title")); p.add(t);
        p.add(new JLabel("Target Date")); p.add(d);
        p.add(new JLabel("Standard Tour Fee (₱)")); p.add(f);
        if (JOptionPane.showConfirmDialog(this, p, "Edit Tour Event Details",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            try {
                standardFee = Double.parseDouble(f.getText().trim());
                tourTitle = t.getText().trim();
                tourDate = d.getText().trim();
                calcNetFee();
                refreshAll();
            } catch (NumberFormatException ex) { warn("Invalid fee amount."); }
        }
    }

    // ===================== SCREEN 2: PAYMENTS =====================
    JComponent buildPaymentScreen() {
        JPanel root = new JPanel(new GridLayout(1, 2, 12, 0));
        root.setOpaque(false);

        // form
        JPanel form = card(new BorderLayout(0, 10));
        JPanel hdr = new JPanel(new GridLayout(0, 1));
        hdr.setOpaque(false);
        JLabel h = new JLabel("Record Payment & Generate Receipt");
        h.setFont(h.getFont().deriveFont(Font.BOLD, 18f));
        hdr.add(h);
        form.add(hdr, BorderLayout.NORTH);

        JPanel f = new JPanel(new GridLayout(0, 1, 0, 3));
        f.setOpaque(false);
        f.add(small("Select Student *")); f.add(payStudentCb);
        JPanel sum = new JPanel(new GridLayout(2, 2));
        sum.setBackground(BG);
        sum.setBorder(BorderFactory.createLineBorder(LINE));
        sum.add(small(" Net Fee Required:")); sum.add(small(" Current Outstanding:"));
        paySumNet.setFont(paySumNet.getFont().deriveFont(Font.BOLD, 14f));
        paySumBal.setFont(paySumBal.getFont().deriveFont(Font.BOLD, 14f));
        paySumBal.setForeground(RED);
        sum.add(paySumNet); sum.add(paySumBal);
        f.add(sum);
        payOr.setEditable(false);
        f.add(small("Auto OR Reference No.")); f.add(payOr);
        f.add(small("Payment Date (yyyy-mm-dd)")); f.add(payDateF);
        f.add(small("Amount Tendered (₱) *")); f.add(payAmount);
        f.add(small("Payment Mode")); f.add(payMode);
        f.add(small("Remarks / Note")); f.add(payRemarks);
        form.add(f, BorderLayout.CENTER);

        JButton save = new JButton("Save & Print Official Receipt");
        save.setBackground(GREEN); save.setForeground(Color.WHITE);
        save.setFont(save.getFont().deriveFont(Font.BOLD));
        save.addActionListener(e -> processPayment());
        form.add(save, BorderLayout.SOUTH);

        payStudentCb.addActionListener(e -> { if (!updating) loadPaymentDetails(); });
        payMode.addActionListener(e -> updateReceipt());
        payAmount.getDocument().addDocumentListener(new SimpleDocListener(this::updateReceipt));
        payRemarks.getDocument().addDocumentListener(new SimpleDocListener(this::updateReceipt));
        payDateF.getDocument().addDocumentListener(new SimpleDocListener(this::updateReceipt));

        // receipt
        JPanel right = new JPanel(new BorderLayout(0, 6));
        right.setOpaque(false);
        JPanel rh = new JPanel(new BorderLayout());
        rh.setOpaque(false);
        rh.add(bold("Live Printable Receipt Preview"), BorderLayout.WEST);
        JButton pr = new JButton("Direct Print");
        pr.addActionListener(e -> printComponent(receiptPanel));
        rh.add(pr, BorderLayout.EAST);
        right.add(rh, BorderLayout.NORTH);

        receiptPanel = new JPanel();
        receiptPanel.setLayout(new BoxLayout(receiptPanel, BoxLayout.Y_AXIS));
        receiptPanel.setBackground(Color.WHITE);
        receiptPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xcbd5e1), 2), BorderFactory.createEmptyBorder(16, 20, 16, 20)));
        receiptPanel.add(centered("<html><center><span style='color:#64748b;font-size:9px'>STATE UNIVERSITY COMPUTER SCIENCE DEPARTMENT</span><br>"
                + "<b style='font-size:15px'>OFFICIAL DEPARTMENT RECEIPT</b><br>"
                + "<span style='color:#475569;font-size:9px'>Tour Fee & Collections Management Office</span></center></html>"));
        receiptPanel.add(Box.createVerticalStrut(8));
        receiptPanel.add(new JSeparator());
        receiptPanel.add(Box.createVerticalStrut(8));
        receiptPanel.add(labelRow("OR Reference No:", rcOr));
        receiptPanel.add(labelRow("Date:", rcDate));
        receiptPanel.add(Box.createVerticalStrut(8));
        receiptPanel.add(labelRow("Received From:", rcName));
        receiptPanel.add(labelRow("Student ID / Sec:", rcIdSec));
        receiptPanel.add(labelRow("Payment Mode:", rcMode));
        receiptPanel.add(Box.createVerticalStrut(8));
        receiptPanel.add(new JSeparator());
        receiptPanel.add(labelRow("Description:", rcDesc));
        receiptPanel.add(labelRow("Remarks:", rcRemarks));
        rcAmount.setFont(rcAmount.getFont().deriveFont(Font.BOLD, 16f));
        receiptPanel.add(labelRow("Amount:", rcAmount));
        receiptPanel.add(new JSeparator());
        receiptPanel.add(labelRow("Amount in Words:", rcWords));
        receiptPanel.add(Box.createVerticalStrut(30));
        JPanel sig = new JPanel(new GridLayout(1, 2, 20, 0));
        sig.setOpaque(false);
        sig.add(sigBox(rcSign, "Student / Representative"));
        sig.add(sigBox(bold("Finance Staff"), "Authorized Collector"));
        receiptPanel.add(sig);
        receiptPanel.add(Box.createVerticalStrut(10));
        receiptPanel.add(centered("<html><center><span style='color:#94a3b8;font-size:8px'>Valid as official receipt for Department Tour 2026 clearance. Computer generated.</span></center></html>"));
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.add(receiptPanel, BorderLayout.NORTH);
        right.add(new JScrollPane(wrap), BorderLayout.CENTER);

        root.add(form); root.add(right);
        return root;
    }

    void loadPaymentDetails() {
        Object o = payStudentCb.getSelectedItem();
        if (!(o instanceof Student)) {
            paySumNet.setText("₱0.00"); paySumBal.setText("₱0.00");
            payAmount.setText(""); payOr.setText("");
            updateReceipt();
            return;
        }
        Student s = (Student) o;
        paySumNet.setText(peso(s.netFee));
        paySumBal.setText(peso(s.balance()));
        if (s.status().equals("Paid")) {
            payAmount.setText("0");
            payOr.setText(s.orNo.isEmpty() ? "EX-PAID" : s.orNo);
        } else {
            payAmount.setText(String.format("%.0f", s.balance()));
            payOr.setText(String.format("OR-TOUR-2026-%04d", orCounter));
        }
        updateReceipt();
    }

    void updateReceipt() {
        Object o = payStudentCb.getSelectedItem();
        Student s = o instanceof Student ? (Student) o : null;
        double amt = parseAmount(payAmount.getText());
        String remarks = payRemarks.getText().trim().isEmpty() ? "Full tour fee payment" : payRemarks.getText().trim();
        rcOr.setText(payOr.getText().isEmpty() ? "OR-TOUR-2026-0000" : payOr.getText());
        rcDate.setText(formatDate(payDateF.getText()));
        rcName.setText(s != null ? s.name : "Select a student...");
        rcIdSec.setText(s != null ? s.id + " (" + s.section + ")" : "-- / --");
        rcSign.setText(s != null ? s.name : "Payer Signature");
        rcMode.setText((String) payMode.getSelectedItem());
        rcDesc.setText(tourTitle);
        rcRemarks.setText(remarks);
        rcAmount.setText(peso(amt));
        rcWords.setText(numberToWords((long) amt) + " Pesos Only");
    }

    void processPayment() {
        Object o = payStudentCb.getSelectedItem();
        if (!(o instanceof Student)) { warn("Please select a student!"); return; }
        Student s = (Student) o;
        double amt = parseAmount(payAmount.getText());
        if (amt <= 0) { warn("Please enter a valid payment amount."); return; }
        if (amt > s.balance() + 0.001) { warn("Amount exceeds the outstanding balance of " + peso(s.balance()) + "."); return; }
        LocalDate date;
        try { date = LocalDate.parse(payDateF.getText().trim()); }
        catch (Exception ex) { warn("Date must be in yyyy-mm-dd format."); return; }

        s.paid += amt;
        s.orNo = payOr.getText();
        s.payDate = date.toString();
        s.payMode = (String) payMode.getSelectedItem();
        payments.add(new Payment(date, amt));
        orCounter++;

        updateReceipt();                 // freeze receipt contents before refresh
        receiptPanel.revalidate();
        receiptPanel.doLayout();
        if (JOptionPane.showConfirmDialog(this, "Payment saved. Print the official receipt now?",
                "Payment Recorded", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
            printComponent(receiptPanel);

        refreshAll();
        payStudentCb.setSelectedItem(s);
        loadPaymentDetails();
    }

    // ===================== SCREEN 3: ANALYTICS =====================
    JComponent buildAnalyticsScreen() {
        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setOpaque(false);
        JPanel hdr = new JPanel(new GridLayout(0, 1));
        hdr.setOpaque(false);
        JLabel h = new JLabel("Tour Financial Analytics Dashboard");
        h.setFont(h.getFont().deriveFont(Font.BOLD, 22f));
        hdr.add(h);

        JPanel kpis = new JPanel(new GridLayout(1, 4, 10, 0));
        kpis.setOpaque(false);
        kpis.add(kpiCard("TOTAL COLLECTED", kCollected, small("Updated live")));
        kCollected.setForeground(GREEN);
        kpis.add(kpiCard("OUTSTANDING BALANCE", kOutstanding, small("Pending collections")));
        kOutstanding.setForeground(RED);
        kpis.add(kpiCard("PAID RATIO", kRatio, kRatioSub));
        kpis.add(kpiCard("COLLECTION TARGET", kPercent, kTarget));
        kPercent.setForeground(new Color(0x1d4ed8));

        JPanel north = new JPanel(new BorderLayout(0, 12));
        north.setOpaque(false);
        north.add(hdr, BorderLayout.NORTH);
        north.add(kpis, BorderLayout.CENTER);
        root.add(north, BorderLayout.NORTH);

        JPanel lower = new JPanel(new GridLayout(1, 2, 12, 0));
        lower.setOpaque(false);
        JPanel secCard = card(new BorderLayout(0, 8));
        secCard.add(bold("Collection Rate by Section"), BorderLayout.NORTH);
        sectionBars.setLayout(new BoxLayout(sectionBars, BoxLayout.Y_AXIS));
        sectionBars.setOpaque(false);
        JPanel sw = new JPanel(new BorderLayout());
        sw.setOpaque(false);
        sw.add(sectionBars, BorderLayout.NORTH);
        secCard.add(sw, BorderLayout.CENTER);
        JPanel chartCard = card(new BorderLayout(0, 8));
        chartCard.add(bold("Daily Collection Trend (Past 5 Days)"), BorderLayout.NORTH);
        chartCard.add(chartPanel, BorderLayout.CENTER);
        lower.add(secCard); lower.add(chartCard);
        root.add(lower, BorderLayout.CENTER);
        return root;
    }

    void refreshAnalytics() {
        double collected = students.stream().mapToDouble(s -> s.paid).sum();
        double target = students.stream().mapToDouble(s -> s.netFee).sum();
        long paidCount = students.stream().filter(s -> s.status().equals("Paid")).count();
        double pct = target > 0 ? collected / target * 100 : 0;

        kCollected.setText(peso(collected));
        kOutstanding.setText(peso(Math.max(0, target - collected)));
        kRatio.setText(paidCount + " / " + students.size());
        kRatioSub.setText(String.format("%.1f%% Roster Cleared", pct));
        kPercent.setText(String.format("%.1f%%", pct));
        kTarget.setText("Target: " + peso(target));

        sidebarPercent.setText(String.format("%.1f%%", pct));
        sidebarBar.setValue((int) Math.round(pct));
        sidebarPaid.setText(paidCount + " Paid");
        sidebarUnpaid.setText((students.size() - paidCount) + " Unpaid");

        sectionBars.removeAll();
        for (String sec : SECTIONS) {
            List<Student> list = students.stream().filter(s -> s.section.equals(sec)).collect(Collectors.toList());
            if (list.isEmpty()) continue;
            double c = list.stream().mapToDouble(s -> s.paid).sum();
            double t = list.stream().mapToDouble(s -> s.netFee).sum();
            int p = t > 0 ? (int) Math.round(c / t * 100) : 0;
            JPanel row = new JPanel(new BorderLayout(0, 2));
            row.setOpaque(false);
            row.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
            JPanel lbl = new JPanel(new BorderLayout());
            lbl.setOpaque(false);
            lbl.add(new JLabel(sec + " (" + list.size() + " Students)"), BorderLayout.WEST);
            JLabel pl = new JLabel(p + "% (" + peso(c) + ")");
            pl.setForeground(new Color(0x1d4ed8));
            lbl.add(pl, BorderLayout.EAST);
            JProgressBar pb = new JProgressBar(0, 100);
            pb.setValue(p);
            pb.setForeground(BLUE);
            row.add(lbl, BorderLayout.NORTH);
            row.add(pb, BorderLayout.CENTER);
            sectionBars.add(row);
        }
        sectionBars.revalidate();
        sectionBars.repaint();

        // trend: last 5 days ending at latest payment (or today)
        LocalDate end = payments.stream().map(p -> p.date).max(LocalDate::compareTo).orElse(LocalDate.now());
        chartPanel.labels.clear();
        chartPanel.values.clear();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM dd");
        for (int i = 4; i >= 0; i--) {
            LocalDate d = end.minusDays(i);
            chartPanel.labels.add(d.format(fmt));
            chartPanel.values.add(payments.stream().filter(p -> p.date.equals(d)).mapToDouble(p -> p.amount).sum());
        }
        chartPanel.repaint();
    }

    static class ChartPanel extends JPanel {
        final List<String> labels = new ArrayList<>();
        final List<Double> values = new ArrayList<>();
        ChartPanel() { setOpaque(false); setPreferredSize(new Dimension(300, 250)); }
        @Override protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight(), l = 55, b = 28, t = 15, r = 10;
            double max = values.stream().mapToDouble(Double::doubleValue).max().orElse(0);
            if (max <= 0) max = 1000;
            max = Math.ceil(max / 1000.0) * 1000;
            g.setColor(LINE);
            for (int i = 0; i <= 4; i++) {
                int y = t + (h - t - b) * i / 4;
                g.drawLine(l, y, w - r, y);
                g.setColor(SLATE);
                g.drawString(String.format("%,.0f", max * (4 - i) / 4), 5, y + 4);
                g.setColor(LINE);
            }
            int n = values.size();
            if (n == 0) { g.dispose(); return; }
            int slot = (w - l - r) / n, bw = (int) (slot * 0.55);
            for (int i = 0; i < n; i++) {
                int bh = (int) ((h - t - b) * (values.get(i) / max));
                int x = l + i * slot + (slot - bw) / 2, y = h - b - bh;
                g.setColor(BLUE);
                g.fillRoundRect(x, y, bw, Math.max(bh, 1), 8, 8);
                g.setColor(SLATE);
                FontMetrics fm = g.getFontMetrics();
                g.drawString(labels.get(i), x + (bw - fm.stringWidth(labels.get(i))) / 2, h - 10);
                if (values.get(i) > 0) {
                    String v = String.format("%,.0f", values.get(i));
                    g.drawString(v, x + (bw - fm.stringWidth(v)) / 2, y - 4);
                }
            }
            g.dispose();
        }
    }

    // ===================== SCREEN 4: REPORTS =====================
    JComponent buildReportsScreen() {
        JPanel root = new JPanel(new GridLayout(1, 2, 12, 0));
        root.setOpaque(false);

        // report generator
        JPanel rp = card(new BorderLayout(0, 8));
        JPanel top = new JPanel(new GridLayout(0, 1, 0, 3));
        top.setOpaque(false);
        top.add(bold("Masterlist Report Generator"));
        top.add(small("Report Filter Type")); top.add(reportType);
        reportSection.addItem("All Sections");
        for (String s : SECTIONS) reportSection.addItem(s);
        top.add(small("Filter Section")); top.add(reportSection);
        JButton gen = new JButton("Generate Masterlist Preview");
        gen.addActionListener(e -> generateReport());
        top.add(gen);
        rp.add(top, BorderLayout.NORTH);
        JPanel mid = new JPanel(new BorderLayout(0, 4));
        mid.setOpaque(false);
        JPanel mh = new JPanel(new BorderLayout());
        mh.setOpaque(false);
        mh.add(reportTitle, BorderLayout.WEST);
        JButton pm = new JButton("Print Masterlist");
        pm.addActionListener(e -> {
            try { reportTable.print(JTable.PrintMode.FIT_WIDTH, new java.text.MessageFormat(reportTitle.getText()), null); }
            catch (PrinterException ex) { warn("Print failed: " + ex.getMessage()); }
        });
        mh.add(pm, BorderLayout.EAST);
        styleTable(reportTable);
        mid.add(mh, BorderLayout.NORTH);
        mid.add(new JScrollPane(reportTable), BorderLayout.CENTER);
        rp.add(mid, BorderLayout.CENTER);

        // clearance
        JPanel cp = card(new BorderLayout(0, 8));
        JPanel ct = new JPanel(new GridLayout(0, 1, 0, 3));
        ct.setOpaque(false);
        ct.add(bold("Individual Tour Clearance Pass"));
        ct.add(small("Select Student to Issue Clearance"));
        ct.add(clearCb);
        clearCb.addActionListener(e -> { if (!updating) renderClearance(); });
        cp.add(ct, BorderLayout.NORTH);

        clearancePanel = new JPanel();
        clearancePanel.setLayout(new BoxLayout(clearancePanel, BoxLayout.Y_AXIS));
        clearancePanel.setBackground(DARK);
        clearancePanel.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
        JLabel univ = new JLabel("STATE UNIVERSITY CS DEPT");
        univ.setForeground(new Color(0x60a5fa)); univ.setFont(univ.getFont().deriveFont(Font.BOLD, 10f));
        JLabel title = new JLabel("OFFICIAL TOUR FINANCIAL CLEARANCE");
        title.setForeground(Color.WHITE); title.setFont(title.getFont().deriveFont(Font.BOLD, 14f));
        clrBadge.setFont(clrBadge.getFont().deriveFont(Font.BOLD, 11f));
        for (JLabel l : new JLabel[]{univ, title, clrBadge}) { l.setAlignmentX(0); clearancePanel.add(l); clearancePanel.add(Box.createVerticalStrut(4)); }
        clearancePanel.add(Box.createVerticalStrut(10));
        clearancePanel.add(darkRow("Student Name:", clrName));
        clearancePanel.add(darkRow("Student ID / Sec:", clrIdSec));
        clearancePanel.add(darkRow("Event Tag:", new JLabel(tourTitle)));
        clearancePanel.add(darkRow("Financial Balance:", clrBalance));
        clearancePanel.add(Box.createVerticalStrut(12));
        JLabel issued = new JLabel("Issued by: Tour Financial Office  |  Date: "
                + LocalDate.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")));
        issued.setForeground(new Color(0x94a3b8)); issued.setFont(issued.getFont().deriveFont(10f));
        issued.setAlignmentX(0);
        clearancePanel.add(issued);
        JPanel cw = new JPanel(new BorderLayout(0, 8));
        cw.setOpaque(false);
        cw.add(clearancePanel, BorderLayout.NORTH);
        JButton pc = new JButton("Print Clearance Pass");
        pc.addActionListener(e -> printComponent(clearancePanel));
        cw.add(pc, BorderLayout.CENTER);
        JPanel cwrap = new JPanel(new BorderLayout());
        cwrap.setOpaque(false);
        cwrap.add(cw, BorderLayout.NORTH);
        cp.add(cwrap, BorderLayout.CENTER);

        root.add(rp); root.add(cp);
        return root;
    }

    void generateReport() {
        String type = (String) reportType.getSelectedItem();
        String sec = (String) reportSection.getSelectedItem();
        reportModel.setRowCount(0);
        for (Student s : students) {
            boolean mt = type.startsWith("All") || s.status().equals(type);
            boolean ms = sec.startsWith("All") || s.section.equals(sec);
            if (mt && ms) reportModel.addRow(new Object[]{s.name, s.section, s.status(), peso(s.balance())});
        }
        reportTitle.setText("Report: " + type.toUpperCase() + " | Section: " + sec);
    }

    void renderClearance() {
        Object o = clearCb.getSelectedItem();
        if (!(o instanceof Student)) {
            clrName.setText("--"); clrIdSec.setText("--"); clrBalance.setText("₱0.00");
            clrBadge.setText("STATUS: UNKNOWN"); clrBadge.setForeground(new Color(0x94a3b8));
            return;
        }
        Student s = (Student) o;
        clrName.setText(s.name);
        clrIdSec.setText(s.id + " (" + s.section + ")");
        clrBalance.setText(peso(s.balance()));
        boolean ok = s.status().equals("Paid") && s.balance() == 0;
        clrBadge.setText(ok ? "STATUS: CLEARED" : "STATUS: UNCLEARED");
        clrBadge.setForeground(ok ? new Color(0x6ee7b7) : new Color(0xfda4af));
        clrBalance.setForeground(ok ? new Color(0x34d399) : new Color(0xfb7185));
    }

    // ===================== REFRESH =====================
    void refreshAll() {
        headerTour.setText("<html><span style='color:#60a5fa'>● Active Tour:</span> " + tourTitle
                + " &nbsp;|&nbsp; <span style='color:#34d399'>" + peso(standardFee) + "</span></html>");
        dispTitle.setText(tourTitle);
        dispDate.setText("Target Date: " + tourDate);
        dispFee.setText(peso(standardFee));
        calcNetFee();
        refreshRoster();

        updating = true;
        Object p = payStudentCb.getSelectedItem(), c = clearCb.getSelectedItem();
        payStudentCb.removeAllItems(); clearCb.removeAllItems();
        payStudentCb.addItem("-- Select Student --"); clearCb.addItem("-- Select Student --");
        for (Student s : students) { payStudentCb.addItem(s); clearCb.addItem(s); }
        if (p != null) payStudentCb.setSelectedItem(p);
        if (c != null) clearCb.setSelectedItem(c);
        updating = false;

        refreshAnalytics();
        generateReport();
        renderClearance();
        updateReceipt();
    }

    // ===================== HELPERS =====================
    static String peso(double v) { return "₱" + String.format("%,.2f", v); }

    static double parseAmount(String s) {
        try { return Double.parseDouble(s.trim()); } catch (Exception e) { return 0; }
    }

    static String formatDate(String s) {
        try { return LocalDate.parse(s.trim()).format(DateTimeFormatter.ofPattern("MMMM d, yyyy")); }
        catch (Exception e) { return "N/A"; }
    }

    static final String[] UNITS = {"", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine", "Ten",
            "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen", "Eighteen", "Nineteen"};
    static final String[] TENS = {"", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"};

    static String numberToWords(long n) {
        if (n == 0) return "Zero";
        if (n < 20) return UNITS[(int) n];
        if (n < 100) return TENS[(int) (n / 10)] + (n % 10 != 0 ? " " + UNITS[(int) (n % 10)] : "");
        if (n < 1000) return UNITS[(int) (n / 100)] + " Hundred" + (n % 100 != 0 ? " " + numberToWords(n % 100) : "");
        if (n < 1_000_000) return numberToWords(n / 1000) + " Thousand" + (n % 1000 != 0 ? " " + numberToWords(n % 1000) : "");
        return Long.toString(n);
    }

    void warn(String msg) { JOptionPane.showMessageDialog(this, msg, "Notice", JOptionPane.WARNING_MESSAGE); }

    static void printComponent(JComponent c) {
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setJobName("TourFinSys Print");
        job.setPrintable((g, pf, page) -> {
            if (page > 0) return Printable.NO_SUCH_PAGE;
            Graphics2D g2 = (Graphics2D) g;
            g2.translate(pf.getImageableX(), pf.getImageableY());
            double s = Math.min(1.0, Math.min(pf.getImageableWidth() / c.getWidth(), pf.getImageableHeight() / c.getHeight()));
            g2.scale(s, s);
            c.printAll(g2);
            return Printable.PAGE_EXISTS;
        });
        if (job.printDialog()) {
            try { job.print(); } catch (PrinterException ex) {
                JOptionPane.showMessageDialog(null, "Print failed: " + ex.getMessage());
            }
        }
    }

    static DefaultTableModel nonEditable(String... cols) {
        return new DefaultTableModel(cols, 0) { @Override public boolean isCellEditable(int r, int c) { return false; } };
    }

    static void styleTable(JTable t) {
        t.setRowHeight(24);
        t.setAutoCreateRowSorter(true);
        t.getTableHeader().setFont(t.getTableHeader().getFont().deriveFont(Font.BOLD));
        t.setSelectionBackground(new Color(0xdbeafe));
        t.setSelectionForeground(Color.BLACK);
        t.setGridColor(LINE);
    }

    static JPanel card(LayoutManager lm) {
        JPanel p = new JPanel(lm);
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(LINE), BorderFactory.createEmptyBorder(12, 14, 12, 14)));
        return p;
    }

    static JLabel bold(String s) { JLabel l = new JLabel(s); l.setFont(l.getFont().deriveFont(Font.BOLD)); return l; }
    static JLabel small(String s) { JLabel l = new JLabel(s); l.setFont(l.getFont().deriveFont(11f)); l.setForeground(SLATE); return l; }
    static JLabel kpiValue() { JLabel l = new JLabel("-"); l.setFont(l.getFont().deriveFont(Font.BOLD, 22f)); return l; }

    static JLabel badge(String s) {
        JLabel l = new JLabel(s);
        l.setFont(l.getFont().deriveFont(Font.BOLD, 10f));
        l.setForeground(new Color(0x1e40af));
        return l;
    }

    static JPanel kpiCard(String title, JLabel value, JLabel sub) {
        JPanel p = card(new GridLayout(0, 1, 0, 4));
        JLabel t = small(title);
        t.setFont(t.getFont().deriveFont(Font.BOLD, 10f));
        p.add(t); p.add(value); p.add(sub);
        return p;
    }

    static JLabel centered(String html) {
        JLabel l = new JLabel(html, SwingConstants.CENTER);
        l.setAlignmentX(Component.CENTER_ALIGNMENT);
        l.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        return l;
    }

    static JPanel labelRow(String label, JLabel value) {
        JPanel p = new JPanel(new BorderLayout(10, 0));
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 0));
        JLabel l = small(label);
        l.setPreferredSize(new Dimension(110, 16));
        p.add(l, BorderLayout.WEST);
        p.add(value, BorderLayout.CENTER);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        return p;
    }

    static JPanel darkRow(String label, JLabel value) {
        JPanel p = new JPanel(new BorderLayout(10, 0));
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 0));
        JLabel l = new JLabel(label);
        l.setForeground(new Color(0x94a3b8));
        l.setPreferredSize(new Dimension(120, 16));
        value.setForeground(value.getForeground().equals(Color.BLACK) || value.getForeground().getRGB() == new JLabel().getForeground().getRGB()
                ? Color.WHITE : value.getForeground());
        p.add(l, BorderLayout.WEST);
        p.add(value, BorderLayout.CENTER);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        p.setAlignmentX(0);
        return p;
    }

    static JPanel sigBox(JLabel name, String role) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        name.setHorizontalAlignment(SwingConstants.CENTER);
        JLabel r = small(role);
        r.setHorizontalAlignment(SwingConstants.CENTER);
        r.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, SLATE));
        p.add(name, BorderLayout.NORTH);
        p.add(Box.createVerticalStrut(24), BorderLayout.CENTER);
        p.add(r, BorderLayout.SOUTH);
        return p;
    }

    /** Tiny DocumentListener that runs one callback on any change. */
    static class SimpleDocListener implements javax.swing.event.DocumentListener {
        final Runnable r;
        SimpleDocListener(Runnable r) { this.r = r; }
        public void insertUpdate(javax.swing.event.DocumentEvent e) { r.run(); }
        public void removeUpdate(javax.swing.event.DocumentEvent e) { r.run(); }
        public void changedUpdate(javax.swing.event.DocumentEvent e) { r.run(); }
    }
}