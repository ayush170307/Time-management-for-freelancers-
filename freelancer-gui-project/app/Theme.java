package app;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.FontUIResource;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.text.AbstractDocument;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.FontMetrics;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;

/**
 * Central place for colours, fonts and styled components. The palette can be switched between
 * light and dark mode; the choice is remembered between runs.
 */
public final class Theme {

    public static Color PRIMARY;
    public static Color PRIMARY_DARK;
    public static Color SIDEBAR;
    public static Color SIDEBAR_HOVER;
    public static Color SIDEBAR_ACTIVE;
    public static Color BACKGROUND;
    public static Color CARD;
    public static Color TEXT;
    public static Color MUTED;
    public static Color BORDER;
    public static Color SUCCESS;
    public static Color WARNING;
    public static Color DANGER;
    public static Color FIELD_BG;
    public static Color FIELD_BORDER;
    public static Color ZEBRA;
    public static Color SELECTION;
    public static Color HIGHLIGHT;
    public static Color TRACK;
    public static Color SECONDARY_BG;
    public static Color SECONDARY_HOVER;
    public static Color HEADER_BG;
    public static Color THUMB;

    private static boolean dark;
    private static boolean lookAndFeelSet;

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    public static final Font BASE = new Font("Segoe UI", Font.PLAIN, 14);
    public static final Font BOLD = new Font("Segoe UI", Font.BOLD, 14);
    public static final Font SMALL_BOLD = new Font("Segoe UI", Font.BOLD, 12);
    public static final Font HEADING = new Font("Segoe UI", Font.BOLD, 22);
    public static final Font BIG_NUMBER = new Font("Segoe UI", Font.BOLD, 30);
    public static final Font TIMER = new Font("Monospaced", Font.BOLD, 36);

    static {
        try {
            dark = Preferences.userNodeForPackage(Theme.class).getBoolean("darkMode", false);
        } catch (RuntimeException e) {
            dark = false;
        }
        applyPalette();
    }

    private Theme() {
    }

    // ------------------------------------------------------------------ light / dark

    public static boolean isDark() {
        return dark;
    }

    /** Switches the palette and remembers the choice. Screens must be rebuilt afterwards. */
    public static void setDark(boolean value) {
        dark = value;
        applyPalette();
        try {
            Preferences.userNodeForPackage(Theme.class).putBoolean("darkMode", value);
        } catch (RuntimeException ignored) {
            // the preference simply is not remembered
        }
    }

    private static void applyPalette() {
        if (dark) {
            PRIMARY = new Color(14, 165, 233);
            PRIMARY_DARK = new Color(2, 132, 199);
            SIDEBAR = new Color(2, 6, 23);
            SIDEBAR_HOVER = new Color(15, 23, 42);
            SIDEBAR_ACTIVE = new Color(2, 132, 199);
            BACKGROUND = new Color(11, 17, 32);
            CARD = new Color(22, 32, 51);
            TEXT = new Color(226, 232, 240);
            MUTED = new Color(148, 163, 184);
            BORDER = new Color(42, 55, 80);
            SUCCESS = new Color(52, 211, 153);
            WARNING = new Color(251, 191, 36);
            DANGER = new Color(248, 113, 113);
            FIELD_BG = new Color(15, 23, 42);
            FIELD_BORDER = new Color(51, 65, 85);
            ZEBRA = new Color(26, 38, 60);
            SELECTION = new Color(30, 58, 95);
            HIGHLIGHT = new Color(30, 58, 95);
            TRACK = new Color(42, 55, 80);
            SECONDARY_BG = new Color(30, 41, 59);
            SECONDARY_HOVER = new Color(42, 55, 80);
            HEADER_BG = new Color(30, 41, 59);
            THUMB = new Color(71, 85, 105);
        } else {
            PRIMARY = new Color(2, 132, 199);
            PRIMARY_DARK = new Color(3, 105, 161);
            SIDEBAR = new Color(10, 25, 47);
            SIDEBAR_HOVER = new Color(24, 50, 84);
            SIDEBAR_ACTIVE = new Color(2, 132, 199);
            BACKGROUND = new Color(238, 244, 250);
            CARD = Color.WHITE;
            TEXT = new Color(15, 23, 42);
            MUTED = new Color(100, 116, 139);
            BORDER = new Color(226, 232, 240);
            SUCCESS = new Color(5, 150, 105);
            WARNING = new Color(217, 119, 6);
            DANGER = new Color(220, 38, 38);
            FIELD_BG = Color.WHITE;
            FIELD_BORDER = new Color(203, 213, 225);
            ZEBRA = new Color(248, 250, 252);
            SELECTION = new Color(224, 242, 254);
            HIGHLIGHT = new Color(224, 242, 254);
            TRACK = new Color(226, 232, 240);
            SECONDARY_BG = new Color(241, 245, 249);
            SECONDARY_HOVER = new Color(226, 232, 240);
            HEADER_BG = new Color(248, 250, 252);
            THUMB = new Color(203, 213, 225);
        }
    }

    /** Call once at start-up and again after every theme switch. */
    public static void install() {
        if (!lookAndFeelSet) {
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (Exception ignored) {
                // fall back to whatever look and feel is the default
            }
            lookAndFeelSet = true;
        }
        String[] fontKeys = {
                "Label.font", "Button.font", "TextField.font", "PasswordField.font", "ComboBox.font",
                "Table.font", "TableHeader.font", "OptionPane.messageFont", "OptionPane.buttonFont",
                "Spinner.font", "FormattedTextField.font"
        };
        for (String key : fontKeys) {
            UIManager.put(key, new FontUIResource(BASE));
        }
        UIManager.put("OptionPane.background", CARD);
        UIManager.put("Panel.background", CARD);
        UIManager.put("OptionPane.messageForeground", TEXT);
        UIManager.put("Label.foreground", TEXT);
    }

    public static Color toneColor(Badge.Tone tone) {
        switch (tone) {
            case SUCCESS:
                return SUCCESS;
            case WARNING:
                return WARNING;
            case DANGER:
                return DANGER;
            default:
                return MUTED;
        }
    }

    // ------------------------------------------------------------------ text helpers

    public static String greeting() {
        int hour = LocalTime.now().getHour();
        if (hour < 12) {
            return "Good morning";
        }
        return hour < 17 ? "Good afternoon" : "Good evening";
    }

    public static String firstName(String fullName) {
        String trimmed = fullName == null ? "" : fullName.trim();
        int space = trimmed.indexOf(' ');
        return space > 0 ? trimmed.substring(0, space) : trimmed;
    }

    public static String todayText() {
        return LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy"));
    }

    /** Mixes two colours; a null colour counts as fully transparent. t = 0 gives a, t = 1 gives b. */
    public static Color blend(Color a, Color b, float t) {
        if (a == null && b == null) {
            return null;
        }
        if (a == null) {
            return new Color(b.getRed(), b.getGreen(), b.getBlue(), Math.round(b.getAlpha() * t));
        }
        if (b == null) {
            return a;
        }
        float r = 1f - t;
        return new Color(
                Math.round(a.getRed() * r + b.getRed() * t),
                Math.round(a.getGreen() * r + b.getGreen() * t),
                Math.round(a.getBlue() * r + b.getBlue() * t));
    }

    // ------------------------------------------------------------------ inputs

    private static void paintFieldBackground(Graphics g, JComponent c) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(FIELD_BG);
        g2.fillRoundRect(0, 0, c.getWidth() - 1, c.getHeight() - 1, 14, 14);
        g2.dispose();
    }

    public static JTextField textField(int columns) {
        JTextField field = new JTextField(columns) {
            @Override
            protected void paintComponent(Graphics g) {
                paintFieldBackground(g, this);
                super.paintComponent(g);
            }
        };
        styleField(field);
        return field;
    }

    public static JPasswordField passwordField(int columns) {
        JPasswordField field = new JPasswordField(columns) {
            @Override
            protected void paintComponent(Graphics g) {
                paintFieldBackground(g, this);
                super.paintComponent(g);
            }
        };
        styleField(field);
        return field;
    }

    /** A text field that only accepts digits (and up to maxDecimals decimal places). */
    public static JTextField numericField(int columns, int maxDecimals) {
        JTextField field = textField(columns);
        ((AbstractDocument) field.getDocument()).setDocumentFilter(new NumericFilter(maxDecimals));
        return field;
    }

    /** A text field that shows grey hint text while it is empty. */
    public static JTextField searchField(final String hint) {
        JTextField field = new JTextField(20) {
            @Override
            protected void paintComponent(Graphics g) {
                paintFieldBackground(g, this);
                super.paintComponent(g);
                if (getText().isEmpty()) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                            RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    g2.setColor(new Color(148, 163, 184));
                    g2.setFont(getFont());
                    Insets in = getInsets();
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString(hint, in.left, (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
                    g2.dispose();
                }
            }
        };
        styleField(field);
        return field;
    }

    private static void styleField(final JTextField field) {
        field.setOpaque(false);
        field.setFont(BASE);
        field.setForeground(TEXT);
        field.setCaretColor(TEXT);
        field.setBorder(new RoundedBorder(FIELD_BORDER, 14, 11, 14, 11, 14));
        // the outline turns blue while you are typing in the field
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                field.setBorder(new RoundedBorder(PRIMARY, 14, 11, 14, 11, 14));
            }

            @Override
            public void focusLost(FocusEvent e) {
                field.setBorder(new RoundedBorder(FIELD_BORDER, 14, 11, 14, 11, 14));
            }
        });
    }

    /** Custom list cell look for drop-downs: padded rows and an accent-coloured selection. */
    private static class ComboRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            JLabel l = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, false);
            l.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
            l.setFont(BASE);
            l.setBackground(isSelected ? SELECTION : FIELD_BG);
            l.setForeground(TEXT);
            return l;
        }
    }

    public static <T> JComboBox<T> styleCombo(JComboBox<T> combo) {
        combo.setUI(new StyledComboBoxUI());
        combo.setOpaque(false);
        combo.setFont(BASE);
        combo.setForeground(TEXT);
        combo.setBorder(new RoundedBorder(FIELD_BORDER, 14, 1, 10, 1, 2));
        combo.setRenderer(new ComboRenderer());
        combo.setPreferredSize(new Dimension(200, 42));
        return combo;
    }

    public static JSpinner styleSpinner(JSpinner spinner) {
        spinner.setFont(BASE);
        spinner.setBorder(BorderFactory.createLineBorder(FIELD_BORDER));
        JComponent editor = spinner.getEditor();
        if (editor instanceof JSpinner.DefaultEditor) {
            JTextField tf = ((JSpinner.DefaultEditor) editor).getTextField();
            tf.setBackground(FIELD_BG);
            tf.setForeground(TEXT);
            tf.setCaretColor(TEXT);
            tf.setFont(BASE);
            tf.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        }
        return spinner;
    }

    public static void styleScroll(JScrollPane scroll) {
        scroll.getVerticalScrollBar().setUI(new ThinScrollBarUI());
        scroll.getHorizontalScrollBar().setUI(new ThinScrollBarUI());
        scroll.getVerticalScrollBar().setBackground(CARD);
        scroll.getHorizontalScrollBar().setBackground(CARD);
    }

    // ------------------------------------------------------------------ labels and cards

    public static JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setFont(BASE);
        l.setForeground(MUTED);
        return l;
    }

    public static JLabel sectionLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(SMALL_BOLD);
        l.setForeground(MUTED);
        return l;
    }

    /** A small hint line shown under a form field (validation feedback). */
    public static JLabel hintLabel() {
        JLabel l = new JLabel(" ");
        l.setFont(BASE.deriveFont(12f));
        l.setForeground(MUTED);
        return l;
    }

    /** A small caption above a component, used to build compact horizontal forms. */
    public static JPanel labeled(String caption, JComponent component) {
        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.setOpaque(false);
        p.add(sectionLabel(caption), BorderLayout.NORTH);
        p.add(component, BorderLayout.CENTER);
        return p;
    }

    /** A card with smooth rounded corners and a blurred shadow. */
    public static JPanel card() {
        return new RoundedPanel();
    }

    public static JPanel statCard(String title, JLabel valueLabel, Color accent) {
        JPanel card = card();
        card.setLayout(new BorderLayout(0, 6));
        card.add(sectionLabel(title.toUpperCase()), BorderLayout.NORTH);
        valueLabel.setFont(BIG_NUMBER);
        valueLabel.setForeground(accent);
        card.add(valueLabel, BorderLayout.CENTER);
        return card;
    }

    public static JPanel chartCard(String title, JComponent chart) {
        JPanel card = card();
        card.setLayout(new BorderLayout(0, 8));
        card.add(sectionLabel(title), BorderLayout.NORTH);
        card.add(chart, BorderLayout.CENTER);
        return card;
    }

    /** Gradient welcome banner shown at the top of the overview pages. */
    public static JPanel banner(String title, String subtitle) {
        RoundedPanel banner = new RoundedPanel();
        banner.setLayout(new GridLayout(2, 1, 0, 4));
        banner.setGradient(PRIMARY, new Color(79, 70, 229));
        banner.setPadding(20, 26, 20, 26);
        JLabel t = new JLabel(title);
        t.setFont(HEADING);
        t.setForeground(Color.WHITE);
        JLabel s = new JLabel(subtitle);
        s.setFont(BASE);
        s.setForeground(new Color(224, 242, 254));
        banner.add(t);
        banner.add(s);
        return banner;
    }

    // ------------------------------------------------------------------ forms

    public static JPanel formPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);
        return p;
    }

    /** Adds a small label with a field underneath it, stacked vertically with roomy spacing. */
    public static void addField(JPanel form, String labelText, JComponent field) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridy = form.getComponentCount();
        gbc.insets = new Insets(14, 2, 6, 0);
        form.add(sectionLabel(labelText), gbc);

        gbc.gridy = form.getComponentCount();
        gbc.insets = new Insets(0, 0, 0, 0);
        form.add(field, gbc);
    }

    /** Adds a validation hint line directly under the field that was just added. */
    public static void addHint(JPanel form, JLabel hint) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridy = form.getComponentCount();
        gbc.insets = new Insets(4, 4, 0, 0);
        form.add(hint, gbc);
    }

    /** Adds a full-width component (usually a button) with some space above it. */
    public static void addWide(JPanel form, JComponent component, int topGap) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridy = form.getComponentCount();
        gbc.insets = new Insets(topGap, 0, 0, 0);
        form.add(component, gbc);
    }

    // ------------------------------------------------------------------ tables

    public static JPanel tableCard(JTable table) {
        return tableCard(table, null, null, null);
    }

    /**
     * Styles the table and wraps it in a rounded card with a live search box.
     * Clicking a column header sorts it. When the table has no rows, a friendly empty-state
     * graphic is shown instead (if an icon kind is given).
     */
    public static JPanel tableCard(final JTable table, EmptyState.Kind kind, String emptyTitle, String emptyHint) {
        styleTable(table);

        final TableRowSorter<TableModel> sorter = new TableRowSorter<TableModel>(table.getModel());
        table.setRowSorter(sorter);

        final JTextField search = searchField("Search this table...");
        search.getDocument().addDocumentListener(new DocumentListener() {
            private void update() {
                String text = search.getText().trim();
                if (text.isEmpty()) {
                    sorter.setRowFilter(null);
                } else {
                    sorter.setRowFilter(RowFilter.<TableModel, Integer>regexFilter("(?i)" + Pattern.quote(text)));
                }
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                update();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                update();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                update();
            }
        });

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(CARD);
        styleScroll(scroll);

        final CardLayout cards = new CardLayout();
        final JPanel body = new JPanel(cards);
        body.setOpaque(false);
        body.add(scroll, "table");
        if (kind != null) {
            body.add(new EmptyState(kind, emptyTitle, emptyHint), "empty");
            final TableModel model = table.getModel();
            final Runnable update = () -> cards.show(body, model.getRowCount() == 0 ? "empty" : "table");
            model.addTableModelListener(e -> update.run());
            update.run();
        }

        JPanel searchRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        searchRow.setOpaque(false);
        searchRow.add(search);

        JPanel card = card();
        card.setLayout(new BorderLayout(0, 12));
        card.add(searchRow, BorderLayout.NORTH);
        card.add(body, BorderLayout.CENTER);
        return card;
    }

    /** Draws a Badge as a rounded coloured pill inside a table cell. */
    private static class PillCell extends JComponent {
        private String text = "";
        private Color tone = MUTED;
        private Color rowBackground = CARD;

        void update(String newText, Color newTone, Color newRowBackground) {
            this.text = newText;
            this.tone = newTone;
            this.rowBackground = newRowBackground;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setColor(rowBackground);
            g2.fillRect(0, 0, getWidth(), getHeight());

            g2.setFont(SMALL_BOLD);
            FontMetrics fm = g2.getFontMetrics();
            int pillW = fm.stringWidth(text) + 24;
            int pillH = 24;
            int x = 12;
            int y = (getHeight() - pillH) / 2;
            g2.setColor(new Color(tone.getRed(), tone.getGreen(), tone.getBlue(), dark ? 55 : 30));
            g2.fillRoundRect(x, y, pillW, pillH, pillH, pillH);
            g2.setColor(tone);
            g2.drawString(text, x + 12, y + (pillH - fm.getHeight()) / 2 + fm.getAscent());
            g2.dispose();
        }
    }

    private static void styleTable(JTable table) {
        table.setRowHeight(40);
        table.setFont(BASE);
        table.setBackground(CARD);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setFillsViewportHeight(true);
        table.setSelectionBackground(SELECTION);
        table.setSelectionForeground(TEXT);

        JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);
        header.setOpaque(true);
        header.setBackground(HEADER_BG);
        header.setPreferredSize(new Dimension(0, 40));
        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean sel,
                                                           boolean focus, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, value, false, false, row, col);
                l.setText(value == null ? "" : value.toString().toUpperCase());
                l.setBackground(HEADER_BG);
                l.setForeground(MUTED);
                l.setFont(SMALL_BOLD);
                l.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER),
                        BorderFactory.createEmptyBorder(0, 12, 0, 12)));
                return l;
            }
        });

        final PillCell pill = new PillCell();
        DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean sel,
                                                           boolean focus, int row, int col) {
                Color rowBg = sel ? SELECTION : (row % 2 == 0 ? CARD : ZEBRA);
                if (value instanceof Badge) {
                    Badge badge = (Badge) value;
                    pill.update(badge.getText(), toneColor(badge.getTone()), rowBg);
                    return pill;
                }
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, value, sel, false, row, col);
                l.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
                l.setBackground(rowBg);
                l.setForeground(TEXT);
                l.setFont(BASE);
                if (value instanceof Double) {
                    l.setText(String.format("%.2f", (Double) value));
                } else if (value instanceof LocalDateTime) {
                    l.setText(((LocalDateTime) value).format(DATE_TIME));
                }
                return l;
            }
        };
        table.setDefaultRenderer(Object.class, cellRenderer);
        table.setDefaultRenderer(Number.class, cellRenderer);
        table.setDefaultRenderer(Double.class, cellRenderer);
    }
}
