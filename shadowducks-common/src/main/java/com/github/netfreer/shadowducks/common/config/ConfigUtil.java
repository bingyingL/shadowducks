package com.github.netfreer.shadowducks.common.config;

import com.github.netfreer.shadowducks.common.utils.DucksFactory;
import com.google.common.primitives.Ints;

/**
 * @author: landy
 * @date: 2017-04-09 22:51
 */
public class ConfigUtil {
    public static AppConfig loadConfig(String[] args) {
        AppConfig config = new AppConfig();
        config.setServerAddress("0.0.0.0");
        config.setLocalAddress("0.0.0.0");
        config.setLocalPort(1080);
        config.setTimeout(30 * 1000);
        int port = 2999;
        if (args.length > 0) {
            Integer tmp = Ints.tryParse(args[0]);
            if (tmp != null) {
                port = tmp;
            }
        }
        String password = "password";
        if (args.length > 1) {
            password = args[1];
        }
        PortContext first = new PortContext(port, DucksFactory.STREAM_AES_256_CFB, password);
        config.setDestPort(first);
        config.getPorts().add(first);
        return config;
    }
}
