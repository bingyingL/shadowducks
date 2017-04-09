package com.github.netfreer.shadowducks.server.dns;


import com.github.netfreer.shadowducks.common.utils.AddressUtils;

import java.util.concurrent.ConcurrentHashMap;

public class FakeDNSContainer {
    private static final ConcurrentHashMap<Integer, String> IPDomainMaps = new ConcurrentHashMap<Integer, String>();
    private static final ConcurrentHashMap<String, Integer> DomainIPMaps = new ConcurrentHashMap<String, Integer>();

    public String getOrCreateFakeIP(String domainString) {
        Integer fakeIP = DomainIPMaps.get(domainString);
        if (fakeIP == null) {
            int hashIP = domainString.hashCode();
            int mark = 0;
            do {
                fakeIP = AddressUtils.FAKE_NETWORK_IP | (hashIP & 0x0000FFFF);
                hashIP++;
                mark++;
                if (mark > 1000) {
                    throw new IllegalStateException("no available fake ip");
                }
            } while (IPDomainMaps.containsKey(fakeIP));

            DomainIPMaps.put(domainString, fakeIP);
            IPDomainMaps.put(fakeIP, domainString);
        }
        return AddressUtils.ipIntToString(fakeIP);
    }

    public String getDomainByFakeIp(String ip) {
        return IPDomainMaps.get(AddressUtils.ipStringToInt(ip));
    }
}
