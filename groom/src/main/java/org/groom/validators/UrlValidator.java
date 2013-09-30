package org.groom.validators;

import com.vaadin.data.Validator;

/**
 * Created with IntelliJ IDEA.
 * User: tlaukkan
 * Date: 30.9.2013
 * Time: 19:57
 * To change this template use File | Settings | File Templates.
 */
public class UrlValidator implements Validator {

    @Override
    public void validate(Object value) throws InvalidValueException {
        final String string = (String) value;
        if (string.indexOf("..") != -1) {
            throw new InvalidValueException("..");
        }
        for (int i = 0; i < string.length(); i++) {
            final char c = string.charAt(i);
            if (!(Character.isLetter(c) || Character.isDigit(c) || "@/:.-_".indexOf(c) != -1)) {
                throw new InvalidValueException("" + c);
            }
        }

    }
}
