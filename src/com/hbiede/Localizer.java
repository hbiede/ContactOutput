package com.hbiede;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.PropertyKey;

import java.text.MessageFormat;
import java.util.ResourceBundle;

final class Localizer {
    @NonNls
    private static final String LOCALES_DIR = "locales.contactsOutput";
    @NonNls
    private static final ResourceBundle resourseBundle = ResourceBundle.getBundle(LOCALES_DIR);

    static String i18n_str(@PropertyKey(resourceBundle = LOCALES_DIR) String key, Object... params) {
        String value = resourseBundle.getString(key);

        if (params.length > 0) return MessageFormat.format(value, params);

        return value;
    }
}
