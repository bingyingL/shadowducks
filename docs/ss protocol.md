##协议 Protocol

Shadowsocks是一种基于SOCKS5的安全拆分代理。

>	client <---> ss-local <--[encrypted]--> ss-remote <---> target

ss-local是shadowsocks的本地组件。它像传统的socks5服务器一样为客户端提供代理服务。它对来自客户端的数据流和数据包进行加密，然后转发给shadowsocks的远程组件ss-remote。ss-remote将解密这些数据，然后将他们转发给目的服务器。如果目的服务器有回应数据，ss-remote对其进行加密，转发给ss-local，然后由ss-local对其解密并转发给最初的客户端。


###Addressing

shadowsocks中使用的地址格式与[socks5的地址格式](https://tools.ietf.org/html/rfc1928#section-5)一致。

>	[1-byte type][variable-length host][2-byte port]

一个字节表示类型，后跟对应类型的变长的host，后跟两个字节的端口号。
地址类型的及其长度格式定义如下:

*	0x01: host 为4字节的IPv4地址。
*	0x03: host 变长字符串，第一个字节是长度，后跟长度不超过255的域名字符串。
*	0x04: host 为16字节的IPv6地址。

端口为2字节、大端、无符号整数。

###TCP

ss-local与ss-remote建立tcp连接，一旦建立成功。ss-local立即向ss-remote发送加密后的目的地址及载荷数据。具体的加密方案取决于使用了哪种密码。

>	[target address][payload]

ss-remote接收加密数据流，对其进行解密，提取出最前面的目标地址。ss-remote与目标地址建立TCP连接并将数据转发给它。ss-remote从目标服务器收到回应数据后对其进行加密，然后转发给ss-local。如此往复，直到ss-local断开连接。

###UDP

ss-local将包含目标地址和有效载荷的加密数据包发送给ss-remote。

>	[target address][payload]

ss-remote收到加密数据包，对其进行解密，提取出目的地址。ss-remote向目的地址发送只包含有效载荷的数据包。
ss-remote收到目的地址的回应数据，将目的地址和回应数据一起进行加密，放入UDP数据包，发送给ss-local。

>	[target address][payload]

实质上，ss-remote为ss-local进行了网络地址转换NAT。


##流密码 Stream Ciphers

###加解密方式

>	Stream_encrypt(key, IV, message) => ciphertext

通过预共享密钥、初始化向量IV和原始数据生成加密数据。加密数据长度与原始数据长度一致。

>	Stream_decrypt(key, IV, ciphertext) => message

通过预共享密钥、初始化向量IV和加密数据生成原始数据。

###数据格式

####TCP

随机初始化向量，后跟加密数据。
>	[IV][encrypted payload]

####UDP

随机初始化向量，后跟加密数据。UDP报文是独立加解密的。
>	[IV][encrypted payload]




##AEAD密码 AEAD Ciphers

###生成subkey
使用[HKDF_SHA1](https://tools.ietf.org/html/rfc5869)生成subkey.

>	HKDF_SHA1(key, salt, info) => subkey

key就是配置文件中配的password，通过EVP_BytesToKey算法生成的字符串。
info用来区分不同的应用程序，本程序固定使用`ss-subkey`。
通过预共享master key以及HKDF_SHA1算法，来确定每个会话使用的subkey。
salt为随机生成，且在预共享master key的整个生命周期中必须是唯一的，即每个会话生成一次。

###加解密方式

加密

>	AE_encrypt(key, nonce, message) => (ciphertext, tag)

此key就是上面说的subkey。
每次调用中，针对同一个key，nonce必须是唯一的。

解密

>	AE_decrypt(key, nonce, ciphertext, tag) => message

nonce无符号小端整形，从0开始，每执行一次加密操作，自增1。采用此算法，加密方和解密方可以自动保持一致。


###数据格式

####TCP

tcp流以随机的salt开始，该salt用于生成每个会话的subkey。salt后面是任意数量的加密块。
每个加密块的格式如下：

>	[encrypted payload length][length tag][encrypted payload][payload tag]

`payload length`为两字节的大端无符号整形，最大0x3FFF。最高两位是预留的，必须设置为0。即`payload`的长度被限制在（16*1024-1）。
对于TCP来说，每个块进行了两次加密，nonce应该增加两次。

####UDP

每个加密块的格式如下：

>	[salt][encrypted payload][tag]

salt用于生成每个会话的subkey，必须随机生成以保证唯一性。
每个UDP报文都是独立加解密的，使用全0的nonce和通过salt生成的subkey进行处理即可。
