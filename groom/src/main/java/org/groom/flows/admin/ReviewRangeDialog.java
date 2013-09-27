package org.groom.flows.admin;

import com.vaadin.data.Validator;
import com.vaadin.event.ShortcutAction;
import com.vaadin.ui.Button;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

/**
 * The comment dialog.
 */
public class ReviewRangeDialog extends Window {

    private final TextField sinceField;
    private final TextField untilField;

    public ReviewRangeDialog(final DialogListener dialogListener,
                             final String sinceHash, final String untilHash) {
        super(); // Set window caption
        setModal(true);
        center();
        setClosable(false);

        final VerticalLayout layout = new VerticalLayout();
        layout.setMargin(true);
        layout.setSpacing(true);

        final Validator validator = new Validator() {
            @Override
            public void validate(Object o) throws InvalidValueException {
                final String value = (String) o;
                for (int i = 0; i < value.length(); i++) {
                    final char c = value.charAt(i);
                    if (!((Character.isLetter(c) && Character.isLowerCase(c))
                            || Character.isDigit(c))) {
                        throw new InvalidValueException("" + c);
                    }
                }
                if (value.length() == 0) {
                    throw new InvalidValueException("?");
                }
            }
        };

        sinceField = new TextField();
        sinceField.setCaption("Since Commit Hash");
        sinceField.setValue(sinceHash);
        sinceField.setWidth(200, Unit.PIXELS);
        sinceField.setMaxLength(100);
        sinceField.addValidator(validator);
        sinceField.setValidationVisible(true);
        layout.addComponent(sinceField);
        untilField = new TextField();
        untilField.setCaption("Until Commit Hash");
        untilField.setValue(untilHash);
        untilField.setWidth(200, Unit.PIXELS);
        untilField.setMaxLength(100);
        untilField.addValidator(validator);
        untilField.setValidationVisible(true);
        layout.addComponent(untilField);
        setContent(layout);

        final HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.setSpacing(true);
        layout.addComponent(buttonLayout);

        final Button okButton = new Button("OK");
        okButton.setClickShortcut(ShortcutAction.KeyCode.ENTER);
        okButton.addStyleName("default");
        okButton.addClickListener(new Button.ClickListener() {
            public void buttonClick(Button.ClickEvent event) {
                if (!sinceField.isValid() || !untilField.isValid()) {
                    return;
                }
                close(); // Close the sub-window
                dialogListener.onOk(
                        sinceField.getValue().trim(),
                        untilField.getValue().trim());
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

    public TextField getSinceField() {
        return sinceField;
    }

    public TextField getUntilField() {
        return untilField;
    }

    public interface DialogListener {
        void onOk(String sinceHash, String untilHash);
        void onCancel();
    }
}