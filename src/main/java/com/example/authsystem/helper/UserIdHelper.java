package com.example.authsystem.helper;

import java.util.UUID;

public class UserIdHelper {
    public static UUID parseUUID(String uuid){
        return UUID.fromString(uuid);
    }
}
