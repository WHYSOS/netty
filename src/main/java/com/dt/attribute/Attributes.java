package com.dt.attribute;

import com.dt.session.Session;
import io.netty.util.AttributeKey;

public interface Attributes {
    AttributeKey<Session> SESSION = AttributeKey.newInstance("session");
}
