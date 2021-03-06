package com.github.netfreer.shadowducks.server;

import com.github.netfreer.shadowducks.common.config.AppConfig;
import com.github.netfreer.shadowducks.common.config.ConfigUtil;

/**
 * @author: landy
 * @date: 2017-04-09 22:47
 */
public class Main {


    public static void main(String[] args) {
        AppConfig config = ConfigUtil.loadConfig(args);
        new DucksServer().start(config);
    }

}
