package com.kt.cfreminder.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum LineBotCommand {
    REMIND("เตือน")
    ;

    private final String thaiCommand;
}
