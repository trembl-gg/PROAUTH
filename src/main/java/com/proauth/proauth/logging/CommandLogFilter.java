package com.proauth.proauth.logging;

import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.filter.AbstractFilter;
import org.apache.logging.log4j.message.Message;

import java.util.Locale;

public class CommandLogFilter extends AbstractFilter {

    @Override
    public Result filter(LogEvent event) {
        Message message = event.getMessage();

        if (message == null) {
            return Result.NEUTRAL;
        }

        String msg = message.getFormattedMessage().toLowerCase(Locale.ROOT);

        if (msg.contains("issued server command: /login ")
                || msg.contains("issued server command: /l ")
                || msg.contains("issued server command: /register ")
                || msg.contains("issued server command: /reg ")
                || msg.contains("issued server command: /changepassword ")
                || msg.contains("issued server command: /cp ")
                || msg.contains("issued server command: /unregister ")) {

            return Result.DENY;
        }

        return Result.NEUTRAL;
    }

}