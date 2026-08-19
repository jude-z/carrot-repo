package jude.carrot.infra.entity.recover;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum DataType {
    Z_SET("ZSET"),STR("STR");

    private final String type;
}
