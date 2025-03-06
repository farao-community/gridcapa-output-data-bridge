/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa.output_data_bridge;

/**
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 */
public class DataBridgeException extends RuntimeException {
    public DataBridgeException(String message) {
        super(message);
    }

    public DataBridgeException(String message, Throwable cause) {
        super(message, cause);
    }
}
