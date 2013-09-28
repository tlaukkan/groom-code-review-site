package org.groom.flows;

import com.vaadin.event.ShortcutAction;
import com.vaadin.server.ThemeResource;
import com.vaadin.ui.*;
import org.groom.GroomSiteUI;

/**
 * The comment dialog.
 */
public class CommentDialog extends Window {

    private final TextArea textArea;

    public CommentDialog(final DialogListener dialogListener) {
        super(); // Set window caption
        setModal(true);
        center();
        setClosable(false);

        final VerticalLayout layout = new VerticalLayout();
        layout.setMargin(true);
        layout.setSpacing(true);

        textArea = new TextArea();
        textArea.setWidth(400, Unit.PIXELS);
        textArea.setHeight(400, Unit.PIXELS);
        layout.addComponent(textArea);
        setContent(layout);

        final HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.setSpacing(true);
        layout.addComponent(buttonLayout);

        final Button giveKudoButton = new Button("Kudo");
        giveKudoButton.setIcon(new ThemeResource(("icons/icon-kudo.png")));
        giveKudoButton.setClickShortcut(ShortcutAction.KeyCode.ENTER);
        giveKudoButton.addStyleName("default");
        giveKudoButton.addClickListener(new Button.ClickListener() {
            public void buttonClick(Button.ClickEvent event) {
                close(); // Close the sub-window
                dialogListener.onOk(textArea.getValue(), 1);
            }
        });
        buttonLayout.addComponent(giveKudoButton);

        final Button raiseWarning = new Button("Warning");
        raiseWarning.setIcon(new ThemeResource(("icons/icon-warning.png")));
        raiseWarning.setClickShortcut(ShortcutAction.KeyCode.ENTER);
        raiseWarning.addStyleName("default");
        raiseWarning.addClickListener(new Button.ClickListener() {
            public void buttonClick(Button.ClickEvent event) {
                close(); // Close the sub-window
                dialogListener.onOk(textArea.getValue(), -1);
            }
        });
        buttonLayout.addComponent(raiseWarning);

        final Button raiseRedFlag = new Button("Red Flag");
        raiseRedFlag.setIcon(new ThemeResource(("icons/icon-red-flag.png")));
        raiseRedFlag.setClickShortcut(ShortcutAction.KeyCode.ENTER);
        raiseRedFlag.addStyleName("default");
        raiseRedFlag.addClickListener(new Button.ClickListener() {
            public void buttonClick(Button.ClickEvent event) {
                close(); // Close the sub-window
                dialogListener.onOk(textArea.getValue(), -2);
            }
        });
        buttonLayout.addComponent(raiseRedFlag);

        final Button cancelButton = new Button("Cancel");
        cancelButton.addClickListener(new Button.ClickListener() {
            public void buttonClick(Button.ClickEvent event) {
                close(); // Close the sub-window
                dialogListener.onCancel();
            }
        });
        buttonLayout.addComponent(cancelButton);

    }

    public TextArea getTextArea() {
        return textArea;
    }


    public interface DialogListener {
        void onOk(String message, int severity);
        void onCancel();
    }
}