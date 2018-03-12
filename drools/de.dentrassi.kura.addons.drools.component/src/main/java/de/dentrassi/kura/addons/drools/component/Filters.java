/*
 * Copyright (C) 2018 Jens Reimann <jreimann@redhat.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.dentrassi.kura.addons.drools.component;

import org.osgi.framework.Constants;

public final class Filters {
    private Filters() {
    }

    public static String simpleFilter(final Class<?> objectClass, final String property, final String value) {
        return String
                .format("(&(%s=%s)(%s=%s))",
                        Constants.OBJECTCLASS,
                        objectClass.getName(),
                        property,
                        quote(value));
    }

    public static String quote(final String value) {

        if (value == null) {
            return null;
        }

        final int len = value.length();
        final StringBuilder sb = new StringBuilder(value.length());

        for (int i = 0; i < len; i++) {
            final char c = value.charAt(i);

            switch (c) {
            case '*': //$FALL-THROUGH$
            case '(': //$FALL-THROUGH$
            case ')': //$FALL-THROUGH$
                sb.append('\\');
                break;
            default:
                break;
            }

            sb.append(c);
        }
        return sb.toString();
    }
}
