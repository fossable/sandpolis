package com.sandpolis.viewer.cli.view.login;

import static com.googlecode.lanterna.gui2.GridLayout.Alignment.BEGINNING;
import static com.googlecode.lanterna.gui2.GridLayout.Alignment.CENTER;

import java.util.Collections;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.BorderLayout;
import com.googlecode.lanterna.gui2.Borders;
import com.googlecode.lanterna.gui2.Button;
import com.googlecode.lanterna.gui2.EmptySpace;
import com.googlecode.lanterna.gui2.GridLayout;
import com.googlecode.lanterna.gui2.Interactable;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.LayoutData;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.TextBox;
import com.googlecode.lanterna.gui2.Window;
import com.googlecode.lanterna.gui2.dialogs.MessageDialog;
import com.googlecode.lanterna.gui2.dialogs.MessageDialogBuilder;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.sandpolis.viewer.Viewer;
import com.sandpolis.viewer.cmd.LoginCmd;

public class LoginWindow extends BasicWindow {

	private TextBox fld_username;
	private TextBox fld_password;

	static {
		MessageDialog badCert = new MessageDialogBuilder().setTitle("Certificate verification failed")
				.setText("Verification of the server's certificate failed. Do you want to continue connecting?")
				.build();
	}

	public LoginWindow() {
		super("Login");
		init();
	}

	private void init() {
		setHints(Collections.singleton(Window.Hint.CENTERED));

		Panel content = new Panel(new GridLayout(1));
		LayoutData labelData = GridLayout.createLayoutData(CENTER, BEGINNING, false, false);

		{
			Panel server = new Panel(new GridLayout(2));

			Label lbl_address = new Label("Address");
			server.addComponent(lbl_address);

			Label lbl_port = new Label("Port").setLayoutData(labelData);
			server.addComponent(lbl_port);

			TextBox fld_address = new TextBox().setPreferredSize(new TerminalSize(30, 1));
			fld_address.setLayoutData(GridLayout.createLayoutData(BEGINNING, BEGINNING, true, false, 1, 1));
			server.addComponent(fld_address);

			TextBox fld_port = new TextBox().setPreferredSize(new TerminalSize(6, 1))
					.setInputFilter((Interactable e, KeyStroke b) -> {
						if (b.getKeyType() == KeyType.Character) {
							return Character.isDigit(b.getCharacter()) && ((TextBox) e).getText().length() < 5;
						}
						return true;
					});
			server.addComponent(fld_port);

			content.addComponent(server.withBorder(Borders.singleLine("Server")));
		}

		{
			Panel credentials = new Panel(new GridLayout(2).setHorizontalSpacing(5));

			Label lbl_username = new Label("Username").setLayoutData(labelData);
			credentials.addComponent(lbl_username);

			Label lbl_password = new Label("Password").setLayoutData(labelData);
			credentials.addComponent(lbl_password);

			fld_username = new TextBox().setPreferredSize(new TerminalSize(16, 1));
			credentials.addComponent(fld_username);

			fld_password = new TextBox().setPreferredSize(new TerminalSize(16, 1)).setMask('*');
			credentials.addComponent(fld_password);

			content.addComponent(credentials.withBorder(Borders.singleLine("Credentials")));
		}

		{
			Panel buttons = new Panel(new BorderLayout());

			Button btn_exit = new Button("Exit", () -> {
				close();
				try {
					Viewer.getUI().stop();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			});
			buttons.addComponent(btn_exit, BorderLayout.Location.LEFT);
			buttons.addComponent(new EmptySpace(), BorderLayout.Location.CENTER);

			Button btn_login = new Button("Login", () -> {
				fld_username.setEnabled(false);
				fld_password.setEnabled(false);
				try {

					LoginCmd.login(fld_username.getText(), fld_password.getText());
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} finally {
					fld_username.setEnabled(true);
					fld_password.setEnabled(true);
				}
			});
			buttons.addComponent(btn_login, BorderLayout.Location.RIGHT);

			content.addComponent(buttons.withBorder(Borders.singleLineBevel()));
		}

		setComponent(content);
	}
}
