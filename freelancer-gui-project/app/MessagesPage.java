package app;

import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * Two-way messaging screen shared by freelancers and clients.
 * Shows an inbox (with unread markers) and a sent list (with read receipts).
 * Opening the page marks incoming messages as read, which the sender then sees as "Read".
 */
public class MessagesPage extends JPanel {

    private final User me;
    private final String recipientRole;
    private final IntConsumer onUnreadChanged;

    private final UserDao userDao = new UserDao();
    private final MessageDao messageDao = new MessageDao();

    private final DefaultTableModel inboxModel = Ui.readOnlyModel("From", "Message", "Received", "Status");
    private final DefaultTableModel sentModel = Ui.readOnlyModel("To", "Message", "Sent", "Receipt");

    private final JComboBox<User> recipientCombo = Theme.styleCombo(new JComboBox<User>());
    private final JTextField messageField = Theme.textField(24);
    private final RoundedButton inboxBtn = new RoundedButton("Inbox", RoundedButton.Style.SEGMENT);
    private final RoundedButton sentBtn = new RoundedButton("Sent", RoundedButton.Style.SEGMENT);
    private final CardLayout listCards = new CardLayout();
    private final JPanel listArea = new JPanel(listCards);

    /**
     * @param me              the logged-in user
     * @param recipientRole   who this user can write to ("CLIENT" for freelancers, "FREELANCER" for clients)
     * @param onUnreadChanged called with the new unread count after the inbox has been opened
     */
    public MessagesPage(User me, String recipientRole, IntConsumer onUnreadChanged) {
        super(new BorderLayout(0, 4));
        this.me = me;
        this.recipientRole = recipientRole;
        this.onUnreadChanged = onUnreadChanged;
        setOpaque(false);

        RoundedButton sendBtn = new RoundedButton("Send", RoundedButton.Style.PRIMARY);
        JPanel compose = Theme.card();
        compose.setLayout(new FlowLayout(FlowLayout.LEFT, 14, 0));
        compose.add(Theme.labeled("TO", recipientCombo));
        compose.add(Theme.labeled("MESSAGE", messageField));
        compose.add(Theme.labeled(" ", sendBtn));

        inboxBtn.addActionListener(e -> showList("inbox"));
        sentBtn.addActionListener(e -> showList("sent"));
        JPanel toggles = new JPanel(new GridLayout(1, 2, 8, 0));
        toggles.setOpaque(false);
        toggles.add(inboxBtn);
        toggles.add(sentBtn);
        JPanel toggleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        toggleRow.setOpaque(false);
        toggleRow.add(toggles);

        listArea.setOpaque(false);
        listArea.add(Theme.tableCard(new JTable(inboxModel), EmptyState.Kind.CHAT,
                "Your inbox is empty", "Messages sent to you will appear here."), "inbox");
        listArea.add(Theme.tableCard(new JTable(sentModel), EmptyState.Kind.CHAT,
                "Nothing sent yet", "Write a message above to get started."), "sent");

        JPanel center = new JPanel(new BorderLayout(0, 6));
        center.setOpaque(false);
        center.add(toggleRow, BorderLayout.NORTH);
        center.add(listArea, BorderLayout.CENTER);

        add(compose, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);

        sendBtn.addActionListener(e -> send());
        messageField.addActionListener(e -> send());
        showList("inbox");
    }

    private void showList(String name) {
        listCards.show(listArea, name);
        inboxBtn.setActive("inbox".equals(name));
        sentBtn.setActive("sent".equals(name));
    }

    /** Reloads everything, then marks the inbox as read. */
    public void refresh() {
        try {
            User previous = (User) recipientCombo.getSelectedItem();
            int previousId = previous == null ? -1 : previous.getId();
            recipientCombo.removeAllItems();
            for (User candidate : userDao.findByRole(recipientRole)) {
                recipientCombo.addItem(candidate);
                if (candidate.getId() == previousId) {
                    recipientCombo.setSelectedItem(candidate);
                }
            }

            List<Message> inbox = messageDao.findReceivedBy(me.getId());
            int unread = 0;
            for (Message msg : inbox) {
                if (msg.getReadAt() == null) {
                    unread++;
                }
            }
            Ui.fill(inboxModel, inbox, m -> new Object[]{
                    m.getSenderName(), m.getContent(), m.getSentAt(), Badge.ofInbox(m)});

            List<Message> sent = messageDao.findSentBy(me.getId());
            Ui.fill(sentModel, sent, m -> new Object[]{
                    m.getReceiverName(), m.getContent(), m.getSentAt(), Badge.ofReceipt(m)});

            inboxBtn.setText(unread > 0 ? "Inbox (" + unread + " new)" : "Inbox");
            if (unread > 0) {
                messageDao.markAllRead(me.getId());
            }
            onUnreadChanged.accept(0);
        } catch (DatabaseException ex) {
            Ui.error(this, ex.getMessage());
        }
    }

    private void send() {
        User to = (User) recipientCombo.getSelectedItem();
        if (to == null) {
            Ui.error(this, "There is nobody to send a message to yet.");
            return;
        }
        try {
            Session.requireUser(me.getId());
            String text = Validator.requireText(messageField.getText(), "Message");
            messageDao.save(new Message(0, me.getId(), to.getId(), text, LocalDateTime.now()));
            messageField.setText("");
            Ui.toast(this, "Message sent to " + to.getName());
            refresh();
            showList("sent");
        } catch (InvalidInputException | DatabaseException ex) {
            Ui.error(this, ex.getMessage());
        }
    }
}
