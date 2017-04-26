package com.github.netfreer.shadowducks.client;

import com.github.netfreer.shadowducks.common.config.AppConfig;
import com.github.netfreer.shadowducks.common.config.ConfigUtil;

/**
 * @author: landy
 * @date: 2017-04-26 22:22
 */
public class Main {


    public static void main(String[] args) {
        AppConfig config = ConfigUtil.loadConfig(args);
        new DucksClient().start(config);
    }
}