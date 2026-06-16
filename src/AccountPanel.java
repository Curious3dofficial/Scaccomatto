import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.Border;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.prefs.Preferences;

public final class AccountPanel extends JPanel {
    private static final float AUTH_SCALE = 1.183216f; // sqrt(1.4): 1.4x total area.
    private static final int AUTH_WIDTH = scaled(560);
    private static final int TOP_SPACE = scaled(80);
    private static final int AUTH_HEIGHT = scaled(600);
    private static final int FIELD_HEIGHT = scaled(36);
    private static final int CONTROL_WIDTH = scaled(480);

    private static final Color GOLD = new Color(231, 178, 62);
    private static final Color GOLD_BRIGHT = new Color(255, 211, 103);
    private static final Color TEXT = new Color(244, 238, 224);
    private static final Color MUTED = new Color(190, 158, 110);
    private static final Color ERROR = new Color(255, 107, 107);

    private static int scaled(int value) {
        return Math.round(value * AUTH_SCALE);
    }

    private static float scaled(float value) {
        return value * AUTH_SCALE;
    }

    private static final String REMEMBER_TOKEN_KEY = "rememberToken";

    private final AccountApiClient api = new AccountApiClient();
    private final CardLayout cards = new CardLayout();
    private final JPanel cardHost = new JPanel(cards);
    private final JLabel status = new JLabel(" ", SwingConstants.CENTER);
    private final Preferences preferences = Preferences.userNodeForPackage(AccountPanel.class);
    private Image kingImage;

    public AccountPanel(MainMenu host) {
        setLayout(new BorderLayout());
        setOpaque(false);
        setPreferredSize(new Dimension(AUTH_WIDTH, AUTH_HEIGHT));
        kingImage = loadImage("/assets/pieces/wk.png");

        cardHost.setOpaque(false);
        cardHost.add(createAuthPanel(), "AUTH");
        cardHost.add(createProfilePanel(), "PROFILE");
        add(cardHost, BorderLayout.CENTER);

        status.setFont(UiFonts.uiRegular(scaled(13f)));
        status.setForeground(MUTED);
        attemptSilentRemember();
        showCurrentState();
    }

    private JComponent createAuthPanel() {
        CardLayout authCards = new CardLayout();
        JPanel forms = new JPanel(authCards);
        forms.setOpaque(false);
        forms.add(createLoginForm(), "LOGIN");
        forms.add(createRegisterForm(), "REGISTER");

        AuthTabButton signInTab = new AuthTabButton("SIGN IN");
        AuthTabButton registerTab = new AuthTabButton("CREATE ACCOUNT");
        signInTab.setSelectedTab(true);
        JPanel tabs = new JPanel(new GridLayout(1, 2, scaled(18), 0));
        tabs.setOpaque(false);
        tabs.setBorder(new EmptyBorder(0, scaled(20), scaled(3), scaled(20)));
        tabs.add(signInTab);
        tabs.add(registerTab);

        signInTab.addActionListener(event -> {
            signInTab.setSelectedTab(true);
            registerTab.setSelectedTab(false);
            authCards.show(forms, "LOGIN");
        });
        registerTab.addActionListener(event -> {
            signInTab.setSelectedTab(false);
            registerTab.setSelectedTab(true);
            authCards.show(forms, "REGISTER");
        });

        JPanel holder = new AuthStage();
        holder.setLayout(new BorderLayout(0, scaled(7)));
        holder.setBorder(new EmptyBorder(
                scaled(66) + TOP_SPACE,
                scaled(24),
                scaled(10),
                scaled(24)));
        holder.add(tabs, BorderLayout.NORTH);
        holder.add(forms, BorderLayout.CENTER);
        return holder;
    }

    private JComponent createLoginForm() {
        JPanel form = formPanel();
        JTextField login = styledTextField(
                "person",
                "Enter your username or email");
        JPasswordField password = styledPasswordField(
                "lock",
                "Enter your password");
        JButton submit = styledButton("Sign In", true);

        addField(form, "USERNAME OR EMAIL", login);
        addField(form, "PASSWORD", password);

        JPanel options = new JPanel(new BorderLayout());
        options.setOpaque(false);
        options.setAlignmentX(Component.LEFT_ALIGNMENT);
        options.setMaximumSize(new Dimension(Integer.MAX_VALUE, scaled(32)));
        JCheckBox remember = new JCheckBox("Remember me");
        remember.setOpaque(false);
        remember.setFocusPainted(false);
        remember.setFont(UiFonts.uiRegular(scaled(13f)));
        remember.setForeground(new Color(225, 198, 139));
        remember.setIcon(new CheckBoxIcon());
        remember.setSelectedIcon(new CheckBoxIcon(true));
        JLabel forgot = new JLabel("<html><u>Forgot password?</u></html>");
        forgot.setFont(UiFonts.uiRegular(scaled(13f)));
        forgot.setForeground(GOLD_BRIGHT);
        options.add(remember, BorderLayout.WEST);
        options.add(forgot, BorderLayout.EAST);
        form.add(options);
        form.add(Box.createVerticalStrut(scaled(9)));

        submit.setMaximumSize(new Dimension(Integer.MAX_VALUE, scaled(44)));
        submit.setPreferredSize(new Dimension(CONTROL_WIDTH, scaled(44)));
        form.add(submit);
        form.add(Box.createVerticalStrut(scaled(10)));

        JComponent divider = new OrDivider();
        divider.setAlignmentX(Component.CENTER_ALIGNMENT);
        form.add(divider);
        form.add(Box.createVerticalStrut(scaled(5)));

        JPanel socials = new JPanel(new FlowLayout(FlowLayout.CENTER, scaled(34), 0));
        socials.setOpaque(false);
        socials.setAlignmentX(Component.LEFT_ALIGNMENT);
        socials.setMaximumSize(new Dimension(Integer.MAX_VALUE, scaled(52)));
        socials.add(socialButton(SocialProvider.GOOGLE));
        socials.add(socialButton(SocialProvider.DISCORD));
        socials.add(socialButton(SocialProvider.STEAM));
        form.add(socials);
        form.add(Box.createVerticalGlue());

        Runnable action = () -> {
            char[] secret = password.getPassword();
            runRequest(
                    submit,
                    () -> api.login(login.getText().trim(), secret, remember.isSelected()),
                    session -> {
                        Arrays.fill(secret, '\0');
                        password.setText("");
                        AccountSession.set(session);
                        if (remember.isSelected() && session.rememberToken() != null) {
                            saveRememberToken(session.rememberToken());
                        } else {
                            clearRememberToken();
                        }
                        showProfile(session.profile());
                        setStatus("Signed in successfully.", false);
                    },
                    () -> Arrays.fill(secret, '\0'));
        };
        submit.addActionListener(event -> action.run());
        password.addActionListener(event -> action.run());
        return form;
    }

    private JComponent createRegisterForm() {
        JPanel form = formPanel();
        JTextField profileName = styledTextField("person", "Your profile name");
        JTextField username = styledTextField("at", "Choose a username");
        JTextField email = styledTextField("mail", "Enter your email");
        JPasswordField password = styledPasswordField("lock", "10+ character password");
        JPasswordField confirm = styledPasswordField("lock", "Confirm your password");
        JButton submit = styledButton("Create Account", true);

        addCompactField(form, "PROFILE NAME", profileName);
        addCompactField(form, "USERNAME", username);
        addCompactField(form, "EMAIL", email);
        addCompactField(form, "PASSWORD (10+ CHARACTERS)", password);
        addCompactField(form, "CONFIRM PASSWORD", confirm);
        form.add(Box.createVerticalStrut(scaled(5)));
        submit.setMaximumSize(new Dimension(Integer.MAX_VALUE, scaled(42)));
        submit.setPreferredSize(new Dimension(CONTROL_WIDTH, scaled(42)));
        form.add(submit);

        Runnable action = () -> {
            char[] secret = password.getPassword();
            char[] confirmation = confirm.getPassword();
            if (!Arrays.equals(secret, confirmation)) {
                Arrays.fill(secret, '\0');
                Arrays.fill(confirmation, '\0');
                setStatus("Passwords do not match.", true);
                return;
            }
            runRequest(
                    submit,
                    () -> api.register(
                            profileName.getText().trim(),
                            username.getText().trim(),
                            email.getText().trim(),
                            secret),
                    session -> {
                        Arrays.fill(secret, '\0');
                        Arrays.fill(confirmation, '\0');
                        password.setText("");
                        confirm.setText("");
                        AccountSession.set(session);
                        showProfile(session.profile());
                        setStatus("Account created. Welcome to Scaccomatto.", false);
                    },
                    () -> {
                        Arrays.fill(secret, '\0');
                        Arrays.fill(confirmation, '\0');
                    });
        };
        submit.addActionListener(event -> action.run());
        confirm.addActionListener(event -> action.run());
        return form;
    }

    private JComponent createProfilePanel() {
        JPanel profile = centeredCard();
        profile.setLayout(new BorderLayout(0, 18));
        profile.putClientProperty("profile_root", Boolean.TRUE);
        return center(profile, scaled(540), AUTH_HEIGHT);
    }

    private void showCurrentState() {
        AccountApiClient.Session session = AccountSession.get();
        if (session == null) {
            cards.show(cardHost, "AUTH");
        } else {
            showProfile(session.profile());
        }
    }

    private void attemptSilentRemember() {
        if (AccountSession.isSignedIn()) return;
        String rememberToken = preferences.get(REMEMBER_TOKEN_KEY, "");
        if (rememberToken.isBlank()) return;

        try {
            AccountApiClient.Session session = api.remember(rememberToken);
            AccountSession.set(session);
            if (session.rememberToken() != null && !session.rememberToken().isBlank()) {
                saveRememberToken(session.rememberToken());
            } else {
                clearRememberToken();
            }
        } catch (AccountApiClient.ApiException exception) {
            clearRememberToken();
        } catch (IOException exception) {
            // The account server may be offline. Keep the token locally for a later attempt.
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private void saveRememberToken(String token) {
        preferences.put(REMEMBER_TOKEN_KEY, token);
    }

    private void clearRememberToken() {
        preferences.remove(REMEMBER_TOKEN_KEY);
    }

    private void showProfile(AccountApiClient.Profile profile) {
        JPanel wrapper = (JPanel) cardHost.getComponent(1);
        JPanel card = (JPanel) wrapper.getComponent(0);
        card.removeAll();

        JPanel identity = new JPanel();
        identity.setOpaque(false);
        identity.setLayout(new BoxLayout(identity, BoxLayout.Y_AXIS));
        JLabel avatar = new JLabel(profile.profileName().substring(0, 1).toUpperCase(), SwingConstants.CENTER);
        avatar.setFont(UiFonts.title(42f));
        avatar.setForeground(GOLD_BRIGHT);
        avatar.setBorder(BorderFactory.createLineBorder(GOLD, 2, true));
        avatar.setPreferredSize(new Dimension(86, 86));
        avatar.setMinimumSize(new Dimension(86, 86));
        avatar.setMaximumSize(new Dimension(86, 86));
        avatar.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel name = new JLabel(profile.profileName());
        name.setFont(UiFonts.title(30f));
        name.setForeground(TEXT);
        name.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel profileNameLabel = new JLabel("PROFILE NAME");
        profileNameLabel.setFont(UiFonts.subtext(12f));
        profileNameLabel.setForeground(GOLD);
        profileNameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel handle = new JLabel(profile.email());
        handle.setFont(UiFonts.subtext(15f));
        handle.setForeground(MUTED);
        handle.setAlignmentX(Component.CENTER_ALIGNMENT);
        identity.add(avatar);
        identity.add(Box.createVerticalStrut(14));
        identity.add(profileNameLabel);
        identity.add(Box.createVerticalStrut(3));
        identity.add(name);
        identity.add(Box.createVerticalStrut(5));
        identity.add(handle);
        card.add(identity, BorderLayout.NORTH);

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));

        JPanel stats = new JPanel(new GridLayout(1, 4, 12, 0));
        stats.setOpaque(false);
        stats.setMaximumSize(new Dimension(Integer.MAX_VALUE, 84));
        stats.add(stat("RATING", profile.rating()));
        stats.add(stat("WINS", profile.wins()));
        stats.add(stat("LOSSES", profile.losses()));
        stats.add(stat("DRAWS", profile.draws()));
        body.add(stats);
        body.add(Box.createVerticalStrut(20));

        JLabel profileNameHeading = sectionLabel("PROFILE NAME");
        JLabel profileNameHelp = helpLabel("Change this any time. This is the name other players see.");
        body.add(profileNameHeading);
        body.add(Box.createVerticalStrut(2));
        body.add(profileNameHelp);
        body.add(Box.createVerticalStrut(7));

        JPanel profileNameRow = new JPanel();
        profileNameRow.setOpaque(false);
        profileNameRow.setLayout(new BoxLayout(profileNameRow, BoxLayout.X_AXIS));
        JTextField displayName = styledTextField();
        displayName.setText(profile.profileName());
        displayName.setMaximumSize(new Dimension(Integer.MAX_VALUE, FIELD_HEIGHT));
        JButton save = styledButton("Save Profile Name", true);
        profileNameRow.add(displayName);
        profileNameRow.add(Box.createHorizontalStrut(10));
        profileNameRow.add(save);
        body.add(profileNameRow);
        body.add(Box.createVerticalStrut(20));

        JLabel usernameHeading = sectionLabel("USERNAME");
        JLabel usernameHelp = helpLabel(
                "Current: @" + profile.username()
                        + "  |  Changes require a code sent to " + profile.email());
        body.add(usernameHeading);
        body.add(Box.createVerticalStrut(2));
        body.add(usernameHelp);
        body.add(Box.createVerticalStrut(7));

        JPanel usernameRow = new JPanel();
        usernameRow.setOpaque(false);
        usernameRow.setLayout(new BoxLayout(usernameRow, BoxLayout.X_AXIS));
        JTextField newUsername = styledTextField();
        newUsername.setText(profile.username());
        newUsername.setMaximumSize(new Dimension(Integer.MAX_VALUE, FIELD_HEIGHT));
        JButton sendCode = styledButton("Send Email Code", false);
        usernameRow.add(newUsername);
        usernameRow.add(Box.createHorizontalStrut(10));
        usernameRow.add(sendCode);
        body.add(usernameRow);
        body.add(Box.createVerticalStrut(9));

        JPanel otpRow = new JPanel();
        otpRow.setOpaque(false);
        otpRow.setLayout(new BoxLayout(otpRow, BoxLayout.X_AXIS));
        JTextField otpCode = styledTextField();
        otpCode.setToolTipText("Enter the six-digit code");
        otpCode.setMaximumSize(new Dimension(Integer.MAX_VALUE, FIELD_HEIGHT));
        otpCode.setEnabled(false);
        JButton verifyCode = styledButton("Verify Username", true);
        verifyCode.setEnabled(false);
        otpRow.add(otpCode);
        otpRow.add(Box.createHorizontalStrut(10));
        otpRow.add(verifyCode);
        body.add(otpRow);
        card.add(body, BorderLayout.CENTER);

        JButton logout = styledButton("Log Out", false);
        logout.setForeground(ERROR);
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        footer.setOpaque(false);
        footer.add(logout);
        card.add(footer, BorderLayout.SOUTH);

        save.addActionListener(event -> {
            AccountApiClient.Session current = AccountSession.get();
            if (current == null) return;
            runRequest(
                    save,
                    () -> api.updateProfileName(current.token(), displayName.getText().trim()),
                    updated -> {
                        AccountSession.set(new AccountApiClient.Session(
                                current.token(),
                                current.expiresAt(),
                                updated,
                                current.rememberToken()));
                        showProfile(updated);
                        setStatus("Profile updated.", false);
                    },
                    null);
        });
        sendCode.addActionListener(event -> {
            AccountApiClient.Session current = AccountSession.get();
            if (current == null) return;
            runRequest(
                    sendCode,
                    () -> api.requestUsernameChange(
                            current.token(),
                            newUsername.getText().trim()),
                    delivery -> {
                        otpCode.setEnabled(true);
                        verifyCode.setEnabled(true);
                        otpCode.requestFocusInWindow();
                        String message = "Verification code sent to " + delivery.email() + ".";
                        if ("development_outbox".equals(delivery.delivery())) {
                            message += " Local code: server/data/otp-outbox.log";
                        }
                        setStatus(message, false);
                    },
                    null);
        });
        Runnable verifyUsername = () -> {
            AccountApiClient.Session current = AccountSession.get();
            if (current == null) return;
            runRequest(
                    verifyCode,
                    () -> api.verifyUsernameChange(
                            current.token(),
                            otpCode.getText().trim()),
                    updated -> {
                        AccountSession.set(new AccountApiClient.Session(
                                current.token(),
                                current.expiresAt(),
                                updated,
                                current.rememberToken()));
                        showProfile(updated);
                        setStatus("Username changed to @" + updated.username() + ".", false);
                    },
                    null);
        };
        verifyCode.addActionListener(event -> verifyUsername.run());
        otpCode.addActionListener(event -> verifyUsername.run());
        logout.addActionListener(event -> {
            AccountApiClient.Session current = AccountSession.get();
            if (current == null) return;
            String storedToken = preferences.get(REMEMBER_TOKEN_KEY, "");
            runRequest(
                    logout,
                    () -> {
                        if (!storedToken.isBlank()) {
                            api.deleteRememberToken(storedToken);
                        }
                        api.logout(current.token());
                        return Boolean.TRUE;
                    },
                    ignored -> {
                        AccountSession.clear();
                        clearRememberToken();
                        cards.show(cardHost, "AUTH");
                        setStatus("Signed out.", false);
                    },
                    null);
        });

        card.revalidate();
        card.repaint();
        cards.show(cardHost, "PROFILE");
    }

    private JLabel sectionLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(UiFonts.title(15f));
        label.setForeground(GOLD_BRIGHT);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private JLabel helpLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(UiFonts.subtext(13f));
        label.setForeground(MUTED);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private JComponent stat(String label, int value) {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(177, 127, 54, 170), 1, true),
                new EmptyBorder(14, 10, 14, 10)));
        JLabel valueLabel = new JLabel(String.valueOf(value));
        valueLabel.setFont(UiFonts.title(28f));
        valueLabel.setForeground(GOLD_BRIGHT);
        valueLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel nameLabel = new JLabel(label);
        nameLabel.setFont(UiFonts.subtext(13f));
        nameLabel.setForeground(MUTED);
        nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(valueLabel);
        panel.add(Box.createVerticalStrut(4));
        panel.add(nameLabel);
        return panel;
    }

    private JPanel centeredCard() {
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D g2 = (Graphics2D) graphics.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(2, 9, 12, 232));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 28, 28);
                g2.setColor(new Color(217, 159, 55, 210));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 28, 28);
                g2.dispose();
                super.paintComponent(graphics);
            }
        };
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(26, 34, 26, 34));
        return panel;
    }

    private JComponent center(JComponent component, int width, int height) {
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setOpaque(false);
        component.setPreferredSize(new Dimension(width, height));
        wrapper.add(component);
        return wrapper;
    }

    private JPanel formPanel() {
        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBorder(new EmptyBorder(
                scaled(6),
                scaled(16),
                scaled(4),
                scaled(16)));
        return form;
    }

    private void addField(JPanel form, String labelText, JComponent field) {
        JLabel label = new JLabel(labelText);
        label.setFont(UiFonts.subtext(scaled(13f)));
        label.setForeground(GOLD_BRIGHT);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(label);
        form.add(Box.createVerticalStrut(scaled(3)));
        form.add(field);
        form.add(Box.createVerticalStrut(scaled(7)));
    }

    private void addCompactField(JPanel form, String labelText, JComponent field) {
        JLabel label = new JLabel(labelText);
        label.setFont(UiFonts.subtext(scaled(12f)));
        label.setForeground(GOLD_BRIGHT);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, FIELD_HEIGHT));
        field.setPreferredSize(new Dimension(CONTROL_WIDTH, FIELD_HEIGHT));
        form.add(label);
        form.add(Box.createVerticalStrut(scaled(3)));
        form.add(field);
        form.add(Box.createVerticalStrut(scaled(4)));
    }

    private JTextField styledTextField() {
        return styledTextField("", "");
    }

    private JTextField styledTextField(String icon, String placeholder) {
        JTextField field = new RoyalTextField(icon, placeholder);
        styleField(field, icon, false);
        return field;
    }

    private JPasswordField styledPasswordField() {
        return styledPasswordField("", "");
    }

    private JPasswordField styledPasswordField(String icon, String placeholder) {
        JPasswordField field = new RoyalPasswordField(icon, placeholder);
        styleField(field, icon, true);
        return field;
    }

    private void styleField(JTextField field, String icon, boolean password) {
        field.setFont(UiFonts.subtext(scaled(15f)));
        field.setForeground(TEXT);
        field.setCaretColor(GOLD_BRIGHT);
        field.setSelectionColor(new Color(139, 91, 28));
        field.setSelectedTextColor(Color.WHITE);
        field.setBackground(new Color(2, 8, 10, 238));
        field.setOpaque(false);
        field.setBorder(new RoyalFieldBorder(icon, password));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, FIELD_HEIGHT));
        field.setPreferredSize(new Dimension(CONTROL_WIDTH, FIELD_HEIGHT));
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent event) {
                field.repaint();
            }

            @Override
            public void focusLost(FocusEvent event) {
                field.repaint();
            }
        });
    }

    private JButton styledButton(String text, boolean primary) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D g2 = (Graphics2D) graphics.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int arc = primary ? 14 : 5;
                if (primary) {
                    boolean pressed = getModel().isPressed();
                    boolean rollover = getModel().isRollover();
                    Color edge = pressed
                            ? new Color(117, 70, 10)
                            : new Color(143, 87, 12);
                    Color center = rollover
                            ? new Color(255, 206, 83)
                            : new Color(232, 171, 48);
                    g2.setPaint(new LinearGradientPaint(
                            0,
                            0,
                            getWidth(),
                            0,
                            new float[] { 0f, 0.18f, 0.5f, 0.82f, 1f },
                            new Color[] { edge, new Color(189, 123, 20), center,
                                    new Color(189, 123, 20), edge }));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);

                    g2.setPaint(new GradientPaint(
                            0, 0, new Color(255, 235, 157, pressed ? 35 : 125),
                            0, getHeight(), new Color(75, 34, 0, 115)));
                    g2.fillRoundRect(2, 2, getWidth() - 4, getHeight() - 4, arc - 2, arc - 2);

                    g2.setColor(new Color(255, 221, 111, 215));
                    g2.setStroke(new BasicStroke(1.1f));
                    g2.drawRoundRect(3, 3, getWidth() - 7, getHeight() - 7, arc - 3, arc - 3);

                    if (getWidth() >= 300) {
                        FontMetrics metrics = g2.getFontMetrics(getFont());
                        int textWidth = metrics.stringWidth(getText());
                        int centerX = getWidth() / 2;
                        int ornamentGap = 34;
                        int diamondOffset = textWidth / 2 + ornamentGap;
                        int y = getHeight() / 2;
                        int leftDiamond = centerX - diamondOffset;
                        int rightDiamond = centerX + diamondOffset;
                        int lineLength = Math.min(45, Math.max(18, (getWidth() - textWidth) / 2 - 55));

                        g2.setColor(new Color(45, 24, 2, 205));
                        g2.setStroke(new BasicStroke(1f));
                        g2.drawLine(leftDiamond - lineLength, y, leftDiamond - 6, y);
                        g2.drawLine(rightDiamond + 6, y, rightDiamond + lineLength, y);
                        g2.fillPolygon(
                                new int[] { leftDiamond, leftDiamond + 5, leftDiamond, leftDiamond - 5 },
                                new int[] { y - 5, y, y + 5, y },
                                4);
                        g2.fillPolygon(
                                new int[] { rightDiamond, rightDiamond + 5, rightDiamond, rightDiamond - 5 },
                                new int[] { y - 5, y, y + 5, y },
                                4);
                    }
                } else {
                    g2.setColor(getModel().isRollover()
                            ? new Color(36, 29, 17, 235)
                            : new Color(3, 9, 11, 225));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
                }
                g2.setColor(getModel().isRollover() ? GOLD_BRIGHT : GOLD);
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
                g2.dispose();
                super.paintComponent(graphics);
            }
        };
        button.setText(primary ? text.toUpperCase() : text.toUpperCase());
        button.setFont(primary
                ? UiFonts.titleSemiBold(scaled(21f))
                : UiFonts.subtext(scaled(14f)));
        button.setForeground(primary ? new Color(24, 13, 3) : TEXT);
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        button.setBorder(new EmptyBorder(
                scaled(10),
                scaled(20),
                scaled(10),
                scaled(20)));
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        return button;
    }

    private JButton socialButton(SocialProvider provider) {
        JButton button = styledButton("", false);
        button.setIcon(loadSocialIcon(provider));
        button.setToolTipText("Continue with " + provider.label);
        button.getAccessibleContext().setAccessibleName("Continue with " + provider.label);
        Dimension size = new Dimension(scaled(52), scaled(52));
        button.setPreferredSize(size);
        button.setMinimumSize(size);
        button.setMaximumSize(size);
        button.addActionListener(event ->
                setStatus(provider.label + " sign-in is not configured yet.", false));
        return button;
    }

    private Icon loadSocialIcon(SocialProvider provider) {
        java.net.URL url = AccountPanel.class.getResource(provider.resource);
        if (url == null) return null;
        Image source = new ImageIcon(url).getImage();
        int iconSize = provider == SocialProvider.GOOGLE
                ? scaled(28)
                : provider == SocialProvider.DISCORD ? scaled(37) : scaled(34);
        if (provider == SocialProvider.DISCORD) {
            try {
                java.awt.image.BufferedImage image = ImageIO.read(url);
                source = image.getSubimage(10, 10, image.getWidth() - 20, image.getHeight() - 20);
            } catch (Exception ignored) {
                // Keep the complete source image if the optional crop cannot be read.
            }
        }
        Image image = source.getScaledInstance(
                iconSize,
                iconSize,
                Image.SCALE_SMOOTH);
        return new ImageIcon(image);
    }

    private Image loadImage(String resource) {
        java.net.URL url = AccountPanel.class.getResource(resource);
        if (url == null) return null;
        ImageIcon source = new ImageIcon(url);
        int width = source.getIconWidth();
        int height = source.getIconHeight();
        if (width <= 0 || height <= 0) return source.getImage();
        java.awt.image.BufferedImage tinted = new java.awt.image.BufferedImage(
                width,
                height,
                java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = tinted.createGraphics();
        g2.drawImage(source.getImage(), 0, 0, null);
        g2.setComposite(AlphaComposite.SrcIn);
        g2.setPaint(new GradientPaint(
                0, 0, new Color(255, 221, 121),
                0, height, new Color(172, 101, 20)));
        g2.fillRect(0, 0, width, height);
        g2.dispose();
        return tinted;
    }

    private final class AuthStage extends JPanel {
        AuthStage() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

            int cardY = 0;
            g2.setPaint(new GradientPaint(
                    0, cardY, new Color(6, 11, 12, 247),
                    0, getHeight(), new Color(2, 7, 9, 247)));
            g2.fillRoundRect(0, cardY, getWidth(), getHeight() - cardY, 30, 30);
            g2.setColor(new Color(211, 146, 38, 190));
            g2.setStroke(new BasicStroke(1.35f));
            g2.drawRoundRect(1, cardY + 1, getWidth() - 3, getHeight() - cardY - 3, 30, 30);

            int centerX = getWidth() / 2;
            int centerY = scaled(76);
            for (int radius : new int[] { scaled(47), scaled(39), scaled(32) }) {
                g2.setColor(new Color(
                        231,
                        171,
                        58,
                        radius == scaled(47) ? 165 : 120));
                g2.drawOval(centerX - radius, centerY - radius, radius * 2, radius * 2);
            }
            g2.setColor(new Color(236, 179, 64, 170));
            for (int angle = 0; angle < 360; angle += 45) {
                double radians = Math.toRadians(angle);
                int x = centerX + (int) Math.round(Math.cos(radians) * scaled(48));
                int y = centerY + (int) Math.round(Math.sin(radians) * scaled(48));
                Polygon diamond = new Polygon(
                        new int[] { x, x + scaled(4), x, x - scaled(4) },
                        new int[] { y - scaled(6), y, y + scaled(6), y },
                        4);
                g2.drawPolygon(diamond);
            }
            if (kingImage != null) {
                g2.drawImage(
                        kingImage,
                        centerX - scaled(27),
                        centerY - scaled(30),
                        scaled(54),
                        scaled(54),
                        this);
            }
            g2.dispose();
            super.paintComponent(graphics);
        }
    }

    private static final class AuthTabButton extends JButton {
        private boolean selectedTab;

        AuthTabButton(String text) {
            super(text);
            setFont(UiFonts.trackedTitle(20f, 0.065f));
            setForeground(MUTED);
            setContentAreaFilled(false);
            setBorder(new EmptyBorder(
                    scaled(8),
                    scaled(7),
                    scaled(13),
                    scaled(7)));
            setFocusPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        void setSelectedTab(boolean selected) {
            selectedTab = selected;
            setForeground(selected ? GOLD_BRIGHT : new Color(151, 130, 99));
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            if (!selectedTab) return;
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int y = getHeight() - 6;
            g2.setPaint(new GradientPaint(
                    0, y, new Color(201, 133, 30, 30),
                    getWidth() / 2f, y, GOLD_BRIGHT,
                    true));
            g2.setStroke(new BasicStroke(2f));
            g2.drawLine(12, y, getWidth() - 12, y);
            int x = getWidth() / 2;
            g2.setColor(GOLD_BRIGHT);
            g2.fillPolygon(
                    new int[] { x, x + 6, x, x - 6 },
                    new int[] { y - 6, y, y + 6, y },
                    4);
            g2.dispose();
        }
    }

    private static class RoyalTextField extends JTextField {
        private final String placeholder;

        RoyalTextField(String icon, String placeholder) {
            this.placeholder = placeholder;
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            paintRoyalFieldBackground(graphics, this);
            super.paintComponent(graphics);
            if (getText().isEmpty() && !isFocusOwner() && !placeholder.isEmpty()) {
                paintPlaceholder(graphics, this, placeholder);
            }
        }
    }

    private static final class RoyalPasswordField extends JPasswordField {
        private final String placeholder;
        private final char maskedEchoChar;
        private boolean passwordVisible;

        RoyalPasswordField(String icon, String placeholder) {
            this.placeholder = placeholder;
            maskedEchoChar = getEchoChar();

            MouseAdapter eyeListener = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent event) {
                    if (isEnabled()
                            && SwingUtilities.isLeftMouseButton(event)
                            && isOverEye(event.getX(), event.getY())) {
                        requestFocusInWindow();
                        setPasswordVisible(true);
                        event.consume();
                    }
                }

                @Override
                public void mouseReleased(MouseEvent event) {
                    setPasswordVisible(false);
                }

                @Override
                public void mouseExited(MouseEvent event) {
                    setCursor(Cursor.getDefaultCursor());
                    setPasswordVisible(false);
                }

                @Override
                public void mouseMoved(MouseEvent event) {
                    setCursor(isEnabled() && isOverEye(event.getX(), event.getY())
                            ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                            : Cursor.getDefaultCursor());
                }

                @Override
                public void mouseDragged(MouseEvent event) {
                    boolean leftButtonDown = (event.getModifiersEx()
                            & MouseEvent.BUTTON1_DOWN_MASK) != 0;
                    setPasswordVisible(leftButtonDown
                            && isEnabled()
                            && isOverEye(event.getX(), event.getY()));
                }
            };
            addMouseListener(eyeListener);
            addMouseMotionListener(eyeListener);
            addFocusListener(new FocusAdapter() {
                @Override
                public void focusLost(FocusEvent event) {
                    setPasswordVisible(false);
                }
            });
        }

        private boolean isOverEye(int x, int y) {
            return x >= getWidth() - scaled(58)
                    && x < getWidth()
                    && y >= 0
                    && y < getHeight();
        }

        private void setPasswordVisible(boolean visible) {
            if (passwordVisible == visible) return;
            passwordVisible = visible;
            setEchoChar(visible ? (char) 0 : maskedEchoChar);
            repaint();
        }

        boolean isPasswordVisible() {
            return passwordVisible;
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            paintRoyalFieldBackground(graphics, this);
            super.paintComponent(graphics);
            if (getPassword().length == 0 && !isFocusOwner() && !placeholder.isEmpty()) {
                paintPlaceholder(graphics, this, placeholder);
            }
        }
    }

    private static void paintRoyalFieldBackground(Graphics graphics, JComponent component) {
        Graphics2D g2 = (Graphics2D) graphics.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(new Color(2, 8, 10, 244));
        g2.fillRoundRect(0, 0, component.getWidth(), component.getHeight(), 16, 16);
        g2.dispose();
    }

    private static void paintPlaceholder(
            Graphics graphics,
            JTextField field,
            String placeholder) {
        Graphics2D g2 = (Graphics2D) graphics.create();
        g2.setFont(field.getFont());
        g2.setColor(new Color(174, 143, 88));
        FontMetrics metrics = g2.getFontMetrics();
        g2.drawString(
                placeholder,
                field.getInsets().left,
                (field.getHeight() - metrics.getHeight()) / 2 + metrics.getAscent());
        g2.dispose();
    }

    private static final class RoyalFieldBorder implements Border {
        private final String icon;
        private final boolean password;

        RoyalFieldBorder(String icon, boolean password) {
            this.icon = icon == null ? "" : icon;
            this.password = password;
        }

        @Override
        public Insets getBorderInsets(Component component) {
            return new Insets(
                    scaled(7),
                    icon.isEmpty() ? scaled(16) : scaled(64),
                    scaled(7),
                    password ? scaled(52) : scaled(16));
        }

        @Override
        public boolean isBorderOpaque() {
            return false;
        }

        @Override
        public void paintBorder(
                Component component,
                Graphics graphics,
                int x,
                int y,
                int width,
                int height) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            boolean focused = component.isFocusOwner();
            g2.setColor(focused ? GOLD_BRIGHT : new Color(177, 119, 36));
            g2.setStroke(new BasicStroke(focused ? 1.8f : 1.2f));
            g2.drawRoundRect(x, y, width - 1, height - 1, 16, 16);
            if (!icon.isEmpty()) {
                g2.setColor(focused ? GOLD_BRIGHT : new Color(218, 164, 69));
                int iconX = x + scaled(32);
                int iconY = y + height / 2;
                g2.setStroke(new BasicStroke(
                        scaled(2.2f),
                        BasicStroke.CAP_ROUND,
                        BasicStroke.JOIN_ROUND));
                if ("person".equals(icon)) {
                    g2.fillOval(
                            iconX - scaled(6),
                            iconY - scaled(15),
                            scaled(12),
                            scaled(12));
                    g2.fillRoundRect(
                            iconX - scaled(11),
                            iconY,
                            scaled(22),
                            scaled(14),
                            scaled(7),
                            scaled(7));
                } else if ("mail".equals(icon)) {
                    g2.drawRoundRect(
                            iconX - scaled(13),
                            iconY - scaled(10),
                            scaled(26),
                            scaled(20),
                            scaled(3),
                            scaled(3));
                    g2.drawLine(iconX - scaled(12), iconY - scaled(8), iconX, iconY + scaled(1));
                    g2.drawLine(iconX + scaled(12), iconY - scaled(8), iconX, iconY + scaled(1));
                } else if ("at".equals(icon)) {
                    g2.setFont(UiFonts.subtext(scaled(23f)));
                    FontMetrics metrics = g2.getFontMetrics();
                    g2.drawString("@", iconX - metrics.stringWidth("@") / 2, iconY + metrics.getAscent() / 2);
                } else {
                    g2.drawRoundRect(
                            iconX - scaled(10),
                            iconY - scaled(4),
                            scaled(20),
                            scaled(16),
                            scaled(3),
                            scaled(3));
                    g2.drawArc(
                            iconX - scaled(7),
                            iconY - scaled(14),
                            scaled(14),
                            scaled(17),
                            0,
                            180);
                    g2.fillOval(
                            iconX - scaled(2),
                            iconY + scaled(2),
                            scaled(4),
                            scaled(5));
                }
            }
            if (password) {
                boolean revealing = component instanceof RoyalPasswordField
                        && ((RoyalPasswordField) component).isPasswordVisible();
                g2.setColor(revealing ? GOLD_BRIGHT : new Color(190, 155, 91));
                int eyeX = x + width - scaled(38);
                int eyeY = y + height / 2;
                if (revealing) {
                    g2.setColor(new Color(231, 178, 62, 42));
                    g2.fillOval(
                            eyeX - scaled(16),
                            eyeY - scaled(12),
                            scaled(32),
                            scaled(24));
                    g2.setColor(GOLD_BRIGHT);
                }
                g2.drawOval(
                        eyeX - scaled(11),
                        eyeY - scaled(7),
                        scaled(22),
                        scaled(14));
                g2.fillOval(
                        eyeX - scaled(3),
                        eyeY - scaled(3),
                        scaled(6),
                        scaled(6));
            }
            g2.dispose();
        }
    }

    private static final class OrDivider extends JComponent {
        OrDivider() {
            setMaximumSize(new Dimension(Integer.MAX_VALUE, scaled(30)));
            setPreferredSize(new Dimension(CONTROL_WIDTH, scaled(30)));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            String text = "OR CONTINUE WITH";
            g2.setFont(UiFonts.uiRegular(scaled(13f)));
            FontMetrics metrics = g2.getFontMetrics();
            int textWidth = metrics.stringWidth(text);
            int centerX = getWidth() / 2;
            int y = getHeight() / 2;
            g2.setColor(new Color(181, 143, 83));
            g2.drawLine(0, y, centerX - textWidth / 2 - 28, y);
            g2.drawLine(centerX + textWidth / 2 + 28, y, getWidth(), y);
            g2.setColor(GOLD);
            g2.fillPolygon(
                    new int[] { centerX - textWidth / 2 - 22, centerX - textWidth / 2 - 17,
                            centerX - textWidth / 2 - 22, centerX - textWidth / 2 - 27 },
                    new int[] { y - 5, y, y + 5, y },
                    4);
            g2.fillPolygon(
                    new int[] { centerX + textWidth / 2 + 22, centerX + textWidth / 2 + 27,
                            centerX + textWidth / 2 + 22, centerX + textWidth / 2 + 17 },
                    new int[] { y - 5, y, y + 5, y },
                    4);
            g2.setColor(MUTED);
            g2.drawString(text, centerX - textWidth / 2, y + metrics.getAscent() / 2 - 1);
            g2.dispose();
        }
    }

    private static final class CheckBoxIcon implements Icon {
        private final boolean selected;

        CheckBoxIcon() {
            this(false);
        }

        CheckBoxIcon(boolean selected) {
            this.selected = selected;
        }

        @Override
        public int getIconWidth() {
            return scaled(24);
        }

        @Override
        public int getIconHeight() {
            return scaled(24);
        }

        @Override
        public void paintIcon(Component component, Graphics graphics, int x, int y) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(GOLD);
            g2.drawRoundRect(x, y, scaled(22), scaled(22), scaled(5), scaled(5));
            if (selected) {
                g2.setStroke(new BasicStroke(
                        scaled(2.2f),
                        BasicStroke.CAP_ROUND,
                        BasicStroke.JOIN_ROUND));
                g2.drawLine(x + scaled(5), y + scaled(12), x + scaled(10), y + scaled(17));
                g2.drawLine(x + scaled(10), y + scaled(17), x + scaled(18), y + scaled(6));
            }
            g2.dispose();
        }
    }

    private enum SocialProvider {
        GOOGLE("Google", "/assets/social/google-gold.png"),
        DISCORD("Discord", "/assets/social/discord-gold.png"),
        STEAM("Steam", "/assets/social/steam-gold.png");

        private final String label;
        private final String resource;

        SocialProvider(String label, String resource) {
            this.label = label;
            this.resource = resource;
        }
    }

    private <T> void runRequest(
            JButton trigger,
            Callable<T> request,
            Consumer<T> success,
            Runnable cleanup) {
        trigger.setEnabled(false);
        setStatus("Connecting to account server...", false);
        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                return request.call();
            }

            @Override
            protected void done() {
                trigger.setEnabled(true);
                try {
                    success.accept(get());
                } catch (Exception exception) {
                    Throwable cause = exception.getCause() == null ? exception : exception.getCause();
                    setStatus(cause.getMessage() == null ? "Account request failed." : cause.getMessage(), true);
                    if (cleanup != null) cleanup.run();
                }
            }
        }.execute();
    }

    private void setStatus(String message, boolean error) {
        status.setText(message == null || message.isBlank() ? " " : message);
        status.setForeground(error ? ERROR : MUTED);
    }
}
