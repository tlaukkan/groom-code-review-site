package org.groom.flows;

import com.vaadin.event.ShortcutAction;
import com.vaadin.ui.*;

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

        final Button okButton = new Button("OK");
        okButton.setClickShortcut(ShortcutAction.KeyCode.ENTER);
        okButton.addStyleName("default");
        okButton.addClickListener(new Button.ClickListener() {
            public void buttonClick(Button.ClickEvent event) {
                close(); // Close the sub-window
                dialogListener.onOk(textArea.getValue());
            }
        });
        buttonLayout.addComponent(okButton);

        buttonLayout.addComponent(okButton);
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
        void onOk(String message);
        void onCancel();
    }
}